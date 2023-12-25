package org.nutrivision.bscs.capstone.detection;

import static android.widget.Toast.LENGTH_SHORT;
import static org.nutrivision.bscs.capstone.detection.API.GET_ID;
import static org.nutrivision.bscs.capstone.detection.API.SURVEY_SITE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
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
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Questions questionBank;
    RecyclerView featuredRecycler,mostViewedRecycler;
    RecyclerView.Adapter adapterModel,adapterMostViewed,adapterCategories;
    ImageView imgIcon;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    static final float END_SCALE = 0.7f;
    LinearLayout contentView;
    TextView txtQuestionNo, txtQuestion,txtDesc,txtTitle;
    RadioButton radioYes,radioNo;
    RadioGroup radioGroupOptions;
    Button btnNext,done;
    AlertDialog successDialog,endSurveyDialog;
    SharedPreferences preferences;
    boolean isSurveyed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
        isSurveyed = preferences.getBoolean("isSurveyed",false);
        getID();
        if(!isSurveyed){
            showSurvey();
        }
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
    public void showSurvey(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.survey_inflater,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView).setCancelable(false);
        successDialog = builder.create();
        txtQuestion = dialogView.findViewById(R.id.txtQuestion);
        txtQuestionNo = dialogView.findViewById(R.id.txtQuestionNumber);
        radioGroupOptions = dialogView.findViewById(R.id.radioGroupOptions);
        radioNo = dialogView.findViewById(R.id.radioNo);
        radioYes = dialogView.findViewById(R.id.radioYes);
        btnNext = dialogView.findViewById(R.id.btnNext);
        questionBank = new Questions();
        Animation popAnim = AnimationUtils.loadAnimation(this,R.anim.pop_animation);
        dialogView.setAnimation(popAnim);
        updateQuestion();
        successDialog.show();
        btnNext.setOnClickListener(v -> onNextButtonClick());
    }
    private void updateQuestion(){
        String currentQuestion = questionBank.getCurrentQuestion();
        Log.d("SurveyData", "Updating question: " + currentQuestion);
        txtQuestion.setText(currentQuestion);
        String[] choices = questionBank.getChoices();
        radioYes.setText(choices[0]);
        radioNo.setText(choices[1]);
        radioGroupOptions.clearCheck();
        // Update txtQuestionNumber
        int currentPos = Questions.currentQuestionIndex + 1;
        int questionSize = questionBank.getQuestionSize();
        txtQuestionNo.setText(String.format(Locale.getDefault(), "%d / %d", currentPos, questionSize));
    }

    private void onNextButtonClick(){
        SharedPreferences getID = getSharedPreferences("LogInSession",MODE_PRIVATE);
        int ID = getID.getInt("userId",0);
        if (radioGroupOptions.getCheckedRadioButtonId() == -1) {
            Toast.makeText(MainActivity.this, "Please select an option", LENGTH_SHORT).show();
            return;
        }
        String currentQuestion = questionBank.getCurrentQuestion();
        // Save the answer to the current question
        String selectedAnswer = radioYes.isChecked() ? "Yes" : "No";
        Log.d("SurveyData", "Question: " + currentQuestion + ", Answer: " + selectedAnswer);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, SURVEY_SITE,
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
                                            // Save session information
                                            preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = preferences.edit();
                                            editor.putBoolean("isSurveyed", true);
                                            editor.apply();
                                            Map<String, ?> allEntries = preferences.getAll();

                                            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                                Log.d("SharedPreferences", entry.getKey() + ": " + entry.getValue().toString());
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.this, "Error: " + response, LENGTH_SHORT).show();
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
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("question",currentQuestion);
                params.put("answer",selectedAnswer);
                params.put("ID", String.valueOf(ID));
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(stringRequest);

        // Mark the current question as answered
        questionBank.setQuestionAnswered(true);
        questionBank.moveToNextQuestion();

        updateQuestion();
        if(questionBank.isLastQuestion()){
            Log.d("SurveyData", "Last question reached");
        }
        // Check if all questions are answered
        if (questionBank.areAllQuestionsAnswered()) {
            // Call onNextInflater or perform other actions when the survey is completed
            onNextInflater();
        }
    }
    private void onDoneButtonClick(){
        endSurveyDialog.dismiss();
    }
    private void onNextInflater(){
        successDialog.dismiss();
        View endSurveyView = getLayoutInflater().inflate(R.layout.survey_end_inflater,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(endSurveyView).setCancelable(false);
        endSurveyDialog = builder.create();
        txtTitle = endSurveyView.findViewById(R.id.txtTitle);
        txtDesc = endSurveyView.findViewById(R.id.txtDescription);
        imgIcon = endSurveyView.findViewById(R.id.imgIcon);
        done = endSurveyView.findViewById(R.id.btnDone);
        Animation popAnim = AnimationUtils.loadAnimation(this,R.anim.pop_animation);
        endSurveyView.setAnimation(popAnim);
        done.setOnClickListener(v -> onDoneButtonClick());
        endSurveyDialog.show();
    }
    private void getID() {
        SharedPreferences getID = getSharedPreferences("LogInSession",MODE_PRIVATE);
        String username = getID.getString("username","");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_ID,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject object = new JSONObject(response);
                            String result = object.getString("status");
                            if (result.equals("success")) {
                                // Get the user ID from the response
                                int userId = object.getInt("userId");

                                // Save session information
                                SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("userId", userId);
                                editor.apply();

                                Map<String, ?> allEntries = preferences.getAll();
                                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                    Log.d("SharedPreferences", entry.getKey() + ": " + entry.getValue().toString());
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Cannot fetch ID!", LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Username", username);
                return params;
            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(stringRequest);
    }
}
