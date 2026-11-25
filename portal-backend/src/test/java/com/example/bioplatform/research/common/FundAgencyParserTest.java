package com.example.bioplatform.research.common;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 验证基金统计仅使用agency或编号字典全称，不重复计算缩写和项目。 */
class FundAgencyParserTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void agencyWinsOverAcronymAndDictionary() throws Exception {
        var grant=json.readTree("{\"agency\":\"NIGMS NIH HHS\",\"acronym\":\"GM\",\"grantId\":\"R01 GM054762\"}");
        assertEquals("NIGMS NIH HHS",FundAgencyParser.resolve(grant,Map.of("r01 gm054762","Dictionary name")));
    }
    @Test void nullAgencyLooksUpGrantId() throws Exception {
        assertEquals("Known fund",FundAgencyParser.resolve(json.readTree("{\"agency\":null,\"grantId\":\" ABC-123 \"}"),Map.of("abc-123","Known fund")));
    }
    @Test void idAliasCanResolveName() throws Exception {
        assertEquals("Known fund",FundAgencyParser.resolve(json.readTree("{\"id\":12345}"),Map.of("12345","Known fund")));
    }
    @Test void bioProjectGrantIdAliasIsSupported() throws Exception {
        assertEquals("Known fund",FundAgencyParser.resolve(json.readTree("{\"agency\":\"\",\"grantId\":\"\",\"grantID\":\"ABC\"}"),Map.of("abc","Known fund")));
    }
    @Test void unmatchedIdDoesNotBecomeNumberOrAcronym() throws Exception {
        assertEquals("",FundAgencyParser.resolve(json.readTree("{\"agency\":null,\"grantId\":\"missing\",\"acronym\":\"GM\",\"program\":\"A program\"}"),Map.of()));
    }
    @Test void missingIdentifiersAndLegacyArraysAreIgnored() throws Exception {
        assertEquals("",FundAgencyParser.resolve(json.readTree("{\"acronym\":\"GM\",\"legacyFundLevel1\":[\"old\"]}"),Map.of()));
    }
    @Test void nullTextAndBlankDictionaryNamesAreMissing() throws Exception {
        var grant=json.readTree("{\"agency\":\" null \",\"id\":\"123\"}");
        assertEquals("",FundAgencyParser.resolve(grant,Map.of("123","  ")));
        assertEquals("",FundAgencyParser.resolve(grant,Map.of("123","null")));
    }
    @Test void repeatedAgencyAndDictionaryMatchCountOncePerPublication() throws Exception {
        var grants=json.readTree("[{\"agency\":\" Same   agency \"},{\"agency\":\"Same agency\"},{\"id\":\"ABC\"}]");
        var names=new HashSet<String>();
        grants.forEach(grant->names.add(FundAgencyParser.resolve(grant,Map.of("abc","Same agency"))));
        assertEquals(Set.of("Same agency"),names);
    }
}
