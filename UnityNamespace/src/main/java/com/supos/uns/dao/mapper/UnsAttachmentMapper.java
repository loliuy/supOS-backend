package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.uns.dao.po.UnsAttachmentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 15:01
 */
@Mapper
public interface UnsAttachmentMapper extends BaseMapper<UnsAttachmentPo> {

    @Select("<script>select id, uns_alias, original_name, attachment_name, attachment_path, create_at from " + UnsAttachmentPo.TABLE_NAME + " where uns_alias = #{unsAlias}</script>")
    List<UnsAttachmentPo> attachmentListByUnsAlias(@Param("unsAlias") String unsAlias);

    @Select("<script>select id, uns_alias, original_name, attachment_name, attachment_path, create_at from " + UnsAttachmentPo.TABLE_NAME + " where attachment_path = #{attachmentPath}</script>")
    List<UnsAttachmentPo> attachmentListByAttachmentPath(@Param("attachmentPath") String attachmentPath);
}
