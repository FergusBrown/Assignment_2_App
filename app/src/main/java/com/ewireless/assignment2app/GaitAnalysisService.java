
package com.ewireless.assignment2app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.spin.gaitlib.GaitAnalysis;
import org.spin.gaitlib.core.GaitData;
import org.spin.gaitlib.core.IGaitUpdateListener;
import org.spin.gaitlib.filter.FilterNotSetException;
import org.spin.gaitlib.gait.IClassifierModelLoadingListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Background service which logs stride gait and cadence using GaitLib
 * Once logged the cadence is uploaded to the database under the specific user's key with an
 * appropriate timestamp.
 * @author Fergus Brown
 */
public class GaitAnalysisService extends Service {

    // Create Firebase database object
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    // Create database reference pointing to users
    private DatabaseReference mDatabaseReference = mDatabase.getReference().child("Users");

    // Tag for log prints
    private final static String TAG = "GaitAnalysisService";

    // broadcast receiver for gait analysis
    private final GaitAnalysisServiceReceiver receiver = new GaitAnalysisServiceReceiver();

    // GaitLib parameters
    public static final String GAIT_UPDATE = "spin.gaitlib.GaitAnalysisService.GAIT_UPDATE";
    public static final String CADENCE = "spin.gaitlib.GaitAnalysisService.CADENCE";
    public static final String GAIT = "spin.gaitlib.GaitAnalysisService.GAIT";
    public static final String GAIT_ALL = "spin.gaitlib.GaitAnalysisService.GAIT_ALL";
    public static final String GAITLIB_STATUS_UPDATE = "spin.gaitlib.GaitAnalysisService.GAITLIB_STATUS_UPDATE";
    public static final String GAITLIB_STATUS_MESSAGE = "spin.gaitlib.GaitAnalysisService.GAITLIB_STATUS_MESSAGE";

    private WakeLock wakeLock;

    // create gait analysis object
    private GaitAnalysis mGaitAnalysis = null;
    // create logger object
    private Logger logger = null;

    // Method run on service creation
    @Override
    public void onCreate() {

        // use wake_lock for power management
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        int WAKE_LOCK = PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE;
        wakeLock = mgr.newWakeLock(WAKE_LOCK, "app:MyWakeLock");
        wakeLock.acquire();
        mGaitAnalysis = new GaitAnalysis();
        logger = new Logger();
        registerSensorListeners();

        // Register IGaitUpdateListener to log gait and cadence data
        mGaitAnalysis.registerGaitUpdateListener(new IGaitUpdateListener() {

            public void onGaitUpdated(GaitData data) {
                try {
                    float cadence = mGaitAnalysis.getCadence(false);
                    String gait = mGaitAnalysis.getCurrentGait();
                    logger.addCadence(cadence);
                    logger.addGait(gait);
                    logger.addTimeStamp(data.getTimeStamp());

                    Intent intent = new Intent(GAIT_UPDATE);
                    intent.putExtra(CADENCE, cadence);
                    intent.putExtra(GAIT, gait);
                    intent.putStringArrayListExtra(GAIT_ALL, logger.getGaits());

                    sendBroadcast(intent);
                } catch (FilterNotSetException e) {
                }

            }
        });

        // loads model for gait classification (not working in this version)
        mGaitAnalysis
                .addGaitClassifierModelLoadingListener(new IClassifierModelLoadingListener() {

                    public void onModelLoaded(boolean success) {
                        String message = success ? "Model loaded successfully."
                                : "GaitLib failed to load the model.";
                        Intent intent = new Intent(GAITLIB_STATUS_UPDATE);
                        intent.putExtra(GAITLIB_STATUS_MESSAGE, message);
                        sendBroadcast(intent);
                    }

                    public void onLoadingStart() {
                        String message = "GaitLib is loading the model.";
                        Intent intent = new Intent(GAITLIB_STATUS_UPDATE);
                        intent.putExtra(GAITLIB_STATUS_MESSAGE, message);
                        sendBroadcast(intent);
                    }
                });

        // windowSize is the windows used for analysis, sampling interval is the number of times the system analyses and outputs the current cadence.
        mGaitAnalysis.startGaitAnalysis(2000, 1000);

        IntentFilter gaitUpdateFilter = new IntentFilter(
                GaitAnalysisService.GAIT_UPDATE);
        registerReceiver(receiver, gaitUpdateFilter);
        IntentFilter gaitLibStatusUpdateFilter = new IntentFilter(
                GaitAnalysisService.GAITLIB_STATUS_UPDATE);
        registerReceiver(receiver, gaitLibStatusUpdateFilter);

        super.onCreate();
    }

