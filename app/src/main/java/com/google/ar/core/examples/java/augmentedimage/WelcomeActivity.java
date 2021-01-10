package com.google.ar.core.examples.java.augmentedimage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
    }

    public void buttonPress(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }
}