package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 14:56
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(UnsAttachmentPo.TABLE_NAME)
public class UnsAttachmentPo {

    public static final String TABLE_NAME = "uns_attachment";

    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 关联模型实例别名
     */
    private String unsAlias;

    /**
     * 原始名称
     */
    private String originalName;

    /**
     * 附件名称
     */
    private String attachmentName;

    /**
     * 附件存储路径
     */
    private String attachmentPath;

    /**
     * 扩展名
     */
    private String extensionName;

    /**
     * 附件上传时间
     */
    private Date createAt;
}
