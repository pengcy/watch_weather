package com.codexpedia.app.watchweather.data.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.codexpedia.app.watchweather.MainActivity;
import com.codexpedia.app.watchweather.R;
import com.codexpedia.app.watchweather.data.WeatherContract;
import com.codexpedia.app.watchweather.model.openweathermap.City;
import com.codexpedia.app.watchweather.model.openweathermap.Coord;
import com.codexpedia.app.watchweather.model.openweathermap.Forecast;
import com.codexpedia.app.watchweather.model.openweathermap.Temp;
import com.codexpedia.app.watchweather.model.openweathermap.Weather;
import com.codexpedia.app.watchweather.model.openweathermap.WeatherItem;
import com.codexpedia.app.watchweather.service.WeatherService;
import com.codexpedia.app.watchweather.util.Constants;
import com.codexpedia.app.watchweather.util.Utility;

import java.util.List;
import java.util.Vector;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class WatchWeatherAdapterSync extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG                         = WatchWeatherAdapterSync.class.getSimpleName();
    public static final int SYNC_INTERVAL               = 60 * 180;       // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_FLEXTIME               = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS             = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID    = 3004;


    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID   = 0;
    private static final int INDEX_MAX_TEMP     = 1;
    private static final int INDEX_MIN_TEMP     = 2;
    private static final int INDEX_SHORT_DESC   = 3;

    public WatchWeatherAdapterSync(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        // Getting movie from REST endpoint
        RestAdapter rest = new RestAdapter.Builder()
                .setEndpoint(Constants.WEATHER_API_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();
        final WeatherService weatherService = rest.create(WeatherService.class);
        final String location = Utility.getPreferredLocation(getContext());
        String query = Utility.capitalizeEveryFristLetter(location);

        weatherService.getForecast(query, new Callback<Forecast>() {
            @Override
            public void success(Forecast forecast, Response response) {
                List<WeatherItem> weatherList = (List<WeatherItem>) forecast.getList();
                saveWeatherData(forecast, location);
            }
            @Override
            public void failure(RetrofitError error) {
            }
        });
        return;
    }


    private void saveWeatherData(Forecast forecast, String locationSetting) {
            List<WeatherItem> weatherItems  = forecast.getList();
            City city                       = forecast.getCity();
            String cityName                 = city.getName();
            Coord cityCoord                 = city.getCoord();
            double cityLatitude             = cityCoord.getLat();
            double cityLongitude            = cityCoord.getLon();
            long locationId                 = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherItems.size());

            // OWM returns daily forecasts based upon the local time of the city that is being asked for, which means that we need to know the GMT offset to translate this data properly. Since this data is also sent in-order and the first day is always the current day, we're going to take advantage of that to get a nice normalized UTC date for all of our weather.
            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time(); // now we work exclusively in UTC


            for(int i = 0; i < weatherItems.size(); i++) {
                // These are the values that will be collected.
                long dateTime                   = dayTime.setJulianDay(julianStartDay + i);    // Cheating to convert this to UTC time, which is what we want anyhow
                WeatherItem dayForecast         = weatherItems.get(i); // Get the JSON object representing the day

                double pressure                 = dayForecast.getPressure();
                int humidity                    = dayForecast.getHumidity();
                double windSpeed                = dayForecast.getSpeed();
                double windDirection            = dayForecast.getDeg();

                Weather weather                 = dayForecast.getWeather().get(0);
                String description              = weather.getDescription();
                int weatherId                   = weather.getId();

                Temp temperature                = dayForecast.getTemp();
                double high                     = temperature.getMax();
                double low                      = temperature.getMin();

                ContentValues weatherValues = new ContentValues();
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY,      locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE,         dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY,     humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE,     pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,   windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES,      windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,     high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,     low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,   description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,   weatherId);

                cVVector.add(weatherValues);
            }

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);

                // delete old data so we don't build up an endless history
                getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                        WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?",
                        new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});

                notifyWeather();
            }

            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");

    }

    private void notifyWeather() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs         = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey  = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications    = prefs.getBoolean(displayNotificationsKey, Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {
            String lastNotificationKey  = context.getString(R.string.pref_last_notification);
            long lastSync               = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                String locationQuery    = Utility.getPreferredLocation(context); // Last sync was more than 1 day ago, let's send a notification with the weather.
                Uri weatherUri          = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
                Cursor cursor           = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    int weatherId       = cursor.getInt(INDEX_WEATHER_ID);
                    double high         = cursor.getDouble(INDEX_MAX_TEMP);
                    double low          = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc         = cursor.getString(INDEX_SHORT_DESC);
                    int iconId          = Utility.getIconResourceForWeatherCondition(weatherId);
                    Resources resources = context.getResources();
                    Bitmap largeIcon    = BitmapFactory.decodeResource(resources, Utility.getArtResourceForWeatherCondition(weatherId));
                    String title        = context.getString(R.string.app_name);
                    String contentText  = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context, high),
                            Utility.formatTemperature(context, low));

                    // NotificationCompatBuilder is a very convenient way to build backward-compatible notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setColor(resources.getColor(R.color.colorLightBlue))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    // In this case, opening the app when the user clicks on the notification.
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the started Activity.
                    // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build()); // WEATHER_NOTIFICATION_ID allows you to update the notification later on.

                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
                cursor.close();
            }
        }
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = getContext().getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId          = locationCursor.getLong(locationIdIndex);
        } else {
            // Inserting rows of data is pretty simple.
            ContentValues locationValues = new ContentValues();
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            Uri insertedUri = getContext().getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            locationId = ContentUris.parseId(insertedUri); // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
        }
        locationCursor.close();

        return locationId;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context), context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE); // Get an instance of the Android account manager
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type)); // Create the account type and default account

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                Log.e("WatchWeatherAdapterSync",  "Failed to create new account.");
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        WatchWeatherAdapterSync.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}