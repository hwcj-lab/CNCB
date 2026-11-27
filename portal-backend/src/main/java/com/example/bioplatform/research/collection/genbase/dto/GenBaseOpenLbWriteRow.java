package com.example.bioplatform.research.collection.genbase.dto;

import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentWrite;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GenBase批量补全写入；状态允许SUCCESS、NO_PMID或EMPTY。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GenBaseOpenLbWriteRow extends PublicationEnrichmentWrite {
    /**
     * paperId：GenBase支持文章的原库paper_id。
     */
    private Long paperId;
}
