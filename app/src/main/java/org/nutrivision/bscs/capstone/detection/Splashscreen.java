package org.nutrivision.bscs.capstone.detection;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class Splashscreen extends Activity {
    SharedPreferences onBoardingScreen, preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
                .load(R.drawable.nutrivision)
                .into(splash);

        // Set a delay to redirect to the next activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

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
        }, 4000);
    }
}