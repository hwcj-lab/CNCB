package com.example.bioplatform.research.statistics.prepared.model;
import lombok.Data;
import java.util.List;
@Data
public class PreparedScope {
    private long revision;
    private List<String> sources;
    private String scope="TOTAL",type,memberKey;
    private List<String> sourceFilter;
    private boolean includeRelated,includeResources;
}
