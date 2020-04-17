package com.ewireless.charts;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.annotations.NotNull;

/**
 * Extension of ValueFormatter used to format the X labels on the graph
 * @author Fergus Brown
 */

public class XAxisFormatter extends ValueFormatter {

    String[] xTicks;
    public XAxisFormatter(String[] xTicks) {
        this.xTicks = xTicks;
    }

    @NotNull
    public String getAxisLabel(float value, @Nullable AxisBase axis) {

        // only return xticks value if they are an int
        if (value >= 0) {
            if (value <= xTicks.length - 1) {
               return xTicks[(int) value];
            }
            return "";
        }
        return "";
   }
}
