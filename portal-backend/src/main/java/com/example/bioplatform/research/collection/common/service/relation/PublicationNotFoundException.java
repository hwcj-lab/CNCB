package com.example.bioplatform.research.collection.common.service.relation;

import java.io.IOException;

/**
 * Both metadata services returned valid responses without the requested PMID.
 */
public class PublicationNotFoundException extends IOException {
    public PublicationNotFoundException(String pmid) {
        super("OpenLB及PubMed均未返回PMID " + pmid);
    }
}
