package com.ewireless.charts;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.annotations.NotNull;

public class XAxisFormatter extends ValueFormatter {

    String[] xTicks;
    public XAxisFormatter(String[] xTicks) {
        this.xTicks = xTicks;
    }

    @NotNull
    public String getAxisLabel(float value, @Nullable AxisBase axis) {
        //return xTicks[(int) value];

        if (value >= 0) {
            if (value <= xTicks.length - 1) {
               return xTicks[(int) value];
            }
            return "";
        }
        return "";
   }
}
