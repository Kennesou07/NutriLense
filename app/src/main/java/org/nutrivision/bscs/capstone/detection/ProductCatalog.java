package org.nutrivision.bscs.capstone.detection;

import static android.widget.Toast.LENGTH_SHORT;
import static org.nutrivision.bscs.capstone.detection.API.CONSUMPTIONS;
import static org.nutrivision.bscs.capstone.detection.API.FEEDBACK;
import static org.nutrivision.bscs.capstone.detection.API.PRODUCT;
import static org.nutrivision.bscs.capstone.detection.API.PRODUCT_CATEGORY;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.ConsumedAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.ConsumedItem;
import org.nutrivision.bscs.capstone.detection.adapter.ProductList;
import org.nutrivision.bscs.capstone.detection.adapter.ProductListAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductCatalog extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    RecyclerView productRecyclerView;
    static final float END_SCALE = 0.7f;
    private LinearLayout contentView;
    AlertDialog feedbackDialog;
    ProductListAdapter productAdapter;
    private List<ProductList> productList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_catalog);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        contentView = findViewById(R.id.content);
        productRecyclerView = findViewById(R.id.recyclerView);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductListAdapter(productList);
        productRecyclerView.setAdapter(productAdapter);

        /*----------------TOOLBAR---------------*/
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        /*-------------NAVIGATION DRAWER MENU---------------*/
//        Menu menu = navigationView.getMenu();
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_open, R.string.navigation_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
//        navigationView.setCheckedItem(R.id.nav_consumed);
        animateNavigationDrawer();
        displayProducts();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                startActivity(new Intent(ProductCatalog.this, MainActivity.class));
                break;
            case R.id.nav_realtime:
                startActivity(new Intent(ProductCatalog.this, DetectorActivity.class));
                break;
            case R.id.nav_select_image:
                startActivity(new Intent(ProductCatalog.this, SelectImage.class));
                break;
            case R.id.nav_consumed:
                startActivity(new Intent(ProductCatalog.this, History.class));
                break;
            case R.id.nav_profile:
                startActivity(new Intent(ProductCatalog.this, Profile.class));
                break;
            case R.id.nav_logout:
                logout();
                break;
            case R.id.nav_share:
                shareApplication();
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

    private void logout() {
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                clearPreferences();
                finish();
                startActivity(new Intent(ProductCatalog.this, Login.class));
            }
        });
    }

    private void clearPreferences() {
        SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    private void shareApplication() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String message = "Hey, I found this cool app that can change the way you live by eating healthy products.!\n\nDownload link: https://nutrilense.ucc-bscs.com/";
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this app!");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showFeedback() {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.inflater_feedback, null);
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
                if (feedbacks.isEmpty() || feedbacks == "") {
                    feedback.setError("Missing Field*");
                    feedback.requestFocus();
                    imm.showSoftInput(feedback, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    feedbackDialog.dismiss();
                    sendFeedback(feedbacks);
                }
            }
        });
        feedbackDialog.show();
    }

    private void sendFeedback(String feedback) {
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
                                        Toast.makeText(ProductCatalog.this, "Thank you for your feedback!", LENGTH_SHORT).show();
                                        feedbackDialog.dismiss();
                                    } else {
                                        Toast.makeText(ProductCatalog.this, "Try Again.", LENGTH_SHORT).show();
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
        RequestQueue queue = Volley.newRequestQueue(ProductCatalog.this);
        queue.add(stringRequest);
    }

    private void displayProducts(){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, PRODUCT,
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
                                        JSONObject dataObject = jsonObject.getJSONObject("data");

                                        // Assuming "Product Names" is the key containing the array of product names
                                        if (dataObject.has("Product Names")) {
                                            JSONArray productsArray = dataObject.getJSONArray("Product Names");

                                            // Now proceed with parsing the productsArray
                                            for (int i = 0; i < productsArray.length(); i++) {
                                                String productName = productsArray.getString(i);

                                                ProductList product = new ProductList(productName);
                                                productList.add(product);
                                            }

                                            // After parsing the data, you can update your RecyclerView
                                            updateRecyclerView(productList);
                                        } else {
                                            Log.e("Error", "Key 'Product Names' not found in data object");
                                        }
                                    } else {
                                        String message = jsonObject.getString("message");
                                        Log.e("Error", "Server response: " + message);
                                        TextView noProducts = findViewById(R.id.noRecordTextView);
                                        noProducts.setTextColor(Color.RED);
                                        noProducts.setVisibility(View.VISIBLE);
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
//                params.put("Category", String.valueOf(title.getText()));
                return params;
            }
        };

        // Set the retry policy and add the request to the queue
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(ProductCatalog.this);
        queue.add(stringRequest);
    }
    private void updateRecyclerView(List<ProductList> productList) {
        productAdapter.updateData(productList);
        productAdapter.notifyDataSetChanged();
    }
}
