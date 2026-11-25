package com.example.bioplatform.research.collection.gsa.service.impl;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;import java.nio.file.*;import java.util.*;

/** 流式读取article.publications；每个批次完成入库后再推进断点，不加载整个JSON树。 */
public final class GsaStatisticsReader {
    private GsaStatisticsReader(){}
    @FunctionalInterface public interface BatchConsumer { boolean accept(List<JsonNode> rows,long nextIndex)throws Exception; }
    /** 只在本地快照重读时跳过已提交索引；一轮导入只遍历文件一次。 */
    public static boolean read(Path file,long after,int size,ObjectMapper json,BatchConsumer sink)throws Exception {
        try(InputStream in=Files.newInputStream(file);JsonParser p=json.getFactory().createParser(in)) {
            if(p.nextToken()!=JsonToken.START_OBJECT)throw new IOException("GSA统计响应不是JSON对象，可能被安全验证拦截");
            while(p.nextToken()!=JsonToken.END_OBJECT) {
                String name=p.currentName();p.nextToken();
                if(!"article".equals(name)){p.skipChildren();continue;}
                if(p.currentToken()!=JsonToken.START_OBJECT)throw new IOException("article不是对象");
                while(p.nextToken()!=JsonToken.END_OBJECT){
                    String field=p.currentName();p.nextToken();
                    if(!"publications".equals(field)){p.skipChildren();continue;}
                    if(p.currentToken()!=JsonToken.START_ARRAY)throw new IOException("article.publications不是数组");
                    long index=0;List<JsonNode> batch=new ArrayList<>(size);
                    while(p.nextToken()!=JsonToken.END_ARRAY){
                        if(p.currentToken()!=JsonToken.START_OBJECT)throw new IOException("支持文章条目不是对象");
                        if(index++<after){p.skipChildren();continue;}
                        batch.add(json.readTree(p));
                        if(batch.size()>=size){if(!sink.accept(List.copyOf(batch),index))return false;batch.clear();}
                    }
                    if(!batch.isEmpty() && !sink.accept(List.copyOf(batch),index))return false;
                    if(index<after)throw new IOException("本地快照长度小于持久化游标，请恢复原缓存文件");
                    return true;
                }
                throw new IOException("GSA统计缺少article.publications，禁止将结构变化当作采集完成");
            }
            throw new IOException("GSA统计缺少article对象");
        }
    }
}