    // when service destroyed stop gait analysis
    @Override
    public void onDestroy() {
        wakeLock.release();
        mGaitAnalysis.stopGaitAnalysis();
        unregisterSensorListeners();
        unregisterReceiver(receiver);
        clearCache();
        super.onDestroy();
    }

    // register accelerometer listener for analysis
    private void registerSensorListeners() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mGaitAnalysis.getSignalListener(),
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    // unregister accelerometer listener
    private void unregisterSensorListeners() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mGaitAnalysis.getSignalListener());
    }

    // Upon intent broadcast from IGaitUpdateListener
    public class GaitAnalysisServiceReceiver extends BroadcastReceiver {

        // trigger on intent broadcast
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            // process the new cadence data
            if (GaitAnalysisService.GAIT_UPDATE.equals(action)) {
                float cadence = intent.getFloatExtra(GaitAnalysisService.CADENCE, 0);
                processCadence(cadence);

            } else if (GaitAnalysisService.GAITLIB_STATUS_UPDATE.equals(action)) {
                String message = intent
                        .getStringExtra(GaitAnalysisService.GAITLIB_STATUS_MESSAGE);
                Log.d(TAG, message);
            }

        }

    }

    // variable used to count for walk start and end
    private int secondCount = 0;

    // boolean which indicates whether user is walking or not
    private boolean isWalking = false;
    // cadence threshold frequency for detecting a walk
    final private double cadenceThreshold = 0.4;
    // second threshold to indicate start or end of walk
    final private int secondThreshold = 5;

    // handle the cadence data based on whether the user is walking
    private void processCadence(float cadence) {
        // Handle walking accordingly
        if(isWalking) {
            handleWalking(cadence);
        } else {
            handleNotWalking(cadence);
        }
    }

    // Strings to create database children names
    private String year;
    private  String month;
    private  String day;
    private String timeStamp;

    // list for storing data, key for seconds, float for cadence value
    private List<Float> cadenceData = new ArrayList<Float>();

    // if the user is walking the add to cadence Data array and detect whether walk has ended
    private void handleWalking(float cadence) {

        // open preferences to get unique use key
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userKey = prefs.getString("User ID", null);

        // create database child under the current date
        DatabaseReference timeRef = mDatabaseReference.child(userKey).child("Cadence Data")
                .child("YEAR: " + year)
                .child("MONTH: " + month)
                .child("DAY: " + day)
                .child("TIME: " + timeStamp);

        // Add data to list
        cadenceData.add(cadence);

        // return and reset if greater than threshold
        // if less than threshold then increment counter
        if(cadence >= cadenceThreshold) {
            secondCount = 0;
            return;
        } else if (secondCount < secondThreshold) {
            secondCount++;
            return;
        }


        // if at second threshold end the walk and upload cadenceData to database
        if (secondCount == secondThreshold) {
            secondCount = 0;
            isWalking = false;
            // erase last X instances
            for (int i = 0; i <= secondThreshold; i++) {
                int lastIndex = cadenceData.size()-1;
                cadenceData.remove(lastIndex);
            }

            // set timestamp value to the list of cadence values
            timeRef.setValue(cadenceData);
            // clear list
            cadenceData.clear();
        }
    }

    // If cadence above a threshold for more than 3 secs then start recording as a walk
    private void handleNotWalking(float cadence) {
        // return if not at 3 and reset if less than threshold
        if(cadence < cadenceThreshold) {
            secondCount = 0;
        } else {
            // return without doing anything if 3 seconds have not passed
            while(secondCount < secondThreshold) {
                secondCount++;
            }

            if (secondCount == secondThreshold) {
                secondCount = 0;
                year = new SimpleDateFormat("yyyy", Locale.UK).format(new Date());
                month = new SimpleDateFormat("MM", Locale.UK).format(new Date());
                day = new SimpleDateFormat("dd", Locale.UK).format(new Date());
                timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.UK).format(new Date());
                isWalking = true;
            }
        }

        return;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void clearCache() {
        logger.clearAll();
    }

}
