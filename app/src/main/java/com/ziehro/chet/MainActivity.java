package com.ziehro.chet;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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

    private Intent recognizerIntent;


    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            // Do nothing
        }

        @Override
        public void onDone(String utteranceId) {
            if (utteranceId.equals("utteranceId")) {
                // Start listening for new request
                //speechRecognizer.startListening(recognizerIntent);
            }
        }

        @Override
        public void onError(String utteranceId) {
            // Handle any errors that occur during TTS playback
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.textView);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L);

        speechRecognizer.setRecognitionListener(this);
        // Start Continuous Speech Recognition
        speechRecognizer.startListening(recognizerIntent);


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
        speechRecognizer.startListening(recognizerIntent);
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
    public void onPartialResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            String command = matches.get(0);
            if (command.equalsIgnoreCase("hey there chet")) {
                // Stop current TTS playback
                textToSpeech.stop();
                // Listen for new request
                speechRecognizer.stopListening();
                textToSpeech.speak("yes?", TextToSpeech.QUEUE_FLUSH, null, null);
                speechRecognizer.startListening(recognizerIntent);
            }
        }
    }
    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        ArrayList<String> matches2 = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches2.size() > 0) {
            String command = matches2.get(0);
            if (command.equalsIgnoreCase("hey there chet")) {
                // Stop current TTS playback
                textToSpeech.stop();
                // Restart speech recognition
                speechRecognizer.cancel();
                textToSpeech.speak("yes?", TextToSpeech.QUEUE_FLUSH, null, null);
                speechRecognizer.startListening(recognizerIntent);
                return;
            }
        }

        if (matches != null) {
            String userInput = matches.get(0);
            sendRequestToOpenAI(userInput);
            speechRecognizer.startListening(recognizerIntent);
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
            jsonObject.put("temperature", 1);
            jsonObject.put("max_tokens", 500);
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
                        textView.setText(text.substring(31));
                        speak(text.substring(31));
                        //textToSpeech.speak(text.substring(31), TextToSpeech.QUEUE_FLUSH, null, null);

                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error parsing JSON response: " + responseData);
                }
            }



        });
    }

    private void speak(String message) {
        // Set the keep screen on flag
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Speak the message using the Text-to-Speech engine
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
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
            Locale locale = new Locale("en", "GB");
            Voice voice = new Voice("en-gb-x-rjs-local", locale, Voice.QUALITY_HIGH, Voice.LATENCY_HIGH, false, null);
            textToSpeech.setVoice(voice);

            int result = textToSpeech.setLanguage(locale);
            //int result = textToSpeech.setLanguage(Locale.getDefault());
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
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

