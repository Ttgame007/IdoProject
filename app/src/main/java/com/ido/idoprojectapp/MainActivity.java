package com.ido.idoprojectapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    Button signIn, signUp;
    ImageButton thwakz;
    ImageView SignInForeground;
    Boolean isSign;
    LinearLayout layout;
    EditText usernameET, passET;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        PrefsHelper prefs = new PrefsHelper(this);

        isSign = false;

        SignInForeground = findViewById(R.id.signInForeground);
        layout = findViewById(R.id.layout);
        signUp = findViewById(R.id.signUpText);
        thwakz = findViewById(R.id.thwakzLogo);
        signIn = findViewById(R.id.signIn);
        usernameET = findViewById(R.id.usrET);
        passET = findViewById(R.id.passET);

        layout.setVisibility(View.INVISIBLE);
        SignInForeground.setVisibility(View.INVISIBLE);

        //ui too probelmatic if i bhave time fix this later this line is to force protrait couse horizontal is messing up the views
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Log.d("data", "screen is forced to prtrait");

        Log.d("data", "attempting to log in user");

        //check if user previusly loged in and saved infon into prefs
        if (prefs.isLoggedIn()) {
            Log.d("data", "loged in succesfully. found user " + usernameET.getText().toString() + " pre existing data. moving to AiActivity");
            Intent intent = new Intent(this, AiActivity.class);
            startActivity(intent);
        }
        Log.d("data", "attempting to log in user failed data not found or an error has occured");

        //go to sign up activity if user clicks he doesnt have an account
        signUp.setOnClickListener(v -> {
            Log.d("data", "attempting to go to sign up activity");
            Intent intent = new Intent(this, SignUp.class);
            startActivity(intent);
        });

        //rediracts to the thwakz website
        thwakz.setOnClickListener(v -> {
            Log.d("data", "attempting to go to thwakz website");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.thwakz.org")));
        });


        signIn.setOnClickListener(v -> {

            String username = usernameET.getText().toString().trim();
            String password = passET.getText().toString().trim();
            //if user presses the sign in for the first time it opens the field after that will attepmpting to log in the user and checking cardentials
            if (isSign) {
                User user = new User(usernameET.getText().toString(), passET.getText().toString());
                HelperUserDB hudb = new HelperUserDB(this);

                if (hudb.checkUser(username, password)){
                    prefs.saveCardensials(username, password);
                    Log.d("data", "user" + username + " logged in");
                    Intent intent = new Intent(this, AiActivity.class);
                    startActivity(intent);
                }
                else {
                    usernameET.setError("Invalid username or password");
                }
            } else {
                Log.d("data", "showing log in fields");
                isSign = true;
                layout.setVisibility(View.VISIBLE);
                SignInForeground.setVisibility(View.VISIBLE);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}