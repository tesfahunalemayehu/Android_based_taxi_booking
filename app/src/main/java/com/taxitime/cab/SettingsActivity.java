package com.taxitime.cab;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.bumptech.glide.Glide;
import com.taxitime.cab.Customer.CustomersMapActivity;
import com.taxitime.cab.Driver.DriverMapActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity
{
    private String getType;

   private CircleImageView profileImageView;
    private EditText nameEditText, phoneEditText, driverCarName;
    private ImageView closeButton, saveButton;
    private TextView profileChangeBtn;
    private Uri resultUri;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private String checker = "";
    private Uri imageUri;
    private String myUrl = "";
    private StorageTask uploadTask;
    private StorageReference storageProfilePicsRef;
String userId;
CustomerObject mCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        getType = getIntent().getStringExtra("type");
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();


        mAuth = FirebaseAuth.getInstance();
        userId=mAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfilePicsRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");
        mCustomer=new CustomerObject(userId);


       profileImageView = findViewById(R.id.profile_image);

        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.phone_number);

        driverCarName = findViewById(R.id.driver_car_name);
        if (getType.equals("Drivers"))
        {
            driverCarName.setVisibility(View.VISIBLE);
        }

        closeButton = findViewById(R.id.close_button);
        saveButton = findViewById(R.id.save_button);

        profileChangeBtn = findViewById(R.id.change_picture_btn);



        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getType.equals("Drivers"))
                {
                    startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
                }
                else
                {
                    startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (checker.equals("clicked"))
                {
                    validateControllers();
                }
                else
                {
                    validateAndSaveOnlyInformation();
                }
            }
        });

        profileChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                checker = "clicked";

               Intent intent=new Intent(Intent.ACTION_PICK);
               intent.setType("image/*");
               startActivityForResult(intent,1);
            }
        });

        getUserInformation();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Glide.with(getApplication())
                        .load(bitmap) // Uri of the picture
                        .apply(RequestOptions.circleCropTransform())
                        .into(profileImageView);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void validateControllers()
    {
        if (TextUtils.isEmpty(nameEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your name.", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your phone number.", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Drivers")  &&  TextUtils.isEmpty(driverCarName.getText().toString()))
        {
            Toast.makeText(this, "Please provide your car Name.", Toast.LENGTH_SHORT).show();
        }
        else if (checker.equals("clicked"))
        {
            uploadProfilePicture();
        }
    }

    private void uploadProfilePicture()
    {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait, while we are settings your account information");
        progressDialog.show();
        if (imageUri != null)
        {
            //final StorageReference fileRef = storageProfilePicsRef.child(mAuth.getCurrentUser().getUid()  +  ".jpg");
            final StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://cabapp-553e7.appspot.com").child("profile_images").child(userId);
            uploadTask = fileRef.putFile(imageUri);
            uploadTask.addOnFailureListener(e->{
                finish();
            });
            uploadTask.addOnSuccessListener(taskSnapshot ->fileRef.getDownloadUrl().addOnSuccessListener(uri->{
                Map newImage=new HashMap();
                newImage.put("profileImageUrl",uri.toString());
                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("uid", mAuth.getCurrentUser().getUid());
                userMap.put("name", nameEditText.getText().toString());
                userMap.put("phone", phoneEditText.getText().toString());
                userMap.put("image", newImage);

                if (getType.equals("Drivers")) {
                    userMap.put("car", driverCarName.getText().toString());
                }
                databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(newImage);
                progressDialog.dismiss();
                if (getType.equals("Drivers"))
                {
                    startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
                }
                else
                {
                    startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
                }
             finish();
            }).addOnFailureListener(exception->{
                finish();
            }));
        }
        else
        {
            Toast.makeText(this, "Image is not selected.", Toast.LENGTH_SHORT).show();
        }
    }


    private void validateAndSaveOnlyInformation()
    {
        if (TextUtils.isEmpty(nameEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your name.", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString()))
        {
            Toast.makeText(this, "Please provide your phone number.", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Drivers")  &&  TextUtils.isEmpty(driverCarName.getText().toString()))
        {
            Toast.makeText(this, "Please provide your car Name.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", mAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone", phoneEditText.getText().toString());

            if (getType.equals("Drivers"))
            {
                userMap.put("car", driverCarName.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

            if (getType.equals("Drivers"))
            {
                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            }
            else
            {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));
            }
        }
    }


    private void getUserInformation()
    {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    mCustomer.parseData(dataSnapshot);
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    nameEditText.setText(name);
                    phoneEditText.setText(phone);

                    if (getType.equals("Drivers"))
                    {
                        String car = dataSnapshot.child("car").getValue().toString();
                        driverCarName.setText(car);
                    }


                    if (!mCustomer.getProfileImage().equals("default"))
                    {
                        Glide.with(getApplication()).load(mCustomer.getProfileImage()).apply(RequestOptions.circleCropTransform()).into(profileImageView);

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


}
