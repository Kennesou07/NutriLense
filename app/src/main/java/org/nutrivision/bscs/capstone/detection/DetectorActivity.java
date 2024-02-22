package org.nutrivision.bscs.capstone.detection;

import static org.nutrivision.bscs.capstone.detection.API.HEALTH_CONDITION;
import static org.nutrivision.bscs.capstone.detection.API.PRODUCT_DETAILS;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.adapter.DetectedObjectsAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionAdapter;
import org.nutrivision.bscs.capstone.detection.adapter.NutritionItem;
import org.nutrivision.bscs.capstone.detection.env.BorderedText;
import org.nutrivision.bscs.capstone.detection.env.ImageUtils;
import org.nutrivision.bscs.capstone.detection.env.Logger;
import org.nutrivision.bscs.capstone.detection.tflite.Classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nutrivision.bscs.capstone.detection.customview.OverlayView;
import org.nutrivision.bscs.capstone.detection.customview.OverlayView.DrawCallback;
import org.nutrivision.bscs.capstone.detection.tflite.DetectorFactory;
import org.nutrivision.bscs.capstone.detection.tflite.YoloV5Classifier;
import org.nutrivision.bscs.capstone.detection.tracking.MultiBoxTracker;
import org.w3c.dom.Text;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.7f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    private RecyclerView detectedObjectsRecyclerView;
    private DetectedObjectsAdapter detectedObjectsAdapter;
    private String prodName;
    private List<NutritionItem> nutritionItemList = new ArrayList<>();
    private double avgCalorie = 2000.00; // kcal unit

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
        // Initialize RecyclerView and its adapter
        detectedObjectsRecyclerView = findViewById(R.id.detectedObjectsRecyclerView);
        detectedObjectsAdapter = new DetectedObjectsAdapter();
        detectedObjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        detectedObjectsRecyclerView.setAdapter(detectedObjectsAdapter);
        detectedObjectsAdapter.setOnItemClickListener(new DetectedObjectsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String objectName) {
                Toast.makeText(DetectorActivity.this, "Clicked: " + objectName, Toast.LENGTH_SHORT).show();
                Log.d("DetectedObjectsAdapter", "Item clicked: " + objectName);
                displayInformation(objectName);
            }
        });
    }

    protected void updateActiveModel() {
        // Get UI information before delegating to background
        final int modelIndex = modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();
        String threads = threadsTextView.getText().toString().trim();
        final int numThreads = Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice
                    && numThreads == currentNumThreads) {
                return;
            }
            currentModel = modelIndex;
            currentDevice = deviceIndex;
            currentNumThreads = numThreads;

            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                // Customize the interpreter to the type of device we want to use.
                if (detector == null) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            if (device.equals("CPU")) {
                detector.useCPU();
            } else if (device.equals("GPU")) {
                detector.useGpu();
            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
            }
            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        Log.e("CHECK", "run: " + results.size());

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");
                                    }
                                });
                        // Update RecyclerView with detected objects
                        runOnUiThread(() -> detectedObjectsAdapter.updateData(mappedRecognitions));
                        Log.d("DetectorActivity", "data size: " + mappedRecognitions.size());
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
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
        RequestQueue queue = Volley.newRequestQueue(DetectorActivity.this);
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
        RequestQueue queue = Volley.newRequestQueue(DetectorActivity.this);
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
            if (triggerCondition.contains("kidney problem")) {
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
