package com.supos.common.dto;


import com.supos.common.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页对象
 * @author xinwangji@supos.com
 * @date 2022/11/24 10:10
 * @description
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationDTO {

    /**
     * 当前页数
     * 默认为第1页
     */
    @Schema(description = "当前页数")
    private Integer pageNo = Constants.DEFAULT_PAGE_NUM;
    /**
     * 每页记录数
     * 默认为20条记录，最大返回1000条记录
     */
    @Schema(description = "每页记录数")
    private Integer pageSize = Constants.DEFAULT_PAGE_SIZE;
    /**
     * 总页数
     */
    private Integer totalPage;
    /**
     * 总记录数
     */
    private Integer total;

    public Integer getPageNo() {
        return pageNo = pageNo == null ? Constants.DEFAULT_PAGE_NUM: pageNo;
    }

    public Integer getPageSize() {
        return pageSize = pageSize > Constants.MAX_PAGE_SIZE ? Constants.MAX_PAGE_SIZE : pageSize;
    }
}
