package com.adviser.regression.utils;

import com.adviser.regression.model.ModeData;
import com.adviser.regression.model.TickData;
import lombok.experimental.UtilityClass;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

    public static boolean isUpTrend(int start, int tickNumber, LineFunction2D linefunction2d) {
        XYDataset dataset = DatasetUtilities.sampleFunction2D(linefunction2d,
                start, tickNumber, 200, "");
        float shortOpenPrice = dataset.getY(0, 0).floatValue();
        float shortClosePrice = dataset.getY(0, 1).floatValue();
        return shortOpenPrice < shortClosePrice;
    }

}
