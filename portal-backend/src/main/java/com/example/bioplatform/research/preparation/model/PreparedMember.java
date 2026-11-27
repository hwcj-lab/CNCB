package com.example.bioplatform.research.preparation.model;
import lombok.Data;
@Data
public class PreparedMember {
    private Long documentId;
    private String sourceCode, documentKey, pmid, doi, sourceUrl;
    private String memberKey, displayName, authorName, institutionName;
    private Integer authorOrder, statYear;
}
