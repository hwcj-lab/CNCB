package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.ObjectMapper;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class PublicationMetadataProcessorTest {
 private final ObjectMapper json=new ObjectMapper();private final PublicationMetadataProcessor processor=new PublicationMetadataProcessor(json);
 private PublicationMetadataProcessor.Input input(String authors,String affiliations,String grants){return new PublicationMetadataProcessor.Input(authors,affiliations,grants,null,null,null);}
 @Test void missingAuthorInheritsOnlyOneCommonInstitutionAndKeepsFirstLastNameOrder(){
  String authors="[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"Department of Biology, University A, City, China\"},{\"firstName\":\"Yan\",\"lastName\":\"Wang\",\"affiliation\":\"\"}]";
  var out=processor.parse(input(authors,null,"[]"),Map.of());assertEquals(Set.of(new PublicationMetadataProcessor.AuthorIdentity("Wei Li","University A"),new PublicationMetadataProcessor.AuthorIdentity("Yan Wang","University A")),out.authors());assertEquals(Set.of("University A"),out.institutions());assertThrows(UnsupportedOperationException.class,()->out.authors().clear());
 }
 @Test void differentAuthorInstitutionsKeepMissingAuthorUnknownAndNeverUseProjectOrganization(){
  String authors="[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"School, University A, City\"},{\"firstName\":\"Yan\",\"lastName\":\"Wang\",\"affiliation\":\"School, University B, City\"},{\"firstName\":\"Ming\",\"lastName\":\"Zhang\"}]";
  var out=processor.parse(input(authors,"Project Submitter Organization","[]"),Map.of());assertTrue(out.authors().contains(new PublicationMetadataProcessor.AuthorIdentity("Ming Zhang",null)));assertFalse(out.authors().stream().anyMatch(a->"Project Submitter Organization".equals(a.institution())));
 }
 @Test void agencyDictionaryAndRawMergeHaveSameStableContract()throws Exception{
  var out=processor.parse(input("[]",null,"[{\"agency\":\"Agency A\",\"acronym\":\"GM\"},{\"grantId\":\"G2\"},{\"grantId\":\"unknown\"}]"),Map.of("g2","Full Agency B"));assertEquals(Set.of("Agency A","Full Agency B"),out.funds());
  var before=input("[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"University A\"}]",null,"[{\"grantId\":\"G1\",\"agency\":\"Keep\"}]");var after=input("[{\"firstName\":\"Wei\",\"lastName\":\"Li\"}]",null,"[{\"grantId\":\"G1\",\"agency\":\"New\"}]");var merged=processor.merge(before,after);assertEquals("Keep",json.readTree(merged.grantsJson()).get(0).path("agency").asText());assertTrue(merged.authorsJson().contains("University A"));assertEquals(1,json.readTree(merged.grantsJson()).size());
 }
 @Test void malformedAndMissingFieldsRemainEmptyAndTopicsUseOriginalNormalization(){
  var out=processor.parse(input("bad",null,"null"),Map.of());assertTrue(out.authors().isEmpty());assertTrue(out.funds().isEmpty());
  var topics=processor.parse(new PublicationMetadataProcessor.Input(null,null,null,"[\"Databases, Protein\",\"Expression, Gene\"]",null,null),Map.of()).topics();assertEquals(Set.of("Protein","Gene Expression"),topics);
 }
}
