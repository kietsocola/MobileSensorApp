package com.example.sensorappmain;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Landmark;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HandWaveActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "HandWaveActivity";
    private ImageView lightIndicator;
    private boolean isLightOn = false;
    private HandLandmarker handLandmarker;
    private long lastWaveTime = 0;
    private static final long WAVE_COOLDOWN_MS = 2000; // 2-second cooldown
    private float lastWristX = -1;
    private int waveCount = 0;
    private static final int WAVE_THRESHOLD = 3; // Number of direction changes for a wave
    private static final float X_MOVEMENT_THRESHOLD = 0.1f; // Normalized x-coordinate change

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hand_wave);

        lightIndicator = findViewById(R.id.lightIndicator);
        PreviewView previewView = findViewById(R.id.previewView);

        // Initialize MediaPipe HandLandmarker
        try {
            Log.d(TAG, "Initializing MediaPipe HandLandmarker");
            handLandmarker = HandLandmarker.createFromOptions(
                    this,
                    HandLandmarker.HandLandmarkerOptions.builder()
                            .setBaseOptions(
                                    com.google.mediapipe.tasks.core.BaseOptions.builder()
                                            .setModelAssetPath("hand_landmarker.task")
                                            .build())
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setNumHands(1)
                            .setResultListener(this::processHandLandmarkerResult)
                            .setErrorListener(this::onMediaPipeError)
                            .build()
            );
            Log.d(TAG, "MediaPipe HandLandmarker initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipe", e);
            Toast.makeText(this, "Failed to initialize MediaPipe: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted, starting camera");
            startCamera(previewView);
        }
    }

    private void startCamera(PreviewView previewView) {
        Log.d(TAG, "Starting camera");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                Executor analysisExecutor = Executors.newSingleThreadExecutor();
                imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                Log.d(TAG, "Camera started successfully");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (handLandmarker == null) {
            Log.w(TAG, "HandLandmarker is null, skipping analysis");
            imageProxy.close();
            return;
        }

        try {
            Log.d(TAG, "Analyzing image frame");
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = toBitmap(imageProxy);
            if (bitmap == null) {
                Log.w(TAG, "Failed to convert ImageProxy to Bitmap");
                imageProxy.close();
                return;
            }

            // Convert Bitmap to MPImage
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            Log.d(TAG, "Converted ImageProxy to MPImage");

            // Process image with HandLandmarker
            handLandmarker.detectAsync(mpImage, System.currentTimeMillis());
            Log.d(TAG, "Image sent to HandLandmarker for processing");

            // Recycle Bitmap to avoid memory leaks
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error during image analysis", e);
        } finally {
            imageProxy.close();
        }
    }

    private Bitmap toBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes.length == 0) {
            Log.w(TAG, "No planes in ImageProxy");
            return null;
        }

        java.nio.ByteBuffer yBuffer = planes[0].getBuffer(); // Y plane
        java.nio.ByteBuffer uBuffer = planes[1].getBuffer(); // U plane
        java.nio.ByteBuffer vBuffer = planes[2].getBuffer(); // V plane

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y, U, V data into NV21 format
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();

        // Convert NV21 to Bitmap
        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();

        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.d(TAG, "Converted ImageProxy to Bitmap: " + (bitmap != null ? "success" : "failed"));
        return bitmap;
    }

    private void processHandLandmarkerResult(HandLandmarkerResult result, MPImage mpImage) {
        if (result == null || result.landmarks().isEmpty()) {
            Log.d(TAG, "No hands detected in frame");
            lastWristX = -1;
            waveCount = 0;
            mpImage.close();
            return;
        }

        // Get wrist landmark (index 0)
        List<NormalizedLandmark> landmarks = result.landmarks().get(0);
        float wristX = landmarks.get(0).x();
        Log.d(TAG, "Hand detected, wrist x-coordinate: " + wristX);

        // Detect waving gesture
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWaveTime < WAVE_COOLDOWN_MS) {
            Log.d(TAG, "Wave ignored due to cooldown");
            mpImage.close();
            return; // Prevent multiple triggers
        }

        if (lastWristX != -1) {
            float deltaX = wristX - lastWristX;
            Log.d(TAG, "Wrist x movement: deltaX = " + deltaX);
            if (Math.abs(deltaX) > X_MOVEMENT_THRESHOLD) {
                waveCount++;
                Log.d(TAG, "Wave count incremented: " + waveCount);
                if (waveCount >= WAVE_THRESHOLD) {
                    // Wave detected
                    Log.d(TAG, "Wave detected, toggling light");
                    runOnUiThread(() -> {
                        isLightOn = !isLightOn;
                        lightIndicator.setImageResource(isLightOn ? R.drawable.ic_light_on : R.drawable.ic_light_off);
                        Toast.makeText(this, "Light " + (isLightOn ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                    });
                    lastWaveTime = currentTime;
                    waveCount = 0;
                }
            }
        } else {
            Log.d(TAG, "Initial wrist x-coordinate set");
        }

        lastWristX = wristX;
        mpImage.close();
    }

    private void onMediaPipeError(Exception e) {
        Log.e(TAG, "MediaPipe error occurred", e);
        runOnUiThread(() -> {
            Toast.makeText(this, "MediaPipe error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted, starting camera");
            startCamera(findViewById(R.id.previewView));
        } else {
            Log.w(TAG, "Camera permission denied");
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handLandmarker != null) {
            Log.d(TAG, "Closing HandLandmarker");
            handLandmarker.close();
        }
    }
}
