package com.jeffwritescode.Relearning.learningandroidhardcore.app;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;


public class MainActivity extends Activity implements LocationListener, Button.OnClickListener {

    private static String nearbyPlacesURL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=AIzaSyBfHe0bIaE1sk4OrL9K3zvAGkwPMo97u84";

    private LocationManager locationManager;
    private Location location;
    private String provider;
    private GoogleMap map;
    boolean locationFound;

    TextView longitude;
    TextView latitude;
    EditText locationSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);

        setContentView(R.layout.activity_main);

        Button searchButton = (Button)findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);

        longitude = (TextView)findViewById(R.id.longitude);
        latitude = (TextView)findViewById(R.id.latitude);
        locationSearch = (EditText)findViewById(R.id.location_edittext);

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();

        if (location != null) {
            onLocationChanged(location);
        } else {
            longitude.setText("No location");
            latitude.setText("No location");
        }


        locationSearch.setOnKeyListener(new TextView.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.FLAG_EDITOR_ACTION) {
                    Log.d("Editor Action", "Clicked search button");
                }
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onLocationChanged(Location location) {
        latitude.setText(location.getLatitude() + "");
        longitude.setText(location.getLongitude() + "");

        this.location = location;

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (!locationFound) {
            locationFound = true;

            map.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
            map.animateCamera(CameraUpdateFactory.zoomTo(50), 2000, null);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onClick(View view) {
        findNearbyPlaces(this.location, locationSearch.getText().toString());
    }

    private void findNearbyPlaces(Location location, String keyword) {
        String latlng = location.getLatitude() + "," + location.getLongitude();

        location = locationManager.getLastKnownLocation(provider);

        try {
            keyword = URLEncoder.encode(keyword, "UTF-8");
            nearbyPlacesURL += "&location=" + latlng + "&radius=150" + "&sensor=true" + "&keyword=" + keyword;
        } catch (Exception e) {
            Log.d("URL Exception", e.getMessage());
        }

        AsyncHttpClient httpClient = new AsyncHttpClient();

        setProgressBarIndeterminateVisibility(true);

        httpClient.get(nearbyPlacesURL, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray resultsArray = response.getJSONArray("results");
                    addMarkers(resultsArray);
                    setProgressBarIndeterminateVisibility(false);
                } catch (JSONException e) {
                    Log.d("JSON Exception", e.getMessage());
                    setProgressBarIndeterminateVisibility(false);
                }
            }

            @Override
            public void onFailure(Throwable e, JSONObject errorResponse) {
                Log.d("Failure", errorResponse.toString());
            }
        });
    }

    private void addMarkers(JSONArray locations) {
        int length = locations.length();

        for (int i = 0; i < length; i++) {
            try {
                JSONObject firstResult = locations.getJSONObject(i);

                String locationName = firstResult.optString("name");

                JSONObject location = firstResult.optJSONObject("geometry").optJSONObject("location");

                Double latitude = location.optDouble("lat");
                Double longitude = location.optDouble("lng");

                LatLng currentLocation = new LatLng(latitude, longitude);

                String snippetInfo = "No info";
                if (firstResult.has("opening_hours")) {
                    String openingHours = "Open Now : " + firstResult.optJSONObject("opening_hours").optBoolean("open_now");
                    snippetInfo = openingHours;
                }

                if (firstResult.has("rating")) {
                    String rating = "Rating : " + firstResult.optDouble("rating");
                    snippetInfo += "\n" + rating;
                }

                map.addMarker(new MarkerOptions()
                        .position(currentLocation)
                        .title(locationName))
                        .setSnippet(snippetInfo);


            } catch (JSONException e) {
                Log.d("JSON Exception", e.getMessage());
            }
        }



    }
}

