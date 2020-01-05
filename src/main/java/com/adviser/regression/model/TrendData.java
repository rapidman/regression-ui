package com.adviser.regression.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendData {
    private int closedTick;
    private float openPrice;
    private float closePrice;
    int tickNumber;
    boolean up;
    boolean shortUp;
    boolean impulse;
}
