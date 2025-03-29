package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(ExamplePo.TABLE_NAME)
public class ExamplePo {

    public static final String TABLE_NAME = "supos_example";

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 示例名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 安装包路径
     */
    private String packagePath;

    /**
     * 安装状态：1-未安装，2-安装中，3已安装
     */
    private Integer status;

    /**
     * 类型：1-OT 2-IT
     */
    private Integer type;

    /**
     * 1-grafana 2-fuxa
     */
    private Integer dashboardType;

    private String dashboardId;

    private String dashboardName;

    private Date createAt;

}
