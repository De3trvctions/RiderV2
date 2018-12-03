package com.example.a.riderv2;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.provider.ContactsContract;
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
import java.util.List;
import java.util.Map;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        RoutingListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private LatLng DriverLocation;

    private Button driverLogoutButton;


    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private Boolean currentLogoutDriverStatus = false;

    private DatabaseReference assignedCustomerRef;
    private DatabaseReference assignedCustomerPickUpRef;
    private DatabaseReference DriverLocationRef;
    SupportMapFragment mapFragment;
    private String driverID;
    private String customerID = "";
    private String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);


        polylines = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();

        driverLogoutButton = (Button)findViewById(R.id.driver_logout_btn);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriversMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        else{
            mapFragment.getMapAsync(this);
        }

        // when login button is click, logout the driver and remove it from the firebase availablity
        driverLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogoutDriverStatus = true;
                disconnectTheDriver();
                mAuth.signOut();
                DriverLogout();
                return;
            }
        });

        GetAssignedCustomerRequest();
    }


    //we are receiving customer ride ID from customerMapActivity,
    // then by using addValueEvenListener we are going to retrieve customer id
    private void GetAssignedCustomerRequest() {
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    DriverLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    customerID = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickUpLocation(); // ori code
                }
                else{
                    customerID = "";
                    if(pickUpMarker != null){
                        pickUpMarker.remove();
                        //if(driverMarker != null){
                        //    driverMarker.remove();
                        //}
                    }
                    if(assignedCustomerPickUpRefListener != null){
                        assignedCustomerPickUpRef.removeEventListener(assignedCustomerPickUpRefListener);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }
    Marker pickUpMarker;
    Marker driverMarker;
    private ValueEventListener assignedCustomerPickUpRefListener;
    private ValueEventListener DriverLocationRefListener;

    private void getAssignedCustomerPickUpLocation() {
        assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference()
                .child("Customer Request").child(customerID).child("l");
        assignedCustomerPickUpRefListener = assignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (customerLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if (customerLocationMap.get(0) != null) {
                        locationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }
                    //add marker to customer position
                    LatLng customerLATLNG = new LatLng(locationLat, locationLng);
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(customerLATLNG).title("Your Customer is here"));
                    //getRouteToMarker(customerLATLNG);

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


                    /*
                    DriverLocationRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(userId).child("l");
                    DriverLocationRefListener = DriverLocationRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double locationLat = 0;
                            double locationLng = 0;

                            if (driverLocationMap.get(0) != null) {
                                locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if (driverLocationMap.get(0) != null) {
                                locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }
                            //add marker to customer position
                            LatLng driverLatLng = new LatLng(locationLat, locationLng);
                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("I'm here"));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });



*/


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


        /*
        assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference()
                .child("Customer Requests").child(customerID).child("l");
        assignedCustomerPickUpRefListener = assignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && customerID.equals("")){
                    List<Object> customerLocationMap = (List<Object>)dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(customerLocationMap.get(0)!= null){
                        locationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if(customerLocationMap.get(0)!= null){
                        locationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    LatLng customerPickUpLocation = new LatLng(locationLat,locationLng);

                    LatLng pickUpLATLNG = new LatLng(locationLat,locationLng);
                    pickUpMarker = mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("Pick Up customer Location"));
                    getRouteToMarker(pickUpLATLNG);
                }
                else{
                    erasePolyLines();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        }); */
    }


    private void getRouteToMarker(LatLng pickUpLATLNG) {
        Routing routing = new Routing.Builder()
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
//changed!!!
        buildGoogleAPIClient();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriversMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriversMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
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
        if(getApplicationContext() != null){
            lastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
            GeoFire geoFireAvailability = new GeoFire(DriverAvailabilityRef);

            DatabaseReference driverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            GeoFire geoFireWorking = new GeoFire(driverWorkingRef);

            //String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            //DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers");
            //GeoFire geofire = new GeoFire(ref);
            //geofire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));


            //a switch case where if no customerID, the driver will be free and no customer request
            switch (customerID){
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailability.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()));
                    break;
                default:

                    geoFireAvailability.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(),location.getLongitude()));
                    break;
            }
        }
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
        //we need to disconnect the driver after the driver click logout
        //check the the boolean value
        if(!currentLogoutDriverStatus){
            disconnectTheDriver();
        }

    }

    private void disconnectTheDriver() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(userID);
    }

    private void DriverLogout() {
        Intent welcomeIntent = new Intent(DriversMapActivity.this, WelcomeActivity.class);
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
