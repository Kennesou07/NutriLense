package org.nutrivision.bscs.capstone.detection;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class Splashscreen extends Activity {
    SharedPreferences onBoardingScreen, preferences;
    Button connectBtn, closeBtn,retryBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.splashscreen);

        ImageView splash = findViewById(R.id.imageView);
        TextView poweredBy = findViewById(R.id.powered_by);
        Animation bottomAnim,sideAnim;

        //Animations
        sideAnim = AnimationUtils.loadAnimation(this,R.anim.side_animation);
        bottomAnim = AnimationUtils.loadAnimation(this,R.anim.button_animation);

        //Setting animations
        splash.setAnimation(sideAnim);
        poweredBy.setAnimation(bottomAnim);

        // Load the GIF into the ImageView using Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.nutrilense)
                .into(splash);

        // Set a delay to redirect to the next activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isConnected()){
                    onBoardingScreen = getSharedPreferences("onBoardingScreen",MODE_PRIVATE);
                    preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);

                    boolean isFirstTime = onBoardingScreen.getBoolean("firstTime",true);
                    boolean isLoggedIn = preferences.getBoolean("isLoggedIn",false);

                    if(isFirstTime){
                        SharedPreferences.Editor editor = onBoardingScreen.edit();
                        editor.putBoolean("firstTime",false);
                        editor.commit();

                        Intent intent = new Intent(Splashscreen.this, OnBoarding.class);
                        startActivity(intent);
                        finish();
                    }
                    else if(isLoggedIn){
                        Intent intent = new Intent(Splashscreen.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else{
                        Intent intent = new Intent(Splashscreen.this, Welcome.class);
                        startActivity(intent);
                        finish();
                    }
                }
                else{
                    showConnectionDialog();
                }
            }
        }, 4000);
    }
    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        return (wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected());
    }

    private void showConnectionDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.inflater_network_connection,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setView(dialogView);
        final AlertDialog connectionDialog = builder.create();
        connectBtn = dialogView.findViewById(R.id.connectBtn);
        closeBtn = dialogView.findViewById(R.id.exitBtn);
        retryBtn = dialogView.findViewById(R.id.retryBtn);
        Animation popAnim = AnimationUtils.loadAnimation(this,R.anim.wiggle_animation);
        dialogView.setAnimation(popAnim);

       connectBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
           }
       });
       retryBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               startActivity(new Intent(Splashscreen.this,Splashscreen.class));
               connectionDialog.dismiss();
               finish();
           }
       });
       closeBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               finish();
           }
       });
        connectionDialog.show();
    }
}