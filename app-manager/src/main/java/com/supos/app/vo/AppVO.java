package com.supos.app.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppVO {

    // app别名，对应一个文件夹
    private String name;

    private String homepage;

    /**
     * html url列表
     */
    private Map<Long, String> urls;

}
