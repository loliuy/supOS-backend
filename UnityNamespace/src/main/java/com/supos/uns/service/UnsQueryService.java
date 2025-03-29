package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.http.ssl.DefaultSSLFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.supos.adapter.mqtt.service.MQTTPublisher;
import com.supos.camunda.service.ProcessService;
import com.supos.common.Constants;
import com.supos.common.NodeType;
import com.supos.common.annotation.DateTimeConstraint;
import com.supos.common.dto.*;
import com.supos.common.dto.protocol.RestConfigDTO;
import com.supos.common.dto.protocol.RestServerConfigDTO;
import com.supos.common.enums.FieldType;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.exception.BuzException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.*;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.mapper.AlarmHandlerMapper;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.ParserUtil;
import com.supos.uns.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.supos.uns.service.UnsManagerService.genIdForPath;
import static com.supos.uns.service.UnsManagerService.getShowPath;

@Slf4j
@Service
public class UnsQueryService {

    private final UnsMapper unsMapper;
    private final AlarmMapper alarmMapper;
    private final UnsLabelMapper unsLabelMapper;
    private final AlarmHandlerMapper alarmHandlerMapper;
    private final ProcessService processService;

    private MQTTPublisher mqttPublisher;

    // 缓存外部topic
    public static Map<String, Date> EXTERNAL_TOPIC_CACHE = new ConcurrentHashMap<>();

    private static final List<String> fieldTypes =
            Collections.unmodifiableList(Arrays.stream(FieldType.values()).map(FieldType::getName).collect(Collectors.toList()));

    public UnsQueryService(@Autowired UnsMapper unsMapper, @Autowired AlarmMapper alarmMapper,
                           @Autowired UnsLabelMapper unsLabelMapper, @Autowired MQTTPublisher mqttPublisher,
                           @Autowired AlarmHandlerMapper alarmHandlerMapper, @Autowired ProcessService processService) {
        this.unsMapper = unsMapper;
        this.alarmMapper = alarmMapper;
        this.unsLabelMapper = unsLabelMapper;
        this.mqttPublisher = mqttPublisher;
        this.alarmHandlerMapper = alarmHandlerMapper;
        this.processService = processService;
    }

    public JsonResult<Collection<String>> listTypes() {
        return new JsonResult<Collection<String>>().setData(fieldTypes);
    }

    public static void batchRemoveExternalTopic(Collection<CreateTopicDto[]> topics) {
        for (CreateTopicDto[] arr : topics) {
            for (CreateTopicDto t : arr) {
                EXTERNAL_TOPIC_CACHE.remove(t.getTopic());
            }
        }
    }

