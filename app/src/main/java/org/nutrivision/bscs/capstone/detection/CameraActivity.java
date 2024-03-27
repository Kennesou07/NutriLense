package org.nutrivision.bscs.capstone.detection;

import static android.widget.Toast.LENGTH_SHORT;
import static org.nutrivision.bscs.capstone.detection.API.FEEDBACK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.text.Layout;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.nutrivision.bscs.capstone.detection.R;
import org.nutrivision.bscs.capstone.detection.env.ImageUtils;
import org.nutrivision.bscs.capstone.detection.env.Logger;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
//        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener {
    BroadcastReceiver resetReceiver, stopReceiver, startReceiver;
    String cameraId;
    Fragment fragment;
    CameraManager manager;
    private static final Logger LOGGER = new Logger();
    private long lastDetectionTime = 0;
    private long INACTIVITY_DURATION = 20 * 1000; // 20 seconds in milliseconds
    private static final int MAX_PROMPT_COUNT = 3;
    private int promptCount = 0;
    private int minExposureCompensation = -4;
    private int initialExposureCompensation = 0;
    private int maxExposureCompensation = 4;
    private CountDownTimer inactivityTimer;
    private static final int PERMISSIONS_REQUEST = 1;
    private static final int FLASHLIGHT_PERMISSION_REQUEST_CODE = 2;
    AlertDialog feedbackDialog, promptDialog;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String ASSET_PATH = "";
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    protected Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    protected int defaultModelIndex = 0;
    protected int defaultDeviceIndex = 0;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    protected ArrayList<String> modelStrings = new ArrayList<String>();

    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;

    protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
    protected ImageView bottomSheetArrowImageView;
    private ImageView plusImageView, minusImageView;
    protected ListView deviceView;
    protected TextView threadsTextView;
    protected ListView modelView;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    static final float END_SCALE = 0.7f;
    protected GoogleSignInOptions gso;
    protected GoogleSignInClient gsc;
    protected CoordinatorLayout contentView;

    /**
     * Current indices of device and model.
     */
    int currentDevice = -1;
    int currentModel = -1;
    int currentNumThreads = -1;

    ArrayList<String> deviceStrings = new ArrayList<String>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        setContentView(R.layout.tfe_od_activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageView settings = findViewById(R.id.btnSettings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSettings();
            }
        });
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        startInactivityTimer();
        IntentFilter resetFilter = new IntentFilter("RESET_TIMER_ACTION");
        IntentFilter stopFilter = new IntentFilter("STOP_TIMER_ACTION");
        IntentFilter startFilter = new IntentFilter("START_TIMER_ACTION");
        IntentFilter toggleFilter = new IntentFilter("TOGGLE_LIGHT_ACTION");
        IntentFilter exposureFilter = new IntentFilter("EXPOSURE_ACTION");
        registerReceiver(toggleReceiver, toggleFilter);
        registerReceiver(exposureReceiver, exposureFilter);
        resetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                resetInactivityTimer();
                promptCount = 0;
            }
        };
        registerReceiver(resetReceiver, resetFilter);
        stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopInactivityTimer();
            }
        };
        registerReceiver(stopReceiver, stopFilter);
        startReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startInactivityTimer();
            }
        };
        registerReceiver(startReceiver, startFilter);
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        threadsTextView = findViewById(R.id.threads);
        currentNumThreads = Integer.parseInt(threadsTextView.getText().toString().trim());
        plusImageView = findViewById(R.id.plus);
        minusImageView = findViewById(R.id.minus);
        deviceView = findViewById(R.id.device_list);
        deviceStrings.add("CPU");
        deviceStrings.add("GPU");
        deviceStrings.add("NNAPI");
        deviceView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ArrayAdapter<String> deviceAdapter =
                new ArrayAdapter<>(
                        CameraActivity.this, R.layout.deviceview_row, R.id.deviceview_row_text, deviceStrings);
        deviceView.setAdapter(deviceAdapter);
        deviceView.setItemChecked(defaultDeviceIndex, true);
        currentDevice = defaultDeviceIndex;
        deviceView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateActiveModel();
                    }
                });

        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
        modelView = findViewById((R.id.model_list));

        modelStrings = getModelStrings(getAssets(), ASSET_PATH);
        modelView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ArrayAdapter<String> modelAdapter =
                new ArrayAdapter<>(
                        CameraActivity.this, R.layout.listview_row, R.id.listview_row_text, modelStrings);
        modelView.setAdapter(modelAdapter);
        modelView.setItemChecked(defaultModelIndex, true);
        currentModel = defaultModelIndex;
        modelView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateActiveModel();
                    }
                });

        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        //                int width = bottomSheetLayout.getMeasuredWidth();
                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.arrow_down);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.arrow_up);
                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.arrow_up);
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    }
                });

        frameValueTextView = findViewById(R.id.frame_info);
        cropValueTextView = findViewById(R.id.crop_info);
        inferenceTimeTextView = findViewById(R.id.inference_info);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);
        plusImageView.setOnClickListener(this);
        minusImageView.setOnClickListener(this);
        drawerLayout = findViewById(R.id.drawerLayout);
        contentView = findViewById(R.id.containerView);
        navigationView = findViewById(R.id.nav_view);
        /*-------------NAVIGATION DRAWER MENU---------------*/
