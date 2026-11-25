package com.example.bioplatform.research.collection.common.service.relation;

import java.util.Map;

/**
 * 每个原库自己的业务唯一值解释器。
 */
public interface RelationIdentifierResolver {
    record Identifier(String type, String value) {
    }

    boolean supports(String database);

    int priority();

    Identifier resolve(String database, Map<String, String> identifiers);
}
