package com.adviser.regression.service;

import com.adviser.regression.model.TickData;

import java.io.IOException;

public interface DataConsumer {
    void addTickData(TickData tickData, boolean saveToFile) throws IOException;
}
