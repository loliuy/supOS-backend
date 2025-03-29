package com.supos.common.dto.mock;

import cn.hutool.core.util.RandomUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.RoundingMode;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MockDemoDTO {

    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 装机容量
     */
    private double installedCapacity;

    /**
     * 日发电量
     */
    private String dailyPowerGeneration;

    /**
     * 负责人
     */
    private String owner;

    public MockDemoDTO mockDemoData(){
        MockDemoDTO demo = new MockDemoDTO();
        demo.setId(RandomUtil.randomInt());
        String name = "";
        String owner = "";
        if ("en-US".equals(System.getenv("SYS_OS_LANG"))){
            name = "Photovoltaic base station-";
            owner = "Jack";
        } else {
            name = "光伏基站-";
            owner = "张灿";
        }
        demo.setName(name + RandomUtil.randomString(4));
        demo.setInstalledCapacity(RandomUtil.randomDouble(10000,2, RoundingMode.UP));
        demo.setDailyPowerGeneration(RandomUtil.randomNumbers(5));
        demo.setOwner(owner);
        return demo;
    }


    public Object[] convertOrderToParams() {
        return new Object[] { this.getId(), this.getName(), this.getInstalledCapacity(),this.getDailyPowerGeneration(),this.getOwner() };
    }
}
