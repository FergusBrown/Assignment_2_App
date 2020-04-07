package com.ewireless.assignment2app;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class FallDetectionService extends Service implements SensorEventListener {
    // Tag for log prints
    private final static String TAG = "FallDetectionService";

    private SensorManager sm;
    private Sensor aSensor;
    private Sensor gSensor;

    float[] accelerometerValues = new float[3];
    float[] gyroscopeValues = new float[3];
    double aThreshold;
    double gThreshold;
    double aMax;
    double gMax;
    long time;
    double ayMin;
    double ayMax;
    int f;

    private boolean isCountRunning = false;

    
    // count down timer for 30 seconds
    CountDownTimer timer = new CountDownTimer(30000, 1000) {
        public void onTick(long millisUntilFinished) {
            long currentTime = millisUntilFinished / 1000;
            Log.d(TAG, "Countdown time = " + currentTime);
            //tvTimer.setText("seconds remaining: " + millisUntilFinished / 1000);
        }

        public void onFinish() {
            isCountRunning = false;
            sendMessage();
            time = 0;
            timer.cancel();
            // TODO: timer sometimes immediately restarts after this -might be fixed
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        initialization();
        calculateOrientation();

        sm.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void initialization() {
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    private void calculateOrientation() {
        aThreshold = Math.sqrt(accelerometerValues[0]*accelerometerValues[0] + accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]);
        gThreshold = Math.sqrt(gyroscopeValues[0]*gyroscopeValues[0] + gyroscopeValues[1]*gyroscopeValues[1] + gyroscopeValues[2]*gyroscopeValues[2]);

        if (aThreshold > aMax) aMax = aThreshold;
        if (gThreshold > gMax) gMax = gThreshold;


        if (aThreshold > 20 && time == 0 && !isCountRunning) {
            //Toast.makeText(this, "Fall check 1", Toast.LENGTH_SHORT).show();
            //aMax = 0;
            //gMax = 0;
            if(gThreshold < 10) {
                //Toast.makeText(this, "Fall check 1", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Fall check 1");
                aMax = 0;
                gMax = 0;
                time = System.currentTimeMillis();
            }
        }

        if(time > 0) {

            if(accelerometerValues[1] < ayMin) ayMin = accelerometerValues[1];
            if(accelerometerValues[1] > ayMax) ayMax = accelerometerValues[1];
            if(ayMin > -5 && ayMax < 5) {
                Log.d(TAG, "Fall check 2");
                f = 1;
            }
            if(System.currentTimeMillis() - time > 5000) {
                Log.d(TAG, "time reset");
                time = 0;
                ayMin = 0;
                ayMax = 0;
            }
        }

        if(f == 1 && !isCountRunning){
            f = 0;
            timer.start();
            isCountRunning = true;
        }
    }




    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        sm.unregisterListener(this);
        super.onDestroy();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            accelerometerValues = event.values;
        }
        if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
            gyroscopeValues = event.values;
        }
        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void sendMessage(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String messageToSend = "Fall has been detected";
        String number = prefs.getString("Carer Phone", "0000");

        SmsManager sms = SmsManager.getDefault();
        PendingIntent sentPI;
        String SENT = "SMS_SENT";

        sentPI = PendingIntent.getBroadcast(this, 0,new Intent(SENT), 0);

        sms.sendTextMessage(number, null, messageToSend, sentPI, null);
        //Toast.makeText(this, "Fall registered, message sent", Toast.LENGTH_SHORT).show();
    }
}
