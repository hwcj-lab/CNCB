package com.example.bioplatform.research.collection.openlb.service.impl;

import com.example.bioplatform.research.collection.openlb.dto.OpenLbPublicationWriteRow;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbCollectionMapper;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;


import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 按银行名称分流；未知目标或空银行统一保存到OpenLB表，配置后通过本地分流接口重试。
 */
@Service
@RequiredArgsConstructor
public class OpenLbRoutingService {
    private final OpenLbRoutingMapper mapper;
    private final OpenLbCollectionMapper fallback;
    private final ObjectMapper json;

    /**
     * 分批迁移既有OpenLB文献，返回下一批游标；原始归档保留。
     */
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> redistribute(long after) {
        var rows = mapper.legacy(after);
        route(rows);
        return Map.of("processed", rows.size(), "nextAfterId", rows.isEmpty() ? after : rows.get(rows.size() - 1).getId(), "pmids", rows.stream().map(OpenLbPublicationWriteRow::getPmid).toList());
    }

    public void route(List<OpenLbPublicationWriteRow> rows) {
        Map<String, String> targets = new HashMap<>();
        for (var route : mapper.routes())
            targets.put(route.get("bank_db").toString().trim().toLowerCase(Locale.ROOT), route.get("source_code").toString());
        Map<String, List<OpenLbPublicationWriteRow>> batches = new LinkedHashMap<>();
        Map<String, OpenLbPublicationWriteRow> fallbackRows = new LinkedHashMap<>();
        for (var row : rows)
            try {
                JsonNode banks = json.readTree(Optional.ofNullable(row.getCncbBanksJson()).orElse("null"));
                if (banks == null || !banks.isArray() || banks.isEmpty()) {
                    fallbackRows.put(row.getPmid(), row);
                    continue;
                }
                Set<String> seen = new HashSet<>();
                for (var bank : banks) {
                    String db = bank.path("db").asText("").trim();
                    String code = targets.get(db.toLowerCase(Locale.ROOT));
                    if (code == null) {
                        fallbackRows.put(row.getPmid(), row);
                        continue;
                    }
                    if (seen.add(code)) batches.computeIfAbsent(code, k -> new ArrayList<>()).add(row);
                }
            } catch (Exception e) {
                throw new IllegalStateException("OpenLB分流失败 PMID=" + row.getPmid(), e);
            }
        if (!fallbackRows.isEmpty()) fallback.insertPublicationsIgnore(new ArrayList<>(fallbackRows.values()));
        batches.forEach((code, batch) -> {
            if ("GENBASE".equals(code)) mapper.writeGenBase(batch);
            else mapper.write(table(code), batch);
            mapper.copyRelations(code, batch.stream().map(OpenLbPublicationWriteRow::getPmid).toList());
        });
    }

    /**
     * 表名只由后台配置的严格字母数字编码生成，禁止请求参数进入SQL标识符。
     */
    public static String table(String code) {
        if (code == null || !code.matches("[A-Z][A-Z0-9_]{0,39}") || code.equals("OPENLB"))
            throw new IllegalArgumentException("无效目标库编码");
        return code.toLowerCase(Locale.ROOT) + "_publication";
    }
}
