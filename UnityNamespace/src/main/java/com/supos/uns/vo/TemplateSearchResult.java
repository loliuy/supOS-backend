package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSearchResult {

    private String id;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称")
    String path;

    /**
     * 模型描述
     */
    @Schema(description = "模型描述")
    String description;

    /**
     * 别名
     */
    @Schema(description = "别名")
    String alias;
}
