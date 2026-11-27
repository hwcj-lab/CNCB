package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentWrite;
import com.example.bioplatform.research.collection.bioproject.mapper.BioProjectCollectionMapper;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectPublicationParser;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentTask;
import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentWrite;
import com.example.bioplatform.research.collection.gsa.dto.GsaPublicationSeed;
import com.example.bioplatform.research.collection.gsa.mapper.GsaCollectionMapper;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * 无PMID项目论文沿用来源表稳定键写入；不创建额外论文表、不误标为主文献。
 */
@Component
public class NativeRelatedPublicationWriter {
    /**
     * gsa：GSA来源的持久化或元数据访问组件。
     */
    private final GsaCollectionMapper gsa;
    /**
     * projects：BioProject元数据访问组件或项目结果集合。
     */
    private final BioProjectCollectionMapper projects;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * integration：旧全量文献整合服务。
     */
    private final ResearchPublicationIntegrationService integration;
    /**
     * tx：事务执行模板，保证本批数据与进度一起提交或回滚。
     */
    private final TransactionTemplate tx;
    /**
     * metadata：来源元数据补全组件或规范化元数据结果。
     */
    private final PublicationMetadataEnricher metadata;
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final CollectionHttpTransport http = new CollectionHttpTransport();

    /**
     * 构造NativeRelatedPublicationWriter，注入本组件所需依赖并建立初始状态。
     * @param gsa GSA来源的持久化或元数据访问组件
     * @param projects BioProject元数据访问组件或项目结果集合
     * @param json JSON解析与序列化组件
     * @param integration 旧全量文献整合服务
     * @param tx 事务执行模板，保证本批数据与进度一起提交或回滚
     * @param resolver 当前选择的来源或资源解析器
     */
    public NativeRelatedPublicationWriter(GsaCollectionMapper gsa, BioProjectCollectionMapper projects, ObjectMapper json, ResearchPublicationIntegrationService integration, TransactionTemplate tx, BioProjectRelatedResourceResolver resolver) {
        this.gsa = gsa;
        this.projects = projects;
        this.json = json;
        this.integration = integration;
        this.tx = tx;
        metadata = new PublicationMetadataEnricher(http, json, resolver);
    }

    /**
     * 判断该来源是否具备无PMID出版记录的专用写入支持。
     * @param code 当前来源、策略或业务响应使用的编码
     * @return true表示满足上述条件，false表示不满足
     */
    public boolean supportsSource(String code) {
        return Set.of("GSA", "BIOPROJECT").contains(code);
    }

    /**
     * 来源唯一值的类型与内容组合，用于原库写入、查询和关联边定位。
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param value 待校验、转换或合并的值
     */
    public record Identifier(String type, String value) {
    }

