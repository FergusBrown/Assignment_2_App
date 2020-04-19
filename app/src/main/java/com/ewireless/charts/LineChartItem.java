
package com.ewireless.charts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ewireless.assignment2app.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.LineData;

/**
 * Extension of the base Chart class to handle line charts
 * @author philipp (MPCHART)
 * Adapted by Fergus Brown s1525959
 */
public class LineChartItem extends ChartItem {

    private final Typeface mTf;

    public LineChartItem(ChartData<?> cd, Context c) {
        super(cd);

        mTf = Typeface.defaultFromStyle(Typeface.NORMAL);
    }

    // constructor for X axis ticks
    public LineChartItem(ChartData<?> cd, int maxTicks, String title, Context c) {
        super(cd, maxTicks, title);

        mTf = Typeface.defaultFromStyle(Typeface.NORMAL);
    }

    @Override
    public int getItemType() {
        return TYPE_LINECHART;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, Context c) {

        ViewHolder holder;

        if (convertView == null) {

            holder = new ViewHolder();

            convertView = LayoutInflater.from(c).inflate(
                    R.layout.list_item_linechart, null);
            holder.chart = convertView.findViewById(R.id.chart);

            // initialise text view
            holder.chart_title = convertView.findViewById(R.id.chart_title);
            holder.chart_title.setText(title);
            holder.chart_title.setTextColor(Color.BLACK);
            holder.chart_title.setGravity(Gravity.CENTER);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // apply styling
        holder.chart.getDescription().setEnabled(false);
        holder.chart.setDrawGridBackground(false);

        XAxis xAxis = holder.chart.getXAxis();
        xAxis.setPosition(XAxisPosition.BOTTOM);
        xAxis.setTypeface(mTf);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisMinimum(1f);
        xAxis.setAxisMaximum(maxTicks);

        YAxis leftAxis = holder.chart.getAxisLeft();
        leftAxis.setTypeface(mTf);
        leftAxis.setLabelCount(5, false);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = holder.chart.getAxisRight();
        rightAxis.setTypeface(mTf);
        rightAxis.setLabelCount(5, false);
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f);

        // set data
        holder.chart.setData((LineData) mChartData);

        // do not forget to refresh the chart
        holder.chart.animateX(750);

        return convertView;
    }

    private static class ViewHolder {
        LineChart chart;
        TextView chart_title;
    }
}
