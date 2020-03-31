package com.example.falldetection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

    private SensorManager sm;
    private Sensor aSensor;

    float[] accelerometerValues = new float[3];
    double threshold;
    double max;

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

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void calculateOrientation() {
        threshold = Math.sqrt(accelerometerValues[0]*accelerometerValues[0] + accelerometerValues[1]*accelerometerValues[1] + accelerometerValues[2]*accelerometerValues[2]);

        tvAcceleration.setText("Accelerometer: " + threshold + "/" + accelerometerValues[0] + "/" + accelerometerValues[1] + "/" + accelerometerValues[2] + "/" + max);

        if (threshold > 20) Toast.makeText(this, "Fall detected", Toast.LENGTH_SHORT).show();

        if (threshold > max) max = threshold;
    }

    @Override
    protected void onResume(){
        super.onResume();
        sm.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
        calculateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
