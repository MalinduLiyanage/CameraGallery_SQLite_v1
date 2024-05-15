package com.testapp.gallery;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CaptureActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    Bitmap bitmap;
    TextView camBtn;
    PreviewView previewView;
    SQLiteHelper dbHandler;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    ProcessCameraProvider cameraProvider;
    String capturePath = null, latitudeString = null, longitudeString = null, timeStamp = null;
    boolean isBind = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_capture);

        if (!allPermissionsGranted()) {
            Intent intent = new Intent(CaptureActivity.this, PermissionActivity.class);
            startActivity(intent);
            finish();
        }

        dbHandler = new SQLiteHelper(CaptureActivity.this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        camBtn = findViewById(R.id.capture_btn);
        previewView = findViewById(R.id.previewView);

        startCamera();

        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBind){
                    captureAndSaveImage(CaptureActivity.this);
                }else{
                    camBtn.setText("Capture Image");
                    isBind = true;
                    bindPreview(cameraProvider);
                }

            }
        });

    }

    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider != null) {
                    imageCapture = new ImageCapture.Builder()
                            .build();
                    bindPreview(cameraProvider);
                } else {
                    Log.e(TAG, "Unable to retrieve camera provider.");
                    Toast.makeText(CaptureActivity.this, "Unable to start camera.", Toast.LENGTH_SHORT).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                Toast.makeText(CaptureActivity.this, "Error starting camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        //camBtn.setText("Click to Capture");
        Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }
    private void captureAndSaveImage(Context context) {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                if (cameraProvider != null) {
                    int locationPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION);
                    if (locationPermission != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(CaptureActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                        return;
                    }

                    timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = "IMG_" + timeStamp; // Generate a unique file name
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

                    File outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    File outputFile = new File(outputDirectory, fileName + ".jpg");

                    Location location = getLocation(context); // Implement getLocation() method below
                    if (location != null) {
                        contentValues.put(MediaStore.Images.Media.LATITUDE, location.getLatitude());
                        contentValues.put(MediaStore.Images.Media.LONGITUDE, location.getLongitude());

                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        latitudeString = String.valueOf(latitude);
                        longitudeString = String.valueOf(longitude);

                    }else{
                        latitudeString = "No Data";
                        longitudeString = "No Data";
                    }

                    ImageCapture.OutputFileOptions outputOptions = new ImageCapture
                            .OutputFileOptions
                            .Builder(outputFile)
                            .build();

                    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                            capturePath = outputFile.getAbsolutePath();
                            Log.d(TAG, "Image saved successfully: " + outputFile.getAbsolutePath());
                            Toast.makeText(CaptureActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
                            dbHandler.addCapture(capturePath, timeStamp, latitudeString, longitudeString);
                            processCapturePath(context);
                        }

                        @Override
                        public  void onError(ImageCaptureException exc) {
                            Log.e(TAG, "Error capturing image: " + exc.getMessage(), exc);
                            Toast.makeText(CaptureActivity.this, "Error capturing image.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error capturing image: " + e.getMessage(), e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
    private Location getLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                return location;
            } else {
                return null;
            }
        }
        return null;
    }
    private void processCapturePath(Context context) {
        if (capturePath != null) {
            Log.d(TAG, "Capture path: " + capturePath);
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Uri uri = Uri.fromFile(new File(capturePath));

                try {
                    bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));

                    cameraProvider.unbindAll();
                    camBtn.setText("Open Camera");
                    isBind = false;
                    latitudeString = null;
                    longitudeString = null;
                    timeStamp = null;

                } catch (IOException e) {
                    Log.e(TAG, "Error loading captured image: " + e.getMessage());
                    Toast.makeText(CaptureActivity.this, "Error loading image.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CaptureActivity.this, "Permission Problem", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(CaptureActivity.this, "Capture Path null", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean allPermissionsGranted() {
        String[] REQUIRED_PERMISSIONS = new String[]{
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        };

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}