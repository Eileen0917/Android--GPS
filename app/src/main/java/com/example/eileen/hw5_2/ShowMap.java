package com.example.eileen.hw5_2;

import android.app.Activity;
import android.app.DownloadManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by eileen on 2015/12/15.
 */
public class ShowMap extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,OnMapReadyCallback {

    MapView mapView;
    final int MIN_TIME=1*1000;
    GoogleMap map;
    Location currLoc;
    double lat,lon;
    GoogleApiClient mGoogleApiClient;

    public static ShowMap newInstance(double lat, double lon){
        ShowMap myShowMap=new ShowMap();
        Bundle args=new Bundle();

        args.putDouble("lat", lat);
        args.putDouble("lon", lon);
        myShowMap.setArguments(args);
        return myShowMap;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.map, container,false);
        mapView=(MapView) v.findViewById(R.id.mvMap);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        lat=getArguments().getDouble("lat");
        lon=getArguments().getDouble("lon");
    }

    @Override
    public void onResume(){
        mapView.onResume();
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void onConnected(Bundle bundle) {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(MIN_TIME);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,  this);
    }

    public void onConnectionSuspended(int i){

    }

    public void onLocationChanged(Location location){
        currLoc=location;
        lat=currLoc.getLatitude();
        lon=currLoc.getLongitude();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lon),14.0f));
        map.addMarker(new MarkerOptions()
                        .position(new LatLng(lat,lon))
                        .title("you are here!"));
        ((MainActivity) this.getActivity()).checkLocation(currLoc);
    }


    public void onConnectionFailed(ConnectionResult connectionResult){

    }

    public void pan2Home(double lat, double lon){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lon),16.0f));
        map.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.red_point))
                .title("Home"));
    }

    void updatePlaces(){
        new GetPlaces().execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map=googleMap;
        map.setMyLocationEnabled(true);
        MapsInitializer.initialize(ShowMap.this.getActivity());
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 14.0f));
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            public boolean onMyLocationButtonClick() {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 16.0f));
                return true;
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener(){

            @Override
            public boolean onMarkerClick(Marker marker){
                if(marker.getTitle().contains("here"))
                    Toast.makeText(getActivity().getApplicationContext(),"you are pressing the here maker",Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    }

    class GetPlaces extends AsyncTask{
        String placesRequestStr= "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location="+lat+","+lon+
                "&radius=2000"+
                "&types=food"+
                "&language=zh-tw"+
                "&key=AIzaSyDIayVaiVfytKlK6oXTmOYBqkM-a-eRSzQ";
        private final int MAX_PLACES=20;
        private Marker[] placeMarkers;
        boolean NODATA=false;
        String result=null;

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            placeMarkers=new Marker[MAX_PLACES];
            if(placeMarkers!=null){
                for(int pm=0;pm<placeMarkers.length;pm++){
                    if(placeMarkers[pm]!=null)
                        placeMarkers[pm].remove();
                }
            }
            if(!NODATA){
                try{
                    JSONArray places=new JSONObject(result).getJSONArray("results");
                    MarkerOptions[] aPlaceMarkerOpt=new MarkerOptions[places.length()];
                    for(int p=0;p<places.length();p++){
                        JSONObject aPlace=places.getJSONObject(p);
                        String placeAdd=aPlace.getString("vicinity");
                        String placeName=aPlace.getString("name");
                        JSONObject loc=aPlace.getJSONObject("geometry").getJSONObject("location");
                        LatLng placeLL=new LatLng(Double.valueOf(loc.getString("lat")),
                                Double.valueOf(loc.getString("lng")));
                        aPlaceMarkerOpt[p]=new MarkerOptions()
                                .position(placeLL)
                                .title(placeName)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_point))
                                .snippet(placeAdd);
                        placeMarkers[p]=map.addMarker(aPlaceMarkerOpt[p]);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }



        @Override
        protected Object doInBackground(Object[] objects){
            try{
                OkHttpClient mHttpClient=new OkHttpClient();
                Request request=new Request.Builder()
                        .url(placesRequestStr)
                        .build();
                Response response=mHttpClient.newCall(request).execute();

                if(response.isSuccessful()){
                    result=response.body().string();
                }
                NODATA=false;

            } catch (IOException e){
                NODATA=true;
            }
            return null;
        }
    }
}
