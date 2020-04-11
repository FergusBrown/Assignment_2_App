package com.ewireless.assignment2app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.ewireless.assignment2app.BuildConfig;
import com.ewireless.assignment2app.MainActivity;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityRecognitionService extends Service {

    // Database reference initialisation
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    private DatabaseReference mDatabaseReference = mDatabase.getReference().child("Users");

    /***********Begin definitions for Activity API************/

    // Tag for log prints
    private final static String TAG = "ActivityService";

    // Bool true if activity tracking is enabled
    private boolean activityTrackingEnabled;

    // List of activity transitions e.g. walking, still
    private List<ActivityTransition> activityTransitionList;

    // Action fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    private PendingIntent mActivityTransitionsPendingIntent;
    private TransitionsReceiver mTransitionsReceiver;

    /***********End definitions for Activity API************/

    @Override
    public void onCreate() {


        // Activity tracker is initially turned off
        activityTrackingEnabled = false;

        // List of activity transitions to track.
        activityTransitionList = new ArrayList<>();

        // Add the transitions we want to track to the array
        // In this case we are tracking when we enter and exit walking and still states#
        // The toActivity and toTransition methods handle string creation
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        // Initialize PendingIntent that will be triggered when a activity transition occurs.
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mActivityTransitionsPendingIntent =
                PendingIntent.getBroadcast(ActivityRecognitionService.this, 0, intent, 0);

        //  Create and register a BroadcastReceiver to listen for activity transitions.
        // The receiver listens for the PendingIntent above that is triggered by the system when an
        // activity transition occurs.
        mTransitionsReceiver = new TransitionsReceiver();
        //LocalBroadcastManager.getInstance(this).registerReceiver(
        registerReceiver(
                mTransitionsReceiver,
                new IntentFilter(TRANSITIONS_RECEIVER_ACTION)
        );

        Log.d(TAG, "App initialized.");

        enableActivityTransitions();


        super.onCreate();
    }

    // What happens when an application activity starts the service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Activities service starting", Toast.LENGTH_SHORT).show();

        // Create a notification channel for the startForeground method
        // TODO: if a foreground activity is not called then service is a candidate for freeing uo memory
        createNotificationChannel();

        // Start a foreground notif so service is always running
        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    // Required values for startForeground method
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "com.ewireless.assignment2.ANDROID";
    public static final String ANDROID_CHANNEL_NAME = "ANDROID CHANNEL";
    public static final String ANDROID_CHANNEL_DESCRIP = "Activity Service";

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, ANDROID_CHANNEL_NAME, importance);
            channel.setDescription(ANDROID_CHANNEL_DESCRIP);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Provide info about app when not in use
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running in the background")
                .setContentIntent(pendingIntent)
                .build());
    }

    /***********Begin Activity API methods************/

    // handles list creation
    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.IN_VEHICLE:
                return "IN VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON BICYCLE";
            default:
                return "UNKNOWN";
        }
    }

    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }


    // Unregister receivers which should only stop when the app is destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy() Called");

         //Unregister activity transition receiver when user destroys the app.
        disableActivityTransitions();
        if (mTransitionsReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mTransitionsReceiver);
            mTransitionsReceiver = null;
        }
    }



    /**
     * Registers callbacks for {@link ActivityTransition} events via a custom
     * {@link BroadcastReceiver}
     */
    private void enableActivityTransitions() {

        Log.d(TAG, "enableActivityTransitions()");


        // TODO: Create request and listen for activity changes.
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent);


        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        activityTrackingEnabled = true;
                        Log.d(TAG, "Enable Complete");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Enable Failed");
                    }
                });


    }

    /**
     * Unregisters callbacks for {@link ActivityTransition} events via a custom
     * {@link BroadcastReceiver}
     */
    private void disableActivityTransitions() {

        Log.d(TAG, "disableActivityTransitions()");


        // TODO: Stop listening for activity changes.
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mActivityTransitionsPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        activityTrackingEnabled = false;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
        Log.d(TAG, "Disable Complete");
    }

    public void onClickEnableOrDisableActivityRecognition(View view) {

        // TODO: Add requests for activity tracking.
        if (activityTrackingEnabled) {
            disableActivityTransitions();

        } else {
            enableActivityTransitions();
        }

    }

    /**
     * Handles intents from from the Transitions API.
     */
    public class TransitionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String activityType, transitionType;


            Log.d(TAG, "onReceive() Called");

            if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {

                return;
            }

            // Extract activity transition information from listener.
            if (ActivityTransitionResult.hasResult(intent)) {

                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);

                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    activityType = toActivityString(event.getActivityType());
                    transitionType = toTransitionType(event.getTransitionType());
                    //continue only if the activity happened in the last 30 seconds
                    //for some reason callbacks are received for old activities when the receiver is registered
                    if(((SystemClock.elapsedRealtime()-(event.getElapsedRealTimeNanos()/1000000))/1000) <= 30) {
                        //activity transition is legit. Do stuff here.
                        transitionHandler(activityType, transitionType);
                    }

                }
            }
        }
    }

    // Decides what to do when a transition occurs
    // If entering walk then start gaitAnalysis service and write cadence data to file
    // If exiting walk end gaitAnalysis service and upload data to database
    private void transitionHandler(String activityType, String transitionType) {
        String info = "Transition: " + activityType +
                " (" + transitionType + ")" + "   " +
                new SimpleDateFormat("yyyy:MM:DD:HH:mm:ss", Locale.UK).format(new Date());
        // Tag info for debug
        Log.d(TAG, info);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userKey = prefs.getString("User ID", null);

        DatabaseReference newRef = mDatabaseReference.child(userKey).child("Activity Data")
                .child("YEAR: " + new SimpleDateFormat("yyyy", Locale.UK).format(new Date()))
                .child("MONTH: " + new SimpleDateFormat("MM", Locale.UK).format(new Date()))
                .child("DAY: " + new SimpleDateFormat("dd", Locale.UK).format(new Date()))
                .child("TIME: " + new SimpleDateFormat("HH:mm:ss", Locale.UK).format(new Date()));

        String newData = "Transition: " + activityType + " (" + transitionType + ")";

        newRef.setValue(newData);
    }




    // Required by Service interface
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
