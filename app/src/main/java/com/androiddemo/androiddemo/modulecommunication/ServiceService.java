package com.androiddemo.androiddemo.modulecommunication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ServiceService extends Service {
    public ServiceService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
