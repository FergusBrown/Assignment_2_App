package com.ewireless.assignment2app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Contacts;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseReference = mDatabase.getReference();

    // update the appropriate keys in the database when leaving settings page
    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userKey = prefs.getString("User ID", null);
        String patientName = prefs.getString("Patient Name", null);
        String carerName = prefs.getString("Carer Name", null);
        String carerPhone = prefs.getString("Carer Phone", null);
        String carerEmail = prefs.getString("Carer Email", null);
        int geofenceRadius = prefs.getInt("Geofence Radius", 50);
        double latitude = Double.longBitsToDouble(prefs.getLong("Latitude", 0));
        double longitude = Double.longBitsToDouble(prefs.getLong("Longitude", 0));
        User user = new User(userKey, patientName, carerName, carerPhone, carerEmail, geofenceRadius, latitude, longitude);

        mDatabaseReference.child("Users").child(userKey).child("Details").setValue(user);


    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

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