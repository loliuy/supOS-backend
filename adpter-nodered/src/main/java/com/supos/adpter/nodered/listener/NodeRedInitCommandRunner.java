package com.supos.adpter.nodered.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supos.adpter.nodered.dao.mapper.NodeFlowMapper;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.service.ImportNodeRedFlowService;
import com.supos.adpter.nodered.service.NodeRedAdapterService;
import com.supos.common.exception.NodeRedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * init default flow
 * 移动到node-red服务，通过挂载的方式实现流程初始化
 */
@Component
@Slf4j
public class NodeRedInitCommandRunner implements CommandLineRunner {

    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;
    @Autowired
    private NodeFlowMapper nodeFlowMapper;

    @Override
    public void run(String... args) throws Exception {
        initFlow("/demo-it.json", "demo-it", "demo for it flow");
        initFlow("/demo-ot.json", "demo-ot", "demo for ot flow");
    }

    private void initFlow(String flowFile, String name, String desc) throws Exception {
        NodeFlowPO standardFlow = nodeFlowMapper.getByName(name);
        int retry = 5; // 失败重试
        while (retry > 0) {
            retry -= 1;
            try {
                if (standardFlow == null) {
                    log.info("==> start to init default flows, name = {}", name);
                    String defaultFlowJson = readDefaultFlowJson(flowFile);
                    JSONArray nodes = JSON.parseArray(defaultFlowJson);
                    long id = nodeRedAdapterService.createFlow(name, desc, "node-red");
                    nodeRedAdapterService.proxyDeploy(id, nodes, null);
                    log.info("<== init flow {} successfully", name);
                }
                return;
            } catch (Exception e) {
                log.error("failed: {}, sleep 20s and try again", e.getMessage());
                Thread.sleep(20_000); // sleep 20s
            }
        }
        log.error("<== finally, flow init failed");
    }

    private String readDefaultFlowJson(String filepath) {
        try (InputStream inputStream = ImportNodeRedFlowService.class.getResourceAsStream(filepath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder builder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            log.error("读取模版文件({})异常", filepath, e);
            throw new NodeRedException(400, "nodered.template.read.error");
        }
    }
}
