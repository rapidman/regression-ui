package com.adviser.regression.utils;

import com.adviser.regression.Constants;
import com.adviser.regression.model.TickData;
import com.adviser.regression.service.DataConsumer;
import lombok.experimental.UtilityClass;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static com.adviser.regression.utils.UiUtils.REAL_PRICE;
import static org.springframework.util.ResourceUtils.getFile;

@UtilityClass
public class PersistenceUtils {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final String TMP_FX_TICKS_FILE_PREFIX = "/home/timur/workspace/forex/forex_ticks.";
    private static Set<String> CURRENCIES = new HashSet<>();
    static {
        CURRENCIES.add("eur_usd1");
//        CURRENCIES.add("aud_nzd");
//        CURRENCIES.add("usd_cad");
//        CURRENCIES.add("eur_jpy");
//        CURRENCIES.add("chf_jpy");
    }

    public static void saveLine(TickData tickData) throws IOException {
        if(!"eur_usd1".equals(tickData.getCurrency())) return;
        try (PrintWriter output = new PrintWriter(new FileWriter(TMP_FX_TICKS_FILE_PREFIX + tickData.getCurrency(), true))) {
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

    public static void loadData(DataConsumer dataConsumer) {
        for (String currency : CURRENCIES) {
            File file = new File(TMP_FX_TICKS_FILE_PREFIX + currency);
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
                    dataConsumer.addTickData(TickData.builder()
                            .tickNumber(scanner.nextInt())
                            .currency(currency)
                            .price(scanner.nextFloat())
                            .build(), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void addSeries(Scanner scanner, XYSeries series, DataConsumer dataConsumer, String currency) throws IOException {
        scanner.next();
        scanner.next();
        int xTickNumber = scanner.nextInt();
        float yPrice = scanner.nextFloat();
        series.add(xTickNumber, yPrice);
        dataConsumer.addTickData(TickData.builder()
                .currency(currency)
                .price(yPrice)
                .tickNumber(xTickNumber)
                .build(), false);
    }

    public static XYDataset createDatasetFromFile(String inputFileName, DataConsumer dataConsumer, String currency) throws IOException {
        File file = getFile(inputFileName);
        Scanner scanner = new Scanner(file);

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(REAL_PRICE);

        int lineCount = 0;
        while (scanner.hasNextLine() && scanner.hasNext()) {
            addSeries(scanner, series, dataConsumer, currency);
            lineCount++;
            if (lineCount % 10000 == 0) {
                System.out.println(lineCount);
            }
        }
        scanner.close();
        dataset.addSeries(series);

        return dataset;
    }
}
