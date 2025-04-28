package com.supos.uns.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supos.adpter.elasticsearch.ElasticsearchAdpterService;
import com.supos.common.Constants;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.TopologyLog;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.event.EventBus;
import com.supos.common.event.NamespaceChangeEvent;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.event.UnsTopologyChangeEvent;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.RuntimeUtil;
import com.supos.uns.bo.InstanceTopologyData;
import com.supos.uns.bo.PathTypeCount;
import com.supos.uns.bo.ProtocolCount;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsTopologyMapper;
import com.supos.uns.vo.ICMPStateVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 拓扑图相关信息
 * @date 2024/12/11 11:18
 */
@Service
@Slf4j
public class UnsTopologyService {

    private volatile GlobalTopologyData globalTopologyData;

    private static final String INDEX_REX = "filebeat-%s-%s";

    private ScheduledExecutorService statisticsExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        AtomicInteger threadNum = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Topology-" + threadNum.incrementAndGet());
        }
    });

    @Value("${ELASTICSEARCH_TIMEHORIZON:60}")
    private Long timeHorizon;
    @Value("${ELASTICSEARCH_VERSION:7.10.2}")
    private String version;
    @Value("${elk.enabled:true}")
    private String enableElk;

    @Resource
    private UnsTopologyMapper unsTopologyMapper;

    @Resource
    private AlarmMapper alarmMapper;

    @Resource
    private ElasticsearchAdpterService elasticsearchAdpterService;

    @Resource
    private SystemConfig systemConfig;

    @EventListener(classes = ContextRefreshedEvent.class)
    private void init() {
        // 平台启动，初始化统计信息
        statisticsExecutor.scheduleAtFixedRate(this::refresh, 2, 10, TimeUnit.SECONDS);
    }

    private static final boolean isLocalDebug = RuntimeUtil.isLocalRuntime();

    private void refresh() {
        try {
            GlobalTopologyData topologyData = new GlobalTopologyData();
            // 统计模型、实例数量
            List<PathTypeCount> namespaceNumDatas = unsTopologyMapper.selectGroupByPathType();
            if (CollectionUtil.isNotEmpty(namespaceNumDatas)) {
                for (PathTypeCount namespaceNumData : namespaceNumDatas) {
                    int pathType = namespaceNumData.getPathType();
                    long count = namespaceNumData.getCount();
                    if (isModelOrInstance(pathType) && count != -1L) {
                        if (pathType == 0) {
                            topologyData.setModelNum(count);
                        } else {
                            topologyData.setInstanceNum(count);
                        }
                    }
                }
            }

            // 统计协议实例数量
            List<ProtocolCount> protocolTypeNumDatas = unsTopologyMapper.selectGroupByProtocolType();
            if (CollectionUtil.isNotEmpty(protocolTypeNumDatas)) {
                for (ProtocolCount protocolTypeNumData : protocolTypeNumDatas) {
                    String protocolType = protocolTypeNumData.getProtocol();
                    long count = protocolTypeNumData.getCount();

                    if (protocolType != null && count != -1L) {
                        IOTProtocol protocol = IOTProtocol.getByName(protocolType);
                        topologyData.getProtocol().put(protocol != IOTProtocol.UNKNOWN ? protocol.getName() : protocolType, count);
                    }
                }
            }

            //报警数
            topologyData.setAlarmNum(alarmMapper.selectCount(null));

            if (!isLocalDebug) {
                //emqx 连接数
                HttpResponse httpResponse = HttpRequest.get("http://emqx:18083/api/v5/monitor_current")
                        .basicAuth(Constants.EMQX_API_KEY, Constants.EMQX_SECRET_KEY)
                        .execute();
                if (200 == httpResponse.getStatus()) {
                    JSONObject data = JSONObject.parseObject(httpResponse.body());
                    topologyData.setLiveConnections(data.getLong("live_connections"));
                    topologyData.setAllConnections(data.getLong("connections"));
                }
            }

            if (!topologyData.equals(globalTopologyData)) {
                if (globalTopologyData != null) {
                    topologyData.setIcmpStates(globalTopologyData.getIcmpStates());
                }
                globalTopologyData = topologyData;
                String prev = topologyJson;
                topologyJson = topologyData.toMessage();
                log.debug("UNS 或 协议数 发生变化: {} -> {}", prev, topologyJson);
            }

            EventBus.publishEvent(new UnsTopologyChangeEvent(this));
        } catch (Exception e) {
            log.error("schedule refresh topology error!", e);
        }
    }

    private boolean isModelOrInstance(int path) {
        return (path == 0 || path == 2);
    }

    private volatile String topologyJson = "{}";

    public JsonResult<String> getLastMsg() {
        return new JsonResult<>(0, "ok", topologyJson);
    }

    public void removeFromGlobalTopologyData(String topic) {
        Set<ICMPStateVO> icmpStates = globalTopologyData.getIcmpStates();
        icmpStates.remove(new ICMPStateVO(topic, 0));
    }

    @EventListener(classes = NamespaceChangeEvent.class)
    public void namespaceChange(NamespaceChangeEvent event) {
        // 模型实例变动，重新统计信息
        statisticsExecutor.schedule(this::refresh, 1, TimeUnit.SECONDS);
    }


    /**
     * 监听icmp数据推送
     * @param event
     */
    @EventListener(classes = TopicMessageEvent.class)
    public void icmpRealDataChange(TopicMessageEvent event) {
        if (!IOTProtocol.ICMP.name().equalsIgnoreCase(event.protocol)) {
            return;
        }
        // 设置icmp数据
        Map<String, Object> data = event.data;
        Set<ICMPStateVO> icmpStates = globalTopologyData.getIcmpStates();
        Object status = data.get("status"); // icmp类型都有固定status属性
        ICMPStateVO realState = new ICMPStateVO(event.topic, Integer.valueOf(status.toString()));
        boolean isChanged = false;
        if (icmpStates.contains(realState)) {
            for (ICMPStateVO state : icmpStates) {
                // 比较状态是否一致
                if (state.getTopic().equals(event.topic) && realState.getStatus().intValue() != state.getStatus().intValue()) {
                    state.setStatus(realState.getStatus());
                    isChanged = true;
                    break;
                }
            }
        } else {
            icmpStates.add(realState);
            isChanged = true;
        }
        if (isChanged) {
            log.debug("icmp节点（{}）状态发生变化: {} -> {}", event.topic, 1 - realState.getStatus(), realState.getStatus());
            topologyJson = globalTopologyData.toMessage();
            EventBus.publishEvent(new UnsTopologyChangeEvent(this));
        }
    }



    /**
     * 获取实例的拓扑节点状态
     *
     * @param topic
     */
    public List<InstanceTopologyData> gainTopologyDataOfInstance(String topic) {

        // windows does not support topologies
        if (StringUtils.equals(systemConfig.getPlatformType(), "windows") || "false".equals(enableElk)) {
            return createDefaultTopologyData();
        }

        Calendar calendar = Calendar.getInstance();
        long startTime = calendar.getTimeInMillis() - timeHorizon * 1000;
        // 构建查询索引：
        String today = DateUtil.format(calendar.getTime(), "yyyy.MM.dd");
        String index = String.format(INDEX_REX, version, today);

        if (!elasticsearchAdpterService.isIndexExist(index) || !elasticsearchAdpterService.isFieldExist(index, "instanceTopic")) {
            log.warn("index:{} or field:instanceTopic is not exist!", index);
            return createDefaultTopologyData();
        }

        List<String> topologyNodes = TopologyLog.topologyNodes;
        List<InstanceTopologyData> topologyDatas = new ArrayList<>(topologyNodes.size());
        for (String topologyNode : topologyNodes) {
            InstanceTopologyData topologyData = new InstanceTopologyData();
            topologyData.setTopologyNode(topologyNode);
            SearchRequest searchRequest = new SearchRequest(index);
            searchRequest.source(createSearchSourceBuilder(topic, topologyNode, startTime));

            SearchHits hits = elasticsearchAdpterService.search(searchRequest);
            if (hits != null && hits.getHits() != null && hits.getHits().length >= 1) {
                SearchHit hit = hits.getAt(0);
                Map<String, Object> valueMap = hit.getSourceAsMap();
                Object value = valueMap.get("eventCode");
                if (value != null && value instanceof String) {
                    topologyData.setEventCode((String) value);
                }

                value = valueMap.get("eventMessage");
                if (value != null && value instanceof String) {
                    topologyData.setEventMessage((String) value);
                }

                value = valueMap.get("eventTime");
                if (value != null && value instanceof Long) {
                    topologyData.setEventTime((Long) value);
                }
            } else {
                topologyData.setEventCode(TopologyLog.EventCode.SUCCESS);
            }
            topologyDatas.add(topologyData);
        }
        return topologyDatas;
    }

    /**
     * 创建缺省拓扑节点信息
     *
     * @return
     */
    private List<InstanceTopologyData> createDefaultTopologyData() {
        List<String> topologyNodes = TopologyLog.topologyNodes;
        List<InstanceTopologyData> topologyDatas = new ArrayList<>(topologyNodes.size());
        for (String topologyNode : topologyNodes) {
            InstanceTopologyData topologyData = new InstanceTopologyData();
            topologyData.setTopologyNode(topologyNode);
            topologyData.setEventCode(TopologyLog.EventCode.SUCCESS);
            topologyDatas.add(topologyData);
        }
        return topologyDatas;
    }

    /**
     * 创建查询条件：
     * 查询出时间范围内，符合条件的最新一条数据
     *
     * @param topic
     * @param topologyNode
     * @param startTime
     * @return
     */
    private SearchSourceBuilder createSearchSourceBuilder(String topic, String topologyNode, long startTime) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.termQuery("topologyNode", topologyNode));
        boolQueryBuilder.must(QueryBuilders.boolQuery()
                .should(QueryBuilders.termQuery("instanceTopic", "_ALL"))
                .should(QueryBuilders.termQuery("instanceTopic", topic)));

        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("eventTime")
                .gte(startTime); // 起始时间
        boolQueryBuilder.filter(rangeQueryBuilder);

        searchSourceBuilder.fetchSource(new String[]{"instanceTopic", "topologyNode", "eventCode", "eventMessage", "eventTime"}, new String[]{});
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(1); // 设置为1获取最多一条记录
        searchSourceBuilder.sort("eventTime", SortOrder.DESC);
        return searchSourceBuilder;
    }

    @Data
    @EqualsAndHashCode(exclude = {"icmpStates"})
    static class GlobalTopologyData {
        @JsonProperty("Folder")
        long modelNum;
        @JsonProperty("File")
        long instanceNum;
        @JsonProperty("Alarm")
        long alarmNum;
        @JsonProperty("allConnections")
        long allConnections;//总连接数
        @JsonProperty("liveConnections")
        long liveConnections;//在线连接数

        Map<String, Long> protocol = new HashMap<>();
        // 监控目标服务器ping状态，状态在树节点展示
        Set<ICMPStateVO> icmpStates = new HashSet<>();

        public String toMessage() {
            return JsonUtil.toJson(this);
        }
    }
}
