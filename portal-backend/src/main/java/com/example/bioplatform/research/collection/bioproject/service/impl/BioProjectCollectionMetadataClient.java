package com.example.bioplatform.research.collection.bioproject.service.impl;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentTask;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentWrite;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.example.bioplatform.research.collection.common.service.impl.PublicationMetadataEnricher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;import org.springframework.beans.BeanUtils;import java.util.function.*;
@Component public class BioProjectCollectionMetadataClient {
 private final CollectionHttpTransport http=new CollectionHttpTransport();private final PublicationMetadataEnricher delegate;
 public BioProjectCollectionMetadataClient(ObjectMapper json,BioProjectRelatedResourceResolver projects){delegate=new PublicationMetadataEnricher(http,json,projects);}
 public BioProjectEnrichmentWrite enrich(BioProjectEnrichmentTask task,BioProjectCollectionStartRequest config,BooleanSupplier stop,Consumer<String> progress)throws Exception {
  http.configure(config.delay());var result=new BioProjectEnrichmentWrite();BeanUtils.copyProperties(delegate.enrich(task,config.attempts(),stop,progress),result);return result;
 }
 public static BioProjectEnrichmentWrite fromTask(BioProjectEnrichmentTask task){var result=new BioProjectEnrichmentWrite();BeanUtils.copyProperties(PublicationMetadataEnricher.fromTask(task),result);return result;}
}
