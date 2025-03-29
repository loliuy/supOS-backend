package com.supos.adpter.kong.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.kong.listener.KongAdapterCommandRunner;
import com.supos.adpter.kong.vo.MarkRouteRequestVO;
import com.supos.adpter.kong.vo.RouteVO;
import com.supos.adpter.kong.vo.ServiceResponseVO;
import com.supos.common.Constants;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.protocol.KeyValuePair;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.RuntimeUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KongAdapterService {

    private static final String GET_KONG_ROUTE = "routes";
    private static final String GET_KONG_SERVICE = "services";
    private static final String KONG_PLUGINS = "plugins";

    @Value("${kong.host:kong}")
    private String host;
    @Value("${kong.port:8001}")
    private String port;

    @Resource
    private SystemConfig systemConfig;



    /**
     * 查询kong所有路由， 并标记是否被勾选展示
     * @return
     */
    public List<RouteVO> queryRoutes() {
        String url = String.format("http://%s:%s/%s", host, port, GET_KONG_ROUTE);
        HttpRequest request = HttpUtil.createGet(url);
        HttpResponse response = request.execute();
        List<RouteVO> routes = new ArrayList<>();
        if (response.getStatus() != 200) {
            log.error("request kong failed, response: {}", response.body());
            return routes;
        }
        InternalKongResponseVO internalKongResponse = JSON.parseObject(response.body(), InternalKongResponseVO.class);
        for (InternalKongVO kongVO : internalKongResponse.getData()) {
            if (isMenu(kongVO.getTags())) {
                RouteVO rvo = new RouteVO();
                String menuName = I18nUtils.getMessage(kongVO.getName());
                rvo.setName(kongVO.getName());
                rvo.setShowName(menuName);
                rvo.setTags(parseTags(kongVO.getTags()));
                rvo.setService(queryServiceById(kongVO.getService().getId()));
//                rvo.setService();
                // set menu checked or not
                if (kongVO.getPaths() != null && !kongVO.getPaths().isEmpty()) {
//                    boolean checked = KongAdapterCommandRunner.localMenus.containsKey(kongVO.getName());
//                    rvo.setMenu(new RouteVO.MenuVO(kongVO.getPaths().get(0), checked));
                    rvo.setMenu(new RouteVO.MenuVO(kongVO.getPaths().get(0)));
                }
                routes.add(rvo);
            }
        }
        return routes;
    }

    private List<KeyValuePair> parseTags(List<String> tags) {
        List<KeyValuePair> tagList = new ArrayList<>();
        for (String t : tags) {
            if (t.startsWith("parentName:")) {
                String tag = t.replace("parentName:", "");
                String tagShowName = I18nUtils.getMessage(tag);
                tagList.add(new KeyValuePair(t, tagShowName));
            } else if (t.startsWith("description:")) {
                String tag = t.replace("description:", "");
                String tagShowName = I18nUtils.getMessage(tag);
                tagList.add(new KeyValuePair(t, tagShowName));
            } else if (t.startsWith("homeParentName:")) {
                String tag = t.replace("homeParentName:", "");
                String tagShowName = I18nUtils.getMessage(tag);
                tagList.add(new KeyValuePair(t, tagShowName));
            } else {
                tagList.add(new KeyValuePair("", t));
            }
        }
        return tagList;
    }

    // Check whether the route is a menu
    public boolean isMenu(List<String> tags) {
        return tags != null && tags.contains("menu");
    }

    /**
     * update menu which is checked
     * @param routes
     */
    public void markMenu(List<MarkRouteRequestVO> routes) throws IOException {
        Map<String, String> newLocalMenus = new HashMap<>();
        routes.stream().forEach(r -> {
            newLocalMenus.put(r.getName(),r.getUrl());
        });
        // file storage
        File file = new File(KongAdapterCommandRunner.LOCAL_MENU_CHECKED_STORAGE_PATH);
        if (!file.exists()) {
            // create new file
            file.createNewFile();
        }
        try (FileWriter fileWriter = new FileWriter(KongAdapterCommandRunner.LOCAL_MENU_CHECKED_STORAGE_PATH)) {
            if (!newLocalMenus.isEmpty()) {
                for (Map.Entry<String, String> entry : newLocalMenus.entrySet()) {
                    fileWriter.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            } else {
                // clear file
                fileWriter.write("");
            }
        }
        KongAdapterCommandRunner.localMenus = newLocalMenus;
        log.info("update menu cache success");
    }

    @Data
    static class InternalKongVO {

        private String name;

        private List<String> paths;

        private List<String> tags;

        private ServiceResponseVO service;
    }

    @Data
    static class InternalKongResponseVO {

        private List<InternalKongVO> data;
    }

    private String getCategory(List<String> tags){
        tags.remove("menu");
        return CollectionUtil.getFirst(tags);
    }

    private ServiceResponseVO queryServiceById(String serviceId){
        String url = String.format("http://%s:%s/%s/%s", host, port, GET_KONG_SERVICE,serviceId);
        HttpRequest request = HttpUtil.createGet(url);
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("request kong service failed, response: {}", response.body());
            return null;
        }
        ServiceResponseVO res = JSON.parseObject(response.body(),ServiceResponseVO.class);
        return res;
    }

//    public ResponseEntity pluginsProxy(String id, JSONObject params){
//        String url = "http://kong:8001/" + KONG_PLUGINS + "/" + id;
//        log.info(">>>>>>>>>>>>>pluginsProxy url:{},id:{},params:{}",url,id,params);
//        JSONObject req = new JSONObject();
//        req.put("enabled",params.getBoolean("enabled"));
//        HttpResponse response = HttpRequest.patch(url)
//                .body(req.toJSONString())                                         // 设置请求体
//                .timeout(5000)                                             // 设置超时时间
//                .execute();
//        log.info(">>>>>>>>>>>>>pluginsProxy response:{}",response.body());
//        //如果auth check插件修改启用状态，同步修改环境变量配置
//        if (200 == response.getStatus() && Constants.AUTH_CHECK_KONG_PLUGIN_ID.equals(id)){
//            systemConfig.setAuthEnable(params.getBoolean("enabled"));
//        }
//        return ResponseEntity.status(response.getStatus()).body(response.body());
//    }

//    @PostConstruct
//    public void init() {
//        if (!RuntimeUtil.isLocalRuntime()) {
//            ThreadUtil.execute(() -> {
//                String url = "http://kong:8001/" + KONG_PLUGINS + "/" + Constants.AUTH_CHECK_KONG_PLUGIN_ID;
//                String body = HttpUtil.get(url);
//                if (StringUtils.isNotBlank(body)){
//                    boolean authEnable = JSON.parseObject(body).getBoolean("enabled");
//                    systemConfig.setAuthEnable(authEnable);
//                    log.info(">>>>>>>>>>>>>authEnable set success :{}",authEnable);
//                }
//            });
//        }
//    }
}
