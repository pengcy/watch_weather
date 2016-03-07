package com.codexpedia.app.watchweather.data.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WatchWeatherServiceSync extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static WatchWeatherAdapterSync watchWeatherSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("WatchWeatherServiceSync", "onCreate - WatchWeatherServiceSync");
        synchronized (sSyncAdapterLock) {
            if (watchWeatherSyncAdapter == null) {
                watchWeatherSyncAdapter = new WatchWeatherAdapterSync(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return watchWeatherSyncAdapter.getSyncAdapterBinder();
    }
}