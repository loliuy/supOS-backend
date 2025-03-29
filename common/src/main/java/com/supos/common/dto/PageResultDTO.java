package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResultDTO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数")
    private long pageNo;

    @Schema(description = "每页记录数")
    private long pageSize;

    private long total;

    private long code;

    private List<T> data;


}
