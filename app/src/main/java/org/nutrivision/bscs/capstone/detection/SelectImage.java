package org.nutrivision.bscs.capstone.detection;

import static org.nutrivision.bscs.capstone.detection.API.HEALTH_CONDITION;
import static org.nutrivision.bscs.capstone.detection.API.PRODUCT_DETAILS;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.DetectedObjectsAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionItem;
import org.nutrivision.bscs.capstone.detection.customview.OverlayView;
import org.nutrivision.bscs.capstone.detection.env.ImageUtils;
import org.nutrivision.bscs.capstone.detection.env.Logger;
import org.nutrivision.bscs.capstone.detection.env.Utils;
import org.nutrivision.bscs.capstone.detection.tflite.Classifier;
import org.nutrivision.bscs.capstone.detection.tflite.YoloV5Classifier;
import org.nutrivision.bscs.capstone.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SelectImage extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener{
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

    private static final String TF_OD_API_MODEL_FILE = "model295.tflite";

    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/label295.txt";

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
    private List<NutritionItem> nutritionItemList = new ArrayList<>();
    private String prodName;
    private double avgCalorie = 2000.00; // kcal unit

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
        detectedObjectsAdapter.setOnItemClickListener(new DetectedObjectsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String objectName) {
                Toast.makeText(SelectImage.this,"Please wait! " ,Toast.LENGTH_SHORT).show();
                Log.d("DetectedObjectsAdapter", "Item clicked: " + objectName);
                displayInformation(objectName);
            }
        });
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
            case R.id.nav_profile:
                startActivity(new Intent(SelectImage.this, Profile.class));
                break;
            case R.id.nav_logout:
                logout();
                break;
            case R.id.nav_share:
                break;
            case R.id.nav_feedback:
                break;
            case R.id.nav_about:
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void animateNavigationDrawer() {
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
    private void displayInformation(String objectName) {
        prodName = objectName;
        SharedPreferences getID = getSharedPreferences("LogInSession", MODE_PRIVATE);
        int ID = getID.getInt("userId", 0);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, PRODUCT_DETAILS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d("RawResponse", response); // Print the raw response

                            if (response.startsWith("<br")) {
                                // Handle unexpected response, it might be an error message or HTML content.
                                Log.e("Error", "Unexpected response format");
                            } else {
                                // Proceed with parsing as JSON
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    String result = jsonObject.getString("status");

                                    if (result.equals("success")) {
                                        // Extract data from the JSON response
                                        JSONObject data = jsonObject.getJSONObject("data");

                                        // Now data contains only non-null values, you can iterate over it
                                        Iterator<String> keys = data.keys();
                                        nutritionItemList.clear();
                                        while (keys.hasNext()) {
                                            String key = keys.next();
                                            String value = data.getString(key);
                                            if (!key.equals("id") && !key.equals("Product Names") && !value.isEmpty() && !key.equals("Category")) {
                                                // Create a NutritionItem for each key-value pair
                                                NutritionItem nutritionItem = new NutritionItem(key, value);
                                                nutritionItemList.add(nutritionItem);
                                                // Handle key-value pairs as needed
                                                Log.d("NutritionInfo", key + ": " + value);
                                            }
                                        }
                                        checkHealthCondition(ID, nutritionItemList);
                                        // Update your UI or perform other actions with the retrieved data
                                        // updateRecyclerView(nutritionItemList,false,null,null);
                                    } else {
                                        // Handle the case where the status is not "success"
                                        String message = jsonObject.getString("message");
                                        Log.e("Error", "Server response: " + message);
                                    }
                                } catch (JSONException e) {
                                    Log.e("Error", "Error parsing JSON: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Error", "Exception: " + e.getMessage());
                        }
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VolleyError", "Error: " + error.getMessage());
                // Handle error response
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("productName", objectName);
                return params;
            }
        };

        // Set the retry policy and add the request to the queue
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(SelectImage.this);
        queue.add(stringRequest);
    }
    private AlertDialog productDialog;

    private void updateRecyclerView(List<NutritionItem> nutritionItemList, boolean isSafeToConsume, String triggeringCondition, String highIn) {
        if (productDialog == null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.inflater_product_details, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);
            TextView productName = dialogView.findViewById(R.id.ProductName);
            Button btnDone = dialogView.findViewById(R.id.Done);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            productName.setText(prodName);
            // Create an adapter for the RecyclerView
            NutritionAdapter adapter = new NutritionAdapter(nutritionItemList);
            recyclerView.setAdapter(adapter);

            // Update the color of the product name based on health condition
            if (isSafeToConsume) {
                productName.setTextColor(getResources().getColor(R.color.green)); // Set color to green

                // Add a note under the product name
                TextView noteTextView = dialogView.findViewById(R.id.Note);
                noteTextView.setText("This food is safe to consume.");
                noteTextView.setTextColor(getResources().getColor(R.color.green));
                noteTextView.setBackgroundColor(getResources().getColor(R.color.black));
                TextView reminderTextView = dialogView.findViewById(R.id.reminder);
                reminderTextView.setVisibility(View.VISIBLE);

            } else {
                productName.setTextColor(getResources().getColor(R.color.red)); // Set color to red

                // Display a warning based on the triggering health condition
                String warningMessage = "Warning: This food is high in " + highIn + " might trigger " + triggeringCondition + ".";
                // Add a warning under the product name
                TextView warningTextView = dialogView.findViewById(R.id.Note);
                warningTextView.setText(warningMessage);
                warningTextView.setTextColor(getResources().getColor(R.color.warning));
                warningTextView.setBackgroundColor(getResources().getColor(R.color.black));
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView);
            productDialog = builder.create();
            Animation sideAnim = AnimationUtils.loadAnimation(this, R.anim.side_animation);
            dialogView.setAnimation(sideAnim);
            btnDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    productDialog.dismiss();
                    productDialog = null;
                }
            });
            productDialog.show();
        }
    }

    private void checkHealthCondition(int ID, List<NutritionItem> nutritionItemList) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, HEALTH_CONDITION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d("RawResponse", response); // Print the raw response

                            if (response.startsWith("<br")) {
                                // Handle unexpected response, it might be an error message or HTML content.
                                Log.e("Error", "Unexpected response format");
                            } else {
                                // Proceed with parsing as JSON
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    String result = jsonObject.getString("status");

                                    if (result.equals("success")) {
                                        // Extract data from the JSON response
                                        JSONObject data = jsonObject.getJSONObject("data");

                                        // Check for specific health conditions
                                        List<String> triggeringConditions = new ArrayList<>();
                                        List<String> highIn = new ArrayList<>();
                                        String isDiabeticString = data.getString("isDiabetic");
                                        boolean isDiabetic = isDiabeticString.equalsIgnoreCase("yes");

                                        String isHighBloodString = data.getString("isHighBlood");
                                        boolean isHighBlood = isHighBloodString.equalsIgnoreCase("yes");

                                        String hasHeartProblemString = data.getString("hasHeartProblem");
                                        boolean hasHeartProblem = hasHeartProblemString.equalsIgnoreCase("yes");

                                        String hasKidneyProblemString = data.getString("hasKidneyProblem");
                                        boolean hasKidneyProblem = hasKidneyProblemString.equalsIgnoreCase("yes");

                                        String isObeseString = data.getString("isObese");
                                        boolean isObese = isObeseString.equalsIgnoreCase("yes");

                                        // Analyze nutrition details based on health conditions
                                        if (isDiabetic) {
                                            analyzeForDiabetes(nutritionItemList);
                                        }

                                        if (isHighBlood) {
                                            analyzeForHighBloodPressure(nutritionItemList);
                                        }
                                        if (hasHeartProblem) {
                                            analyzeForHeartProblem(nutritionItemList);
                                        }
                                        if (hasKidneyProblem) {
                                            analyzeForKidneyProblem(nutritionItemList);
                                        }
                                        if (isObese) {
                                            analyzeForObese(nutritionItemList);
                                        } else {
                                            updateRecyclerView(nutritionItemList, true, "", "");
                                        }
                                        //updateRecyclerView(nutritionItemList, true, allTriggers,"");
                                    } else {
                                        // Handle the case where the status is not "success"
                                        String message = jsonObject.getString("message");
                                        Log.e("Error", "Server response: " + message);
                                    }
                                } catch (JSONException e) {
                                    Log.e("Error", "Error parsing JSON: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Error", "Exception: " + e.getMessage());
                        }
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VolleyError", "Error: " + error.getMessage());
                // Handle error response
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("ID", String.valueOf(ID));
                return params;
            }
        };

        // Set the retry policy and add the request to the queue
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(SelectImage.this);
        queue.add(stringRequest);
    }

    private void analyzeForDiabetes(List<NutritionItem> nutritionItemList) {
        boolean safeToConsume = true;
        String triggerConditions = "";
        List<String> triggerCondition = new ArrayList<>();
        List<String> highIn = new ArrayList<>();
        double servingSize = 0;
        // Find the serving size in the nutritionItemList
        for (NutritionItem item : nutritionItemList) {
            if ("No.Servings".equals(item.getLabel())) {
                servingSize = extractNumericValue(item.getValue());
                break;  // Exit the loop once the serving size is found
            }
        }
        for (NutritionItem item : nutritionItemList) {
            String key = item.getLabel();
            double value = extractNumericValue(item.getValue());

            switch (key) {
                case "Saturated Fat":
                    double diabetesSatFatsLimit = 10; // %
                    double satFat = (diabetesSatFatsLimit / 100) * avgCalorie; // kcal
                    double satFatSafeRange = satFat / 9; // grams
                    double satFatsValue = value * servingSize;
                    if (satFatsValue > satFatSafeRange) {
                        triggerCondition.add("diabetes");
                        highIn.add("Saturated Fats");
                        safeToConsume = false;
                    }
                    break;
                case "Total Carbohydrate":
                    double maxCarbohydrates = 60; // 60g
                    double carbohydrateValue = value * servingSize;
                    if (carbohydrateValue > maxCarbohydrates) {
                        triggerCondition.add("diabetes");
                        highIn.add("carbohydrates");
                        safeToConsume = false;
                    }
                    break;

                case "Added Sugar":
                case "Sugar":
                    double addedSugarSafeRange = 25; //25g
                    double addedSugarValue = value * servingSize;
                    if (addedSugarValue > addedSugarSafeRange) {
                        triggerCondition.add("diabetes");
                        highIn.add("added sugar");
                        safeToConsume = false;
                    }
                    break;

                case "Sodium":
                    double sodiumSafeRange = 1500; //1500mg
                    double sodiumValue = value * servingSize;
                    if (sodiumValue > sodiumSafeRange) {
                        triggerCondition.add("diabetes");
                        highIn.add("salt/sodium");
                        safeToConsume = false;
                    }
                    break;
                default:
                    break;
            }
        }
        if (triggerCondition.contains("diabetes")) {
            triggerConditions = triggerCondition.toString();
        }
        String allhighIn = TextUtils.join(", ", highIn);
        updateRecyclerView(nutritionItemList, safeToConsume, triggerConditions, allhighIn);
        Log.e("Trigger", "Trigger List: " + triggerConditions);
        Log.e("Trigger", "high in List: " + allhighIn);
        Log.e("Trigger", "Safe to Consume?: " + safeToConsume);
        Log.e("Trigger", "No. of servings: " + servingSize);

    }

    private void analyzeForHighBloodPressure(List<NutritionItem> nutritionItemList) {
        boolean safeToConsume = true;
        List<String> triggerCondition = new ArrayList<>();
        List<String> highIn = new ArrayList<>();
        String triggerConditions = "";
        double servingSize = 0;
        for (NutritionItem item : nutritionItemList) {
            if ("No.Servings".equals(item.getLabel())) {
                servingSize = extractNumericValue(item.getValue());
                break;  // Exit the loop once the serving size is found
            }
        }
        for (NutritionItem item : nutritionItemList) {
            String key = item.getLabel();
            double value = extractNumericValue(item.getValue());
            switch (key) {
                case "Category":
                    if (key == "Alcohol") {
                        triggerCondition.add("High Blood / Hypertension");
                        highIn.add("Alcohol");
                        safeToConsume = false;
                    }
                    break;
                case "Total Fat":
                    double highBloodTotalFatLimit = 10; //%
                    double totalFat = (highBloodTotalFatLimit / 100) * avgCalorie;
                    double totalFatSafeRange = totalFat / 9;
                    double totalFatValue = value * servingSize;
                    if (totalFatValue > totalFatSafeRange) {
                        triggerCondition.add("High Blood / Hypertension");
                        highIn.add("Fat");
                        safeToConsume = false;
                    }
                case "Saturated Fat":
                    double highBloodSatFatsLimit = 10; // %
                    double satFat = (highBloodSatFatsLimit / 100) * avgCalorie; // kcal
                    double satFatSafeRange = satFat / 9; // grams
                    double satFatsValue = value * servingSize;
                    if (satFatsValue > satFatSafeRange) {
                        triggerCondition.add("High Blood / Hypertension");
                        highIn.add("Saturated Fats");
                        safeToConsume = false;
                    }
                    break;
                case "Total Carbohydrate":
                    double maxCarbohydrates = 60; //60g
                    double carbohydratesValue = value * servingSize;
                    if (carbohydratesValue > maxCarbohydrates) {
                        triggerCondition.add("High Blood / Hypertension");
                        highIn.add("carbohydrates");
                        safeToConsume = false;
                    }
                    break;
                case "Added Sugar":
                case "Sugar":
                    double sugarSafeRange = 25; //25g
                    double sugarValue = value * servingSize;
                    if (sugarValue > sugarSafeRange) {
                        triggerCondition.add("High Blood / Hypertension");
                        highIn.add("added sugar");
                        safeToConsume = false;
                    }
                    break;
                case "Sodium":
                    double sodiumSafeRange = 1500; //1500mg
                    double sodiumValue = value * servingSize;
                    if (sodiumValue > sodiumSafeRange) {
                        triggerCondition.add("High Blood / Hypertension");
                        highIn.add("salt/sodium");
                        safeToConsume = false;
                    }
                    break;
                default:
                    break;
            }
            if (triggerCondition.contains("High Blood / Hypertension")) {
                triggerConditions = triggerCondition.toString();
            }
            String allhighIn = TextUtils.join(", ", highIn);
            updateRecyclerView(nutritionItemList, safeToConsume, triggerConditions, allhighIn);
            Log.e("Trigger", "Trigger List: " + triggerConditions);
            Log.e("Trigger", "high in List: " + allhighIn);
            Log.e("Trigger", "Safe to Consume?: " + safeToConsume);
            Log.e("Trigger", "No. of servings: " + servingSize);

        }
    }

    private void analyzeForHeartProblem(List<NutritionItem> nutritionItemList) {
        boolean safeToConsume = true;
        List<String> triggerCondition = new ArrayList<>();
        List<String> highIn = new ArrayList<>();
        String triggerConditions = "";
        double servingSize = 0;  // Initialize to a default value or handle it based on your requirements

        // Find the serving size in the nutritionItemList
        for (NutritionItem item : nutritionItemList) {
            if ("No.Servings".equals(item.getLabel())) {
                servingSize = extractNumericValue(item.getValue());
                break;  // Exit the loop once the serving size is found
            }
        }

        for (NutritionItem item : nutritionItemList) {
            String key = item.getLabel();
            double value = extractNumericValue(item.getValue());

            switch (key) {
                case "Saturated Fat":
                    double heartDiseaseSatFatsLimit = 7; // %
                    double satFat = (heartDiseaseSatFatsLimit / 100) * avgCalorie; // kcal
                    double satFatSafeRange = satFat / 9; // grams
                    double satFatsValue = value * servingSize;
                    if (satFatsValue > satFatSafeRange) {
                        triggerCondition.add("Heart Problem");
                        highIn.add("Saturated Fats");
                        safeToConsume = false;
                    }
                    break;
                case "Added Sugar":
                    double sugarSafeRange = 25; //25g
                    double sugarValue = value * servingSize;
                    if (sugarValue > sugarSafeRange) {
                        triggerCondition.add("Heart Problem");
                        highIn.add("added sugar");
                        safeToConsume = false;
                    }
                    break;
                case "Sodium":
                    double sodiumSafeRange = 1500; //1500mg
                    double sodiumValue = value * servingSize;
                    if (sodiumValue > sodiumSafeRange) {
                        triggerCondition.add("Heart Problem");
                        highIn.add("salt/sodium");
                        safeToConsume = false;
                    }
                    break;
                case "TransFat":
                    double transFatRange = 0; // 0 Consume is safe
                    double transFatValue = value * servingSize;
                    if (transFatRange > transFatRange) {
                        triggerCondition.add("Heart problem");
                        highIn.add("trans fat");
                        safeToConsume = false;
                    }
                    break;
                default:
                    break;
            }
            if (triggerCondition.contains("Heart problem")) {
                triggerConditions = triggerCondition.toString();
            }
            String allhighIn = TextUtils.join(", ", highIn);
            updateRecyclerView(nutritionItemList, safeToConsume, triggerConditions, allhighIn);

            Log.e("Trigger", "Trigger List: " + triggerConditions);
            Log.e("Trigger", "high in List: " + allhighIn);
            Log.e("Trigger", "Safe to Consume?: " + safeToConsume);
            Log.e("Trigger", "No. of servings: " + servingSize);
        }
    }

    private void analyzeForKidneyProblem(List<NutritionItem> nutritionItemList) {
        boolean safeToConsume = true;
        List<String> triggerCondition = new ArrayList<>();
        List<String> highIn = new ArrayList<>();
        String triggerConditions = "";
        double servingSize = 0;
        for (NutritionItem item : nutritionItemList) {
            if ("No.Servings".equals(item.getLabel())) {
                servingSize = extractNumericValue(item.getValue());
                break;  // Exit the loop once the serving size is found
            }
        }
        for (NutritionItem item : nutritionItemList) {
            String key = item.getLabel();
            double value = extractNumericValue(item.getValue());
            switch (key) {
                case "Sodium":
                    double sodiumSafeRange = 1500; //1500mg
                    double sodiumValue = value * servingSize;
                    if (sodiumValue > sodiumSafeRange) {
                        triggerCondition.add("kidney problem");
                        highIn.add("sodium");
                        safeToConsume = false;
                    }
                    break;
                case "Potassium":
                    double potassiumSafeRange = 2500; // mg
                    double potassiumValue = value * servingSize;
                    if (potassiumValue > potassiumSafeRange) {
                        triggerCondition.add("kidney problem");
                        highIn.add("potassium");
                        safeToConsume = false;
                    }
                    break;
                case "Phosphorus":
                    double phosphorousSafeRange = 800; // mg
                    double phosphorousValue = value * servingSize;
                    if (phosphorousValue > phosphorousValue) {
                        triggerCondition.add("kidney problem");
                        highIn.add("potassium");
                        safeToConsume = false;
                    }
                    break;
                case "Protein":
                    double proteinSafeRange = 0.8; // grams
                    //TODO FETCH WEIGHT OF THE PERSON AND DIVIDE IT TO THE WEIGHT OF PERSON IT SHOULD BE 0.8 GRAMS PER KILOGRAM OF BODY
                    break;
                case "Calcium":
                    double calciumSafeRange = 1000; //mg
                    double calciumValue = value * servingSize;
                    if (calciumValue > calciumSafeRange) {
                        triggerCondition.add("kidney problem");
                        highIn.add("calcium");
                        safeToConsume = false;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void analyzeForObese(List<NutritionItem> nutritionItemList) {
        boolean safeToConsume = true;
        List<String> triggerCondition = new ArrayList<>();
        List<String> highIn = new ArrayList<>();
        String triggerConditions = "";
        double servingSize = 0;  // Initialize to a default value or handle it based on your requirements

        // Find the serving size in the nutritionItemList
        for (NutritionItem item : nutritionItemList) {
            if ("No.Servings".equals(item.getLabel())) {
                servingSize = extractNumericValue(item.getValue());
                break;  // Exit the loop once the serving size is found
            }
        }

        for (NutritionItem item : nutritionItemList) {
            String key = item.getLabel();
            double value = extractNumericValue(item.getValue());

            switch (key) {
                case "Calories":
                    double calorieSafeRange = 1000; // kcal
                    double calorieValue = value * servingSize;
                    if (calorieValue > calorieSafeRange) {
                        triggerCondition.add("Obesity");
                        highIn.add("Saturated Fats");
                        safeToConsume = false;
                    }
                    break;

                case "Saturated Fat":
                    double obeseSatFatsLimit = 6; // %
                    double satFat = (obeseSatFatsLimit / 100) * avgCalorie; // kcal
                    double satFatSafeRange = satFat / 9; // grams
                    double satFatsValue = value * servingSize;
                    if (satFatsValue > satFatSafeRange) {
                        triggerCondition.add("Obesity");
                        highIn.add("Saturated Fats");
                        safeToConsume = false;
                    }
                    break;
                case "Sugar":
                case "Added Sugar":
                    double sugarSafeRange = 25; //25g
                    double sugarValue = value * servingSize;
                    if (sugarValue > sugarSafeRange) {
                        triggerCondition.add("Obesity");
                        highIn.add("added sugar");
                        safeToConsume = false;
                    }
                    break;
                case "Sodium":
                    double sodiumSafeRange = 1500; //1500mg
                    double sodiumValue = value * servingSize;
                    if (sodiumValue > sodiumSafeRange) {
                        triggerCondition.add("Obesity");
                        highIn.add("salt/sodium");
                        safeToConsume = false;
                    }
                    break;
                case "TransFat":
                    double transFatRange = 0; // 0 Consume is safe
                    double transFatValue = value * servingSize;
                    if (transFatRange > transFatRange) {
                        triggerCondition.add("Obesity");
                        highIn.add("trans fat");
                        safeToConsume = false;
                    }
                    break;
                default:
                    break;
            }
            if (triggerCondition.contains("Heart problem")) {
                triggerConditions = triggerCondition.toString();
            }
            String allhighIn = TextUtils.join(", ", highIn);
            updateRecyclerView(nutritionItemList, safeToConsume, triggerConditions, allhighIn);

            Log.e("Trigger", "Trigger List: " + triggerConditions);
            Log.e("Trigger", "high in List: " + allhighIn);
            Log.e("Trigger", "Safe to Consume?: " + safeToConsume);
            Log.e("Trigger", "No. of servings: " + servingSize);
        }
    }

    private double extractNumericValue(String input) {
        // Assuming the input is in the format "123mg" or "456g" etc.
        String numericValue = input.replaceAll("[^\\d.]+", "");
        return Double.parseDouble(numericValue);
    }
}
