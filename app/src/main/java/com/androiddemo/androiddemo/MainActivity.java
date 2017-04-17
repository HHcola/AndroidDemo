package com.androiddemo.androiddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.androiddemo.androiddemo.modulecommunication.ActivityClient;

public class MainActivity extends Activity  implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_memory).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_memory) {
            onMemory();
        }
    }

    private void onMemory() {
        Intent intent = new Intent(this, ActivityClient.class);
        this.startActivity(intent);
    }
}
