package org.nutrivision.bscs.capstone.detection;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;


public class Welcome extends AppCompatActivity {
    Intent intent;
    SharedPreferences isLoggedIn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        checkSession();
    }
    public void callLoginScreen(View view){
        intent = new Intent(Welcome.this, Login.class);
        Pair[] pairs = new Pair[1];
        pairs[0] = new Pair<View,String>(findViewById(R.id.loginBtn),"login_transition");
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(Welcome.this,pairs);
        startActivity(intent,options.toBundle());
    }
    public void callSignUpScreen(View view){
        intent = new Intent(Welcome.this, SignUp.class);
        Pair[] pairs = new Pair[1];
        pairs[0] = new Pair<View,String>(findViewById(R.id.signUpBtn),"signup_transition");
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(Welcome.this,pairs);
        startActivity(intent,options.toBundle());
    }
    public void callBoardingScreen(View view){
        intent = new Intent(Welcome.this,OnBoarding.class);
        startActivity(intent);
        finish();
    }
    private void checkSession(){
        isLoggedIn = getSharedPreferences("LogInSession", MODE_PRIVATE);
        boolean isLogged = isLoggedIn.getBoolean("isLoggedIn", false);

        if (isLogged) {
            Intent intent = new Intent(Welcome.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        Log.d("SharedPreferences","isLoggedIn: " + isLogged);
    }
}
