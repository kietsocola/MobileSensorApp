package com.example.sensorappmain;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BluetoothSpeakerActivity extends AppCompatActivity {

    private static final String SERVER_IP = "192.168.1.2"; // Thay bằng IP của máy tính chạy server
    private static final int SERVER_PORT = 12345; // Cổng server

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_speaker);

        Button powerButton = findViewById(R.id.powerButton);
        Button volumeUpButton = findViewById(R.id.volumeUpButton);
        Button volumeDownButton = findViewById(R.id.volumeDownButton);
        Button muteButton = findViewById(R.id.muteButton);
        Button channelUpButton = findViewById(R.id.channelUpButton);
        Button channelDownButton = findViewById(R.id.channelDownButton);
        Button homeButton = findViewById(R.id.homeButton);

        powerButton.setOnClickListener(v -> sendCommand("POWER"));
        volumeUpButton.setOnClickListener(v -> sendCommand("VOL_UP"));
        volumeDownButton.setOnClickListener(v -> sendCommand("VOL_DOWN"));
        muteButton.setOnClickListener(v -> sendCommand("MUTE"));
        channelUpButton.setOnClickListener(v -> sendCommand("CHANNEL_UP"));
        channelDownButton.setOnClickListener(v -> sendCommand("CHANNEL_DOWN"));
        homeButton.setOnClickListener(v -> sendCommand("HOME"));
    }

    private void sendCommand(String command) {
        new SocketTask().execute(command);
    }

    private class SocketTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String command = params[0];
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(command); // Gửi lệnh
                return in.readLine(); // Nhận phản hồi từ server

            } catch (IOException e) {
                android.util.Log.e("SocketTask", "Error connecting to " + SERVER_IP + ":" + SERVER_PORT, e);
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(BluetoothSpeakerActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }
}