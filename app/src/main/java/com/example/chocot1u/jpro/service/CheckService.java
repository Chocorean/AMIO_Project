package com.example.chocot1u.jpro.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.chocot1u.jpro.R;
import com.example.chocot1u.jpro.SensorsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckService extends Service {
    private final String API_URL = "http://iotlab.telecomnancy.eu/rest/data/1/light1-light2/last";

    private final static AtomicInteger c = new AtomicInteger(0);
    public static int getID() {
        return c.incrementAndGet();
    }
    // timertask
    private TimerTask timerTask;

    private Timer timer = new Timer();

    // binder
    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        CheckService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CheckService.this;
        }
    }

    // service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("CheckService","Setting mode to sticky");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        timer.scheduleAtFixedRate(timerTask,0,  10000);
    }

    @Override
    public void onDestroy() {
        Log.d("CheckService", "Stopping CheckService");
        timer.cancel();
        this.stopSelf();
    }

    public CheckService() {
        final CheckService instance = this;
         timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    Log.d("CheckService", String.format("[TimerTask] GET %s", API_URL));
                    HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(1000);
                    conn.connect();
                    Reader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    BufferedReader rd = new BufferedReader(reader);
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    conn.disconnect();
                    // analysing response
                    JSONObject json = new JSONObject(content.toString());
                    JSONArray data = json.getJSONArray("data");
                    // sensor 1
                    JSONObject sensor1Data = data.getJSONObject(0);
                    long timestamp1 = sensor1Data.getLong("timestamp");
                    double light1Value = sensor1Data.getDouble("value");
                    String mote1 = sensor1Data.getString("mote");
                    boolean isLight1On = light1Value > 275;
                    // sensor 2
                    JSONObject sensor2Data = data.getJSONObject(3);
                    long timestamp2 = sensor2Data.getLong("timestamp");
                    double light2Value = sensor2Data.getDouble("value");
                    String mote2 = sensor2Data.getString("mote");
                    boolean isLight2On = light2Value > 275;
                    // let's go
                    if (isLight1On || isLight2On) {
                        Log.d("CheckService", "A light is on.");
                        // checking if we should notify or not
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(instance);
                        DateFormat formater = new SimpleDateFormat("hh:mm");
                        // notification
                        long w_start_hour = formater.parse(sp.getString("w_start_hour", "19:00")).getTime();
                        long w_end_hour = formater.parse(sp.getString("w_end_hour", "23:00")).getTime();
                        // mail
                        long we_start_hour = formater.parse(sp.getString("we_start_hour", "23:00")).getTime();
                        long we_end_hour = formater.parse(sp.getString("we_end_hour", "06:00")).getTime();
                        // current time
                        long current_time = (System.currentTimeMillis() / 1000) % 86400000;
                        boolean w = current_time <= w_end_hour && w_start_hour <= current_time;
                        boolean we = current_time < we_end_hour && we_start_hour <= current_time;
                        Log.d("DEBUG", ""+w_start_hour+" "+current_time+" "+w_end_hour);
                        if (!w && !we) {
                            return;
                        }
                        // notifying
                        String msg = "";
                        if (isLight1On) {
                            SimpleDateFormat formatter = new SimpleDateFormat("[hh:mm:ss]");
                            String ts = formatter.format(new Date(timestamp1));
                            msg += String.format("[%s] Light 1 was turned on at %s.\n", mote1, ts);
                        }
                        if (isLight2On) {
                            SimpleDateFormat formatter = new SimpleDateFormat("[hh:mm:ss]");
                            String ts = formatter.format(new Date(timestamp2));
                            msg += String.format("[%s] Light 2 was turned on at %s.\n", mote2, ts);
                        }
                        if (w) {
                            // preparing intent
                            Intent notificationIntent = new Intent(instance, SensorsActivity.class);
                            PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, notificationIntent, 0);

                            // send a notification
                            Notification notification  = new Notification.Builder(instance)
                                    .setContentTitle("A light is on !")
                                    .setContentText(msg)
                                    .setSmallIcon(R.drawable.ic_stat_light)
                                    .setAutoCancel(true)
                                    .setPriority(Notification.PRIORITY_HIGH)
                                    .setContentIntent(contentIntent)
                                    .setVisibility(Notification.VISIBILITY_PUBLIC).build();
                            NotificationManager notificationManager =
                                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.notify(getID(), notification);
                            Log.d("CheckService", "Sent notification.");
                        }
                        if (!we) {
                            Intent mailIntent = new Intent();
                            mailIntent.setAction(Intent.ACTION_SEND);
                            mailIntent.setData(Uri.parse("mailto:"));
                            mailIntent.putExtra(Intent.EXTRA_TEXT, msg);
                            mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {String.format("Supervisor<%s>", PreferenceManager.getDefaultSharedPreferences(instance).getString("email", "baptiste.chocot@gmail.com"))});
                            mailIntent.setType("text/plain");
                            startActivity(Intent.createChooser(mailIntent, "Alert supervisor"));
                            Log.d("CheckService", "Sent mail.");
                        }
                    }
                } catch (IOException e) {
                    Log.d("CheckService", "URL is malformed. please contact devs.");
                } catch (JSONException e) {
                    Log.d("CheckService", "Response is not JSON formatted.");
                } catch (ParseException e) {
                    Log.d("CheckService", "Unable to parse hour.");
                }
            }
        };
    }
}
