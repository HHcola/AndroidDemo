package com.androiddemo.androiddemo.modulecommunication.okhttp;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by hewei05 on 2017/5/9.
 */

public class OkHttp3Execute implements OkHttpImpl{

    private static final String TAG = OkHttpExecute.class.getSimpleName();
    private OkHttpClient mOkHttpClient;

    public OkHttp3Execute() {
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
                public void onFailure(Call call, IOException e) {
                    Log.d(TAG, "Execute: onFailure" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(TAG, "Execute: Response" + response.code() + " msg = " + response.message());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
