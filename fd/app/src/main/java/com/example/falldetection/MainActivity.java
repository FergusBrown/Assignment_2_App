package com.example.falldetection;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    // orientation  threshold
    // lying down time threshold
    // acceleration lower and upper threshold

    TextView tvAcceleration;
    TextView tvRotation;

    private SensorManager sm;
    private Sensor aSensor;
    private Sensor gSensor;
    private Sensor mSensor;

    float[] accelerometerValues = new float[3];
    float[] gyroscopeValues = new float[3];
    float[] magnetometerValues = new float[3];
    double aThreshold;
    double gThreshold;
    double mThreshold;
    double aMax;
    double gMax;
    double aMax0;
    double aMax2;
    double tMin1 = 90;
    double tMin2 = 90;
    double theta1;
    double theta2;
    double degree;
    long time;
    double ayMin;
    double ayMax;
    int f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initialization();
        calculateOrientation();
    }

    private void initialization() {
        tvAcceleration = (TextView)findViewById(R.id.tvAcceleration);
        tvRotation = (TextView)findViewById(R.id.tvRotation);

        f = 0;

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void calculateOrientation() {
        aThreshold = Math.sqrt(accelerometerValues[0]*accelerometerValues[0] + accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]);
        gThreshold = Math.sqrt(gyroscopeValues[0]*gyroscopeValues[0] + gyroscopeValues[1]*gyroscopeValues[1] + gyroscopeValues[2]*gyroscopeValues[2]);
        theta1 = Math.toDegrees(Math.atan((Math.sqrt(accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]))/(accelerometerValues[0])));
        theta2 = Math.toDegrees(Math.atan((Math.sqrt(accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[0]*accelerometerValues[0]))/(accelerometerValues[2])));

        float[] values = new float[3];
        float[] R = new float[9];

        SensorManager.getRotationMatrix(R, null, accelerometerValues, magnetometerValues);
        SensorManager.getOrientation(R, values);

        degree = (float) Math.toDegrees(values[2]);

        //if (theta > aMax) aMax = theta;
        //if (aThreshold < gMax) gMax = gThreshold;

        tvAcceleration.setText("Acceleration: " + aThreshold);
        //tvRotation.setText("Rotation: " + gThreshold);

        if(f < 1000){
            f++;
            if(accelerometerValues[0] > aMax0) aMax0 = accelerometerValues[0];
            if(accelerometerValues[2] > aMax2) aMax2 = accelerometerValues[2];
            if(Math.abs(theta1) < tMin1) tMin1 = Math.abs(theta1);
            if(Math.abs(theta2) < tMin2) tMin2 = Math.abs(theta2);
            if(aMax0 > 18 && aMax2 > 18 && (tMin1 < 45 || tMin2 < 45)){
                Toast.makeText(this, "Fall registered", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, CDTimer.class));
            }
        }
        else{
            f = 0;
            aMax0 = 0;
            aMax2 = 0;
            tMin1 = 90;
            tMin2 = 90;
        }

        /*if (aThreshold > 20 && time == 0) {
            //Toast.makeText(this, "Fall check 1", Toast.LENGTH_SHORT).show();
            //aMax = 0;
            //gMax = 0;
            if(gThreshold < 10) {
                Toast.makeText(this, "Fall check 1", Toast.LENGTH_SHORT).show();
                aMax = 0;
                gMax = 0;
                time = System.currentTimeMillis();
            }
        }

        while(time > 0) {
            if(accelerometerValues[1] < ayMin) ayMin = accelerometerValues[1];
            if(accelerometerValues[1] > ayMax) ayMax = accelerometerValues[1];
            if(ayMin > -5 && ayMax < 5) {
                Toast.makeText(this, "Fall check 2", Toast.LENGTH_SHORT).show();
                f = 1;
            }
            if(System.currentTimeMillis() - time > 5000) {
                time = 0;
                ayMin = 0;
                ayMax = 0;
            }
        }

        if(f == 1){
            Toast.makeText(this, "Fall registered", Toast.LENGTH_SHORT).show();
            f = 0;
            startActivity(new Intent(MainActivity.this, CDTimer.class));
        }*/
    }

    @Override
    protected void onResume(){
        super.onResume();
        sm.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            accelerometerValues = event.values;
        }
        if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
            gyroscopeValues = event.values;
        }
        if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            magnetometerValues = event.values;
        }
        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
