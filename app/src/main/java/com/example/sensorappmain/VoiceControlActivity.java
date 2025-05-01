package com.example.sensorappmain;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class VoiceControlActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private ImageView lightIndicator;
    private TextView voiceResultText;
    private boolean isLightOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_control);

        lightIndicator = findViewById(R.id.lightIndicator);
        voiceResultText = findViewById(R.id.voiceResultText);
        Button speakButton = findViewById(R.id.speakButton);

        speakButton.setOnClickListener(v -> startVoiceRecognition());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String command = result.get(0).toLowerCase();
            voiceResultText.setText("Heard: " + command);

            if (command.contains("turn on") || command.contains("light on")) {
                isLightOn = true;
                lightIndicator.setImageResource(R.drawable.ic_light_on);
                Toast.makeText(this, "Light ON", Toast.LENGTH_SHORT).show();
            } else if (command.contains("turn off") || command.contains("light off")) {
                isLightOn = false;
                lightIndicator.setImageResource(R.drawable.ic_light_off);
                Toast.makeText(this, "Light OFF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
