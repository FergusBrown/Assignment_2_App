package com.ewireless.assignment2app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class FirstLaunch extends AppCompatActivity {


    EditText patientName;
    EditText carerName;
    EditText carerPhone;
    EditText carerEmail;

    TextView radiusLabel;
    private int radius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_launch);

        patientName = (EditText)findViewById(R.id.patientName);
        carerName = (EditText)findViewById(R.id.carerName);
        carerEmail = (EditText)findViewById(R.id.carerEmail);
        carerPhone = (EditText)findViewById(R.id.carerPhone);

        //Slider
        // set a change listener on the SeekBar
        SeekBar seekBar = findViewById(R.id.radiusSlider);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        radius = seekBar.getProgress();
        radiusLabel = findViewById(R.id.radiusText);
        radiusLabel.setText("Patient home radius: " + radius);

        // Open initial dialog box
        openDialog();
    }

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

    public void formComplete(View view) {
        // Set EditTextValues to preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("Patient Name", patientName.getText().toString() );
        editor.putString("Carer Name", carerName.getText().toString() );
        editor.putString("Carer Phone", carerPhone.getText().toString() );
        editor.putString("Carer Email", carerEmail.getText().toString() );
        editor.putInt("Geofence Radius", radius);

        editor.putBoolean("Setup Complete", true);

        // flush the buffer
        editor.apply();

        // Activity Finished
        finish();
    }

    public void closeDialog(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.welcome_dialog);
        dialog.dismiss();
    }

    // Seekbar listener
    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
            // updated continuously as the user slides the thumb

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

}
