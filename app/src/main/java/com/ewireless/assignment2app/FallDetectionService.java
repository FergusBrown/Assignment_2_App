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

/**
 * Background service used to detect and register falls.
 * Falls are detected using device accelerometer, gravity and magnetometer values
 * based on device movement and orientation.
 * Once a fall is registered an SMS text is sent to the stored phone number.
 * @author Roland Podlucki
 */
public class FallDetectionService extends Service implements SensorEventListener {
    // Tag for log prints
    private final static String TAG = "FallDetectionService";

    private SensorManager sm;
    private Sensor aSensor;

    float[] accelerometerValues = new float[3];
    double aThreshold;
    double aMax;
    double aMax0;
    double aMax2;
    double tMin1 = 90;
    double tMin2 = 90;
    double theta1;
    double theta2;
    long time;
    int f;

    private boolean isCountRunning = false;

    // count down timer for 30 seconds
    CountDownTimer timer = new CountDownTimer(30000, 1000) {
        public void onTick(long millisUntilFinished) {
            long currentTime = millisUntilFinished / 1000;
            Log.d(TAG, "Countdown time = " + currentTime);
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
    }

    @Override
    public IBinder onBind(Intent intent) {
        // ignore, this is not required
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void initialization() {
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void calculateOrientation() {
        aThreshold = Math.sqrt(accelerometerValues[0]*accelerometerValues[0] + accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]);
        theta1 = Math.toDegrees(Math.atan((Math.sqrt(accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]))/(accelerometerValues[0])));
        theta2 = Math.toDegrees(Math.atan((Math.sqrt(accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[0]*accelerometerValues[0]))/(accelerometerValues[2])));

        if(f < 1000 && !isCountRunning){
            f++;
            if(aThreshold > aMax) aMax = aThreshold;
            if(accelerometerValues[0] > aMax0) aMax0 = accelerometerValues[0];
            if(accelerometerValues[2] > aMax2) aMax2 = accelerometerValues[2];
            if(Math.abs(theta1) < tMin1) tMin1 = Math.abs(theta1);
            if(Math.abs(theta2) < tMin2) tMin2 = Math.abs(theta2);
            if(aMax > 25 && aMax0 > 18 && aMax2 > 18 && (tMin1 < 45 || tMin2 < 45)){
                timer.start();
                isCountRunning = true;
            }
        }
        else{
            f = 0;
            aMax = 0;
            aMax0 = 0;
            aMax2 = 0;
            tMin1 = 90;
            tMin2 = 90;
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
        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void sendMessage(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String patientName = prefs.getString("Patient Name", "");
        String messageToSend = "Fall has been detected for your patient, " + patientName;
        String number = prefs.getString("Carer Phone", "0000");

        SmsManager sms = SmsManager.getDefault();
        PendingIntent sentPI;
        String SENT = "SMS_SENT";

        sentPI = PendingIntent.getBroadcast(this, 0,new Intent(SENT), 0);

        sms.sendTextMessage(number, null, messageToSend, sentPI, null);
        //Toast.makeText(this, "Fall registered, message sent", Toast.LENGTH_SHORT).show();
    }
}
