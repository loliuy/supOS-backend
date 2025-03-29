package com.supos.webhook.dao.mapper;

import com.supos.webhook.dao.po.WebhookPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookMapper {

    @Select("<script>" +
            "select * from supos_webhook where subscribe_event=#{event} and status=1 " +
            "</script>")
    List<WebhookPO> selectByEvent(@Param("event") String event);


}
