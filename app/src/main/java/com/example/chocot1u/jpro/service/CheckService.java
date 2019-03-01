package com.example.chocot1u.jpro.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckService extends Service {
    private final String MOTES_URL = "http://iotlab.telecomnancy.eu/rest/info/motes";
    private final String DATA_URL = "http://iotlab.telecomnancy.eu/rest/data/1/light1-light2/last";

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
        // Log.d("CheckService","Setting mode to sticky");
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
                    // discover motes
                    JSONObject response = new JSONObject(askURL(MOTES_URL));
                    JSONArray motes = response.getJSONArray("sender");
                    int motesNb = response.getInt("motesNb");
                    // get data and read it
                    JSONArray data = new JSONObject(askURL(DATA_URL)).getJSONArray("data");
                    ArrayList<Integer> lit_lights = new ArrayList<>();
                    for (int i=0; i<data.length(); i++) {
                        JSONObject light_data = data.getJSONObject(i);
                        if (light_data.getDouble("value") > 500.0) {
                            lit_lights.add(i);
                        }
                    }
                    if (lit_lights.size() != 0) {
                        for (int i : lit_lights) {
                            JSONObject light_data = data.getJSONObject(i);
                            // first check if we need to notify or not
                            long timestamp = light_data.getLong("timestamp") / 1000;
                            timestamp %= 84600;
                            boolean isWeekNotificationTime = isWeekNotificationTime(timestamp);
                            boolean isWeekEndNotificationTime = isWeekEndNotificationTime(timestamp);
                            if (!isWeekNotificationTime && !isWeekEndNotificationTime) {
                                return;
                            }
                            // if we're here, we have to notify
                            JSONObject mote = null;
                            for (int j=0; j<motes.length(); j++) {
                                JSONObject tmpMote = motes.getJSONObject(j);
                                if (tmpMote.getString("mac").equals(light_data.getString("mote"))) {
                                    mote = tmpMote;
                                    break;
                                }
                            }
                            assert mote != null; // might never happen
                            // now send notification of mal
                            if (isWeekNotificationTime) {
                                // notification
                                // TODO
                            }
                            if (isWeekEndNotificationTime) {
                                // mail
                                // TODO
                            }
                        }
                    }

                    /*
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
                    }*/
                } catch (JSONException e) {
                    Log.d("CheckService", "Response is not JSON formatted.");
                }
            }
        };
    }

    private String askURL(String url) {
        try {
            Log.d("CheckService", String.format("[TimerTask] GET %s", url));
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
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
            return content.toString();
        } catch (UnsupportedEncodingException e) {
            Log.d("CheckService", "UnsupportedEncodingException");
        } catch (ProtocolException e) {
            Log.d("CheckService", "ProtocolException");
        } catch (MalformedURLException e) {
            Log.d("CheckService", "MalformedURLException");
        } catch (IOException e) {
            Log.d("CheckService", "IOException");
        }
        return "";
    }

    private boolean isWeekNotificationTime(long timestamp) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        DateFormat formatter = new SimpleDateFormat("hh:mm");
        try {
            long w_start_hour = formatter.parse(sp.getString("w_start_hour", "19:00")).getTime();
            long w_end_hour = formatter.parse(sp.getString("w_end_hour", "23:00")).getTime();
            if (w_end_hour < w_start_hour)
                w_end_hour += 84600;
            return timestamp >= w_start_hour && timestamp <= w_end_hour;
        } catch (ParseException e) {
            Log.d("CheckService", "ParseException");
        }
        return false;
    }

    private boolean isWeekEndNotificationTime(long timestamp) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm");
        try {
            long we_start_hour = formatter.parse(sp.getString("we_start_hour", "23:00")).getTime();
            long we_end_hour = formatter.parse(sp.getString("we_end_hour", "06:00")).getTime();
            if (we_end_hour < we_start_hour)
                we_end_hour += 84600;
            return timestamp >= we_start_hour && timestamp <= we_end_hour;
        } catch (ParseException e) {
            Log.d("CheckService", "ParseException");
        }
        return false;
    }
}
