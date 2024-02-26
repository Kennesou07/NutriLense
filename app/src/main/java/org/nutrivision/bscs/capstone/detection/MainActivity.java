package org.nutrivision.bscs.capstone.detection;

import static android.widget.Toast.LENGTH_SHORT;
import static org.nutrivision.bscs.capstone.detection.API.FEEDBACK;
import static org.nutrivision.bscs.capstone.detection.API.GET_ID;
import static org.nutrivision.bscs.capstone.detection.API.HEALTH_CONDITION;
import static org.nutrivision.bscs.capstone.detection.API.SURVEY_SITE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SearchView;
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
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.CategoriesAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.CategoriesClass;
import org.nutrivision.bscs.capstone.detection.adapter.FeaturedAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.FeaturedClass;
import org.nutrivision.bscs.capstone.detection.adapter.mostViewedProductsAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Questions questionBank;
    RecyclerView featuredRecycler,mostViewedRecycler,categoriesRecycler;
    RecyclerView.Adapter adapterModel,adapterMostViewed,adapterCategories;
    ImageView imgIcon,imgBtn;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    static final float END_SCALE = 0.7f;
    LinearLayout contentView,realTimeBtn, selectImgBtn, productsBtn;
    TextView txtQuestionNo, txtQuestion,txtDesc,txtTitle, viewAllCategories;
    RadioButton radioYes,radioNo;
    RadioGroup radioGroupOptions;
    Button btnNext,done;
    AlertDialog successDialog,endSurveyDialog;
    SharedPreferences preferences;
    SearchView search;
    AlertDialog feedbackDialog;
    int userId;
    boolean isSurveyed;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
        isSurveyed = preferences.getBoolean("isSurveyed",false);


        /*-------------------HOOKS---------------*/
        featuredRecycler = findViewById(R.id.featured_recyclerView);
        mostViewedRecycler = findViewById(R.id.most_viewed_recyclerView);
        categoriesRecycler = findViewById(R.id.category_recyclerView);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        contentView = findViewById(R.id.content);
        realTimeBtn = findViewById(R.id.RealTimeBtn);
        selectImgBtn = findViewById(R.id.selectImageBtn);
        productsBtn = findViewById(R.id.ProductsBtn);
        viewAllCategories = findViewById(R.id.viewAllBtn);
        search = findViewById(R.id.searchView);

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
        realTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DetectorActivity.class));
            }
        });
        selectImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SelectImage.class));
            }
        });
        productsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO display all the products here
            }
        });

        viewAllCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,Categories.class));
            }
        });
        getID();

    }

    private void Recycler() {

        /*---------------FEATURED MODELS-------------*/
        featuredRecycler.setHasFixedSize(true);
        featuredRecycler.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));

        ArrayList<FeaturedClass> featuredModels = new ArrayList<>();
        featuredModels.add(new FeaturedClass(R.drawable.sample_image,"Model 35","Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."));
        featuredModels.add(new FeaturedClass(R.drawable.sample_image,"Model 295","Accuracy: 89.7% \nPrecision: 88.9% \nRecall: 77.1%"));
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

        /*--------------- CATEGORIES -------------*/

        ArrayList<CategoriesClass> categoriesHelperClasses = new ArrayList<>();
        categoriesHelperClasses.add(new CategoriesClass(R.drawable.vegetable_backdrop, R.drawable.vegetable, "Vegetables",R.string.vegetable_desc));
        categoriesHelperClasses.add(new CategoriesClass(R.drawable.fruit_backdrop, R.drawable.fruit, "Fruits",R.string.fruit_desc));
        categoriesHelperClasses.add(new CategoriesClass(R.drawable.raw_backdrop, R.drawable.raw, "Raw",R.string.raw_desc));
        categoriesHelperClasses.add(new CategoriesClass(R.drawable.processfood_backdrop, R.drawable.processfood, "Processed Foods",R.string.processfoods_desc));
        categoriesHelperClasses.add(new CategoriesClass(R.drawable.snack_backdrop, R.drawable.snack, "Snacks",R.string.snacks_desc));
        categoriesHelperClasses.add(new CategoriesClass(R.drawable.drinks_backdrop, R.drawable.drinks, "Drinks",R.string.drinks_desc));
        categoriesRecycler.setHasFixedSize(true);
        adapterCategories = new CategoriesAdapter(categoriesHelperClasses,this);
        categoriesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoriesRecycler.setAdapter(adapterCategories);
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
            case R.id.nav_profile:
                startActivity(new Intent(MainActivity.this, Profile.class));
                break;
            case R.id.nav_logout:
                logout();
                break;
            case R.id.nav_share:
                break;
            case R.id.nav_feedback:
                showFeedback();
                break;
            case R.id.nav_about:
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
    HashMap<String, String> userAnswers = new HashMap<>();

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
        userAnswers.put(currentQuestion, selectedAnswer); // Store the answer with question ID
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
                params.put("isSurveyed", "true");
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
        // Recaps all answers and counts the number of "No" answers
        int noCount = recapAnswers();

        // If the number of "No" answers is equal to 5, modify text and photo
        if (noCount != 5) {
            ifNotHealthy();
        }

        done.setOnClickListener(v -> onDoneButtonClick());
        endSurveyDialog.show();
    }
    private int recapAnswers() {
        int noCount = 0;
        for (String answer : userAnswers.values()) {
            if (answer.equals("No")) {
                noCount++;
            }
        }
        Log.d("SurveyData", "Number of No Answers: " + noCount);
        // Display or handle the count as needed
        return noCount;
    }
    private void ifNotHealthy() {
        txtTitle.setText("Unhealthy");
        txtTitle.setTextSize(16);
        txtDesc.setText(R.string.unhealthy);
        imgIcon.setImageResource(R.drawable.unhealthy_icon);
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
                                userId = object.getInt("userId");

                                // Save session information
                                SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt("userId", userId);
                                editor.apply();

                                Map<String, ?> allEntries = preferences.getAll();
                                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                    Log.d("SharedPreferences", entry.getKey() + ": " + entry.getValue().toString());
                                }
                                fetchIfSurvey();
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
    private void fetchIfSurvey() {
        SharedPreferences getID = getSharedPreferences("LogInSession",MODE_PRIVATE);
        int ID = getID.getInt("userId",0);
        Log.d("SURVEY","ID:" + ID);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, HEALTH_CONDITION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String result = jsonObject.getString("status");

                            if (result.equals("no_record")) {
                                // User does not have a record in the database, show the survey
                                showSurvey();
                                Log.d("SURVEY","this should not been called.");
                            } else if (result.equals("success")) {
                                Log.d("SURVEY","this has been called.");
                            } else {
                                // Handle other cases if needed
                            }
                        } catch (JSONException e) {
                            Log.e("Error", "Error parsing JSON: " + e.getMessage());
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
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000, // Timeout in milliseconds
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }
    private void showFeedback(){
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.inflater_feedback,null);
        TextInputEditText feedback = dialogView.findViewById(R.id.etFeedback);
        Button submit = dialogView.findViewById(R.id.submitBtn);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(dialogView);
        feedbackDialog = alertDialog.create();
        Animation popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_animation);
        dialogView.setAnimation(popAnim);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String feedbacks = feedback.getText().toString().trim();
                if (feedbacks.isEmpty() || feedbacks == ""){
                    feedback.setError("Missing Field*");
                    feedback.requestFocus();
                    imm.showSoftInput(feedback, InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    feedbackDialog.dismiss();
                    sendFeedback(feedbacks);
                }
            }
        });
        feedbackDialog.show();
    }
    private void sendFeedback(String feedback){
        SharedPreferences getID = getSharedPreferences("LogInSession", MODE_PRIVATE);
        int ID = getID.getInt("userId", 0);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, FEEDBACK,
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
                                        Toast.makeText(MainActivity.this, "Thank you for your feedback!", LENGTH_SHORT).show();
                                        feedbackDialog.dismiss();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Try Again.", LENGTH_SHORT).show();
                                        feedbackDialog.show();
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
                params.put("Feedback", feedback);
                return params;
            }
        };

        // Set the retry policy and add the request to the queue
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(stringRequest);
    }
}
