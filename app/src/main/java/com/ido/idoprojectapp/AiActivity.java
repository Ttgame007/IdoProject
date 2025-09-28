package com.ido.idoprojectapp;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

public class AiActivity extends AppCompatActivity {
    LLMW llmw;
    Button BTNsend;
    TextView welcomeText;
    TextView TVoutput;
    EditText ETinput;
    DrawerLayout drawerLayout;

    NavigationView navigationView;

    float startX;
    ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai);

        //inisializing variables
        PrefsHelper prefs = new PrefsHelper(this);

        welcomeText = findViewById(R.id.TVguest);
        TVoutput = findViewById(R.id.textView);
        ETinput = findViewById(R.id.ETinput);
        drawerLayout = findViewById(R.id.main);
        layout = findViewById(R.id.content);
        navigationView = findViewById(R.id.navigationView);
        BTNsend = findViewById(R.id.BTNsend);
        Menu menu = navigationView.getMenu();
        Menu groupChat = menu.findItem(R.id.chatsGroup).getSubMenu();
        List<Chat> chats = JsonHelper.loadChats(this, prefs.getUsername());

        //initiallizing menu
        for (Chat chat : chats) {
            groupChat.add(Menu.NONE, chat.getId(), Menu.NONE, chat.getName());
        }

        //setting keyboard to not override ETinput
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        llmw = null;
        try {
            llmw = LLMW.Companion.getInstance(preparePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BTNsend.setOnClickListener(v -> {
            String input = "user: " + ETinput.getText().toString() + "\n Assistant: ";
            TVoutput.setText("");

            llmw.send(input, msg -> runOnUiThread(() -> {
                TVoutput.append(msg);
                Log.d("input", input);
                Log.d("output", msg);
                ETinput.setText("");
                ETinput.clearFocus();
            }));

        });





        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.logoutmenu) {
                logOut();
                return true;
            } else if (id == R.id.newChat) {
                Chat newChat = new Chat("Chat" + groupChat.size(), menu.size(), null);
                chats.add(newChat);
                JsonHelper.saveChats(this, prefs.getUsername(), chats);
                JsonHelper.loadChats(this, prefs.getUsername());
                groupChat.add(R.id.chatsGroup, newChat.getId(), newChat.getId(), newChat.getName());
                navigationView.requestLayout();
                return true;
            }
            return false;
        });

        //welcome text with username
        setupWelocome(prefs);

        //changes the profile item test to username
        MenuItem profileItem = menu.findItem(R.id.profile);
        profileItem.setTitle(prefs.getUsername());

        //setupDrawer();

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
        navigationView.requestLayout();
        navigationView.invalidate();
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
                navigationView.requestLayout();
                navigationView.invalidate();
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });
    }


    private String preparePath() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.llama);

        File tempFile = File.createTempFile("myfile", ".gguf", getCacheDir());
        OutputStream out = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        inputStream.close();
        out.close();
        return tempFile.getAbsolutePath().toString();
    }
}