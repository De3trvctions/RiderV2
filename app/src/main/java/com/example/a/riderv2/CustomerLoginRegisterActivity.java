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

public class CustomerLoginRegisterActivity extends AppCompatActivity {

    private Button CustomerLoginButton;
    private Button CustomerRegisterButton;
    private TextView CustomerRegisterLink;
    private TextView CustomerStatus;
    private EditText CustomerEmail;
    private EditText CustomerPassword;
    private ProgressDialog loadingBar;
    private boolean checkMail;

    private FirebaseAuth mAuth;

    private DatabaseReference customerDatabaseRef;
    private String customerID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_register);

        CustomerLoginButton = (Button)findViewById(R.id.customer_login_btn);
        CustomerRegisterButton = (Button)findViewById(R.id.customer_register_btn);
        CustomerRegisterLink = (TextView)findViewById(R.id.customer_register_link);
        CustomerStatus = (TextView)findViewById(R.id.customer_status);
        CustomerEmail = (EditText)findViewById(R.id.customer_email);
        CustomerPassword = (EditText)findViewById(R.id.customer_password);
        loadingBar = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        CustomerRegisterButton.setVisibility(View.INVISIBLE);
        CustomerRegisterButton.setEnabled(false);

        CustomerRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomerLoginButton.setVisibility(View.INVISIBLE);
                CustomerRegisterLink.setVisibility(View.INVISIBLE);
                CustomerStatus.setText("Customer Registration");

                CustomerRegisterButton.setVisibility(View.VISIBLE);
                CustomerRegisterButton.setEnabled(true);
            }
        });

        CustomerRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = CustomerEmail.getText().toString();
                String password = CustomerPassword.getText().toString();

                RegisterCustomer(email,password);
            }
        });

        CustomerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = CustomerEmail.getText().toString();
                String password = CustomerPassword.getText().toString();

                SignInCustomer(email,password);
            }
        });
    }

    private void SignInCustomer(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
        }
        else{
            checkMail = isEmailValid(email);
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Customer Login");
            loadingBar.setMessage("Please wait while we are checking your information");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this, CustomersMapActivity.class);
                        startActivity(customerIntent);

                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Login Successful.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else if(checkMail == false){
                        Toast.makeText(CustomerLoginRegisterActivity.this, "Register Failed. Enter a correct Email", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else{
                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Login Failed.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void RegisterCustomer(String email, String password) {

        if(TextUtils.isEmpty(email)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
        }
        else{
            checkMail = isEmailValid(email);
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Customer Registration");
            loadingBar.setMessage("Please wait while we are registering your information");
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        customerID = mAuth.getCurrentUser().getUid();
                        //store the customer uid into Firebase database
                        customerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Customers").child(customerID);
                        customerDatabaseRef.setValue(true);

                        Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this, CustomersMapActivity.class);
                        startActivity(customerIntent);

                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Register Successful.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else if(checkMail == false){
                        Toast.makeText(CustomerLoginRegisterActivity.this, "Register Failed. Enter a correct Email", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                    else{
                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Register Failed.", Toast.LENGTH_SHORT).show();
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
