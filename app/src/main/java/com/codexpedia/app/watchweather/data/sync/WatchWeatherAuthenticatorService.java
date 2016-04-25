package com.codexpedia.app.watchweather.data.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;

/**
 * The service which allows the sync adapter framework to access the authenticator.
 */
public class WatchWeatherAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private WatchWeatherAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        Log.d("AuthenticatorService", "onCreate");
        // Create a new authenticator object
        mAuthenticator = new WatchWeatherAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("AuthenticatorService", "onCreate");
        return mAuthenticator.getIBinder();
    }
}