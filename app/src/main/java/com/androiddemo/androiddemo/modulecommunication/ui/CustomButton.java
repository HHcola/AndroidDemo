package com.androiddemo.androiddemo.modulecommunication.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

/**
 * Created by hewei05 on 2017/5/3.
 */

public class CustomButton extends Button {

    private long lastTime = 0;

    public CustomButton(Context context) {
        super(context);
    }


    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("CustomButton", "onDraw ");
        super.onDraw(canvas);
        calcFPS();

    }


    @Override
    public void invalidate() {
        super.invalidate();
        Log.d("CustomButton", "invalidate ");
    }

    private void calcFPS() {
        if (lastTime <= 0) {
            lastTime = System.currentTimeMillis();
        } else {
            long currentTime = System.currentTimeMillis();
            long timeGap = currentTime - lastTime;
            Log.d("CustomButton", "calcFPS: " + timeGap);
        }
    }
}
