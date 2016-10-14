package com.pinkcabs.pinkcabs;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appdatasearch.GetRecentContextCall;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    TextView tvGeoCode;
    RequestQueue rq;
    private static final String TAG = "MapsActivity";
    public static final Integer PLACE_AUTOCOMPLETE_REQUEST_CODE=2209;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        rq= Volley.newRequestQueue(this);
        tvGeoCode= (TextView) findViewById(R.id.tv_geocode);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        final RouteMaker rm=new RouteMaker(this);

        try {
            final Intent placeAutoCompleteIntent=new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(this);
            tvGeoCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(placeAutoCompleteIntent,PLACE_AUTOCOMPLETE_REQUEST_CODE);
                }
            });
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        rm.setOnDirectionsReceivedListener(new RouteMaker.OnDirectionsReceivedListener() {
            @Override
            public void displayPolyline(List<LatLng> latLngList) {
                mMap.addPolyline(new PolylineOptions().addAll(latLngList));
            }

            @Override
            public void getDistanceString(String distance) {
                Log.d(TAG, "getDistanceString: distance="+distance);
            }

            @Override
            public void getDistanceValue(Double distance) {
                Log.d(TAG, "getDistanceValue: distance="+distance);
            }

            @Override
            public void getTimeString(String time) {
                Log.d(TAG, "getTimeString: time="+time);
            }

            @Override
            public void getTimeValue(Double time) {
                Log.d(TAG, "getTimeValue: time="+time);
            }
        });
        // Add a marker in Sydney and move the camera
        final LatLng sydney = new LatLng(-34, 151);
        LatLng xyz=new LatLng(-33,150);
        rm.findPath(sydney,xyz);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        mMap.setMyLocationEnabled(true);
        final Marker marker=mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney").draggable(true));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                                         @Override
                                         public void onCameraMove() {
                                             LatLng center=mMap.getCameraPosition().target;
                                             marker.setPosition(center);
                                             rq.add(reverseGeoCodeRequestBuilder(center));
                                             //rm.findPath(sydney,center);

                                         }
                                     }

        );

    }
    StringRequest reverseGeoCodeRequestBuilder(LatLng ll){
        String basic="https://maps.googleapis.com/maps/api/geocode/json?latlng=";
        String key="&key=AIzaSyDIVZ-j79nYVjEW0B99YiUG5zb5Jf_JVWc";
        final String requestString=basic+ll.latitude+","+ll.longitude+key;
        StringRequest stringRequest=new StringRequest(Request.Method.GET, requestString, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d(TAG, "onResponse: "+response);
                try {
                    JSONObject responseJSon =new JSONObject(response);
                    JSONArray results=responseJSon.getJSONArray("results");
                    JSONObject jobj=results.getJSONObject(0);
                    tvGeoCode.setText(jobj.getString("formatted_address"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: "+error.toString());
            }
        });
        return  stringRequest;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.d(TAG, "Place:" + place.toString());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.d(TAG, status.getStatusMessage());
            } else if (requestCode == RESULT_CANCELED) {

            }
        }
    }
}

//AIzaSyDIVZ-j79nYVjEW0B99YiUG5zb5Jf_JVWc   geocodind api key
//https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=YOUR_API_KEY

//    AIzaSyAa2CKARbz6bU6Nx6UJNVFG_2hwR3lVgkQ   directions api key


//AIzaSyDxdGZkc176riyJN8KfZENp5kNxHU1-Lw4  places api key