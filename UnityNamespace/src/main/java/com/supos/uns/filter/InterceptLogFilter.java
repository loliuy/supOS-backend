//package com.supos.uns.filter;
//
//import cn.hutool.json.JSONUtil;
//import lombok.Data;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.time.StopWatch;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//
//import jakarta.servlet.*;
//import jakarta.servlet.annotation.WebFilter;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//
//
///**
// * 日志拦截器
// * @author xinwangji@supos.com
// * @date 2021/10/20 11：30
// * @description
// */
//@Data
//@Component
//@WebFilter(filterName = "logFilter",urlPatterns = "/*")
//@ConfigurationProperties(prefix = "log.filter")
//public class InterceptLogFilter implements Filter{
//
//    private final Logger logger = LoggerFactory.getLogger(getClass());
//
//    private List<String> ignoreUrls = Arrays.asList("^/inter-api/supos/uns/ws.*$");
//
//    @Override
//    public void init(FilterConfig filterConfig) {
//        logger.info("interceptLogFilter 初始化完成");
//    }
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        String url = request.getRequestURI();
//        if(CollectionUtils.isNotEmpty(ignoreUrls) && ignoreUrls.stream().anyMatch(url::matches)){
//            filterChain.doFilter(servletRequest, servletResponse);
//            return;
//        }
//        StopWatch watch = StopWatch.createStarted();
//        //判断如果是文件上传请求,只打印一些基本信息
//        boolean isMultipartRequest = StringUtils.contains(request.getContentType(), "multipart/form-data");
//        if (isMultipartRequest) {
//            logger.info("开始调用接口：{}，{}，url参数：{}", request.getMethod(), url, JSONUtil.toJsonStr(request.getParameterMap()));
//            filterChain.doFilter(servletRequest, servletResponse);
//            logger.info("调用接口结束：{}，{}，耗时：{}ms", request.getMethod(), url, watch.getTime());
//        } else {
//            BodyReaderHttpServletRequestWrapper requestWrapper = new BodyReaderHttpServletRequestWrapper(request);
//            String json = requestWrapper.getBodyString();
//            logger.info("开始调用接口：{}，{}，调用接口body参数：{}，url参数：{}", request.getMethod(), url, json, JSONUtil.toJsonStr(request.getParameterMap()));
//            HttpServletResponse response = (HttpServletResponse) servletResponse;
//            BodyReaderHttpServletResponseWrapper responseWrapper = new BodyReaderHttpServletResponseWrapper(response);
//            filterChain.doFilter(requestWrapper, responseWrapper);
//            long spendTime = watch.getTime();
//            logger.info("调用接口结束：{}，接口返回结果：{}，耗时：{}ms", url, responseWrapper.getContent(), spendTime);
//        }
//    }
//
//    @Override
//    public void destroy() {
//        logger.info("interceptLogFilter 销毁完成");
//    }
//}
