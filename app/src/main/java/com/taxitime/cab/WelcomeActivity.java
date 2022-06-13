package com.taxitime.cab;


import android.content.Intent;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.onesignal.OneSignal;
import com.taxitime.cab.Customer.CustomerLoginRegisterActivity;
import com.taxitime.cab.Driver.DriverLoginRegisterActivity;

public class WelcomeActivity extends AppCompatActivity {
    private Button DriverWelcomeButton;
    private Button CustomerWelcomeButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListner;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.initWithContext(this);
        OneSignal.setAppId("944a4906-1db8-49fb-b39f-227d168911ff");
        DriverWelcomeButton = (Button) findViewById(R.id.driver_welcome_btn);
        CustomerWelcomeButton = (Button) findViewById(R.id.customer_welcome_btn);

        DriverWelcomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent DriverIntent = new Intent(WelcomeActivity.this, DriverLoginRegisterActivity.class);
                startActivity(DriverIntent);
            }
        });

        CustomerWelcomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent CustomerIntent = new Intent(WelcomeActivity.this, CustomerLoginRegisterActivity.class);
                startActivity(CustomerIntent);
            }
        });
    }
}
