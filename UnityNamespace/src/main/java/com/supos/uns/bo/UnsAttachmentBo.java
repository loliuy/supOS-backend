package com.supos.uns.bo;

import lombok.Data;

import java.util.Date;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 14:56
 */
@Data
public class UnsAttachmentBo {

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
