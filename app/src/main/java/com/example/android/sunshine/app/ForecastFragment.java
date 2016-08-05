package com.example.android.sunshine.app;

/**
 * Created by akiode.ko on 6/24/2016.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> adapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        adapter = new ArrayAdapter<>(
                getActivity(), // current context: this fragment's parent activity
                R.layout.list_item_forecast, // id of the listitem layout file
                R.id.list_item_forecast_textview, // id of the view rendering each list item
                new ArrayList<String>()); // the data being represented

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // retrieve list item data
                String data = parent.getItemAtPosition(position).toString();
                //// Display toast
                //Toast toast = Toast.makeText(view.getContext(), data, Toast.LENGTH_SHORT);
                //toast.show();
                // Launch detail activity
                Intent showDetail = new Intent(view.getContext(), DetailActivity.class);
                showDetail.putExtra(Intent.EXTRA_TEXT, data);
                startActivity(showDetail);
            }
        });


        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_launch_map) {
            //TODO: launch map app using location stored in preferences
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String weatherUnit;
    private void updateWeather() {
        // retrieve location from settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        weatherUnit = prefs.getString(getString(R.string.pref_unit_key),
                getString(R.string.pref_unit_default));
        new FetchWeatherTask().execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String format = "json";
            String units = "metric";
            int numDays = 5; // not used, the api returns 5 day data
            String appId = "de4c4afca110d19db289004bb7e2fbf7";

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast?q=benin,ng&units=metric&appid=de4c4afca110d19db289004bb7e2fbf7");
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt"; // not used, the api returns 5 day data
                final String KEY_PARAM = "appid";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(KEY_PARAM, appId)
                        .build();

                URL url = new URL(builtUri.toString());
                //Log.v(LOG_TAG, "Built URI" + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                //Log.v(LOG_TAG, forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // if something went wrong actually...
            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if (strings == null) { return; }
            adapter.clear();
            adapter.addAll(Arrays.asList(strings));
            //
        }

        /* Date/time conversion code. Will be moved out of AsyncTask later

         */
//        private String getReadableDateString(long time) {
//            // API returns a Unix timestamp (in seconds)
//        }

        private String formatHighLows(double high, double low) {
            // round to whole figures
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh + "°" + weatherUnit + "/" + roundedLow + "°" + weatherUnit;
            return highLowStr;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

            // Names of the JSON objects needing extraction
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_MAIN ="main";
            final String OWM_TEMP_MAX = "temp_max";
            final String OWM_TEMP_MIN = "temp_min";
            final String OWM_DESCRIPTION = "description";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            String[] resultStrs = new String[numDays];
            GregorianCalendar gc = new GregorianCalendar(); // with current date/time
            // The api actually returns forecasts for 3hr periods, i.e. 8 forecasts per day. So
            // That means we pick successive forecasts by incrementing the counter 8 steps each time
            int cnt = 0;

            for (int i=0; i < weatherArray.length(); i+=8) {
                // Use format "Day, description, high/low"
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                Date time = gc.getTime();
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                day = shortenedDateFormat.format(time);
                // advance the GregorianCalendar to the next date, then loop
                gc.add(GregorianCalendar.DATE, 1);

                // description is in a child array called 'weather', having just one element
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called 'main' (NOT 'temp' anymore)
                JSONObject mainObject = dayForecast.getJSONObject(OWM_MAIN);
                double high = mainObject.getDouble(OWM_TEMP_MAX);
                double low = mainObject.getDouble(OWM_TEMP_MIN);
                if (weatherUnit.equals("F")) {
                    high = high * 9.0/5 + 32;
                    low = low * 9.0/5 + 32;
                }

                highAndLow = formatHighLows(high,low);
                resultStrs[cnt] = day + " - " + description + " - " + highAndLow;
                cnt++;
            }

//            for (String s: resultStrs) {
//                Log.v(LOG_TAG, "Forecast entry: " + s);
//            }

            return resultStrs;
        }
    }
}
