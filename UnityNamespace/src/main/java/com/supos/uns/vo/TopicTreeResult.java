package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.vo.FieldDefineVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.TreeSet;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicTreeResult {
    @Schema(description = "节点值，本期表示消费下来的数据条数")
    Long value; //节点值，本期表示消费下来的数据条数
    @Schema(description = "最新更新时间")
    Long lastUpdateTime;
    @Schema(description = "子节点数")
    Integer countChildren;
    @Schema(description = "节点类型: 0--纯路径，1--模型, 2--实例")
    int type; // 节点类型: 0--纯路径，1--模型, 2--实例
    @Schema(description = "显示名称")
    String name;//显示名称
    @Schema(description = "树的路径")
    String path;//树的路径
    @Schema(description = "协议类型")
    String protocol; // 协议类型
    @Schema(description = "字段定义")
    FieldDefineVo[] fields;// 字段定义
    @Schema(description = "子节点")
    Collection<TopicTreeResult> children;// 子节点

    public TopicTreeResult(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public void addChild(TopicTreeResult child) {
        if (children == null) {
            children = new TreeSet<>((o1, o2) -> {
                String a = o1.name, b = o2.name;
                if ("".equals(o1.name) && "".equals(o2.name) ) {
                    return 0;
                }
                if (Character.isDigit(a.charAt(a.length() - 1)) && Character.isDigit(b.charAt(b.length() - 1))) {
                    int i = numberStartIndex(a), j = numberStartIndex(b);
                    String n1 = i > 0 ? a.substring(0, i) : "", n2 = j > 0 ? b.substring(0, j) : "";
                    int ns = n1.compareTo(n2);// 先比较字符串前缀
                    if (ns != 0) {
                        return ns;
                    }
                    String aNumStr = a.substring(i), bNumStr = b.substring(j);
                    return StringUtils.compare(aNumStr, bNumStr);// 按后缀数字大小排序
                } else {
                    int r = a.compareTo(b);
                    if (r == 0) {
                        return o1.path.compareTo(o2.path);
                    }
                    return r;
                }
            });//sort by name
        }
        child.parent = this;
        children.add(child);
    }

    private static int numberStartIndex(String s) {
        boolean prevIsNumber = false;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (Character.isDigit(s.charAt(i))) {
                prevIsNumber = true;
            } else if (prevIsNumber) {
                return i + 1;
            }
        }
        return prevIsNumber ? 0 : -1;
    }

    public Integer getCountChildren() {
        if (countChildren == null) {
            if (children != null) {
                int count = 0;
                for (TopicTreeResult child : children) {
                    if (child.type == 2) {
                        count++;
                    }
                    count += child.getCountChildren();
                }
                countChildren = count;
            } else {
                countChildren = 0;
            }
        }
        return countChildren;
    }

    public Long getValue() {
        if (value == null) {
            if (children != null) {
                long total = 0;
                for (TopicTreeResult child : children) {
                    total += child.getValue();
                }
                value = total;
            } else {
                value = 0L;
            }
        }
        return value;
    }

    public TopicTreeResult setType(int type) {
        this.type = type;
        return this;
    }

    public TopicTreeResult setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getName() {
        Long value;
        if (lastUpdateTime != null && (value = getValue()) != null && value > 0 && name != null) {
            return name + '(' + value + ')';
        }
        return name;
    }

    public
    @JsonIgnore
    transient TopicTreeResult parent;

    public String toString() {
        return path;
    }
}
