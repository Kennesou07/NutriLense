package org.nutrivision.bscs.capstone.detection;

import static android.widget.Toast.LENGTH_SHORT;

import static org.nutrivision.bscs.capstone.detection.API.*;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Login extends Activity {
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    ImageView googleBtn,closeBtn;
    Button login,sendEmail,backToLoginBtn;
    ImageView arrowBack;
    TextView forgotPass, register,linkSent;
    TextInputEditText editTextUsername,editTextPassword;
    TextInputLayout username,password, emailLayout;
    EditText editTextEmail;
    ProgressDialog progressDialog;
    InputMethodManager imm;
    CheckBox remember;
    int GREEN = Color.rgb(0, 210, 0);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        checkSession();
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);

        if(acc != null){
            goToLandingPage();
        }
        googleBtn = findViewById(R.id.googleBtn);
        arrowBack = findViewById(R.id.btnBack);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        editTextUsername = findViewById(R.id.emailEditText);
        editTextPassword = findViewById(R.id.passwordEditText);
        username = findViewById(R.id.textInputLayoutEmail);
        password = findViewById(R.id.textInputLayoutPassword);
        remember = findViewById(R.id.remember_me);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        login = findViewById(R.id.loginButton);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                if(username.isEmpty()){
                    editTextUsername.setError("Field cannot be empty");
                    editTextUsername.requestFocus();
                    imm.showSoftInput(editTextUsername,InputMethodManager.SHOW_IMPLICIT);
                }
                else if(password.isEmpty()){
                    editTextPassword.setError("Field cannot be empty");
                    editTextPassword.requestFocus();
                    imm.showSoftInput(editTextPassword,InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGIN_SITE,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject object = new JSONObject(response);
                                        String result = object.getString("status");
                                        if(result.equals("success")){
                                            progressDialog.dismiss();

                                            // Save session information
                                            SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = preferences.edit();
                                            editor.putString("username", username);
                                            editor.putString("password",password);
                                            editor.putBoolean("isLoggedIn", true);
                                            editor.apply();
                                            Map<String, ?> allEntries = preferences.getAll();

                                            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                                Log.d("SharedPreferences", entry.getKey() + ": " + entry.getValue().toString());
                                            }
                                            Toast.makeText(Login.this,"Login Success! Please wait.",LENGTH_SHORT).show();
                                            Intent intent = new Intent(Login.this,MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                        else{
                                            progressDialog.dismiss();
                                            Toast.makeText(Login.this,"Invalid Credentials!", LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                        }
                    }){
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String,String> params = new HashMap<>();
                            params.put("Username",username);
                            params.put("Password",password);
                            return params;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    RequestQueue queue = Volley.newRequestQueue(Login.this);
                    queue.add(stringRequest);
                }
            }
        });
        forgotPass = findViewById(R.id.forgotPass);
        forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showForgotPasswordDialog();
            }
        });
        register = findViewById(R.id.registerTextView);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login.this,SignUp.class);
                startActivity(intent);
                finish();
            }
        });
        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent googleSignIn = gsc.getSignInIntent();
                startActivityForResult(googleSignIn,1000);

            }
        });
        remember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    SharedPreferences remember = getSharedPreferences("LogInSession",MODE_PRIVATE);
                    SharedPreferences.Editor editor = remember.edit();
                    editor.putBoolean("remember",true);
                    editor.apply();
                }
                if(!compoundButton.isChecked()){
                    SharedPreferences remember = getSharedPreferences("LogInSession",MODE_PRIVATE);
                    SharedPreferences.Editor editor = remember.edit();
                    editor.putBoolean("remember",false);
                    editor.apply();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Save session information
            SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Intent intent = new Intent(Login.this,MainActivity.class);
            startActivity(intent);
            finish();
            // Signed in successfully, show authenticated UI.
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void checkSession(){
        SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            goToLandingPage();
        }
    }
    private void goToLandingPage(){
        Intent intent = new Intent(Login.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void showLinkSentDialog(String email){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.inflater_link_sent,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        final AlertDialog successDialog = builder.create();
        backToLoginBtn = dialogView.findViewById(R.id.btnBackToLogin);
        linkSent = dialogView.findViewById(R.id.link_sent_desc);
        linkSent.append(email);
        Animation popAnim = AnimationUtils.loadAnimation(this,R.anim.pop_animation);
        dialogView.setAnimation(popAnim);

        backToLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                successDialog.dismiss();
            }
        });
        successDialog.show();
    }
    private void showForgotPasswordDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.forgotpassword,null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();
        Animation popAnim = AnimationUtils.loadAnimation(this,R.anim.pop_animation);
        dialogView.setAnimation(popAnim);
        emailLayout = dialogView.findViewById(R.id.emailLayout);
        editTextEmail = dialogView.findViewById(R.id.edtTextEmail);
        sendEmail = dialogView.findViewById(R.id.btnSendEmail);
        closeBtn = dialogView.findViewById(R.id.closedBtn);

        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String email = editable.toString().trim();
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    emailLayout.setHelperText("Invalid email address!");
                    emailLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    emailLayout.setBoxStrokeColor(Color.RED);
                    editTextEmail.requestFocus();
                    imm.showSoftInput(editTextEmail,InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    emailLayout.setHelperText(null);
                    emailLayout.setBoxStrokeColor(GREEN);
                }
            }
        });
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    emailLayout.setBoxStrokeColor(Color.RED);
                    emailLayout.setHelperText("Cannot be empty!");
                    emailLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    editTextEmail.requestFocus();
                    imm.showSoftInput(editTextEmail,InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    String resetToken = TokenGenerator.generateToken();
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, FORGOT_SITE,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject jsonResponse = new JSONObject(response);
                                        String status = jsonResponse.getString("Status");
                                        String message = jsonResponse.getString("Message");
                                        if ("Success".equals(status)) {
                                            Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Toast.makeText(Login.this, "JSON parsing error", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                        }
                    }){
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String,String> params = new HashMap<>();
                            params.put("Email",email);
                            params.put("Token",resetToken);
                            return params;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    RequestQueue queue = Volley.newRequestQueue(Login.this);
                    queue.add(stringRequest);
                    alertDialog.dismiss();
                    showLinkSentDialog(email);
                }
            }
        });
        alertDialog.show();
    }
    public void callLoginScreen(View view){
        Intent intent = new Intent(Login.this, Welcome.class);
        Pair[] pairs = new Pair[1];
        pairs[0] = new Pair<View,String>(findViewById(R.id.btnBack),"login_transition");
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(Login.this,pairs);
        startActivity(intent,options.toBundle());
        finish();
    }
}