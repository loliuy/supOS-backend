package com.supos.webhook.dao.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookPO {
    private long id;

    private String name;

    private String subscribeEvent;

    private String url;

    private String headers;

    private int status;

    private String description;
}
