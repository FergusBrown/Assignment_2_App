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

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    private void calculateOrientation() {
        aThreshold = Math.sqrt(accelerometerValues[0]*accelerometerValues[0] + accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]);
        gThreshold = Math.sqrt(gyroscopeValues[0]*gyroscopeValues[0] + gyroscopeValues[1]*gyroscopeValues[1] + gyroscopeValues[2]*gyroscopeValues[2]);

        if (aThreshold > aMax) aMax = aThreshold;
        if (gThreshold > gMax) gMax = gThreshold;

        tvAcceleration.setText("Acceleration: " + aThreshold);
        tvRotation.setText("Rotation: " + gThreshold);

        if (aThreshold > 20 && time == 0) {
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
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        sm.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
