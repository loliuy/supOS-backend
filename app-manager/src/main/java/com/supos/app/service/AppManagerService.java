package com.supos.app.service;

import com.supos.app.dao.mapper.AppHtmlMapper;
import com.supos.app.dao.mapper.AppMapper;
import com.supos.app.dao.po.AppHtmlPO;
import com.supos.app.dao.po.AppPO;
import com.supos.app.util.Constants;
import com.supos.app.util.RegUtils;
import com.supos.app.vo.AppHtmlVO;
import com.supos.app.vo.AppVO;
import com.supos.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppManagerService {

    @Autowired
    private AppMapper appMapper;
    @Autowired
    private AppHtmlMapper appHtmlMapper;

    /**
     * 根据名称或者别名模糊搜索，不支持分页
     * @param criteria 过滤条件
     * @return app列表
     */
    public List<AppVO> searchApps(String criteria) {
        List<AppVO> appsVOs = new ArrayList<>();
        // 数据量不大不分页
        List<AppPO> apps = appMapper.selectApps(criteria);
        if (apps.isEmpty()) {
            return appsVOs;
        }
        // 查询每个app的主页
        List<String> appNames = apps.stream().map(AppPO::getName).collect(Collectors.toList());
        List<AppHtmlPO> appHtmlList = appHtmlMapper.selectAppHomepage(appNames);
        // key=appName, value=html fileName
        Map<String, String> homepageMap = appHtmlList.stream().collect(Collectors.toMap(AppHtmlPO::getAppName, AppHtmlPO::getFileName, (existing, replacement) -> replacement));
        apps.stream().forEach(app -> {
            AppVO appVO = new AppVO();
            appVO.setName(app.getName());
            String fileName = homepageMap.get(app.getName());
            if (StringUtils.hasText(fileName)) {
                String homepage = Constants.APP_PREVIEW_URL.replace("{name}", app.getName()).replace("{htmlName}", fileName);
                appVO.setHomepage(homepage);
            } else {
                appVO.setHomepage("");
            }

            appsVOs.add(appVO);
        });
        return appsVOs;
    }

    /**
     * 创建app
     * @param name
     */
   public void createApp(String name) {
       File file = new File(Constants.APP_ROOT + name);
       if (file.exists()) {
           throw new AppException(500, "nodered.app.exist");
       }
       file.mkdirs();
       AppPO app = new AppPO();
       app.setName(name);
       appMapper.insertApp(app);
       log.info("app {} 创建成功", name);
   }

    /**
     * 获取app内html文件
     * @param name app名称
     * @return
     * @throws IOException
     */
   public AppVO getApp(String name) {
       File appFolder = new File(Constants.APP_ROOT + name);
       if (!appFolder.exists()) {
           throw new AppException(500, "nodered.app.not.exist");
       }
       String homepage = "";
       List<AppHtmlPO> appHtmlList = appHtmlMapper.selectAppHtml(name);
       Map<Long, String> htmlMap = new HashMap<>();
       for (AppHtmlPO appHtml : appHtmlList) {
           String url = Constants.APP_PREVIEW_URL.replace("{name}", name).replace("{htmlName}", appHtml.getFileName());
           if (appHtml.getHomepage() == 1) {
               homepage = url;
           }
           htmlMap.put(appHtml.getId(), url);
       }
       return new AppVO(name, homepage, htmlMap);
   }

    /**
     * 获取app的html内容
     * @param appName
     * @param htmlName
     * @return
     */
    public AppHtmlVO getHtmlContent(String appName, String htmlName) {
       String htmlPath = Constants.APP_ROOT + appName + "/" + htmlName;
       if (!new File(htmlPath).exists()) {
           throw new AppException(500, "nodered.app.html.not.exist");
       }
       StringBuilder contentBuilder = new StringBuilder();
       try (BufferedReader br = new BufferedReader(new FileReader(htmlPath))) {
           String line;
           while ((line = br.readLine()) != null) {
               contentBuilder.append(line).append(System.lineSeparator());
           }
       } catch (IOException e) {
           log.error("文件({})读取异常", htmlPath, e);
           throw new AppException(500, "nodered.file.read.error");
       }
       String url = Constants.APP_PREVIEW_URL.replace("{name}", appName).replace("{htmlName}", htmlName);
       return new AppHtmlVO(appName, url, contentBuilder.toString());
   }

    /**
     * 上传一个html文件
     * @param appName
     * @param path
     * @param fileName
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void addAppHtml(String appName, String path, String fileName) {
        // TODO 雪花算法
       long id = new Date().getTime();
       AppHtmlPO appHtmlPO = new AppHtmlPO();
       appHtmlPO.setId(id);
       appHtmlPO.setAppName(appName);
       appHtmlPO.setPath(path);
       appHtmlPO.setFileName(fileName);
       // 先删除重名的再新增
       appHtmlMapper.deleteHtmlByName(appName, fileName);
       appHtmlMapper.insertHtml(appHtmlPO);
    }

    /**
     * 根据名称删除app
     * @param name
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void deleteApp(String name) throws IOException {
       log.info("删除 app {} ", name);
       if (!RegUtils.validate(Constants.NAME_REG, name)) {
           throw new AppException(500, "nodered.invalid.parameter");
       }
       if (!StringUtils.hasText(name)) {
           throw new AppException(500, "nodered.app.not.specified");
       }
       File file = new File(Constants.APP_ROOT + name);
       if (file.exists()) {
           FileUtils.deleteDirectory(file);
       }
       appMapper.deleteApp(name);
       appHtmlMapper.deleteAppHtml(name);
       log.info("删除 app {} 成功", name);
    }

    /**
     * 删除单个html
     * @param htmlId
     * @throws IOException
     */
    public void deleteHtml(long htmlId) throws IOException {
        log.info("开始删除 html, id={} ", htmlId);
        AppHtmlPO htmlPO = appHtmlMapper.getById(htmlId);
        if (htmlPO.getPath().endsWith(".html")) {
            // delete html file on disk
            new File(htmlPO.getPath()).delete();
        }
        appHtmlMapper.deleteHtml(htmlId);
        log.info("删除 html id = {} 成功", htmlId);
    }


    /**
     * 设置app主页
     * @param appName
     * @param htmlId
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void setHomepage(String appName, long htmlId) {
        appHtmlMapper.clearHomepage(appName);
        appHtmlMapper.setHomepage(htmlId);
    }
}
