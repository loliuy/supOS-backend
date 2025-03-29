package com.supos.uns;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.net.URLDecoder;
import com.supos.common.adpater.TopicMessageConsumer;
import com.supos.common.dto.JsonResult;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.TopicMessageEvent;
import com.supos.common.event.UnsTopologyChangeEvent;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.service.UnsQueryService;
import com.supos.uns.service.UnsTopologyService;
import com.supos.uns.util.FileUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UnsWebsocketHandler implements WebSocketHandler {

    private static final ConcurrentHashMap<String, WsSubscription> sessions = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Set<String>> topicToSessionsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WebSocketSession> topologySessions = new ConcurrentHashMap<>();
    final UnsQueryService unsQueryService;
    final UnsExcelService unsExcelService;
    final UnsTopologyService unsTopologyService;

    private ExecutorService dataPublishExecutor = new ForkJoinPool(1);

    public UnsWebsocketHandler(@Autowired UnsQueryService unsQueryService, @Autowired UnsExcelService unsExcelService,
                               @Autowired UnsTopologyService unsTopologyService) {
        this.unsQueryService = unsQueryService;
        this.unsExcelService = unsExcelService;
        this.unsTopologyService = unsTopologyService;
    }

    private static class WsSubscription {
        final WebSocketSession conn;
        final ConcurrentHashSet<String> topics = new ConcurrentHashSet<>();

        WsSubscription(WebSocketSession conn) {
            this.conn = conn;
        }
    }

    @Data
    static class TopicAwareDto {
        int type;// 1--订阅 2--取消订阅
        List<String> topics;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        final String connectionId = session.getId();
        UriComponents components = UriComponentsBuilder.fromUri(session.getUri()).build();
        List<String> topics = components.getQueryParams().get("topic");
        log.debug("WebSocket open: {}", session);
        if (CollectionUtils.isEmpty(topics)) {
            String file = components.getQueryParams().getFirst("file");
            if (file != null) {
                String path = URLDecoder.decode(file, StandardCharsets.UTF_8);
                File excelFile = new File(FileUtils.getFileRootPath(), path);
                unsExcelService.asyncImport(excelFile, runningStatus -> dataPublishExecutor.submit(() -> {
                    String json = null;
                    try {
                        json = JsonUtil.toJson(runningStatus);
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        log.error("fail to send uploadStatus: " + json, e);
                    }
                }),true);
            }
            String globalTopology = components.getQueryParams().getFirst("globalTopology");
            if (globalTopology != null) {
                topologySessions.computeIfAbsent(connectionId, k -> session);
                log.debug("topology: {}", connectionId);
                publishTopologyMessage(session);
            }
            return;
        }
        topics = topics.stream().map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8)).collect(Collectors.toList());
        WsSubscription subscription = sessions.computeIfAbsent(connectionId, k -> new WsSubscription(session));
        log.debug("subscribe: {} topic={}", connectionId, topics);
        subscription.topics.addAll(topics);
        for (String topic : topics) {
            getConnectionIds(topic).add(connectionId);
            publishMessage(session, topic);
        }

    }

    private String getTopicLastMessage(String topic) {
        JsonResult<String> rs = unsQueryService.getLastMsg(topic);
        String str = rs.getData();
        return str != null ? str : "{}";
    }

    private void publishMessage(WebSocketSession session, String topic) {
        String msg = getTopicLastMessage(topic);
        try {
            session.sendMessage(new TextMessage(msg));
        } catch (IOException e) {
            log.error("fail to sendWs: topic={}, session={}", topic, session);
        }
    }

    private void publishTopologyMessage(WebSocketSession session) {
        JsonResult<String> rs = unsTopologyService.getLastMsg();
        String str = rs.getData();
        str = str != null ? str : "{}";
        try {
            session.sendMessage(new TextMessage(str));
        } catch (IOException e) {
            log.error("fail to sendWs: session={}", session);
        }
    }

    private static final String SEND_PREV = "/send?t=";
    private static final String SEND_BODY = "&body=";

    @Autowired
    TopicMessageConsumer topicMessageConsumer;

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            if (!(message instanceof TextMessage)) {
                return;
            }
            TextMessage textMessage = (TextMessage) message;

            String payload = textMessage.getPayload();
            log.trace("WebSocket handleMessage[{}] : {}", session.getId(), payload);

            //heartbeat
            if ("ping".equals(payload)) {
                session.sendMessage(new TextMessage("pong"));
                return;
            }

            if (payload.startsWith(SEND_PREV)) {
                int bodyIndex = payload.indexOf(SEND_BODY, SEND_PREV.length());
                if (bodyIndex > 0) {
                    String topic = payload.substring(SEND_PREV.length(), bodyIndex);
                    String body = payload.substring(bodyIndex + SEND_BODY.length());
                    log.debug("ws onMessage: {}, payload={}", topic, body);
                    if (StringUtils.hasText(topic) && StringUtils.hasText(body)) {
                        topicMessageConsumer.onMessage(topic, body);
                    }
                }
                return;
            }
            TopicAwareDto dto = JsonUtil.fromJson(textMessage.getPayload(), TopicAwareDto.class);

            WsSubscription subscription = sessions.get(session.getId());
            final List<String> topics = dto.topics;
            if (!CollectionUtils.isEmpty(topics)) {
                final String connectionId = session.getId();
                if (dto.type == 1) {
                    subscription.topics.addAll(topics);
                    for (String topic : topics) {
                        getConnectionIds(topic).add(connectionId);
                    }
                } else if (dto.type == 2) {
                    subscription.topics.removeAll(topics);
                    for (String topic : topics) {
                        getConnectionIds(topic).remove(connectionId);
                    }
                }
            }
        } catch (Exception e) {
            log.error(">>>>>>>>>>>>handleMessage IOException", e);
        }
    }

    void uncaughtException(Thread t, Throwable e) {
        log.error("发送Ws数据失败: " + t.getName(), e);
    }

    private static final Set<String> getConnectionIds(String topic) {
        return topicToSessionsMap.computeIfAbsent(topic, k -> Collections.synchronizedSet(new TreeSet<>()));
    }


    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(100)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        for (String topic : event.topics.keySet()) {
            log.info("========== remove topic: {} =========", topic, new Throwable());
            unsTopologyService.removeFromGlobalTopologyData(topic);
            Set<String> connectionIds = topicToSessionsMap.remove(topic);
            if (connectionIds != null) {
                for (String connId : connectionIds) {
                    WsSubscription subscription = sessions.get(connId);
                    if (subscription != null) {
                        subscription.topics.remove(topic);
                    }
                }
            }
        }
    }

    @EventListener(classes = TopicMessageEvent.class)
    @Order(1000)
    void onSaveDataEvent(TopicMessageEvent event) {
        final String topic = event.topic;
        Set<String> connectionIds = topicToSessionsMap.get(topic);
        if (!CollectionUtils.isEmpty(connectionIds)) {
            TextMessage message = new TextMessage(getTopicLastMessage(topic));
            for (String connId : connectionIds) {
                WsSubscription subscription = sessions.get(connId);
                try {
                    subscription.conn.sendMessage(message);
                } catch (IOException e) {
                    log.error("fail to sendMessage to[{}]", connId);
                }
            }
        } else {
            log.trace("topic:{}, connectionIds={}", topic, connectionIds);
        }
    }

    @EventListener(classes = UnsTopologyChangeEvent.class)
    void onTopologyChangeEvent(UnsTopologyChangeEvent event) {
        if (!topologySessions.isEmpty()) {
            JsonResult<String> rs = unsTopologyService.getLastMsg();
            String str = rs.getData();
            str = str != null ? str : "{}";
            TextMessage msg = new TextMessage(str);
            for (WebSocketSession session : topologySessions.values()) {
                try {
                    session.sendMessage(msg);
                } catch (IOException e) {
                    log.error("fail to sendWs: session={}", session);
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        if (exception != null && exception.getClass() != java.io.EOFException.class) {
            log.error("WebSocket handleTransportError[{}]", session.getId(), exception);
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        final String connectionId = session.getId();
        log.debug("ws ConnectionClosed: {}", connectionId);
        WsSubscription subscription = sessions.remove(connectionId);
        if (subscription != null && !CollectionUtils.isEmpty(subscription.topics)) {
            for (String topic : subscription.topics) {
                Set<String> connectionIds = topicToSessionsMap.get(topic);
                if (connectionIds != null) {
                    connectionIds.remove(connectionId);
                }
            }
        }
        topologySessions.remove(connectionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false; // 不支持部分消息
    }
}
