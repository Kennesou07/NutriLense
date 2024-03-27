package org.nutrivision.bscs.capstone.detection;

import static android.widget.Toast.LENGTH_SHORT;
import static org.nutrivision.bscs.capstone.detection.API.FEEDBACK;
import static org.nutrivision.bscs.capstone.detection.API.FORGOT_SITE;
import static org.nutrivision.bscs.capstone.detection.API.LOAD_PROFILE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Profile extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    String emailAddress = " support@nutrilense.ucc-bscs.com";
    private long INACTIVITY_DURATION = 20 * 1000; // 20 seconds in milliseconds
    Button btnEditProfile;
    TextView txtPassword, txtUsername, txtContact, txtAge, txtGender, txtEmail, txtName, txtHeight, txtWeight;
    ImageView imgProfile, imgContact, imgSettings, imgLogout;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    LinearLayout contentView;
    AlertDialog feedbackDialog, contactDialog, promptDialog;
    static final float END_SCALE = 0.7f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadDetails();
        setContentView(R.layout.profile);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
        btnEditProfile = findViewById(R.id.btnEditButton);
        txtEmail = findViewById(R.id.txtEmail);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtUsername = findViewById(R.id.txtShowUsername);
        txtContact = findViewById(R.id.txtShowMobile);
        txtAge = findViewById(R.id.txtShowAge);
        txtGender = findViewById(R.id.txtShowGender);
        txtHeight = findViewById(R.id.txtShowHeight);
        txtWeight = findViewById(R.id.txtShowWeight);
        imgProfile = findViewById(R.id.imgProfile);
        imgContact = findViewById(R.id.imgContact);
//        imgSettings = findViewById(R.id.imgSettings);
        imgLogout = findViewById(R.id.imgLogout);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        contentView = findViewById(R.id.content);

        /*-------------NAVIGATION DRAWER MENU---------------*/
//        Menu menu = navigationView.getMenu();
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_open, R.string.navigation_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_profile);
        animateNavigationDrawer();

        imgLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        imgContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showContact();
            }
        });
