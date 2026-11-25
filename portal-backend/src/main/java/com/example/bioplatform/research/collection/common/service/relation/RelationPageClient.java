package com.example.bioplatform.research.collection.common.service.relation;

import org.springframework.stereotype.Component;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.IOException;

/**
 * 关联路径共享HTTP读取器；保留原超时、重定向与状态码行为，不触发采集任务。
 */
@Component
public class RelationPageClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();

    public String get(String url) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Accept", "application/json,text/html;q=0.9,*/*;q=0.8").GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + "：" + url);
        return response.body();
    }
}
