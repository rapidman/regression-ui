package com.adviser.regression.model;

import lombok.Data;

@Data
public class ModeData {
    float modePrice = 0;
    int maxPriceCount = 0;
    float antiModePrice = 0;
    float confirmAntiModePrice = 0;
    int minPriceCount = Integer.MAX_VALUE;
}
