package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FileVo {

    @Schema(description = "文件ID")
    String unsId;
    @Schema(description = "显示名称")
    String name;//显示名称
    @Schema(description = "树的路径")
    String path;//树的路径
    /*
     * 0--文件夹，1--模板，2--文件
     */
    @Schema(description = "路径类型")
    Integer pathType;

}
