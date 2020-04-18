package com.ewireless.assignment2app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.NotificationUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.ewireless.assignment2app.R;

/**
 * @author Jialin Fan
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("chson", "--------Receive reminder");
        //Determine whether the notification switch is turned on, if it is turned on, a notification pops up
        if (NotificationUtils.areNotificationsEnabled()) {
            NotificationUtils.notify(0, new  Utils.Func1<Void, NotificationCompat.Builder>() {
                @Override
                public Void call(NotificationCompat.Builder param) {
                    param.setContentText("Taking medicine time");//Set the text content of notification
                    param.setSmallIcon(R.mipmap.ic_launcher);//Set the notification icon
                    param.setContentTitle("Medicine Alarm");//Set the title of the notification
                    param.setAutoCancel(true);
                    return null;
                }
            });
        } else {//If the switch is not turned on, a prompt will pop up
            ToastUtils.showShort("open notification");
        }

    }

}
