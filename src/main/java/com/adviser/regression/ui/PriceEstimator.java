package com.adviser.regression.ui;

import com.adviser.regression.service.Adviser;
import org.jfree.chart.ChartPanel;
import org.jfree.ui.RefineryUtilities;

import java.io.IOException;

import static com.adviser.regression.utils.PersistenceUtils.TMP_FX_TICKS_FILE;
import static com.adviser.regression.utils.PersistenceUtils.createDatasetFromFile;

public class PriceEstimator extends BaseVisualiser {
    private static final String CURRENCY = "USD";

    public static void main(String[] args) throws IOException {
        final String inputFileName = TMP_FX_TICKS_FILE;
        Adviser adviser = new Adviser();
        final PriceEstimator demo = new PriceEstimator(inputFileName, adviser);
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);
        Thread th = new Thread(() -> {
            // Draw the regression line on the chart
            adviser.drawLines(CURRENCY, demo);
        });
        th.start();
    }

    public PriceEstimator(String inputFileName, Adviser adviser) throws IOException {
        super("Linear Regression", createDatasetFromFile(inputFileName, adviser, CURRENCY));
        // Read sample data from prices.txt file


        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
    }


}
