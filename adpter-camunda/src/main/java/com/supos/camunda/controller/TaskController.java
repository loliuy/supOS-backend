package com.supos.camunda.controller;

import com.alibaba.fastjson.JSONObject;
import com.supos.camunda.service.ProcessTaskService;
import com.supos.camunda.vo.TaskSubmit;
import com.supos.common.dto.JsonResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/28 13:21
 */
@Slf4j
@RestController
@RequestMapping("/inter-api/supos/task")
public class TaskController {

    @Resource
    private ProcessTaskService processTaskService;

    @GetMapping("/list")
    public JsonResult<List<JSONObject>> list(@RequestParam("assignee") String assignee) {
        List<JSONObject> list = processTaskService.query(assignee);
        return new JsonResult<>(0, "ok", list);
    }

    @ResponseBody
    @PostMapping("/complete")
    public JsonResult<JSONObject> complete(@RequestBody TaskSubmit taskSubmit) {
        processTaskService.complete(taskSubmit.getTaskId(), taskSubmit.getVariables());
        return new JsonResult<>(0, "ok", new JSONObject());
    }

    @ResponseBody
    @PostMapping("/completeByInstanceId")
    public JsonResult<JSONObject> completeByInstanceId(@RequestBody TaskSubmit taskSubmit) {
        processTaskService.claimAndCompleteTask(taskSubmit.getProcessInstanceId(), taskSubmit.getUserId(),null);
        return new JsonResult<>(0, "ok", new JSONObject());
    }
}
