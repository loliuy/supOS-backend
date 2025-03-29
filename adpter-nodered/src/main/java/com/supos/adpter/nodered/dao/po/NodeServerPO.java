package com.supos.adpter.nodered.dao.po;

import lombok.Data;

@Data
public class NodeServerPO {

    private String id;

    private String serverName;

    private String protocolName;

    private String configJson;
}
