package com.example.bioplatform.research.collection.common.service.impl;
import com.example.bioplatform.research.collection.common.service.impl.PubMedXmlMetadataParser;
import org.junit.jupiter.api.Test;import com.fasterxml.jackson.databind.ObjectMapper;import static org.junit.jupiter.api.Assertions.*;
class PubMedXmlMetadataParserTest {
 @Test void keepsAuthorsOwnAffiliationsAndAgencyGrantFields()throws Exception {
  String xml="<?xml version='1.0'?><!DOCTYPE PubmedArticleSet SYSTEM 'https://example.invalid/should-not-be-loaded.dtd'><PubmedArticleSet><PubmedArticle><MedlineCitation><PMID>41028458</PMID><Article><ArticleTitle>A paper</ArticleTitle><Journal><Title>A journal</Title><PubDate><Year>2026</Year></PubDate></Journal><AuthorList><Author><LastName>Li</LastName><ForeName>Wei</ForeName><AffiliationInfo><Affiliation>Department A, University A, China</Affiliation></AffiliationInfo></Author><Author><LastName>Wang</LastName><ForeName>Jun</ForeName><AffiliationInfo><Affiliation>Department B, University B, China</Affiliation></AffiliationInfo></Author><Author><LastName>Zhang</LastName><ForeName>Yu</ForeName></Author></AuthorList><GrantList><Grant><GrantID>GM123</GrantID><Acronym>GM</Acronym><Agency>NIGMS NIH HHS</Agency></Grant></GrantList></Article></MedlineCitation></PubmedArticle></PubmedArticleSet>";
  var out=PubMedXmlMetadataParser.parse(xml,"41028458",new ObjectMapper());
  assertEquals("Wei",out.path("authors").get(0).path("firstName").asText());assertTrue(out.path("authors").get(0).path("affiliation").asText().contains("University A"));assertTrue(out.path("authors").get(1).path("affiliation").asText().contains("University B"));assertEquals("",out.path("authors").get(2).path("affiliation").asText());
  assertEquals("NIGMS NIH HHS",out.path("grants").get(0).path("agency").asText());assertNull(PubMedXmlMetadataParser.parse(xml,"99999999",new ObjectMapper()));
 }
}
