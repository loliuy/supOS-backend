package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.AlarmQueryVo;
import com.supos.uns.vo.AlarmVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.checkerframework.checker.index.qual.SameLenBottom;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AlarmMapper extends BaseMapper<AlarmPo> {


    @Select("<script>select count(1) from " +AlarmPo.TABLE_NAME+
            " where topic = #{topic} and read_status = false</script>")
    long countByTopicNoRead(@Param("topic") String topic);

    IPage<AlarmVo> pageList(Page<?> page, AlarmQueryVo params);

    @Delete("<script> delete from " + AlarmPo.TABLE_NAME +
            " where topic in " +
            "  <foreach collection=\"topics\" item=\"topic\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{topic}" +
            "  </foreach>" +
            "</script>")
    void deleteByTopics(@Param("topics") Collection<String> topics);

    @Select("select * from "+ AlarmPo.TABLE_NAME + " where topic = #{topic} and read_status is null")
    List<AlarmPo> getNoReadListByTopic(@Param("topic") String topic);


    @Select("SELECT a.* from uns_alarms_data a LEFT JOIN uns_alarms_handler b on a.topic = b.topic " +
            "where a.topic = #{topic} and b.user_id = #{userId} and read_status is null ")
    List<AlarmPo> getNoReadListByTopicAndUserId(@Param("topic") String topic,@Param("userId") String userId);
}
