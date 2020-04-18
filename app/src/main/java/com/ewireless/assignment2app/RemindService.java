package com.ewireless.assignment2app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * @author Jialin Fan
 */
public class RemindService extends Service {
    int count = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Use the alarm service to set the time when the alarm starts
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //Calculate the reminder time in ms. After xxx ms, the broadcast of the reminder is received
        Long secondsNextEarlyMorning = Utils.getSecondsNextEarlyMorning(AlarmActivity.HOUR, AlarmActivity.MIN);
        //Set reminder broadcast
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, count++, i, PendingIntent.FLAG_UPDATE_CURRENT);
        //Really set to remind when to broadcast
        manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + secondsNextEarlyMorning, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
