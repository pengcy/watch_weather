package com.codexpedia.app.watchweather.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.codexpedia.app.watchweather.R;
import com.codexpedia.app.watchweather.WeatherAdapter;
import com.codexpedia.app.watchweather.data.WeatherContract;
import com.codexpedia.app.watchweather.data.sync.WatchWeatherAdapterSync;
import com.codexpedia.app.watchweather.util.Utility;


public class ForecastFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int FORECAST_LOADER = 0;
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };
    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, thes must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;

    private static final String SELECTED_KEY = "selected_position";
    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private ListView listView;
    private WeatherAdapter weatherAdapter;
    private int mPosition = ListView.INVALID_POSITION; // When tablets rotate, the currently selected list item needs to be saved.
    private boolean mUseTodayLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.list_view_fragment,container,false);

        // The ForecastAdapter will take data from a source and use it to populate the ListView it's attached to.
        weatherAdapter = new WeatherAdapter(getActivity(), null, 0);
        listView = (ListView) v.findViewById(R.id.weather_list);
        listView.setAdapter(weatherAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null && cursor.getCount() > 0) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    // call method from the MainActivity
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry
                                    .buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                }
                mPosition = position;
            }
        });

        if (!mUseTodayLayout) {
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setItemChecked(0, true);
                    Cursor cursor = (Cursor) weatherAdapter.getItem(0);
                    if (cursor != null && cursor.getCount() > 0) {
                        String locationSetting = Utility.getPreferredLocation(getActivity());
                        ((Callback) getActivity())
                                .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                        locationSetting, cursor.getLong(COL_WEATHER_DATE)
                                ));
                    }
                }
            });
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        weatherAdapter.setUseTodayLayout(mUseTodayLayout);
        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
        //content://com.codexpedia.app.watchweather/weather/New%20York?date=1460865600000
        Log.d("ForecastFragment", "onCreateLoader weatherForLocationUri>>>" + weatherForLocationUri);
        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        weatherAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        weatherAdapter.swapCursor(null);
    }

    // since we read the location when we create the loader, all we need to do is restart things
    public void onSettingChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        public void onItemSelected(Uri dateUri);
    }

    private void updateWeather() {
        WatchWeatherAdapterSync.syncImmediately(getActivity());
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (weatherAdapter != null) {
            weatherAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

}
