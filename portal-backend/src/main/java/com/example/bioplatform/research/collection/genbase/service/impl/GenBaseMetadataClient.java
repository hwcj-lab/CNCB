package com.example.bioplatform.research.collection.genbase.service.impl;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectDocumentParser;
import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentTask;
import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentWrite;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.example.bioplatform.research.collection.common.service.impl.PublicationMetadataEnricher;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseOpenLbWriteRow;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePmidTask;
import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * GenBase元数据读取：已有PMID详情、项目内精确匹配；缺失PMID不推断、不补写；不把提交单位作为作者署名。
 */
@Component
public class GenBaseMetadataClient {
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final CollectionHttpTransport http;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * metadata：来源元数据补全组件或规范化元数据结果。
     */
    private final PublicationMetadataEnricher metadata;

    /**
     * 构造GenBaseMetadataClient，注入本组件所需依赖并建立初始状态。
     * @param json JSON解析与序列化组件
     */
    @org.springframework.beans.factory.annotation.Autowired
    public GenBaseMetadataClient(ObjectMapper json) {
        this(new CollectionHttpTransport(), json);
    }

    /**
     * 构造GenBaseMetadataClient，注入本组件所需依赖并建立初始状态。
     * @param http 共享HTTP传输组件或HTTP客户端
     * @param json JSON解析与序列化组件
     */
    GenBaseMetadataClient(CollectionHttpTransport http, ObjectMapper json) {
        this.http = http;
        this.json = json;
        this.metadata = new PublicationMetadataEnricher(http, json, null);
    }

