package com.cyhex.flashcom;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TimePicker;

import com.cyhex.flashcom.lib.Transmitter;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_YES;
import static android.support.v7.app.AppCompatDelegate.setDefaultNightMode;


public class MainActivity extends AppCompatActivity {

    private ProgressDialog progress;
    private SharedPreferences sharedPref;
    //private PulseView pulseView;
    private CameraManager mCameraManager;
    private String mCameraId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultNightMode(MODE_NIGHT_YES);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        //flash
        boolean isFlashAvailable = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isFlashAvailable)
            showNoFlashError();
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert mCameraManager != null;
            mCameraId = mCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }
    public void showNoFlashError() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .create();
        alert.setTitle("Oops!");
        alert.setMessage("Flash not available in this device...");
        alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alert.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }



    public void sendData(View view) {
        //EditText editTest = findViewById(R.id.editText);
        TimePicker editTest = findViewById(R.id.simpleTimePicker);
        //editTest.setIs24HourView(true);
        //final String data = editTest.getText().toString();
        final int hour = editTest.getHour();
        final int minute = editTest.getMinute();
        LocalDateTime from = LocalDateTime.of(LocalDate.now(), LocalTime.now());
        System.out.println("date now:" + from);
        LocalDateTime to = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute));
        if (from.isAfter(to))
            to = to.plusDays(1);
        System.out.println("date to: " + to);
        Duration diff = Duration.between(from, to).plusMinutes(1);
        final String data = String.valueOf(diff.toMinutes());
        System.out.println("sending: " + data + " minutes");

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);



        progress = ProgressDialog.show(MainActivity.this, "Sending", "Setting alarm in: " + data + " minutes.");


        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Transmitter t = new Transmitter(mCameraManager, mCameraId);
                    t.setTimeHigh(Integer.parseInt(sharedPref.getString("high_pulse", "60")));
                    t.setTimeLow(Integer.parseInt(sharedPref.getString("low_pulse", "40")));
                    t.setTimeLightPulse(Integer.parseInt(sharedPref.getString("light_pulse", "50")));
                    t.transmit(data);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        });
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            progress.dismiss();
        }
    };

}