package com.example.bioplatform.research.preparation;
import com.example.bioplatform.research.preparation.service.PreparedMetadataParser;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.SourceMembership;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PreparedMetadataParserTest {
 final PreparedMetadataParser parser=new PreparedMetadataParser(new ObjectMapper());
 @Test void institutionsAreSeparateMembersAndMissingAuthorsAreNotMisassigned(){
  var row=new SourcePublicationRecord();row.setAuthorsJson("[{\"firstName\":\"Alice\",\"lastName\":\"Li\",\"institutionName\":\"University One; University Two\"},{\"fullName\":\"Bob\",\"institutionName\":\"University One\"},{\"fullName\":\"Unknown\"}]");
  row.setGrantsJson("[{\"agency\":\"Agency\"},{\"agency\":\"Agency\"},{\"grantId\":\"G1\"},{\"acronym\":\"GM\"}]");row.setKeywordList("[\"gene\",\"gene\"]");
  var result=parser.parse(row,Map.of("g1","Full Funder"));assertEquals(4,result.members().get("AUTHOR").size());assertEquals(2,result.members().get("INSTITUTION").size());assertEquals(2,result.members().get("FUND").size());assertEquals(1,result.members().get("TOPIC").size());
  var unknown=result.members().get("AUTHOR").stream().filter(m->m.getAuthorName().equals("Unknown")).findFirst().orElseThrow();assertNull(unknown.getInstitutionName());assertEquals(3,unknown.getAuthorOrder());
  assertEquals(result.contentHash(),parser.parse(row,Map.of("g1","Full Funder")).contentHash());
 }
 @Test void singleKnownInstitutionCanBeInheritedButAuthorsWithTheirOwnInstitutionKeepIt(){
  var row=new SourcePublicationRecord();row.setAuthorsJson("[{\"fullName\":\"First\",\"institutionName\":\"University One\"},{\"fullName\":\"Second\"}]");
  assertTrue(parser.parse(row,Map.of()).members().get("AUTHOR").stream().allMatch(m->m.getInstitutionName().equals("University One")));
  row.setAuthorsJson("[{\"fullName\":\"First\",\"institutionName\":\"University One\"},{\"fullName\":\"Second\",\"institutionName\":\"University Two\"},{\"fullName\":\"Third\"}]");
  assertNull(parser.parse(row,Map.of()).members().get("AUTHOR").get(2).getInstitutionName());
 }
 @Test void malformedJsonFailsRatherThanPublishingPartialMembersAndNullIsAllowed(){
  var row=new SourcePublicationRecord();assertTrue(parser.parse(row,Map.of()).members().values().stream().allMatch(List::isEmpty));row.setAuthorsJson("[{bad");assertThrows(IllegalArgumentException.class,()->parser.parse(row,Map.of()));
 }
 @Test void ownershipIsRelativeToExplicitConfiguredBanks(){var membership=new SourceMembership(new ObjectMapper());String banks="[{\"db\":\"Database Commons\"},{\"db\":\"GSA\"}]";var routes=Map.of("database commons","DATABASECOMMONS","gsa","GSA");assertTrue(membership.belongs("GSA",banks,routes));assertTrue(membership.belongs("DATABASECOMMONS",banks,routes));assertFalse(membership.belongs("BIOPROJECT",banks,routes));assertFalse(membership.belongs("GSA",null,routes));}
}
