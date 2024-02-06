package org.nutrivision.bscs.capstone.detection;

import static org.nutrivision.bscs.capstone.detection.API.FORGOT_SITE;
import static org.nutrivision.bscs.capstone.detection.API.LOAD_PROFILE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Profile extends AppCompatActivity {
    Button btnEditProfile;
    TextView txtPassword, txtUsername, txtContact, txtAge, txtGender, txtEmail, txtName;
    ImageView imgProfile, imgContact, imgSettings, imgLogout;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadDetails();
        setContentView(R.layout.profile);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);
        btnEditProfile = findViewById(R.id.btnEditButton);
        txtEmail = findViewById(R.id.txtEmail);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtUsername = findViewById(R.id.txtShowUsername);
        txtContact = findViewById(R.id.txtShowMobile);
        txtAge = findViewById(R.id.txtShowAge);
        txtGender = findViewById(R.id.txtShowGender);
        imgProfile = findViewById(R.id.imgProfile);
        imgContact = findViewById(R.id.imgContact);
        imgSettings = findViewById(R.id.imgSettings);
        imgLogout = findViewById(R.id.imgLogout);
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
    }
    private void loadDetails(){
        SharedPreferences getID = getSharedPreferences("LogInSession",MODE_PRIVATE);
        int ID = getID.getInt("userId",0);
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
                                txtName.setText(name);
                                txtEmail.setText(email);
                                txtUsername.setText(username);
                                txtContact.append(contactNo);
                                txtAge.setText(age);
                                txtGender.setText(gender);
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
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
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
    private void showContact(){
        // TODO CREATE AN INFLATER FOR CONTACT
    }
}
