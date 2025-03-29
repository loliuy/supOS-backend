package com.supos.adpter.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 对接es服务
 * @date 2024/12/19 14:31
 */
@Slf4j
@Component
public class ElasticsearchAdpterService {

    @Resource
    private RestHighLevelClient client;

    public SearchHits search(SearchRequest searchRequest) {
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            if (response != null && 200 != response.status().getStatus()) {
                log.error("es search失败");
            }
            return response.getHits();
        } catch (Exception e) {
            log.error("es search失败", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 判断索引是否存在
     * @param indexName
     * @return
     */
    public boolean isIndexExist(String indexName) {
        try {
            GetIndexRequest request = new GetIndexRequest(indexName);
            return client.indices().exists(request, RequestOptions.DEFAULT);

        } catch (Exception e) {
            log.error("es isIndexExist失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查indexName索引中是否包含字段fieldName
     * @param indexName
     * @param fieldName
     * @return
     */
    public boolean isFieldExist(String indexName, String fieldName) {
        try {
            GetFieldMappingsRequest request = new GetFieldMappingsRequest()
                    .indices(indexName)
                    .fields(fieldName);

            GetFieldMappingsResponse response = client.indices().getFieldMapping(request, RequestOptions.DEFAULT);

            // 检查字段是否存在
            Map<String, Map<String, GetFieldMappingsResponse.FieldMappingMetadata>> mappings = response.mappings();
            Map<String, GetFieldMappingsResponse.FieldMappingMetadata> index = mappings.get(indexName);
            GetFieldMappingsResponse.FieldMappingMetadata fields = index.get(fieldName);
            if (fields == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("es isFieldExist失败,index:{}, field:{}", e, indexName, fieldName);
            throw new RuntimeException(e);
        }
    }
}
