package com.ewireless.assignment2app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import com.blankj.utilcode.util.SPUtils;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Jialin Fan?
 */
public class AlarmActivity extends AppCompatActivity {
    public static int HOUR = 0;
    public static int MIN = 0;
    private TextView mAlarmTimeTv;
    Intent alarmIntent;
    DecimalFormat format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        //Request notification permission, the phone is generally closed by default
        Utils.requestOpenNotify(this);
        //Initialize the reminder service intent
        alarmIntent = new Intent(AlarmActivity.this, RemindService.class);
        mAlarmTimeTv = findViewById(R.id.alarm_time_tv);
        format = new DecimalFormat("00");
        //Read the set time from the sp file and display it.
        mAlarmTimeTv.setText(SPUtils.getInstance().getString("time", "10:00"));
        mAlarmTimeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Click the time, a time selection box pops up
                showTimePickerDialog();
            }
        });
        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save time in sp
                SPUtils.getInstance().put("time", format.format(HOUR) + ":" + format.format(MIN));
                //Before starting, close the previous service. If there is no reminder service before, no error will be reported.
                stopService(alarmIntent);
                //Start service
                startService(alarmIntent);
            }
        });

    }

    /***
     * Show time selection box
     */
    private void showTimePickerDialog() {
        final Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH), startDate.get(Calendar.HOUR_OF_DAY), startDate.get(Calendar.MINUTE));
        startDate.set(Calendar.SECOND, 0);
        endDate.set(2022, 11, 31);
        TimePickerView pvTime = new TimePickerBuilder(this, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {
                //Get the selected time
                int hour = date.getHours();
                HOUR = hour;
                int minute = date.getMinutes();
                MIN = minute;
                //Display the selected time on the view
                mAlarmTimeTv.setText("" + format.format(HOUR) + ":" + format.format(MIN));
            }
        }).setType(new boolean[]{false, false, false, true, true, false})//Only need to display hours and minutes, other set to false
                .setRangDate(startDate, endDate)//Start and end date setting
                .build();
        pvTime.show();//display
    }


}
