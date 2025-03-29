package com.supos.uns.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoVo {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 模块编码
     * @see com.supos.common.enums.SysModuleEnum
     */
    @Schema(description = "模块名称")
    private String moduleCode;

    /**
     * 事项信息
     */
    @Schema(description = "事项信息")
    private String todoMsg;

    /**
     * 业务主键
     */
    @Schema(description = "业务主键")
    private String businessId;

    /**
     * 链接
     */
    @Schema(description = "链接")
    private String link;
}
