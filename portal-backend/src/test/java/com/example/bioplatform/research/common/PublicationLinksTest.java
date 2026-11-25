package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.ObjectMapper;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PublicationLinksTest {
 private final ObjectMapper json=new ObjectMapper();
 @Test void paperIdentifiersHavePriorityOverResourceIdentifiers(){assertEquals("https://pubmed.ncbi.nlm.nih.gov/41769996/",PublicationLinks.external("41769996","10.1128/example","CRA042678","PRJCA063830",null,json));assertEquals("https://doi.org/10.1128/example",PublicationLinks.external(null,"10.1128/example","CRA042678","PRJCA063830",null,json));}
 @Test void explicitAccessionsSupportBothResourceTypes(){assertEquals("https://ngdc.cncb.ac.cn/gsa/browse/CRA042678",PublicationLinks.external(null,null,"CRA042678",null,null,json));assertEquals("https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA063830",PublicationLinks.external(null,null,null,"PRJCA063830",null,json));}
 @Test void existingBanksAllowLinksWithoutBackfill(){assertEquals("https://ngdc.cncb.ac.cn/gsa/browse/CRA042678",PublicationLinks.external(null,null,null,null,"[{\"db\":\"GSA\",\"link\":\"https://ngdc.cncb.ac.cn/gsa/search?searchTerm=CRA042678\"}]",json));assertEquals("https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA063830",PublicationLinks.external(null,null,null,null,"[{\"accession\":\"PRJCA063830\"}]",json));}
 @Test void unrelatedOrMissingLinksAreNotFabricated(){assertNull(PublicationLinks.external(null,null,null,null,"[{\"accession\":\"HRA123\"}]",json));assertNull(PublicationLinks.external(null,"javascript:alert(1)",null,null,"invalid",json));}
}
