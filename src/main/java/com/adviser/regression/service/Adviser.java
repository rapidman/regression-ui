package com.adviser.regression.service;

import com.adviser.regression.model.Advise;
import com.adviser.regression.model.ModeData;
import com.adviser.regression.model.OrderType;
import com.adviser.regression.model.TickData;
import com.adviser.regression.model.TrendData;
import com.adviser.regression.ui.Visualiser;
import com.adviser.regression.utils.PersistenceUtils;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.adviser.regression.Constants.MODE_OFFSET;
import static com.adviser.regression.Constants.REGRESSION_LINE_COUNT;
import static com.adviser.regression.Constants.SHORT_TREND_OFFSET;
import static com.adviser.regression.utils.MathUtils.getModeData;
import static com.adviser.regression.utils.MathUtils.getPricesMap;
import static com.adviser.regression.utils.MathUtils.isUpTrend;
import static com.adviser.regression.utils.UiUtils.getSeries;
import static com.adviser.regression.utils.UiUtils.showRegression;

@Component
public class Adviser implements DataConsumer {


    private AtomicInteger count = new AtomicInteger(0);
    private Map<String, LinkedList<Advise>> adviseHistory = new HashMap<>();

    private Map<String, LinkedList<TickData>> ticks = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        PersistenceUtils.loadData(this);
    }

    public Advise getAdvise(String currency, Visualiser visualiser) {
        return getAdvise(new LinkedList<>(ticks.get(currency)), visualiser);
    }

    public Advise getAdvise(LinkedList<TickData> tickDataList, Visualiser visualiser) {
        if (tickDataList.size() < 1 || tickDataList.size() < MODE_OFFSET) {
            return Advise.builder()
                    .orderType(OrderType.NONE)
                    .modePrice("0")
                    .antiModePrice("0")
                    .build();
        }

        List<TickData> shortTrendList = tickDataList.subList(tickDataList.size() - SHORT_TREND_OFFSET, tickDataList.size());
        List<TickData> longTrendList = tickDataList.subList(tickDataList.size() - MODE_OFFSET, tickDataList.size());
        List<TickData> shortestTrendList = tickDataList.subList(tickDataList.size() - REGRESSION_LINE_COUNT, tickDataList.size());

        TrendData longTrendData = drawTrendLine(longTrendList, visualiser);
        TrendData shortTrendData = drawTrendLine(shortTrendList, null);
        TrendData shortestTrendData = drawTrendLine(shortestTrendList, null);


        Map<Float, Integer> prices = getPricesMap(tickDataList, tickDataList.size(), MODE_OFFSET);
        ModeData modeData = getModeData(prices);
        Advise result = Advise.builder()
                .closedTick(shortTrendData.getClosedTick())
                .closePrice(shortTrendData.getClosePrice())
                .openPrice(shortTrendData.getOpenPrice())
                .modePrice(String.valueOf(modeData.getModePrice()))
                .antiModePrice(String.valueOf(modeData.getAntiModePrice()))
                .orderType(longTrendData.isUp() ? OrderType.BUY : OrderType.SELL)
                .hedgingOrderType(shortTrendData.isShortUp() ? OrderType.BUY : OrderType.SELL)
                .build();

//        if (adviseHistory.size() > 0) {
//            Advise previousAdvise = adviseHistory.getLast();
//            if ((previousAdvise.getOrderType() == OrderType.BUY && result.getOrderType() == OrderType.BUY)
//                    && (result.getClosePrice() - result.getOpenPrice()) > (previousAdvise.getClosePrice() - previousAdvise.getOpenPrice())) {
//                result.setHedgingOrderType(OrderType.BUY);
//            }
//            if ((previousAdvise.getOrderType() == OrderType.SELL && result.getOrderType() == OrderType.SELL)
//                    && (result.getOpenPrice() - result.getClosePrice()) > (previousAdvise.getOpenPrice() - previousAdvise.getClosePrice())) {
//                result.setHedgingOrderType(OrderType.SELL);
//            }
//        }
        String currency = tickDataList.get(0).getCurrency();
        LinkedList<Advise> historyMap = adviseHistory.get(currency);
        if (historyMap == null) {
            historyMap = new LinkedList<>();
            adviseHistory.put(currency, historyMap);
        }
        historyMap.push(result);
        return result;
    }

    private TrendData drawTrendLine(List<TickData> tickDataList, Visualiser visualiser) {
        double[] regressionParameters = Regression.getOLSRegression(getSeries(tickDataList), 0);

        LineFunction2D linefunction2d = new LineFunction2D(
                regressionParameters[0], regressionParameters[1]);

        int start = tickDataList.get(0).getTickNumber();
        int tickNumber = tickDataList.get(tickDataList.size() - 1).getTickNumber();


        XYDataset dataset = DatasetUtilities.sampleFunction2D(linefunction2d,
                start, tickNumber, 200, "");
        float openPrice = dataset.getY(0, 0).floatValue();
        float closePrice = dataset.getY(0, 1).floatValue();
        boolean up = openPrice < closePrice;

        if (visualiser != null) {
            showRegression(visualiser, count.incrementAndGet(), regressionParameters, dataset, openPrice, closePrice, up);
        }
        boolean shortUp = isUpTrend(tickDataList.get(tickDataList.size() - REGRESSION_LINE_COUNT).getTickNumber(), tickNumber, linefunction2d);
        return TrendData.builder()
                .closedTick(tickNumber)
                .closePrice(closePrice)
                .openPrice(openPrice)
                .up(up)
                .shortUp(shortUp)
                .build();
    }


    @Override
    public void addTickData(TickData tickData, boolean saveToFile) throws IOException {
        ticks.computeIfAbsent(tickData.getCurrency(), s -> new LinkedList<>())
                .add(tickData);
        int size = ticks.get(tickData.getCurrency()).size();
        if (size % 1000 == 0) {
            System.out.println("ticks size:" + size);
        }
        if (saveToFile && size > MODE_OFFSET + 1) {
            ticks.get(tickData.getCurrency()).removeFirst();
        }
        if (!saveToFile) {
            return;
        }

        PersistenceUtils.saveLine(tickData);
    }

    public void drawLines(String currency, Visualiser visualiser) {
        int count = 0;
        LinkedList<TickData> ticks = new LinkedList<>();

        Map<Float, Integer> prices = new HashMap<>();
        for (TickData tickData : this.ticks.get(currency)) {
            ticks.add(tickData);
            Integer currentCount = prices.computeIfAbsent(tickData.getPrice(), aFloat -> 0);
            prices.put(tickData.getPrice(), ++currentCount);
            if (count % REGRESSION_LINE_COUNT * 50 == 0) {
                Advise advise = getAdvise(ticks, visualiser);
//                    if (OrderType.SELL == advise.getOrderType() && OrderType.BUY == advise.getHedgingOrderType()) {
//                        visualiser.drawVerticalLine(advise.getClosedTick(), "", Color.YELLOW);
//                    }
//                    if (OrderType.BUY == advise.getOrderType() && OrderType.SELL == advise.getHedgingOrderType()) {
//                        visualiser.drawVerticalLine(advise.getClosedTick(), "", Color.RED);
//                    }
                System.out.println(advise);

//                    visualiser.drawHorizontalLine(Float.parseFloat(advise.getModePrice()), "____ " + advise.getModePrice(), Color.black);
//                    visualiser.drawHorizontalLine(Float.parseFloat(advise.getAntiModePrice()), "___ " + advise.getAntiModePrice(), Color.RED);
            }
            count++;
        }
        drawPoints(currency, visualiser, true);
        drawPoints(currency, visualiser, false);
    }

    private void drawPoints(String currency, Visualiser visualiser, boolean mode) {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        XYSeries series = new XYSeries("Mode");
        for (Advise advise : adviseHistory.get(currency)) {
            series.add(advise.getClosedTick(), Float.parseFloat(mode ? advise.getModePrice() : advise.getAntiModePrice()));
        }
        seriesCollection.addSeries(series);
        visualiser.drawPoints(this.count.incrementAndGet(), seriesCollection, mode ? Color.GREEN : Color.BLACK);
    }
}

