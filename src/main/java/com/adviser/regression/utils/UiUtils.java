package com.adviser.regression.utils;

import com.adviser.regression.model.TickData;
import com.adviser.regression.model.VisualiserData;
import com.adviser.regression.ui.Visualiser;
import lombok.experimental.UtilityClass;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.IOException;
import java.util.List;

@UtilityClass
public class UiUtils {
    public static final String REAL_PRICE = "Real price";
    public static XYSeriesCollection getSeries(List<TickData> shortTrendList) {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        XYSeries series = new XYSeries(REAL_PRICE);
        for (TickData tickData : shortTrendList) {
            series.add(tickData.getTickNumber(), tickData.getPrice());
        }
        seriesCollection.addSeries(series);
        return seriesCollection;
    }

    public static void showRegression(Visualiser visualiser,
                                      int adviseCount,
                                      double[] regressionParameters,
                                      XYDataset dataset,
                                      float openPrice,
                                      float closePrice,
                                      boolean up) {
        visualiser.show(VisualiserData.builder()
                .closePrice(closePrice)
                .up(up)
                .openPrice(openPrice)
                .regressionParameters(regressionParameters)
                .index(adviseCount)
                .dataset(dataset)
                .build());
    }

    public static JFreeChart createChart(XYDataset inputData) throws IOException {
        // Create the chart using the data read from the prices.txt file
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Price by ticks", "Ticks", "Prices", inputData,
                PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, Color.blue);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        return chart;
    }
}
