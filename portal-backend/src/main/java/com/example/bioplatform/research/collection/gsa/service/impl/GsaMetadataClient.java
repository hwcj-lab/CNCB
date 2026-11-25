package com.example.bioplatform.research.collection.gsa.service.impl;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.example.bioplatform.research.collection.gsa.dto.GsaCollectionStartRequest;
import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentTask;
import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentWrite;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.impl.PublicationMetadataEnricher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;import org.springframework.beans.BeanUtils;import java.util.function.*;
/** GSA采集专用调用门面，实际论文元数据补全由公共组件完成。 */
@Component public class GsaMetadataClient {
 private final PublicationMetadataEnricher delegate;
 public GsaMetadataClient(CollectionHttpTransport http,ObjectMapper json,BioProjectRelatedResourceResolver projects){delegate=new PublicationMetadataEnricher(http,json,projects);}
 public GsaEnrichmentWrite enrich(GsaEnrichmentTask task,GsaCollectionStartRequest config,BooleanSupplier stop,Consumer<String> progress)throws Exception {
  GsaEnrichmentWrite result=new GsaEnrichmentWrite();BeanUtils.copyProperties(delegate.enrich(task,config.attempts(),stop,progress),result);return result;
 }
 public static GsaEnrichmentWrite fromTask(GsaEnrichmentTask task){var result=new GsaEnrichmentWrite();BeanUtils.copyProperties(PublicationMetadataEnricher.fromTask(task),result);return result;}
}