    public static JsonResult<List<OuterStructureVo>> parseJson2uns(String json) {
        Object vo;
        try {
            vo = JsonUtil.fromJson(json);
        } catch (Exception e) {
            return new JsonResult<>(400, I18nUtils.getMessage("uns.invalid.json"));
        }
        FindDataListUtils.SearchResult searchResult = FindDataListUtils.findMultiDataList(vo, null);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.multiResults)) {
            return new JsonResult<>(400, I18nUtils.getMessage("uns.rest.data404"));
        }
        LinkedList<FindDataListUtils.ListResult> onlyLists = new LinkedList<>();
        for (FindDataListUtils.ListResult rs : searchResult.multiResults) {
            if (rs.dataInList) {
                onlyLists.add(rs);
            }
        }
        Collection<FindDataListUtils.ListResult> list;
        if (!onlyLists.isEmpty()) {// 数组非空，就只要数组
            list = onlyLists;
        } else {// 否则取所有结果，即所有对象
            list = searchResult.multiResults;
        }
        List<OuterStructureVo> rsList = list.stream().map(UnsQueryService::map2fields).toList();
        return new JsonResult<>(0, "ok", rsList);
    }

    private static OuterStructureVo map2fields(FindDataListUtils.ListResult rs) {
        LinkedHashMap<String, FieldType> fieldTypes = new LinkedHashMap<>();
        for (Map<String, Object> data : rs.list) {
            for (Map.Entry<String, Object> m : data.entrySet()) {
                String k = m.getKey();
                Object v = m.getValue();
                FieldType fieldType = fieldTypes.get(k);
                FieldType guessType = guessType(v);
                if (fieldType == null || guessType.ordinal() > fieldType.ordinal()) {
                    fieldTypes.put(k, guessType);
                }
            }
        }
        if (fieldTypes.isEmpty()) {
            return null;
        }
        List<FieldDefine> fields = fieldTypes.entrySet().stream()
                .map(e -> new FieldDefine(e.getKey(), e.getValue()))
                .toList();
        return new OuterStructureVo(rs.dataPath, fields);
    }

    public static FieldType guessType(Object o) {
        if (o != null) {
            Class clazz = o.getClass();
            if (clazz == Integer.class) {
                return FieldType.INT;
            } else if (clazz == Long.class) {
                return FieldType.LONG;
            } else if (clazz == Double.class || clazz == BigDecimal.class) {
                return FieldType.DOUBLE;
            } else if (clazz == Boolean.class) {
                return FieldType.BOOLEAN;
            } else if (DateTimeConstraint.parseDate(o.toString()) != null) {
                return FieldType.DATETIME;
            }
        }
        return FieldType.STRING;
    }

    public JsonResult parseJson2TreeUns(String json) {
        return new JsonResult<>(0, "ok", ParserUtil.parserJson2Tree(json));
    }

    public TopicPaginationSearchResult searchPaged(String modelTopic, String keyword, NodeType searchType, Set<Integer> dataTypes, int pageNumber, int pageSize, Integer minNumFields) {
        if ("".equals(keyword)) {
            keyword = null;
        }
        if (keyword != null) {
            keyword = keyword.replace("_", "\\_").replace("%", "\\%");
            keyword = "%" + keyword + "%";
        }
        TopicPaginationSearchResult result = new TopicPaginationSearchResult();
        if (searchType == NodeType.Path || searchType == NodeType.Instance) {
            if ("".equals(modelTopic)) {
                modelTopic = null;
            }
            if (searchType == NodeType.Path) {
                dataTypes = null;
            }
            String modelId = null;
/*            if (modelTopic != null && !modelTopic.isEmpty()) {
                char ec = modelTopic.charAt(modelTopic.length() - 1);
                if (ec != '*') {
                    if (ec == '/') {
                        modelTopic = modelTopic + '*';
                    } else {
                        modelTopic = modelTopic + "/*";
                    }
                }
                modelId = genIdForPath(modelTopic);
            }*/
            if (dataTypes != null && dataTypes.isEmpty()) {
                dataTypes = null;
            }
            int total = unsMapper.countPaths(modelId, keyword, searchType.code, dataTypes);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageDto page = new PageDto();
            page.setPage(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<String> paths = unsMapper.listPaths(modelId, keyword, searchType.code, dataTypes, offset, pageSize);
                if (!CollectionUtils.isEmpty(paths) && searchType == NodeType.Model) {
                    for (int i = 0, len = paths.size(); i < len; i++) {
                        String path = paths.get(i);
                        if (path.endsWith("/")) {
                            paths.set(i, path.substring(0, path.length() - 1));
                        }
                    }
                }
                List strList = paths;
                result.setData(strList);
            } else {
                result.setData(Collections.emptyList());
            }
        } else if (searchType == NodeType.InstanceForCalc) {
            if (minNumFields == null) {
                minNumFields = 1;
            }
            int total = unsMapper.countNotCalcSeqInstance(keyword, minNumFields);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageDto page = new PageDto();
            page.setPage(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<UnsPo> list = unsMapper.listNotCalcSeqInstance(keyword, minNumFields, offset, pageSize);
                ArrayList<CalcInstanceSearchResult> rs = new ArrayList<>(list.size());
                for (UnsPo po : list) {
                    FieldDefine[] fs = JsonUtil.fromJson(po.getFields(), FieldDefine[].class);
                    List<FieldDefineVo> fields = Arrays.stream(fs)
                            .filter(f -> (f.getType().isNumber || f.getType() == FieldType.BOOLEAN) && !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV))
                            .map(f -> new FieldDefineVo(f.getName(), f.getType().name)).collect(Collectors.toList());
                    if (fields.size() > 0) {
                        CalcInstanceSearchResult srs = new CalcInstanceSearchResult();
                        srs.setTopic(po.getPath());
                        srs.setFields(fields);
                        rs.add(srs);
                    }
                }
                List objList = rs;
                result.setData(objList);
            } else {
                result.setData(Collections.emptyList());
            }
        } else if (searchType == NodeType.InstanceForTimeseries) {
            int total = unsMapper.countTimeSeriesInstance(keyword);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageDto page = new PageDto();
            page.setPage(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<UnsPo> list = unsMapper.listTimeSeriesInstance(keyword, offset, pageSize);
                ArrayList<TimeseriesInstanceSearchResult> rs = new ArrayList<>(list.size());
                for (UnsPo po : list) {
                    FieldDefine[] fs = JsonUtil.fromJson(po.getFields(), FieldDefine[].class);
                    List<FieldDefineVo> fields = Arrays.stream(fs)
                            .filter(f -> (f.getType().isNumber) && !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV))
                            .map(f -> new FieldDefineVo(f.getName(), f.getType().name)).collect(Collectors.toList());
                    if (fields.size() > 0) {
                        TimeseriesInstanceSearchResult srs = new TimeseriesInstanceSearchResult();
                        srs.setTopic(po.getPath());
                        srs.setFields(fields);
                        rs.add(srs);
                    }
                }
                List objList = rs;
                result.setData(objList);
            } else {
                result.setData(Collections.emptyList());
            }
        } else if (searchType == NodeType.AlarmRule) {
            int total = unsMapper.countAlarmRules(keyword);
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            int offset = (pageNumber - 1) * pageSize;
            PageDto page = new PageDto();
            page.setPage(pageNumber);
            page.setTotal(total);
            page.setPageSize(pageSize);
            result.setPage(page);
            if (total > 0) {
                ArrayList<UnsPo> list = unsMapper.listAlarmRules(keyword, offset, pageSize);
                ArrayList<AlarmRuleSearchResult> rs = new ArrayList<>(list.size());
                Map<String, Long[]> countAlarms = new HashMap<>(Math.max(64, list.size()));
                if (!CollectionUtils.isEmpty(list)) {
                    Collection<String> topics = list.stream().map(p -> p.getPath()).collect(Collectors.toSet());
                    final String TOPIC = AlarmRuleDefine.FIELD_TOPIC;
                    List<AlarmPo> alarmPos = alarmMapper.selectList(new QueryWrapper<AlarmPo>()
                            .select(TOPIC, "count(1) as currentValue,SUM(CASE WHEN read_status = false THEN 1 ELSE 0 END) AS noReadCount").groupBy(TOPIC)
                            .in(TOPIC, topics));
                    for (AlarmPo po : alarmPos) {
                        countAlarms.put(po.getTopic(), new Long[]{po.getCurrentValue().longValue(), po.getNoReadCount()});
                    }
                }
                for (UnsPo po : list) {
                    AlarmRuleSearchResult srs = new AlarmRuleSearchResult();
                    srs.setId(po.getId());
                    srs.setTopic(po.getPath());
                    srs.setName(po.getDataPath());
                    srs.setDescription(po.getDescription());
                    srs.setWithFlags(AlarmService.checkWithFlags(po.getWithFlags()));
                    srs.setHandlerList(Collections.emptyList());
                    if (srs.getWithFlags() == Constants.UNS_FLAG_ALARM_ACCEPT_PERSON) {
                        srs.setHandlerList(alarmHandlerMapper.getByTopic(srs.getTopic()));
                    } else if (srs.getWithFlags() == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW) {
                        srs.setProcessDefinition(processService.getById(Long.valueOf(po.getExtend())));
                    }
                    Long[] counts = countAlarms.get(po.getPath());
                    if (ObjectUtil.isNotNull(counts)) {
                        Long count = countAlarms.get(po.getPath())[0];
                        Long noReadCount = countAlarms.get(po.getPath())[1];
                        srs.setAlarmCount(count != null ? count.longValue() : 0);
                        srs.setNoReadCount(noReadCount != null ? noReadCount.longValue() : 0);
                    }
                    InstanceField[] refers = JsonUtil.fromJson(po.getRefers(), InstanceField[].class);
                    if (ObjectUtil.isNotNull(refers)) {
                        srs.setRefTopic(refers[0].getTopic());
                        srs.setField(refers[0].getField());
                    }
                    AlarmRuleDefine ruleDefine = JsonUtil.fromJson(po.getProtocol(), AlarmRuleDefine.class);
                    ruleDefine.parseExpression(po.getExpression());
                    srs.setAlarmRuleDefine(ruleDefine);
                    rs.add(srs);
                }
                List objList = rs;
                result.setData(objList);
            } else {
                result.setData(Collections.emptyList());
            }
        }
        return result;
    }

    public JsonResult<List<TopicTreeResult>> searchByTag(String keyword) {
        List<UnsPo> allNamespaces = unsLabelMapper.getUnsByKeyword(keyword);
        List<TopicTreeResult> treeResults = new ArrayList<>(allNamespaces.size());
        treeResults = allNamespaces.stream().filter(uns -> {
            if (StringUtils.hasText(keyword)) {
                String name = PathUtil.getName(uns.getPath());
                if (name.toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        }).map(uns -> {
            TopicTreeResult result = new TopicTreeResult();
            result.setType(2);
            result.setProtocol(uns.getProtocolType());
            result.setPath(uns.getPath());
            result.setName(PathUtil.getName(uns.getPath()));
            return result;
        }).collect(Collectors.toList());
        return new JsonResult<>(0, "ok", treeResults);
    }

    public JsonResult<List<TopicTreeResult>> searchByTemplate(String keyword) {
        List<TopicTreeResult> treeResults = new ArrayList<>();

        List<UnsPo> allNamespaces = unsMapper.listInTemplate(keyword);
        if (!CollectionUtils.isEmpty(allNamespaces)) {
            treeResults = new ArrayList<>(allNamespaces.size());
            for (UnsPo uns : allNamespaces) {
                String name = PathUtil.getName(uns.getPath());
                if (StringUtils.hasText(keyword)) {
                    if (!name.toLowerCase().contains(keyword.toLowerCase())) {
                        continue;
                    }
                }
                TopicTreeResult result = new TopicTreeResult();
                result.setType(uns.getPathType());
                result.setProtocol(uns.getProtocolType());
                result.setPath(uns.getPath());
                result.setName(name);
                treeResults.add(result);
            }
        }
        return new JsonResult<>(0, "ok", treeResults);
    }

    public JsonResult<List<TopicTreeResult>> searchTree(String keyword, boolean showRec) {
        List<UnsPo> allNamespaces = unsMapper.listAllNamespaces();
        List<UnsPo> list = allNamespaces;
        if (allNamespaces != null && keyword != null && (keyword = keyword.trim().toLowerCase()).length() > 0) {
            list = new ArrayList<>(allNamespaces.size());
            for (UnsPo po : allNamespaces) {
                if (po.getPath().toLowerCase().contains(keyword)) {
                    list.add(po);
                }
            }
        }
        List<TopicTreeResult> treeResults = getTopicTreeResults(allNamespaces, list, showRec);
        return new JsonResult<>(0, "ok", treeResults);
    }

    public JsonResult<InstanceDetail> getInstanceDetail(String topic) {
        String fileId = genIdForPath(topic);
        String folderId = null;
        if (topic.contains("/")) {
            folderId = genIdForPath(topic.substring(0, topic.lastIndexOf('/') + 1));
        }
        UnsPo folder = null;
        if (folderId != null) {
            folder = unsMapper.selectById(folderId);
            if (folder == null) {
                return new JsonResult<>(0, "ModelNotFound!");
            }
        }

        InstanceDetail dto = new InstanceDetail();
        UnsPo file = unsMapper.selectById(fileId);
        dto.setId(fileId);
        if (file == null) {
            return new JsonResult<>(0, "InstanceNotFound!", dto);
        }

        dto.setDataType(file.getDataType());
        dto.setModelDescription(folder != null ? folder.getDescription() : null);
        dto.setFields(getFields(file.getFields()));
        dto.setTopic(file.getPath());
        dto.setDataPath(file.getDataPath());
        String protocol = file.getProtocol();
        if (protocol != null && protocol.startsWith("{")) {
            dto.setProtocol(JsonUtil.fromJson(protocol, Map.class));
        }
        dto.setInstanceDescription(file.getDescription());
        dto.setCreateTime(getDatetime(file.getCreateAt()));
        dto.setAlias(file.getAlias());
        dto.setName(PathUtil.getName(file.getPath()));
        String expression = file.getExpression();
        String refStr = file.getRefers();
        if (refStr != null && refStr.startsWith("[")) {
            InstanceField[] fs = JsonUtil.fromJson(refStr, InstanceField[].class);
            dto.setRefers(fs);
            Map<String, Object> protocolMap = dto.getProtocol();
            Object whereExpr;
            if (expression != null) {
                Map<String, String> varReplacer = new HashMap<>(8);
                for (int i = 0; i < fs.length; i++) {
                    InstanceField field = fs[i];
                    if (field != null) {
                        varReplacer.put(Constants.VAR_PREV + (i + 1), String.format("$\"%s\".%s#", field.getTopic(), field.getField()));
                    }
                }
                if (!varReplacer.isEmpty()) {
                    expression = ExpressionUtils.replaceExpression(expression, varReplacer);
                }
            } else if (protocolMap != null && (whereExpr = protocolMap.get("whereCondition")) != null) {
                expression = ExpressionUtils.replaceExpression(whereExpr.toString(), var -> String.format("$\"%s\".%s#", fs[0].getTopic(), var));
            }
        }
        dto.setExpression(expression);
        Integer flagsN = file.getWithFlags();
        if (flagsN != null) {
            int flags = flagsN.intValue();
            dto.setWithFlow(Constants.withFlow(flags));
            dto.setWithDashboard(Constants.withDashBoard(flags));
            dto.setWithSave2db(Constants.withSave2db(flags));
        }
        dto.setLabelList(unsLabelMapper.getLabelByUnsId(fileId));

        String templateId = file.getModelId();
        if (templateId != null) {
            UnsPo template = unsMapper.selectById(templateId);
            if (template != null) {
                dto.setModelId(templateId);
                dto.setModelName(template.getPath());
            }
        }
        return new JsonResult<>(0, "ok", dto);
    }

    public JsonResult<ModelDetail> getModelDefinition(String topic) {
        if (topic == null || topic.length() < 2) {
            String msg = I18nUtils.getMessage("uns.topic.format.invalid");
            return new JsonResult<>(400, msg);
        }

        String modelId = genIdForPath(topic);
        UnsPo po = unsMapper.selectById(modelId);
        if (po == null) {
            String msg = I18nUtils.getMessage("uns.model.not.found");
            return new JsonResult<>(0, msg);
        }
        ModelDetail dto = new ModelDetail();
        dto.setName(PathUtil.getName(topic));
        dto.setTopic(getShowPath(po.getPath()));
        dto.setDataType(po.getDataType());
        dto.setCreateTime(getDatetime(po.getCreateAt()));
        dto.setDescription(po.getDescription());
        dto.setAlias(po.getAlias());
        FieldDefineVo[] fs = getFields(po.getFields());
        if (fs != null) {
            for (FieldDefineVo f : fs) {
                f.setIndex(null);// 模型的定义 给前端消除掉 index
            }
        }
        dto.setFields(fs);

        String templateId = po.getModelId();
        if (templateId != null) {
            UnsPo template = unsMapper.selectById(templateId);
            if (template != null) {
                dto.setModelId(templateId);
                dto.setModelName(template.getPath());
            }
        }
        return new JsonResult<>(0, "ok", dto);
    }


    private static final FieldDefineVo[] getFields(String fs) {
        if (fs != null && fs.length() > 3 && fs.charAt(0) == '[') {
            List<FieldDefineVo> list = JsonUtil.fromJson(fs, new TypeReference<List<FieldDefineVo>>() {
            }.getType());
            return list.stream().filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).toArray(n -> new FieldDefineVo[n]);
        }
        return null;
    }

    private static final Long getDatetime(Date date) {
        return date != null ? date.getTime() : null;
    }

    public JsonResult<String> getLastMsg(String topic) {
        TopicMessageInfo msgInfo = topicLastMessages.get(topic);
        return new JsonResult<>(0, "ok", msgInfo != null ? msgInfo.newestMessage : null);
    }

    static class TopicMessageInfo {
        JSONObject jsonObject, data, dt;
        String newestMessage;
        long messageCount;
        long lastUpdateTime;

        public TopicMessageInfo() {
            this.jsonObject = new JSONObject();
        }

        synchronized void update(long lastUpdateTime, String payload, JSONObject data, final Map<String, Long> dt, String err) {
            jsonObject.put("updateTime", lastUpdateTime);
            jsonObject.put("payload", payload);
            jsonObject.put("msg", err);
            if (data != null) {
                jsonObject.put("data", data);
                jsonObject.put("dt", dt);
            } else {
                jsonObject.remove("data");
                jsonObject.remove("dt");
            }
            newestMessage = jsonObject.toJSONString();
            messageCount++;
            this.lastUpdateTime = lastUpdateTime;
        }
    }

    private static final ConcurrentHashMap<String, TopicMessageInfo> topicLastMessages = new ConcurrentHashMap<>();

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(90)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        for (String topic : event.topics.keySet()) {
            topicLastMessages.remove(topic);
        }
    }

    @EventListener(classes = TopicMessageEvent.class)
    @Order(9)
    void onTopicMessageEvent(TopicMessageEvent event) {

        TopicMessageInfo msgInfo = topicLastMessages.computeIfAbsent(event.topic, k -> new TopicMessageInfo());
        if (event.fieldsMap == null) {
            // 非 UNS topic
            // cache external topics
            EXTERNAL_TOPIC_CACHE.put(event.topic, new Date());
            msgInfo.update(event.nowInMills, event.payload, null, event.lastDataTime, event.err);
            return;
        }
        if (!CollectionUtils.isEmpty(event.data)) {
            Map<String, Object> bean = event.lastData != null ? event.lastData : event.data;
            JSONObject data = new JSONObject(Math.max(bean.size(), 8));
            for (Map.Entry<String, Object> entry : bean.entrySet()) {
                String name = entry.getKey();
                if (name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                    continue;
                }
                Object v = entry.getValue();
                data.put(name, v);
            }
            msgInfo.update(event.nowInMills, event.payload, data, event.lastDataTime, event.err);
        } else {
            msgInfo.update(event.nowInMills, event.payload, null, event.lastDataTime, event.err);
        }
    }

    public JsonResult<RestTestResponseVo> searchRestField(RestTestRequestVo requestVo) {
        Triple<JsonResult<RestTestResponseVo>, FindDataListUtils.SearchResult, String> resultSearchResultPair = doSearchRestField(requestVo);
        if (resultSearchResultPair.getLeft() != null) {
            return resultSearchResultPair.getLeft();
        }
        FindDataListUtils.SearchResult rs = resultSearchResultPair.getMiddle();
        if (CollectionUtils.isEmpty(rs.list) || !rs.dataInList) {
            log.warn("dataListNotFound: {}, from: {}", JsonUtil.toJson(rs), resultSearchResultPair.getRight());
            return new JsonResult<>(404, I18nUtils.getMessage("uns.rest.data404"));
        }
        RestTestResponseVo responseVo = new RestTestResponseVo();
        responseVo.setDataPath(rs.dataPath);
        responseVo.setDataFields(rs.list.get(0).keySet().stream()
                .filter(f -> !f.startsWith(Constants.SYSTEM_FIELD_PREV)).collect(Collectors.toList()));
        return new JsonResult<>(0, resultSearchResultPair.getRight(), responseVo);
    }

    private Triple<JsonResult<RestTestResponseVo>, FindDataListUtils.SearchResult, String> doSearchRestField(RestTestRequestVo requestVo) {
        String respBody = "";
        String url = "";
        Object msgBody = null;
        Map<String, Object> jsonBody = requestVo.getJsonBody();
        if (jsonBody == null || jsonBody.isEmpty()) {
            String[] err = new String[2];
            url = err[1];
            respBody = getJsonBody(requestVo, err);
            if (err[0] != null) {
                return Triple.of(new JsonResult<>(400, err[0] + " : " + respBody), null, respBody);
            }
            try {
                msgBody = JsonUtil.fromJson(respBody);
            } catch (Exception ex) {
            }
            if (msgBody == null) {
                throw new BuzException("jsonErr: " + respBody + ", url=" + url);
            }
        } else {
            msgBody = jsonBody;
        }

        FieldDefine[] fields = requestVo.getFields();
        if (ArrayUtil.isEmpty(fields)) {
            String topic = requestVo.getTopic();
            if (StringUtils.hasText(topic)) {
                JsonResult<InstanceDetail> inst = getInstanceDetail(topic);
                InstanceDetail detail = inst.getData();
                if (detail == null) {
                    return Triple.of(new JsonResult<>(inst.getCode(), inst.getMsg()), null, respBody);
                } else if (ArrayUtil.isNotEmpty(detail.getFields())) {
                    fields = Arrays.stream(detail.getFields()).map(f -> new FieldDefine(f.getName(), FieldType.getByName(f.getType()))).toArray(n -> new FieldDefine[n]);
                } else {
                    return Triple.of(new JsonResult<>(400, I18nUtils.getMessage("uns.fieldsIsEmptyAt", topic)), null, respBody);//"fields is Null at topic:" + topic
                }
            } else {
                return Triple.of(new JsonResult<>(400, I18nUtils.getMessage("uns.fsAndTopicIsEmpty")), null, respBody);//"both fields and topic is Null"
            }
        }
        FieldDefines fieldDefines = new FieldDefines(fields);
        FindDataListUtils.SearchResult rs = FindDataListUtils.findDataList(msgBody, 0, fieldDefines);
        return Triple.of(null, rs, respBody);
    }

    /**
     * 查询外部topic，并组装树状结构返回
     *
     * @param fuzzyTopic
     * @return
     */
    public List<TopicTreeResult> searchExternalTopics(String fuzzyTopic) {
        List<UnsPo> externalTopics = new ArrayList<>();
        EXTERNAL_TOPIC_CACHE.forEach((topic, date) -> {
            UnsPo uns = new UnsPo();
            uns.setPath(topic);
            uns.setPathType(2);
            if (!StringUtils.hasText(fuzzyTopic)) {
                externalTopics.add(uns);
            } else if (topic.toLowerCase().contains(fuzzyTopic.toLowerCase())) {
                externalTopics.add(uns);
            }
        });
        return getTopicTreeResults(externalTopics, externalTopics, false);
    }

    static List<TopicTreeResult> getTopicTreeResults(List<UnsPo> all, List<UnsPo> list, boolean showRec) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        final Map<String, TopicTreeResult> nodeMap = new HashMap<>();
        if (CollectionUtils.isEmpty(all)) {
            all = list;
        }
        for (UnsPo po : all) {
            String path = po.getPath();
            int type = po.getPathType();
            String name = PathUtil.getName(path);
            TopicTreeResult rs = new TopicTreeResult(name, path).setType(po.getPathType()).setProtocol(po.getProtocolType());

            if (type == 2) {
                TopicMessageInfo info = topicLastMessages.get(path);
                if (info != null) {
                    rs.setValue(info.messageCount);
                    if (showRec) {
                        rs.setLastUpdateTime(info.lastUpdateTime);
                    }
                }
            }
            FieldDefineVo[] fs = getFields(po.getFields());
            if (fs != null) {
                for (FieldDefineVo f : fs) {
                    f.setIndex(null);// 模型的定义 给前端消除掉 index
                }
            }
            rs.setFields(fs);

            nodeMap.put(path, rs);
        }
        TreeMap<String, TopicTreeResult> rootNodes = new TreeMap<>();
        HashMap<String, Set<String>> childrenMap = new HashMap<>();
        for (UnsPo po : list) {
            String path = po.getPath();
            String parentPath = PathUtil.subParentPath(path);
            TopicTreeResult currentNode = nodeMap.get(path);

            // 当前节点就是根节点
            if (parentPath == null) {
                Set<String> childMap = childrenMap.computeIfAbsent(parentPath, k -> new HashSet<>());
                if (childMap.add(path)) {
                    rootNodes.put(path, currentNode);
                }
                continue;
            }

            // 当前节点不是根节点
            TopicTreeResult tempParentNode = null;
            TopicTreeResult tempCurrentNode = currentNode;
            boolean addRoot = true;
            do {
                tempParentNode = nodeMap.get(parentPath);
                if (tempParentNode == null) {
                    String name = PathUtil.getName(parentPath);
                    tempParentNode = new TopicTreeResult(name, parentPath).setType(0);
                    nodeMap.put(parentPath, tempParentNode);
                }
                Set<String> childMap = childrenMap.computeIfAbsent(parentPath, k -> new HashSet<>());
                if (childMap.add(tempCurrentNode.getPath())) {
                    // 节点未添加过
                    tempParentNode.addChild(tempCurrentNode);
                } else {
                    // 节点已添加过
                    addRoot = false;
                    break;
                }
                parentPath = PathUtil.subParentPath(parentPath);
                tempCurrentNode = tempParentNode;
            } while (parentPath != null);

            if (addRoot) {
                Set<String> childMap = childrenMap.computeIfAbsent(parentPath, k -> new HashSet<>());
                if (childMap.add(tempParentNode.getPath())) {
                    rootNodes.put(tempParentNode.getPath(), tempParentNode);
                }
            }

        }
        return new ArrayList<>(rootNodes.values());
    }

    private static String getJsonBody(RestTestRequestVo requestVo, String[] err) {
        String mStr = requestVo.getMethod();
        HttpMethod method = HttpMethod.valueOf(mStr != null ? mStr.toUpperCase() : "GET");
        String url = requestVo.getFullUrl();
        if (!StringUtils.hasText(url)) {
            StringBuilder builder = new StringBuilder(256);
            RestServerConfigDTO server = requestVo.getServer();
            String host = server.getHost();
            if (!StringUtils.hasText(host)) {
                err[0] = "host is Empty";
                return null;
            }
            builder.append(host);
            String portStr = server.getPort();
            if (StringUtils.hasText(portStr)) {
                builder.append(':').append(portStr);
            }
            String uri = requestVo.getPath();
            if (StringUtils.hasText(uri)) {
                if (uri.charAt(0) != '/') {
                    builder.append('/');
                }
                builder.append(uri);
            }
            List<StrMapEntry> params = requestVo.getParams();
            PageDef pageDef = requestVo.getPageDef();
            if (!CollectionUtils.isEmpty(params) || (pageDef != null && pageDef.getStart() != null)) {
                boolean hasParams = false;
                if (params != null) {
                    Iterator<StrMapEntry> iterator = params.iterator();
                    if (iterator.hasNext()) {
                        hasParams = true;
                        builder.append('?');
                        appendKeyValueParam(builder, iterator.next());
                    }
                    while (iterator.hasNext()) {
                        builder.append('&');
                        appendKeyValueParam(builder, iterator.next());
                    }
                }
                if (pageDef != null) {
                    builder.append(hasParams ? '&' : '?');
                    StrMapEntry start = pageDef.getStart(), offset = pageDef.getOffset();
                    appendKeyValueParam(builder, start.getKey(), !"0".equals(start.getValue()) ? "1" : "0");
                    if (offset != null) {
                        builder.append('&');
                        appendKeyValueParam(builder, offset.getKey(), "1");
                    }
                }
            }
            url = builder.toString();
        }
        if (err.length > 1) {
            err[1] = url;
        }
        log.info("restUrl: {}", url);
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(method.name()), url);
        Map<String, String> headers = requestVo.getHeaderMap();
        if (headers != null) {
            // 设置headers
            request.addHeaders(headers);
        }
        String body = requestVo.getBody();
        if (StringUtils.hasText(body)) {
            request.body(body, "application/json;charset=UTF-8");
        }

        HttpResponse resp = null;
        try {
            int connTimeout = Math.max(500, requestVo.getTimeoutConnectMills()), readTimeout = Math.max(3000, requestVo.getTimeoutReadMills());
            resp = request
                    .setConnectionTimeout(connTimeout).setReadTimeout(readTimeout)
                    .setSSLSocketFactory(new DefaultSSLFactory())
                    .execute();
        } catch (Exception e) {
            log.error("call restapi error.", e);
            throw new BuzException("uns.restapi.call.error");
        }

        int status = resp.getStatus();
        if (status != 200) {
            err[0] = "status:" + status;
            return resp.body();
        }
        return resp.body();
    }

    /**
     * 显式触发restApi协议流程，并将获取到的数据推送至mqtt
     *
     * @param topic
     * @return
     */
    public JsonResult<RestTestResponseVo> triggerRestApi(String topic) {
        String fileId = genIdForPath(topic);
        UnsPo file = unsMapper.selectById(fileId);
        if (file == null) {
            String msg = I18nUtils.getMessage("uns.file.not.exist");
            return new JsonResult<>(0, msg);
        }
        RestTestRequestVo requestVo = new RestTestRequestVo();

        FieldDefineVo[] fs = getFields(file.getFields());
        FieldDefine[] fieldDefines = Arrays.stream(fs).map(FieldDefineVo::convert).toArray(n -> new FieldDefine[n]);
        requestVo.setFields(fieldDefines);

        RestConfigDTO config = null;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(file.getProtocol())) {
            config = JsonUtil.fromJson(file.getProtocol(), RestConfigDTO.class);
        }
        requestVo.setPath(config.getPath());
        requestVo.setMethod(config.getMethod());
        requestVo.setServer(config.getServer());
        requestVo.setFullUrl(config.getFullUrl());

        if (config.getPageDef() != null) {
            requestVo.setPageDef(JsonUtil.fromJson(JsonUtil.toJson(config.getPageDef()), PageDef.class));
        }

        String body = config.getBody();
        if (body != null && !org.apache.commons.lang3.StringUtils.equals(body, "{}")) {
            Map<String, Object> bodyMap = JSON.parseObject(body);
            requestVo.setBody(bodyMap);
        }

        JSONArray headers = config.getHeaders();
        if (headers != null) {
            List<StrMapEntry> newHeaders = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                JSONObject headerJsonObj = headers.getJSONObject(i);
                StrMapEntry header = new StrMapEntry();
                header.setKey(headerJsonObj.getString("key"));
                header.setValue(headerJsonObj.getString("value"));
                newHeaders.add(header);
            }
            requestVo.setHeaders(newHeaders);
        }

        Triple<JsonResult<RestTestResponseVo>, FindDataListUtils.SearchResult, String> resultSearchResultTriple = doSearchRestField(requestVo);
        if (resultSearchResultTriple.getLeft() != null) {
            return resultSearchResultTriple.getLeft();
        }
        FindDataListUtils.SearchResult rs = resultSearchResultTriple.getMiddle();
        if (CollectionUtils.isEmpty(rs.list) || !rs.dataInList) {
            log.warn("dataListNotFound: {}, from: {}", JsonUtil.toJson(rs), resultSearchResultTriple.getRight());
            return new JsonResult<>(404, I18nUtils.getMessage("uns.rest.data404"));
        }

        List<Map<String, Object>> dataList = rs.list;
        List<FieldDefine> fieldDefineList = Arrays.stream(fieldDefines)
                .filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV) && f.getIndex() != null)
                .collect(Collectors.toList());
        List<Map<String, Object>> payload = dataList.stream().map(data -> {
            Map<String, Object> values = new HashMap<>(data.size());
            fieldDefineList.forEach(field -> {
                values.put(field.getName(), data.get(field.getName()));
            });
            return values;
        }).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(payload)) {
            try {
                JSONObject msg = new JSONObject();
                msg.put(Constants.MSG_RAW_DATA_KEY, JsonUtil.fromJson(resultSearchResultTriple.getRight()));
                msg.put(Constants.MSG_RES_DATA_KEY, payload);

                log.info("send message [{}] to sourceTopic[{}]", msg, topic);
                mqttPublisher.publishMessage(topic, com.alibaba.fastjson2.JSON.toJSONBytes(msg), 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return new JsonResult<>(0, "success");
    }

    private static final void appendKeyValueParam(StringBuilder builder, StrMapEntry entry) {
        builder.append(URLEncodeUtil.encode(entry.getKey(), StandardCharsets.UTF_8)).append('=')
                .append(URLEncodeUtil.encode(entry.getValue(), StandardCharsets.UTF_8));
    }

    private static final void appendKeyValueParam(StringBuilder builder, String key, String value) {
        builder.append(URLEncodeUtil.encode(key, StandardCharsets.UTF_8)).append('=')
                .append(URLEncodeUtil.encode(value, StandardCharsets.UTF_8));
    }

    public PageResultDTO<TemplateSearchResult> templatePageList(TemplateQueryVo params) {
        Page<UnsPo> page = new Page<>(params.getPageNo(), params.getPageSize());
        LambdaQueryWrapper<UnsPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UnsPo::getPathType, 1).eq(UnsPo::getDataType, 0);
        queryWrapper.like(StrUtil.isNotBlank(params.getKey()), UnsPo::getPath, params.getKey());
        IPage<UnsPo> iPage = unsMapper.selectPage(page, queryWrapper);
        PageResultDTO.PageResultDTOBuilder<TemplateSearchResult> pageBuilder = PageResultDTO.<TemplateSearchResult>builder()
                .total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        List<TemplateSearchResult> list = iPage.getRecords().stream()
                .map(uns -> BeanUtil.copyProperties(uns, TemplateSearchResult.class))
                .collect(Collectors.toList());
        return pageBuilder.code(0).data(list).build();
    }

    public TemplateVo getTemplateById(String id) {
        UnsPo po = unsMapper.selectById(id);
        if (po == null) {
            return null;
        }
        TemplateVo dto = new TemplateVo();
        dto.setId(po.getId());
        dto.setAlias(po.getAlias());
        dto.setPath(po.getPath());
        dto.setCreateTime(getDatetime(po.getCreateAt()));
        dto.setDescription(po.getDescription());
        FieldDefineVo[] fs = getFields(po.getFields());
        if (fs != null) {
            for (FieldDefineVo f : fs) {
                f.setIndex(null);// 模型的定义 给前端消除掉 index
            }
        }
        dto.setFields(fs);
        //模板引用的模型和实例列表
        List<UnsPo> templateRefs = unsMapper.selectList(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getModelId, id));
        if (!CollectionUtils.isEmpty(templateRefs)) {
            List<FileVo> fileList = templateRefs.stream().map(uns -> {
                FileVo fileVo = new FileVo();
                fileVo.setUnsId(uns.getId());
                fileVo.setPathType(uns.getPathType());
                fileVo.setPath(uns.getPath());
                fileVo.setName(PathUtil.getName(uns.getPath()));
                return fileVo;
            }).collect(Collectors.toList());
            dto.setFileList(fileList);
        }
        return dto;
    }


    public ResultVO<Integer> checkDuplicationName(String folderPath, String name, int checkType) {
        String path = name;
        long count = 0;
        if (checkType == 1) {
            // 校验文件夹名称是否已存在
            if (org.apache.commons.lang3.StringUtils.isNotBlank(folderPath)) {
                path = folderPath + name + "/";
            } else {
                path = name + "/";
            }
            String folderId = genIdForPath(path);
            count = unsMapper.selectCount(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 0).eq(UnsPo::getId, folderId));
        } else if (checkType == 2) {
            // 校验文件名称是否已存在
            if (org.apache.commons.lang3.StringUtils.isNotBlank(folderPath)) {
                path = folderPath + name;
            }
            String fileId = genIdForPath(path);
            count = unsMapper.selectCount(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2).eq(UnsPo::getId, fileId));
        }

        return ResultVO.successWithData(count);
    }
}
