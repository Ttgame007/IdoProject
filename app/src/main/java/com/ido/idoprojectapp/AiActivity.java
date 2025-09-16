package com.ido.idoprojectapp;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.lang.reflect.Field;

public class AiActivity extends AppCompatActivity {
    Button logOut;
    TextView welcomeText;
    DrawerLayout drawerLayout;

    float startX;
    ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai);

        //inisializing variables
        logOut = findViewById(R.id.logout);
        welcomeText = findViewById(R.id.TVguest);
        drawerLayout = findViewById(R.id.main);
        layout = findViewById(R.id.content);

        //welcome text with username
        PrefsHelper prefs = new PrefsHelper(this);
        setupWelocome(prefs);


        setupDrawer();

        //log out test function delete and move later
        logOut.setOnClickListener(v -> {
                    logOut();
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    public void logOut(){
        PrefsHelper prefs = new PrefsHelper(this);
        prefs.clearCardensials();
        finish();
    }

    public void setupWelocome(PrefsHelper prefs){
        String username = prefs.getUsername();
        welcomeText.setText(username);
    }
    public void setupDrawer(){
        //adds a slider effect that pushes the screen when the drawer opens
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                float slideX = drawerView.getWidth() * slideOffset;
                layout.setTranslationX(slideX);
            }
        });
        //setting the drag field to open the menu to 100% of the screen
        drawerLayout.setOnTouchListener((v, event) -> {
            float deltaX = event.getX() - startX;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startX = event.getX();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE && deltaX > 200 && !drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });
    }

}