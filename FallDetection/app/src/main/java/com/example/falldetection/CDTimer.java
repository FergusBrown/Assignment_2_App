package com.example.falldetection;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;

public class CDTimer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cdtimer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvTimer = (TextView)findViewById(R.id.tvTimer);

        timer.start();
    }

    TextView tvTimer;

    CountDownTimer timer = new CountDownTimer(30000, 1000) {

        public void onTick(long millisUntilFinished) {
            tvTimer.setText("seconds remaining: " + millisUntilFinished / 1000);
        }

        public void onFinish() {
            tvTimer.setText("Sending text message");
            sendMessage();
        }
    };
    //.start();

    public void FD(View view){
        timer.cancel();
        startActivity(new Intent(CDTimer.this, MainActivity.class));
    }

    public void sendMessage(){
        String messageToSend = "Fall has been detected";
        String number = "xxxxxxxxxx";

        SmsManager sms = SmsManager.getDefault();
        PendingIntent sentPI;
        String SENT = "SMS_SENT";

        sentPI = PendingIntent.getBroadcast(this, 0,new Intent(SENT), 0);

        sms.sendTextMessage(number, null, messageToSend, sentPI, null);
    }

}
