package com.supos.camunda.controller;

import com.supos.camunda.dto.CreateProcessDefinition;
import com.supos.camunda.dto.ProcessQueryDto;
import com.supos.camunda.service.ProcessService;
import com.supos.camunda.service.ProcessTaskService;
import com.supos.camunda.vo.ProcessDefinitionVo;
import com.supos.camunda.vo.ProcessInstanceVo;
import com.supos.camunda.vo.ProcessStartParam;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.exception.vo.ResultVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/26 10:51
 */
@Slf4j
@RestController
@RequestMapping("/inter-api/supos/process")
public class ProcessController {

    @Resource
    private ProcessService processService;
    @Resource
    private ProcessTaskService processTaskService;
    @Resource
    private RuntimeService runtimeService ;
    @Resource
    private TaskService taskService;

    /**
     * 流程定义列表
     * @param params
     * @return
     */
    @PostMapping("/definition/pageList")
    public PageResultDTO<ProcessDefinitionVo> pageList(@RequestBody ProcessQueryDto params){
        return processService.pageList(params);
    }

    /**
     * 新建or更新流程定义
     * @param createProcess
     * @return
     */
    @PostMapping("/definition/createOrUpdate")
    public ResultVO createOrUpdate(@RequestBody CreateProcessDefinition createProcess){
        return processService.createOrUpdate(createProcess);
    }

    /**
     * 流程定义发布
     * 状态设置为运行中
     */
    @PostMapping("/definition/deploy")
    public ResultVO<ProcessDefinitionVo> deplopy(@RequestParam("id") Long id, @RequestParam("file") MultipartFile file) throws IOException {
        try {
            return processService.deploy(id, file);
        } catch (Exception e) {
            log.error("deploy error", e);
            return ResultVO.fail("deploy error");
        }
    }

    /**
     * 暂停工作流
     * @param id
     * @return
     */
    @PutMapping("/definition/stop")
    public ResultVO stop(@RequestParam("id")Long id){
        return processService.stop(id);
    }


    /**
     * 开启一个流程实例
     * @param processStartParam  process_id
     * @return
     */
    @PostMapping("/instance/start")
    public ResultVO<ProcessInstanceVo> startProcess(@RequestBody ProcessStartParam processStartParam) {
        ProcessInstanceVo vo = processTaskService.startProcess(processStartParam.getProcessId(), processStartParam.getVariables());
        return ResultVO.successWithData(vo);
    }

    /**
     * 流程实例列表
     * @return
     */
    @GetMapping("/instance/list")
    public ResultVO<List<ProcessDefinitionVo>> processDefinitionList() {
        return ResultVO.successWithData(processService.processDefinitionList());
    }

    /**
     * 流程实例详情
     * @param processDefinitionId
     * @return
     */
    @GetMapping("/instance/detail")
    public ResultVO<ProcessDefinitionVo> detail(@RequestParam String processDefinitionId) {
       return ResultVO.successWithData(processService.queryById(processDefinitionId));
    }

    @GetMapping("test")
    public void test(){
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionId("Alarm001:2:b37fd03f-fd55-11ef-a447-00ff06c1062d") // 按流程定义 ID 过滤
                .active() // 只查询运行中的实例
                .list(); // 获取实例列表
        for (ProcessInstance instance : instances) {
            System.out.println(instance.getBusinessKey());
            System.out.println(instance.getProcessDefinitionId());
            System.out.println(instance.getCaseInstanceId());
            System.out.println(instance.getRootProcessInstanceId());
            System.out.println(instance.getProcessInstanceId());
            System.out.println("-------------------------------------------");

            processTaskService.getInstanceListByDefinitionId(instance.getProcessInstanceId());

            List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list();
            for (Task task : tasks) {
                System.out.println("任务 ID: " + task.getId());
                System.out.println("任务名称: " + task.getName());
                System.out.println("任务执行人: " + task.getAssignee());
                System.out.println("任务创建时间: " + task.getCreateTime());
                System.out.println("---------------------------------");
            }
        }
        System.out.println();
    }
}
