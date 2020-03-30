package com.ewireless.assignment2app;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

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

    // Class to handle permissions
    private PermissionsHelper permissionsHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean setupComplete = prefs.getBoolean("setupComplete", false);

        if (!setupComplete) {
            // begin setup user form
            callSetup();
        }

        // check permissions
        if (!checkPermissions()){
            // Check permissions
            requestPermissions();
        }




        super.onCreate(savedInstanceState);

        // TODO: replace this with a splash screen or intro screen
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        Log.d(TAG, "App initialized.");

    }

    private void callSetup() {

        startActivity(new Intent(MainActivity.this, FirstLaunch.class));
        Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_LONG)
                .show();

    }


    // Initialise all background services
    private void startServices() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean setupComplete = prefs.getBoolean("setupComplete", false);

        // If permissions are granted
        if (checkPermissions() && setupComplete) {
            // Start services if not already running
            if (!isServiceRunning(ActivityRecognitionService.class)) {
                activityService = new Intent(this, ActivityRecognitionService.class);
                startService(activityService);
            }

            if (!isServiceRunning(GaitAnalysisService.class)) {
                gaitAnalysisService = new Intent(this, GaitAnalysisService.class);
                startService(gaitAnalysisService);
            }
        }

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialise services if not already running
        startServices();
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
        stopService(gaitAnalysisService);
        super.onDestroy();
    }


    /*
     // API 29+ only: check permissions are granted
    private boolean activityRecognitionPermissionApproved() {

        // TODO: Review permission check for 29+.
        if (runningQOrLater) {

            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
            );
        } else {
            return true;
        }
    }*/



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

        //Open settings menu if selected
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    /*************** Begin methods for checking permissions *********************/


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private boolean checkPermissions() {

        int activityPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION);
        int foregroundPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE);
        int wakePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK);
        int writePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(activityPerm == PackageManager.PERMISSION_GRANTED
            && foregroundPerm == PackageManager.PERMISSION_GRANTED
                && wakePerm == PackageManager.PERMISSION_GRANTED
                && writePerm == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    // Must call all required permissions as:
    // "Manifest.permission.YOUR_PERMISSION
    private void requestPermissions() {
        permissionsHelper = new PermissionsHelper();
        permissionsHelper.checkAndRequestPermissions(this,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                );
    }
    /*************** End methods for checking permissions *********************/

}
