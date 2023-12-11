package org.nutrivision.bscs.capstone.detection;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;

import org.nutrivision.bscs.capstone.detection.R;
import org.nutrivision.bscs.capstone.detection.adapter.FeaturedAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.FeaturedClass;
import org.nutrivision.bscs.capstone.detection.adapter.mostViewedProductsAdapter;
import org.nutrivision.bscs.capstone.detection.customview.OverlayView;
import org.nutrivision.bscs.capstone.detection.env.ImageUtils;
import org.nutrivision.bscs.capstone.detection.env.Logger;
import org.nutrivision.bscs.capstone.detection.env.Utils;
import org.nutrivision.bscs.capstone.detection.tflite.Classifier;
import org.nutrivision.bscs.capstone.detection.tflite.YoloV5Classifier;
import org.nutrivision.bscs.capstone.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    RecyclerView featuredRecycler,mostViewedRecycler;
    RecyclerView.Adapter adapterModel,adapterMostViewed,adapterCategories;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    static final float END_SCALE = 0.7f;
    LinearLayout contentView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*-------------------HOOKS---------------*/
        featuredRecycler = findViewById(R.id.featured_recyclerView);
        mostViewedRecycler = findViewById(R.id.most_viewed_recyclerView);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        contentView = findViewById(R.id.content);
        /*----------------TOOLBAR---------------*/
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        /*-------------NAVIGATION DRAWER MENU---------------*/
//        Menu menu = navigationView.getMenu();
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.navigation_open,R.string.navigation_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);
        animateNavigationDrawer();
        Recycler();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);

        if(acc != null){
            String name = acc.getDisplayName();
            String email = acc.getEmail();
        }
    }

    private void Recycler() {
        /*---------------FEATURED MODELS-------------*/

        featuredRecycler.setHasFixedSize(true);
        featuredRecycler.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));

        ArrayList<FeaturedClass> featuredModels = new ArrayList<>();
        featuredModels.add(new FeaturedClass(R.drawable.sample_image,"Model 35","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));
        featuredModels.add(new FeaturedClass(R.drawable.sample_image,"Model 9","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));
        featuredModels.add(new FeaturedClass(R.drawable.sample_image,"Model 100","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));

        adapterModel = new FeaturedAdapter(featuredModels);
        featuredRecycler.setAdapter(adapterModel);

        /*---------------MOST VIEWED PRODUCTS-------------*/

        mostViewedRecycler.setHasFixedSize(true);
        mostViewedRecycler.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));

        ArrayList<FeaturedClass> mostViewedProducts = new ArrayList<>();
        mostViewedProducts.add(new FeaturedClass(R.drawable.sample_image,"San Marino","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));
        mostViewedProducts.add(new FeaturedClass(R.drawable.sample_image,"Loaded","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));
        mostViewedProducts.add(new FeaturedClass(R.drawable.sample_image,"Pancit Canton","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));

        adapterMostViewed = new mostViewedProductsAdapter(mostViewedProducts);
        mostViewedRecycler.setAdapter(adapterMostViewed);

        GradientDrawable gradient1 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xffeff400,0xffaff600});
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    private void logout() {
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                clearPreferences();
                finish();
                startActivity(new Intent(MainActivity.this, Login.class));
            }
        });
    }

    private void clearPreferences() {
        SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_home:
                break;
            case R.id.nav_realtime:
                startActivity(new Intent(MainActivity.this, DetectorActivity.class));
                break;
            case R.id.nav_select_image:
                startActivity(new Intent(MainActivity.this, SelectImage.class));
                break;
            case R.id.nav_logout:
                logout();
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void animateNavigationDrawer() {
        //Add any color or remove it to use the default one!
        //To make it transparent use Color.Transparent in side setScrimColor();
        drawerLayout.setScrimColor(getResources().getColor(R.color.tfe_color_primary_dark));
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Scale the View based on current slide offset
                final float diffScaledOffset = slideOffset * (1 - END_SCALE);
                final float offsetScale = 1 - diffScaledOffset;
                contentView.setScaleX(offsetScale);
                contentView.setScaleY(offsetScale);
                // Translate the View, accounting for the scaled width
                final float xOffset = drawerView.getWidth() * slideOffset;
                final float xOffsetDiff = contentView.getWidth() * diffScaledOffset / 2;
                final float xTranslation = xOffset - xOffsetDiff;
                contentView.setTranslationX(xTranslation);
            }
        });
    }
}
