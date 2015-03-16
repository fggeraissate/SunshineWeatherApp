package com.example.android.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    private final  String LOG_TAG = MainActivity.class.getSimpleName();

    // -- Lifecycle --

    // onCreate (created) - you build and wire up your UI. Once that's done, your activity is created.
    // onStart (visible) - when the activity becomes visible. <---------------------------|
    // onResume (visible) - when it gets focus and becomes the active foreground app.     |
    //                                                                                    |
    // onPause (partially visible) - when the activity lost focus                         |
    // onStop (hidden)- when the app is no longer visible. -----------------> onRestart --|
    // onDestroy (destroyed) - indicates the end of the app cycle

    // Maintaining State (*)
    // Active -> *onSaveInstanceState -> onPause -> Terminated ----|
    //                     *onRestoreInstanceStage <- onCreate <---|

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.v(LOG_TAG, "in onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // We inflate the layout and associate it with the Activity
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }

//    @Override
//    protected void onStart() {
//        Log.v(LOG_TAG, "in onStart");
//        super.onStart();
//        // The activity is about to become visible.
//    }
//
//    @Override
//    protected void onResume() {
//        Log.v(LOG_TAG, "in onResume");
//        super.onResume();
//        // The activity has become visible (it is now "resumed").
//    }
//
//    @Override
//    protected void onPause() {
//        Log.v(LOG_TAG, "in onPause");
//        super.onPause();
//        // Another activity is taking focus (this activity is about to be "paused").
//    }
//
//    @Override
//    protected void onStop() {
//        Log.v(LOG_TAG, "in onStop");
//        super.onStop();
//        // The activity is no longer visible (it is now "stopped")
//    }
//
//    @Override
//    protected void onDestroy() {
//        Log.v(LOG_TAG, "in onDestroy");
//        super.onDestroy();
//        // The activity is about to be destroyed.
//    }

    // -- End Lifecycle --

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String stringLocation = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        // Using the URI scheme for showing a location found on a map. This super-handy intent is
        // detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        Uri uriGeoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", stringLocation)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uriGeoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);

        } else {
            Log.d(LOG_TAG, "Couldn't call " + stringLocation + ", no receiving apps installed!");
        }
    }
}