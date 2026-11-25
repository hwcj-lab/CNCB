package com.example.bioplatform.research.collection.common.service.impl;


import org.springframework.stereotype.Component;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;
import java.util.function.*;

/** 补充采集共用HTTP传输：读超时、全局请求间隔、有限重试及磁盘流式下载。 */
@Component
public class CollectionHttpTransport {
    private volatile long delay=800,nextRequest;
    public long delay(){return delay;}
    public void configure(long delay){this.delay=delay;}
    /** 接口失败最多重试配置次数；限流/暂不可用时降速，不返回伪空数据。 */
    public String text(String url,int attempts,BooleanSupplier stopped,Consumer<String> progress)throws Exception {
        ByteArrayOutputStream data=new ByteArrayOutputStream();
        execute(url,attempts,stopped,progress,(in,length)->{
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){check(stopped);if(data.size()+n>8_000_000)throw new IOException("元数据响应过大");data.write(buffer,0,n);}
        },()->data.reset());
        return data.toString(StandardCharsets.UTF_8);
    }
    /** 下载结束后原子改名；断在下载阶段时仅重新下载快照，不会推进文章导入游标。 */
    public void download(String url,Path file,int attempts,BooleanSupplier stopped,Consumer<String> progress)throws Exception {
        Files.createDirectories(file.getParent());Path partial=file.resolveSibling(file.getFileName()+".part");
        try {
            execute(url,attempts,stopped,progress,(in,length)->{
                try(OutputStream out=Files.newOutputStream(partial)){
                    byte[] buffer=new byte[32768];long count=0,last=System.currentTimeMillis();int n;
                    while((n=in.read(buffer))!=-1){check(stopped);count+=n;if(count>512_000_000L)throw new IOException("统计快照超过512MB安全上限");out.write(buffer,0,n);
                        if(System.currentTimeMillis()-last>1000){progress.accept("正在下载GSA固定快照 "+count+" / "+length+" 字节");last=System.currentTimeMillis();}
                    }
                }
            },()->{});
            validateJsonPrefix(partial);
            try{Files.move(partial,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(partial,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(partial);}
    }
    /** 共享下载器允许对象或数组，具体来源Reader再严格校验结构。 */
    public static void validateJsonPrefix(Path file)throws IOException {
        try(InputStream in=Files.newInputStream(file)){int c;do{c=in.read();}while(c>=0&&Character.isWhitespace(c));if(c!='{' && c!='[')throw new IOException("上游响应不是JSON，可能触发安全验证");}
    }
    @FunctionalInterface private interface Receiver {void read(InputStream in,long length)throws Exception;}
    private void execute(String url,int attempts,BooleanSupplier stopped,Consumer<String> progress,Receiver receiver,Runnable reset)throws Exception {
        Exception error=null;
        for(int attempt=1;attempt<=attempts;attempt++){
            check(stopped);reset.run();long wait=reserveRequest();if(wait>0)pause(wait,stopped);
            HttpURLConnection connection=null;
            try{
                URI uri=URI.create(url);
                if(!"https".equals(uri.getScheme()) || !java.util.Set.of("ngdc.cncb.ac.cn","eutils.ncbi.nlm.nih.gov").contains(uri.getHost()))throw new IOException("未允许的采集地址");
                connection=(HttpURLConnection)uri.toURL().openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(25000);
                connection.setRequestProperty("Accept","application/json,application/xml");connection.setRequestProperty("User-Agent","BioPortalPublicationCollector/1.0");
                int code=connection.getResponseCode();
                if(code!=200){if(code==429||code>=500)delay=Math.min(10000,Math.max(800,delay*2));throw new IOException("HTTP "+code+"："+url);}
                try(InputStream in=connection.getInputStream()){receiver.read(in,connection.getContentLengthLong());}return;
            }catch(InterruptedException ex){Thread.currentThread().interrupt();throw ex;}
            catch(Exception ex){error=ex;progress.accept("上游请求失败 "+attempt+"/"+attempts+"："+ex.getMessage()+"；当前间隔 "+delay+" ms");}
            finally{if(connection!=null)connection.disconnect();}
            if(attempt<attempts)pause(delay,stopped);
        }
        throw error==null?new IOException("采集请求失败"):error;
    }
    private synchronized long reserveRequest(){long now=System.currentTimeMillis(),target=Math.max(now,nextRequest);nextRequest=target+delay;return target-now;}
    private static void check(BooleanSupplier stopped)throws InterruptedException {if(stopped.getAsBoolean() || Thread.currentThread().isInterrupted())throw new InterruptedException("采集已停止");}
    private static void pause(long ms,BooleanSupplier stopped)throws InterruptedException{long end=System.currentTimeMillis()+ms;while(System.currentTimeMillis()<end){check(stopped);Thread.sleep(Math.min(200,Math.max(1,end-System.currentTimeMillis())));}}
}
