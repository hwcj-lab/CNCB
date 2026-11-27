package com.example.bioplatform.research.collection.common.service.impl;


import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 补充采集共用HTTP传输：读超时、全局请求间隔、有限重试及磁盘流式下载。
 */
@Component
public class CollectionHttpTransport {
    /**
     * delay：请求或处理间隔，具体单位见调用方法。
     * nextRequest：下次允许发出上游请求的时间边界。
     */
    private volatile long delay = 800, nextRequest;

    /**
     * 规范化请求间隔配置，避免小于采集器的限速下限。
     * @return 按上述规则计算或读取的long结果
     */
    public long delay() {
        return delay;
    }

    /**
     * 设置共享HTTP传输的限速、尝试次数及停止检查规则。
     * @param delay 请求或处理间隔，具体单位见调用方法
     */
    public void configure(long delay) {
        this.delay = delay;
    }

    /**
     * 接口失败最多重试配置次数；限流/暂不可用时降速，不返回伪空数据。
     */
    public String text(String url, int attempts, BooleanSupplier stopped, Consumer<String> progress) throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        execute(url, attempts, stopped, progress, (in, length) -> {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                check(stopped);
                if (data.size() + n > 8_000_000) throw new IOException("元数据响应过大");
                data.write(buffer, 0, n);
            }
        }, () -> data.reset());
        return data.toString(StandardCharsets.UTF_8);
    }

    /**
     * 下载结束后原子改名；断在下载阶段时仅重新下载快照，不会推进文章导入游标。
     */
    public void download(String url, Path file, int attempts, BooleanSupplier stopped, Consumer<String> progress) throws Exception {
        Files.createDirectories(file.getParent());
        Path partial = file.resolveSibling(file.getFileName() + ".part");
        try {
            execute(url, attempts, stopped, progress, (in, length) -> {
                try (OutputStream out = Files.newOutputStream(partial)) {
                    byte[] buffer = new byte[32768];
                    long count = 0, last = System.currentTimeMillis();
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        check(stopped);
                        count += n;
                        if (count > 512_000_000L) throw new IOException("统计快照超过512MB安全上限");
                        out.write(buffer, 0, n);
                        if (System.currentTimeMillis() - last > 1000) {
                            progress.accept("正在下载GSA固定快照 " + count + " / " + length + " 字节");
                            last = System.currentTimeMillis();
                        }
                    }
                }
            }, () -> {
            });
            validateJsonPrefix(partial);
            try {
                Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    /**
     * 共享下载器允许对象或数组，具体来源Reader再严格校验结构。
     */
    public static void validateJsonPrefix(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            int c;
            do {
                c = in.read();
            } while (c >= 0 && Character.isWhitespace(c));
            if (c != '{' && c != '[') throw new IOException("上游响应不是JSON，可能触发安全验证");
        }
    }

    /**
     * HTTP响应消费回调，避免传输层绑定具体数据库解析规则。
     */
    @FunctionalInterface
    private interface Receiver {
        /**
         * 消费已经建立的HTTP响应并返回解析结果；响应流的关闭由传输层负责。
         * @param in 原始输入流或输入对象
         * @param length 截断后的文本长度上限
         */
        void read(InputStream in, long length) throws Exception;
    }

    /**
     * 按传输配置发送请求并交给响应处理器；限流及网络异常遵循有限重试规则。
     * @param url 当前请求或资源的访问地址
     * @param attempts 当前或配置的尝试次数
     * @param stopped 是否已经停止或检查停止状态的回调
     * @param progress 用于持久化或展示进度的回调
     * @param receiver 消费网络响应体的回调
     * @param reset 是否重置相关任务状态
     */
    private void execute(String url, int attempts, BooleanSupplier stopped, Consumer<String> progress, Receiver receiver, Runnable reset) throws Exception {
        Exception error = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            check(stopped);
            reset.run();
            long wait = reserveRequest();
            if (wait > 0) pause(wait, stopped);
            HttpURLConnection connection = null;
            try {
                URI uri = URI.create(url);
                if (!"https".equals(uri.getScheme()) || !java.util.Set.of("ngdc.cncb.ac.cn", "eutils.ncbi.nlm.nih.gov").contains(uri.getHost()))
                    throw new IOException("未允许的采集地址");
                connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(25000);
                connection.setRequestProperty("Accept", "application/json,application/xml");
                connection.setRequestProperty("User-Agent", "BioPortalPublicationCollector/1.0");
                int code = connection.getResponseCode();
                if (code != 200) {
                    if (code == 429 || code >= 500) delay = Math.min(10000, Math.max(800, delay * 2));
                    throw new IOException("HTTP " + code + "：" + url);
                }
                try (InputStream in = connection.getInputStream()) {
                    receiver.read(in, connection.getContentLengthLong());
                }
                return;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } catch (Exception ex) {
                error = ex;
                progress.accept("上游请求失败 " + attempt + "/" + attempts + "：" + ex.getMessage() + "；当前间隔 " + delay + " ms");
            } finally {
                if (connection != null) connection.disconnect();
            }
            if (attempt < attempts) pause(delay, stopped);
        }
        throw error == null ? new IOException("采集请求失败") : error;
    }

    /**
     * 为当前请求预留全局发送时刻，避免多个调用同时突破配置的请求间隔。
     * @return 按上述规则计算或读取的long结果
     */
    private synchronized long reserveRequest() {
        long now = System.currentTimeMillis(), target = Math.max(now, nextRequest);
        nextRequest = target + delay;
        return target - now;
    }

    /**
     * 检查人工停止或线程中断状态，在继续请求或写入前及时退出。
     * @param stopped 是否已经停止或检查停止状态的回调
     */
    private static void check(BooleanSupplier stopped) throws InterruptedException {
        if (stopped.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("采集已停止");
    }

    /**
     * 按指定时长等待，并持续遵守线程中断或停止约定。
     * @param ms 等待时长，单位毫秒
     * @param stopped 是否已经停止或检查停止状态的回调
     */
    private static void pause(long ms, BooleanSupplier stopped) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            check(stopped);
            Thread.sleep(Math.min(200, Math.max(1, end - System.currentTimeMillis())));
        }
    }
}
