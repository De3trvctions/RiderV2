package com.example.a.riderv2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DriverLoginRegisterActivity extends AppCompatActivity {

    private Button DriverLoginButton;
    private Button DriverRegisterButton;

    private TextView DriverRegisterLink;
    private TextView DriverStatus;

    private EditText DriverEmail;
    private EditText DriverPassword;

    private ProgressDialog loadingBar;

    private boolean checkMail;

    private FirebaseAuth mAuth;

    private DatabaseReference driverDatabaseRef;

    private String driverID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);

        DriverLoginButton = (Button)findViewById(R.id.driver_login_btn);
        DriverRegisterButton = (Button)findViewById(R.id.driver_register_btn);
        DriverRegisterLink = (TextView)findViewById(R.id.driver_register_link);
        DriverStatus = (TextView)findViewById(R.id.driver_status);
        DriverEmail = (EditText)findViewById(R.id.driver_email);
        DriverPassword = (EditText)findViewById(R.id.driver_password);
        loadingBar = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        DriverRegisterButton.setVisibility(View.INVISIBLE);
        DriverRegisterButton.setEnabled(false);

        DriverRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DriverLoginButton.setVisibility(View.INVISIBLE);
                DriverRegisterLink.setVisibility(View.INVISIBLE);
                DriverStatus.setText("Driver Registration");

                DriverRegisterButton.setVisibility(View.VISIBLE);
                DriverRegisterButton.setEnabled(true);
            }
        });


        DriverRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = DriverEmail.getText().toString();
                String password = DriverPassword.getText().toString();

                RegisterDriver(email,password);
            }
        });

        DriverLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = DriverEmail.getText().toString();
                String password = DriverPassword.getText().toString();

                SignInDriver(email,password);
            }
        });

    }

    private void SignInDriver(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
        }
        else{
            checkMail = isEmailValid(email);
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Driver Login");
            loadingBar.setMessage("Please wait while we are checking your information");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        Intent driverIntent = new Intent(DriverLoginRegisterActivity.this, DriversMapActivity.class);
                        startActivity(driverIntent);

                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver Login Successful.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else if(checkMail == false){
                        Toast.makeText(DriverLoginRegisterActivity.this, "Sign In Failed. Enter a correct Email", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else{
                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver Login Failed.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void RegisterDriver(String email, String password) {

        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
        }
        else{
            checkMail = isEmailValid(email);
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Driver Registration");
            loadingBar.setMessage("Please wait while we are registering your information");
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        driverID = mAuth.getCurrentUser().getUid();
                        //store the customer uid into Firebase database
                        driverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverID);
                        driverDatabaseRef.setValue(true);

                        Intent driverIntent = new Intent(DriverLoginRegisterActivity.this, DriversMapActivity.class);
                        startActivity(driverIntent);

                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver Register Successful.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else if(checkMail == false){
                        Toast.makeText(DriverLoginRegisterActivity.this, "Register Failed. Enter a correct Email", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else{
                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver Register Failed.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }


    /**
     * method is used for checking valid email id format.
     *
     * @param email
     * @return boolean true for valid false for invalid
     */
    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}
