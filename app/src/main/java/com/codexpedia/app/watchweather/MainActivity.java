package com.codexpedia.app.watchweather;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.codexpedia.app.watchweather.data.sync.WatchWeatherAdapterSync;
import com.codexpedia.app.watchweather.fragment.DetailFragment;
import com.codexpedia.app.watchweather.util.Utility;
import com.crashlytics.android.Crashlytics;
import com.codexpedia.app.watchweather.fragment.ForecastFragment;
import com.codexpedia.app.watchweather.util.WeatherApplication;
import com.google.android.gms.analytics.Tracker;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {
    private Tracker mTracker;
    private static final String FORECASTFRAGMENT_TAG = "FFTAG";
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    public static final String IS_TWO_PANEL = "ISTWOPANEL";

    private String locationSetting;
    private String unitSetting;
    private boolean isTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        locationSetting = Utility.getPreferredLocation(getBaseContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(locationSetting);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        unitSetting = Utility.getPreferredUnit(getBaseContext());

        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            isTwoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
            ForecastFragment fFragment = new ForecastFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_forecast, fFragment, FORECASTFRAGMENT_TAG)
                    .commit();

        } else {
            isTwoPane = false;
            getSupportActionBar().setElevation(0f);
            ForecastFragment fFragment = new ForecastFragment();
            fFragment.setUseTodayLayout(!isTwoPane);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, fFragment, FORECASTFRAGMENT_TAG)
                .commit();
        }

        //initialize data sync
        WatchWeatherAdapterSync.initializeSyncAdapter(getApplicationContext());

        // Obtain the shared Tracker instance for google analytics
        WeatherApplication application = (WeatherApplication) getApplication();
        mTracker = application.getDefaultTracker();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.edit_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySettingsChange();
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        Bundle args = new Bundle();
        args.putParcelable(DetailFragment.DETAIL_URI, contentUri);
        args.putBoolean(IS_TWO_PANEL, isTwoPane);
        Fragment detailFragment = new DetailFragment();
        detailFragment.setArguments(args);
        if (isTwoPane) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, detailFragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, detailFragment, DETAILFRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void applySettingsChange() {
        String newLocation = Utility.getPreferredLocation(getBaseContext());
        String newUnit = Utility.getPreferredUnit(getBaseContext());

        // update the location in our second pane using the fragment manager
        if ((newLocation != null && !newLocation.equalsIgnoreCase(locationSetting))
            || (newUnit != null && !newUnit.equalsIgnoreCase(unitSetting)) ){
            getSupportActionBar().setTitle(Utility.capitalizeEveryFristLetter((newLocation)));
            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentByTag(FORECASTFRAGMENT_TAG);
            if (ff != null) {
                ff.onSettingChanged();
            }
            DetailFragment df = (DetailFragment)getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (df != null) {
                df.onSettingChanged(newLocation);
            }
            locationSetting = newLocation;
            unitSetting = newUnit;
        }

    }

}
