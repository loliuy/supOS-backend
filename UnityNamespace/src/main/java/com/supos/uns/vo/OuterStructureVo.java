package com.supos.uns.vo;

import com.supos.common.dto.FieldDefine;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OuterStructureVo {
    String dataPath;
    List<FieldDefine> fields;// 解析出的结果

    public OuterStructureVo(String dataPath, List<FieldDefine> fields) {
        this.dataPath = dataPath;
        this.fields = fields;
    }
}
