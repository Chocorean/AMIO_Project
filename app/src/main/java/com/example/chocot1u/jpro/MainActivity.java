package com.example.chocot1u.jpro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.example.chocot1u.jpro.service.CheckService;
import com.example.chocot1u.jpro.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {
    Button tbSensors, tbSettings;
    Intent sensorsIntent, settingsIntent, checkServiceIntent, mailIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // service intents
        checkServiceIntent = new Intent(this, CheckService.class);
        startService(checkServiceIntent);
        // activity intents
        sensorsIntent = new Intent(this, SensorsActivity.class);
        settingsIntent = new Intent(this, SettingsActivity.class);
        // buttons
        tbSensors = findViewById(R.id.TB1);
        tbSettings = findViewById(R.id.TB2);
        // buttons actions
        tbSensors.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Clicked sensors button");
                startActivity(sensorsIntent);
            }
        });
        tbSettings.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Clicked settings button");
                startActivity(settingsIntent);
            }
        });
    }
}