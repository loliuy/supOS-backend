package com.supos.uns;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.supos.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/inter-api/supos/dev")
public class DevtoolsController {
    private ScheduledExecutorService scheduledExecutorService;
    private static Map<String, String> origLogLevelMap;

    static LoggerContext ctx;

    static {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (!(factory instanceof LoggerContext)) {
            log.error("not logback!");
        } else {
            ctx = (LoggerContext) factory;
        }

        TreeMap<String, String> logLevelMap = new TreeMap<>();
        List<Logger> loggers = ctx.getLoggerList();
        for (Logger logger : loggers) {
            String name = logger.getName();
            Level level = logger.getLevel();
            if (level != null) {
                logLevelMap.put(name, level.levelStr);
            }
        }
        origLogLevelMap = Collections.unmodifiableMap(logLevelMap);
    }

    private final AtomicBoolean restoreIng = new AtomicBoolean();

    @GetMapping(value = "/logs", produces = "application/json")
    public String logs(HttpServletRequest request) {
        if (ctx == null) {
            return "not support!";
        }

        int updated = 0;
        Map<String, String[]> params = request.getParameterMap();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String k = entry.getKey();
                String[] vs = entry.getValue();
                Logger logger = ctx.getLogger(k);
                if (logger != null && vs != null && vs.length > 0) {
                    logger.setLevel(Level.toLevel(vs[0]));
                    updated++;
                }
            }
        }
        if (updated > 0) {
            if (scheduledExecutorService == null) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
            if (restoreIng.compareAndSet(false, true)) {
                scheduledExecutorService.schedule(this::restoreLoggerLevels, 30, TimeUnit.MINUTES);
            }
        }
        LinkedHashMap<String, Object> rs = new LinkedHashMap<>();
        rs.put("updated", updated);
        getLogQueryResult(rs);
        return JsonUtil.toJson(rs);
    }

    private static void getLogQueryResult(LinkedHashMap<String, Object> rs) {
        TreeMap<String, String> logLevelMap = new TreeMap<>();
        for (Map.Entry<String, String> entry : origLogLevelMap.entrySet()) {
            Logger logger = ctx.getLogger(entry.getKey());
            if (logger != null) {
                logLevelMap.put(logger.getName(), logger.getLevel().levelStr);
            }
        }
        rs.put("loggers", logLevelMap);
    }

    private void restoreLoggerLevels() {
        log.info("还原日志配置!");
        List<Logger> loggers = ctx.getLoggerList();
        for (Logger logger : loggers) {
            String name = logger.getName();
            String level = origLogLevelMap.get(name);
            if (level != null) {
                logger.setLevel(Level.toLevel(level));
            }
        }
        restoreIng.set(false);
    }

}
