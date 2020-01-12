package com.adviser.regression.service;

import com.adviser.regression.model.Advise;
import com.adviser.regression.model.TickData;
import com.adviser.regression.ui.Visualiser;

import java.io.IOException;

public interface DataConsumer {
    void addTickData(TickData tickData, boolean saveToFile) throws IOException;

    Advise getAdvise(String currency, Visualiser visualiser);
}
