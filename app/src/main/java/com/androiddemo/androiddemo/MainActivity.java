package com.androiddemo.androiddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.androiddemo.androiddemo.modulecommunication.ActivityClient;
import com.androiddemo.androiddemo.modulecommunication.okhttp.OkHttpExecute;
import com.androiddemo.androiddemo.modulecommunication.thread.AThread;
import com.androiddemo.androiddemo.modulecommunication.thread.BThread;
import com.androiddemo.androiddemo.modulecommunication.ui.CustomButton;

public class MainActivity extends Activity  implements View.OnClickListener{

    private static final String TAG = "MainActivity";
    private volatile boolean stop = false;
    private CustomButton customButton;
    private Handler handler;
    private OkHttpExecute mOkHttpExecute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        customButton = (CustomButton)findViewById(R.id.btn_memory);
        customButton.setOnClickListener(this);
        findViewById(R.id.btn_thread).setOnClickListener(this);
        findViewById(R.id.btn_invalidate).setOnClickListener(this);
        findViewById(R.id.btn_okhttp).setOnClickListener(this);
        init();
    }

    private void init() {
        handler = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d("CustomButton", "handleMessage");
                customButton.invalidate();
            }
        };
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_memory) {
            onMemory();
        } else if (v.getId() == R.id.btn_thread) {
            onThread();
        } else if (v.getId() == R.id.btn_invalidate) {
            if (stop) {
                invalidateBtn();
                stop = false;
            } else {
                stop = true;
            }
        } else if (v.getId() == R.id.btn_okhttp) {
            onOkHttpRequest();
        }
    }

    private void onMemory() {
        Intent intent = new Intent(this, ActivityClient.class);
        this.startActivity(intent);
    }

    private void onThread() {
        String threadName = Thread.currentThread().getName();
        Log.d(AThread.TAG, threadName + " start.");
        BThread bt = new BThread();
        AThread at = new AThread(bt);
        try {
            bt.start();
//            Thread.sleep(2000);
            at.start();
            bt.interrupt();
            at.join();
        } catch (Exception e) {
            Log.d(AThread.TAG, "Exception from main");
        }
        Log.d(AThread.TAG, threadName + " end!");
    }


    private void invalidateBtn() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    Log.d("CustomButton", "run: send msg");
                    handler.sendEmptyMessage(1001);
                }
            }
        }).start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d("CustomButton", "dispatchTouchEvent  " + ev.getAction());
        return super.dispatchTouchEvent(ev);
    }

    private void onOkHttpRequest() {
        if (mOkHttpExecute == null) {
            mOkHttpExecute = new OkHttpExecute();
        }
        mOkHttpExecute.Execute();
    }
}
