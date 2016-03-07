package com.codexpedia.app.watchweather.service;

import com.codexpedia.app.watchweather.model.openweathermap.Forecast;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by peng on 11/23/15.
 */
public interface WeatherService {
    //http://api.openweathermap.org/data/2.5/forecast/daily?q=London&mode=json&units=metric&cnt=7&appid=2de143494c0b295cca9337e1e96b00e0
    @GET("/daily?appid=7297d32c5741c2deab6484b5af45bb5d&format=json&mode=json&units=metric&cnt=7")
    public void getForecast(@Query("q") String query, Callback<Forecast> response);

}
