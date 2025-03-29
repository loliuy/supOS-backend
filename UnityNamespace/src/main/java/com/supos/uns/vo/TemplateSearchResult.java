package com.supos.uns.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSearchResult {

    private String id;

    /**
     * 模型名称
     */
    String path;

    /**
     * 模型描述
     */
    String description;

    /**
     * 别名
     */
    String alias;
}
