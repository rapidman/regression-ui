package com.adviser.regression.service;

import com.adviser.regression.model.Advise;
import com.adviser.regression.model.ITickData;
import com.adviser.regression.model.ModeData;
import com.adviser.regression.model.OrderType;
import com.adviser.regression.model.TickData;
import com.adviser.regression.model.TrendData;
import com.adviser.regression.ui.Visualiser;
import com.adviser.regression.utils.PersistenceUtils;
import org.jfree.data.function.PowerFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.adviser.regression.Constants.ADVISE_HISTORY_OFFSET;
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
    private Map<String, List<ITickData>> adviseHistory = new HashMap<>();

    private Map<String, LinkedList<ITickData>> ticks = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        PersistenceUtils.loadData(this);
    }

    public Advise getAdvise(String currency, Visualiser visualiser) {
        return getAdvise(new LinkedList<>(ticks.get(currency)), visualiser);
    }

    public Advise getAdvise(LinkedList<ITickData> tickDataList, Visualiser visualiser) {
        if (tickDataList.size() < 1 || tickDataList.size() < MODE_OFFSET) {
            return Advise.builder()
                    .orderType(OrderType.NONE)
                    .modePrice("0")
                    .antiModePrice("0")
                    .confirmAntiModePrice("0")
                    .build();
        }

        List<ITickData> shortTrendList = tickDataList.subList(tickDataList.size() - SHORT_TREND_OFFSET, tickDataList.size());
        // List<ITickData> stdTrendList = tickDataList.subList(tickDataList.size() - STD_TREND_OFFSET, tickDataList.size());
//        List<ITickData> longTrendList = tickDataList.subList(tickDataList.size() - MODE_OFFSET, tickDataList.size());
        List<ITickData> shortestTrendList = tickDataList.subList(tickDataList.size() - REGRESSION_LINE_COUNT, tickDataList.size());

//        TrendData longTrendData = drawTrendLine(longTrendList, null);
        TrendData shortTrendData = drawTrendLine(shortTrendList, visualiser);
        TrendData shortestTrendData = drawTrendLine(shortestTrendList, null);

        // double deviation = MathUtils.getStandardDeviation(stdTrendList);
//        System.out.println(deviation);


        Map<Float, Integer> prices = getPricesMap(tickDataList, tickDataList.size(), MODE_OFFSET);
        ModeData modeData = getModeData(prices);
        Advise result = Advise.builder()
                .closedTick(shortTrendData.getClosedTick())
                .closePrice(shortTrendData.getClosePrice())
                .openPrice(shortTrendData.getOpenPrice())
                .modePrice(String.valueOf(modeData.getModePrice()))
                .antiModePrice(String.valueOf(modeData.getAntiModePrice()))
                .confirmAntiModePrice(String.valueOf(modeData.getConfirmAntiModePrice()))
//                .orderType(longTrendData.isUp() ? OrderType.BUY : OrderType.SELL)
                .hedgingOrderType(shortTrendData.isShortUp() ? OrderType.BUY : OrderType.SELL)
                .build();


//        if (deviation < STD_TRESHOLD) {
//            String confirmAntiModePrice = String.valueOf(shortestTrendList.get(shortestTrendList.size() - 1).getPrice());
//            result.setConfirmAntiModePrice(confirmAntiModePrice);
//            //System.out.println("confirmAntiModePrice:" + confirmAntiModePrice);
//        }


        String currency = ((TickData) tickDataList.get(0)).getCurrency();
        List<ITickData> adviseHistory = this.adviseHistory.get(currency);
        if (adviseHistory == null) {
            adviseHistory = new ArrayList<>();
            this.adviseHistory.put(currency, adviseHistory);
        }
        adviseHistory.add(result);
        if (adviseHistory.size() > ADVISE_HISTORY_OFFSET) {
            adviseHistory.remove(0);
        }
        if (adviseHistory.size() < ADVISE_HISTORY_OFFSET) {
            result.setOrderType(OrderType.NONE);
        } else {
            TrendData modeTrendData = drawTrendLine(adviseHistory, null);
            prices = getPricesMap(adviseHistory, adviseHistory.size(), ADVISE_HISTORY_OFFSET);
            modeData = getModeData(prices);
            result = Advise.builder()
                    .closedTick(shortTrendData.getClosedTick())
                    .closePrice(shortTrendData.getClosePrice())
                    .openPrice(shortTrendData.getOpenPrice())
                    .modePrice(String.valueOf(modeData.getModePrice()))
                    .antiModePrice(String.valueOf(modeData.getAntiModePrice()))
                    .confirmAntiModePrice(String.valueOf(modeData.getConfirmAntiModePrice()))
                    .orderType(modeTrendData.isUp() ? OrderType.BUY : OrderType.SELL)
                    .hedgingOrderType(shortTrendData.isShortUp() ? OrderType.BUY : OrderType.SELL)
                    .build();
        }
        return result;
    }

    private TrendData drawTrendLine(List<ITickData> tickDataList, Visualiser visualiser) {
        double[] regressionParameters = Regression.getPowerRegression(getSeries(tickDataList), 0);

//        LineFunction2D linefunction2d = new LineFunction2D(
//                regressionParameters[0], regressionParameters[1]);
        PowerFunction2D powerFunction2D = new PowerFunction2D(regressionParameters[0], regressionParameters[1]);

        int startTickNumber = tickDataList.get(0).getTickNumber();
        int endTickNumber = tickDataList.get(tickDataList.size() - 1).getTickNumber();


        XYDataset dataset = DatasetUtilities.sampleFunction2D(powerFunction2D,
                startTickNumber, endTickNumber, 200, "");
        float openPrice = tickDataList.get(0).getPrice();
        float closePrice = tickDataList.get(tickDataList.size() - 1).getPrice();
        boolean up = openPrice < closePrice;

        if (visualiser != null) {
            showRegression(visualiser, count.incrementAndGet(), regressionParameters, dataset, openPrice, closePrice, up);
        }
        boolean shortUp = isUpTrend(tickDataList.get(tickDataList.size() - REGRESSION_LINE_COUNT).getTickNumber(), endTickNumber, powerFunction2D);
        return TrendData.builder()
                .closedTick(endTickNumber)
                .closePrice(closePrice)
                .openPrice(openPrice)
                .up(up)
                .shortUp(shortUp)
                .build();
    }


    @Override
    public void addTickData(TickData tickData, boolean saveToFile) throws IOException {
        tickData.setTickNumber(PersistenceUtils.TICK_COUNT.incrementAndGet());
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
        LinkedList<ITickData> ticks = new LinkedList<>();

        Map<Float, Integer> prices = new HashMap<>();
        for (ITickData tickData : this.ticks.get(currency)) {
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
//        drawPoints(currency, visualiser, true);
//        drawPoints(currency, visualiser, false);
    }

//    private void drawPoints(String currency, Visualiser visualiser, boolean mode) {
//        XYSeriesCollection seriesCollection = new XYSeriesCollection();
//        XYSeries series = new XYSeries("Mode");
//        for (ITickData advise : adviseHistory.get(currency)) {
//            series.add(advise.getClosedTick(), Float.parseFloat(mode ? advise.getModePrice() : advise.getAntiModePrice()));
//        }
//        seriesCollection.addSeries(series);
//        visualiser.drawPoints(this.count.incrementAndGet(), seriesCollection, mode ? Color.GREEN : Color.BLACK);
//    }
}

