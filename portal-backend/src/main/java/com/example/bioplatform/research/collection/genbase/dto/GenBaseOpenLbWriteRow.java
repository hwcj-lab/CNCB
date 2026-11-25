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
    private Long paperId;
}
