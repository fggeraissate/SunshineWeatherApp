package com.example.android.sunshine;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> arrayAdapterForecast;

    public ForecastFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add this line in order for this fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will automatically handle clicks on
        // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and use it to populate the ListView it's
        // attached to.
        arrayAdapterForecast = new ArrayAdapter<String>(
                getActivity(), // the current context (this activity)
                R.layout.list_item_forecast, // The name of the layout id
                R.id.list_item_forecast_textview, // The ID of the textview to populate
                new ArrayList<String>());

        // To turn an xml layout into java view objects, we need to inflate the layout
        // In our Fragment classes we inflate the layout, which includes a LayoutInflater as a parameter:
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listview = (ListView)rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(arrayAdapterForecast);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                String stringForecast = arrayAdapterForecast.getItem(position);

                // Toast.makeText(getActivity(), stringForecast, Toast.LENGTH_SHORT).show(); // Adding a toast when the user touches a listview item

                // Launch a new Activity called DetailActivity by initiating an explicit intent
                Intent intent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, stringForecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateWeather() {

        //Get location from shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String stringLocation = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        // Request a forecast for a given zip code.
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute(stringLocation);
        // weatherTask.execute("94043");
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    // AsyncTask Methods:
    //     onPreExecute     - This method is run on the UI before the task starts and is responsible for any setup that needs to be done.
    //     doInBackground   - This is the code for the actual task you want done off the main thread. It will be run on a background thread and not disrupt the UI.
    //     onProgressUpdate - This is a method that is run on the UI thread and is meant for showing the progress of a task, such as animating a loading bar.
    //     onPostExecute    - This is a method that is run on the UI after the task is finished.
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        // The date/time conversion code is going to be moved outside the asynctask later,
        // so for convenience we're breaking it out into own method now.
        private String getReadableDateString(long time) {

            // Because the API returns a unix timestap (measured in seconds), it must be converted
            // to miliseconds in order to be converted to valid date.
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd");

            return simpleDateFormat.format(time);
        }

        // Prepare the weather high/lows for presentation
        private String formatHighLows(double high, double low) {

            // For presentation, assume the user doesn't care about tenths of a degree.
            long longRoundedHigh = Math.round(high);
            long longRoundedLow = Math.round(low);

            String stringHighLow = longRoundedHigh + "/" + longRoundedLow;

            return stringHighLow;
        }

        // Take the String representing the complete forecast in JSON Format and pull out the data
        // we need to construct the Strings needed for the wireframes.
        //
        // Fortunately parsing is easy: constructor takes the JSON string and converts it into an
        // Object hierarchy for us.
        private String[] getWeatherDataFromJson(String stringJsonForecast, int intNumDays) throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject jsonForecast = new JSONObject(stringJsonForecast);
            JSONArray jsonArrayWeather = jsonForecast.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being asked
            // for, which means that we need to know the GMT offset to translate this data properly.
            //
            // Since this data is also sent in-order and the first day is always the current day,
            // we're going to take advantage of that to get a nice normalized UTC date for all of
            // our weather.
            Time timeDay = new Time();
            timeDay.setToNow();

            // We start at the day returned by local time. Otherwise this is a mess.
            int intJulianStartDay = Time.getJulianDay(System.currentTimeMillis(), timeDay.gmtoff);

            // Now we work exclusively in UTC
            timeDay = new Time();

            String[] arrayResults = new String[intNumDays];
            for (int i = 0; i < jsonArrayWeather.length(); i++) {

                // For now, using the format "Day, description, high/low"
                String stringDay;
                String stringDescription;
                String stringHighAndLow;

                // Get the JSON object representing the day
                JSONObject jsonForecastDay = jsonArrayWeather.getJSONObject(i);

                // The date/time is returned as a long. We need to convert that into something more
                // human-readable, since most people won' read "400356800" as "this saturday".
                long longTimeDate;
                // Cheating to convert this to UTC time, which is what we eant anyhow
                longTimeDate = timeDay.setJulianDay(intJulianStartDay+i);
                stringDay = getReadableDateString(longTimeDate);

                // Description is in a child array called "weather", which is 1 element long.
                JSONObject jsonWeather = jsonForecastDay.getJSONArray(OWM_WEATHER).getJSONObject(0);
                stringDescription = jsonWeather.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp". Try not to name variables "temp"
                // when working with temperature. It confuses everybody.
                JSONObject jsonTemperature = jsonForecastDay.getJSONObject(OWM_TEMPERATURE);
                double doubleHigh = jsonTemperature.getDouble(OWM_MAX);
                double doubleLow = jsonTemperature.getDouble(OWM_MIN);

                stringHighAndLow = formatHighLows(doubleHigh, doubleLow);
                arrayResults[i] = stringDay + " - " + stringDescription + " - " + stringHighAndLow;
            }

            // for (String string : arrayResults) {
            //     Log.v(LOG_TAG, "Forecast entry: " + string);
            // }

            return  arrayResults;
        }

        @Override
        protected String[] doInBackground(String... params) {

            // If there' no zip code, there's nothing to look up. Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch so that they can be closed in the final block.
            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;

            // Will contain the raw JSON response as a string
            String stringJsonForecast = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaliable at OWM's forecast api page, at http://openweathermap.org/API#forecast
                final String BASE_URL_FORECAST = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String PARAM_QUERY = "q";
                final String PARAM_FORMAT = "mode";
                final String PARAM_UNITS = "units";
                final String PARAM_DAYS = "cnt";

                Uri builtUri = Uri.parse(BASE_URL_FORECAST).buildUpon()
                        .appendQueryParameter(PARAM_QUERY, params[0])
                        .appendQueryParameter(PARAM_FORMAT, format)
                        .appendQueryParameter(PARAM_UNITS, units)
                        .appendQueryParameter(PARAM_DAYS, Integer.toString(numDays))
                        .build();

                URL url = new URL(builtUri.toString());

                // Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to OpenWeatherMap and open the connection
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer stringBuffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String stringLine;
                while ((stringLine = bufferedReader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing),
                    // but it does make debbuging a *lot* easier if you print out the completed buffer for debugging
                    stringBuffer.append(stringLine + "\n");
                }

                if (stringBuffer.length() == 0) {
                    // Stream was empty. No point in parsing
                    return  null;
                }
                stringJsonForecast = stringBuffer.toString();

                // Verified that the data returned is correct
                // Log.v(LOG_TAG, "String Forecast JSON: " + stringJsonForecast);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                // If the code didn't successfully get the weather data, there' no point in attemping to parse it
                return null;

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(stringJsonForecast, numDays);

            } catch (JSONException e) {
                Log.e (LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                arrayAdapterForecast.clear();
                for (String stringForecastDay : result) {
                    arrayAdapterForecast.add(stringForecastDay); // Note the "add" method already calls "notifyDataSetChanged" internally
                }
                // New data is back from the server!
            }
        }
    }
}
