package com.ziehro.chet;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements RecognitionListener, TextToSpeech.OnInitListener {

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String TAG = "MainActivity";
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private OkHttpClient httpClient;
    private String token = "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";
    private Button mButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.textView);

        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupSpeechRecognizer();
            }
        });

        // Check for record audio permission
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            setupSpeechRecognizer();
        }

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);

        // Initialize OkHttpClient
        httpClient = new OkHttpClient();
    }

    private void setupSpeechRecognizer() {
        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        // Create intent for SpeechRecognizer
        Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        // Start listening
        speechRecognizer.startListening(speechRecognizerIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Log.d(TAG, "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        Log.e(TAG, "Error: " + error);
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            speechRecognizer.stopListening();
            speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "onPartialResults");
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            String userInput = matches.get(0);
            sendRequestToOpenAI(userInput);
        }
    }

    private void sendRequestToOpenAI(String prompt) {
        OkHttpClient client = new OkHttpClient();
        String apiUrl = "https://api.openai.com/v1/chat/completions";
        String apiKey = "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";

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");
            //jsonObject.put("prompt", prompt);
            jsonObject.put("temperature", 0.5);
            jsonObject.put("max_tokens", 50);
            jsonObject.put("n", 1);
            jsonObject.put("stop", "\n");

            // Add messages parameter
            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", prompt);
            messagesArray.put(messageObject);
            jsonObject.put("messages", messagesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                TextView textView = findViewById(R.id.textView);
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray choicesArray = jsonObject.getJSONArray("choices");
                    JSONObject choiceObject = choicesArray.getJSONObject(0);
                    String text = choiceObject.getString("message");
                    runOnUiThread(() -> {
                        // Update UI with response text
                        textView.setText(text);
                        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error parsing JSON response: " + responseData);
                }
            }
        });
    }




    private String extractResponseText(String responseBody) {
        // Extract response text from JSON response
        String responseText = "";
        try {
            responseText = responseBody.substring(responseBody.indexOf("\"text\":") + 8);
            responseText = responseText.substring(0, responseText.indexOf("\""));
        } catch (Exception e) {
            Log.e(TAG, "Error extracting response text: " + e.getMessage());
        }
        return responseText;
    }

    private void speakResponse(final String responseText) {
        // Speak response text
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textToSpeech.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    @Override
    public void onInit(int status) {
        // Check if Text-to-Speech is available
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Text-to-Speech language not supported.");
                Toast.makeText(getApplicationContext(), "Text-to-Speech language not supported.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Text-to-Speech initialization failed.");
            Toast.makeText(getApplicationContext(), "Text-to-Speech initialization failed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "onEvent");
    }
}

