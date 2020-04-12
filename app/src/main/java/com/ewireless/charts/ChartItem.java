package com.ewireless.charts;

import android.content.Context;
import android.view.View;

import com.github.mikephil.charting.data.ChartData;

/**
 * Base class of the Chart ListView items
 * @author philipp
 *
 */
@SuppressWarnings("unused")
public abstract class ChartItem {

    static final int TYPE_BARCHART = 0;
    static final int TYPE_LINECHART = 1;
    static final int TYPE_PIECHART = 2;

    ChartData<?> mChartData;
    String[] xTicks;
    String title;
    int maxTicks;

    // constructor for chart data
    ChartItem(ChartData<?> cd) {
        this.mChartData = cd;
    }

    // constructor for X axis ticks
    ChartItem(ChartData<?> cd, String[] xTicks, String title) {
        this.mChartData = cd;
        this.xTicks = xTicks;
        this.title = title;
    }

    // constructor for max xTicks
    ChartItem(ChartData<?> cd, int maxTicks, String title) {
        this.mChartData = cd;
        this.maxTicks = maxTicks;
        this.title = title;
    }



    public abstract int getItemType();

    public abstract View getView(int position, View convertView, Context c);
}
