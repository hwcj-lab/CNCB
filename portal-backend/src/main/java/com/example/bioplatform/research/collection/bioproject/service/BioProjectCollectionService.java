package com.example.bioplatform.research.collection.bioproject.service;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;import java.util.Map;
/** BIOPROJECT主文献采集与补全；关联采集由现有公共关联接口控制。 */
public interface BioProjectCollectionService {
 Map<String,Object> start(BioProjectCollectionStartRequest request,boolean restart);
 Map<String,Object> stop();Map<String,Object> status();Map<String,Object> resetEnrichment();
}
