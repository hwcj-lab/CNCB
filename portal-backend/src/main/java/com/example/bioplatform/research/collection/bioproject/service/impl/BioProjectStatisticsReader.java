package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.fasterxml.jackson.core.*;import com.fasterxml.jackson.databind.*;import java.io.*;import java.nio.file.*;import java.util.*;
/** 项目列表以磁盘快照流式读取；每个项目全部论文落库后才提交项目游标。 */
public final class BioProjectStatisticsReader {
 @FunctionalInterface public interface ProjectLoader {JsonNode load(JsonNode row)throws Exception;}
 @FunctionalInterface public interface BatchConsumer {boolean accept(List<JsonNode> rows,long nextIndex)throws Exception;}
 public static boolean read(Path file,long after,int size,ObjectMapper json,ProjectLoader loader,BatchConsumer sink)throws Exception {
  try(var in=Files.newInputStream(file);var parser=json.getFactory().createParser(in)){
   if(parser.nextToken()!=JsonToken.START_ARRAY)throw new IOException("BioProject列表不是JSON数组");
   long index=0;
   while(parser.nextToken()!=JsonToken.END_ARRAY){
    if(parser.currentToken()!=JsonToken.START_OBJECT)throw new IOException("BioProject列表条目不是对象");
    if(index++<after){parser.skipChildren();continue;}
    JsonNode row=json.readTree(parser);JsonNode project=loader.load(row);
    List<JsonNode> batch=new ArrayList<>(size);
    for(JsonNode paper:project.path("listPublication")){
     var enriched=paper.deepCopy();if(!enriched.isObject())continue;
     ((com.fasterxml.jackson.databind.node.ObjectNode)enriched).put("proAccession",BioProjectPublicationParser.projectCode(row));
     ((com.fasterxml.jackson.databind.node.ObjectNode)enriched).set("projectGrants",project.path("listGrants"));
     ((com.fasterxml.jackson.databind.node.ObjectNode)enriched).set("projectOrganization",project.path("submitter").path("organization"));
     batch.add(enriched);
     // 项目最后一个批次之前不推进项目游标；异常后重做本项目，UPSERT幂等。
     if(batch.size()==size){if(!sink.accept(List.copyOf(batch),index-1))return false;batch.clear();}
    }
    if(!sink.accept(List.copyOf(batch),index))return false;
   }
   if(index<after)throw new IOException("BioProject固定列表短于已提交游标");return true;
  }
 }
}