    /**
     * 基于已有标识补充元数据并保留原有有效字段；具体网络读取和合并规则由本来源及公共组件共同处理。
     */
    public GenBaseOpenLbWriteRow enrich(GenBasePmidTask original, long delay, BooleanSupplier stopped, Consumer<String> progress) throws Exception {
        http.configure(delay);
        PublicationEnrichmentTask task = new PublicationEnrichmentTask();
        BeanUtils.copyProperties(original, task);
        // 项目请求由下方精确匹配控制，避免公共补全在匹配前使用项目的其他论文。
        task.setBioprojectAccession(null);
        PublicationEnrichmentWrite out = metadata.enrich(task, 1, stopped, progress);
        validateIdentifiers(task, out);
        String resolutionMethod = !blank(original.getPmid()) ? "EXISTING_PMID" : "NO_SOURCE_PMID";
        ArrayNode projectEvidence = json.createArrayNode();
        boolean needsProject = blank(out.getPmid()) || "EMPTY".equals(out.getStatus()) || emptyArray(out.getGrantsJson());
        if (needsProject && !blank(original.getBioprojectAccession())) {
            Set<String> codes = new LinkedHashSet<>(Arrays.asList(original.getBioprojectAccession().split("[,;\\s]+")));
            for (String code : codes) {
                if (stopped.getAsBoolean()) throw new InterruptedException("GenBase补全已停止");
                if (!code.matches("PRJ(?:CA|NA|EB|DA)[A-Z0-9]+")) continue;
                JsonNode project = BioProjectDocumentParser.fetch(code, json, url -> http.text(url, 1, stopped, progress));
                PublicationEnrichmentTask matchTask = new PublicationEnrichmentTask();
                BeanUtils.copyProperties(out, matchTask);
                JsonNode match = matchPublication(project.path("listPublication"), matchTask);
                // 同一项目可能有多篇文章；不拿无关论文的PMID、作者或基金覆盖当前记录。
                if (match == null) continue;
                if (blank(out.getTitle()))
                    out.setTitle(com.example.bioplatform.research.common.PublicationValues.text(match, "articleTitle"));
                if (blank(out.getJournalTitle()))
                    out.setJournalTitle(com.example.bioplatform.research.common.PublicationValues.text(match, "journalTitle"));
                int year = match.path("year").asInt();
                if (out.getPubYear() == null && year >= 1500 && year <= 2200) out.setPubYear(year);
                if (project.path("listGrants").isArray())
                    out.setGrantsJson(GrantPayloads.merge(out.getGrantsJson(), project.path("listGrants").toString(), json));
                ArrayNode banks = json.createArrayNode();
                banks.addObject().put("db", "BioProject").put("accession", code).put("link", "https://ngdc.cncb.ac.cn/bioproject/browse/" + code);
                out.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(out.getCncbBanksJson(), banks.toString(), json));
                projectEvidence.addObject().put("accession", code).put("projectOrganization", project.path("submitter").path("organization").asText(""))
                        .set("publication", match.deepCopy());
                // 仅采用匹配到的原始元数据，不使用其他页面的PMID补写本来源。
                if (match.path("authors").isArray())
                    out.setAuthorsJson(ResearchPublicationJsonMerger.mergeAuthors(out.getAuthorsJson(), match.path("authors").toString(), json));
                if (blank(original.getPmid())) resolutionMethod = "BIOPROJECT_METADATA";
            }
        }
        ObjectNode raw = json.createObjectNode();
        if (!blank(out.getRawPayload())) raw.set("publication", json.readTree(out.getRawPayload()));
        if (!projectEvidence.isEmpty()) raw.set("bioProjects", projectEvidence);
        raw.putObject("identifierResolution").put("method", resolutionMethod).put("originalPmid", original.getPmid())
                .put("resolvedPmid", out.getPmid()).put("originalDoi", original.getDoi()).put("resolvedDoi", out.getDoi());
        out.setRawPayload(raw.toString());
        out.setPmid(original.getPmid());
        out.setDoi(original.getDoi());
        if (blank(out.getPmid())) out.setStatus("NO_PMID");
        // 哈希只描述论文业务内容，不包含状态、时间、任务ID；GenBase列表自身source_hash保持原语义。
        out.setSourceHash(com.example.bioplatform.research.common.PublicationValues.sha(String.join("|", safe(out.getPmid()), safe(out.getDoi()), safe(out.getAuthorsJson()), safe(out.getGrantsJson()), safe(out.getCncbBanksJson()))));
        GenBaseOpenLbWriteRow result = new GenBaseOpenLbWriteRow();
        BeanUtils.copyProperties(out, result);
        result.setPaperId(original.getPaperId());
        return result;
    }

    /**
     * PMID优先，其次精确DOI，最后严格规范化标题；多篇冲突不猜测，不按项目仅一篇就强行匹配。
     */
    static JsonNode matchPublication(JsonNode papers, PublicationEnrichmentTask task) {
        if (!papers.isArray()) return null;
        List<JsonNode> matches = new ArrayList<>();
        int best = 0;
        for (JsonNode paper : papers) {
            String pmid = com.example.bioplatform.research.common.PublicationValues.text(paper, "pubmedId"), doi = com.example.bioplatform.research.common.PublicationValues.normalizeDoi(com.example.bioplatform.research.common.PublicationValues.text(paper, "doi"));
            if (!blank(task.getPmid()) && !pmid.isBlank() && !task.getPmid().equals(pmid)) continue;
            if (!blank(task.getDoi()) && !doi.isBlank() && !com.example.bioplatform.research.common.PublicationValues.normalizeDoi(task.getDoi()).equals(doi))
                continue;
            int score = 0;
            if (!blank(task.getPmid()) && task.getPmid().equals(pmid)) score = 3;
            else if (!blank(task.getDoi()) && com.example.bioplatform.research.common.PublicationValues.normalizeDoi(task.getDoi()).equals(doi))
                score = 2;
            else if (!normalizedTitle(task.getTitle()).isBlank() && normalizedTitle(task.getTitle()).equals(normalizedTitle(com.example.bioplatform.research.common.PublicationValues.text(paper, "articleTitle"))))
                score = 1;
            if (score > best) {
                best = score;
                matches.clear();
            }
            if (score > 0 && score == best) matches.add(paper);
        }
        if (matches.isEmpty()) return null;
        // 重复项必须在标识和标题上都一致，否则将该项目视为歧义。
        JsonNode first = matches.get(0);
        for (JsonNode candidate : matches)
            if (!com.example.bioplatform.research.common.PublicationValues.text(first, "pubmedId").equals(com.example.bioplatform.research.common.PublicationValues.text(candidate, "pubmedId")) || !com.example.bioplatform.research.common.PublicationValues.normalizeDoi(com.example.bioplatform.research.common.PublicationValues.text(first, "doi")).equals(com.example.bioplatform.research.common.PublicationValues.normalizeDoi(com.example.bioplatform.research.common.PublicationValues.text(candidate, "doi"))))
                return null;
        return first;
    }

    /**
     * 核对来源已提供的PMID、DOI及项目标识，不通过标题搜索生成缺失PMID。
     * @param task 当前待处理的元数据任务
     * @param out 用于累计解析结果的输出对象
     */
    private static void validateIdentifiers(PublicationEnrichmentTask task, PublicationEnrichmentWrite out) {
        if (!blank(task.getPmid()) && !blank(out.getPmid()) && !task.getPmid().equals(out.getPmid()))
            throw new IllegalStateException("补全PMID与原记录不一致，保留原文献等待核对");
        if (!blank(task.getDoi()) && !blank(out.getDoi()) && !com.example.bioplatform.research.common.PublicationValues.normalizeDoi(task.getDoi()).equals(com.example.bioplatform.research.common.PublicationValues.normalizeDoi(out.getDoi())))
            throw new IllegalStateException("补全DOI与原记录不一致，保留原文献等待核对");
    }

    /**
     * 整理标题大小写、标记和空白，仅供已有候选范围内比较。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private static String normalizedTitle(String value) {
        return safe(value).replaceAll("<[^>]+>", " ").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /**
     * 生成本次调用需要的空JSON数组，避免用null代替约定的集合结构。
     * @param value 待校验、转换或合并的值
     * @return true表示满足上述条件，false表示不满足
     */
    private boolean emptyArray(String value) throws Exception {
        if (blank(value)) return true;
        JsonNode node = json.readTree(value);
        return node == null || node.isNull() || node.isArray() && node.isEmpty();
    }

    /**
     * 判断文本是否为空或仅有空白，避免空数据覆盖原有有效内容。
     * @param value 待校验、转换或合并的值
     * @return true表示满足上述条件，false表示不满足
     */
    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 将可空字段转换为当前调用约定的安全值，避免空引用中断批处理。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
