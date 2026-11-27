package com.example.bioplatform.research.preparation.model;
import lombok.Data;
@Data
public class PreparedDocument {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long sourceRowId;
    private Long id, validFrom, validUntil, seenRun;
    private String sourceCode, sourceKey, sourceHash, documentKey, contentHash;
    private String pmid, doi, title, journalTitle, sourceUrl, recordKind, resourceAccession;
    private String gsaAccessions, bioprojectAccession;
    private Integer pubYear;
    private Boolean primaryFlag;
}
