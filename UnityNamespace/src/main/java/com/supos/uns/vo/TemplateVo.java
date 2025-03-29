package com.supos.uns.vo;

import com.supos.common.vo.FieldDefineVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVo {

    String id;

    /**
     * 模型名称
     */
    String path;

    /**
     * 别名
     */
    String alias;

    /**
     * 字段定义
     */
    FieldDefineVo[] fields;
    /**
     * 创建时间--单位：毫秒
     */
    Long createTime;

    /**
     * 模型描述
     */
    String description;

    /**
     * 模板引用的文件和文件夹列表
     */
    List<FileVo> fileList;
}
