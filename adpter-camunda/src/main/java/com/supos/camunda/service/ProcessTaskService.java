package com.supos.camunda.service;

import com.alibaba.fastjson.JSONObject;
import com.supos.camunda.dto.NodeDto;
import com.supos.camunda.po.ProcessDefinitionPo;
import com.supos.camunda.vo.ProcessInstanceVo;
import com.supos.camunda.vo.TaskVo;
import jakarta.annotation.Resource;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/28 13:04
 */
@Service
public class ProcessTaskService {

    @Resource
    private TaskService taskService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ProcessService processService;

    /**
     * 开始一个流程
     * @param processId
     * @param variables
     * @return
     */
    public ProcessInstanceVo startProcess(Long processId, Map<String, Object> variables) {
        ProcessDefinitionPo processDefinitionPo = processService.getById(processId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionPo.getProcessDefinitionId(), variables);
        ProcessInstanceVo vo = new ProcessInstanceVo();
        vo.setProcessId(processId);
        vo.setProcessDefinitionId(processDefinitionPo.getProcessDefinitionId());
        vo.setProcessInstanceId(processInstance.getProcessInstanceId());
        return vo;
    }

    /**
     * 领取并完成任务
     * @param processInstanceId
     * @param userId
     * @param param
     */
    public void claimAndCompleteTask(String processInstanceId, String userId,Map<String, Object> param) {
        TaskVo task = queryTaskListByInstanceId(processInstanceId);
        if (task == null ){
            return;
        }
        // 认领任务
        taskService.claim(task.getTaskId(), userId);
        // 完成任务
        taskService.complete(task.getTaskId(),param);
    }


    public void complete(String taskId, Map<String, Object> param) {
        taskService.complete(taskId, param);
    }


    /**
     * 通过实例ID查询当前的用户任务
     * @param processInstanceId
     * @return
     */
    public TaskVo queryTaskListByInstanceId(String processInstanceId) {
        Task task  = taskService.createTaskQuery().processInstanceId(processInstanceId).active().taskDefinitionKeyLike("userTask%").singleResult();
        if (task == null){
            return null;
        }
        TaskVo vo = new TaskVo();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getName());
        return vo;
    }

    public List<UserTask> getUserTaskListByProcessDefinitionId(String processDefinitionId) {
        // 获取 BPMN 模型实例
        BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);

        if (modelInstance != null) {
            // 获取所有 UserTask 节点
            List<UserTask> userTasks = modelInstance.getModelElementsByType(UserTask.class).stream().toList();
            return userTasks;
        }
        return null;
    }

    public List<JSONObject> query(String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        if (!CollectionUtils.isEmpty(tasks)) {
            Set<String> processDefinitionIds = tasks.stream().map(Task::getProcessDefinitionId).collect(Collectors.toSet());
            List<ProcessDefinition> processDefinitions = processService.queryByIds(processDefinitionIds);
            Map<String, ProcessDefinition> processDefinitionMap = processDefinitions.stream().collect(Collectors.toMap(ProcessDefinition::getId, Function.identity(), (k1, k2) -> k2));
            return tasks.stream().map(task -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("taskId", task.getId());
                jsonObject.put("processId", processDefinitionMap.get(task.getProcessDefinitionId()).getResourceName());
                return jsonObject;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    public NodeDto getCurrentTaskNode(String processInstanceId) {
        // 获取当前任务
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (task == null) {
            return null;
        }

        // 获取流程定义 ID
        String processDefinitionId = task.getProcessDefinitionId();

        // 获取流程定义模型
        BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);
        Collection<FlowNode> flowNodes = modelInstance.getModelElementsByType(FlowNode.class);

        // 将所有节点按执行顺序存入列表
        List<String> nodeIdList = new ArrayList<>();
        for (FlowNode node : flowNodes) {
            nodeIdList.add(node.getId());
        }

        // 获取当前任务的节点 ID
        String currentActivityId = task.getTaskDefinitionKey();

        // 计算当前任务在流程定义中的索引
        int index = nodeIdList.indexOf(currentActivityId) + 1; // 加 1 使其变成人类可读的编号
        NodeDto node = new NodeDto();
        node.setTotalNode(nodeIdList.size());
        node.setCurrentIndex(index);
        node.setCurrentNodeName(task.getName());
        return node;
    }


    public List<ProcessInstanceVo> getInstanceListByDefinitionId(String processDefinitionId){
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionId(processDefinitionId) // 按流程定义 ID 过滤
                .active() // 只查询运行中的实例
                .list(); // 获取实例列

        if (CollectionUtils.isEmpty(instances)){
            return Collections.emptyList();
        }

        List<ProcessInstanceVo> voList = instances.stream().map(ins ->{
            ProcessInstanceVo vo = new ProcessInstanceVo();
            vo.setProcessDefinitionId(processDefinitionId);
            vo.setProcessInstanceId(ins.getProcessInstanceId());
            vo.setNode(getCurrentTaskNode(ins.getProcessInstanceId()));
            return vo;
        }).collect(Collectors.toList());
        return voList;
    }
}
