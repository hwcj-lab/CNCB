package com.example.bioplatform.research.collection.common.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * OpenLB没有该PMID时解析NCBI efetch XML；每个作者保留自己的AffiliationInfo。
 */
public final class PubMedXmlMetadataParser {
    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
    private PubMedXmlMetadataParser() {
    }

    /**
     * 解析PubMed XML中的匹配PMID，保留逐作者机构、基金和MeSH；正常无匹配返回null，错误响应或无效XML继续抛出。
     * @param xml PubMed返回的原始XML正文
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     * @param json JSON解析与序列化组件
     * @return 按上述规则计算或读取的ObjectNode结果
     */
    public static ObjectNode parse(String xml, String pmid, ObjectMapper json) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        if (!"PubmedArticleSet".equals(doc.getDocumentElement().getTagName())
                || doc.getElementsByTagName("ERROR").getLength() > 0)
            throw new java.io.IOException("PubMed响应结构异常或包含服务错误");
        Element record = null;
        NodeList articles = doc.getElementsByTagName("PubmedArticle");
        for (int i = 0; i < articles.getLength(); i++) {
            Element candidate = (Element) articles.item(i);
            if (pmid.equals(text(candidate, "PMID"))) {
                record = candidate;
                break;
            }
        }
        if (record == null) return null;
        ObjectNode out = json.createObjectNode().put("pmid", pmid).put("title", text(record, "ArticleTitle")).put("rawPubmedXml", xml);
        Element journal = first(record, "Journal");
        out.put("journalTitle", text(journal, "Title"));
        String year = text(first(journal, "PubDate"), "Year");
        if (year.matches("[12][0-9]{3}")) out.put("year", Integer.parseInt(year));
        ArrayNode authors = out.putArray("authors");
        NodeList list = record.getElementsByTagName("Author");
        for (int i = 0; i < list.getLength(); i++) {
            Element author = (Element) list.item(i);
            ObjectNode a = authors.addObject().put("firstName", text(author, "ForeName")).put("lastName", text(author, "LastName")).put("collectiveName", text(author, "CollectiveName"));
            ArrayNode affiliationList = a.putArray("affiliations");
            NodeList affiliations = author.getElementsByTagName("Affiliation");
            StringBuilder summary = new StringBuilder();
            for (int j = 0; j < affiliations.getLength(); j++) {
                String value = affiliations.item(j).getTextContent().trim();
                affiliationList.addObject().put("affiliation", value);
                if (!summary.isEmpty()) summary.append("; ");
                summary.append(value);
            }
            a.put("affiliation", summary.toString());
        }
        ArrayNode grants = out.putArray("grants");
        NodeList funding = record.getElementsByTagName("Grant");
        for (int i = 0; i < funding.getLength(); i++) {
            Element g = (Element) funding.item(i);
            grants.addObject().put("grantId", text(g, "GrantID")).put("acronym", text(g, "Acronym")).put("agency", text(g, "Agency")).put("country", text(g, "Country"));
        }
        ArrayNode mesh = out.putArray("meshHeadings");
        NodeList headings = record.getElementsByTagName("DescriptorName");
        for (int i = 0; i < headings.getLength(); i++) mesh.addObject().put("name", headings.item(i).getTextContent());
        ArrayNode keywords = out.putArray("keywords");
        NodeList words = record.getElementsByTagName("Keyword");
        for (int i = 0; i < words.getLength(); i++) keywords.add(words.item(i).getTextContent());
        StringBuilder abstractText = new StringBuilder();
        NodeList abstracts = record.getElementsByTagName("AbstractText");
        for (int i = 0; i < abstracts.getLength(); i++) {
            if (!abstractText.isEmpty()) abstractText.append('\n');
            abstractText.append(abstracts.item(i).getTextContent());
        }
        out.put("abstractText", abstractText.toString());
        NodeList ids = record.getElementsByTagName("ArticleId");
        for (int i = 0; i < ids.getLength(); i++) {
            Element id = (Element) ids.item(i);
            if ("doi".equals(id.getAttribute("IdType"))) out.put("doi", id.getTextContent().trim());
        }
        return out;
    }

    /**
     * 取得第一个符合字段或节点约定的有效内容，没有匹配时返回空值。
     * @param e 当前异常或XML元素，按声明类型解释
     * @param name 当前对象或资源的展示名称
     * @return 按上述规则计算或读取的Element结果
     */
    private static Element first(Element e, String name) {
        if (e == null) return null;
        NodeList list = e.getElementsByTagName(name);
        return list.getLength() == 0 ? null : (Element) list.item(0);
    }

    /**
     * 读取指定节点中的文本字段，按本类空值约定返回结果。
     * @param e 当前异常或XML元素，按声明类型解释
     * @param name 当前对象或资源的展示名称
     * @return 按上述规则计算或读取的String结果
     */
    private static String text(Element e, String name) {
        Element item = first(e, name);
        return item == null ? "" : item.getTextContent().trim();
    }
}
