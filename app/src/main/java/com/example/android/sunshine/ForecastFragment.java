package com.example.android.sunshine;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> adapterForecast;

    public ForecastFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] arrayData = {  "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"};

        List<String> listWeekForecast = new ArrayList<String>(Arrays.asList(arrayData));

        // Now that we have some dummy forecast data, create an ArrayAdapter.
        // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.
        adapterForecast = new ArrayAdapter<String>(
                getActivity(), // the current context (this activity)
                R.layout.list_item_forecast, // The name of the layout id
                R.id.list_item_forecast_textview, // The ID of the textview to populate
                listWeekForecast);

        // To turn an xml layout into java view objects, we need to inflate the layout
        // In our Fragment classes we inflate the layout, which includes a LayoutInflater as a parameter:
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listview = (ListView)rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(adapterForecast);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<Void, Void, Void> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected Void doInBackground(Void... params) {

            // These two need to be declared outside the try/catch so that they can be closed in the final block.
            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;

            // Will contain the raw JSON response as a string
            String stringForecastJSON = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaliable at OWM' forecast api page, at http://openweathermap.org/API#forecast
                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");

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
                stringForecastJSON = stringBuffer.toString();

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

            return null;
        }
    }

}