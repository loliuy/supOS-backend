package com.supos.common.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MockWeatherDTO {

    private String city;

    private double temperature;

    private double humidity;
}
