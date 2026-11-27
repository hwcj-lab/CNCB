package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.research.collection.common.dto.IntegratedPublicationRecord;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;

import java.util.*;

/**
 * 只解析当前有变化批次的标识；新增PMID时保留原整合键和历史穿透引用。
 */
final class IntegrationIdentityResolver {
    /**
     * 旧全量整合的身份匹配分组，保留记录键以兼容历史引用。
     * @param key 当前对象的业务匹配键
     * @param rows 本批待处理记录
     * @param previous 此前有效的记录版本
     */
    record Group(String key, List<SourcePublicationRecord> rows, List<IntegratedPublicationRecord> previous) {
    }

    /**
     * 去除DOI访问地址前缀并规范大小写，用于已有标识的匹配。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    static String doi(String value) {
        return value == null ? "" : com.example.bioplatform.research.common.PublicationValues.normalizeDoi(value);
    }

    /**
     * 构造不依赖内部自增主键的文献业务身份，使用已有标识参与合并。
     * @param row 本次处理的一条记录
     * @return 按上述规则计算或读取的String结果
     */
    static String natural(SourcePublicationRecord row) {
        if ("RESOURCE".equals(row.getRecordKind())) return row.getSourceCode() + ":" + row.getSourceRecordKey();
        return present(row.getPmid()) ? "PMID:" + row.getPmid() : present(row.getDoi()) ? "DOI:" + doi(row.getDoi()) : present(row.getSourceRecordKey()) ? row.getSourceCode() + ":" + row.getSourceRecordKey() : row.getSourceCode() + ":UNKEYED:" + com.example.bioplatform.research.common.PublicationValues.sha(row.getTitle() == null ? "" : row.getTitle());
    }

    /**
     * 收集当前记录可用于身份匹配的业务键集合。
     * @param rows 本批待处理记录
     * @return 按上述规则计算或读取的Set<String>结果
     */
    static Set<String> keys(List<SourcePublicationRecord> rows) {
        Set<String> result = new LinkedHashSet<>();
        for (var row : rows) {
            result.add(natural(row));
            if (present(row.getSourceRecordKey())) result.add(row.getSourceCode() + ":" + row.getSourceRecordKey());
            if (present(row.getDoi())) result.add("DOI:" + doi(row.getDoi()));
        }
        return result;
    }

    /**
     * 把原始标识解析成规范的类型与值，或建立当前上下文的身份对应关系。
     * @param rows 本批待处理记录
     * @param existing 数据库或目标对象中已存在的内容
     * @return 按上述规则计算或读取的List<Group>结果
     */
    static List<Group> resolve(List<SourcePublicationRecord> rows, List<IntegratedPublicationRecord> existing) {
        Map<String, String> doiPmids = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (var row : rows)
            if (present(row.getDoi()) && present(row.getPmid())) {
                String key = doi(row.getDoi()), old = doiPmids.putIfAbsent(key, row.getPmid());
                if (old != null && !old.equals(row.getPmid())) ambiguous.add(key);
            }
        Map<String, List<SourcePublicationRecord>> groups = new LinkedHashMap<>();
        for (var row : rows) {
            String key = natural(row);
            if (!present(row.getPmid()) && present(row.getDoi()) && !ambiguous.contains(doi(row.getDoi())) && doiPmids.containsKey(doi(row.getDoi())))
                key = "PMID:" + doiPmids.get(doi(row.getDoi()));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        List<Group> result = new ArrayList<>();
        Set<String> claimed = new HashSet<>();
        for (var group : groups.entrySet()) {
            var sourceRows = group.getValue();
            Set<String> aliases = keys(sourceRows);
            Set<String> ids = new HashSet<>(), dois = new HashSet<>();
            sourceRows.forEach(row -> {
                if (present(row.getPmid())) ids.add(row.getPmid());
                if (present(row.getDoi())) dois.add(doi(row.getDoi()));
            });
            var matches = existing.stream().filter(old -> {
                if (claimed.contains(old.getPublicationKey())) return false;
                if (present(old.getPmid()) && !ids.isEmpty() && !ids.contains(old.getPmid())) return false;
                return aliases.contains(old.getPublicationKey()) || present(old.getPmid()) && ids.contains(old.getPmid()) || !ambiguous.contains(doi(old.getDoi())) && present(old.getDoi()) && dois.contains(doi(old.getDoi()));
            }).sorted(Comparator.comparing(IntegratedPublicationRecord::getId)).toList();
            String key = matches.isEmpty() ? group.getKey() : matches.get(0).getPublicationKey();
            claimed.add(key);
            matches.forEach(old -> claimed.add(old.getPublicationKey()));
            result.add(new Group(key, List.copyOf(sourceRows), matches));
        }
        return result;
    }

    /**
     * 判断值或来源身份是否存在且有效，避免把空字符串或字面null当成有效信息。
     * @param value 待校验、转换或合并的值
     * @return true表示满足上述条件，false表示不满足
     */
    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
