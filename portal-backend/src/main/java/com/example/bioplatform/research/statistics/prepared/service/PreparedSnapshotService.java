package com.example.bioplatform.research.statistics.prepared.service;
import com.example.bioplatform.research.preparation.service.*;
import com.example.bioplatform.research.statistics.prepared.mapper.PreparedStatisticsMapper;
import com.example.bioplatform.research.statistics.prepared.model.*;
import com.example.bioplatform.research.statistics.service.ResearchSnapshotService;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.statistics.dto.*;
import com.example.bioplatform.research.statistics.vo.*;
import com.example.bioplatform.research.collection.common.vo.ResearchPublicationSourceOption;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
/** Snapshots read only prepared facts/members. No raw JSON parsing or per-snapshot member copies. */
@Service @RequiredArgsConstructor
public class PreparedSnapshotService implements ResearchSnapshotService {
    private final PreparationService preparation;
    private final PreparedStatisticsMapper prepared;
    private final ResearchSnapshotMapper mapper;
    private final ResearchPublicationSourceRegistry registry;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final AtomicBoolean running=new AtomicBoolean(),stop=new AtomicBoolean();
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r-> {
        var t=new Thread(r,"prepared-snapshot");
        t.setDaemon(true);
        return t;
    });
    private volatile Long currentId;
    private volatile String message="先采集，再整理，最后生成快照";
    private static final List<String> TYPES=List.of("YEAR","AUTHOR","INSTITUTION","FUND","TOPIC","DATABASE");
    @Override public synchronized ResearchSnapshotStatus startBuild(ResearchSnapshotBuildRequest request) {
        long revision=preparation.requireReady();
        if(!running.compareAndSet(false,true))throw conflict("已有快照正在生成");
        try {
            ResearchSnapshotCreate create=tx.execute(s-> {
                if(prepared.lock()!=1)throw conflict("其他进程正在创建快照");
                try {
                    if(mapper.countBuildingSnapshots()>0)throw conflict("存在未结束快照，请确认旧进程停止后恢复");
                    var sources=registry.options();
                    var row=new ResearchSnapshotCreate();
                    row.setVersionNo(mapper.selectNextVersionNo());
                    row.setSnapshotName(request!=null&&request.getName()!=null&&!request.getName().isBlank()?request.getName().trim():"科研统计快照 v"+row.getVersionNo());
                    row.setIncludeRelated(request!=null&&Boolean.TRUE.equals(request.getIncludeRelated()));
                    row.setIncludeResources(request!=null&&Boolean.TRUE.equals(request.getIncludeResources()));
                    row.setSourceCount(sources.size());
                    row.setSourceCodes(sources.stream().map(ResearchPublicationSourceOption::code).collect(Collectors.joining(",")));
                    row.setStartedTime(LocalDateTime.now());
                    mapper.insertSnapshot(row);
                    Map<String,String> names=new LinkedHashMap<>();
                    sources.forEach(o->names.put(o.code(),o.name()));
                    var cfg=new PreparedSnapshotConfig();
                    cfg.setSnapshotId(row.getId());
                    cfg.setPreparationRevision(revision);
                    cfg.setSourceNamesJson(encode(names));
                    cfg.setTopicScopesJson(encode(topicScopes(names)));
                    prepared.saveConfig(cfg);
                    return row;
                }
                finally {
                    prepared.unlock();
                }
            });
            stop.set(false);
            currentId=create.getId();
            message="正在从整理表聚合";
            executor.submit(()->build(create.getId()));
            return status();
        }
        catch(RuntimeException ex) {
            running.set(false);
            throw ex;
        }
    }
    private Map<String,List<String>> topicScopes(Map<String,String> names) {
        Map<String,List<String>> result=new LinkedHashMap<>();
        var mappings=mapper.selectConfirmedDatabaseTopics();
        for(var topic:mapper.selectEnabledTopicCategories()) {
            var codes=names.keySet().stream().filter(code->mappings.stream().anyMatch(m->topic.getTopicCode().equals(m.getTopicCode())&&(normalize(code).equals(normalize(m.getDatabaseCode()))||normalize(names.get(code)).equals(normalize(m.getDatabaseCode()))))).toList();
            result.put(topic.getTopicCode()+"|"+topic.getTopicName(),codes);
        }
        return result;
    }
    private void build(long id) {
        try {
            var snapshot=detail(id);
            var cfg=config(id);
            var q=scope(snapshot,cfg,"TOTAL");
            int total=exact(prepared.count(q));
            long resources=prepared.resources(q);
            int year=exact(prepared.coverage(q,PreparedMetadataParser.table("YEAR"))),author=exact(prepared.coverage(q,PreparedMetadataParser.table("AUTHOR"))),institution=exact(prepared.coverage(q,PreparedMetadataParser.table("INSTITUTION"))),fund=exact(prepared.coverage(q,PreparedMetadataParser.table("FUND")));
            List<String> scopes=new ArrayList<>();
            scopes.add("TOTAL");
            scopes.addAll(q.getSources());
            for(String code:scopes) {
                checkStopped();
                q=scope(snapshot,cfg,code);
                message="聚合 "+code;
                for(String type:List.of("YEAR","AUTHOR","INSTITUTION","FUND","TOPIC")) {
                    checkStopped();
                    if("TOTAL".equals(code)&&"TOPIC".equals(type))continue;
                    q.setType(type);
                    var ranked=prepared.rank(q,PreparedMetadataParser.table(type));
                    for(var row:ranked) {
                        row.setScopeCode(code);
                        row.setDimensionType(type);
                    }
                    for(int offset=0;
                    offset<ranked.size();
                    offset+=50)prepared.insertDimensions(id,ranked.subList(offset,Math.min(offset+50,ranked.size())));
                }
            }
            q=scope(snapshot,cfg,"TOTAL");
            for(var entry:names(cfg).entrySet()) {
                checkStopped();
                q.setScope(entry.getKey());
                save(id,"TOTAL","DATABASE",entry.getKey(),entry.getValue(),exact(prepared.count(q)));
            }
            Set<String> topicSources=new LinkedHashSet<>();
            for(var entry:topics(cfg).entrySet()) {
                checkStopped();
                int count=0;
                for(String code:entry.getValue()) {
                    q.setScope(code);
                    count=Math.addExact(count,exact(prepared.count(q)));
                    topicSources.add(code);
                }
                String[] parts=entry.getKey().split("\\|",2);
                save(id,"TOTAL","TOPIC",parts[0],parts.length>1?parts[1]:parts[0],count);
            }
            q=scope(snapshot,cfg,"TOTAL");
            q.setSourceFilter(new ArrayList<>(topicSources));
            int topic=exact(prepared.count(q));
            checkStopped();
            mapper.markSuccess(id,total,resources,year,topic,author,institution,fund);
            message="快照生成完成";
        }
        catch(Stopped ex) {
            mapper.deleteSnapshotDimensions(id);
            mapper.markStopped(id,"已停止");
            message="快照已停止";
        }
        catch(Exception ex) {
            mapper.deleteSnapshotDimensions(id);
            String error=Objects.toString(ex.getMessage(),ex.getClass().getSimpleName());
            mapper.markFailed(id,error.substring(0,Math.min(1900,error.length())));
            message="快照失败："+error.substring(0,Math.min(300,error.length()));
        }
        finally {
            running.set(false);
            stop.set(false);
        }
    }
    private void save(long id,String scope,String type,String key,String label,int count) {
        var row=new ResearchSnapshotDimension();
        row.setScopeCode(scope);
        row.setDimensionType(type);
        row.setDimensionKey(key);
        row.setDisplayName(label);
        row.setPublicationCount(count);
        prepared.insertDimension(id,row);
    }
    @Override public ResearchSnapshotStatus stopBuild() {
        if(running.get())stop.set(true);
        return status();
    }
    public synchronized ResearchSnapshotStatus recover() {
        if(running.get())throw conflict("当前进程仍在生成快照");
        var building=mapper.selectAnyBuildingSnapshot();
        if(building!=null) {
            mapper.deleteSnapshotDimensions(building.getId());
            mapper.markFailed(building.getId(),"进程中断，已回收未发布版本");
        }
        return status();
    }
    @Override public ResearchSnapshotStatus status() {
        var building=mapper.selectAnyBuildingSnapshot();
        return new ResearchSnapshotStatus(running.get()||building!=null,message,building!=null?building:currentId==null?null:mapper.selectDetail(currentId),latest());
    }
    @Override public List<ResearchPublicationSourceOption> sourceOptions() {
        return registry.options();
    }
    @Override public List<ResearchSnapshotDetail> versions() {
        return prepared.versions();
    }
    @Override public ResearchSnapshotDetail latest() {
        return prepared.latestSuccess();
    }
    @Override public ResearchSnapshotDetail detail(long id) {
        config(id);
        var row=mapper.selectDetail(id);
        if(row==null)throw missing("快照不存在");
        return row;
    }
    @Override public ResearchSnapshotResult latestResult(int limit) {
        var latest=latest();
        if(latest==null)throw missing("尚无整理后生成的成功快照，请先整理数据");
        return result(latest.getId(),limit);
    }
    @Override public ResearchSnapshotResult result(long id,int limit) {
        return scopedResult(id,"TOTAL",limit);
    }
    @Override public ResearchSnapshotResult databaseResult(long id,String code,int limit) {
        String scope=code.toUpperCase(Locale.ROOT);
        if(!names(config(id)).containsKey(scope))throw missing("该快照没有此来源");
        return scopedResult(id,scope,limit);
    }
    private ResearchSnapshotResult scopedResult(long id,String code,int limit) {
        var snapshot=detail(id);
        if(!"SUCCESS".equals(snapshot.getStatus()))throw conflict("该快照尚未成功发布");
        Map<String,List<ResearchSnapshotDimension>> result=new LinkedHashMap<>();
        for(String type:TYPES)result.put(type,mapper.selectDimensions(id,code,type,Set.of("YEAR","DATABASE").contains(type)||("TOTAL".equals(code)&&"TOPIC".equals(type))?1000:Math.min(50,Math.max(1,limit))));
        if("TOTAL".equals(code))result.put("TOPIC",completeTopicCategories(id,result.get("TOPIC")));
        return new ResearchSnapshotResult(snapshot,result);
    }
    /** Keep the frozen taxonomy visible even when a category has no source or no matching records. */
    private List<ResearchSnapshotDimension> completeTopicCategories(long id,List<ResearchSnapshotDimension> stored) {
        Map<String,ResearchSnapshotDimension> byCode=new LinkedHashMap<>();
        for(var row:stored)byCode.put(row.getDimensionKey(),row);
        List<ResearchSnapshotDimension> result=new ArrayList<>();
        for(String category:topics(config(id)).keySet()) {
            String[] parts=category.split("\\|",2);
            var row=byCode.remove(parts[0]);
            if(row==null) {
                row=new ResearchSnapshotDimension();
                row.setScopeCode("TOTAL");
                row.setDimensionType("TOPIC");
                row.setDimensionKey(parts[0]);
                row.setDisplayName(parts.length>1?parts[1]:parts[0]);
                row.setPublicationCount(0);
            }
            result.add(row);
        }
        result.addAll(byCode.values());
        return result;
    }
    @Override public List<ResearchSnapshotDatabaseOption> databases(long id) {
        detail(id);
        return mapper.selectDimensions(id,"TOTAL","DATABASE",1000).stream().map(d->new ResearchSnapshotDatabaseOption(d.getDimensionKey(),d.getDisplayName(),d.getPublicationCount())).toList();
    }
    @Override public List<ResearchSnapshotDatabaseOption> topicDatabases(long id,String topic) {
        var codes=topics(config(id)).entrySet().stream().filter(e->e.getKey().split("\\|",2)[0].equalsIgnoreCase(topic)).flatMap(e->e.getValue().stream()).collect(Collectors.toSet());
        return databases(id).stream().filter(d->codes.contains(d.code())).toList();
    }
    @Override public List<ResearchSnapshotDimension> dimensions(long id,String type,int limit) {
        String t=type.toUpperCase(Locale.ROOT);
        if(!TYPES.contains(t))throw missing("无此维度");
        return scopedResult(id,"TOTAL",limit).dimensions().get(t);
    }
    @Override public ResearchSnapshotPublicationPage dimensionPublications(long id,long dimensionId,int page,int size) {
        var snapshot=detail(id);
        if(!"SUCCESS".equals(snapshot.getStatus()))throw conflict("只能查询已发布快照");
        var row=prepared.dimension(id,dimensionId);
        if(row==null)throw missing("维度不存在");
        var cfg=config(id);
        var q=scope(snapshot,cfg,row.getScopeCode());
        q.setType(row.getDimensionType());
        q.setMemberKey(row.getDimensionKey());
        String table=null;
        if("TOTAL".equals(q.getScope())&&"TOPIC".equals(q.getType())) {
            q.setType("CATEGORY");
            q.setSourceFilter(topics(cfg).entrySet().stream().filter(e->e.getKey().split("\\|",2)[0].equals(row.getDimensionKey())).flatMap(e->e.getValue().stream()).toList());
        }
        else if(!Set.of("YEAR","DATABASE").contains(q.getType()))table=PreparedMetadataParser.table(q.getType());
        int p=Math.max(1,page),n=Math.max(1,Math.min(100,size));
        long count=prepared.coverage(q,table);
        var items=prepared.publications(q,table,(long)(p-1)*n,n);
        items.forEach(r->r.setScopeCode(row.getScopeCode()));
        return new ResearchSnapshotPublicationPage(count,p,n,items);
    }
    @Override public void deleteSnapshot(long id) {
        var snapshot=detail(id);
        if("BUILDING".equals(snapshot.getStatus()))throw conflict("先停止快照");
        tx.executeWithoutResult(s-> {
            prepared.deleteConfig(id);
            mapper.deleteSnapshot(id);
        });
    }
    @Override public ResearchSnapshotDetail renameSnapshot(long id,String name) {
        detail(id);
        if(name==null||name.isBlank()||name.trim().length()>100)throw conflict("名称必须为1至100字");
        mapper.updateSnapshotName(id,name.trim());
        return detail(id);
    }
    private PreparedSnapshotConfig config(long id) {
        var c=prepared.config(id);
        if(c==null)throw missing("快照不存在或属于旧统计格式；旧数据保留，请从整理结果生成新快照");
        return c;
    }
    private PreparedScope scope(ResearchSnapshotDetail s,PreparedSnapshotConfig c,String code) {
        var q=new PreparedScope();
        q.setRevision(c.getPreparationRevision());
        q.setSources(new ArrayList<>(names(c).keySet()));
        q.setScope(code);
        q.setIncludeRelated(Boolean.TRUE.equals(s.getIncludeRelated()));
        q.setIncludeResources(Boolean.TRUE.equals(s.getIncludeResources()));
        return q;
    }
    private Map<String,String> names(PreparedSnapshotConfig c) {
        try {
            return json.readValue(c.getSourceNamesJson(),json.getTypeFactory().constructMapType(LinkedHashMap.class,String.class,String.class));
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private Map<String,List<String>> topics(PreparedSnapshotConfig c) {
        try {
            return json.readValue(c.getTopicScopesJson(),new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String,List<String>>>() {
            });
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private String encode(Object v) {
        try {
            return json.writeValueAsString(v);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private static String normalize(String s) {
        return s==null?"":s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");
    }
    private static int exact(long n) {
        return Math.toIntExact(n);
    }
    private void checkStopped() {
        if(stop.get())throw new Stopped();
    }
    private static class Stopped extends RuntimeException {
    }
    private static BusinessException conflict(String s) {
        return new BusinessException(HttpStatus.CONFLICT,"SNAPSHOT_NOT_READY",s);
    }
    private static BusinessException missing(String s) {
        return new BusinessException(HttpStatus.NOT_FOUND,"SNAPSHOT_NOT_FOUND",s);
    }
    @PreDestroy public void shutdown() {
        stop.set(true);
        executor.shutdown();
        try {
            if(!executor.awaitTermination(10,TimeUnit.SECONDS))executor.shutdownNow();
        }
        catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
