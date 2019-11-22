package com.adviser.regression.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jfree.data.xy.XYDataset;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisualiserData {
    private float openPrice;
    private float closePrice;
    private boolean up;
    double[] regressionParameters;
    int index;
    XYDataset dataset;

}
