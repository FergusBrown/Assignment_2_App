package com.ewireless.assignment2app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    // Database reference initialisation
    //FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    //private DatabaseReference mDatabaseReference = mDatabase.getReference().child("Activity Data");


    // Tag for log prints
    private final static String TAG = "MainActivity";

    // Boolean for whether device is running API level 29 or later
    // TODO: implement permission check
    private boolean runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;


    // Intents for services definitions
    private Intent activityService;
    private Intent gaitAnalysisService;

    // Required for cadence recording
    public float cadence;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //status = findViewById(R.id.status);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // initialise intents for the various services
        activityService = new Intent(this, ActivityRecognitionService.class);
        gaitAnalysisService = new Intent(this, GaitAnalysisService.class);


        // Start services
        startService(activityService);
        startService(gaitAnalysisService);

        Log.d(TAG, "App initialized.");
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // Perform appropriate action when the user leaves (but does not destroy) the app
    @Override
    protected void onStop() {
        super.onStop();
    }

    // Unregister receivers which should only stop when the app is destroyed
    @Override
    protected void onDestroy() {
        // End all services
        stopService(activityService);
        super.onDestroy();
    }





    // Default methods automatically added
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
