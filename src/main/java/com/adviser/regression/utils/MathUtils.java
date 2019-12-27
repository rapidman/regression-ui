package com.adviser.regression.utils;

import com.adviser.regression.model.ModeData;
import com.adviser.regression.model.TickData;
import com.adviser.regression.model.TrendData;
import lombok.experimental.UtilityClass;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@UtilityClass
public class MathUtils {
    public static Map<Float, Integer> getPricesMap(LinkedList<TickData> tickDataList, int size, int offset) {
        Map<Float, Integer> prices = new HashMap<>();
        for (TickData tickData : tickDataList.subList(size - offset, size)) {
            Integer currentCount = prices.computeIfAbsent(tickData.getPrice(), aFloat -> 0);
            prices.put(tickData.getPrice(), ++currentCount);
        }
        return prices;
    }


    public static ModeData getModeData(Map<Float, Integer> prices){
        ModeData data = new ModeData();
        Map<Integer, Float> pricesByCount = new TreeMap<>(Collections.reverseOrder());

        for (Map.Entry<Float, Integer> entry : prices.entrySet()) {
            pricesByCount.put(entry.getValue(), entry.getKey());
            if (entry.getValue() > data.getMaxPriceCount()) {
                data.setModePrice(entry.getKey());
                data.setMaxPriceCount(entry.getValue());
            }
            if (data.getMinPriceCount() > entry.getValue()) {
                data.setAntiModePrice(entry.getKey());
                data.setMinPriceCount(entry.getValue());
            }
        }



        float amplitude = Math.abs(data.getAntiModePrice() - data.getModePrice());
        float antiModePriceCandidate = data.getAntiModePrice();
        int antiModePriceCandidateCount = 0;
        for (Map.Entry<Integer, Float> entry : pricesByCount.entrySet()) {
            float tmpAmplitude = Math.abs(entry.getValue() - data.getModePrice());
            if (tmpAmplitude > amplitude) {
                amplitude = tmpAmplitude;
                antiModePriceCandidate = entry.getValue();
                antiModePriceCandidateCount = entry.getKey();
            }
        }
        data.setAntiModePrice(antiModePriceCandidate);
       // data.setConfirmAntiModePrice(antiModePriceCandidate);
//        if (antiModePriceCandidateCount > 12) {
//            data.setConfirmAntiModePrice(antiModePriceCandidate);
//        }

        return data;
    }

    public static boolean isUpTrend(int start, int tickNumber, Function2D linefunction2d) {
        XYDataset dataset = DatasetUtilities.sampleFunction2D(linefunction2d,
                start, tickNumber, 200, "");
        float shortOpenPrice = dataset.getY(0, 0).floatValue();
        float shortClosePrice = dataset.getY(0, 1).floatValue();
        return shortOpenPrice < shortClosePrice;
    }

    public static double getStandardDeviation(List<TickData> list){
        SummaryStatistics stats = new SummaryStatistics();
        for (TickData data : list) {
            stats.addValue(data.getPrice());
        }
        return stats.getStandardDeviation();
    }

}
