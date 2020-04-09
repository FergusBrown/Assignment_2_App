package com.ewireless.assignment2app;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;


import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    // Tag for log prints
    private final static String TAG = "MainActivity";

    // Extra message for find home activity
    public final static String EXTRA_MESSAGE ="com.ewireless.assignment2app";

    // Boolean for whether device is running API level 29 or later
    // TODO: implement permission check
    private boolean runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    // Intents for services definitions
    private Intent activityService;
    private Intent gaitAnalysisService;
    private Intent fallDetectionService;

    // Class to handle permissions
    private PermissionsHelper permissionsHelper;

    // required for geofencing
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;
    private GoogleMap googleMap;
    private GeofencingRequest geofencingRequest;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean setupComplete = prefs.getBoolean("Setup Complete", false);

        if (!setupComplete) {
            // begin setup user form
            callSetup();
        }

        // check permissions
        if (!checkPermissions()){
            // Check permissions
            requestPermissions();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // bind map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // initialise google API client if setup complete
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        Log.d(TAG, "App initialized.");
    }

    private void callSetup() {

        startActivity(new Intent(MainActivity.this, FirstLaunch.class));
        //Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_LONG)
                //.show();

    }


    // Initialise all background services
    private void startServices() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean setupComplete = prefs.getBoolean("Setup Complete", false);

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

            if (!isServiceRunning(FallDetectionService.class)) {
                fallDetectionService = new Intent(this, FallDetectionService.class);
                startService(fallDetectionService);
            }
        }

    }

    // checks if a service is currently running
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

        // Log google API
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (response != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Service Not Available");
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, response, 1).show();
        } else {
            Log.d(TAG, "Google play service available");
        }

        // Initialise services if not already running
        startServices();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // reconnect to google API client
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean setupComplete = prefs.getBoolean("Setup Complete", false);

        if (setupComplete) {
            googleApiClient.reconnect();
        }

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
        stopService(fallDetectionService);
        googleApiClient.reconnect();
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
        int fineLocationPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int smsPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        //TODO: this perm is only for API 29+, may need to be handled differently for lower level APIs
        int backgroundLocationPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        if(activityPerm == PackageManager.PERMISSION_GRANTED
            && foregroundPerm == PackageManager.PERMISSION_GRANTED
                && wakePerm == PackageManager.PERMISSION_GRANTED
                && writePerm == PackageManager.PERMISSION_GRANTED
                && fineLocationPerm == PackageManager.PERMISSION_GRANTED
                && coarseLocationPerm == PackageManager.PERMISSION_GRANTED
                && backgroundLocationPerm == PackageManager.PERMISSION_GRANTED
                && smsPerm == PackageManager.PERMISSION_GRANTED) {
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.SEND_SMS
                );
    }

    /*************** End methods for checking permissions *********************/

    /*************** Begin methods for geofencing *********************/

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google Api Client Connected");
        startGeofencing();
        startLocationMonitor();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());
    }

    // Non override methods below

    // starts geofencing service
    private void startGeofencing() {
        Log.d(TAG, "Start geofencing monitoring call");
        geofenceIntent = getGeofencePendingIntent();
        geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(getGeofence())
                .build();

        if (!googleApiClient.isConnected()) {
            Log.d(TAG, "Google API client not connected");
        } else {
            try {
                LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest, geofenceIntent).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Successfully Geofencing Connected");
                        } else {
                            Log.d(TAG, "Failed to add Geofencing " + status.getStatus());
                        }
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        invalidateOptionsMenu();
    }

    private PendingIntent geofenceIntent;

    @NonNull
    private Geofence getGeofence() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int geofenceRadius = prefs.getInt("Geofence Radius", 50);

        LatLng latLng = Constants.AREA_LANDMARKS.get(Constants.GEOFENCE_ID);
        double latitude;
        double longitude;

        latitude = Double.longBitsToDouble(prefs.getLong("Latitude", Double.doubleToLongBits(latLng.latitude)));
        longitude = Double.longBitsToDouble(prefs.getLong("Longitude", Double.doubleToLongBits(latLng.latitude)));


        return new Geofence.Builder()
                .setRequestId(Constants.GEOFENCE_ID)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latitude, longitude, geofenceRadius)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // returns the intent for the geofence service
    private PendingIntent getGeofencePendingIntent() {
        if (geofenceIntent != null) {
            return geofenceIntent;
        }
        Intent intent = new Intent(this, GeofenceRegistrationService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private void stopGeoFencing() {
        geofenceIntent = getGeofencePendingIntent();
        LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofenceIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess())
                            Log.d(TAG, "Stop geofencing");
                        else
                            Log.d(TAG, "Not stop geofencing");
                    }
                });
        invalidateOptionsMenu();
    }



    /*************** geofence debug below (displays geofence perimeter and location on map *********************/

    // required for markers
    private MarkerOptions markerOptions;
    private Marker currentLocationMarker;

    // Shows the map when ready (required when implementing onMapReadyCallback)
    // Use this for debug
    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        this.googleMap = googleMap;
        LatLng latLng = Constants.AREA_LANDMARKS.get(Constants.GEOFENCE_ID);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int geofenceRadius = prefs.getInt("Geofence Radius", 50);
        double latitude;
        double longitude;

        latitude = Double.longBitsToDouble(prefs.getLong("Latitude", Double.doubleToLongBits(latLng.latitude)));
        longitude = Double.longBitsToDouble(prefs.getLong("Longitude", Double.doubleToLongBits(latLng.latitude)));


        googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Home"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));

        googleMap.setMyLocationEnabled(true);

        Circle circle = googleMap.addCircle(new CircleOptions()
                .center(new LatLng(latitude, longitude))
                .radius(geofenceRadius)
                .strokeColor(Color.RED)
                .strokeWidth(4f));



    }

    // Monitors location, also sets markers on the map. **NOT REQUIRED FOR GEOFENCING**
    private void startLocationMonitor() {
        Log.d(TAG, "start location monitor");
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (googleMap != null) {
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                        if (currentLocationMarker != null) {
                            currentLocationMarker.remove();
                        }
                        markerOptions = new MarkerOptions();
                        markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                        markerOptions.title("Current Location");
                        currentLocationMarker = googleMap.addMarker(markerOptions);
                        Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }
        }


    }
    /*************** End methods for geofencing *********************/

    /*************** Begin methods for find home ********************/

    public void goToMap(View view) {
        Intent intent = new Intent(MainActivity.this, StartMapActivity.class);
        intent.putExtra(EXTRA_MESSAGE, "ML12 6QB");
        startActivity(intent);
        // TODO: this should stop all services on open
    }

    /*************** End methods for find home **********************/

}
