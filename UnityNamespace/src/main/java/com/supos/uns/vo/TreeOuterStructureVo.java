package com.supos.uns.vo;

import com.supos.common.dto.FieldDefine;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TreeOuterStructureVo{

    String name;//解析出来的key

    String dataPath;//数据路径

    List<FieldDefine> fields;//解析出来的字段信息

    List<TreeOuterStructureVo> children = new ArrayList<>(); //子节点信息
}
