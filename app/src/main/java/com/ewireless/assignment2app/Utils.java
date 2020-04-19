package com.ewireless.assignment2app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

import static com.blankj.utilcode.util.ActivityUtils.startActivity;

/**
 * @author Jialin Fan s1952282
 */
public class Utils {
    public static Long getSecondsNextEarlyMorning(int hour, int min) {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) - hour > 0) {
            //If the current time is greater than or equal to the selected number of hours, it means that this reminder is tomorrow.
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        //Calculate the time difference between the current time and the reminder time we set,
        // the unit is ms, that is, from now on, it will trigger seconds reminder after a time of seconds milliseconds
        Long seconds = (cal.getTimeInMillis() - System.currentTimeMillis());
        return seconds.longValue();
    }

    /***
     * Jump to the setting interface and turn on the notification switch
     * @param context
     */
    private static void gotToSystemNotifyManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            //This scheme applies to API21-25, that is, versions between 5.0-7.1 can be used
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
            //This solution is applicable to API 26, which can be used above 8.0 (including 8.0)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
            startActivity(intent);
        } else {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
            startActivity(localIntent);
        }

    }

    /***
     * Determine whether the notification switch is turned on
     */
    private static boolean isNotifyOpen() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(com.blankj.utilcode.util.Utils.getApp());
        boolean isOpened = manager.areNotificationsEnabled();
        return isOpened;
    }

    /**
     * Request to open the notification permission, will jump to the settings interface
     * @param context
     */
    public static void requestOpenNotify(final Context context) {
        if (!isNotifyOpen()) {
            if (!isNotifyOpen()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("open notify");
                builder.setMessage("please open notify");
                builder.setNegativeButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gotToSystemNotifyManager(context);
                    }
                });
                builder.show();
            }
        }
    }
}
