package com.adviser.regression.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advise {
    private OrderType orderType;
    @Builder.Default
    private OrderType hedgingOrderType = OrderType.NONE;
    private int closedTick;
    private float openPrice;
    private float closePrice;
    private String modePrice;
    private String antiModePrice;
    private String confirmAntiModePrice;
}
