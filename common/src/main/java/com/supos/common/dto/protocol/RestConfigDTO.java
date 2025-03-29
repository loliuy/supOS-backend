package com.supos.common.dto.protocol;

import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.net.url.UrlBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.supos.common.annotation.UrlValidator;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
@Valid
public class RestConfigDTO extends BaseConfigDTO {


    private String serverName;

    /**
     * 同步第三方接口的频率
     */
    @NotNull
    @Valid
    private RateDTO syncRate;

    /**
     * 查询参数
     */
    @Valid
    private List<KeyValuePair<String>> params;

    private JSONArray headers;

    /**
     * 分页参数
     */
    @Valid
    private PageDef pageDef;

    private String path;

    private boolean https;

    private String body;

    public String getBody() {
        return StringUtils.hasText(body) ? body : "{}";
    }

    @NotNull
    @Valid
    private String method;

    private RestServerConfigDTO server;

    public void setPageDef(PageDef pageDef) {
        this.pageDef = pageDef;
        if (pageDef != null && params != null && params.size() > 0) {
            String pageK = "", offsetK = "";
            if (pageDef.start != null && pageDef.start.getKey() != null) {
                pageK = pageDef.start.getKey();
            }
            if (pageDef.offset != null && pageDef.offset.getKey() != null) {
                offsetK = pageDef.offset.getKey();
            }
            Iterator<KeyValuePair<String>> itr = params.iterator();
            while (itr.hasNext()) {
                KeyValuePair<String> pair = itr.next();
                String k = pair.getKey();
                String v = pair.getValue();
                if (pageK.equals(k)) {
                    itr.remove();
                    if (pageDef.start.getValue() == null) {
                        pageDef.start.setValue(v);
                    }
                } else if (offsetK.equals(k)) {
                    itr.remove();
                    if (pageDef.offset.getValue() == null) {
                        pageDef.offset.setValue(v);
                    }
                }
            }
        }
    }

    public String getUrl() {
        if (fullUrl != null) {
            // 如果配置了fullUrl，则直接返回url
            return fullUrl;
        }
        if (server != null) {
            // 如果没有fullUrl，但server有值，可通过server组装
            String proto = https ? "https" : "http";
            String url = proto + "://" + server.getHost();
            if (server.getPort() != null) {
                url += ":" + server.getPort();
            }
            if (path != null) {
                if (path.startsWith("/")) {
                    url += path;
                } else {
                    url += "/" + path;
                }
            }
            return url;
        }
        return null;
    }

    @UrlValidator
    private String fullUrl;// 完整地址，excel可简化配置，省略 host,port,path,https,method,params

    public void setFullUrl(String fullUrl) throws MalformedURLException {
        this.fullUrl = fullUrl;
        if (fullUrl != null) {
            this.server = new RestServerConfigDTO();
            URL url = new URL(fullUrl);
            String proto = url.getProtocol(), host = url.getHost(), path = url.getPath(), query = url.getQuery();
            int port = url.getPort();
            if (port > 0) {
                this.server.setPort(String.valueOf(port));
            }
            this.https = "https".equals(proto);
            this.server.setHost(host);
            this.path = path;
            if (query != null) {
                String pageK = "", offsetK = "";
                if (pageDef != null) {
                    if (pageDef.start != null) {
                        pageK = pageDef.start.getKey();
                    }
                    if (pageDef.offset != null) {
                        offsetK = pageDef.offset.getKey();
                    }
                }
                String[] ps = query.split("&");
                params = new ArrayList<>(ps.length);
                for (String p : ps) {
                    int eq = p.indexOf('=');
                    String k = p.substring(0, eq);
                    if (!k.equals(pageK) && !k.equals(offsetK)) {
                        String v = p.substring(eq + 1).trim();
                        v = URLDecoder.decode(v, StandardCharsets.UTF_8);
                        params.add(new KeyValuePair<>(k, v));
                    }
                }
            }
        }
    }

    public String gainFullUrl() {
        if (this.fullUrl != null) {
            return this.fullUrl;
        }

        UrlBuilder urlBuilder = UrlBuilder.of(getUrl());
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(param -> {
                urlBuilder.addQuery(param.getKey(), param.getValue());
            });
        }
        return urlBuilder.build();
    }

    /**
     * 分页参数，变量名由用户定义
     */
    @Data
    public static class PageDef {
        @NotNull
        private KeyValuePair<String> start;

        private KeyValuePair<String> offset;
    }

}
