package com.colossus.teletaxiapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPass;
    private Button bLogin, bRegister;

    private FirebaseAuth fbAuth;
    private FirebaseAuth.AuthStateListener fbAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        etEmail = findViewById(R.id.email);
        etPass = findViewById(R.id.pass);
        bLogin = findViewById(R.id.login);
        bRegister = findViewById(R.id.register);

        fbAuth = FirebaseAuth.getInstance();
        fbAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    Log.i(">>>", user.getEmail());
                    Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CharSequence email = etEmail.getText();
                if (!isValidEmail(email)) {
                    Toast.makeText(DriverLoginActivity.this, "Email incorrecto", Toast.LENGTH_SHORT).show();
                    etEmail.setError("Email invalido");
                }

                final String passwd = etPass.getText().toString();

                fbAuth.createUserWithEmailAndPassword(email.toString(), passwd).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(DriverLoginActivity.this, "No se pudo crear el usuario", Toast.LENGTH_SHORT).show();
                        } else {
                            String userId = fbAuth.getCurrentUser().getUid();
                            String usertype = getIntent().getExtras().getString("userType");
                            DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference().child("User").child(usertype).child(userId);
                            dbReference.setValue(true);
                        }
                    }
                });
            }
        });

        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CharSequence email = etEmail.getText();
                if (!isValidEmail(email)) {
                    Toast.makeText(DriverLoginActivity.this, "Email incorrecto", Toast.LENGTH_SHORT).show();
                    etEmail.setError("Email invalido");
                    return;
                }
                final String passwd = etPass.getText().toString();

                fbAuth.signInWithEmailAndPassword(email.toString(), passwd).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(DriverLoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        fbAuth.addAuthStateListener(fbAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        fbAuth.removeAuthStateListener(fbAuthListener);
    }

    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}