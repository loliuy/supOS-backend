package com.supos.uns.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class LabelVo {

    @NotNull
    private Long id;

    private String labelName;

    private List<FileVo> fileVoList;

}
