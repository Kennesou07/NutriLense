package org.nutrivision.bscs.capstone.detection;

import static org.nutrivision.bscs.capstone.detection.API.PRODUCT_CATEGORY;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionItem;
import org.nutrivision.bscs.capstone.detection.adapter.ProductList;
import org.nutrivision.bscs.capstone.detection.adapter.ProductListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class categoriesList extends AppCompatActivity {
    TextView title, description,availability;
    ImageView image;
    RecyclerView recyclerView;
    ProductListAdapter adapter;
    List<ProductList> productList = new ArrayList<>(); // Declare this as a class field
    ImageView arrowBack;
    LinearLayout background;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_list);

        /*------------ HOOKS -----------*/
        title = findViewById(R.id.title);
        description = findViewById(R.id.desc);
        image = findViewById(R.id.imgView);
        recyclerView = findViewById(R.id.recyclerView);
        arrowBack = findViewById(R.id.arrowBackBtn);
        background = findViewById(R.id.linearLayout);
        availability = findViewById(R.id.availability);

        /*------------ USE -----------*/
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            title.setText(bundle.getString("Title"));
            image.setImageResource(bundle.getInt("Image"));
            description.setText(getString(bundle.getInt("Description")));
            background.setBackgroundResource(bundle.getInt("Background"));
        }
        arrowBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoriesList.super.onBackPressed();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductListAdapter(productList);
        recyclerView.setAdapter(adapter);
        displayRecyclerView();
    }
    private void displayRecyclerView() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, PRODUCT_CATEGORY,
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
                                        availability.setTextColor(Color.RED);
                                        availability.setText("Not Available");
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
                params.put("Category", String.valueOf(title.getText()));
                return params;
            }
        };

        // Set the retry policy and add the request to the queue
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(categoriesList.this);
        queue.add(stringRequest);
    }

    // Update RecyclerView with the new data
    private void updateRecyclerView(List<ProductList> productList) {
        // Assuming you have a RecyclerView and its adapter already set up
        // Update the adapter with the new data and notify the changes
        adapter.updateData(productList);
        adapter.notifyDataSetChanged();
    }

}