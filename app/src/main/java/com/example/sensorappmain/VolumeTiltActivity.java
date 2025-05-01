package com.example.sensorappmain;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class VolumeTiltActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private AudioManager audioManager;
    private TextView volumeLevelText;
    private ProgressBar circularProgressBar;
    private ImageView speakerIcon;
    private boolean isVolumeChanging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volume_tilt);

        volumeLevelText = findViewById(R.id.volumeLevelText);
        circularProgressBar = findViewById(R.id.circularProgressBar);
        speakerIcon = findViewById(R.id.speakerIcon);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Initialize progress bar max value
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        circularProgressBar.setMax(maxVolume);
        updateVolumeUI(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0]; // Tilt along x-axis
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // Adjust volume based on tilt
        if (x > 2 && currentVolume < maxVolume) {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            isVolumeChanging = true;
            updateVolumeUI(currentVolume + 1);
        } else if (x < -2 && currentVolume > 0) {
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            isVolumeChanging = true;
            updateVolumeUI(currentVolume - 1);
        } else {
            isVolumeChanging = false;
        }
    }

    private void updateVolumeUI(int volume) {
        // Update text
        volumeLevelText.setText("Volume: " + volume);

        // Update circular progress
        circularProgressBar.setProgress(volume);

        // Apply glow effect based on volume
        float glowIntensity = (float) volume / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        circularProgressBar.setAlpha(0.6f + glowIntensity * 0.4f); // Adjust transparency for glow effect

        // Animate speaker icon
        if (isVolumeChanging) {
            // Pulse animation
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
            speakerIcon.startAnimation(pulse);

            // Scale animation based on volume change
            float scale = 1.0f + (volume / (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) * 0.5f;
            speakerIcon.setScaleX(scale);
            speakerIcon.setScaleY(scale);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}