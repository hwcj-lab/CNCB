package com.example.bioplatform.research.collection.common.service.relation;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 关联路径共享HTTP读取器；保留原超时、重定向与状态码行为，不触发采集任务。
 */
@Component
public class RelationPageClient {
    /**
     * client：用于访问上游的HTTP客户端实例。
     */
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();

    /**
     * 读取指定地址的上游正文；网络、状态码及解析相关异常由调用方处理。
     * @param url 当前请求或资源的访问地址
     * @return 按上述规则计算或读取的String结果
     */
    public String get(String url) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Accept", "application/json,text/html;q=0.9,*/*;q=0.8").GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + "：" + url);
        return response.body();
    }
}
