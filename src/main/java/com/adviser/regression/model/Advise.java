package com.adviser.regression.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advise implements ITickData {
    private OrderType orderType;
    @Builder.Default
    private OrderType hedgingOrderType = OrderType.NONE;
    private int closedTick;
    private float openPrice;
    private float closePrice;
    private String modePrice;
    private String antiModePrice;
    private String confirmAntiModePrice;
    @Builder.Default
    private String impulse = "false";

    @Override
    public int getTickNumber() {
        return closedTick;
    }

    @Override
    public float getPrice() {
        return Float.parseFloat(modePrice);
    }
}
