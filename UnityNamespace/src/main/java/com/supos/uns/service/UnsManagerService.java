package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.NodeType;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.enums.TimeUnits;
import com.supos.common.enums.WebhookSubscribeEvent;
import com.supos.common.event.*;
import com.supos.common.event.multicaster.EventStatusAware;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.*;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.bo.CreateModelInstancesArgs;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.bo.UnsBo;
import com.supos.uns.dao.mapper.AlarmMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.WebhookUtils;
import com.supos.uns.vo.*;
import com.supos.webhook.WebhookDataPusher;
import com.supos.webhook.dto.WebhookDataDTO;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.supos.common.vo.FieldDefineVo.convert;

@Slf4j
@Service
public class UnsManagerService extends ServiceImpl<UnsMapper, UnsPo> {
    static final Validator validator;
    static final Pattern ALIAS_PATTERN;

    @Resource
    private UnsAttachmentService unsAttachmentService;
    @Autowired
    AlarmMapper alarmMapper;
    @Autowired
    private WebhookDataPusher webhookDataPusher;
    @Resource
    private UnsLabelService unsLabelService;
    @Resource
    private AlarmService alarmService;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        ALIAS_PATTERN = Pattern.compile(Constants.ALIAS_REG);
    }


    /**
     * 创建模型和实例 -- excel 导入发起
     *
     * @param topicList
     * @param flowName
     * @param statusConsumer
     * @return Map &lt; key=数组index, value=ErrTip &gt;
     */
    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public Map<String, String> createModelAndInstance(List<CreateTopicDto> topicList, Map<String, String[]> labelsMap, String flowName, Consumer<RunningStatus> statusConsumer) {
        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.topics = topicList;
        args.fromImport = true;
        args.throwModelExistsErr = true;
        args.flowName = flowName;
        args.statusConsumer = statusConsumer;
        args.labelsMap = labelsMap;

        return createModelAndInstancesInner(args);
    }

    /**
     * 创建模型和实例 -- 前端界面创建单个实例发起
     *
     * @param dto
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public BaseResult createModelInstance(CreateModelInstanceVo dto) {

        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            StringBuilder er = new StringBuilder(128);
            addValidErrMsg(er, violations);
            return new BaseResult(400, er.toString());
        }
        ArrayList<CreateTopicDto> topicDtos = new ArrayList<>(2);
        String topic = dto.getTopic();
        String alias = dto.getAlias();
        String modelDesc = dto.getModelDescription(), insDesc = dto.getInstanceDescription();
        if (topic.endsWith("/")) {
            // 创建文件夹
            CreateTopicDto modelDto = new CreateTopicDto(topic, alias, null, convert(dto.getFields()), modelDesc);
            modelDto.setBatch(0);
            modelDto.setIndex(0);
            modelDto.setModelId(dto.getModelId());
            modelDto.setAddFlow(dto.getAddFlow());
            modelDto.setAddDashBoard(dto.getAddDashBoard());
            modelDto.setSave2db(dto.getSave2db());
            topicDtos.add(modelDto);
        } else {
            // 创建文件
            CreateTopicDto instanceDto = new CreateTopicDto(topic, alias, insDesc, dto.getProtocol())
                    .setDataPath(dto.getDataPath())
                    .setCalculation(dto.getRefers(), dto.getExpression())
                    .setStreamCalculation(dto.getReferTopic(), dto.getStreamOptions());
            instanceDto.setBatch(0);
            instanceDto.setIndex(0);
            instanceDto.setModelId(dto.getModelId());
            instanceDto.setFields(convert(dto.getFields()));
            instanceDto.setDataType(dto.getDataType());
            instanceDto.setAddFlow(dto.getAddFlow());
            instanceDto.setAddDashBoard(dto.getAddDashBoard());
            instanceDto.setSave2db(dto.getSave2db());
            instanceDto.setReferTopics(dto.getReferTopics());
            instanceDto.setFrequency(dto.getFrequency());
            topicDtos.add(instanceDto);
        }
        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.topics = topicDtos;
        args.fromImport = false;
        args.throwModelExistsErr = true;
        if (dto.getDataType() != null && (dto.getDataType() == Constants.CALCULATION_HIST_TYPE || dto.getDataType() == Constants.CALCULATION_REAL_TYPE)) {
            args.topics = topicDtos.stream().map(topicDto -> {
                topicDto.setAddFlow(false);
                return topicDto;
            }).collect(Collectors.toList());
        }
        if (dto.getSave2db() != null) {
            args.topics = topicDtos.stream().map(topicDto -> {
                topicDto.setSave2db(dto.getSave2db());
                return topicDto;
            }).collect(Collectors.toList());
        }
        Map<String, String> rs = createModelAndInstancesInner(args);
        BaseResult result = new BaseResult(0, "ok");
        if (rs != null && !rs.isEmpty()) {
            result.setCode(400);
            result.setMsg(rs.values().toString());
        }
        return result;
    }

    /**
     * 检测是否引用计算实例和流程
     *
     * @param alias
     * @param fields
     * @return
     */
    public ResultVO detectIfFieldReferenced(String alias, FieldDefineVo[] fields) {
        UnsPo uns = baseMapper.getByAlias(alias);
//        String[] errors = new String[1];
//        FieldDefine[] autoFilledFields = processFieldDefines(uns.getPath(), uns.getDataType(), uns.getDataSrcId(), fields, errors, true);
//        if (StringUtils.hasText(errors[0])) {
//            throw new BuzException(errors[0]);
//        }
        Map<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("referred", false);

        if (uns.getPathType() == 0) {
            return ResultVO.successWithData(dataMap);
        }
        // 查询模型下的实例, 以便后续修改实例的表结构
        List<UnsPo> instances = baseMapper.listInstancesByModel(uns.getId());
        if (instances.isEmpty()) {
            return ResultVO.successWithData(dataMap);
        }
        List<FieldDefine> oldFields = JSON.parseArray(uns.getFields(), FieldDefine.class);
        // 筛选出新增和删除的属性集合
//        List<FieldDefine> inputFields = new ArrayList<>(Arrays.asList(autoFilledFields));
        List<FieldDefine> inputFields = Arrays.stream(fields).map(f -> BeanUtil.copyProperties(f, FieldDefine.class)).collect(Collectors.toList());
        List<FieldDefine> delFields = oldFields.stream().filter(e -> !inputFields.contains(e)).collect(Collectors.toList());
        // 检测计算实例是否有引用
        List<UnsPo> calcInsts = detectReferencedCalcInstance(instances, uns.getPath(), delFields, false);
        // 检测node-red流程是否引用了
        List<String> instanceTopics = instances.stream().map(UnsPo::getPath).collect(Collectors.toList());
        // 筛选出告警实例
        List<String> alarms = calcInsts.stream().filter(i -> i.getDataType() == 5).map(UnsPo::getPath).collect(Collectors.toList());

        boolean bingo = detectReferencedNodeRed(instanceTopics);
        int tipFlow = bingo ? 1 : 0;
        int tipCalc = calcInsts.isEmpty() ? 0 : 2;
        int tipAlarm = alarms.isEmpty() ? 0 : 4;
        String tips = I18nUtils.getMessage("uns.update.field.tips" + (tipFlow + tipCalc + tipAlarm));
        dataMap.put("referred", (tipFlow + tipCalc + tipAlarm) > 0);
        dataMap.put("tips", tips);

        return ResultVO.successWithData(dataMap);
    }

    private boolean detectReferencedNodeRed(List<String> instanceTopics) {
        HttpRequest clientRequest = HttpUtil.createPost("http://localhost:8080/service-api/supos/flow/by/topics");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        clientRequest.addHeaders(headers);
        clientRequest.timeout(3000);
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("topics", instanceTopics);
        clientRequest.body(JSON.toJSONString(bodyMap));
        HttpResponse response = clientRequest.execute();
        if (response.getStatus() == 200) {
            ResultDTO<List<NodeFlowDTO>> resultDTO = JSON.parseObject(response.body(), ResultDTO.class);
            return resultDTO.getData() != null && !resultDTO.getData().isEmpty();
        }
        return false;
    }


    /**
     * 修改模型描述
     *
     * @param alias
     * @param description
     * @return
     */
    public ResultVO updateDescription(String alias, String description) {
        baseMapper.updateDescByAlias(alias, description);
        return ResultVO.success("ok");
    }

    /**
     * 添加或者删除模型字段
     *
     * @param alias
     * @param fields
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public ResultVO updateFields(String alias, FieldDefineVo[] fields) {
        UnsPo uns = baseMapper.getByAlias(alias);

        // 统计数字类型的字段个数，不包含系统字段
        int totalNumberField = countNumericFields(fields);

        // 自动添加系统字段 _ct _qos _id
//        String[] errors = new String[1];
//        FieldDefine[] autoFilledFields = processFieldDefines(uns.getPath(), uns.getDataType(), uns.getDataSrcId(), fields, errors, true);
//        if (StringUtils.hasText(errors[0])) {
//            throw new BuzException(errors[0]);
//        }

        // 如果修改虚拟路径，走创建模型流程
        if (uns.getPathType() == 0 && uns.getModelId() == null) {
            baseMapper.updateModelFieldsById(uns.getId(), JSON.toJSONString(fields), totalNumberField);
            return ResultVO.successWithData(uns.getId());
        }

//        List<FieldDefine> inputFields = new ArrayList<>(Arrays.asList(autoFilledFields));
        List<FieldDefine> inputFields = Arrays.stream(fields).map(f -> BeanUtil.copyProperties(f, FieldDefine.class)).collect(Collectors.toList());
        //过滤掉_开头的系统字段
        List<FieldDefine> oldFields = JSON.parseArray(uns.getFields(), FieldDefine.class).stream()
                .filter(i -> !i.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).collect(Collectors.toList());
        // 筛选出新增和删除的属性集合
        List<FieldDefine> delFields = oldFields.stream().filter(e -> !inputFields.contains(e)).collect(Collectors.toList());

        // 查询模型下的实例, 以便后续修改实例的表结构
        List<UnsPo> instances = baseMapper.listInstancesByModel(uns.getId());

        // 如果有字段删除，需要同时删除关联的计算实例
        List<UnsPo> delInstList = null; // 包含计算和告警实例
        List<String> delAlarmTopics = null; // 告警实例
        if (!delFields.isEmpty()) {
            delInstList = detectReferencedCalcInstance(instances, uns.getPath(), delFields, true);
            delAlarmTopics = delInstList.stream().filter(i -> i.getDataType() == 5).map(UnsPo::getPath).collect(Collectors.toList());
        }
        // 筛选出告警实例，然后删除告警历史数据
        if (delAlarmTopics != null && !delAlarmTopics.isEmpty()) {
            alarmMapper.deleteByTopics(delAlarmTopics);
            log.info("---批量删除关联告警数据, topic为： {}", delAlarmTopics);
        }

        String fieldsString = JsonUtil.toJson(inputFields);
        // 更新模型字段到数据库
        baseMapper.updateModelFieldsById(uns.getId(), fieldsString, totalNumberField);
        if (!instances.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (UnsPo inst : instances) {
                Map<String, FieldDefineVo> fieldDefineMap = JSONArray.parseArray(inst.getFields(), FieldDefineVo.class).stream().collect(Collectors.toMap(FieldDefineVo::getName, Function.identity()));
                inputFields.forEach(in -> {
                    FieldDefineVo fieldDefine = fieldDefineMap.get(in.getName());
                    if (null != fieldDefine) {
                        in.setIndex(fieldDefine.getIndex());
                    }
                });
                fieldsString = JsonUtil.toJson(inputFields);
                inst.setFields(fieldsString);
                ids.add(inst.getId());
            }
            // 更新实例的fields字段
            baseMapper.updateInstanceFieldsByIds(ids, fieldsString, totalNumberField);
        }

        // 更新td或者pg的实例表字段(同create事件)
        sendEvent(instances, SrcJdbcType.getById(uns.getDataSrcId()), delInstList);
        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(instances, fieldsString);
        if (!webhookData.isEmpty()) {
            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_FIELD_CHANGE, webhookData, false);
        }
        return ResultVO.success("ok");
    }

    private String simpleCreateModel(UnsPo uns, FieldDefineVo[] fields, int numberFields) {
        UnsPo po = new UnsPo();
        String newPath = uns.getPath().endsWith("/") ? (uns.getPath() + "*") : (uns.getPath() + "/*");
        String alias = PathUtil.generateAlias(newPath, 0);
        String newId = genIdForPath(newPath);
        po.setId(newId);
        po.setAlias(alias);
        po.setDataPath(uns.getDataPath());
        po.setNumberFields(numberFields);
        po.setFields(JSON.toJSONString(fields));
        po.setPathType(1);
        po.setPath(newPath);
        po.setDataType(uns.getDataType());
        po.setDescription(uns.getDescription());
        po.setDataSrcId(uns.getDataSrcId());
        baseMapper.insert(po);
        return po.getId();
    }

    private void sendEvent(List<UnsPo> instances, SrcJdbcType jdbcType, List<UnsPo> delCalcInst) {
        // 发送 delete计算实例事件
        if (delCalcInst != null && delCalcInst.size() > 0) {
            Map<String, SimpleUnsInstance> calcInstances = new HashMap<>();
            for (UnsPo po : delCalcInst) {
                SimpleUnsInstance sui = new SimpleUnsInstance(po.getPath(), po.getAlias(), po.getTableName(), po.getDataType(), false);
                calcInstances.put(po.getPath(), sui);
            }
            RemoveTopicsEvent removeTopicsEvent = new RemoveTopicsEvent(this, timeDataType, calcInstances, false, false, null);
            EventBus.publishEvent(removeTopicsEvent);
        }
        // 发送create事件，修改pg或者td的table schema
        if (instances.size() > 0) {
            HashMap<SrcJdbcType, CreateTopicDto[]> topics = new HashMap<>();
            CreateTopicDto[] createTopicArray = new CreateTopicDto[instances.size()];
            for (int i = 0; i < instances.size(); i++) {
                createTopicArray[i] = po2dto(instances.get(i), "");
                createTopicArray[i].setAddFlow(false);
                createTopicArray[i].setAddDashBoard(false);
                createTopicArray[i].setFlags(null); // 跳过创建流程和dashboard
            }
            topics.put(jdbcType, createTopicArray);
            BatchCreateTableEvent event = new BatchCreateTableEvent(this, false, topics);
            EventBus.publishEvent(event);
        }
    }

    private List<UnsPo> detectReferencedCalcInstance(List<UnsPo> instances, String modelPath, List<FieldDefine> delFields, boolean isDelete) {
        List<String> instanceTopics = new ArrayList<>();
        // 存放模型关联的计算实例ID
        Set<String> calcInstanceIds = new HashSet<>();
        instances.stream().forEach(refU -> {
            instanceTopics.add(refU.getPath());
            if (StringUtils.hasText(refU.getRefUns())) {
                JSONObject jsonObject = JSON.parseObject(refU.getRefUns());
                // 提取ref_uns的topic，并根据topic计算其对应的id
                Set<String> instanceIds = jsonObject.keySet().stream().map(k -> genIdForPath(k)).collect(Collectors.toSet());
                calcInstanceIds.addAll(instanceIds);
            }
        });
        // 批量查询引用计算实例,再根据refers字段判断删除的属性是否包含其中
        if (!calcInstanceIds.isEmpty()) {
            List<UnsPo> refersList = baseMapper.listInstancesById(calcInstanceIds);
            // 过滤出先要删除的计算实例ID
            List<UnsPo> calcUns = filterAssociatedCalcInstanceIds(delFields, instanceTopics, refersList);
            if (!calcUns.isEmpty()) {
                if (isDelete) {
                    List<String> calcInstIds = calcUns.stream().map(UnsPo::getId).collect(Collectors.toList());
                    baseMapper.deleteByIds(calcInstIds);
                    log.info("--- 批量删除关联计算实例, 模型名称为： {}", modelPath);
                }
                return calcUns;
            }
        }
        return new ArrayList<>();
    }

    private int countNumericFields(FieldDefineVo[] fields) {
        int total = 0;
        for (FieldDefineVo f : fields) {
            if (f.getType().equals(FieldType.LONG.getName().toLowerCase())
                    || f.getType().equals(FieldType.INT.getName().toLowerCase())
                    || f.getType().equals(FieldType.FLOAT.getName().toLowerCase())
                    || f.getType().equals(FieldType.DOUBLE.getName().toLowerCase())) {
                total++;
            }
        }
        return total;
    }

    private List<UnsPo> filterAssociatedCalcInstanceIds(List<FieldDefine> delFields, List<String> instanceTopics, List<UnsPo> refers) {
        List<UnsPo> calcInst = new ArrayList<>();
        for (UnsPo referPO : refers) {
            // refers数据结构为[{"field":"f1","topic":"/a/b"}]
            if (!StringUtils.hasText(referPO.getRefers())) {
                continue;
            }
            JSONArray jsonArray = JSON.parseArray(referPO.getRefers());
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject referObj = jsonArray.getJSONObject(i);
                String t = referObj.getString("topic");
                String f = referObj.getString("field");
                if (t != null && f != null && instanceTopics.contains(t)) {
                    for (FieldDefine ff : delFields) {
                        if (ff.getName().equals(f)) {
                            calcInst.add(referPO);
                            break;
                        }
                    }
                }
            }
        }
        return calcInst;
    }


    private static void addValidErrMsg(StringBuilder er, Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> v : violations) {
            String t = v.getRootBeanClass().getSimpleName();
            er.append('[').append(t).append('.').append(v.getPropertyPath()).append(' ').append(v.getMessage()).append(']');
        }
    }

    /**
     * 自测批量导入
     *
     * @param topicDtos
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public Map<String, String> createModelAndInstance(List<CreateTopicDto> topicDtos) {
        final int TASK_ID = System.identityHashCode(topicDtos);
        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.topics = topicDtos;
        args.fromImport = true;
        args.throwModelExistsErr = false;
        args.flowName = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        args.statusConsumer = runningStatus -> {
            Long spend = runningStatus.getSpendMills();
            if (spend != null) {
                Integer i = runningStatus.getI(), n = runningStatus.getN();
                log.info("[{}] {}/{} 已处理， {}：耗时{} ms", TASK_ID, i, n, runningStatus.getTask(), spend);
            }
        };
        AtomicInteger index = new AtomicInteger(0);
        args.topics = topicDtos.stream().map(topicDto -> {
            topicDto.setBatch(0);
            topicDto.setIndex(index.getAndIncrement());
            topicDto.setAddFlow(true);
            topicDto.setAddDashBoard(true);
            return topicDto;
        }).collect(Collectors.toList());

        Map<String, String[]> labelsMap = new HashMap<>();
        topicDtos.stream().filter(topicDto -> ObjectUtil.isNotEmpty(topicDto.getLabelNames())).forEach(topicDto -> {
            labelsMap.put(topicDto.getTopic(), topicDto.getLabelNames());
        });
        args.labelsMap = labelsMap;

        Map<String, String> rs = createModelAndInstancesInner(args);
        log.info("[{}] 处理完毕.", TASK_ID);
        return rs;
    }


    @Transactional(rollbackFor = Throwable.class)
    public RemoveResult removeModelOrInstance(String path, boolean withFlow, boolean withDashboard, Boolean removeRefer) {
        final char endChar = path.charAt(path.length() - 1);
        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().in("path_type", 1, 2);
        QueryWrapper<UnsPo> removeQuery = new QueryWrapper<>();
        if (endChar == '/' || endChar == '*') {//按 模型 或模型的某个层级 或 按通配符 删除
            if (endChar == '*') {
                path = path.substring(0, path.length() - 1);
            }
            removeQuery.likeRight("path", path);
            queryWrapper = queryWrapper.likeRight("path", path);
        } else {//只删除单个实例
            removeQuery.eq("path", path);
            queryWrapper = queryWrapper.eq("path", path);
        }

        List<UnsPo> unsPos = this.list(queryWrapper.select("path", "path_type", "data_src_id", "alias", "ref_uns", "refers", "data_type", "table_name", "with_flags"));
        RemoveResult removeResult = getRemoveResult(withFlow, withDashboard, removeRefer, removeQuery, unsPos);
        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(unsPos);
        if (!webhookData.isEmpty()) {
            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_DELETE, webhookData, false);
        }
        return removeResult;
    }

    private RemoveResult getRemoveResult(boolean withFlow, boolean withDashboard, Boolean removeRefer, QueryWrapper<UnsPo> removeQuery, List<UnsPo> unsPos) {
        RemoveResult rs = new RemoveResult();
        if (!CollectionUtils.isEmpty(unsPos)) {
            HashMap<SrcJdbcType, TopicBaseInfoList> typeListMap = new HashMap<>();
            HashMap<String, UnsPo> calcTopics = new HashMap<>();
            HashSet<String> allTopics = new HashSet<>(unsPos.size());
            ArrayList<String> alarmTopics = new ArrayList<>();
            Iterator<UnsPo> itr = unsPos.iterator();
            Map<SrcJdbcType, LinkedList<String>> modelTopics = new HashMap<>();
            List<String> aliases = new ArrayList<>(unsPos.size());
            while (itr.hasNext()) {
                UnsPo po = itr.next();
                if (po.getPathType() == 1) {
                    SrcJdbcType jdbcType = SrcJdbcType.getById(po.getDataSrcId());
                    String topicPath = po.getPath();
                    if (topicPath.endsWith("/*")) {
                        topicPath = topicPath.substring(0, topicPath.length() - 2);
                    }
                    modelTopics.computeIfAbsent(jdbcType, k -> new LinkedList<>()).add(topicPath);
                    itr.remove();
                    continue;
                }
                addPo4Remove(typeListMap, po);
                allTopics.add(po.getPath());
                aliases.add(po.getAlias());
                if (po.getRefers() != null) {
                    calcTopics.put(po.getPath(), po);
                }
                if (ObjectUtil.equal(po.getDataType(), Constants.ALARM_RULE_TYPE)) {
                    alarmTopics.add(po.getPath());
                }
            }
            if (removeRefer == null) {
                for (UnsPo po : unsPos) {
                    String refUns = po.getRefUns();
                    if (refUns != null && refUns.length() > 5) {
                        JSONObject object = JSON.parseObject(refUns);
                        for (Object topic : object.keySet()) {
                            if (!calcTopics.containsKey(topic)) {
                                RemoveResult.RemoveTip tip = new RemoveResult.RemoveTip();
                                tip.setRefs(object.size());
                                rs.setData(tip);
                                return rs;
                            }
                        }
                    }
                }
            } else if (removeRefer) {
                List<String> incQueryCalcTopics = new ArrayList<>(unsPos.size());
                for (UnsPo po : unsPos) {

                    String refUns = po.getRefUns();
                    if (refUns != null && refUns.length() > 5) {
                        JSONObject object = JSON.parseObject(refUns);
                        for (Object topic : object.keySet()) {
                            if (!calcTopics.containsKey(topic)) {
                                incQueryCalcTopics.add(genIdForPath(topic.toString()));
                            }
                        }
                    }
                }
                if (!incQueryCalcTopics.isEmpty()) {
                    List<UnsPo> list = this.listByIds(incQueryCalcTopics);
                    List<Pair<String, String>> unRefList = new ArrayList<>();
                    for (UnsPo po : list) {
                        addPo4Remove(typeListMap, po);
                        addRefer(allTopics, unRefList, po);
                    }
                    if (!unRefList.isEmpty()) {
                        removeCalcRef(unRefList);
                    }
                    removeQuery.or(p -> p.in("id", incQueryCalcTopics));
                }
            }
            // 删除计算实例本身，对引用的实例解除引用
            List<Pair<String, String>> unRefList = new ArrayList<>();
            for (UnsPo calcPo : calcTopics.values()) {
                addRefer(allTopics, unRefList, calcPo);
            }
            if (!unRefList.isEmpty()) {
                removeCalcRef(unRefList);
            }
            if (!typeListMap.isEmpty()) {
                for (Map.Entry<SrcJdbcType, TopicBaseInfoList> entry : typeListMap.entrySet()) {
                    SrcJdbcType srcJdbcType = entry.getKey();
                    TopicBaseInfoList v = entry.getValue();
                    LinkedList<String> mps = modelTopics.get(srcJdbcType);
                    RemoveTopicsEvent event = new RemoveTopicsEvent(this, srcJdbcType, v.topics, withFlow, withDashboard, mps);
                    EventBus.publishEvent(event);
                }
            } else if (!modelTopics.isEmpty()) {
                for (Map.Entry<SrcJdbcType, LinkedList<String>> entry : modelTopics.entrySet()) {
                    SrcJdbcType srcJdbcType = entry.getKey();
                    LinkedList<String> mps = entry.getValue();
                    RemoveTopicsEvent event = new RemoveTopicsEvent(this, srcJdbcType, Collections.emptyMap(), withFlow, withDashboard, mps);
                    EventBus.publishEvent(event);
                }
            }
            if (!alarmTopics.isEmpty()) {
                for (List<String> alarmTopic : Lists.partition(alarmTopics, Constants.SQL_BATCH_SIZE)) {
                    log.info("删除告警数据：topic = {}", alarmTopic);
                    alarmMapper.delete(new QueryWrapper<AlarmPo>().in(AlarmRuleDefine.FIELD_TOPIC, alarmTopic));
                }
            }
            unsAttachmentService.delete(aliases);
        }
        this.remove(removeQuery);

        return rs;
    }

    static class TopicBaseInfoList {
        Map<String, SimpleUnsInstance> topics = new LinkedHashMap<>();
    }

    private void addRefer(HashSet<String> allTopics, List<Pair<String, String>> unRefList, UnsPo po) {
        InstanceField[] refs = JsonUtil.fromJson(po.getRefers(), InstanceField[].class);
        Set<String> refTopics = Arrays.stream(refs).filter(Objects::nonNull).map(InstanceField::getTopic).collect(Collectors.toSet());
        String calcTopic = po.getPath();
        for (String ref : refTopics) {
            if (!allTopics.contains(ref)) {
                // 要删除的计算实例 引用的其他范围的实例，需要解除到计算实例的引用
                unRefList.add(Pair.of(genIdForPath(ref), calcTopic));
            }
        }
    }

    private void removeCalcRef(List<Pair<String, String>> unRefList) {
        this.executeBatch(unRefList, (session, pair) -> {
            UnsMapper mapper = session.getMapper(UnsMapper.class);
            String id = pair.getLeft(), calcTopic = pair.getRight();
            mapper.removeRefUns(id, Collections.singletonList(calcTopic));
        });
    }

    private void addPo4Remove(HashMap<SrcJdbcType, TopicBaseInfoList> typeListMap, UnsPo po) {
        SrcJdbcType jdbcType = SrcJdbcType.getById(po.getDataSrcId());
        TopicBaseInfoList list = typeListMap.computeIfAbsent(jdbcType, k -> new TopicBaseInfoList());
        SimpleUnsInstance instance = new SimpleUnsInstance(po.getPath(), po.getAlias(), po.getTableName(), po.getDataType(), !Constants.withRetainTableWhenDeleteInstance(po.getWithFlags()));
        list.topics.put(instance.getTopic(), instance);
    }

    private SrcJdbcType relationType = SrcJdbcType.Postgresql, timeDataType;

    Map<String, String> createModelAndInstancesInner(final CreateModelInstancesArgs args) {
        log.info("createModelAndInstances args:{}", args);
        List<CreateTopicDto> topicDtos = args.topics;
        Map<String, String[]> labelsMap = args.labelsMap != null ? args.labelsMap : Collections.emptyMap();
        Set<String> aliasSet = new HashSet<>(topicDtos.size());
        for (CreateTopicDto dto : topicDtos) {
            dto.setAlias(dto.getAlias() != null ? dto.getAlias().trim() : null);// trim alias, Excel 导入时可能含有空格
            if (dto.getAlias() != null) {
                aliasSet.add(dto.getAlias());
            }
        }
        final Map<String, String> errTipMap = new HashMap<>(topicDtos.size());
        final HashMap<String, UnsPo> folderTemp = new HashMap<>();
        HashMap<String, UnsBo> paramFiles = new HashMap<>(topicDtos.size());
        Map<String, UnsBo> paramFolders = initParamsUns(topicDtos, errTipMap, paramFiles);
        if (paramFolders.isEmpty() && paramFiles.isEmpty()) {
            log.warn("不存在任何文件夹或文件, 无法继续保存");
            return errTipMap;
        }
        Set<String> templateIds = new HashSet<>();
        final LinkedList<UnsPo> list = new LinkedList<>();
        final LinkedList<UnsPo> updateList = new LinkedList<>();
        final Map<String, String[]> addLabelMap = new HashMap<>();
        // 收集提交的文件夹、模板便于后续存在校验
        Collection<String> paramFolderIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(paramFiles)) {
            paramFolderIds.addAll(paramFiles.values().stream().map(file -> {
                //收集文件所属文件夹
                String folderId = file.getFolderId();
                if (folderId == null) {
                    String path = file.getPath();
                    int lastSp = path.lastIndexOf('/');
                    if (lastSp > 0) {
                        String folderPath = path.substring(0, lastSp + 1);
                        folderId = genIdForPath(folderPath);
                    }
                    file.setFolderId(folderId);
                }
                // 收集文件模板
                if (file.getModelId() != null) {
                    templateIds.add(file.getModelId());
                }
                return folderId;
            }).collect(Collectors.toSet()));
        }
        if (!CollectionUtils.isEmpty(paramFolders)) {
            //收集文件夹和文件夹的模板
            paramFolderIds.addAll(paramFolders.values().stream().map(folder -> {
                if (folder.getModelId() != null) {
                    templateIds.add(folder.getModelId());
                }
                return folder.getId();
            }).collect(Collectors.toSet()));
        }

        checkAndUpdateCalcRefInstance(errTipMap, paramFiles);

        Map<String, UnsPo> existsFoldersById = this.listFolders(paramFolderIds);
        Set<String> instances = !paramFiles.isEmpty() ? this.listInstances(paramFiles.keySet()) : Collections.emptySet();
        Map<String, UnsPo> existInstancesByAlias = !aliasSet.isEmpty() ? this.listInstancesByAlias(aliasSet) : Collections.emptyMap();
        Map<String, UnsPo> existFoldersByAlias = !aliasSet.isEmpty() ? this.listFoldersByAlias(aliasSet) : Collections.emptyMap();
        Map<String, UnsPo> existTemplates = !templateIds.isEmpty() ? queryByIds(templateIds) : Collections.emptyMap();
        HashMap<SrcJdbcType, ArrayList<CreateTopicDto>> topics = new HashMap<>(2);
        LinkedList<CreateTopicDto> calcUpdateList = null;
        for (UnsBo bo : paramFiles.values()) {
            String path = bo.getPath();
            String alias = bo.getAlias();
            String batchIndex = bo.gainBatchIndex();
            String instanceId = bo.getId();

            boolean isUpdate = false;
            if (instances.contains(instanceId)) {
                if (StringUtils.hasText(bo.getExpression())) {
                    UnsPo instance = new UnsPo();
                    instance.setId(instanceId);
                    instance.setDescription(bo.getDescription());
                    if (ArrayUtil.isNotEmpty(bo.getRefers())) {
                        instance.setRefers(JsonUtil.toJson(bo.getRefers()));
                    }
                    instance.setDataPath(bo.getDataPath());
                    instance.setProtocol(JsonUtil.toJson(bo.getProtocol()));
                    instance.setExpression(bo.getExpression());
                    instance.setExtend(bo.getExtend());
                    instance.setWithFlags(bo.getWithFlags());
                    updateList.add(instance);
                    log.debug("更新计算实例: {}, 表达式: {}", path, bo.getExpression());
                    if (calcUpdateList == null) {
                        calcUpdateList = new LinkedList<>();
                    }
                    CreateTopicDto createTopicDto = buildCreateTopicDto(bo, path, alias, bo.getDataType(), bo.getDataSrcId(), bo.getFieldDefines());
                    createTopicDto.setDataSrcId(timeDataType);
                    calcUpdateList.add(createTopicDto);
                    continue;
                } else if (bo.getStreamOptions() != null) {
                    isUpdate = true;
                } else if (ArrayUtil.isNotEmpty(bo.getRefers())) {
                    UnsPo instance = new UnsPo();
                    instance.setId(instanceId);
                    instance.setDescription(bo.getDescription());
                    instance.setRefers(JsonUtil.toJson(bo.getRefers()));
                    updateList.add(instance);
                    log.debug("更新合并实例: {}, id={}, 引用: {}", path, instanceId, bo.getReferTopics());
                    if (calcUpdateList == null) {
                        calcUpdateList = new LinkedList<>();
                    }
                    CreateTopicDto createTopicDto = buildCreateTopicDto(bo, path, alias, bo.getDataType(), bo.getDataSrcId(), bo.getFieldDefines());
                    createTopicDto.setDataSrcId(SrcJdbcType.Postgresql);
                    calcUpdateList.add(createTopicDto);
                    continue;
                } else if (bo.getCompileExpression() == null) {
                    String msg = I18nUtils.getMessage("uns.instance.has.exist");
                    errTipMap.put(batchIndex, bo.getPath() + " " + msg);
                    continue;
                }
            }
            if (!isUpdate && checkAliasDuplicate(instanceId, alias, existInstancesByAlias)) {
                String msg = I18nUtils.getMessage("uns.alias.has.exist");
                errTipMap.put(batchIndex, msg);
                continue;
            }
            UnsBo folderInParam = bo.getParent();
            String folderId = bo.getFolderId();
            UnsPo folderInDB = null;
            final int DATA_TYPE = bo.getDataType();
            if (folderId != null) {
                folderInDB = existsFoldersById.get(folderId);
                if (folderInParam == null) {
                    // excel中不存在所属文件夹
                    if (folderInDB == null) {
                        // excel和数据库中不都存在所属文件夹
/*                        String folderPath = bo.getPath();
                        int lastSp = path.lastIndexOf('/');
                        if (lastSp > 0) {
                            folderPath = path.substring(0, lastSp + 1);
                        }
                        UnsPo virTopic = new UnsPo(folderPath);
                        virTopic.setPathType(0);
                        virTopic.setId(folderId);
                        virTopic.setDataType(0);
                        virTopic.setAlias(PathUtil.generateAlias(folderPath, 0));
                        list.add(virTopic);
                        ids.put(folderId, virTopic);*/
                    }
                } else {
                    // excel中存在所属文件夹
                    if (folderInDB != null) {
                        if (!isUpdate && args.throwModelExistsErr) {
                            String msg = I18nUtils.getMessage("uns.model.has.exist");
                            errTipMap.put(batchIndex, msg);
                            continue;
                        }
                    }
                }

            }

            SrcJdbcType jdbcType = bo.getDataSrcId();
            switch (DATA_TYPE) {
                case Constants.CALCULATION_HIST_TYPE:
                case Constants.CALCULATION_REAL_TYPE:
                case Constants.TIME_SEQUENCE_TYPE:
                    jdbcType = timeDataType;
                    break;
                case Constants.ALARM_RULE_TYPE:
                case Constants.RELATION_TYPE:
                case Constants.REFER_TYPE:
                    jdbcType = relationType;
                    break;
            }
            final int DATA_SRC_ID = jdbcType.id;

            UnsPo instance = new UnsPo(instanceId, path, 2, DATA_TYPE, DATA_SRC_ID, null, bo.getDescription());
            instance.setModelId(bo.getModelId());
            instance.setDataPath(bo.getDataPath());
            instance.setAlias(alias);
            instance.setTableName(bo.getTableName());
            instance.setNumberFields(bo.countNumberFields());
            instance.setExtend(bo.getExtend());
            if (ArrayUtil.isNotEmpty(bo.getRefers())) {
                instance.setRefers(JsonUtil.toJson(bo.getRefers()));
            }
            instance.setExpression(bo.getExpression());
            Map<String, Object> protocol = bo.getProtocol();
            if (protocol != null && protocol.size() > 0) {
                Object protocolType = protocol.get("protocol");
                if (protocolType != null) {
                    instance.setProtocolType(protocolType.toString());
                }
                if (!args.fromImport) {
                    instance.setProtocol(JsonUtil.toJson(protocol));
                } else {
                    instance.setProtocol(bo.getProtocolBean() != null ? JsonUtil.toJson(bo.getProtocolBean()) : null);
                }
            }
            FieldDefine[] insFs = bo.getFieldDefines();//
            if (ArrayUtil.isNotEmpty(insFs)) {
                String[] err = new String[1];
                insFs = FieldUtils.processFieldDefines(path, jdbcType, insFs, err, true);
                if (err[0] != null) {
                    errTipMap.put(batchIndex, err[0]);
                    continue;
                }
                instance.setFields(JsonUtil.toJson(insFs));
            } else {
                insFs = null;
            }
            if (bo.getModelId() != null) {
                UnsPo template = existTemplates.get(bo.getModelId());
                if (template != null) {
                    FieldDefine[] fields = JsonUtil.fromJson(template.getFields(), FieldDefine[].class);
                    if (ArrayUtil.isNotEmpty(insFs)) {
                        String checkError = checkInstanceFields(fields, insFs);
                        if (checkError != null) {
                            errTipMap.put(batchIndex, checkError);
                            continue;
                        } else {
                            instance.setFields(template.getFields());
                        }
                    } else {
                        String[] err = new String[1];
                        insFs = FieldUtils.processFieldDefines(path, jdbcType, fields, err, true);
                        if (err[0] != null) {
                            errTipMap.put(batchIndex, err[0]);
                            continue;
                        }
                        bo.setFieldDefines(insFs);
                        instance.setFields(JsonUtil.toJson(insFs));
                    }
                } else {
                    errTipMap.put(batchIndex, I18nUtils.getMessage("uns.template.not.exists"));
                    continue;
                }
            }
            if (StringUtils.isEmpty(instance.getFields())) {
                errTipMap.put(batchIndex, I18nUtils.getMessage("uns.field.empty"));
                continue;
            }
            String upFolderPath = null;
            if (folderId != null) {
                //文件挂在文件夹下
                UnsPo folder = folderTemp.get(folderId);
                if (folder == null) {
                    if (folderInParam != null) {
                        //excel中存在文件夹
                        if (folderInParam.getModelId() != null) {
                            UnsPo template = existTemplates.get(folderInParam.getModelId());
                            if (template != null) {
                                FieldDefine[] templateFields = JsonUtil.fromJson(template.getFields(), FieldDefine[].class);
                                String checkError = checkInstanceFields(templateFields, folderInParam.getFieldDefines());
                                if (checkError != null) {
                                    errTipMap.put(batchIndex, checkError);
                                    continue;
                                }
                            }
                        }
                        folder = new UnsPo(folderId, folderInParam.getPath(), 0, 0, null, JsonUtil.toJson(folderInParam.getFieldDefines()), folderInParam.getDescription());
                        folder.setModelId(folderInParam.getModelId());
                        folder.setWithFlags(folderInParam.getWithFlags());
                        folder.setAlias(folderInParam.getAlias());
                        list.add(folder);
                        folderTemp.put(folderId, folder);
                    } else if (folderInDB == null) {
                        // excel和db都不存在文件夹
                        String folderPath = bo.getPath();
                        int lastSp = path.lastIndexOf('/');
                        if (lastSp > 0) {
                            folderPath = path.substring(0, lastSp + 1);
                        }
                        folder = new UnsPo(folderPath);
                        folder.setPathType(0);
                        folder.setId(folderId);
                        folder.setDataType(0);
                        folder.setAlias(PathUtil.generateAlias(folderPath, 0));
                        list.add(folder);
                        folderTemp.put(folderId, folder);
                    }
                }

                // 所属文件夹上级文件夹
                if (folder != null && org.apache.commons.lang3.StringUtils.countMatches(folder.getPath(), '/') > 1) {
                    upFolderPath = org.apache.commons.lang3.StringUtils.substring(folder.getPath(), 0, org.apache.commons.lang3.StringUtils.lastOrdinalIndexOf(folder.getPath(), "/", 2) + 1);
                } else {
                    upFolderPath = null;
                }
            }
            instance.setWithFlags(bo.getWithFlags());
            if (labelsMap != null) {
                String[] labels = labelsMap.get(instance.getPath());
                if (labels != null) {
                    addLabelMap.put(instance.getId(), labels);
                }
            }
            list.add(instance);
            int x = upFolderPath != null ? upFolderPath.lastIndexOf('/') : -1;
            while (x > 0) {
                String xpath = path.substring(0, x + 1);
                String vid = genIdForPath(xpath);
                UnsPo p = folderTemp.get(vid);
                if (p == null) {
                    UnsPo virTopic = new UnsPo(xpath);
                    virTopic.setPathType(0);
                    virTopic.setId(vid);
                    virTopic.setDataType(0);
                    virTopic.setAlias(PathUtil.generateAlias(xpath, 0));
                    list.add(virTopic);
                    folderTemp.put(vid, virTopic);
                } else {
                    break;
                }
                x = path.lastIndexOf('/', x - 1);
            }
            CreateTopicDto createTopicDto = buildCreateTopicDto(bo, path, alias, DATA_TYPE, jdbcType, insFs);
            createTopicDto.setFlags(instance.getWithFlags());
            topics.computeIfAbsent(jdbcType, k -> new ArrayList<>(paramFiles.size())).add(createTopicDto);
        }
        if (!args.fromImport && !errTipMap.isEmpty() && list.isEmpty()) {
            log.warn("Won't save when errTips: {}", errTipMap);
            return errTipMap;
        }
        for (Map.Entry<String, UnsBo> entry : paramFolders.entrySet()) {
            String id = entry.getKey();
            UnsBo m = entry.getValue();
            UnsPo tempFolder = folderTemp.get(id);
            if (tempFolder == null) {
                String batchIndex = m.gainBatchIndex();
                if (checkAliasDuplicate(id, m.getAlias(), existFoldersByAlias)) {
                    String msg = I18nUtils.getMessage("uns.alias.has.exist");
                    errTipMap.put(batchIndex, msg);
                    continue;
                }

                if (m.getModelId() != null) {
                    UnsPo template = existTemplates.get(m.getModelId());
                    if (template != null) {
                        FieldDefine[] fields = JsonUtil.fromJson(template.getFields(), FieldDefine[].class);
                        String checkError = checkInstanceFields(fields, m.getFieldDefines());
                        if (checkError != null) {
                            errTipMap.put(batchIndex, checkError);
                            continue;
                        }
                    }
                }
                UnsPo modelPo = new UnsPo(id, m.getPath(), 0, 0, null, m.getFields(), m.getDescription());
                modelPo.setModelId(m.getModelId());
                modelPo.setWithFlags(m.getWithFlags());
                modelPo.setAlias(m.getAlias());
                list.add(modelPo);

                // 上级文件夹处理
                int x = m.getPath().lastIndexOf('/', m.getPath().lastIndexOf('/') - 1);
                while (x > 0) {
                    String xpath = m.getPath().substring(0, x + 1);
                    String vid = genIdForPath(xpath);
                    UnsPo p = folderTemp.get(vid);
                    if (p == null) {
                        UnsPo virTopic = new UnsPo(xpath);
                        virTopic.setPathType(0);
                        virTopic.setId(vid);
                        virTopic.setDataType(0);
                        virTopic.setAlias(PathUtil.generateAlias(xpath, 0));
                        list.add(virTopic);
                        folderTemp.put(vid, virTopic);
                    } else {
                        break;
                    }
                    x = m.getPath().lastIndexOf('/', x - 1);
                }
            } else {
                if (tempFolder != null) {
                    BeanUtil.copyProperties(m, tempFolder);
                }
            }
        }
        if (!args.fromImport && !errTipMap.isEmpty()) {
            log.warn("Won't save when errTips: {}", errTipMap);
            return errTipMap;
        }
        Map topicsMap = topics;
        for (Map.Entry<SrcJdbcType, ArrayList<CreateTopicDto>> entry : topics.entrySet()) {
            ArrayList<CreateTopicDto> ls = entry.getValue();
            topicsMap.put(entry.getKey(), ls.toArray(new CreateTopicDto[ls.size()]));
        }
        Map<SrcJdbcType, CreateTopicDto[]> topicsByJdbcType = topicsMap;
        Consumer<RunningStatus> statusConsumer = args.statusConsumer;
        AtomicInteger total = new AtomicInteger();
        if (!topicsByJdbcType.isEmpty()) {
            BatchCreateTableEvent event = new BatchCreateTableEvent(this, args.fromImport, topicsByJdbcType).setFlowName(args.flowName);
            if (statusConsumer != null) {
                setEventStatusCallback(statusConsumer, event, total);
            }
            UnsQueryService.batchRemoveExternalTopic(event.topics.values()); // 删除在uns已经存在的topic
            EventBus.publishEvent(event);
        }
        int countSaved = -1;
        if (!list.isEmpty() || !updateList.isEmpty()) {//保存uns
            countSaved = 0;

            if (!list.isEmpty()) {
                if (statusConsumer != null) {
                    long tStart = System.currentTimeMillis();
                    String task = I18nUtils.getMessage("uns.create.task.name.uns");
                    String startMsg = I18nUtils.getMessage("uns.create.status.running");
                    final int N = total.get() + 1;
                    statusConsumer.accept(new RunningStatus(N, N, task, startMsg).setProgress(98.0));
                    Throwable err = null;
                    try {
                        for (List<UnsPo> poList : Lists.partition(list, Constants.SQL_BATCH_SIZE)) {
                            countSaved += this.baseMapper.saveOrIgnoreBatch(poList);
                        }
                        unsLabelService.makeLabel(addLabelMap);
                    } catch (Throwable ex) {
                        err = ex;
                        throw ex;
                    } finally {
                        String msg;
                        if (err == null) {
                            msg = I18nUtils.getMessage("uns.create.status.finished");
                        } else {
                            msg = I18nUtils.getMessage("uns.create.status.error") + err.getMessage();
                        }
                        statusConsumer.accept(new RunningStatus(N, N, task, msg)
                                .setSpendMills(System.currentTimeMillis() - tStart).setCode(err == null ? 0 : 400).setProgress(99.0));
                    }
                } else {
                    for (List<UnsPo> poList : Lists.partition(list, Constants.SQL_BATCH_SIZE)) {
                        countSaved += this.baseMapper.saveOrIgnoreBatch(poList);
                    }
                    unsLabelService.makeLabel(addLabelMap);
                }
            }
            if (!updateList.isEmpty()) {
                this.updateBatchById(updateList);
            }
            if (calcUpdateList != null && calcUpdateList.size() > 0) {
                EventBus.publishEvent(new UpdateCalcInstanceEvent(this, calcUpdateList));
            }
            EventBus.publishEvent(new NamespaceChangeEvent(this));
        }

        log.info("countSaved:{} {}, errTips: {}", countSaved, list.size(), errTipMap);
        // webhook send
        List<WebhookDataDTO> webhookData = WebhookUtils.transfer(list);
        if (!webhookData.isEmpty()) {
            webhookDataPusher.push(WebhookSubscribeEvent.INSTANCE_FIELD_CHANGE, webhookData, false);
        }
        return errTipMap;
    }

    private CreateTopicDto buildCreateTopicDto(UnsBo bo, String path, String alias, int DATA_TYPE, SrcJdbcType jdbcType, FieldDefine[] fields) {
        CreateTopicDto createTopicDto = new CreateTopicDto(path, alias, DATA_TYPE, fields);
        createTopicDto.setDescription(bo.getDescription());
        createTopicDto.setProtocol(bo.getProtocol());
        if (!StringUtils.hasText(bo.getProtocolType())) {
            Object p = bo.getProtocol() == null ? null : bo.getProtocol().get("protocol");
            if (p != null) {
                createTopicDto.setProtocolType(p.toString());
            }
        } else {
            createTopicDto.setProtocolType(bo.getProtocolType());
        }
        createTopicDto.setProtocolBean(bo.getProtocolBean());
        createTopicDto.setDataPath(bo.getDataPath());
        createTopicDto.setDataSrcId(jdbcType);
        createTopicDto.setRefers(bo.getRefers());
        createTopicDto.setStreamCalculation(bo.getReferTopic(), bo.getStreamOptions());
        createTopicDto.setReferTable(bo.getReferTable());
        if (bo.getReferTopic() != null && createTopicDto.getRefFields() == null) {
            String referId = genIdForPath(bo.getReferTopic());
            UnsPo refPo = baseMapper.selectById(referId);
            if (refPo == null && bo.getReferTable() != null) {
                List<UnsPo> list = baseMapper.selectList(new QueryWrapper<UnsPo>().eq("alias", bo.getReferTable()));
                if (list != null && !list.isEmpty()) {
                    refPo = list.get(0);
                }
            }
            if (refPo != null && refPo.getFields() != null) {
                createTopicDto.setRefFields(JsonUtil.fromJson(refPo.getFields(), FieldDefine[].class));
                createTopicDto.setReferModelId(refPo.getModelId());
            }
        }
        createTopicDto.setTableName(bo.getTableName());
        createTopicDto.setFrequencySeconds(bo.getFrequencySeconds());
        createTopicDto.setModelId(bo.getModelId());
        if (bo.getDataType() == Constants.ALARM_RULE_TYPE) {
            AlarmRuleDefine ruleDefine = bo.getAlarmRuleDefine();
            if (ruleDefine == null && bo.getProtocol() != null) {
                ruleDefine = new AlarmRuleDefine();
                BeanUtil.copyProperties(bo.getProtocol(), ruleDefine);
            }
            createTopicDto.setAlarmRuleDefine(ruleDefine);
        }

        String exp = bo.getExpression();
        createTopicDto.setExpression(exp);
        Object compiledExpression = bo.getCompileExpression();
        if (compiledExpression != null) {
            createTopicDto.setCompileExpression(compiledExpression);
        } else if (StringUtils.hasText(exp)) {
            compiledExpression = ExpressionFunctions.compileExpression(exp);
            createTopicDto.setCompileExpression(compiledExpression);
        }
        createTopicDto.setFlags(bo.getWithFlags());
        return createTopicDto;
    }

    static final Set<String> streamAggregateFunctions = new HashSet<>(Arrays.asList("count", "avg", "sum", "max", "min"));

    private void checkAndUpdateCalcRefInstance(Map<String, String> errTipMap, HashMap<String, UnsBo> paramInstances) {
        Map<String, Set<UnsBo>> topicIdRefMap = Collections.EMPTY_MAP;
        Map<String, UnsBo> calcTopics = Collections.EMPTY_MAP;
        for (UnsBo ins : paramInstances.values()) {
            InstanceField[] refers = ins.getRefers();
            String batchIndex = ins.gainBatchIndex();
            if (ins.getDataType() == Constants.REFER_TYPE) {
                if (ArrayUtil.isEmpty(ins.getReferTopics())) {
                    errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.empty.referTopic"));
                    paramInstances.remove(ins.getId());
                    continue;
                }
                if (ins.getFrequencySeconds() == null) {
                    errTipMap.put(batchIndex, I18nUtils.getMessage("uns.create.empty.frequency"));
                    paramInstances.remove(ins.getId());
                    continue;
                }
            }
            if (ArrayUtil.isEmpty(refers) && ArrayUtil.isNotEmpty(ins.getReferTopics())) {
                String[] refs = ins.getReferTopics();
                refers = new InstanceField[refs.length];
                for (int i = 0; i < refs.length; i++) {
                    refers[i] = new InstanceField(refs[i], null);
                }
                ins.setRefers(refers);
            }
            if (ins.getDataType() == Constants.CALCULATION_HIST_TYPE) {
                if (ins.getStreamOptions() == null) {
                    errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.window.emptyStreamOptions"));
                    paramInstances.remove(ins.getId());
                    continue;
                }
                FieldDefine[] fieldDefines = ins.getFieldDefines();
                if (fieldDefines == null && ins.getParent() != null) {
                    fieldDefines = ins.getParent().getFieldDefines();
                }
                refers = new InstanceField[fieldDefines.length];
                String referTopic = ins.getReferTopic();
                if (!StringUtils.hasText(referTopic)) {
                    errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.empty.referTopic"));
                    paramInstances.remove(ins.getId());
                    continue;
                }
                for (int i = 0, sz = refers.length; i < sz; i++) {
                    FieldDefine define = fieldDefines[i];
                    String fun = define.getIndex();
                    int qStart = fun.indexOf('(');
                    int qEnd = fun.indexOf(')');
                    if (qStart > 0 && qEnd > qStart) {
                        String funName = fun.substring(0, qStart);
                        if (!streamAggregateFunctions.contains(funName.toLowerCase())) {
                            errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.func.invalid", funName));
                            paramInstances.remove(ins.getId());
                            break;
                        }
                        String referField = fun.substring(qStart + 1, qEnd);
                        refers[i] = new InstanceField(referTopic, referField);
                    } else {
                        errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.index.invalid"));
                        paramInstances.remove(ins.getId());
                        break;
                    }
                }
                if (!paramInstances.containsKey(ins.getId())) {
                    continue;
                }
                ins.setRefers(refers);
            }
            if (ArrayUtil.isNotEmpty(refers)) {
                if (calcTopics == Collections.EMPTY_MAP) {
                    calcTopics = new HashMap<>(Math.max(16, paramInstances.size()));
                }
                calcTopics.put(ins.getPath(), ins);
                if (topicIdRefMap == Collections.EMPTY_MAP) {
                    topicIdRefMap = new HashMap<>(8);
                }
                HashMap<String, Set<String>> topicFields = new HashMap<>();
                for (InstanceField f : refers) {
                    if (f != null) {
                        String topic = f.getTopic();
                        topicIdRefMap.computeIfAbsent(topic, k -> new HashSet<>()).add(ins);
                        if (f.getField() != null) {
                            topicFields.computeIfAbsent(topic, k -> new HashSet<>()).add(f.getField());
                        }
                    }
                }
                ins.setRefTopicFields(topicFields);
            }
        }
        if (topicIdRefMap.isEmpty()) {
            return;
        }
        Set<String> allIds = Stream.of(topicIdRefMap.keySet(), calcTopics.keySet()).flatMap(Collection::stream).map(t -> genIdForPath(t)).collect(Collectors.toSet());
        List<UnsPo> refUns = this.baseMapper.listInstanceByIds(allIds);
        Map<String, UnsPo> refPoMap = refUns.stream().collect(Collectors.toMap(UnsPo::getPath, k -> k));
        for (Map.Entry<String, Set<UnsBo>> e : topicIdRefMap.entrySet()) {
            String topic = e.getKey();
            Set<UnsBo> bos = e.getValue();
            UnsPo po = refPoMap.get(topic);
            if (po == null) {
                for (UnsBo bo : bos) {
                    errTipMap.put(bo.gainBatchIndex(), I18nUtils.getMessage("uns.topic.calc.expression.topic.ref.notFound", topic));
                    paramInstances.remove(bo.getId());
                }
            } else {
                FieldDefine[] defines = JsonUtil.fromJson(po.getFields(), FieldDefine[].class);
                Map<String, FieldDefine> fMap = Arrays.stream(defines).collect(Collectors.toMap(FieldDefine::getName, f -> f));
                final CopyOptions copyOptions = new CopyOptions().ignoreNullValue();
                for (UnsBo bo : bos) {
                    String batchIndex = bo.gainBatchIndex();
                    if (!paramInstances.containsKey(bo.getId())) {
                        continue;
                    }
                    if (bo.getDataType() == Constants.CALCULATION_HIST_TYPE) {
                        if (bo.getStreamOptions().getWindow().getOptionBean() instanceof StreamWindowOptionsStateWindow) {
                            StreamWindowOptionsStateWindow stateWindow = (StreamWindowOptionsStateWindow) bo.getStreamOptions().getWindow().getOptionBean();
                            FieldDefine rff = fMap.get(stateWindow.getField());
                            if (rff != null) {
                                FieldType fieldType = rff.getType();
                                if (fieldType != FieldType.INT && fieldType != FieldType.LONG && fieldType != FieldType.BOOLEAN && fieldType != FieldType.STRING) {
                                    errTipMap.put(batchIndex, I18nUtils.getMessage("uns.invalid.stream.state.colType", stateWindow.getField(), fieldType.name));
                                    paramInstances.remove(bo.getId());
                                    continue;
                                }
                            } else {
                                errTipMap.put(batchIndex, I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.notFound", topic, stateWindow.getField()));
                                paramInstances.remove(bo.getId());
                                continue;
                            }
                        }
                        bo.setReferTable(po.getAlias());
                        bo.setDataPath(bo.getReferTopic());
                        HashMap<String, Object> protocol = new HashMap<>();
                        BeanUtil.copyProperties(bo.getStreamOptions(), protocol, copyOptions);
                        bo.setProtocol(protocol);// 流计算的 protocol 存放 StreamOptions
                    }
                    Map<String, Set<String>> refTopicFields = bo.getRefTopicFields();
                    Set<String> refFs = refTopicFields != null ? refTopicFields.get(topic) : null;
                    if (refFs != null) {
                        for (String ref : refFs) {
                            FieldDefine rff = fMap.get(ref);
                            FieldType ft;
                            if (rff == null) {
                                errTipMap.put(batchIndex, I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.notFound", topic, ref));
                                paramInstances.remove(bo.getId());
                            } else if ((ft = rff.getType()) != null && (!ft.isNumber && FieldType.BOOLEAN != ft)) {
                                errTipMap.put(batchIndex, I18nUtils.getMessage(
                                        "uns.topic.calc.expression.fields.ref.invalidType", topic, ref, ft.name));
                                paramInstances.remove(bo.getId());
                            } else if (ft == null) {
                                errTipMap.put(batchIndex, I18nUtils.getMessage(
                                        "uns.topic.calc.expression.fields.ref.invalidType", topic, ref, "Null"));
                                paramInstances.remove(bo.getId());
                            }
                        }
                    }
                }
            }
        }
        HashMap<String, Set<String>> topicRemoveRefMap = new HashMap<>();
        HashMap<String, Set<String>> topicUpdateRefMap = new HashMap<>();
        for (Map.Entry<String, UnsBo> entry : calcTopics.entrySet()) {
            String calcTopic = entry.getKey();
            UnsBo bo = entry.getValue();
            if (!paramInstances.containsKey(bo.getId())) {
                continue;// 新增计算实例校验不通过的情况
            }
            UnsPo po = refPoMap.get(calcTopic);
            InstanceField[] newRefs = bo.getRefers();
            Set<String> newRefTopics = Arrays.stream(newRefs).filter(f -> f != null).map(f -> f.getTopic()).collect(Collectors.toSet());
            if (po != null) {
                InstanceField[] oldRefs = !org.apache.commons.lang3.StringUtils.isEmpty(po.getRefers()) ?
                        JsonUtil.fromJson(po.getRefers(), InstanceField[].class) : new InstanceField[0];
                Set<String> oldRefTopics = Arrays.stream(oldRefs).filter(f -> f != null).map(f -> f.getTopic()).collect(Collectors.toSet());
                for (String oldRef : oldRefTopics) {
                    if (!newRefTopics.contains(oldRef)) {
                        topicRemoveRefMap.computeIfAbsent(oldRef, k -> new HashSet<>()).add(calcTopic);
                    }
                }
            }
            for (String ref : newRefTopics) {
                topicUpdateRefMap.computeIfAbsent(ref, k -> new HashSet<>()).add(calcTopic);
            }
        }
        if (topicRemoveRefMap.size() + topicUpdateRefMap.size() > 0) {
            List<RefUnsUpdateInfo> refUnsList = new ArrayList<>();
            addRefUns2List(topicRemoveRefMap, refUnsList);
            final int removeIndex = refUnsList.size();
            addRefUns2List(topicUpdateRefMap, refUnsList);
            this.executeBatch(refUnsList, (session, info) -> {
                UnsMapper unsMapper = session.getMapper(UnsMapper.class);
                if (info.i <= removeIndex) {
                    unsMapper.removeRefUns(info.topicId, info.refTopics);
                } else {
                    unsMapper.updateRefUns(info.topicId, info.refTopics);
                }
            });
        }
    }

    private static class RefUnsUpdateInfo {
        final String topicId;
        final Collection<String> refTopics;
        final int i;

        RefUnsUpdateInfo(String topicId, Collection<String> refTopics, int i) {
            this.topicId = topicId;
            this.refTopics = refTopics;
            this.i = i;
        }

    }

    private static void addRefUns2List(Map<String, Set<String>> topicRefMap, List<RefUnsUpdateInfo> list) {
        for (Map.Entry<String, Set<String>> entry : topicRefMap.entrySet()) {
            String topic = entry.getKey();
            String id = genIdForPath(topic);
            if (entry.getValue().size() <= 20) {
                list.add(new RefUnsUpdateInfo(id, entry.getValue(), list.size() + 1));
            } else {
                for (List<String> topics : Lists.partition(new ArrayList<>(entry.getValue()), 20)) {
                    list.add(new RefUnsUpdateInfo(id, topics, list.size() + 1));
                }
            }
        }
    }

    private void setEventStatusCallback(Consumer<RunningStatus> statusConsumer, BatchCreateTableEvent event, AtomicInteger total) {
        final String START_MSG = I18nUtils.getMessage("uns.create.status.running");
        final String END_MSG = I18nUtils.getMessage("uns.create.status.finished");
        final String ERR_MSG = I18nUtils.getMessage("uns.create.status.error");
        event.setDelegateAware(new EventStatusAware() {
            long t0;

            @Override
            public void beforeEvent(int N, int i, String listenerName) {
                total.compareAndSet(0, N);
                double progress = 0;
                if (i > 1 && N > 0) {
                    progress = ((int) (1000.0 * (i - 1) / N)) / 10.0;
                }
                statusConsumer.accept(new RunningStatus(N + 1, i, listenerName, START_MSG).setProgress(progress));
                t0 = System.currentTimeMillis();
            }

            @Override
            public void afterEvent(int N, int i, String listenerName, Throwable ex) {
                String msg;
                if (ex == null) {
                    msg = END_MSG;
                } else {
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        msg = cause.getMessage();
                    } else {
                        msg = ex.getMessage();
                    }
                    if (msg == null) {
                        msg = ERR_MSG;
                    } else {
                        msg = ERR_MSG + msg;
                    }
                }
                statusConsumer.accept(new RunningStatus(N + 1, i, listenerName, msg)
                        .setSpendMills(System.currentTimeMillis() - t0).setCode(ex == null ? 0 : 500));
            }
        });
    }

    private static String checkInstanceFields(FieldDefine[] modelFields, FieldDefine[] insFields) {
        if (modelFields == null) {
            modelFields = new FieldDefine[0];
        }
        if (insFields == null) {
            insFields = new FieldDefine[0];
        }
        HashMap<String, FieldDefine> insMap = new HashMap<>(insFields.length);
        for (FieldDefine insField : insFields) {
            String name = insField.getName();
            if (!name.startsWith(Constants.SYSTEM_FIELD_PREV) && insMap.put(name, insField) != null) {
                return "fields name duplicate: " + name;
            }
        }
        for (FieldDefine mf : modelFields) {
            String name = mf.getName();
            if (!name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                FieldDefine insF = insMap.remove(name);
                if (insF == null) {
                    return "instance need field: " + name;
                } else if (!mf.getType().equals(insF.getType())) {
                    return String.format("instance field type changed: %s, %s -> %s", name, mf.getType(), insF.getType());
                }
            }
        }
        if (!insMap.isEmpty()) {
            return "instance has unknown Fields in model: " + insMap.values();
        }
        return null;
    }

    private boolean checkAliasDuplicate(String id, String alias, Map<String, UnsPo> existModelOrInstances) {
        UnsPo existModelOrInstance = existModelOrInstances.get(alias);
        if (existModelOrInstance != null && !id.equals(existModelOrInstance.getId())) {
            return true;
        }
        return false;
    }

    private static Map<String, UnsBo> initParamsUns(List<CreateTopicDto> topicDtos,
                                                    Map<String, String> errTipMap,
                                                    HashMap<String, UnsBo> paramInstances) {
        Map<String, List<UnsBo>> orphaned = new HashMap<>();
        Map<String, UnsBo> paramModels = new HashMap<>(2 + topicDtos.size() / 2);
        for (CreateTopicDto dto : topicDtos) {
            String topic = dto.getTopic();
            if (topic != null) {
                dto.setTopic(topic.trim());
            }
            checkTopicDto(errTipMap, paramInstances, orphaned, paramModels, dto);
        }
        for (UnsBo inst : paramInstances.values()) {
            String err = checkInstance(inst);
            if (err != null) {
                paramInstances.remove(inst.getId());
                errTipMap.put(inst.gainBatchIndex(), err);
            }
        }
        return paramModels;
    }

    private static String checkInstance(UnsBo inst) {
        int dataType = inst.getDataType();
        if (dataType == Constants.CALCULATION_REAL_TYPE || dataType == Constants.ALARM_RULE_TYPE) {
            // 校验计算实例
            String expression = inst.getExpression();
            if (!StringUtils.hasText(expression)) {
                String msg = I18nUtils.getMessage("uns.topic.calc.expression.empty");
                return msg;
            }
            ExpressionUtils.ParseResult rs;
            Object compileExpression;
            try {
                rs = ExpressionUtils.parseExpression(expression);
                compileExpression = ExpressionFunctions.compileExpression(expression);
            } catch (Exception ex) {
                return I18nUtils.getMessage("uns.topic.calc.expression.invalid", ex.getMessage());
            }
            for (String fun : rs.functions) {
                if (!ExpressionFunctions.hasFunction(fun)) {
                    return I18nUtils.getMessage("uns.topic.calc.expression.func.invalid", fun);
                }
            }
            InstanceField[] refFields = inst.getRefers();
            if (ArrayUtil.isEmpty(refFields)) {
                return I18nUtils.getMessage("uns.topic.calc.expression.fields.empty");
            }
            int countRefs = refFields != null ? refFields.length : 0;
            HashMap<String, Object> testMap = new HashMap<>();
            HashSet<Integer> indexes = new HashSet<>(Math.max(4, rs.vars.size()));
            for (String var : rs.vars) {
                String errMsg = null;
                if (var != null && var.length() > 1 && var.startsWith(Constants.VAR_PREV)) {
                    Integer refIndex = IntegerUtils.parseInt(var.substring(1));
                    if (refIndex == null) {
                        errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.invalid", var);
                    } else if (refIndex > countRefs) {
                        errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.indexOutOfBounds", refIndex, countRefs);
                    } else {
                        indexes.add(refIndex);
                    }
                } else {
                    errMsg = I18nUtils.getMessage("uns.topic.calc.expression.fields.ref.invalid", var);
                }
                if (errMsg != null) {
                    return errMsg;
                }
                testMap.put(var, 1);
            }
            if (rs.vars.size() > 0 && rs.vars.size() < countRefs) {
                HashMap<String, String> replaceVar = new HashMap<>(4);
                int countEmpty = 0;
                boolean prevIsEmpty = false;
                final String preFsJson = JSON.toJSONString(refFields);
                for (int i = 1; i <= countRefs; i++) {
                    if (!indexes.contains(i)) {
                        refFields[i - 1] = null;  // 删除多余的引用
                        countEmpty++;
                        prevIsEmpty = true;
                    } else if (prevIsEmpty) {
                        replaceVar.put(Constants.VAR_PREV + i, Constants.VAR_PREV + (i - countEmpty));
                        prevIsEmpty = false;
                    }
                }
                String exp = ExpressionUtils.replaceExpression(expression, replaceVar);
                InstanceField[] newFs = new InstanceField[countRefs - countEmpty];
                for (int i = 0, k = 0; i < refFields.length; i++) {
                    if (refFields[i] != null) {
                        newFs[k++] = refFields[i];
                    }
                }
                log.info("{} 表达式优化为: {} -> {}, 字段压缩: {} -> {}", inst.getPath(), expression, exp, preFsJson, JSON.toJSONString(newFs));
                inst.setRefers(newFs);
                inst.setExpression(expression = exp);
                compileExpression = ExpressionFunctions.compileExpression(expression);
                testMap.clear();
                for (int i = 1, sz = newFs.length; i <= sz; i++) {
                    testMap.put(Constants.VAR_PREV + i, 1);
                }
            }
            try {
                Object testRs = ExpressionFunctions.executeExpression(compileExpression, testMap);
                log.debug("eval( {} ) = {}", expression, testRs);
            } catch (Exception ex) {
                return I18nUtils.getMessage("uns.topic.calc.expression.invalid", "evalErr: " + ex.getMessage());
            }
            inst.setCompileExpression(compileExpression);
        }
        return null;
    }

    public static boolean isInstance(String topic) {
        return !topic.endsWith("/");
    }

    private static void checkTopicDto(Map<String, String> errTipMap, HashMap<String, UnsBo> paramInstances, Map<String, List<UnsBo>> orphaned,
                                      Map<String, UnsBo> paramModels, final CreateTopicDto dto) {
        int len;
        String topic = dto.getTopic();
        String alias = dto.getAlias();
        String batchIndex = dto.gainBatchIndex();
        Integer dataType = dto.getDataType();

        if (alias != null) {
            if (alias.length() > 63) {
                String msg = I18nUtils.getMessage("uns.alias.length.limit.exceed", 63);
                errTipMap.put(batchIndex, msg);
            } else if (!PathUtil.isAliasFormatOk(alias)) {
                String msg = I18nUtils.getMessage("uns.alias.format.invalid");
                errTipMap.put(batchIndex, msg);
            }
        }

        if (topic == null) {
            String msg = I18nUtils.getMessage("uns.topic.empty");
            errTipMap.put(batchIndex, msg);
        } else if ((len = topic.length()) > 190) {
            String msg = I18nUtils.getMessage("uns.topic.length.limit.exceed");
            errTipMap.put(batchIndex, msg);
        } else if (!PathUtil.validTopicFormate(topic, dataType)) {
            String msg = I18nUtils.getMessage("uns.topic.format.invalid");
            errTipMap.put(batchIndex, msg);
        } else {
            boolean endWithX = topic.endsWith("/");
            FieldDefine[] fields = dto.getFields();
            String modelId = dto.getModelId();
            if (!StringUtils.hasText(modelId)) {
                if (StringUtils.hasText(dto.getTemplate())) {
                    modelId = genIdForPath(dto.getTemplate());
                }
            }
            if (!endWithX) {
                // current is instance
                if (ArrayUtil.isEmpty(fields) && (dataType != null && dataType == Constants.REFER_TYPE)) {
                    FieldDefine mergeField = new FieldDefine("data_json", FieldType.STRING);
                    mergeField.setMaxLen(512 * 1024);// 聚合的字段总长度限制改大，不能超过mqtt消息长度限制
                    fields = new FieldDefine[]{mergeField};
                }

                String fileId = genIdForPath(topic);
                String folderId = null;
                if (topic.contains("/")) {
                    String folderPath = topic.substring(0, topic.lastIndexOf('/') + 1);
                    folderId = genIdForPath(folderPath);
                }

                UnsBo folder = paramModels.get(folderId);

                UnsBo instance = new UnsBo(fileId, topic, 2, 0, null);
                instance.setBatch(dto.getBatch());
                instance.setIndex(dto.getIndex());
                instance.setFolderId(folderId);
                instance.setParent(folder);
                instance.setDescription(dto.getDescription());
                instance.setProtocol(dto.getProtocol());
                instance.setFieldDefines(fields);
                instance.setDataPath(dto.getDataPath());
                instance.setProtocolBean(dto.getProtocolBean());
                instance.setAlias(org.apache.commons.lang3.StringUtils.isNotBlank(alias) ? alias : PathUtil.generateAlias(topic, 2));
                instance.setRefers(dto.getRefers());
                instance.setTableName(dto.getTableName());
                instance.setExpression(dto.getExpression());
                instance.setProtocolType(dto.getProtocolType());
                instance.setReferTopic(dto.getReferTopic());
                instance.setStreamOptions(dto.getStreamOptions());
                instance.setWithFlags(dto.getFlags() != null ? dto.getFlags() : generateFlag(dto.getAddFlow(), dto.getSave2db(), dto.getAddDashBoard(), dto.getRetainTableWhenDeleteInstance()));
                instance.setAlarmRuleDefine(dto.getAlarmRuleDefine());
                instance.setFrequencySeconds(dto.getFrequencySeconds());
                instance.setReferTopics(dto.getReferTopics());
                instance.setModelId(modelId);
                instance.setExtend(dto.getExtend());
                if (dto.getFrequency() != null) {
                    Map<String, Object> protocol = dto.getProtocol();
                    if (protocol == null) {
                        protocol = new HashMap<>();
                    }
                    String frequency = dto.getFrequency();
                    protocol.put("frequency", frequency);
                    instance.setProtocol(protocol);
                    instance.setFrequencySeconds(getFrequencySeconds(frequency));
                }
                paramInstances.put(fileId, instance);
                if (folder == null) {
                    orphaned.computeIfAbsent(folderId, k -> new LinkedList<>()).add(instance);
                }

                if (dataType != null) {
                    if (Constants.isValidDataType(dataType)) {
                        instance.setDataType(dataType);
                    } else {
                        String msg = I18nUtils.getMessage("uns.topic.data.type.invalid", dataType);
                        errTipMap.put(batchIndex, msg);
                    }
                }
            } else {// current is model
                StringBuilder folderPathBuilder = new StringBuilder(topic.length() + 2);
                folderPathBuilder.append(topic);
/*                if (endWithX) {
                    folderPathBuilder.append('*');
                } else if (!topic.endsWith("/*")) {
                    folderPathBuilder.append("/*");
                }*/
                String folderPath = folderPathBuilder.toString();
                String folderId = genIdForPath(folderPath);
                String fieldsJson = fields != null ? JsonUtil.toJson(fields) : null;
                UnsBo model = new UnsBo(folderId, folderPath, 0, 0, fieldsJson);
                model.setBatch(dto.getBatch());
                model.setIndex(dto.getIndex());
                model.setFieldDefines(fields);
                model.setDescription(dto.getDescription());
                model.setAlias(org.apache.commons.lang3.StringUtils.isNotBlank(alias) ? alias : PathUtil.generateAlias(folderPath, 0));
                model.setWithFlags(dto.getFlags() != null ? dto.getFlags() : generateFlag(dto.getAddFlow(), dto.getSave2db(), dto.getAddDashBoard(), dto.getRetainTableWhenDeleteInstance()));
                model.setModelId(modelId);
                paramModels.put(folderId, model);
                List<UnsBo> children = orphaned.get(folderId);
                if (children != null) {
                    Iterator<UnsBo> childrenItr = children.iterator();
                    while (childrenItr.hasNext()) {
                        UnsBo child = childrenItr.next();
                        child.setParent(model);
                        childrenItr.remove();
                    }
                }
            }
        }
    }

    private static Long getFrequencySeconds(String frequency) {
        Long nano = TimeUnits.toNanoSecond(frequency);
        if (nano != null) {
            long frequencySeconds = nano / TimeUnits.Second.toNanoSecond(1);
            return frequencySeconds;
        }
        return null;
    }

    private static Integer generateFlag(Boolean addFlow, Boolean saveToDB, Boolean addDashBoard, Boolean retainTableWhenDeleteInstance) {
        int flags = 0;
        if (Boolean.TRUE.equals(addFlow)) {
            flags |= Constants.UNS_FLAG_WITH_FLOW;
        }
        if (Boolean.TRUE.equals(saveToDB)) {
            flags |= Constants.UNS_FLAG_WITH_SAVE2DB;
        }
        if (Boolean.TRUE.equals(addDashBoard)) {
            flags |= Constants.UNS_FLAG_WITH_DASHBOARD;
        }
        if (Boolean.TRUE.equals(retainTableWhenDeleteInstance)) {
            flags |= Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE;
        }
        return flags;
    }

    private Map<String, UnsPo> listFolders(Collection<String> ids) {
        List<UnsPo> models = baseMapper.listFolders(ids);
        if (CollectionUtils.isEmpty(models)) {
            return Collections.emptyMap();
        }
        return models.stream().collect(Collectors.toMap(po -> po.getId(), po -> po));
    }

    private Set<String> listInstances(Collection<String> instanceIds) {
        return baseMapper.listInstanceIds(instanceIds);
    }

    private Map<String, UnsPo> listFoldersByAlias(Collection<String> aliases) {
        if (CollectionUtils.isEmpty(aliases)) {
            return Collections.EMPTY_MAP;
        }
        List<UnsPo> models = baseMapper.listFoldersByAlias(aliases);
        if (CollectionUtils.isEmpty(models)) {
            return Collections.emptyMap();
        }
        return models.stream().collect(Collectors.toMap(po -> po.getAlias(), po -> po));
    }

    private Map<String, UnsPo> listInstancesByAlias(Collection<String> aliases) {
        if (CollectionUtils.isEmpty(aliases)) {
            return Collections.EMPTY_MAP;
        }
        List<UnsPo> instances = baseMapper.listInstanceIdsByAlias(aliases);
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyMap();
        }
        return instances.stream().collect(Collectors.toMap(po -> po.getAlias(), po -> po));
    }

    private Map<String, UnsPo> queryByIds(Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.EMPTY_MAP;
        }
        List<UnsPo> items = baseMapper.selectBatchIds(ids);
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyMap();
        }
        return items.stream().collect(Collectors.toMap(po -> po.getId(), po -> po));
    }

    private static int countMatches(CharSequence str, int from, int end, char ch) {
        int count = 0;
        from = Math.max(0, from);
        end = Math.min(str.length(), end);
        for (int i = from; i < end; ++i) {
            if (ch == str.charAt(i)) {
                ++count;
            }
        }

        return count;
    }

    static final String genIdForPath(String path) {
        return HexUtil.encodeHexStr(PATH_ID_DIGEST.digest(path.getBytes(StandardCharsets.UTF_8)));
    }

    static final String getShowPath(String path) {
        if (path.endsWith("/*")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    static final MessageDigest PATH_ID_DIGEST;

    static {
        try {
            PATH_ID_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    private static final String ALARM_MODEL_PATH = Constants.ALARM_TOPIC_PREFIX + "/*";
    private static final String ALARM_MODEL_ID = genIdForPath(ALARM_MODEL_PATH);
    private static final String ALARM_TABLE = "supos." + AlarmPo.TABLE_NAME;
    private static final int ALARM_FLAGS = Constants.UNS_FLAG_WITH_SAVE2DB | Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE;

    @EventListener(classes = ContextRefreshedEvent.class)
    @Order(100)
    void onStartup(ContextRefreshedEvent event) {
        {
            Map<String, DataStorageAdapter> adapterMap = event.getApplicationContext().getBeansOfType(DataStorageAdapter.class);
            if (adapterMap != null && adapterMap.size() > 0) {
                for (DataStorageAdapter adapter : adapterMap.values()) {
                    SrcJdbcType jdbcType = adapter.getJdbcType();
                    if (jdbcType.typeCode == Constants.TIME_SEQUENCE_TYPE) {
                        timeDataType = jdbcType;
                    } else if (jdbcType.typeCode == Constants.RELATION_TYPE) {
                        relationType = jdbcType;
                    }
                }
            }
            log.info("** timeDataType={}, relationType={}", timeDataType, relationType);
        }
        {
            final String fields = getAlarmTableFields();
            UnsPo alarmModel = new UnsPo(ALARM_MODEL_ID, ALARM_MODEL_PATH, NodeType.Model.code, Constants.ALARM_RULE_TYPE, SrcJdbcType.Postgresql.id, fields, "Alarm Model");
            {
                alarmModel.setTableName("supos." + AlarmPo.TABLE_NAME);
                alarmModel.setWithFlags(Constants.UNS_FLAG_WITH_SAVE2DB | Constants.UNS_FLAG_RETAIN_TABLE_WHEN_DEL_INSTANCE);
            }
            UnsPo existsAlarmModel = baseMapper.selectOne(new QueryWrapper<UnsPo>().eq("id", ALARM_MODEL_ID).select("fields", "table_name", "data_src_id"));
            if (existsAlarmModel != null && (!alarmModel.getTableName().equals(existsAlarmModel.getTableName())
                    || !Integer.valueOf(SrcJdbcType.Postgresql.id).equals(existsAlarmModel.getDataSrcId())//
                    || !fields.equals(existsAlarmModel.getFields())//
            )) {
                int updated = baseMapper.fixAlarmFields(fields);
                log.info("修复 alarm实例: {}", updated);
            } else if (existsAlarmModel == null) {
                // public UnsPo(String id, String path, int pathType, int dataType, int dataSrcId, String fields, String description) {
                baseMapper.saveOrIgnoreBatch(Collections.singletonList(alarmModel));
            }
            String alarmFolder = "/$alarm/";
            String alarmFolderId = genIdForPath(alarmFolder);
            UnsPo folderAlarm = new UnsPo();
            folderAlarm.setId(alarmFolderId);
            folderAlarm.setPath(alarmFolder);
            folderAlarm.setPathType(0);
            folderAlarm.setDataType(Constants.ALARM_RULE_TYPE);
            folderAlarm.setDescription("alarm folder");
            folderAlarm.setModelId(ALARM_MODEL_ID);
            folderAlarm.setAlias(PathUtil.generateAlias(alarmFolder, 0));
            baseMapper.saveOrIgnoreBatch(Collections.singletonList(folderAlarm));
        }
//        {
//            UnsPo firstInstance = baseMapper.getFirstInstance();
//            if (firstInstance != null && (firstInstance.getNumberFields() == null || firstInstance.getModelId() == null)) {
//                // 修复所有实例的数字字段个数
//                List<UnsPo> list = baseMapper.selectList(new QueryWrapper<UnsPo>().select("id", "path", "fields")
//                        .eq("path_type", 2).and(ps -> ps.isNull("number_fields").or().isNull("model_id")));
//                log.info("修复 number_fields | model_id：{}", list.size());
//                for (UnsPo ins : list) {
//                    String fsStr = ins.getFields();
//                    int numFields = 0;
//                    if (fsStr != null && fsStr.length() > 0 & fsStr.charAt(0) == '[') {
//                        FieldDefine[] fs = JsonUtil.fromJson(fsStr, FieldDefine[].class);
//                        numFields = UnsBo.countNumberFields(fs);
//                    }
//                    ins.setNumberFields(numFields);
//                    String path = ins.getPath();
//                    path = path.substring(0, path.lastIndexOf('/') + 1) + '*';
//                    ins.setModelId(genIdForPath(path));
//                    ins.setPathType(null);
//                    ins.setFields(null);
//                }
//                this.updateBatchById(list);
//            }
//        }

        List<UnsPo> instances = baseMapper.listAllInstance();
        if (!CollectionUtils.isEmpty(instances)) {
            HashMap<SrcJdbcType, List<CreateTopicDto>> typeListMap = new HashMap<>();
            for (UnsPo p : instances) {
                CreateTopicDto dto = po2dto(p, p.getFields());
                typeListMap.computeIfAbsent(dto.getDataSrcId(), k -> new LinkedList<>()).add(dto);
            }
            EventBus.publishEvent(new InitTopicsEvent(this, typeListMap));
        }
    }

    private static CreateTopicDto po2dto(UnsPo p, String modelFields) {
        CreateTopicDto dto = new CreateTopicDto();
        dto.setTopic(getShowPath(p.getPath()));
        dto.setAlias(p.getAlias());
        dto.setTableName(p.getTableName());
        dto.setDescription(p.getDescription());
        dto.setDataType(p.getDataType());
        dto.setFlags(p.getWithFlags());
        dto.setModelId(p.getModelId());
        dto.setProtocolType(p.getProtocolType());
        String protocolStr = p.getProtocol();
        if (protocolStr != null && protocolStr.length() > 0 && protocolStr.charAt(0) == '{') {
            JSONObject protocol = JSON.parseObject(protocolStr);
            String frequency = protocol.getString("frequency");
            if (frequency != null) {
                dto.setFrequencySeconds(getFrequencySeconds(frequency));
            }
        }
        SrcJdbcType jdbcType = SrcJdbcType.getById(p.getDataSrcId());
        dto.setDataSrcId(jdbcType);
        String refers = p.getRefers();
        if (refers != null) {
            dto.setRefers(JsonUtil.fromJson(refers, InstanceField[].class));
        }
        String calculationExpr = p.getExpression();
        if (calculationExpr != null && calculationExpr.length() > 0) {
            dto.setCompileExpression(ExpressionFunctions.compileExpression(calculationExpr));
        }
        String fs = p.getFields();// 优先用实例自己的字段定义，为空再用模型的字段定义
        if (fs == null || fs.length() == 0) {
            fs = modelFields;
        }
        dto.setFields(JsonUtil.fromJson(fs, FieldDefine[].class));
        if (dto.getDataType() == Constants.ALARM_RULE_TYPE && p.getPathType() == 2) {
            AlarmRuleDefine ruleDefine = JsonUtil.fromJson(p.getProtocol(), AlarmRuleDefine.class);
            dto.setAlarmRuleDefine(ruleDefine);
        }
        return dto;
    }

    private String getAlarmTableFields() {
        List<FieldDefineVo> fieldDefineVos = baseMapper.describeTableFieldInfo(AlarmPo.TABLE_NAME);
        ArrayList<FieldDefine> list = new ArrayList<>(fieldDefineVos.size());
        for (FieldDefineVo vo : fieldDefineVos) {
            String col = vo.getName(), type = vo.getType();
            String fieldTypeStr = PostgresqlTypeUtils.dbType2FieldTypeMap.get(type.toLowerCase());
            FieldType fieldType = FieldType.getByName(fieldTypeStr);
            list.add(new FieldDefine(col.toLowerCase(), fieldType));
        }
        list.sort(Comparator.comparing(FieldDefine::getName));
        return JsonUtil.jackToJson(list);
    }

    public BaseResult createAlarmRule(CreateAlarmRuleVo vo) {
        String name = vo.getDataPath();
        String nameAlias;
        if (StringUtils.hasText(name)) {
            nameAlias = PinyinUtil.getPinyin(name, "") + "_" + IdUtil.fastSimpleUUID();
        } else {
            nameAlias = IdUtil.fastSimpleUUID();
        }
        String topic = Constants.ALARM_TOPIC_PREFIX + "/" + nameAlias;
        String alias = "alarm_" + nameAlias;
        return saveOrUpdateAlarmRule(vo, topic, alias);
    }

    private BaseResult saveOrUpdateAlarmRule(CreateAlarmRuleVo vo, String topic, String alias) {
        CreateTopicDto createTopicDto = new CreateTopicDto();
        createTopicDto.setDataPath(vo.getDataPath());
        createTopicDto.setDescription(vo.getDescription());
        createTopicDto.setRefers(vo.getRefers());
        createTopicDto.setExpression(vo.getExpression());
        createTopicDto.setAlarmRuleDefine(vo.getProtocol());
        Map<String, Object> protocolMap = new HashMap<>();
        BeanUtil.copyProperties(vo.getProtocol(), protocolMap);
        createTopicDto.setProtocol(protocolMap);

        createTopicDto.setTopic(topic);
        createTopicDto.setDataType(Constants.ALARM_RULE_TYPE);
        createTopicDto.setDataSrcId(SrcJdbcType.Postgresql);
        createTopicDto.setAlias(alias);
        createTopicDto.setTableName(ALARM_TABLE);
        createTopicDto.setFlags(ALARM_FLAGS | vo.getWithFlags()); //接收方式 1-人员 2-工作流程
        createTopicDto.setExtend(vo.getExtend());//工作流ID

        createTopicDto.setBatch(0);
        createTopicDto.setIndex(1);
        createTopicDto.setModelId(ALARM_MODEL_ID);
        CreateModelInstancesArgs args = new CreateModelInstancesArgs();
        args.setTopics(Arrays.asList(createTopicDto));
        Map<String, String> rs = createModelAndInstancesInner(args);

        BaseResult result = new BaseResult(0, "ok");
        if (rs != null && !rs.isEmpty()) {
            result.setCode(400);
            result.setMsg(rs.values().toString());
        }
        //16人员  32工作流
        if (Constants.UNS_FLAG_ALARM_ACCEPT_PERSON == vo.getWithFlags()) {
            alarmService.createAlarmHandler(topic, vo.getUserList());
        }
        //工作流只保存ext字段  工作流流程ID
        return result;
    }

    public BaseResult updateAlarmRule(UpdateAlarmRuleVo alarmRuleVo) {
        BaseResult result = new BaseResult(0, "ok");
        UnsPo unsPo = this.baseMapper.selectById(alarmRuleVo.getId());
        if (null == unsPo) {
            result.setCode(404);
            result.setMsg(I18nUtils.getMessage("uns.alarm.rule.not.exist"));
            return result;
        }
        return saveOrUpdateAlarmRule(alarmRuleVo, unsPo.getPath(), unsPo.getAlias());
    }

    public ResultVO<UnsPo> createTemplate(CreateTemplateVo createTemplateVo) {
        String path = createTemplateVo.getPath();
        long count = count(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getPath, path));
        if (count > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.name.already.exists"));
        }
        UnsPo unsPo = new UnsPo(path);
        unsPo.setId(genIdForPath(path));
        unsPo.setPathType(1);
        unsPo.setDataType(0);
        unsPo.setFields(JsonUtil.toJson(createTemplateVo.getFields()));
        unsPo.setDescription(createTemplateVo.getDescription());
        unsPo.setAlias(PathUtil.generateAlias(path, 1));
        save(unsPo);
        return ResultVO.successWithData(unsPo);
    }

    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public Map<String, String> createTemplates(List<CreateTemplateVo> createTemplateVos) {
        Set<String> pathSet = createTemplateVos.stream().map(CreateTemplateVo::getPath).collect(Collectors.toSet());
        List<UnsPo> existTemplates = list(Wrappers.lambdaQuery(UnsPo.class).in(UnsPo::getPath, pathSet).eq(UnsPo::getPathType, 1));
        Map<String, UnsPo> existTemplateMap = existTemplates.stream().collect(Collectors.toMap(UnsPo::getPath, Function.identity(), (k1, k2) -> k2));

        Map<String, String> errorMap = new HashMap<>();
        List<UnsPo> unsPos = new ArrayList<>(createTemplateVos.size());
        for (CreateTemplateVo createTemplateVo : createTemplateVos) {
            String path = createTemplateVo.getPath();
            if (existTemplateMap.containsKey(path)) {
                errorMap.put(createTemplateVo.gainBatchIndex(), I18nUtils.getMessage("uns.template.name.already.exists"));
                continue;
            }
            UnsPo unsPo = new UnsPo(path);
            unsPo.setId(genIdForPath(path));
            unsPo.setPathType(1);
            unsPo.setDataType(0);
            unsPo.setFields(JsonUtil.toJson(createTemplateVo.getFields()));
            unsPo.setDescription(createTemplateVo.getDescription());
            unsPo.setAlias(PathUtil.generateAlias(path, 1));
            unsPos.add(unsPo);
        }

        if (!CollectionUtils.isEmpty(unsPos)) {
            saveBatch(unsPos);
        }
        return errorMap;
    }

    public RemoveResult deleteTemplate(String id) {
        UnsPo template = getById(id);
        if (null == template){
            RemoveResult result = new RemoveResult();
            result.setCode(404);
            result.setMsg(I18nUtils.getMessage("uns.template.not.exists"));
            return result;
        }
        QueryWrapper<UnsPo> queryWrapper = new QueryWrapper<UnsPo>().eq("model_id", id);
        QueryWrapper<UnsPo> removeQuery = new QueryWrapper<>();
        removeQuery.eq("model_id", id).or().eq("id", id);
        List<UnsPo> unsPos = this.list(queryWrapper.select("path", "path_type", "data_src_id", "alias", "ref_uns", "refers", "data_type", "table_name", "with_flags"));
        unsPos.add(template);
        return getRemoveResult(true, true, true, removeQuery, unsPos);
    }

    public ResultVO updateTemplate(String id, String path) {
        long count = count(new LambdaQueryWrapper<UnsPo>().eq(UnsPo::getPath, path));
        if (count > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.name.already.exists"));
        }
        UnsPo template = getById(id);
        if (null == template) {
            return ResultVO.fail(I18nUtils.getMessage("uns.template.not.exists"));
        }

        LambdaUpdateWrapper<UnsPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UnsPo::getId, id);
        updateWrapper.set(UnsPo::getPath, path);
        update(updateWrapper);
        return ResultVO.success("ok");
    }
}
