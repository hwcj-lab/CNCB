package com.example.bioplatform.research.collection.gsa.service;

import com.example.bioplatform.research.collection.gsa.dto.GsaCollectionStartRequest;

import java.util.Map;

/**
 * GSA主文献采集与补全；关联采集由现有公共关联接口控制。
 */
public interface GsaCollectionService {
    Map<String, Object> start(GsaCollectionStartRequest request, boolean restart);

    Map<String, Object> stop();

    Map<String, Object> status();

    Map<String, Object> resetEnrichment();
}