    /**
     * 将无PMID的项目出版记录交给来源专用解析及写入流程，保持稳定业务键并标记为关联记录。
     * @param database 本次处理的数据库来源编码
     * @param reference 原始出版信息引用对象
     * @param delay 请求或处理间隔，具体单位见调用方法
     * @param stop 停止请求标记或用于检查停止状态的回调
     * @param rootPmid 当前根文献PMID
     * @return 按上述规则计算或读取的Identifier结果
     */
    public Identifier write(String database, JsonNode reference, long delay, BooleanSupplier stop, String rootPmid) throws Exception {
        if (!Set.of("GSA", "BIOPROJECT").contains(database))
            throw new IllegalStateException("无PMID项目文献的稳定键写入尚未适配来源：" + database);
        var seed = BioProjectPublicationParser.parse(reference, json);
        seed.setPrimary(false);
        seed.setRelated(true);
        // 已存在的论文无需再次请求上游，仍由调用方写入关系并按visited队列去重。
        if ("BIOPROJECT".equals(database)) {
            var existing = projects.existing(List.of(seed));
            if (!existing.isEmpty()) {
                var old = existing.get(0);
                if (rootPmid != null && rootPmid.equals(old.getPmid()))
                    return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
                projects.markRelated(old.getId(), GrantPayloadsHelper.merge(old.getGrantsJson(), seed.getGrantsJson(), json), ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), seed.getCncbBanksJson(), json));
                integration.syncSourceKeys(database, List.of(old.getRecordKey() != null ? old.getRecordKey() : "PMID:" + old.getPmid()));
                return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
            }
        } else {
            var g = new GsaPublicationSeed();
            BeanUtils.copyProperties(seed, g);
            var existing = gsa.existing(List.of(g));
            if (!existing.isEmpty()) {
                var old = existing.get(0);
                if (rootPmid != null && rootPmid.equals(old.getPmid()))
                    return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
                gsa.markRelated(old.getId(), GrantPayloadsHelper.merge(old.getGrantsJson(), seed.getGrantsJson(), json), ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), seed.getCncbBanksJson(), json));
                integration.syncSourceKeys(database, List.of(old.getRecordKey() != null ? old.getRecordKey() : "PMID:" + old.getPmid()));
                return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
            }
        }
        var task = new PublicationEnrichmentTask();
        BeanUtils.copyProperties(seed, task);
        task.setCncbBanksJson(seed.getCncbBanksJson());
        task.setGrantsJson(seed.getGrantsJson());
        http.configure(delay);
        var enriched = metadata.enrich(task, 3, stop, v -> {
        });
        if (rootPmid != null && rootPmid.equals(enriched.getPmid())) return new Identifier("PMID", rootPmid);
        return tx.execute(status -> {
            String key = seed.getRecordKey();
            if ("BIOPROJECT".equals(database)) {
                var existing = projects.existing(List.of(seed));
                var old = existing.isEmpty() ? null : existing.get(0);
                if (old != null) {
                    if (old.getRecordKey() != null) seed.setRecordKey(old.getRecordKey());
                    seed.setGrantsJson(GrantPayloadsHelper.merge(old.getGrantsJson(), enriched.getGrantsJson(), json));
                    seed.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), enriched.getCncbBanksJson(), json));
                } else {
                    seed.setGrantsJson(enriched.getGrantsJson());
                    seed.setCncbBanksJson(enriched.getCncbBanksJson());
                }
                seed.setPmid(enriched.getPmid());
                projects.upsertSeeds(List.of(seed));
                var stored = projects.existing(List.of(seed)).get(0);
                var write = new BioProjectEnrichmentWrite();
                BeanUtils.copyProperties(enriched, write);
                write.setId(stored.getId());
                write.setGrantsJson(seed.getGrantsJson());
                write.setCncbBanksJson(seed.getCncbBanksJson());
                write.setAuthorsJson(ResearchPublicationJsonMerger.mergeAuthors(stored.getAuthorsJson(), write.getAuthorsJson(), json));
                projects.updateMetadata(List.of(write));
                key = stored.getRecordKey() != null ? stored.getRecordKey() : "PMID:" + stored.getPmid();
            } else {
                var g = new GsaPublicationSeed();
                BeanUtils.copyProperties(seed, g);
                var existing = gsa.existing(List.of(g));
                var old = existing.isEmpty() ? null : existing.get(0);
                if (old != null) {
                    if (old.getRecordKey() != null) g.setRecordKey(old.getRecordKey());
                    g.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), enriched.getCncbBanksJson(), json));
                } else g.setCncbBanksJson(enriched.getCncbBanksJson());
                g.setPmid(enriched.getPmid());
                gsa.upsertSeeds(List.of(g));
                var stored = gsa.existing(List.of(g)).get(0);
                var write = new GsaEnrichmentWrite();
                BeanUtils.copyProperties(enriched, write);
                write.setId(stored.getId());
                write.setGrantsJson(GrantPayloadsHelper.merge(stored.getGrantsJson(), enriched.getGrantsJson(), json));
                write.setCncbBanksJson(g.getCncbBanksJson());
                write.setAuthorsJson(ResearchPublicationJsonMerger.mergeAuthors(stored.getAuthorsJson(), write.getAuthorsJson(), json));
                gsa.updateMetadata(List.of(write));
                key = stored.getRecordKey() != null ? stored.getRecordKey() : "PMID:" + stored.getPmid();
            }
            integration.syncSourceKeys(database, List.of(key));
            return new Identifier(enriched.getPmid() != null ? "PMID" : seed.getDoi() != null ? "DOI" : "SOURCE_KEY", enriched.getPmid() != null ? enriched.getPmid() : seed.getDoi() != null ? seed.getDoi() : key);
        });
    }

    /**
     * 为当前出版记录提取可用于入库及关联边的来源业务唯一值。
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     * @param doi DOI标识；匹配前按调用链统一规范化
     * @param key 当前对象的业务匹配键
     * @return 按上述规则计算或读取的Identifier结果
     */
    private static Identifier identifier(String pmid, String doi, String key) {
        return pmid != null ? new Identifier("PMID", pmid) : doi != null ? new Identifier("DOI", doi) : new Identifier("SOURCE_KEY", key);
    }

    /**
     * 原生出版记录写入中的资助载荷辅助器，复用统一的基金合并规则。
     */
    private static class GrantPayloadsHelper {
        /**
         * 合并当前业务对象的互补信息，按本类规则去重，避免用空字段覆盖已有有效值。
         * @param a 参与比较或合并的第一项
         * @param b 参与比较或合并的第二项
         * @param j 待读取的JSON节点
         * @return 按上述规则计算或读取的String结果
         */
        static String merge(String a, String b, ObjectMapper j) {
            return com.example.bioplatform.research.common.GrantPayloads.merge(a, b, j);
        }
    }
}
