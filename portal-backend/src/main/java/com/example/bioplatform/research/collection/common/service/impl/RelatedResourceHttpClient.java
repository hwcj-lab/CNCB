package com.example.bioplatform.research.collection.common.service.impl;


import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** 新资源解析器共享的限速读取器；只缓存成功响应，缓存有容量及有效期上限。 */
@Component
public class RelatedResourceHttpClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, Cached> cache = new LinkedHashMap<>(256, .75f, true);
    private long nextRequest;
    private record Cached(String body, long time) {}

    /** 单个关联任务内的重复资源优先命中缓存；超时、限流、安全检查页面均抛出错误。 */
    public synchronized String get(String url) throws Exception {
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme()) || !"ngdc.cncb.ac.cn".equalsIgnoreCase(uri.getHost()) || uri.getUserInfo()!=null)
            throw new IllegalArgumentException("资源链接必须为NGDC官方HTTPS地址");
        Cached hit=cache.get(url);
        if(hit!=null && System.currentTimeMillis()-hit.time()<900_000) return hit.body();
        long wait=nextRequest-System.currentTimeMillis();
        if(wait>0) Thread.sleep(wait);
        nextRequest=System.currentTimeMillis()+800;
        var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(25)).header("Accept","application/json,text/html").GET().build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()!=200) throw new IOException("资源接口HTTP "+response.statusCode()+"："+url);
        String body=response.body();
        String lower=body.toLowerCase(Locale.ROOT);
        if(lower.contains("<title>securitycheck</title>") || lower.contains("verifying your browser"))
            throw new IOException("官方资源要求浏览器安全验证，未完成解析："+url);
        if(body.length()>4_000_000) throw new IOException("资源响应过大，请检查接口分页："+url);
        cache.put(url,new Cached(body,System.currentTimeMillis()));
        while(cache.size()>256)cache.remove(cache.keySet().iterator().next());
        return body;
    }
}
