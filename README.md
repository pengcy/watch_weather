#Welcome to the watch_weather

###Forked from 
https://github.com/udacity/Sunshine-Version-2

###Weather Data
Data are saved in sqlite db, synced from this weather api every 3 hours.
http://api.openweathermap.org/data/2.5/forecast/daily?q=boston&mode=json&units=metric&cnt=7&appid=7297d32c5741c2deab6484b5af45bb5d

###Three screens
* ForecastFragment.java
* DetailFragment.java
* SettingsActivity.java

<img src="https://cloud.githubusercontent.com/assets/5489943/13558410/4e46cbd4-e3d2-11e5-8533-411fde33bee4.png" width="250" height="420" />
<img src="https://cloud.githubusercontent.com/assets/5489943/13558409/4e421b16-e3d2-11e5-8e05-d78c99260c04.png" width="250" height="420" />
<img src="https://cloud.githubusercontent.com/assets/5489943/13558411/4e49b43e-e3d2-11e5-981a-0ae84d5e8731.png" width="250" height="420" />

###Topics Covered
* retrofit for downloading the weather data
* sqlite for storing the weather data
* Service and AbstractThreadedSyncAdapter for data sync
* ContentProvider for retrieving data from DB
* And others...
