package org.nutrivision.bscs.capstone.detection;


import static android.widget.Toast.LENGTH_LONG;

import static org.nutrivision.bscs.capstone.detection.API.EMAIL_SITE;
import static org.nutrivision.bscs.capstone.detection.API.SIGNUP_SITE;
import static org.nutrivision.bscs.capstone.detection.API.USERNAME_SITE;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignUp extends Activity {
    TextView titleText;
    TextInputLayout edtUsernameLayout, edtPasswordLayout,edtRepassLayout,edtNameLayout,edtAgeLayout,edtEmailLayout,edtGenderLayout,edtPhoneLayout;
    TextInputEditText edtUsername, edtPassword, edtRePass,edtName,edtAge,edtEmail,edtPhone;
    Button btnSignUp;
    ProgressDialog progressDialog;
    InputMethodManager imm;
    ImageView backButton;
    AutoCompleteTextView autoCompleteGender;
    String gender;
    int GREEN = Color.rgb(0, 210, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        //  EditText
        edtUsername = findViewById(R.id.editTextUsername);
        edtPassword = findViewById(R.id.editTextPassword);
        edtRePass = findViewById(R.id.editTextRePassword);
        edtName = findViewById(R.id.editTextName);
        edtAge = findViewById(R.id.editTextAge);
        edtEmail = findViewById(R.id.editTextEmail);
        edtPhone = findViewById(R.id.editTextPhone);

        //  Buttons
        btnSignUp = findViewById(R.id.SignUpBtn);
        backButton = findViewById(R.id.btnSignUpBack);
        titleText = findViewById(R.id.signUpTitle);

        //  TextInputLayout
        edtUsernameLayout = findViewById(R.id.textInputLayoutUsername);
        edtNameLayout = findViewById(R.id.textInputLayoutName);
        edtPasswordLayout = findViewById(R.id.textInputLayoutPassword);
        edtRepassLayout = findViewById(R.id.textInputLayoutRePassword);
        edtEmailLayout = findViewById(R.id.textInputLayoutEmail);
        edtGenderLayout = findViewById(R.id.textInputLayoutGender);
        edtAgeLayout = findViewById(R.id.textInputLayoutAge);
        edtPhoneLayout = findViewById(R.id.textInputLayoutPhone);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        // Spinner initialization
        String[] genderArray = new String[]{"Male","Female","Others"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderArray);
        autoCompleteGender = findViewById(R.id.spinnerGender);
        autoCompleteGender.setAdapter(genderAdapter);
        autoCompleteGender.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                gender = adapterView.getItemAtPosition(i).toString().trim();
            }
        });
        edtPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showReminderMessage(v);
                }
            }

            private void showReminderMessage(View view) {
                Snackbar snackbar = Snackbar.make(view, "Password should contain at least 1 uppercase letter and 1 special character", Snackbar.LENGTH_LONG);
                View snackbarView = snackbar.getView();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                params.gravity = Gravity.TOP;
                snackbarView.setLayoutParams(params);
                snackbar.show();
            }
        });

        edtUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String username = editable.toString().trim();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, USERNAME_SITE,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response.trim().equals("Exist!")) {
                                    edtUsernameLayout.setHelperText("Username Exist");
                                    edtUsernameLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                                    edtUsernameLayout.setBoxStrokeColor(Color.RED);
                                } else {
                                    edtUsernameLayout.setHelperText("Valid Username");
                                    edtUsernameLayout.setHelperTextColor(ColorStateList.valueOf(GREEN));
                                    edtUsernameLayout.setBoxStrokeColor(GREEN);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        message(error.getMessage());
                    }
                }){
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String,String> params = new HashMap<>();
                        params.put("Username",username);
                        return params;
                    }
                };
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                RequestQueue queue = Volley.newRequestQueue(SignUp.this);
                queue.add(stringRequest);
            }
        });
        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Not used in this example
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Check if the password is weak and update the helper text and box stroke color
                String password = charSequence.toString().trim();
                if (!isPasswordStrong(password)) {
                    String missingRequirements = getMissingRequirements(password);
                    edtPasswordLayout.setHelperText("Weak Password: " + missingRequirements);
                    edtPasswordLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtPasswordLayout.setBoxStrokeColor(Color.RED);
                } else {
                    // Password is strong, clear the helper text and set box stroke color to green
                    edtPasswordLayout.setHelperText("Strong Password");
                    edtPasswordLayout.setHelperTextColor(ColorStateList.valueOf(GREEN));
                    edtPasswordLayout.setBoxStrokeColor(GREEN);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not used in this example
            }
        });

        edtRePass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String password = editable.toString().trim();
                String repassword = editable.toString().trim();
                if (!isPasswordMatch(password,repassword)) {
                    edtRepassLayout.setHelperText("Password Mismatched!");
                    edtRepassLayout.setHelperTextColor(ColorStateList.valueOf(GREEN));
                    edtRepassLayout.setBoxStrokeColor(Color.RED);
                } else {
                    edtRepassLayout.setHelperText("Password Matched");
                    edtRepassLayout.setHelperTextColor(ColorStateList.valueOf(GREEN));
                    edtRepassLayout.setBoxStrokeColor(GREEN);
                }
            }
        });
        edtEmail.addTextChangedListener(new TextWatcher() {
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
                    edtEmailLayout.setHelperText("Invalid email address!");
                    edtEmailLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtEmailLayout.setBoxStrokeColor(Color.RED);
                }
                else{
                    StringRequest stringRequest = new StringRequest(Request.Method.POST, EMAIL_SITE,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    if (response.trim().equals("Exist!")) {
                                        edtEmailLayout.setHelperText("Email already exist!");
                                        edtEmailLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                                        edtEmailLayout.setBoxStrokeColor(Color.RED);
                                    } else {
                                        edtEmailLayout.setHelperText("Valid email");
                                        edtEmailLayout.setHelperTextColor(ColorStateList.valueOf(GREEN));
                                        edtUsernameLayout.setBoxStrokeColor(GREEN);
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                            message(error.getMessage());
                        }
                    }){
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String,String> params = new HashMap<>();
                            params.put("Email",email);
                            return params;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    RequestQueue queue = Volley.newRequestQueue(SignUp.this);
                    queue.add(stringRequest);
                }
            }
        });
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                final String username = edtUsername.getText().toString().trim();
                final String password = edtPassword.getText().toString().trim();
                final String repassword = edtRePass.getText().toString().trim();
                final String name = edtName.getText().toString().trim();
                final String age = edtAge.getText().toString().trim();
                final String email = edtEmail.getText().toString().trim();
                final String phone = edtPhone.getText().toString().trim();

                if(username.isEmpty()){
                    edtUsernameLayout.setHelperText("Required*");
                    edtUsernameLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtUsernameLayout.setBoxStrokeColor(Color.RED);
                    edtUsername.requestFocus();
                    imm.showSoftInput(edtUsername,InputMethodManager.SHOW_IMPLICIT);
                }
                if(password.isEmpty()){
                    edtPasswordLayout.setHelperText("Required*");
                    edtPasswordLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtPasswordLayout.setBoxStrokeColor(Color.RED);
                    edtPassword.requestFocus();
                    imm.showSoftInput(edtPassword,InputMethodManager.SHOW_IMPLICIT);
                }
                if(repassword.isEmpty()){
                    edtRepassLayout.setHelperText("Required*");
                    edtRepassLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtRepassLayout.setBoxStrokeColor(Color.RED);
                    edtRePass.requestFocus();
                    imm.showSoftInput(edtRePass,InputMethodManager.SHOW_IMPLICIT);
                }
                if(name.isEmpty()){
                    edtNameLayout.setHelperText("Required*");
                    edtNameLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtNameLayout.setBoxStrokeColor(Color.RED);
                    edtName.requestFocus();
                    imm.showSoftInput(edtName,InputMethodManager.SHOW_IMPLICIT);
                }
                if(age.isEmpty()){
                    edtAgeLayout.setHelperText("Required*");
                    edtAgeLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtAgeLayout.setBoxStrokeColor(Color.RED);
                    edtAge.requestFocus();
                    imm.showSoftInput(edtAge,InputMethodManager.SHOW_IMPLICIT);
                }
                if(gender.isEmpty()){
                    edtGenderLayout.setHelperText("Required*");
                    edtGenderLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtGenderLayout.setBoxStrokeColor(Color.RED);
                }
                if(email.isEmpty()){
                    edtEmailLayout.setHelperText("Required*");
                    edtEmailLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtEmailLayout.setBoxStrokeColor(Color.RED);
                    edtEmail.requestFocus();
                    imm.showSoftInput(edtEmail,InputMethodManager.SHOW_IMPLICIT);
                }
                if(phone.isEmpty()){
                    edtPhoneLayout.setHelperText("Required*");
                    edtPhoneLayout.setHelperTextColor(ColorStateList.valueOf(Color.RED));
                    edtPhoneLayout.setBoxStrokeColor(Color.RED);
                    edtPhone.requestFocus();
                    imm.showSoftInput(edtPhone,InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    if(isPasswordMatch(password,repassword) && isPasswordStrong(password)){
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, SIGNUP_SITE,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        progressDialog.dismiss();
                                        Log.d("ServerSignUp", "Server Response: " + response);
                                        if (response.trim().equals("Success!")) {
                                            // Registration successful
                                            message("Registration Successful!");
                                            Intent intent = new Intent(SignUp.this,Login.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Registration failed
                                            message(response);
                                            edtEmail.requestFocus();
                                            imm.showSoftInput(edtEmail,InputMethodManager.SHOW_IMPLICIT);
                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                progressDialog.dismiss();
                                Log.e("SignUp", "Volley Error: " + error.toString());
                                message(error.getMessage());
                            }
                        }){
                            @Override
                            protected Map<String, String> getParams() throws AuthFailureError {
                                Map<String,String> params = new HashMap<>();

                                params.put("Username",username);
                                params.put("Password",password);
                                params.put("Name",name);
                                params.put("Age",age);
                                params.put("Gender",gender);
                                params.put("Email",email);
                                params.put("Phone",phone);
                                return params;
                            }
                        };
                        stringRequest.setRetryPolicy(new DefaultRetryPolicy(20000,
                                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                        RequestQueue queue = Volley.newRequestQueue(SignUp.this);
                        queue.add(stringRequest);
                    }
                    else{
                        if(!isPasswordMatch(password,repassword)){
                            edtRePass.requestFocus();
                            imm.showSoftInput(edtRePass,InputMethodManager.SHOW_IMPLICIT);
                            progressDialog.dismiss();
                            return;
                        }
                        else if(!isPasswordStrong(password)){
                            edtPassword.requestFocus();
                            imm.showSoftInput(edtPassword,InputMethodManager.SHOW_IMPLICIT);
                            progressDialog.dismiss();
                            return;
                        }
                    }
                }
            }
        });
    }
    public void callWelcomeScreen(View view){
        Intent intent = new Intent(SignUp.this,Welcome.class);
        Pair[] pairs = new Pair[1];
        pairs[0] = new Pair<View,String>(backButton,"signup_transition");
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(SignUp.this,pairs);
        startActivity(intent, options.toBundle());
        finish();
    }
    public void message(String message){
        Toast.makeText(this,message, LENGTH_LONG).show();
    }
    /*
        VALIDATION
     */
    private boolean isPasswordStrong(String password) {
        // Check for at least 8 characters
        if (password.length() < 8) {
            return false;
        }
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        // Check for at least one symbol (special character)
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*")) {
            return false;
        }
        return true;
    }

    private String getMissingRequirements(String password) {
        StringBuilder missingRequirements = new StringBuilder();

        // Check for at least 8 characters
        if (password.length() < 8) {
            missingRequirements.append("Minimum 8 characters,\n");
        }

        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            missingRequirements.append("Uppercase letter, \n");
        }

        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            missingRequirements.append("Numeric character, \n");
        }

        // Check for at least one symbol (special character)
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*")) {
            missingRequirements.append("Special character, \n");
        }

        // Remove the trailing comma and space
        if (missingRequirements.length() > 0) {
            missingRequirements.delete(missingRequirements.length() - 2, missingRequirements.length());
        }

        return missingRequirements.toString();
    }

    private boolean isPasswordMatch(String password, String repassword){
        // Use equals method to compare the text content of the EditTexts
        password = edtPassword.getText().toString().trim();
        repassword = edtRePass.getText().toString().trim();
        return password.equals(repassword);
    }
}