package com.example.bioplatform.research.collection.openlb.service.relation;
import com.example.bioplatform.research.collection.common.service.impl.PubMedXmlMetadataParser;
import com.example.bioplatform.research.collection.common.service.relation.RelationPageClient;
import com.example.bioplatform.research.collection.common.service.impl.PubMedXmlMetadataParser;import com.fasterxml.jackson.databind.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.beans.factory.annotation.Value;import java.io.IOException;
/** 只读取已有PMID的正文元数据；不搜索、不补写缺失PMID，也不负责递归关联。 */
@Component @RequiredArgsConstructor public class OpenLbPublicationDetailsClient {
 private final RelationPageClient pages;private final ObjectMapper json;
 @Value("${app.openlb.endpoint:https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=}") private String endpoint;
 public JsonNode read(String pmid)throws Exception {
  var root=json.readTree(pages.get(endpoint+pmid));if(!"200".equals(root.path("code").asText()))throw new IOException("OpenLB code "+root.path("code").asText());
  for(JsonNode candidate:root.path("result").path("data"))if(pmid.equals(candidate.path("pmid").asText()))return candidate;
  var fallback=PubMedXmlMetadataParser.parse(pages.get("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id="+pmid),pmid,json);
  if(fallback==null)throw new IOException("OpenLB及PubMed均未返回PMID "+pmid);return fallback;
 }
}