//        Menu menu = navigationView.getMenu();
        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_open, R.string.navigation_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_realtime);
        animateNavigationDrawer();
        legacyCameraFragment = new LegacyCameraConnectionFragment(CameraActivity.this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    protected ArrayList<String> getModelStrings(AssetManager mgr, String path) {
        ArrayList<String> res = new ArrayList<String>();
        try {
            String[] files = mgr.list(path);
            for (String file : files) {
                String[] splits = file.split("\\.");
                if (splits[splits.length - 1].equals("tflite")) {
                    res.add(file);
                }
            }

        } catch (IOException e) {
            System.err.println("getModelStrings: " + e.getMessage());
        }
        return res;
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
        stopInactivityTimer();
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        stopInactivityTimer();
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        unregisterReceiver(resetReceiver);
        unregisterReceiver(startReceiver);
        unregisterReceiver(stopReceiver);
        unregisterReceiver(toggleReceiver);
        unregisterReceiver(exposureReceiver);
        stopInactivityTimer();
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }


    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                                CameraActivity.this,
                                "Camera permission is required for this demo",
                                Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        cameraId = chooseCamera();

        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        Log.e("CAMERA USED", String.valueOf(fragment));
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

//  @Override
//  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//    setUseNNAPI(isChecked);
//    if (isChecked) apiSwitchCompat.setText("NNAPI");
//    else apiSwitchCompat.setText("TFLITE");
//  }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads >= 9) return;
            numThreads++;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        } else if (v.getId() == R.id.minus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads == 1) {
                return;
            }
            numThreads--;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        }
    }

    protected void showFrameInfo(String frameInfo) {
        frameValueTextView.setText(frameInfo);
    }

    protected void showCropInfo(String cropInfo) {
        cropValueTextView.setText(cropInfo);
    }

    protected void showInference(String inferenceTime) {
        inferenceTimeTextView.setText(inferenceTime);
    }

    protected abstract void updateActiveModel();

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void setNumThreads(int numThreads);

    protected abstract void setUseNNAPI(boolean isChecked);

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                startActivity(new Intent(CameraActivity.this, MainActivity.class));
            case R.id.nav_realtime:
                break;
            case R.id.nav_select_image:
                startActivity(new Intent(CameraActivity.this, SelectImage.class));
                break;
            case R.id.nav_consumed:
                startActivity(new Intent(CameraActivity.this, History.class));
                break;
            case R.id.nav_profile:
                startActivity(new Intent(CameraActivity.this, Profile.class));
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
                startActivity(new Intent(CameraActivity.this, Login.class));
            }
        });
    }

    private void clearPreferences() {
        SharedPreferences preferences = getSharedPreferences("LogInSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
                                        Toast.makeText(CameraActivity.this, "Thank you for your feedback!", LENGTH_SHORT).show();
                                        feedbackDialog.dismiss();
                                    } else {
                                        Toast.makeText(CameraActivity.this, "Try Again.", LENGTH_SHORT).show();
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
        RequestQueue queue = Volley.newRequestQueue(CameraActivity.this);
        queue.add(stringRequest);
    }

    public void startInactivityTimer() {
        inactivityTimer = new CountDownTimer(INACTIVITY_DURATION, INACTIVITY_DURATION) {
            public void onTick(long millisUntilFinished) {
                // Timer is ticking, do nothing
            }

            public void onFinish() {
                promptCount++;
                // Timer has finished, show prompt
                if (promptCount >= MAX_PROMPT_COUNT) {
                    // Show message for 3rd prompt
                    showThirdPromptMessage();
                } else {
                    // Show next prompt
                    showInactivityPrompt();
                }
//        showInactivityPrompt();
            }
        }.start();
    }

    private void showInactivityPrompt() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.inflater_idle, null);
        Button keepEditing = dialogView.findViewById(R.id.keepBtn);
        Button exit = dialogView.findViewById(R.id.exitBtn);
        Animation sideAnim = AnimationUtils.loadAnimation(this, R.anim.side_animation);
        dialogView.setAnimation(sideAnim);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView).setCancelable(false);
        promptDialog = builder.create();
        keepEditing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptDialog.dismiss();
                promptDialog = null;
                resetInactivityTimer();
            }
        });
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptDialog.dismiss();
                promptDialog = null;
                startActivity(new Intent(CameraActivity.this, MainActivity.class));
                finish();
            }
        });
        promptDialog.show();
    }

    private void resetInactivityTimer() {
        if (!isFinishing()) {
            if (inactivityTimer != null) {
                inactivityTimer.cancel();
                startInactivityTimer();
            }
        }
    }

    private void showThirdPromptMessage() {
        if (!isFinishing()) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.inflater_nodetetion, null);
            Button sendFeedback = dialogView.findViewById(R.id.sendFeedbackBtn);
            Button exit = dialogView.findViewById(R.id.exitBtn);
            Animation wiggle = AnimationUtils.loadAnimation(this, R.anim.wiggle_animation);
            dialogView.setAnimation(wiggle);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(dialogView).setCancelable(false);
            promptDialog = builder.create();
            sendFeedback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showFeedback();
                }
            });
            exit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    promptDialog.dismiss();
                    promptDialog = null;
                    startActivity(new Intent(CameraActivity.this, MainActivity.class));
                    finish();
                }
            });
            promptDialog.show();
        }

    }

    private void stopInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
            inactivityTimer = null; // Reset the timer instance
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

    private LegacyCameraConnectionFragment legacyCameraFragment;

    private void showSettings() {
        stopInactivityTimer();
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
                    Toast.makeText(CameraActivity.this, "Flash On", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CameraActivity.this, "Flash Off", Toast.LENGTH_SHORT).show();
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptDialog.dismiss();
                promptDialog = null;
                startInactivityTimer();
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

    private BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract the toggle action from the intent and pass it to the fragment
            boolean isFlashOn = intent.getBooleanExtra("FLASH_STATE", false);
            LegacyCameraConnectionFragment fragment = (LegacyCameraConnectionFragment) getFragmentManager().findFragmentById(R.id.container);
            if (fragment != null) {
                try {
                    fragment.toggleFlash(isFlashOn);
                } catch (Exception e) {
                    e.getMessage();
                    e.printStackTrace();
                }
            }
        }
    };
    private BroadcastReceiver exposureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("ExposureReceiver", "Received intent: " + intent.getAction());

            // Extract the toggle action from the intent and pass it to the fragment
            int exposureVal = intent.getIntExtra("EXPOSURE_STATE", 0);
            Log.e("ExposureReceiver", "Exposure state: " + exposureVal);

            LegacyCameraConnectionFragment fragment = (LegacyCameraConnectionFragment) getFragmentManager().findFragmentById(R.id.container);
            if (fragment != null) {
                try {
                    fragment.changeExposure(exposureVal);
                } catch (Exception e) {
                    e.getMessage();
                    e.printStackTrace();
                }
            }
        }
    };
}
