package com.colossus.teletaxiapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private Button bLogout;
    private Button bRequest;

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    LatLng pickupLocation;

    // Finding driver variables
    private int radius = 20;
    private Boolean driverFound =  false;
    private String driverFoundId;
    private Boolean requestBol = false;
    private Marker pickupMarker;

    // getting driver variables
    GeoQuery geoQuery;
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bLogout = findViewById(R.id.logout);
        bLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, DriverLoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        bRequest = findViewById(R.id.request);
        bRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (requestBol) {
                    requestBol = false;
                    // Remove all listeners
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    // Remove custumer Id from Drivers
                    if (driverFoundId != null) {
                        DatabaseReference driverRef =
                                FirebaseDatabase.getInstance().getReference().child("User").child("Driver").child(driverFoundId);
                        driverRef.setValue(true);
                        driverFoundId = null;
                    }

                    // set varibles to default
                    driverFound = false;
                    radius = 1;

                    // remove Cuturmer Request
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customersRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    // Remove Markerfrom the Map
                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }

                    bRequest.setText("Llamar Taxi");

                } else {
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customersRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    pickupLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Recojeme aqui"));

                    bRequest.setText("Buscando un vehiculo");

                    getClosestDrivers();
                }
            }
        });
    }

    public void getClosestDrivers() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire =  new GeoFire(driverLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundId = key;

                    DatabaseReference driverRef =
                            FirebaseDatabase.getInstance().getReference().child("User").child("Driver").child(driverFoundId);
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId", customerId);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    bRequest.setText("Obteniendo ubicacion de Taxi");

                 }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverLocation() {
        driverLocationRef =
                FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }

                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLongitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(pickupLocation.latitude);
                    loc2.setLongitude(pickupLocation.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance < 100) {
                        bRequest.setText("Taxi ha llegado");
                    } else {
                        bRequest.setText("Taxi encontrado " + String.valueOf(distance));
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Tu taxi"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        //LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
