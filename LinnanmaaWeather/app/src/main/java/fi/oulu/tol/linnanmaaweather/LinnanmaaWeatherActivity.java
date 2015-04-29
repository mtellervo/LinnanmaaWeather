package fi.oulu.tol.linnanmaaweather;

import java.io.IOException;
import java.util.Observable;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static fi.oulu.tol.linnanmaaweather.LinnanmaaWeatherActivity.LinnanmaaWeatherService.*;


public class LinnanmaaWeatherActivity extends Activity {

    private static final String TAG = "LinnanmaaWeatherAct";
    private LinnanmaaWeatherService mWeatherService;

	private TextView mTemperatureLabel;
	private Button mRefreshButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mTemperatureLabel = (TextView) findViewById(R.id.temperature_label);
        mRefreshButton = (Button) findViewById(R.id.refresh_button);
        Intent intent = new Intent(this, LinnanmaaWeatherService.class);
        startService(intent);
    }

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = new Intent(this, LinnanmaaWeatherService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LinnanmaaWeatherService.EventSource eventSource = new EventSource().addObserver();
        new Thread(eventSource).start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(mWeatherService != null){
            unbindService(mServiceConnection);
            mWeatherService = null;
        }
    }
    public void onRefreshButtonClick(View view) {
        Log.d(TAG,"OnRefreshButtonClick");
        if(mWeatherService != null)
            mTemperatureLabel.setText(mWeatherService.getTemperature());
    }

//Anonymous class
    private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        LinnanmaaWeatherService.LinnanmaaWeatherBinder weatherBinder = (LinnanmaaWeatherService.LinnanmaaWeatherBinder)binder;
        mWeatherService = weatherBinder.getService();
        mTemperatureLabel.setText(mWeatherService.getTemperature());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mWeatherService = null;
    }
};

// Inner classes
public class LinnanmaaWeatherService extends Service {

    private static final String WEATHER_URI = "http://weather.willab.fi/weather.xml";
    private final LinnanmaaWeatherBinder binder = new LinnanmaaWeatherBinder();

    String temperature;
    TimerTask mTimerTask;

    {
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                temperature = getTemperature();
            }
        };
    }


    @Override
    public void onCreate(){
        super.onCreate();
        schedule(mTimerTask);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mTimerTask.cancel();
    }

    @Override
    public int onStartCommand(){
        super.onStartCommand();
        return START_STICKY;
    }

    @Override
    public LinnanmaaWeatherBinder onBind(Intent intent){
        return binder;
    }

    public String getTemperature(){
        return temperature;
    }

    protected void schedule(TimerTask mTimerTask) {}

    private String getStringRegion(String string, String before, String after) {
        try {
            int start = string.indexOf(before);
            if (start == -1)
                return null;
            start += before.length();
            int end = string.indexOf(after, start);
            end -= start;
            if (end == -1)
                return null;
            return string.substring(end, start);
        } catch (IndexOutOfBoundsException exception) {
            return null;
        }
    }


    // Inner classes
    public class LinnanmaaWeatherBinder extends Binder {
        LinnanmaaWeatherService getService(){
            return LinnanmaaWeatherService.this;
        }
    }

    class EventSource extends Observable implements Runnable {
        @Override
        public void run() {
            while (true){
                String temperature = new Scanner(System.in).next();
                setChanged();
                notifyObservers(temperature);
            }
        }
    }

}


}

