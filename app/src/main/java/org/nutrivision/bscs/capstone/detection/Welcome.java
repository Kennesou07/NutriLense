package org.nutrivision.bscs.capstone.detection;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;


public class Welcome extends AppCompatActivity {
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
    }
    public void callLoginScreen(View view){
        intent = new Intent(Welcome.this, Login.class);
        Pair[] pairs = new Pair[1];
        pairs[0] = new Pair<View,String>(findViewById(R.id.loginBtn),"login_transition");
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(Welcome.this,pairs);
        startActivity(intent,options.toBundle());
    }

    public void signup(View view){
        intent = new Intent(Welcome.this, SignUp.class);
        startActivity(intent);
    }
}
