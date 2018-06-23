package com.colossus.teletaxiapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button bDriver, bCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bDriver = findViewById(R.id.driver);
        bCustomer = findViewById(R.id.customer);

        bDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DriverLoginActivity.class);
                intent.putExtra("userType", "Driver");
                startActivity(intent);
                finish();
            }
        });

        bCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DriverLoginActivity.class);
                intent.putExtra("userType", "Customer");
                startActivity(intent);
                finish();
            }
        });
    }
}
