package com.supos.uns;

import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TimeSequenceQueryController {

    private TimeSequenceDataStorageAdapter timeSequenceDataStorageAdapter;

    @PostMapping("/open-api/supos/rest/sql")
    @ResponseBody
    @Hidden
    public String executeSql(@RequestParam(value = "tz", required = false) String tz, @RequestParam(value = "req_id", required = false) String reqId,
                             @RequestParam(value = "row_with_meta", required = false) String rowWithMeta, HttpEntity<String> requestEntity) throws Exception {
        return timeSequenceDataStorageAdapter.execSQL(requestEntity.getBody());
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order
    void init(ContextRefreshedEvent event) {
        Map<String, TimeSequenceDataStorageAdapter> adapterMap = event.getApplicationContext().getBeansOfType(TimeSequenceDataStorageAdapter.class);
        if (!adapterMap.isEmpty()) {
            timeSequenceDataStorageAdapter = adapterMap.values().iterator().next();
        }
    }
}
