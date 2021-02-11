package com.example.AllForOne;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class FindShelter extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks
        , LocationListener
        , GoogleMap.OnMarkerClickListener{

    private GoogleMap mMap;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    private double latitude, longitude;
    private int ProximityRadius = 6000;
    private PlacesClient placesClient;

    public static final String EXTRA_NAME = "com.example.AllForOne.NAME";
    public static final String EXTRA_ADDRESS = "com.example.AllForOne.ADDRESS";
    public static final String EXTRA_PHONE = "com.example.AllForOne.PHONE";
    public static final String EXTRA_TIME = "com.example.AllForOne.TIME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Places.initialize(FindShelter.this, getResources().getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_shelter);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //Assign variable
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapShelter);
        supportMapFragment.getMapAsync(this);

        //Initialize fused location
        client = LocationServices.getFusedLocationProviderClient(this);

        //check permission
        if(ActivityCompat.checkSelfPermission(FindShelter.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //When permission granted call method
            getCurrentLocation();
        }else{
            //When permission denied, request permission
            ActivityCompat.requestPermissions(FindShelter.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    }

    GetNearbyPlaces getNearbyPlaces = new GetNearbyPlaces();
    private void getCurrentLocation() {
        //Initialize task location
        Task<Location> task = client.getLastLocation();

        String shelter = "shelter";
        Object transferData[] = new Object[2];

        //estaba aqui
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //When success
                if(location != null){
                    //Sync map
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            //Initialize lat lng
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            //Create marker options
                            MarkerOptions options = new MarkerOptions().position(latLng).title("Your Location");
                            //add marker on map
                            googleMap.addMarker(options);
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            String url = getUrl(latitude, longitude, shelter);
                            transferData[0] = mMap;
                            transferData[1] = url;
                            getNearbyPlaces.execute(transferData);
                            //Zoom map
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 44){
            if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                //When permission granted call method
                getCurrentLocation();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace) {
        StringBuilder googleURL = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googleURL.append("location=" + latitude + "," + longitude);
        //googleURL.append("location=" + "49.2827283" + "," + "-123.1207367");
        googleURL.append("&radius=" + ProximityRadius);
        googleURL.append("&type=" + nearbyPlace);
        googleURL.append("&sensor=true");
        googleURL.append("&key=" + getResources().getString(R.string.google_maps_key));

        Log.d("GoogleMapsActivity", "Url: " + googleURL.toString());
        return  googleURL.toString();
    }


    public boolean onMarkerClick(Marker marker) {
        String[][] markerPosition = getNearbyPlaces.getUniqueID();
        String reference = marker.getTitle().toString();
        if(reference.equals("Your Location")){
            Toast.makeText(this, "Your Location", Toast.LENGTH_SHORT).show();
        }else {
            String placeID = null;

            for (int i = 0; i < markerPosition.length; i++) {
                if (reference.equals(markerPosition[i][0])) {
                    placeID = markerPosition[i][1];
                    i = 200;
                }
            }

            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.OPENING_HOURS, Place.Field.PHONE_NUMBER);
            FetchPlaceRequest request = FetchPlaceRequest.builder(placeID, placeFields).build();
            placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                @Override
                public void onSuccess(FetchPlaceResponse response) {
                    Place place = response.getPlace();
                    String name = place.getName();
                    String address = place.getAddress();
                    String phone = place.getPhoneNumber();
                    List<String> time;

                    if(place.getOpeningHours() == null){
                        time = Arrays.asList("There are no information about business hours");
                    } else{
                        time = place.getOpeningHours().getWeekdayText();
                    }

                    Intent intent = new Intent(FindShelter.this, DisplayInfoActivity.class);
                    intent.putExtra(EXTRA_NAME, name);
                    intent.putExtra(EXTRA_ADDRESS, address);
                    intent.putExtra(EXTRA_PHONE, phone);
                    intent.putExtra(EXTRA_TIME, (Serializable) time);

                    Toast.makeText(FindShelter.this, "Loading information", Toast.LENGTH_SHORT).show();
                    startActivity(intent);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    if (exception instanceof ApiException) {
                        Toast.makeText(FindShelter.this, exception.getMessage() + "", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        return false;
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }
}