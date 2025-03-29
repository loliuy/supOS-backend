package com.supos.adpter.nodered;

import com.alibaba.fastjson.JSON;
import com.supos.adpter.nodered.service.enums.IdentifiersInterface;
import com.supos.adpter.nodered.service.register.IdentifiersContext;
import com.supos.adpter.nodered.vo.*;
import com.supos.common.dto.ResultDTO;
import com.supos.common.dto.protocol.BaseConfigDTO;
import com.supos.common.dto.protocol.ProtocolTagEnums;
import com.supos.common.enums.IOTProtocol;
import com.supos.adpter.nodered.service.ProtocolServerService;
import com.supos.common.exception.NodeRedException;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@Slf4j
public class ProtocolServerController {

    @Autowired
    private ProtocolServerService protocolServerService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private IdentifiersContext identifiersContext;

    /**
     * 查询协议下拉列表
     * @return
     */
    @GetMapping("/inter-api/supos/protocols")
    @ResponseBody
    public ResultVO queryProtocol() {
        List<ProtocolsResponseVO> protocolNames = protocolServerService.queryProtocols();
        return ResultVO.successWithData(protocolNames);
    }

    @GetMapping("/inter-api/supos/protocol/{name}")
    public ResultVO getProtocol(@PathVariable("name") String name) {
        ProtocolResponseVO protocol = protocolServerService.getProtocol(name);
        return ResultVO.successWithData(protocol);
    }

    @DeleteMapping("/inter-api/supos/protocol/{name}")
    @ResponseBody
    public ResultVO deleteProtocol(@PathVariable("name") String name) {
        protocolServerService.deleteProtocol(name);
        return ResultVO.success("ok");
    }

    @PostMapping("/inter-api/supos/protocol")
    @ResponseBody
    public ResultVO addProtocol(@Valid @RequestBody AddProtocolRequestVO addProtocolRequest) {
        AddProtocolResponseVO response = protocolServerService.addProtocol(addProtocolRequest);
        return ResultVO.successWithData(response);
    }

    /**
     * 对前端导入的节点配置进行匹配和清洗
     * @param configs
     * @return
     */
    @PostMapping("/inter-api/supos/protocol/format")
    @ResponseBody
    public ResultVO formatProtocol(@RequestBody List<Map<String, Object>> configs) {
        if (configs.size() < 2) {
            throw new NodeRedException(400, "nodered.protocol.import.size.min");
        }
        if (configs.size() > 2) {
            throw new NodeRedException(400, "nodered.protocol.import.size.max");
        }
        ProtocolImportResponseVO response = protocolServerService.cleanProtocolConfig(configs);
        return ResultVO.successWithData(response);
    }

    /**
     * 添加协议server端配置，例如modbus-server， opcua-server
     * @param addServerRequest
     * @return
     */
    @PostMapping("/inter-api/supos/protocol/server")
    public ResultVO addServer(@RequestBody AddServerRequestVO addServerRequest) {
        /*if (!IOTProtocol.contains(addServerRequest.getProtocolName())) {
            throw new NodeRedException(400, "nodered.protocol.unsupported", addServerRequest.getProtocolName());
        }*/
        protocolServerService.addServer(addServerRequest, true);
        return ResultVO.success("ok");
    }

    @GetMapping("/inter-api/supos/protocol/servers")
    public ResultVO<List> queryServers(@Nullable @RequestParam("k") String protocolName) {
        List<NodeServerVO> nodeServerVOS = protocolServerService.searchList(protocolName);
        return ResultVO.successWithData(nodeServerVOS);
    }

    @DeleteMapping("/inter-api/supos/protocol/server/{id}")
    public ResultVO deleteServer(@PathVariable("id") String id) {
        protocolServerService.deleteById(id);
        return ResultVO.success("ok");
    }

    /**
     * 枚举位号
     * @param tagsRequest
     * @return
     */
    @PostMapping({"/inter-api/supos/protocol/tags"})
    public ResultDTO<List<NodeFlowVO>> browseTags(@RequestBody ProtocolTagsRequestVO tagsRequest) {
        IOTProtocol protocol = IOTProtocol.getByName(tagsRequest.getProtocolName());
        // 根据协议获取对应的操作类
        IdentifiersInterface<BaseConfigDTO> instance = identifiersContext.getInstance(protocol);
        // 解析对应协议的配置
        Object config = JSON.parseObject(tagsRequest.getServerConfig(), protocol.getProtocolClass());
        // 查询位号列表
        Collection<ProtocolTagEnums> protocolTagEnums = instance.listTags((BaseConfigDTO) config, tagsRequest.getTopic());
        return ResultDTO.successWithData(protocolTagEnums);
    }
}
