package com.supos.uns.vo;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class FileVo {

    String unsId;
    String name;//显示名称
    String path;//树的路径
    /**
     * 0--虚拟路径，1--模板，2--实例
     */
    Integer pathType;

}
