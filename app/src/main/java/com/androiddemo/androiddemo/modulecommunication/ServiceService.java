package com.androiddemo.androiddemo.modulecommunication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.androiddemo.androiddemo.modulecommunication.memoryfile.CustomMemoryFile;

public class ServiceService extends Service {

    private static String TAG = "ServiceService";
    private static final int SHOW_DATA = 1001;
    private static final String DATA_KEY = "data_key";
    private static Context mContext;
    private boolean mRunning = true;
    public ServiceService() {
    }


    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate: ");
        mContext = this.getApplicationContext();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                onRun();

            }
        });
        thread.start();
    }

    @Override
    public  int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        return START_STICKY;
    }
    
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void onRun() {

        while (mRunning) {
            if (CustomMemoryFile.mLength > 0) {
                byte [] data = new byte[CustomMemoryFile.mLength];
                CustomMemoryFile.onServiceReadData(data);

                if (data != null && data.length > 0) {
                    String sendData = new String(data);
                    Message message = new Message();
                    message.what = SHOW_DATA;
                    Bundle bundle = new Bundle();
                    bundle.putString(DATA_KEY, sendData);
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private static Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_DATA) {
                if (mContext != null) {
                    Toast.makeText(mContext, msg.getData().getString(DATA_KEY), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


    @Override
    public void onDestroy() {
        mRunning = false;
    }


}
