package com.example.bioplatform.research.collection.genbase.dto;

import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GenBase待补全文献：PMID可空，同时保留DOI、项目号及既有作者/基金供合并。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GenBasePmidTask extends PublicationEnrichmentTask {
    /**
     * paperId：GenBase支持文章的原库paper_id。
     */
    private Long paperId;
}
