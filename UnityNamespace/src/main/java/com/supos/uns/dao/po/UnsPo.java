package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supos.common.utils.JsonUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@TableName(UnsPo.TABLE_NAME)
public class UnsPo {
    public static final String TABLE_NAME = "uns_namespace";
    @TableId
    String id;
    String path;

    String modelId; // 实例对应的模板

    /**
     * 0--文件夹，1--模板，2--文件
     */
    Integer pathType;

    Integer dataSrcId;
    /**
     * 0--保留（模板），1--时序，2--关系，3--计算型, 5--告警
     */
    Integer dataType;

    String dataPath;
    String fields;

    String description;

    String protocol;

    Integer withFlags;// 1--addFlow, 2--addDashBoard, 3--二者都有

    Date createAt;

    String alias;

    String protocolType;

    String refUns;
    String refers;// 计算实例引用的其他实例字段
    String expression;// 计算表达式
    String tableName;
    Integer numberFields;
    String extend;//扩展字段   workflow表主键ID

    public UnsPo(String path) {
        this.path = path;
    }

    public UnsPo(String id, String path, int pathType, Integer dataType, Integer dataSrcId, String fields, String description) {
        this.id = id;
        this.path = path;
        this.pathType = pathType;
        this.dataType = dataType;
        this.dataSrcId = dataSrcId;
        this.fields = fields;
        this.description = description;
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
