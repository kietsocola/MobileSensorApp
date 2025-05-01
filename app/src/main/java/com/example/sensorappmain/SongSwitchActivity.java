package com.example.sensorappmain;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SongSwitchActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private MediaPlayer mediaPlayer;
    private ImageView vinylImage;
    private TextView songTitleText, artistText, currentDurationText, totalDurationText;
    private SeekBar durationSeekBar;
    private Button playPauseButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int currentSongIndex = 0;
    private boolean isPlaying = false;
    private final int[] songs = {R.raw.song1, R.raw.song2, R.raw.song3}; // Placeholder song resources
    private final String[] songTitles = {"Song 1", "Song 2", "Song 3"};
    private final String[] artists = {"Artist A", "Artist B", "Artist C"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_switch);

        vinylImage = findViewById(R.id.vinylImage);
        songTitleText = findViewById(R.id.songTitleText);
        artistText = findViewById(R.id.artistText);
        currentDurationText = findViewById(R.id.currentDurationText);
        totalDurationText = findViewById(R.id.totalDurationText);
        durationSeekBar = findViewById(R.id.durationSeekBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Initialize SeekBar
        durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateDurationText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Play/Pause button
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        // Start with first song
        playSong(currentSongIndex);
    }

    private void playSong(int index) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, songs[index]);
        mediaPlayer.setOnPreparedListener(mp -> {
            durationSeekBar.setMax(mediaPlayer.getDuration());
            totalDurationText.setText(formatDuration(mediaPlayer.getDuration()));
            updateUI(index);
            if (isPlaying) {
                mediaPlayer.start();
                startVinylRotation();
                updateSeekBar();
            }
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            currentSongIndex = (currentSongIndex + 1) % songs.length;
            playSong(currentSongIndex);
        });
    }

    private void togglePlayPause() {
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            stopVinylRotation();
            playPauseButton.setBackgroundResource(R.drawable.ic_play);
        } else {
            mediaPlayer.start();
            isPlaying = true;
            startVinylRotation();
            updateSeekBar();
            playPauseButton.setBackgroundResource(R.drawable.ic_pause);
        }
    }

    private void updateUI(int index) {
        songTitleText.setText(songTitles[index]);
        artistText.setText(artists[index]);
        durationSeekBar.setProgress(0);
        currentDurationText.setText("0:00");
        playPauseButton.setBackgroundResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

        // Scale animation for song change
        Animation scale = AnimationUtils.loadAnimation(this, R.anim.scale);
        vinylImage.startAnimation(scale);
    }

    private void startVinylRotation() {
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(10000);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new android.view.animation.LinearInterpolator());
        vinylImage.startAnimation(rotate);
    }

    private void stopVinylRotation() {
        vinylImage.clearAnimation();
    }

    private void updateSeekBar() {
        if (isPlaying && mediaPlayer != null) {
            durationSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            updateDurationText(mediaPlayer.getCurrentPosition());
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private void updateDurationText(int millis) {
        currentDurationText.setText(formatDuration(millis));
    }

    private String formatDuration(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (isPlaying) {
            updateSeekBar();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float z = event.values[2]; // Rotation around z-axis
        if (z > 2 && currentSongIndex < songs.length - 1) {
            currentSongIndex++;
            isPlaying = true;
            playSong(currentSongIndex);
        } else if (z < -2 && currentSongIndex > 0) {
            currentSongIndex--;
            isPlaying = true;
            playSong(currentSongIndex);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        handler.removeCallbacksAndMessages(null);
    }
}