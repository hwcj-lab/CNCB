package com.example.bioplatform.research.collection.genbase.dto;


/** GenBase接口记录的批量写入模型。 */
public record GenBasePublicationWriteRow(
        Long paperId,
        String pmid,
        String doi,
        String bioprojectAccession,
        String journalName,
        String paperTitle,
        Integer publishYear,
        String genbaseAccessions,
        String sourceUrl,
        String rawPayload,
        String sourceHash
) {}

