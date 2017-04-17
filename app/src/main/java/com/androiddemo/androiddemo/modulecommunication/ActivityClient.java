package com.androiddemo.androiddemo.modulecommunication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.androiddemo.androiddemo.R;
import com.androiddemo.androiddemo.modulecommunication.memoryfile.CustomMemoryFile;

public class ActivityClient extends Activity {

    private EditText mEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mEditText = (EditText) findViewById(R.id.edit);
        findViewById(R.id.btn).setOnClickListener(onClickListener);
        onStartService();

    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onSendData();
        }
    };

    private void onSendData() {
        String text = mEditText.getText().toString();
        CustomMemoryFile.onClientWriteData(text.getBytes());
    }

    private void onStartService() {
        Intent intent = new Intent(this, ServiceService.class);
        this.startService(intent);
    }
}