//       imgSettings.setOnClickListener(new View.OnClickListener() {
//           @Override
//           public void onClick(View view) {
//               showSettings();
//           }
//       });
    }

    private void loadDetails() {
        SharedPreferences getID = getSharedPreferences("LogInSession", MODE_PRIVATE);
        int ID = getID.getInt("userId", 0);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOAD_PROFILE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String result = jsonObject.getString("status");

                            if (result.equals("success")) {
                                // Extract data from the JSON response
                                JSONObject data = jsonObject.getJSONObject("data");
                                String name = data.getString("Name");
                                String email = data.getString("Email");
                                String username = data.getString("Username");
                                String contactNo = data.getString("Phone");
                                String age = data.getString("Age");
                                String gender = data.getString("Gender");
                                String height = data.getString("Height");
                                String weight = data.getString("Weight");
                                txtName.setText(name);
                                txtEmail.setText(email);
                                txtUsername.setText(username);
                                txtContact.append(contactNo);
                                txtAge.setText(age);
                                txtGender.setText(gender);
                                txtHeight.setText(height);
                                txtWeight.setText(weight);
                            } else {
                                // Handle the case where the status is not "success"
                                String message = jsonObject.getString("message");
                                Log.e("Error", "Server response: " + message);
                            }
                        } catch (JSONException e) {
                            Log.e("Error", "Error parsing JSON: " + e.getMessage());
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
                params.put("ID", String.valueOf(ID));
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(Profile.this);
        queue.add(stringRequest);
    }

    private void logout() {
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                clearPreferences();
                finish();
                startActivity(new Intent(Profile.this, Login.class));
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
        switch (item.getItemId()) {
            case R.id.nav_home:
                startActivity(new Intent(Profile.this, MainActivity.class));
                break;
            case R.id.nav_realtime:
                startActivity(new Intent(Profile.this, DetectorActivity.class));
                break;
            case R.id.nav_select_image:
                startActivity(new Intent(Profile.this, SelectImage.class));
                break;
            case R.id.nav_consumed:
                startActivity(new Intent(Profile.this, History.class));
                break;
            case R.id.nav_profile:
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

    private void showContact() {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.inflater_contact_us, null);
        Button send = dialogView.findViewById(R.id.sendBtn);
        Button exit = dialogView.findViewById(R.id.exitBtn);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(dialogView);
        contactDialog = alertDialog.create();
        Animation popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_animation);
        dialogView.setAnimation(popAnim);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectToGmail(emailAddress);
            }
        });
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contactDialog.dismiss();
                contactDialog = null;
            }
        });
        contactDialog.show();
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
                                        Toast.makeText(Profile.this, "Thank you for your feedback!", LENGTH_SHORT).show();
                                        feedbackDialog.dismiss();
                                    } else {
                                        Toast.makeText(Profile.this, "Try Again.", LENGTH_SHORT).show();
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
        RequestQueue queue = Volley.newRequestQueue(Profile.this);
        queue.add(stringRequest);
    }

    private void redirectToGmail(String recipientEmail) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipientEmail});

        PackageManager packageManager = getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No app to handle email", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApplication() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String message = "Hey, I found this cool app that can change the way you live by eating healthy products.!\n\nDownload link: https://nutrilense.ucc-bscs.com/";
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this app!");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showSettings() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.inflater_settings, null);
        ToggleButton toggleButton = dialogView.findViewById(R.id.toggleButton);
        TextView progressTextView = dialogView.findViewById(R.id.progressTextView);
        TextView idleTextView = dialogView.findViewById(R.id.idleTextView);
        Button save = dialogView.findViewById(R.id.btnSave);
        SeekBar exposureBar = dialogView.findViewById(R.id.exposureBar);
        SeekBar idleBar = dialogView.findViewById(R.id.idleBar);
        Animation popAnim = AnimationUtils.loadAnimation(this, R.anim.pop_animation);
        dialogView.setAnimation(popAnim);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView).setCancelable(false);
        promptDialog = builder.create();

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean flashState = prefs.getBoolean("flashState", false);
        int exposureVal = prefs.getInt("exposureState", 0);
        int idleVal = prefs.getInt("idleValue", 0);
        toggleButton.setChecked(flashState);
        exposureBar.setProgress(exposureVal);
        idleBar.setProgress(idleVal);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Save the state of the toggle button in SharedPreferences
                SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                editor.putBoolean("flashState", isChecked);
                editor.apply();
                // Send broadcast intent with the toggle action
                Intent toggleIntent = new Intent("TOGGLE_LIGHT_ACTION");
                toggleIntent.putExtra("FLASH_STATE", isChecked);
                sendBroadcast(toggleIntent);
                // Show toast or perform other actions as needed
                if (isChecked) {
                    Toast.makeText(Profile.this, "Flash On", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Profile.this, "Flash Off", Toast.LENGTH_SHORT).show();
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptDialog.dismiss();
                promptDialog = null;
            }
        });
        exposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.e("SeekBarChangeListener", "New exposure compensation: " + progress);

                // Save the state of the toggle button in SharedPreferences
                SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                editor.putInt("exposureState", progress);
                editor.apply();

                // Calculate the exposure compensation value based on progress
                Intent exposureIntent = new Intent("EXPOSURE_ACTION");
                progressTextView.setText(String.valueOf(progress));
                exposureIntent.putExtra("EXPOSURE_STATE", progress);
                Log.d("SeekBarChangeListener", "Sending intent with exposure state: " + progress);
                sendBroadcast(exposureIntent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progressTextView.setVisibility(View.INVISIBLE);

            }
        });
        idleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int idle, boolean b) {
                SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                editor.putInt("idleValue", idle);
                editor.apply();

                idleTextView.setText(String.valueOf(idle) + "seconds");
                INACTIVITY_DURATION = idle * 1000;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                idleTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                idleTextView.setVisibility(View.INVISIBLE);
            }
        });
        promptDialog.show();
    }


}
