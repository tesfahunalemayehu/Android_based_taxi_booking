package com.taxitime.cab.Driver;
import android.Manifest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.taxitime.cab.Objects.LocationObject;
import com.taxitime.cab.Objects.RideObject;
import com.taxitime.cab.R;
import com.taxitime.cab.WelcomeActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.PendingIntent.getActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, DirectionCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;

    Location lastLocation;
    LocationRequest locationRequest;
    private Button LogoutDriverBtn,SettingsDriverButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private RideObject mCurrentRide;
    private Boolean currentLogOutUserStatus = false;
    //getting request customer's id
    private String customerID = "";
    private String driverID;
    private DatabaseReference AssignedCustomerRef,AssignedDriverID,ride_info;
    private DatabaseReference AssignedCustomerPickUpRef;
    Marker PickUpMarker;

    private ValueEventListener AssignedCustomerPickUpRefListner;

    private TextView txtName, txtPhone,txtCarName;
    private CircleImageView profilePic;
    private ImageView  dProfileImage,mCallCustomer;
    private RelativeLayout relativeLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //notice
        setContentView(R.layout.activity_driver_map);
        mAuth = FirebaseAuth.getInstance();

        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();
        LogoutDriverBtn = (Button) findViewById(R.id.logout_driv_btn);
        SettingsDriverButton = (Button) findViewById(R.id.settings_driver_btn);
        AssignedDriverID=FirebaseDatabase.getInstance().getReference().child("BookedRide_Info")
                .child("DriverRideID");
         ride_info=FirebaseDatabase.getInstance().getReference().child("ride_info");
        txtName = findViewById(R.id.name_customer);
        txtPhone = findViewById(R.id.phone_customer);
        profilePic = findViewById(R.id.profile_image_customer);
        relativeLayout = findViewById(R.id.rel2);
         mCallCustomer=(ImageView) findViewById(R.id.call_customer);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(DriverMapActivity.this);
        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this);

        SettingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(DriverMapActivity.this, DriversSettingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        dProfileImage=(ImageView)findViewById(R.id.profile_image_driver);
        LogoutDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                currentLogOutUserStatus = true;
                DisconnectDriver();
                mAuth.signOut();
                LogOutUser();
            }
        });
        mCallCustomer.setOnClickListener(view -> {
            if(mCurrentRide==null){
                Toast.makeText(DriverMapActivity.this, "Sorry driver has no Phone", Toast.LENGTH_LONG).show();
            }

            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE)
                    ==PackageManager.PERMISSION_GRANTED){
                Intent intent=new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+mCurrentRide.getCustomer().getPhone()));
                startActivity(intent);

            }
            else {
                Toast.makeText(DriverMapActivity.this, " You don't give phone call permission", Toast.LENGTH_LONG).show();

            }
        });
        getAssignedCustomersRequest();
    }
    private void getAssignedCustomersRequest()
    {
        AssignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("BookedRide_Info");

        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    String  name = dataSnapshot.child("DriverRideID").getValue().toString();
                    if (name.equals(driverID)){
                        customerID = dataSnapshot.child("CustomerRideID").getValue().toString();
                        makeSound();
                        GetAssignedCustomerPickupLocation();
                        relativeLayout.setVisibility(View.VISIBLE);
                        getAssignedCustomerInformation();
                        ride_info=FirebaseDatabase.getInstance().getReference().child("BookedRide_Info")
                                .child("CustomerRideID");
                    }
                    else
                    {
                        customerID = "";

                        if (PickUpMarker != null)
                        {
                            PickUpMarker.remove();
                        }

                        if (AssignedCustomerPickUpRefListner != null)
                        {
                            AssignedCustomerPickUpRef.removeEventListener(AssignedCustomerPickUpRefListner);
                        }

                        relativeLayout.setVisibility(View.GONE);
                    }
                   // AssignedCustomerRef.removeValue();
                }
                else
                {
                    customerID = "";

                    if (PickUpMarker != null)
                    {
                        PickUpMarker.remove();
                    }

                    if (AssignedCustomerPickUpRefListner != null)
                    {
                        AssignedCustomerPickUpRef.removeEventListener(AssignedCustomerPickUpRefListner);
                    }

                    relativeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void makeSound() {
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.driver_notification);
        mp.start();
    }
    private void GetAssignedCustomerPickupLocation()
    {
        AssignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("customer_requests")
                .child(customerID).child("l");

        AssignedCustomerPickUpRefListner =
                AssignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;

                            if(customerLocationMap.get(0) != null)
                            {
                                LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                            }
                            if(customerLocationMap.get(1) != null)
                            {
                                LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                            LocationObject pickupLocation=new LocationObject(DriverLatLng,"");

                            mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Customer PickUp Location")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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
                if(getApplicationContext() != null)
                {
                    //getting the updated location
                    lastLocation = location;


                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                    String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference DriversAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers_Available");
                    GeoFire geoFireAvailability = new GeoFire(DriversAvailabilityRef);
                    DatabaseReference DriversWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers_Working");
                    GeoFire geoFireWorking = new GeoFire(DriversWorkingRef);
                    switch (customerID)
                    {
                        case "":
                            geoFireWorking.removeLocation(userID);
                            geoFireAvailability.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;

                        default:
                            geoFireAvailability.removeLocation(userID);
                            geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                    }
                }
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this,new  String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CALL_PHONE}, 1);
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState)
    {

        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if(!currentLogOutUserStatus)
        {
            DisconnectDriver();
        }
    }


    private void DisconnectDriver()
    {
        if(mFusedLocationClient!=null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriversAvailabiltyRef = FirebaseDatabase.getInstance().getReference().child("Drivers_Available");
        GeoFire geoFire = new GeoFire(DriversAvailabiltyRef);
        geoFire.removeLocation(userID);

        DatabaseReference DriversWRef = FirebaseDatabase.getInstance().getReference().child("Drivers_Working");
        GeoFire geoFireW = new GeoFire(DriversWRef);
        geoFireW.removeLocation(userID);
    }
    public void LogOutUser()
    {
        Intent startPageIntent = new Intent(DriverMapActivity.this, WelcomeActivity.class);
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish();
    }
    private void getAssignedCustomerInformation()
    {
        relativeLayout.setVisibility(View.VISIBLE);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Customers").child(customerID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    txtName.setText(" Name: "+name);
                    txtPhone.setText("Phone Number:"+phone);

                    if (dataSnapshot.hasChild("profileImageUrl"))
                    {
                        String profileImage = dataSnapshot.child("profileImageUrl").getValue().toString();
                        Glide.with(getApplication())
                                .load(profileImage)
                                .apply(RequestOptions.circleCropTransform())
                                .into(profilePic);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {

    }

    @Override
    public void onDirectionFailure(Throwable t) {

    }
    /**
     * Get Route from pickup to destination, showing the route to the user
     */
    private void getRouteToMarker() {

        String serverKey = getResources().getString(R.string.google_maps_api);
        if (mCurrentRide.getDestination() != null && mCurrentRide.getPickup() != null) {
            GoogleDirection.withServerKey(serverKey)
                    .from(mCurrentRide.getDestination().getCoordinates())
                    .to(mCurrentRide.getPickup().getCoordinates())
                    .transportMode(TransportMode.DRIVING)
                    .execute(this);
        }
    }
}