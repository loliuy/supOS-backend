package com.supos.uns.vo;

import com.supos.common.dto.PaginationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateQueryVo extends PaginationDTO {


    @Schema(description = "搜索关键字")
    String key;

}
