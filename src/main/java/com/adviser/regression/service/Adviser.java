package com.adviser.regression.service;

import com.adviser.regression.model.Advise;
import com.adviser.regression.model.ModeData;
import com.adviser.regression.model.OrderType;
import com.adviser.regression.model.TickData;
import com.adviser.regression.model.TrendData;
import com.adviser.regression.model.VisualiserData;
import com.adviser.regression.ui.Visualiser;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.adviser.regression.Constants.SHORT_TREND_OFFSET;
import static com.adviser.regression.Constants.MODE_OFFSET;
import static com.adviser.regression.Constants.REGRESSION_LINE_COUNT;

@Component
public class Adviser {
    public static final String REAL_PRICE = "Real price";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final String TMP_FOREX_TICKS_FILE = "/tmp/forex_ticks.txt";
    private AtomicInteger count = new AtomicInteger(0);
    private LinkedList<Advise> adviseHistory = new LinkedList<>();

    private Map<String, LinkedList<TickData>> ticks = new ConcurrentHashMap<>();
    private File file = new File(TMP_FOREX_TICKS_FILE);

    @PostConstruct
    public void init() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            file.createNewFile();
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                scanner.next();
                scanner.next();
                addTickData(TickData.builder()
                        .tickNumber(scanner.nextInt())
                        .currency("usd")
                        .price(scanner.nextFloat())
                        .build(), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        TrendData shortTrendData = drawTrendLine(shortTrendList, visualiser);
        TrendData shortestTrendData = drawTrendLine(shortestTrendList, visualiser);


        Map<Float, Integer> prices = getPricesMap(tickDataList, tickDataList.size(), MODE_OFFSET);
        ModeData modeData = getModeData(prices);
        Advise result = Advise.builder()
                .closedTick(shortTrendData.getTickNumber())
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
        adviseHistory.add(result);
        return result;
    }

    private TrendData drawTrendLine(List<TickData> tickDataList, Visualiser visualiser){
        double[] regressionParameters = Regression.getOLSRegression(getSeries(tickDataList), 0);

        LineFunction2D linefunction2d = new LineFunction2D(
                regressionParameters[0], regressionParameters[1]);

        int start = tickDataList.get(0).getTickNumber();
        int tickNumber = tickDataList.get(tickDataList.size() -1).getTickNumber();


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

    private XYSeriesCollection getSeries(List<TickData> shortTrendList) {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        XYSeries series = new XYSeries(REAL_PRICE);
        for (TickData tickData : shortTrendList) {
            series.add(tickData.getTickNumber(), tickData.getPrice());
        }
        seriesCollection.addSeries(series);
        return seriesCollection;
    }

    private Map<Float, Integer> getPricesMap(LinkedList<TickData> tickDataList, int size, int offset) {
        Map<Float, Integer> prices = new HashMap<>();
        for (TickData tickData : tickDataList.subList(size - offset, size)) {
            Integer currentCount = prices.computeIfAbsent(tickData.getPrice(), aFloat -> 0);
            prices.put(tickData.getPrice(), ++currentCount);
        }
        return prices;
    }

    private ModeData getModeData(Map<Float, Integer> prices){
        ModeData data = new ModeData();
        for (Map.Entry<Float, Integer> entry : prices.entrySet()) {
            if (entry.getValue() > data.getMaxPriceCount()) {
                data.setModePrice(entry.getKey());
                data.setMaxPriceCount(entry.getValue());
            }
            if (data.getMinPriceCount() > entry.getValue()) {
                data.setAntiModePrice(entry.getKey());
                data.setMinPriceCount(entry.getValue());
            }
        }
        return data;
    }

    private boolean isUpTrend(int start, int tickNumber, LineFunction2D linefunction2d) {
        XYDataset dataset = DatasetUtilities.sampleFunction2D(linefunction2d,
                start, tickNumber, 200, "");
        float shortOpenPrice = dataset.getY(0, 0).floatValue();
        float shortClosePrice = dataset.getY(0, 1).floatValue();
        return shortOpenPrice < shortClosePrice;
    }

    private void showRegression(Visualiser visualiser, int adviseCount, double[] regressionParameters, XYDataset dataset, float openPrice, float closePrice, boolean up) {
        visualiser.show(VisualiserData.builder()
                .closePrice(closePrice)
                .up(up)
                .openPrice(openPrice)
                .regressionParameters(regressionParameters)
                .index(adviseCount)
                .dataset(dataset)
                .build());
    }

    public void addTickData(TickData tickData, boolean saveToFile) throws IOException {
        ticks.computeIfAbsent(tickData.getCurrency(), s -> new LinkedList<>())
                .add(tickData);
        int size = ticks.get(tickData.getCurrency()).size();
        if (size % 1000 == 0) {
            System.out.println("ticks size:" + size);
        }
        if (size > MODE_OFFSET + 1) {
            ticks.get(tickData.getCurrency()).removeFirst();
        }
        if (!saveToFile) {
            return;
        }

        try (PrintWriter output = new PrintWriter(new FileWriter(file, true))) {
            StringBuilder sb = new StringBuilder();
            sb.append(SIMPLE_DATE_FORMAT.format(new Date()))
                    .append(" ")
                    .append(tickData.getCurrency())
                    .append(" ")
                    .append(tickData.getTickNumber())
                    .append(" ")
                    .append(tickData.getPrice());
            output.printf("%s\r\n", sb.toString());
        }
    }

    public void drawLines(String currency, Visualiser visualiser) {
        int count = 0;
        LinkedList<TickData> ticks = new LinkedList<>();

        Map<Float, Integer> prices = new HashMap<>();
        for (TickData tickData : this.ticks.get(currency)) {
            ticks.add(tickData);
            Integer currentCount = prices.computeIfAbsent(tickData.getPrice(), aFloat -> 0);
            prices.put(tickData.getPrice(), ++currentCount);
            if (ticks.size() > MODE_OFFSET) {
                if (count % REGRESSION_LINE_COUNT * 10 == 0) {
                    Advise advise = getAdvise(ticks, visualiser);
                    if (OrderType.SELL == advise.getOrderType() && OrderType.BUY == advise.getHedgingOrderType()) {
                        visualiser.drawVerticalLine(advise.getClosedTick(), "", Color.YELLOW);
                    }
                    if (OrderType.BUY == advise.getOrderType() && OrderType.SELL == advise.getHedgingOrderType()) {
                        visualiser.drawVerticalLine(advise.getClosedTick(), "", Color.RED);
                    }
                    System.out.println(advise);

                    visualiser.drawHorizontalLine(Float.parseFloat(advise.getModePrice()), "____ " + advise.getModePrice(), Color.black);
                    visualiser.drawHorizontalLine(Float.parseFloat(advise.getAntiModePrice()), "___ " + advise.getAntiModePrice(), Color.RED);
                }
            }
            count++;
        }
    }
}

