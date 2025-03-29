package com.supos.app.dao.mapper;

import com.supos.app.dao.po.AppHtmlPO;
import com.supos.app.dao.po.AppPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppHtmlMapper {

    int insertHtml(AppHtmlPO appHtmlPO);

    List<AppHtmlPO> selectAppHtml(@Param("appName") String appName);

    AppHtmlPO getById(@Param("id") long id);

    /**
     * 查询app下的主页
     * @return
     */
    List<AppHtmlPO> selectAppHomepage(@Param("appNames") List<String> appNames);
    /**
     * just only one homepage can be set
     * @param id
     * @return
     */
    int setHomepage(@Param("id") long id);

    /**
     * do not set homepage
     * @param appName
     * @return
     */
    int clearHomepage(@Param("appName") String appName);

    /**
     * delete all html under app
     * @param appName
     * @return
     */
    int deleteAppHtml(@Param("appName") String appName);

    /**
     * delete single html by id
     * @param id
     * @return
     */
    int deleteHtml(@Param("id") long id);

    /**
     * delete single html by appName and fileName
     * @param appName
     * @param fileName
     * @return
     */
    int deleteHtmlByName(@Param("appName") String appName, @Param("fileName") String fileName);

}
