package com.example.bioplatform.research.collection.common.service;
import com.example.bioplatform.research.collection.ascancer.service.impl.AscancerRelatedResourceResolver;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaRelatedResourceResolver;
import com.example.bioplatform.research.collection.gwh.service.impl.GwhRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.impl.RelatedResourceHttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 用单条官方响应结构验证资源解析；不访问网络、不采集文献、不写数据库。 */
class RelatedResourceResolverTest {
    private final ObjectMapper json=new ObjectMapper();
    private static class StubHttp extends RelatedResourceHttpClient {
        final Map<String,String> responses=new HashMap<>();final List<String> urls=new ArrayList<>();
        @Override public synchronized String get(String url)throws Exception {urls.add(url);String value=responses.get(url);if(value==null)throw new IOException("资源接口HTTP 404："+url);return value;}
    }
    @Test void projectReadsPublicationIdsOnlyAndKeepsGrants()throws Exception {
        var http=new StubHttp();http.responses.put("https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000091",
            "{\"message\":\"SUCCESS\",\"listPublication\":[{\"pubmedId\":\"26561581\"},{\"pubmedId\":\"31599941\"},{\"pubmedId\":\"26561581\"}],\"listGrants\":[{\"agency\":\"NSFC\",\"grantID\":\"12345678\"}]}");
        var result=new BioProjectRelatedResourceResolver(http,json).findRelatedPublications(null,"https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA000091");
        assertEquals(List.of("26561581","31599941"),result.pmids());assertEquals("12345678",result.grants().get(0).path("grantID").asText());
        assertFalse(result.pmids().contains("12345678"));
    }
    @Test void gsaResolvesProjectFromSearchPage()throws Exception {
        var http=new StubHttp();http.responses.put("https://ngdc.cncb.ac.cn/gsa/search?searchTerm=CRA015930","<table><tr><td>CRA015930</td><td><a href='https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA025130'>PRJCA025130</a></td></tr></table>");
        http.responses.put("https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA025130","{\"message\":\"SUCCESS\",\"listPublication\":[{\"pubmedId\":\"41028458\"}],\"listGrants\":[]}");
        var result=new GsaRelatedResourceResolver(http,new BioProjectRelatedResourceResolver(http,json),json).findRelatedPublications("CRA015930",null);
        assertEquals(List.of("41028458"),result.pmids());
    }
    @Test void gwhRecognizesOpenLbPublicationLinkAndExcludesFooter()throws Exception {
        var http=new StubHttp();http.responses.put("https://ngdc.cncb.ac.cn/gwh/Assembly/104189/show","<dt>Publication(s)</dt><dd><a href='https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-21515709'>paper</a></dd><footer><a href='https://pubmed.ncbi.nlm.nih.gov/39977364/'>site citation</a></footer>");
        var result=new GwhRelatedResourceResolver(http,new BioProjectRelatedResourceResolver(http,json),json).findRelatedPublications(null,"https://ngdc.cncb.ac.cn/gwh/Assembly/104189/show");
        assertEquals(List.of("21515709"),result.pmids());
    }
    @Test void ascancerEncodesPositiveStrandAndReadsNamedPmidField()throws Exception {
        var http=new StubHttp();String endpoint="https://ngdc.cncb.ac.cn/ascancer/api/browse/knowledge";
        String url=endpoint+"?eventid=RAC1_chr7_%2B_ES_1%3A2&page=0&size=100";
        http.responses.put(url,"{\"items\":[{\"pubmedid\":\"41028458\",\"position\":6431555}]}");
        var resolver=new AscancerRelatedResourceResolver(http,json);ReflectionTestUtils.setField(resolver,"endpoint",endpoint);
        var result=resolver.findRelatedPublications(null,"https://ngdc.cncb.ac.cn/ascancer/browse/knowledge/details?eventid=RAC1_chr7_+_ES_1:2");
        assertEquals(List.of("41028458"),result.pmids());assertEquals(url,http.urls.get(0));
    }
    @Test void upstreamFailureIsNotAnEmptySuccessfulResult() {
        var http=new StubHttp();var resolver=new AscancerRelatedResourceResolver(http,json);
        ReflectionTestUtils.setField(resolver,"endpoint","https://ngdc.cncb.ac.cn/ascancer/api/browse/knowledge");
        assertThrows(IOException.class,()->resolver.findRelatedPublications("event",null));
    }
    @Test void gsaUnknownPageIsNotAnAssociationTerminal() {
        var http=new StubHttp();http.responses.put("https://ngdc.cncb.ac.cn/gsa/search?searchTerm=CRA015930","<html>CRA015930 No results</html>");
        assertThrows(IllegalStateException.class,()->new GsaRelatedResourceResolver(http,new BioProjectRelatedResourceResolver(http,json),json).findRelatedPublications("CRA015930",null));
    }
}
