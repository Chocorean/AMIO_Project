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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chocot1u.jpro.service.CheckService;
import com.example.chocot1u.jpro.settings.SettingsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    Button tbSettings;
    Intent settingsIntent, checkServiceIntent;
    private LinearLayout layout;
    private MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                layout = findViewById(R.id.layout);
                HashMap<String, ArrayList<Double>> datamap = new HashMap();
                try {
                    JSONArray data = new JSONObject(CheckService.askURL(CheckService.DATA_URL)).getJSONArray("data");
                    for (int i=0; i<data.length(); i++) {
                        JSONObject piece = data.getJSONObject(i);
                        String mote = piece.getString("mote");
                        if (datamap.get(mote) == null) {
                            datamap.put(mote, new ArrayList<Double>());
                        }
                        datamap.get(mote).add((piece.getDouble("value")));
                    }
                } catch (JSONException e) {
                    Log.d("MainActivity", "Unable to parse JSON.");
                    return;
                }
                for (Map.Entry<String, ArrayList<Double>> entry : datamap.entrySet()) {
                    TextView tv = new TextView(instance);
                    ArrayList<Double> lightValues = entry.getValue();
                    String txt = String.format("Mote [%s]\n", entry.getKey());
                    for (int i=0; i<lightValues.size(); i++) {
                        Double value = lightValues.get(i);
                        txt += String.format("    Light #%d: %f", i+1, value);
                        txt += value > 500 ? " (active)\n" : " (inactive)\n";
                    }
                    tv.setText(txt);
                    layout.addView(tv);
                }
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // service intents
        checkServiceIntent = new Intent(this, CheckService.class);
        startService(checkServiceIntent);
        // activity intents
        settingsIntent = new Intent(this, SettingsActivity.class);
        // buttons
        tbSettings = findViewById(R.id.TB2);
        // buttons actions
        tbSettings.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Clicked settings button");
                startActivity(settingsIntent);
            }
        });
    }
}