package com.adviser.regression.ui;

import com.adviser.regression.model.VisualiserData;

import java.awt.*;

public interface Visualiser {
    void show(VisualiserData data);

    void drawHorizontalLine(float yCoordinate, String label, Color color);

    void drawVerticalLine(int xCoordinate, String label, Color color);
}
