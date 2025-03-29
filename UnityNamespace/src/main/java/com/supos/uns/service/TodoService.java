package com.supos.uns.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.camunda.po.ProcessDefinitionPo;
import com.supos.camunda.service.ProcessService;
import com.supos.camunda.service.ProcessTaskService;
import com.supos.camunda.vo.ProcessInstanceVo;
import com.supos.common.Constants;
import com.supos.common.dto.AlarmRuleDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.TodoQueryDto;
import com.supos.common.enums.SysModuleEnum;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.mapper.AlarmHandlerMapper;
import com.supos.uns.dao.mapper.TodoMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.mapper.UserManageMapper;
import com.supos.uns.dao.po.AlarmHandlerPo;
import com.supos.uns.dao.po.TodoPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.CreateTodoVo;
import com.supos.uns.vo.TodoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.supos.uns.service.UnsManagerService.genIdForPath;

@Slf4j
@Service
public class TodoService extends ServiceImpl<TodoMapper, TodoPo> {

    @Resource
    private UnsMapper unsMapper;
    @Resource
    private TodoMapper todoMapper;
    @Resource
    private AlarmHandlerMapper alarmHandlerMapper;
    @Resource
    private ProcessTaskService processTaskService;
    @Resource
    private ProcessService processService;
    @Resource
    private UserManageMapper userManageMapper;

    @EventListener(classes = TopicMessageEvent.class)
    public void alarmEvent(TopicMessageEvent event){
        if (!event.topic.startsWith(Constants.ALARM_TOPIC_PREFIX)){
            return;
        }
        log.info(">>>>>>>>>>>>处理TopicMessageEvent 报警数据,topic:{},data:{}",event.topic,event.data);
        //报警数据
        Map<String, Object> data = event.data;
        //报警规则
        String instanceId = genIdForPath(event.topic);
        UnsPo instance = unsMapper.selectById(instanceId);
        if (null == instance){
            log.warn(">>>>>>>>>>>>报警规则：{}，查询失败，待办不生成.0",instance.getPath());
        }
        List<UserManageVo> userList = null;
        Long processId = instance.getExtend() != null ? Long.valueOf(instance.getExtend()) : null;
        String processInstanceId = null;
        //如果是工作流
        if (AlarmService.checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW){
            //查询工作流所配置的用户  给每个用户发送待办
            ProcessDefinitionPo processDefinitionPo = processService.getById(processId);
            if (null == processDefinitionPo){
                log.warn(">>>>>>>>>>>>报警规则：{}，获取工作流流程为空processId：{}，待办不生成.0",instance.getPath(),processId);
                return;
            }
            List<UserTask> tasks = processTaskService.getUserTaskListByProcessDefinitionId(processDefinitionPo.getProcessDefinitionId());
            if (CollectionUtils.isEmpty(tasks)){
                log.warn(">>>>>>>>>>>>报警规则：{}，获取工作流处理人员列表为空，待办不生成.1",instance.getPath());
                return;
            }
            String userIdsStr = tasks.get(0).getCamundaCandidateUsers();
            List<String> userIds = StrUtil.split(userIdsStr,",");
            userList = userManageMapper.listUserById(userIds);
            if (CollectionUtils.isEmpty(userList)){
                log.warn(">>>>>>>>>>>>报警规则：{}，获取工作流处理人员列表为空，待办不生成.2",instance.getPath());
                return;
            }
            //启动流程实例
            ProcessInstanceVo processInstanceVo = processTaskService.startProcess(processId,null);
            processInstanceId = processInstanceVo.getProcessInstanceId();
        } else if (AlarmService.checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_PERSON ){
            //接收类型：人员
            //获取报警规则配置的处理人列表
            List<AlarmHandlerPo> handlerList = alarmHandlerMapper.getByTopic(instance.getPath());
            if (CollectionUtils.isEmpty(handlerList)){
                log.warn(">>>>>>>>>>>>报警规则：{}，获取处理人员列表为空，待办不生成.",instance.getPath());
                return;
            }
            userList = handlerList.stream().map(h -> new UserManageVo(h.getUserId(),h.getUsername())).collect(Collectors.toList());
        }

        InstanceField instanceField = JsonUtil.fromJson(instance.getRefers(), InstanceField[].class)[0];
        AlarmRuleDefine alarmRuleDefine = new AlarmRuleDefine();
        alarmRuleDefine.parseExpression(instance.getExpression());
        long time = (long) data.get(Constants.SYS_FIELD_CREATE_TIME);
        String date = DateUtil.format(new Date(time),"yyyy/MM/dd HH:mm:ss");
        boolean isAlarm = (boolean) data.get("is_alarm");
        String msg;
        if (isAlarm){
            //【实例】,【属性】,【时间】 ,【条件】,【阀值】,【当前值】
            msg = I18nUtils.getMessage("todo.template.alarm",
                    instanceField.getTopic(),
                    instanceField.getField(),
                    date,
                    alarmRuleDefine.getCondition(),
                    data.get("limit_value"),
                    data.get("current_value"));
        } else {
            msg = I18nUtils.getMessage("todo.template.alarm.cancel",
                    instanceField.getTopic(),
                    instanceField.getField(),
                    date,
                    data.get("current_value"));
        }
        Date now = new Date();
        //待办表生成数据
        if (CollectionUtils.isNotEmpty(userList)) {
            for (UserManageVo user : userList) {
                TodoPo todo = new TodoPo();
                todo.setUserId(user.getId());
                todo.setUsername(user.getPreferredUsername());
                todo.setModuleCode(SysModuleEnum.ALARM.getCode());
                todo.setStatus(0);
                todo.setTodoMsg(msg);
                todo.setBusinessId(instanceId);//实例ID
                todo.setLink(data.get("_id").toString());//报警ID
                todo.setCreateAt(now);
                todo.setProcessId(processId);
                todo.setProcessInstanceId(processInstanceId);
                save(todo);
            }
        }
    }

