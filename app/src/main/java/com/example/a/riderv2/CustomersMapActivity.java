package com.example.a.riderv2;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        RoutingListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private Button customerLogoutButton;
    private Button requestRideButton;

    private String customerID;
    private String driverFoundID;

    private LatLng customerPickUpLocation;

    private int radius = 1;

    private Boolean driverFound = false;
    private Boolean requestBol = false;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private Marker driverMarker;
    private Marker pickupMarker;

    GeoQuery geoquery;

    SupportMapFragment mapFragment;

    private DatabaseReference customerDatabaseRef;
    private DatabaseReference driverAvailableRef;
    private DatabaseReference driverRef;
    private DatabaseReference driverLocationRef;

    private ValueEventListener driverLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        mAuth       = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID  = FirebaseAuth.getInstance().getCurrentUser().getUid();

        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Request");
        driverAvailableRef  = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        driverLocationRef   = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        customerLogoutButton  = (Button)findViewById(R.id.customer_logout_btn);

        requestRideButton     = (Button)findViewById(R.id.customers_request_ride_btn);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        else{
            mapFragment.getMapAsync(this);
        }

        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                customerLogout();
            }
        });

        requestRideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBol){
                    requestBol = false;
                    if(geoquery!=null){
                        geoquery.removeAllListeners();
                    }
                    if(driverFoundID != null){
                        if(driverLocationRefListener != null){
                            driverLocationRef.removeEventListener(driverLocationRefListener);
                        }
                        driverRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID);
                        driverRef.setValue(true);
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Customer Request");
                    GeoFire geofire = new GeoFire(ref);
                    geofire.removeLocation(userID);

                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }
                    if(driverMarker != null){
                        driverMarker.remove();
                    }
                    requestRideButton.setText("Request a Ride");
                }
                else{
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Customer Request");
                    GeoFire geofire = new GeoFire(ref);
                    geofire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));


                    customerPickUpLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    //add a marker on the customer position and display a message
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("Pick up at here"));


                    //change the text of the button
                    //requestRideButton.setText("Getting your driver");

                    //a Method which search closest driver around the customer
                    getClosestDriver();
                }
            }
        });
    }

    private void getClosestDriver() {
        GeoFire geofire = new GeoFire(driverAvailableRef);
        geoquery = geofire.queryAtLocation(new GeoLocation(customerPickUpLocation.latitude,customerPickUpLocation.longitude),radius);
        geoquery.removeAllListeners();

        geoquery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            //will be call whenever the driver is available
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol){
                    driverFound = true;
                    driverFoundID = key;

                    driverRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    driverRef.updateChildren(driverMap);
                    //show customer driver's location
                    gettingDriverLocation();
                    requestRideButton.setText("Looking for Driver's location (Click here to cancel)");

                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            //when the driver is not available, will be keep searching driver available
            public void onGeoQueryReady() {
                if(driverFound){
                    radius = radius + 10;
                    getClosestDriver();
                }
                else{
                    driverFound = false;
                    radius = 1;
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Customer Request");
                    GeoFire geofire = new GeoFire(ref);
                    geofire.removeLocation(userID);
                    requestRideButton.setText("No Driver Found");
                    //erasePolyLines();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

    private void gettingDriverLocation() {
        //this is to retrieve the driver's location(online driver only)
        driverLocationRefListener = driverLocationRef.child(driverFoundID).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //with help of dataSnapShot, we will get the latitude and Longitude
                if(dataSnapshot.exists() && requestBol){
                    List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                    //All data stored in Firebase Database is in String, so we need to convert it into
                    //a double data type as to calculate the distance
                    double locationLat = 0;
                    double locationLng = 0;
                    requestRideButton.setText("Driver Found");

                    if(driverLocationMap.get(0)!= null){
                        locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                    }
                    if(driverLocationMap.get(0)!= null){
                        locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                    }

                    //add marker to driver position
                    LatLng driverLATLNG = new LatLng(locationLat,locationLng);
                    if(driverMarker != null){
                        driverMarker.remove();
                    }

                    Location location1 = new Location("");
                    location1.setLatitude(customerPickUpLocation.latitude);
                    location1.setLongitude(customerPickUpLocation.longitude);

                    //driver location
                    Location location2 = new Location("");
                    location2.setLatitude(driverLATLNG.latitude);
                    location2.setLongitude(driverLATLNG.longitude);

                    //display the define the distance of customer and driver using firebase built in finction
                    float distance = location1.distanceTo(location2);

                    //notice the user if the driver arrived
                    if(distance< 50){
                        requestRideButton.setText("Driver is here.");
                    }
                    else{
                        requestRideButton.setText("Driver found "+String.valueOf(distance)+ " away from you");
                    }

                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLATLNG).title("Your Driver is here"));
                    //getRouteToMarker(driverLATLNG);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void getRouteToMarker(LatLng pickUpLATLNG) {
        Routing routing = new Routing.Builder()
                .key("AIzaSyBUOgq1qbyr4eY7r04z9VJUtvM20OfNa1c")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), pickUpLATLNG)
                .build();
        routing.execute();

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
            ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }

        buildGoogleAPIClient();
        mMap.setMyLocationEnabled(true);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }



    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }



    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }


    protected synchronized void buildGoogleAPIClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case LOCATION_REQUEST_CODE:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }
                else{
                    Toast.makeText(getApplicationContext(),"Please provide the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    private void customerLogout() {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        //this will send the user to welcome activity
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();

    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolyLines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
