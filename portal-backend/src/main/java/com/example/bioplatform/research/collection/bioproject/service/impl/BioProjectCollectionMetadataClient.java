package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentTask;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentWrite;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.example.bioplatform.research.collection.common.service.impl.PublicationMetadataEnricher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * BioProject采集的元数据补全门面；把项目出版记录交给公共补全组件，不把项目提交者当论文作者。
 */
@Component
public class BioProjectCollectionMetadataClient {
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final CollectionHttpTransport http = new CollectionHttpTransport();
    /**
     * delegate：被包装的原始来源适配器。
     */
    private final PublicationMetadataEnricher delegate;

    /**
     * 构造BioProjectCollectionMetadataClient，注入本组件所需依赖并建立初始状态。
     * @param json JSON解析与序列化组件
     * @param projects BioProject元数据访问组件或项目结果集合
     */
    public BioProjectCollectionMetadataClient(ObjectMapper json, BioProjectRelatedResourceResolver projects) {
        delegate = new PublicationMetadataEnricher(http, json, projects);
    }

    /**
     * 基于已有标识补充元数据并保留原有有效字段；具体网络读取和合并规则由本来源及公共组件共同处理。
     */
    public BioProjectEnrichmentWrite enrich(BioProjectEnrichmentTask task, BioProjectCollectionStartRequest config, BooleanSupplier stop, Consumer<String> progress) throws Exception {
        http.configure(config.delay());
        var result = new BioProjectEnrichmentWrite();
        BeanUtils.copyProperties(delegate.enrich(task, config.attempts(), stop, progress), result);
        return result;
    }

    /**
     * 将已存来源记录转换为统一补全输入，保留作者、基金和项目等既有信息。
     */
    public static BioProjectEnrichmentWrite fromTask(BioProjectEnrichmentTask task) {
        var result = new BioProjectEnrichmentWrite();
        BeanUtils.copyProperties(PublicationMetadataEnricher.fromTask(task), result);
        return result;
    }
}
