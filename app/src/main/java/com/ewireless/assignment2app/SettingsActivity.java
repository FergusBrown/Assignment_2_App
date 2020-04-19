package com.ewireless.assignment2app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Based on root_preferences.xml, this allows the user to change carer and patient details.
 * Once this activity is exited (specifically on pause) the changed details are uploaded to the database.
 * @author Fergus Brown s1525959
 */
public class SettingsActivity extends AppCompatActivity {

    // called on creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set layout based on xml
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    // create Firebase database object
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    // create database reference
    private DatabaseReference mDatabaseReference = mDatabase.getReference();

    // update the appropriate keys in the database when leaving settings page
    @Override
    protected void onPause() {
        super.onPause();

        // acquire all the values set locally in preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userKey = prefs.getString("User ID", null);
        String patientName = prefs.getString("Patient Name", null);
        String carerName = prefs.getString("Carer Name", null);
        String carerPhone = prefs.getString("Carer Phone", null);
        String carerEmail = prefs.getString("Carer Email", null);
        String postCode = prefs.getString("Postcode", null);

        int geofenceRadius = prefs.getInt("Geofence Radius", 50);
        double latitude = Double.longBitsToDouble(prefs.getLong("Latitude", 0));
        double longitude = Double.longBitsToDouble(prefs.getLong("Longitude", 0));

        // upload new user data to database
        User user = new User(userKey, patientName, carerName, carerPhone, carerEmail, postCode, geofenceRadius, latitude, longitude);
        mDatabaseReference.child("Users").child(userKey).child("Details").setValue(user);


    }

    // settings fragment for changing settings
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            // preferences items defined in root_preferences.xml
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            EditTextPreference carerPhone = findPreference("Carer Phone");

            if (carerPhone != null) {
                carerPhone.setOnBindEditTextListener(
                        new EditTextPreference.OnBindEditTextListener() {
                            @Override
                            public void onBindEditText(@NonNull EditText editText) {
                                editText.setInputType(InputType.TYPE_CLASS_PHONE);
                            }
                        });
            }
        }
    }



}