package com.ewireless.assignment2app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Activity which is launched the first time the app is opened.
 * This is a form which prompts the user to enter carer and patient details.
 * These details are saved to internal preferences and uploaded to the database
 * with a unique key to identify the user.
 * To register a geofence location, device location is monitored.
 * @author Fergus Brown s1525959
 */
public class FirstLaunch extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    // Tag for log prints
    private final static String TAG = "FirstLaunch";

    // google maps for location
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;

    // UI elements
    EditText patientNameText;
    EditText carerNameText;
    EditText carerPhoneText;
    EditText carerEmailText;
    EditText postCodeText;
    TextView radiusLabel;

    // int to store geofence radius
    private int radius;

    // method run when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set layout defined by xml file
        setContentView(R.layout.activity_first_launch);

        // Bind UI elements
        patientNameText = (EditText)findViewById(R.id.patientName);
        carerNameText = (EditText)findViewById(R.id.carerName);
        carerEmailText = (EditText)findViewById(R.id.carerEmail);
        carerPhoneText = (EditText)findViewById(R.id.carerPhone);
        postCodeText = (EditText)findViewById(R.id.postCode);

        //Slider
        // set a change listener on the SeekBar
        SeekBar seekBar = findViewById(R.id.radiusSlider);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        // radius is set by seekbar value
        radius = seekBar.getProgress();
        radiusLabel = findViewById(R.id.radiusText);
        radiusLabel.setText("Patient home radius: " + radius);

        // initialise google API client
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        // Open initial dialog box
        openDialog();
    }

    // create welcome dialogue
    private void openDialog() {
        // Dialog to tell user to fill in details
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FirstLaunch.this);
        alertDialog.setTitle(R.string.welcome_dialog_title);
        alertDialog.setMessage(R.string.welcome_text);

        alertDialog.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = alertDialog.create();
        alert.show();
    }

    // create Firebase database object
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    // create a database reference
    private DatabaseReference mDatabaseReference = mDatabase.getReference();

    // Store preferences locally and upload data to database under a unique user key
    public void formComplete(View view) {

        // create a unique user key
        String userKey = mDatabaseReference.child("Users").push().getKey();

        // get values entered into the form
        String patientName = patientNameText.getText().toString();
        String carerName = carerNameText.getText().toString();
        String carerPhone = carerPhoneText.getText().toString();
        String carerEmail = carerEmailText.getText().toString();
        String postCode = postCodeText.getText().toString();

        // create a new user object to upload to database
        User user = new User(userKey, patientName, carerName, carerPhone, carerEmail, postCode, radius, latitude, longitude);

        // Upload the user details to the database
        mDatabaseReference.child("Users").child(userKey).child("Details").setValue(user);

        // Set EditTextValues to preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("User ID", userKey);
        editor.putString("Patient Name", patientName );
        editor.putString("Carer Name", carerName );
        editor.putString("Carer Phone", carerPhone );
        editor.putString("Carer Email", carerEmail);
        editor.putString("Postcode", postCode);
        editor.putInt("Geofence Radius", radius);
        editor.putLong("Latitude", Double.doubleToRawLongBits(latitude));
        editor.putLong("Longitude", Double.doubleToRawLongBits(longitude));
        editor.putBoolean("Setup Complete", true);

        // flush the buffer
        editor.apply();

        // Activity Finished
        finish();
    }

    // Close the welcome dialogue
    public void closeDialog(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.welcome_dialog);
        dialog.dismiss();
    }

    // Seekbar listener
    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        // run when slider value is changed
        @Override
        public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
            // updated continuously as the user slides the thumb

            // update value of radius
            radius = value;
            radiusLabel.setText("Patient home radius: " + radius);
        }


        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // called after the user finishes moving the SeekBar
        }
    };

    // Google map API methods
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLocation();
    }

    // variables to store lat and long
    double latitude;
    double longitude;

    // get the location when changed -- used to get geofence location
    private void getLocation() {
        Log.d(TAG, "start location monitor");
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }
        }

    // evaluate connection to API
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());
    }

    // connect to location API on start
    @Override
    protected void onStart() {
        super.onStart();

        // reconnect to google API client
        googleApiClient.reconnect();
    }
}
