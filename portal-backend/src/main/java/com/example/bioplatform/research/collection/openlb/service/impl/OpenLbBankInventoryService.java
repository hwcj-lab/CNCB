package com.example.bioplatform.research.collection.openlb.service.impl;

import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 人工启动的来源盘点任务；游标入库，逐条持久化后推进，限流即停止并保留断点。
 */
@Service
@RequiredArgsConstructor
public class OpenLbBankInventoryService {
    private final OpenLbRoutingMapper mapper;
    private final ObjectMapper json;
    private final AtomicBoolean running = new AtomicBoolean(), stop = new AtomicBoolean();
    private volatile String message = "未启动";

    /**
     * 组合当前进程运行标记及数据库持久化进度，供前端轮询；不启动新任务。
     */
    public Map<String, Object> status() {
        return Map.of("running", running.get(), "message", message, "cursor", Optional.ofNullable(mapper.cursor()).orElse(""));
    }

    /**
     * 校验请求及运行状态后启动本服务后台流程；HTTP调用不等待全部处理结束，进度通过status查询。
     */
    public Map<String, Object> start() {
        if (!running.compareAndSet(false, true)) return status();
        stop.set(false);
        Thread thread = new Thread(this::run, "openlb-bank-inventory");
        thread.setDaemon(true);
        thread.start();
        return status();
    }

    /**
     * 设置停止请求，由后台在安全边界收尾；不直接删除已持久化的记录和断点。
     */
    public Map<String, Object> stop() {
        stop.set(true);
        return status();
    }

    private void run() {
        String cursor = Optional.ofNullable(mapper.cursor()).orElse("");
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            while (!stop.get()) {
                var page = mapper.inventoryPage(cursor);
                if (page.isEmpty()) {
                    message = "盘点完成";
                    break;
                }
                for (String pmid : page) {
                    if (stop.get()) break;
                    message = "正在盘点 PMID " + pmid;
                    var req = HttpRequest.newBuilder(URI.create("https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=" + pmid)).timeout(Duration.ofSeconds(25)).GET().build();
                    var response = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200)
                        throw new IllegalStateException("HTTP " + response.statusCode() + "，已停止；稍后启动从断点继续");
                    JsonNode root = json.readTree(response.body());
                    if (!"200".equals(root.path("code").asText()))
                        throw new IllegalStateException("上游返回异常，已停止");
                    JsonNode records = root.path("result").path("data");
                    if (!records.isArray())
                        throw new IllegalStateException("上游结构变化：result.data不是数组，保留断点");
                    for (JsonNode record : records) {
                        int index = 0;
                        for (JsonNode bank : record.path("cncbBanks"))
                            mapper.inventory(pmid, index++, bank.path("name").asText(null), bank.path("accession").asText(null), bank.path("link").asText(null), bank.path("db").asText(null), bank.path("pmid").asText(null));
                    }
                    cursor = pmid;
                    mapper.progress(cursor, "已盘点 " + pmid);
                    Thread.sleep(800);
                }
            }
            if (stop.get()) message = "已停止，可断点继续";
        } catch (Exception e) {
            message = "盘点停止：" + e.getMessage();
        } finally {
            mapper.progress(cursor, message);
            running.set(false);
        }
    }
}
