package com.example.bioplatform.research.integration.service.impl;
import com.example.bioplatform.research.collection.common.dto.IntegratedPublicationRecord;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.integration.service.impl.IntegrationIdentityResolver;
import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class IntegrationIdentityResolverTest {
 private SourcePublicationRecord row(String source,String key,String pmid,String doi){var r=new SourcePublicationRecord();r.setSourceCode(source);r.setSourceRecordKey(key);r.setPmid(pmid);r.setDoi(doi);return r;}
 private IntegratedPublicationRecord old(long id,String key,String pmid,String doi){var r=new IntegratedPublicationRecord();r.setId(id);r.setPublicationKey(key);r.setPmid(pmid);r.setDoi(doi);return r;}
 @Test void newlyResolvedPmidKeepsOriginalGenBasePaperKey(){var groups=IntegrationIdentityResolver.resolve(List.of(row("GENBASE","PAPER:11","41769996",null)),List.of(old(1,"GENBASE:PAPER:11",null,null)));assertEquals(1,groups.size());assertEquals("GENBASE:PAPER:11",groups.get(0).key());}
 @Test void doiOnlyAndPmidRowsForSameDoiBecomeOneGroup(){var groups=IntegrationIdentityResolver.resolve(List.of(row("GENBASE","PAPER:11","41769996","10.1234/a"),row("GSA","DOI:10.1234/a",null,"10.1234/a")),List.of(old(2,"DOI:10.1234/a",null,"10.1234/a"),old(3,"PMID:41769996","41769996","10.1234/a")));assertEquals(1,groups.size());assertEquals(2,groups.get(0).rows().size());assertEquals("DOI:10.1234/a",groups.get(0).key());assertEquals(2,groups.get(0).previous().size());}
 @Test void repeatedSyncReusesCustomStableKeyByPmid(){var groups=IntegrationIdentityResolver.resolve(List.of(row("GSA","PMID:41769996","41769996",null)),List.of(old(1,"GENBASE:PAPER:11","41769996",null)));assertEquals("GENBASE:PAPER:11",groups.get(0).key());}
 @Test void conflictingPmidsSharingDoiAreNotSilentlyMerged(){var groups=IntegrationIdentityResolver.resolve(List.of(row("A","PMID:11111111","11111111","10.1234/a"),row("B","PMID:22222222","22222222","10.1234/a")),List.of());assertEquals(2,groups.size());}
}
