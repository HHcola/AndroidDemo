package com.androiddemo.androiddemo.modulecommunication.okhttp;

import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by hewei05 on 2017/5/8.
 */

public class OkHttpExecute {

    private static final String TAG = OkHttpExecute.class.getSimpleName();
    private OkHttpClient mOkHttpClient;

    public OkHttpExecute() {
        Log.d(TAG, "OkHttpExecute: " + TAG);
        mOkHttpClient = new OkHttpClient();
    }

    public void Execute() {
        Request request = new Request.Builder()
                .url("http://www.baidu.com")
                .build();
        try {
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.d(TAG, "Execute: onFailure" + e.getMessage());

                }

                @Override
                public void onResponse(Response response) throws IOException {
                    Log.d(TAG, "Execute: Response" + response.code() + " msg = " + response.message());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
