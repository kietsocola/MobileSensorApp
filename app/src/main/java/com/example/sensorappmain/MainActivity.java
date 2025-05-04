package com.example.sensorappmain;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.featureRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Feature> features = new ArrayList<>();
        features.add(new Feature("Hand Wave Light", R.drawable.ic_hand_wave, HandWaveActivity.class));
        features.add(new Feature("Volume Tilt", R.drawable.ic_volume, VolumeTiltActivity.class));
        features.add(new Feature("Song Switch", R.drawable.ic_music, SongSwitchActivity.class));
        features.add(new Feature("Wifi TV", R.drawable.ic_bluetooth, BluetoothSpeakerActivity.class));
        features.add(new Feature("Voice Control", R.drawable.ic_mic, VoiceControlActivity.class));

        FeatureAdapter adapter = new FeatureAdapter(features, (feature, view) -> {
            Intent intent = new Intent(MainActivity.this, feature.getActivityClass());
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        });
        recyclerView.setAdapter(adapter);
    }
}