package com.supos.common.dto;

import lombok.Data;

@Data
public class PageDto {
    int page;
    int pageSize;
    long total;
}
