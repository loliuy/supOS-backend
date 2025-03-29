package com.supos.uns.vo;

import com.supos.common.dto.PaginationDTO;
import lombok.Data;

@Data
public class AlarmQueryVo extends PaginationDTO {


    private String topic;

    /**
     * 是否已读
     */
    private Boolean readStatus;

}
