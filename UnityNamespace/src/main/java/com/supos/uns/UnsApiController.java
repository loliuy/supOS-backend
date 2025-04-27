package com.supos.uns;

import com.supos.common.NodeType;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.*;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.*;
import com.supos.uns.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class UnsApiController {
    @Autowired
    UnsManagerService unsManagerService;
    @Autowired
    UnsQueryService unsQueryService;
    @Autowired
    AlarmService alarmService;
    @Autowired
    UnsLabelService unsLabelService;
    @Autowired
    DbToUnsService dbToUnsService;

    @Operation(summary = "分页搜索主题")
    @GetMapping(path = {"/inter-api/supos/uns/search"}, produces = "application/json")
    public TopicPaginationSearchResult searchPaged(@RequestParam(name = "k", required = false) @Parameter(description = "模糊搜索词") String key,
                                                   @RequestParam(name = "modelTopic", required = false) @Parameter(description = "模型topic") String modelTopic,
                                                   @RequestParam(name = "type", required = false, defaultValue = "2") @Parameter(description = "搜索类型: 1--模型, 2--实例(默认值), 3--非计算时序实例，4--时序实例，5--报警规则") int searchType,
                                                   @RequestParam(name = "normal", required = false) @Parameter(description = "只搜索基本实例") boolean normal,
                                                   @RequestParam(name = "dataTypes", required = false) @Parameter(description = "数据类型") Set<Integer> dataTypes,
                                                   @RequestParam(name = "p", required = false, defaultValue = "1") @Parameter(description = "页码，默认1") Integer pageNo,
                                                   @RequestParam(name = "sz", required = false, defaultValue = "10") @Parameter(description = "每页条数，默认10") Integer pageSize,
                                                   @RequestParam(name = "nfc", required = false) @Parameter(description = "至少需要的数字类型字段的个数") Integer nfc) throws Exception {
        NodeType nodeType = NodeType.valueOf(searchType);
        if (normal && (dataTypes == null || dataTypes.isEmpty())) {
            dataTypes = SrcJdbcType.idMap.keySet();
        }
        return unsQueryService.searchPaged(modelTopic, key, nodeType, dataTypes, pageNo, pageSize, nfc);
    }

    @Operation(summary = "搜索主题树，默认整个树",tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/tree", "/open-api/supos/uns/tree"})
    public JsonResult<List<TopicTreeResult>> searchTree(@RequestParam(name = "key", required = false) @Parameter(description = "子节点模糊搜索词") String keyword,
                                                        @RequestParam(name = "showRec", required = false, defaultValue = "false") @Parameter(description = "显示记录条数") boolean showRec,
                                                        @RequestParam(name = "type", required = false, defaultValue = "1") @Parameter(description = "搜索类型: 1--文本搜索, 2--标签搜索，3--模板搜索") int searchType
    ) throws Exception {
        if (1 == searchType) {
            return unsQueryService.searchTree(keyword, showRec);
        } else if (2 == searchType) {
            return unsQueryService.searchByTag(keyword);
        } else if (3 == searchType) {
            return unsQueryService.searchByTemplate(keyword);
        } else {
            return new JsonResult<>(0, "ok", Collections.emptyList());
        }
    }

    @Operation(summary = "搜索外部topic主题树，默认整个树")
    @GetMapping(path = {"/inter-api/supos/external/tree"})
    public JsonResult<List<TopicTreeResult>> searchExternalTree(@RequestParam(name = "key", required = false) @Parameter(description = "子节点模糊搜索词") String keyword) {
        List<TopicTreeResult> treeResults = unsQueryService.searchExternalTopics(keyword);
        return new JsonResult<>(0, "ok", treeResults);
    }

    @Operation(summary = "枚举数据类型", description = "列出所有支持的数据类型，供建表时下拉选择",tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/types", "/open-api/supos/uns/types"})
    public JsonResult<Collection<String>> listTypes() {
        return unsQueryService.listTypes();
    }

    @Operation(summary = "获取最新消息", parameters = @Parameter(name = "topic", description = "主题"), responses = @ApiResponse(description = "消息体"))
    @GetMapping(value = "/inter-api/supos/uns/getLastMsg", produces = "application/json")
    public JsonResult<Object> getLastMsg(@RequestParam(name = "topic", required = false) @Parameter(description = "主题，实例path路径") String[] topics) throws Exception {
        // 查表最新数据
        if (ArrayUtils.isEmpty(topics)) {
            return new JsonResult<>(0, "Empty topics");
        }
        if (topics.length == 1) {
            String rs = unsQueryService.getLastMsg(topics[0]).getData();
            if (rs != null) {
                Object jsonObj = JsonUtil.fromJson(rs);
                return new JsonResult<>(0, "ok", jsonObj);
            } else {
                return new JsonResult<>(0, "NoData");
            }
        } else {
            LinkedHashMap<String, Object> msgs = new LinkedHashMap<>();
            for (String topic : topics) {
                String rs = unsQueryService.getLastMsg(topics[0]).getData();
                if (rs != null) {
                    Object jsonObj = JsonUtil.fromJson(rs);
                    msgs.put(topic, jsonObj);
                }
            }
            return new JsonResult<>(0, "ok", msgs);
        }
    }

    @Operation(summary = "查询文件夹详情",tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/model", "/open-api/supos/uns/model"})
    public JsonResult<ModelDetail> getModelDefinition(@RequestParam(name = "topic", required = false) @Parameter(description = "模型对应的主题路径") String topic) throws Exception {
        return unsQueryService.getModelDefinition(topic);
    }

    @Operation(summary = "查询文件详情",tags = "openapi.tag.folder.management")
    @GetMapping(path = {"/inter-api/supos/uns/instance", "/open-api/supos/uns/instance"})
    public JsonResult<InstanceDetail> getInstanceDetail(@RequestParam(name = "topic", required = false) @Parameter(description = "模型对应的主题路径") String topic) throws Exception {
        return unsQueryService.getInstanceDetail(topic);
    }

    @Operation(summary = "外部数据源表的字段定义转uns字段定义")
    @PostMapping(path = {"/inter-api/supos/uns/ds2fs"})
    public JsonResult<FieldDefine[]> dataSrc2UnsFields(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "数据源字段定义") DbFieldsInfoVo infoVo) throws Exception {
        return dbToUnsService.parseDatabaseFields(infoVo);
    }

    @Operation(summary = "外部JSON定义转uns字段定义")
    @PostMapping(path = {"/inter-api/supos/uns/json2fs"})
    public JsonResult<List<OuterStructureVo>> parseJson2uns(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "json body") String json) {
        return unsQueryService.parseJson2uns(json);
    }

    @Operation(summary = "外部JSON定义转树结构uns字段定义")
    @PostMapping(path = {"/inter-api/supos/uns/json2fs/tree"})
    public JsonResult<List<TreeOuterStructureVo>> parseJson2TreeUns(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "json body") String json) {
        return unsQueryService.parseJson2TreeUns(json);
    }

    @Operation(summary = "批量创建文件夹和文件(node-red导入专用)", tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/inter-api/supos/uns/for/nodered"})
    public ResultVO createModelsForNodeRed(@RequestBody List<CreateUnsNodeRedDto> requestDto) throws Exception {
        List<String[]> results = unsManagerService.createModelsForNodeRed(requestDto);
        return ResultVO.successWithData(results);
    }

    @Operation(summary = "创建文件夹和文件",tags = "openapi.tag.folder.management")
    @PostMapping(path = {"/inter-api/supos/uns/model", "/open-api/supos/uns/model"})
    @Valid
    public BaseResult createModelInstance(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模型字段定义") CreateModelInstanceVo dto) throws Exception {
        return unsManagerService.createModelInstance(dto);
    }

    @Operation(summary = "修改文件夹字段（只支持删除和新增）和描述",tags = "openapi.tag.folder.management")
    @PutMapping(path = {"/inter-api/supos/uns/model", "/open-api/supos/uns/model"})
    @Valid
    public ResultVO updateFieldAndDesc(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模型字段定义") UpdateModeRequestVo dto) throws Exception {
        if (dto.getFields() != null && dto.getFields().length > 0) {
            return unsManagerService.updateFields(dto.getAlias(), dto.getFields());
        } else {
            return unsManagerService.updateDescription(dto.getAlias(), dto.getModelDescription());
        }
    }

    @Operation(summary = "预先判断是否有属性关联")
    @PostMapping(path = {"/inter-api/supos/uns/model/detect"})
    @Valid
    public ResultVO detectIfFieldReferenced(@RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "模型字段定义") UpdateModeRequestVo dto) throws Exception {
        return unsManagerService.detectIfFieldReferenced(dto.getAlias(), dto.getFields());
    }

    @Operation(summary = "批量创建模型和实例")
    @PostMapping(path = {"/inter-api/supos/uns/batch"})
    public ResultVO createModelInstances(@RequestParam(name = "flags", required = false) Integer flags, @RequestBody List<CreateTopicDto> list) throws Exception {
        if (flags != null) {
            for (CreateTopicDto dto : list) {
                dto.setFlags(flags);
            }
        }
        Map<String, String> rs = unsManagerService.createModelAndInstance(list);
        if (rs == null || rs.isEmpty()) {
            return ResultVO.success("ok");
        }
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(206);
        resultVO.setData(rs.values());
        return resultVO;
    }

    @Operation(summary = "删除指定路径下的所有文件夹和文件",tags = "openapi.tag.folder.management")
    @DeleteMapping({"/inter-api/supos/uns","/open-api/supos/uns"})
    public RemoveResult removeModelOrInstance(@RequestParam(name = "path") @Parameter(description = "主题，文件夹或文件的 path路径，也可能只是某段路径") String path
            , @RequestParam(name = "withFlow", defaultValue = "true") @Parameter(description = "是否删除相关流程") boolean withFlow
            , @RequestParam(name = "withDashboard", defaultValue = "true") @Parameter(description = "是否删除相关可视化面板") boolean withDashboard
            , @RequestParam(name = "cascade", required = false) @Parameter(description = "是否删除关联的文件") Boolean removeRefer
    ) throws Exception {
        RemoveResult rs = unsManagerService.removeModelOrInstance(path, withFlow, withDashboard, removeRefer);
        log.info("删除uns: path={}, cascade={}, rs: {}", path, removeRefer, rs);
        return rs;
    }

    @Operation(summary = "校验指定文件夹夹是否已存在文件夹、文件名称")
    @GetMapping("/inter-api/supos/uns/name/duplication")
    public ResultVO<Integer> checkDuplicationName(@RequestParam(name = "folder", required = false) @Parameter(description = "文件夹path") String folder
            , @RequestParam(name = "name") @Parameter(description = "待校验的文件夹、文件名称") String name
            , @RequestParam(name = "checkType") @Parameter(description = "校验类型：1--文件夹名称校验，2--文件名称校验") int checkType
    ) {
        return unsQueryService.checkDuplicationName(folder, name, checkType);
    }

    @Operation(summary = "从RestApi搜索模型字段")
    @PostMapping("/inter-api/supos/uns/searchRestField")
    public JsonResult<RestTestResponseVo> searchRestField(@RequestBody RestTestRequestVo requestVo) {
        JsonResult<RestTestResponseVo> rs = null;
        try {
            return rs = unsQueryService.searchRestField(requestVo);
        } catch (Exception ex) {
            log.warn("searchRestFieldErr: " + ex.getMessage());
            throw ex;
        } finally {
            log.info("searchRestField: body={}, res={}", requestVo, rs);
        }
    }

    @Operation(summary = "手动触发RestApi")
    @PostMapping("/inter-api/supos/uns/triggerRestApi")
    public JsonResult<RestTestResponseVo> triggerRestApi(@RequestParam(name = "topic", required = false) @Parameter(description = "文件对应的主题路径") String topic) {
        return unsQueryService.triggerRestApi(topic);
    }

    @Operation(summary = "Mock rest")
    @GetMapping("/test/supos/uns/mockRest")
    public JsonResult<Map<String, Object>> mockRestPaged(@RequestParam(name = "name", required = false) List<String> names,
                                                         @RequestParam(name = "size", required = false, defaultValue = "3") int size) {
        if (names == null || names.isEmpty() || (names.size() == 1 && "[]".equals(names.get(0)))) {
            names = Arrays.asList("tag", "version");
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> data = new HashMap<>();
            for (String name : names) {
                if (name.contains("name") || name.contains("ver")) {
                    data.put(name, UUID.randomUUID().toString());
                } else {
                    data.put(name, new Random().nextInt(1000));
                }
            }
            list.add(data);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", list);

        log.info("mockRestPaged: names={}, res={}", names, res);

        return new JsonResult<>(0, "ok", res);
    }

    @GetMapping("/test/i18n")
    public String test18n(@RequestParam(name = "k") String k) {
        return I18nUtils.getMessage(k);
    }

    @Operation(summary = "创建报警规则")
    @PostMapping(path = {"/inter-api/supos/uns/alarm/rule"})
    public BaseResult createAlarmRule(@Valid @RequestBody CreateAlarmRuleVo createAlarmRuleVo) {
        return unsManagerService.createAlarmRule(createAlarmRuleVo);
    }

    @Operation(summary = "更新报警规则")
    @PutMapping(path = {"/inter-api/supos/uns/alarm/rule"})
    public BaseResult updateAlarmRule(@Valid @RequestBody UpdateAlarmRuleVo updateAlarmRuleVo) {
        return unsManagerService.updateAlarmRule(updateAlarmRuleVo);
    }

    @Operation(summary = "查询报警列表")
    @PostMapping(path = {"/inter-api/supos/uns/alarm/pageList"})
    public PageResultDTO<AlarmVo> list(@Valid @RequestBody AlarmQueryVo params) {
        return alarmService.pageList(params);
    }

    @Operation(summary = "确认报警")
    @PostMapping(path = {"/inter-api/supos/uns/alarm/confirm"})
    public BaseResult confirmAlarm(@Valid @RequestBody AlarmConfirmVo alarmConfirmVo) {
        return alarmService.confirmAlarm(alarmConfirmVo);
    }

    @Operation(summary = "标签列表", description = "列出所有支持的标签，下拉选择，支持模糊搜索" ,tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/allLabel", "/open-api/supos/uns/allLabel"})
    public ResultVO<List<UnsLabelPo>> allLabels(@RequestParam(name = "key", required = false) @Parameter(description = "关键字") String key) {
        return unsLabelService.allLabels(key);
    }

    @Operation(summary = "标签详情",tags = "openapi.tag.label.management")
    @GetMapping(path = {"/inter-api/supos/uns/label/detail","/open-api/supos/uns/label/detail"})
    public ResultVO<LabelVo> labelDetail(@RequestParam(name = "id") @Parameter(description = "标签ID") Long id) {
        return unsLabelService.detail(id);
    }

    @Operation(summary = "创建标签",tags = "openapi.tag.label.management")
    @PostMapping(path = {"/inter-api/supos/uns/label","/open-api/supos/uns/label"})
    public ResultVO createLabel(@RequestParam @Parameter(description = "标签名称") String name) {
        return unsLabelService.create(name);
    }

    @Operation(summary = "删除标签",tags = "openapi.tag.label.management")
    @DeleteMapping(path = {"/inter-api/supos/uns/label","/open-api/supos/uns/label"})
    public ResultVO deleteLabel(@RequestParam @Parameter(description = "标签ID") Long id) {
        return unsLabelService.delete(id);
    }

    @Operation(summary = "修改标签",tags = "openapi.tag.label.management")
    @PutMapping(path = {"/inter-api/supos/uns/label","/open-api/supos/uns/label"})
    public ResultVO updateLabel(@Valid @RequestBody LabelVo labelVo) {
        return unsLabelService.update(labelVo);
    }

    @Operation(summary = "文件打标签",tags = "openapi.tag.label.management")
    @PostMapping(path = {"/inter-api/supos/uns/makeLabel", "/open-api/supos/uns/makeLabel"})
    public ResultVO makeLabel(@RequestParam @Parameter(description = "实例别名") String alias,
                              @RequestBody(required = false) @Parameter(description = "标签集合，为空则取消所有标签") List<LabelVo> labelList) {
        return unsLabelService.makeLabel(alias, labelList);
    }

    @Operation(summary = "查询模板列表",tags = "openapi.tag.template.management")
    @PostMapping(path = {"/inter-api/supos/uns/template/pageList","/open-api/supos/uns/template/pageList"})
    public PageResultDTO<TemplateSearchResult> templatePageList(@Valid @RequestBody TemplateQueryVo params) {
        return unsQueryService.templatePageList(params);
    }

    @Operation(summary = "查询模板详情",tags = "openapi.tag.template.management")
    @GetMapping(path = {"/inter-api/supos/uns/template","/open-api/supos/uns/template"})
    public ResultVO<TemplateVo> templateDetail(@RequestParam(name = "id") @Parameter(description = "模板ID") String id) {
        return ResultVO.successWithData(unsQueryService.getTemplateById(id));
    }

    @Operation(summary = "新增模板",tags = "openapi.tag.template.management")
    @PostMapping(path = {"/inter-api/supos/uns/template","/open-api/supos/uns/template"})
    public ResultVO<UnsPo> createTemplate(@Valid @RequestBody CreateTemplateVo createTemplateVo) {
        return unsManagerService.createTemplate(createTemplateVo);
    }

    @Operation(summary = "修改模板",tags = "openapi.tag.template.management")
    @PutMapping(path = {"/inter-api/supos/uns/template","/open-api/supos/uns/template"})
    public ResultVO updateTemplate(@RequestParam(name = "id") @Parameter(description = "模板ID") String id,
                                   @RequestParam(name = "path") @Parameter(description = "模板名称") String path) {
        return unsManagerService.updateTemplate(id, path);
    }

    @Operation(summary = "删除模板",tags = "openapi.tag.template.management")
    @DeleteMapping(path = {"/inter-api/supos/uns/template","/open-api/supos/uns/template"})
    public RemoveResult deleteTemplate(@RequestParam(name = "id") @Parameter(description = "模板ID") String id) {
        return unsManagerService.deleteTemplate(id);
    }
}
