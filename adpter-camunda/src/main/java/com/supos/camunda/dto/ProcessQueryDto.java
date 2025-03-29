package com.supos.camunda.dto;

import com.supos.common.dto.PaginationDTO;
import lombok.Data;

@Data
public class ProcessQueryDto extends PaginationDTO {


    private String processDefinitionName;

    private String description;
}
