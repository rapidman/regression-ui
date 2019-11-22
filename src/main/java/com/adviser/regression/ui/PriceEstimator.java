package com.adviser.regression.ui;

import com.adviser.regression.model.TickData;
import com.adviser.regression.model.VisualiserData;
import com.adviser.regression.service.Adviser;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import static com.adviser.regression.service.Adviser.REAL_PRICE;
import static org.springframework.util.ResourceUtils.getFile;

public class PriceEstimator extends ApplicationFrame implements Visualiser {
    private static final String CURRENCY = "USD";
    XYDataset inputData;
    JFreeChart chart;
    Adviser adviser = new Adviser();

    public static void main(String[] args) throws IOException {
        final String inputFileName = Adviser.TMP_FOREX_TICKS_FILE;
        final PriceEstimator demo = new PriceEstimator(inputFileName);
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

        Thread th = new Thread(new Runnable() {
            public void run() {
                // Draw the regression line on the chart
                demo.adviser.drawLines(CURRENCY, demo);
            }
        });
        th.start();
    }

    public PriceEstimator(String inputFileName) throws IOException {
        super("Linear Regression");

        // Read sample data from prices.txt file
        inputData = createDatasetFromFile(inputFileName);

        // Create the chart using the sample data
        chart = createChart(inputData);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }

    public XYDataset createDatasetFromFile(String inputFileName) throws IOException {
        File file = getFile(inputFileName);
        Scanner scanner = new Scanner(file);

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(REAL_PRICE);

        int lineCount = 0;
        while (scanner.hasNextLine() && scanner.hasNext()) {
            addSeries(scanner, series);
            lineCount++;
            if (lineCount % 10000 == 0) {
                System.out.println(lineCount);
            }
        }
        scanner.close();
        dataset.addSeries(series);

        return dataset;
    }

    public void addSeries(Scanner scanner, XYSeries series) throws IOException {
        scanner.next();
        scanner.next();
        int xTickNumber = scanner.nextInt();
        float yPrice = scanner.nextFloat();
        series.add(xTickNumber, yPrice);
        adviser.addTickData(TickData.builder()
                .currency(CURRENCY)
                .price(yPrice)
                .tickNumber(xTickNumber)
                .build(), false);
    }

    private JFreeChart createChart(XYDataset inputData) throws IOException {
        // Create the chart using the data read from the prices.txt file
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Price by ticks", "Ticks", "Prices", inputData,
                PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, Color.blue);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        return chart;
    }


    @Override
    public void show(VisualiserData data) {
        XYPlot xyplot = chart.getXYPlot();

        xyplot.setDomainPannable(true);
        xyplot.setDataset(data.getIndex(), data.getDataset());
        XYLineAndShapeRenderer xylineandshaperenderer = new XYLineAndShapeRenderer(
                true, false);

        xylineandshaperenderer.setSeriesPaint(0, data.isUp() ? Color.YELLOW : Color.RED);
        xyplot.setRenderer(data.getIndex(), xylineandshaperenderer);


    }

    @Override
    public void drawHorizontalLine(float yCoordinate, String label, Color color) {
        ValueMarker marker = new ValueMarker(yCoordinate);  // position is the value on the axis
        marker.setPaint(color);
        marker.setLabel(label); // see JavaDoc for labels, colors, strokes
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.addRangeMarker(marker);
    }

    @Override
    public void drawVerticalLine(int xCoordinate, String label, Color color) {
        ValueMarker marker = new ValueMarker(xCoordinate);  // position is the value on the axis
        marker.setPaint(color);
        marker.setLabel(label); // see JavaDoc for labels, colors, strokes
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.addDomainMarker(marker);
    }
}
