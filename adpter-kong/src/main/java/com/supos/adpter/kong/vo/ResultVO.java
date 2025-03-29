package com.supos.adpter.kong.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultVO<T> implements Serializable {

    private static final long serialVersionUID = 1l;

    private int code;

    private String msg;

    private T data;

    public static <T> ResultVO success(T data) {
        return ResultVO.builder().code(200).data(data).build();
    }

    public static <T> ResultVO success(String msg) {
        return ResultVO.builder().code(200).msg(msg).build();
    }

    public static <T> ResultVO fail(String msg) {
        return ResultVO.builder().code(500).msg(msg).build();
    }

}
