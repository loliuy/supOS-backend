package com.supos.uns;


import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.TodoQueryDto;
import com.supos.common.event.EventBus;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.uns.service.TodoService;
import com.supos.uns.vo.CreateTodoVo;
import com.supos.uns.vo.TodoVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class TodoController {

    @Resource
    private TodoService todoService;

    @Operation(summary = "分页查询待办信息",tags = "待办中心")
    @PostMapping({"/inter-api/supos/todo/pageList","/open-api/supos/todo/pageList"})
    public PageResultDTO<TodoVo> pageList(@RequestBody TodoQueryDto params){
        return todoService.pageList(params);
    }

    @Operation(summary = "创建待办",tags = "待办中心")
    @PostMapping({"/inter-api/supos/todo/create","/open-api/supos/todo/create"})
    public ResultVO createTodo(@RequestBody CreateTodoVo createTodoVo){
        return todoService.createTodo(createTodoVo);
    }



    @GetMapping("/test")
    public void test(){
        Map<String, Object> data = new HashMap<>();
        data.put("topic","/$alarm/ceshi2_c6d71b518e6447ccbc69");
        data.put("current_value","100");
        data.put("limit_value","50");
        data.put("_ct",new Date());
        data.put("_id","454545");
        TopicMessageEvent topicMessageEvent = new TopicMessageEvent(this, null, "/$alarm/ceshi2_c6d71b518e6447ccbc69", "", data, null, null, null,1L,null);
        EventBus.publishEvent(topicMessageEvent);
    }
}
