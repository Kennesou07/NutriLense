package org.nutrivision.bscs.capstone.detection;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;

import org.nutrivision.bscs.capstone.detection.adapter.DetectedObjectsAdapter;
import org.nutrivision.bscs.capstone.detection.customview.OverlayView;
import org.nutrivision.bscs.capstone.detection.env.ImageUtils;
import org.nutrivision.bscs.capstone.detection.env.Logger;
import org.nutrivision.bscs.capstone.detection.env.Utils;
import org.nutrivision.bscs.capstone.detection.tflite.Classifier;
import org.nutrivision.bscs.capstone.detection.tflite.YoloV5Classifier;
import org.nutrivision.bscs.capstone.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SelectImage extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    ImageView imageView,selectButton;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.7f;
    private static final Logger LOGGER = new Logger();
    private final int SELECT_CODE = 100, CAPTURE_CODE = 102, REALTIME_CODE = 103;

    public static final int TF_OD_API_INPUT_SIZE = 416;

    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    private static final String TF_OD_API_MODEL_FILE = "model35.tflite";

    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/label35.txt";

    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = true;
    private Integer sensorOrientation = 90;

    private Classifier detector;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private OverlayView trackingOverlay;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Bitmap sourceBitmap;
    private Bitmap cropBitmap;
    private LinearLayout contentView;
    static final float END_SCALE = 0.7f;
    private TextView noImageSelect;
    private RecyclerView detectedObjectsRecyclerView;
    private DetectedObjectsAdapter detectedObjectsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_image);
        getPermission();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);
        detectedObjectsAdapter = new DetectedObjectsAdapter();

        /*-------------------HOOKS-------------*/
        selectButton = findViewById(R.id.storageBtn);
        imageView = findViewById(R.id.imgView);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        contentView = findViewById(R.id.content);
        noImageSelect = findViewById(R.id.txtNoImage);
        // Initialize RecyclerView and its adapter
        detectedObjectsRecyclerView = findViewById(R.id.detectedObjectsRecyclerView);

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
        navigationView.setCheckedItem(R.id.nav_select_image);
        animateNavigationDrawer();

        /*-------------------USE----------------*/
        selectButton.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Photo"), SELECT_CODE);
            noImageSelect.setVisibility(View.INVISIBLE);
        });
        detectedObjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        detectedObjectsRecyclerView.setAdapter(detectedObjectsAdapter);

//        this.sourceBitmap = Utils.getBitmapFromAsset(SelectImage.this, "sample.jpg");

        if (sourceBitmap != null) {
            this.cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);
            if (cropBitmap != null) {
                this.imageView.setImageBitmap(cropBitmap);
                initBox();
            } else {
                // Log an error or show a message indicating an issue with processing the bitmap.
                Log.e("MainActivity", "Error processing bitmap");
            }
        } else {
            // Log an error or show a message indicating an issue with loading the bitmap from the asset.
            Log.e("MainActivity", "Error loading bitmap from asset");
        }
        initBox();
        noImageSelect.setVisibility(View.VISIBLE);
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

        System.err.println(Double.parseDouble(configurationInfo.getGlEsVersion()));
        System.err.println(configurationInfo.reqGlEsVersion >= 0x30000);
        System.err.println(String.format("%X", configurationInfo.reqGlEsVersion));
    }
    private void runObjectDetection(Bitmap bitmap) {
        Handler handler = new Handler();

        new Thread(() -> {
            final List<Classifier.Recognition> results = detector.recognizeImage(bitmap);
            handler.post(() -> handleResult(bitmap, results));
        }).start();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri selectedImageUri = data.getData();
        try {
            this.sourceBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImageUri);
            if (sourceBitmap != null) {
                this.cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);

                if (cropBitmap != null) {
                    this.imageView.setImageBitmap(cropBitmap);
                    runObjectDetection(cropBitmap);

                } else {
                    // Log an error or show a message indicating an issue with processing the bitmap.
                    Log.e("MainActivity", "Error processing bitmap");
                }
            } else {
                // Log an error or show a message indicating an issue with loading the bitmap from the asset.
                Log.e("MainActivity", "Error loading bitmap from asset");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void getPermission(){
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},100);
            requestPermissions(new String[] {Manifest.permission.CAMERA},103);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REALTIME_CODE && grantResults.length>0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermission();
            }
        }
        if(requestCode == SELECT_CODE && grantResults.length>0){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermission();
            }
        }
    }

    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        tracker = new MultiBoxTracker(this);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> tracker.draw(canvas));

        tracker.setFrameConfiguration(previewHeight, previewWidth, sensorOrientation);

        try {
            detector =
                    YoloV5Classifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED,
                            TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }
    // Run object detection method
    private void handleResult(Bitmap bitmap, List<Classifier.Recognition> results) {
        if(bitmap == null){
            return;
        }
        else{
//            final Canvas canvas = new Canvas(bitmap);
//            final Paint paint = new Paint();
//            paint.setColor(Color.RED);
//            paint.setStyle(Paint.Style.STROKE);
//            paint.setStrokeWidth(2.0f);
            final List<Classifier.Recognition> mappedRecognitions =
                    new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
//                    canvas.drawRect(location, paint);
                    cropToFrameTransform.mapRect(location);

                    // Set the location after mapping
                    result.setLocation(location);
                    mappedRecognitions.add(result);
                }
            }

            // Track and overlay code
            tracker.trackResults(mappedRecognitions, new Random().nextInt());
            trackingOverlay.postInvalidate();
//            final List<Classifier.Recognition> mappedRecognitions =
//                    new LinkedList<Classifier.Recognition>();
//
//            for (final Classifier.Recognition result : results) {
//                final RectF location = result.getLocation();
//                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
//                    canvas.drawRect(location, paint);
//    //                cropToFrameTransform.mapRect(location);
//    //
//    //                result.setLocation(location);
//    //                mappedRecognitions.add(result);
//                }
//            }
////            tracker.trackResults(mappedRecognitions, new Random().nextInt());
////            trackingOverlay.postInvalidate();
            imageView.setImageBitmap(bitmap);
            // Update data in the adapter when needed
            detectedObjectsAdapter.updateData(mappedRecognitions);
        }
    }
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
                startActivity(new Intent(SelectImage.this, Login.class));
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
                startActivity(new Intent(SelectImage.this,MainActivity.class));
                break;
            case R.id.nav_realtime:
                startActivity(new Intent(SelectImage.this, DetectorActivity.class));
                break;
            case R.id.nav_select_image:
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
