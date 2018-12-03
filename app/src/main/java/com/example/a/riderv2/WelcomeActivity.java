package com.example.a.riderv2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    private Button WelcomeDriverButton;
    private Button WelcomeCustomerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        WelcomeCustomerButton = (Button)findViewById(R.id.welcome_customer_btn);
        WelcomeDriverButton = (Button)findViewById(R.id.welcome_driver_btn);

        WelcomeCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start activity which go to the customer Login page
                Intent LoginRegisterCustomerIntent = new Intent(WelcomeActivity.this, CustomerLoginRegisterActivity.class);
                startActivity(LoginRegisterCustomerIntent);
            }
        });
        WelcomeDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start activity which go to the driver Login page
                Intent LoginRegisterCustomerIntent = new Intent(WelcomeActivity.this, DriverLoginRegisterActivity.class);
                startActivity(LoginRegisterCustomerIntent);
            }
        });
    }
}