    public PageResultDTO<TodoVo> pageList(TodoQueryDto todoQueryDto){
        Page<TodoPo> page = new Page<>(todoQueryDto.getPageNo(), todoQueryDto.getPageSize(),true);
        LambdaQueryWrapper<TodoPo> qw = new LambdaQueryWrapper<>();
        UserInfoVo user = UserContext.get();
        if (null == user){
            //未登录，返回空
            return PageResultDTO.<TodoVo>builder().code(0).data(Collections.emptyList()).build();
        } else if (!user.isSuperAdmin()){
            //非超管，查询自己的
            qw.eq(TodoPo::getUserId,user.getSub());
        }
        qw.eq(StringUtils.isNotBlank(todoQueryDto.getModuleCode()),TodoPo::getModuleCode,todoQueryDto.getModuleCode());
        qw.eq(ObjectUtil.isNotNull(todoQueryDto.getMyTodo()),TodoPo::getHandlerUserId,user.getSub());
        qw.eq(ObjectUtil.isNotNull(todoQueryDto.getStatus()),TodoPo::getStatus,todoQueryDto.getStatus());
        if (StringUtils.isNotBlank(todoQueryDto.getTodoMsg())){
            qw.apply("todo_msg ILIKE CONCAT('%', {0}, '%')", todoQueryDto.getTodoMsg());
        }
        Page<TodoPo> iPage = todoMapper.selectPage(page,qw);
        List<TodoVo> voList = iPage.getRecords().stream().map(todo ->{
            TodoVo vo = BeanUtil.copyProperties(todo,TodoVo.class);
            vo.setModuleName(I18nUtils.getMessage(vo.getModuleCode()));
            return vo;
        }).collect(Collectors.toList());
        PageResultDTO.PageResultDTOBuilder<TodoVo> pageBuilder = PageResultDTO.<TodoVo>builder()
                .total(iPage.getTotal()).pageNo(todoQueryDto.getPageNo()).pageSize(todoQueryDto.getPageSize());
        return pageBuilder.code(0).data(voList).build();
    }

    public boolean handleTodo(SysModuleEnum module, String businessId,String link, int status, UserInfoVo userInfoVo){
        //如果是报警模块
        if (SysModuleEnum.ALARM.equals(module)){
            UnsPo instance = unsMapper.selectById(businessId);
            if (null == instance){
                log.warn("报警规则ID:{},报警规则不存在,工作流领取并完成任务跳过",businessId);
            } else {
                if (AlarmService.checkWithFlags(instance.getWithFlags()) == Constants.UNS_FLAG_ALARM_ACCEPT_WORKFLOW){
                    //如果是配置了工作流，查询工作流的实例ID  领取并完成任务
                    LambdaQueryWrapper<TodoPo> qw = new LambdaQueryWrapper<>();
                    qw.eq(TodoPo::getModuleCode,module.getCode()).eq(TodoPo::getBusinessId,businessId)
                            .eq(TodoPo::getLink,link).eq(TodoPo::getUserId,userInfoVo.getSub()).last("limit 1");
                    TodoPo todoPo = this.baseMapper.selectOne(qw);
                    String processInstanceId = todoPo.getProcessInstanceId();
                    //领取并完成任务
                    processTaskService.claimAndCompleteTask(processInstanceId,userInfoVo.getSub(),null);
                }
            }
        }
        return todoMapper.updateTodoStatus(module.getCode(),businessId,status,userInfoVo.getPreferredUsername(),userInfoVo.getSub(),link) > 0;
    }

    public ResultVO createTodo(CreateTodoVo createTodoVo){
        TodoPo todoPo = BeanUtil.copyProperties(createTodoVo,TodoPo.class);
        save(todoPo);
        return ResultVO.success("ok");
    }
}
