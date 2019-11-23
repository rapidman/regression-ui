package com.adviser.regression.ui;

import com.adviser.regression.model.VisualiserData;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYDrawableAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.block.ColorBlock;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.Drawable;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.ColorModel;
import java.io.IOException;

import static com.adviser.regression.utils.UiUtils.REAL_PRICE;
import static com.adviser.regression.utils.UiUtils.createChart;

public abstract class BaseVisualiser extends ApplicationFrame implements Visualiser {
    JFreeChart chart;

    public BaseVisualiser(String title, XYDataset inputData) throws IOException {
        super(title);
        // Create the chart using the sample data
        chart = createChart(inputData);
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

    @Override
    public void drawPoints(int index, XYDataset xyDataset, Color color) {
        XYPlot xyplot = chart.getXYPlot();

        xyplot.setDomainPannable(true);
        xyplot.setDataset(index, xyDataset);
        XYLineAndShapeRenderer xylineandshaperenderer = new XYLineAndShapeRenderer(
                false, true);

        xylineandshaperenderer.setSeriesPaint(0, color);
        xyplot.setRenderer(index, xylineandshaperenderer);
    }

}
