package com.adviser.regression.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickData implements ITickData {
    private String currency;
    private int tickNumber;
    private float price;
}
