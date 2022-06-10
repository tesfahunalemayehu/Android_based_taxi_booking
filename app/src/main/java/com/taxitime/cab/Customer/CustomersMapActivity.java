package com.taxitime.cab.Customer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.logicbeanzs.uberpolylineanimation.MapAnimator;
import com.taxitime.cab.CustomerObject;
import com.taxitime.cab.Objects.LocationObject;
import com.taxitime.cab.Objects.RideObject;
import com.taxitime.cab.R;
import com.taxitime.cab.SendNotification;
import com.taxitime.cab.SettingsActivity;
import com.taxitime.cab.TypeAdapter;
import com.taxitime.cab.WelcomeActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,DirectionCallback{
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location LastLocation;
    LocationRequest locationRequest;
    private LocationObject pickupLocation,currentLocation,destinationLocation;
    EditText autocompleteFragmentTo;
    private Button Logout, SettingsButton,CallCabCarButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private LatLng CustomerPickUpLocation;
    RideObject mCurrentRide;

    CardView destin;
    private TypeAdapter mAdapter;

    private DatabaseReference DriverAvailableRef, DriverLocationRef,CustomerDatabaseRef,
            customersDatabaseRef,DriversRef,cancelledDatabaseRef;
    private int radius = 1;

    private Boolean driverFound = false, requestType = false;
    private String driverFoundID,customerID,phone="";
    Marker DriverMarker, PickUpMarker;
    GeoQuery geoQuery;

    private ValueEventListener DriverLocationRefListner;

    private ImageView mCallDriver;
    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("customer_requests");
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers_Available");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers_Working");
        autocompleteFragmentTo=findViewById(R.id.to);
        customersDatabaseRef  =FirebaseDatabase.getInstance().getReference()
                .child("BookedRide_Info");
        cancelledDatabaseRef  =FirebaseDatabase.getInstance().getReference()
                .child("cancelled_requests");
        mCurrentRide=new RideObject(CustomersMapActivity.this,null);
        Logout = (Button) findViewById(R.id.logout_customer_btn);
        SettingsButton = (Button) findViewById(R.id.settings_customer_btn);
        CallCabCarButton =  (Button) findViewById(R.id.call_a_car_button);
        mCallDriver=(ImageView)findViewById(R.id.call_driver);
        txtName = findViewById(R.id.name_driver);
        txtPhone = findViewById(R.id.phone_driver);
        txtCarName = findViewById(R.id.car_name_driver);
        profilePic = findViewById(R.id.profile_image_driver);
        relativeLayout = findViewById(R.id.rel1);
        destin=(CardView)findViewById(R.id.dest);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);



        SettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                mAuth.signOut();
                LogOutUser();
            }
        });

        CallCabCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (requestType)
                {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriverLocationRef.removeEventListener(DriverLocationRefListner);

                    if (driverFound!=null)
                    {
                        DriversRef = FirebaseDatabase.getInstance().getReference().child("BookedRide_Info")
                                .child("CustomerRideID");
                        cancelledDatabaseRef= FirebaseDatabase.getInstance().getReference().child("BookedRide_Info")
                                .child("CustomerRideID");
                        DriversRef.removeValue();
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geofire = new GeoFire(cancelledDatabaseRef);
                    geofire.removeLocation(customerId);
                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.removeLocation(customerId);
                    if (PickUpMarker != null)
                    {
                        PickUpMarker.remove();
                    }
                    if (DriverMarker != null)
                    {
                        DriverMarker.remove();
                    }
                    CallCabCarButton.setText("Call a Cab");
                    relativeLayout.setVisibility(View.GONE);
                    destin.setVisibility(View.VISIBLE);
                    destin.setEnabled(true);
                }
                else
                {
                    requestType = true;
                    pickupLocation=currentLocation;
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(customerId, new GeoLocation( LastLocation.getLatitude(), LastLocation.getLongitude()));
                    CustomerPickUpLocation = new LatLng(LastLocation.getLatitude(), LastLocation.getLongitude());
                    fetchLocationName();
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPickUpLocation).
                            title("My Location")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                    mCurrentRide.setPickup(pickupLocation);
                    mCurrentRide.setDestination(destinationLocation);
                    MapAnimator();
                    CallCabCarButton.setText("Getting your Driver...");
                    getClosestDriverCab();
                }
            }
        });
    }

    private void fetchLocationName() {
        if (pickupLocation == null) {
            return;
        }
        try {

            Geocoder geo = new Geocoder(this.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(currentLocation.getCoordinates().latitude, currentLocation.getCoordinates().longitude, 1);
            if (addresses.isEmpty()) {
                // autocompleteFragmentFrom.setText(R.string.waiting_for_location);
            } else {
                addresses.size();
                if (addresses.get(0).getThoroughfare() == null) {
                    pickupLocation.setName(addresses.get(0).getLocality());
                } else if (addresses.get(0).getLocality() == null) {
                    pickupLocation.setName("Unknown Location");
                } else {
                    pickupLocation.setName(addresses.get(0).getLocality() + ", " + addresses.get(0).getThoroughfare());
                }
                // autocompleteFragmentFrom.setText(pickupLocation.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String distance="";
    HashMap driverMap = new HashMap();
    private void getClosestDriverCab()
    {
        GeoFire geoFire = new GeoFire(DriverAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPickUpLocation.latitude,
                CustomerPickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location)
            {
                if(!driverFound && requestType)
                {
                    driverFound = true;
                    driverFoundID = key;
                    bookingInfo();
                }
            }
            @Override
            public void onKeyExited(String key) {
            }
            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }
            @Override
            public void onGeoQueryReady()
            {
                if(!driverFound)
                {
                    radius = radius + 1;
                    getClosestDriverCab();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void bookingInfo() {
        //we tell driver which customer he is going to have
        DatabaseReference DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").
                child("Drivers").child(driverFoundID);
        customerID=FirebaseAuth.getInstance().getCurrentUser().getUid();
        driverMap.put("CustomerRideID", customerID);
        DriverRef.updateChildren(driverMap);
        driverMap.put("DriverRideID", driverFoundID);
        driverMap.put("pickup",mCurrentRide.getPickup());
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss aaa z");
        String dateTime = simpleDateFormat.format(calendar.getTime()).toString();
        driverMap.put("Request_Time",dateTime);
        driverMap.put("Destination",mCurrentRide.getDestination());
        float   Distance=0.0f;
        Location location1 = new Location("");
        location1.setLatitude(mCurrentRide.getPickup().getCoordinates().latitude);
        location1.setLongitude(mCurrentRide.getPickup().getCoordinates().longitude);
        Location location2 = new Location("");
        location2.setLatitude(mCurrentRide.getDestination().getCoordinates().latitude);
        location2.setLongitude(mCurrentRide.getDestination().getCoordinates().longitude);
        Distance = location1.distanceTo(location2);
        Distance/=1000.0f;
        String calculatedDistance=String.valueOf(Distance)+"km";
        driverMap.put("Calculated_Distance",calculatedDistance);
        driverMap.put("Calculated_Time",mCurrentRide.getCalculatedTime());

        GettingDriverLocation();
        customersDatabaseRef.updateChildren(driverMap);
        CallCabCarButton.setText("Looking for Driver Location...");
    }

    //and then we get to the driver location - to tell customer where is the driver
    private void GettingDriverLocation()
    {
        DriverLocationRefListner = DriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        float Distance=0.0f;
                        if(dataSnapshot.exists()  &&  requestType)
                        {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            CallCabCarButton.setText("Driver Found");
                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedDriverInformation();
                            if(driverLocationMap.get(0) != null)
                            {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if(driverLocationMap.get(1) != null)
                            {
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            //adding marker - to pointing where driver is - using this lat lng
                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                            if(DriverMarker != null)
                            {
                                DriverMarker.remove();
                            }
                            Location location1 = new Location("");
                            location1.setLatitude(CustomerPickUpLocation.latitude);
                            location1.setLongitude(CustomerPickUpLocation.longitude);
                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLongitude(DriverLatLng.longitude);
                            destinationLocation=new LocationObject(DriverLatLng,"");
                            Distance = location1.distanceTo(location2);

                            if (Distance < 0.09)
                            {
                                CallCabCarButton.setText("Driver's Reached");
                            }
                            else
                            {
                                CallCabCarButton.setText("Driver Found: " + String.valueOf(Distance/1000)+"km");
                            }
                            DriverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("your driver is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        }
                        distance=String.valueOf(Distance/1000)+" km";
                        driverMap.put("DistanceBetween : ", distance);
                        customersDatabaseRef.updateChildren(driverMap);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    public void searchLocation(View view){
        EditText locationSearch=(EditText) findViewById(R.id.to);
        autocompleteFragmentTo = locationSearch;
        String location="";
        location=autocompleteFragmentTo.getText().toString();
        if(location!=null|| !location.equals("")){
            Geocoder geocoder= new Geocoder(this);
            try {
                List<Address>  addressList=geocoder.getFromLocationName(location,1);
                Address address=addressList.get(0);
                LatLng latLng=new LatLng(address.getLatitude(),address.getLongitude());
                destinationLocation = new LocationObject(latLng, location);
                locationSearch.setText(location);
                mCurrentRide.setDestination(destinationLocation);
                autocompleteFragmentTo.setText(location);
                destin.setVisibility(View.INVISIBLE);
                destin.setEnabled(false);
                MapAnimator();
                getRouteToMarker();

                mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                Toast.makeText(getApplicationContext(), address.getLatitude()+""+address.getLongitude()+location, Toast.LENGTH_SHORT).show();

            }
            catch ( IOException e){
                e.printStackTrace();
            }

        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }
        }
    }


    boolean zoomUpdated = false;
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                try {
                    LastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Geocoder geocoder=new Geocoder(CustomersMapActivity.this,Locale.getDefault());
                    List<Address>addresses=geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
                    String address=addresses.get(0).getAddressLine(0);
                    currentLocation=new LocationObject(latLng,address);
                    mCurrentRide.setCurrent(currentLocation);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * Get permissions for our app if they didn't previously exist.
     * requestCode -> the number assigned to the request that we've made.
     * Each request has it's own unique request code.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CALL_PHONE}, 1))
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(CustomersMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CALL_PHONE}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(getApplication(), "Please provide the permission", Toast.LENGTH_LONG).show();
            }
        }
    }



    //create this method -- for useing apis

    @Override
    protected void onStop()
    {

        super.onStop();
    }
    public void LogOutUser()
    {
        Intent startPageIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }
    private void getAssignedDriverInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverFoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String carModel = dataSnapshot.child("car").getValue().toString();
                    txtName.setText("Name: "+name);
                    txtPhone.setText("Phone: "+phone);
                    txtCarName.setText("Car Model: "+carModel);
                    if (dataSnapshot.hasChild("profileImageUrl"))
                    {
                        String profileImage = dataSnapshot.child("profileImageUrl").getValue().toString();
                        Glide.with(getApplication())
                                .load(profileImage)
                                .apply(RequestOptions.circleCropTransform())
                                .into(profilePic);
                    }
                    CustomerObject mCustomer=new CustomerObject();
                    String kk=mCustomer.getNotificationKey();
                    new SendNotification("You have a customer waiting ","New Ride",driverFoundID);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mCallDriver.setOnClickListener(view -> {

            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE)
                    ==PackageManager.PERMISSION_GRANTED){
                Intent intent=new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phone));

            }
            else {
                Snackbar.make(findViewById(R.id.call_driver)," You don't give phone call permission",Snackbar.LENGTH_LONG).show();
            }
        });
    }
    public static Bitmap generateBitmap(Context context, String location, String duration) {
        Bitmap bitmap = null;
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        RelativeLayout view = new RelativeLayout(context);
        try {
            mInflater.inflate(R.layout.item_marker, view, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView locationTextView = (TextView) view.findViewById(R.id.location);
        TextView durationTextView = (TextView) view.findViewById(R.id.duration);
        locationTextView.setText(location);

        if(duration != null){
            durationTextView.setText(duration);
        }else{
            durationTextView.setVisibility(View.GONE);
        }

        view.setLayoutParams(new ViewGroup.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(bitmap);

        view.draw(c);

        return bitmap;
    }

    public ArrayList<Double> parseJson(JSONObject jObject) {

        List<List<HashMap<String, String>>> routes = new ArrayList<>();
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;
        JSONObject jDistance = null;
        JSONObject jDuration = null;
        long totalDistance = 0;
        int totalSeconds = 0;

        try {

            jRoutes = jObject.getJSONArray("routes");

            /* Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");

                /* Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {

                    jDistance = ((JSONObject) jLegs.get(j)).getJSONObject("distance");

                    totalDistance = totalDistance + Long.parseLong(jDistance.getString("value"));

                    /** Getting duration from the json data */
                    jDuration = ((JSONObject) jLegs.get(j)).getJSONObject("duration");
                    totalSeconds = totalSeconds + Integer.parseInt(jDuration.getString("value"));

                }
            }

            double dist = totalDistance / 1000.0;
            Log.d("distance", "Calculated distance:" + dist);

            int days = totalSeconds / 86400;
            int hours = (totalSeconds - days * 86400) / 3600;
            int minutes = (totalSeconds - days * 86400 - hours * 3600) / 60;
            int seconds = totalSeconds - days * 86400 - hours * 3600 - minutes * 60;
            Log.d("duration", days + " days " + hours + " hours " + minutes + " mins" + seconds + " seconds");

            ArrayList<Double> list = new ArrayList<Double>();
            list.add(dist);
            list.add((double) totalSeconds);

            return list;



        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    ArrayList<Double> routeData;

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        if (direction.isOK()) {
            Route route = direction.getRouteList().get(0);
            try {
                JSONObject obj = new JSONObject(rawBody);

                routeData = parseJson(obj);

                mAdapter.setData(routeData);
                mAdapter.notifyDataSetChanged();

                Log.d("My App", obj.toString());

            } catch (Throwable ignored) {
            }

            Marker destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLocation
                    .getCoordinates()).icon(BitmapDescriptorFactory
                    .fromBitmap(generateBitmap(CustomersMapActivity.this, destinationLocation.getName(), route.getLegList().get(0).getDuration().getText()))));

            List<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();

            MapAnimator.getInstance().animateRoute(mMap, directionPositionList);

            setCameraWithCoordinationBounds(route);
        }
    }
    private void getRouteToMarker() {

        String serverKey = getResources().getString(R.string.google_maps_api);
        if (mCurrentRide.getDestination() != null && mCurrentRide.getPickup() != null) {
            GoogleDirection.withServerKey(serverKey)
                    .from(mCurrentRide.getDestination().getCoordinates())
                    .to(mCurrentRide.getPickup().getCoordinates())
                    .transportMode(TransportMode.DRIVING)
                    .execute((DirectionCallback) this);
        }
    }

    private List<Polyline> polylines = new ArrayList<>();

    /**
     * Remove route polylines from the map
     */
    private void erasePolylines() {
        if (polylines == null) {
            return;
        }
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }

    /**
     * Show map within the pickup and destination marker,
     * This will make sure everything is displayed to the user
     *
     * @param route - route between pickup and destination
     */
    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void MapAnimator() {
        if (polylines == null) {
            return;
        }
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }

    @Override
    public void onDirectionFailure(Throwable t) {

    }
}
