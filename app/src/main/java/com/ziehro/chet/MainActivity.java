package com.ziehro.chet;


import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
    private String token = BuildConfig.API_KEY;
    private Button mButton;
    private Button newQuestionButton;
    private Button shushButton;
    private EditText promptEditText;
    private Button sendButton;

    private FrameLayout mainContainer;
    private ImageButton keyboardButton;

    private boolean isListening = true;
    private boolean isPromptFromKeyboard = false;

    private Intent recognizerIntent;
    StringBuilder conversationHistory = new StringBuilder();
    StringBuilder conversationHistoryfull = new StringBuilder();
    private Dialog listeningDialog;
    private Dialog thinkingDialog;


    int audioIndex;
    Date today = new Date();

    AdView mAdView;


    @Override
    public void onInit(int status) {
        // Check if Text-to-Speech is available

        if (status == TextToSpeech.SUCCESS) {

            textToSpeech.setSpeechRate(1.0F);
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);

        } else {
            Log.e(TAG, "Text-to-Speech initialization failed.");
            Toast.makeText(getApplicationContext(), "Text-to-Speech initialization failed.", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.textView);
        Toast.makeText(this, "Start Speaking Now", Toast.LENGTH_LONG).show();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        textToSpeech = new TextToSpeech(this, this);
        promptEditText = findViewById(R.id.promptEditText);



        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "");



        shushButton = findViewById(R.id.shushButton);
        shushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textToSpeech != null && textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
                }
            }
        });

        // Check for record audio permission
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);

        } else {
            textToSpeech.speak("Hey what's up?", TextToSpeech.QUEUE_FLUSH, null, null);
            setupSpeechRecognizer();
        }

        // Initialize OkHttpClient
        httpClient = new OkHttpClient();

        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textToSpeech != null && textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
                }
                thinkingDialog.show();
                listeningDialog.show();

                speechRecognizer.startListening(recognizerIntent);
            }
        });

        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the prompt text from the EditText field
                String prompt = promptEditText.getText().toString().trim();

                // Check if prompt is not empty
                if (!prompt.isEmpty()) {
                    // Set the flag to indicate prompt from the keyboard
                    isPromptFromKeyboard = true;

                    // Send the prompt to the chatbot
                    sendRequestToOpenAI(prompt);

                    // Clear the EditText field
                    promptEditText.setText("");
                }
            }
        });


        newQuestionButton = findViewById(R.id.newquestionbutton);
        newQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textToSpeech != null && textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
                }
                thinkingDialog.show();
                listeningDialog.show();

                conversationHistory = new StringBuilder("");
                conversationHistoryfull = new StringBuilder("");
                speechRecognizer.startListening(recognizerIntent);
            }
        });


        Button emailButton = findViewById(R.id.emailbutton);
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the response text
                String response = String.valueOf(conversationHistoryfull);

                // Create an intent to open the email client
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Conversation with Jimmy on " + today.getMonth() + " / " + today.getDate() + " at " + today.getHours() + ":" + today.getMinutes());
                emailIntent.putExtra(Intent.EXTRA_TEXT, response);

                // Start the email intent
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            }
        });
    }

    private void setupSpeechRecognizer() {
        // Initialize SpeechRecognizer


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        thinkingDialog = new Dialog(this);
        thinkingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        thinkingDialog.setContentView(R.layout.dialog_thinking);
        thinkingDialog.setCancelable(false);
        thinkingDialog.dismiss();
        thinkingDialog.show();
        listeningDialog = new Dialog(this);
        listeningDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        listeningDialog.setContentView(R.layout.dialog_listening);
        listeningDialog.setCancelable(false);
        listeningDialog.dismiss();
        Button quitButton = listeningDialog.findViewById(R.id.quitButton);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Dismiss the thinking dialog
                        if (thinkingDialog != null && thinkingDialog.isShowing()) {
                            thinkingDialog.dismiss();
                        }

                        // Open the keyboard dialog
                        openKeyboard();
                    }
                });
            }
        });


        listeningDialog.show();



        // Start listening
        speechRecognizer.startListening(recognizerIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioIndex = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        Log.d(TAG, "onReadyForSpeech");



        // ...

        Log.d(TAG, "onReadyForSpeech");

    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.d(TAG, "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        //speechRecognizer.startListening(recognizerIntent);
        Log.d(TAG, "onEndOfSpeech");
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audioIndex,0);
        // Remove the keyboard button from the WindowManager

        // Remove the keyboard button from the window manager
        if (keyboardButton != null && keyboardButton.isAttachedToWindow()) {
            WindowManager windowManager = getWindow().getWindowManager();
            //windowManager.removeView(keyboardButton);
        }


    }

    @Override
    public void onError(int error) {
        Log.e(TAG, "Error: " + error);
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audioIndex,0);

        if (error == 7) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (thinkingDialog != null && thinkingDialog.isShowing()) {
                        thinkingDialog.dismiss();
                        Log.e(TAG, "Inside the loooooop " + error);
                    }
                    if (listeningDialog != null && listeningDialog.isShowing()) {
                        listeningDialog.dismiss();
                    }
                }
            });
        } else if (error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            // Handle network timeout error
            Log.e(TAG, "Network timeout error");
        }
    }


    @Override
    public void onPartialResults(Bundle bundle) {

    }
    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (listeningDialog != null && listeningDialog.isShowing()) {
            listeningDialog.dismiss();
        }



        if (matches != null && isListening) {

            String command = matches.get(0);
            if (command.equalsIgnoreCase("hey Jimmy")) {
                catchPhrases(matches, "hey Jimmy", "Ya Buddy?");
                return;
            }
            String lookGood = matches.get(0);
            conversationHistoryfull.append(" Me: " + lookGood.substring(0, 1).toUpperCase() + lookGood.substring(1));
            String userInput = matches.get(0) + "\n" +conversationHistory.toString();

            sendRequestToOpenAI(userInput);
        }
        else {
            // Get text from keyboard dialog
            EditText editText = this.findViewById(R.id.promptEditText);
            String prompt = editText.getText().toString();

            // Send prompt to OpenAI
            sendRequestToOpenAI(prompt);
            // Clear text in edit text
            editText.setText("");
            // Show thinking dialog
            thinkingDialog.show();
        }
    }
    private void sendRequestToOpenAI(String prompt) {




        OkHttpClient client = new OkHttpClient();
        String apiUrl = "https://api.openai.com/v1/chat/completions";
        String apiKey = token;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");
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
                    for (int i = 0; i < choicesArray.length(); i++) {
                        JSONObject choiceObject = choicesArray.getJSONObject(i);
                        JSONObject message = choiceObject.getJSONObject("message");
                        String text = message.getString("content");
                        String modifiedContent = text.replaceAll("As an AI language model, ", "");
                        String modifiedContenta = modifiedContent.replaceAll("I'm sorry, but as an AI language model, ", "");
                        String modifiedContent2 = modifiedContenta.replaceAll("I'm sorry, as an AI language model, ", "");
                        int index = modifiedContent2.indexOf("however");
                        if (index != -1) {
                            modifiedContent2 = modifiedContent2.substring(index);

                        }

                        String finalModifiedContent = modifiedContent2;
                        runOnUiThread(() -> {
                            // Update UI with response text
                            if (thinkingDialog != null && thinkingDialog.isShowing()) {
                                thinkingDialog.dismiss();
                            }
                            speak(finalModifiedContent);

                            conversationHistory.append(finalModifiedContent + "\n");
                            conversationHistoryfull.append("\n" + finalModifiedContent + "\n");
                            textView.setText(finalModifiedContent);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                }
                            }, 60000);

                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error parsing JSON response: " + responseData);
                }
            }
        });

    }

    private final UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            // Do nothing
        }

        @Override
        public void onDone(String utteranceId) {
            if (utteranceId.equals(utteranceId)) {
                // Start listening for new request
                //Toast.makeText(getApplicationContext(), "Done Now", Toast.LENGTH_SHORT).show();
                runOnUiThread(() -> {
                    thinkingDialog.show();
                    listeningDialog.show();
                speechRecognizer.stopListening();
                speechRecognizer.startListening(recognizerIntent);
                });
            }
        }

        @Override
        public void onError(String utteranceId) {
            // Handle any errors that occur during TTS playback
        }
    };

    private void speak(String message) {
        // Set the keep screen on flag
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Check if the prompt is not from the keyboard
        if (!isPromptFromKeyboard) {
            int utteranceId = hashCode();
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, Integer.toString(utteranceId));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }, 60000);
        } else {
            // Clear the flag for the next prompt
            isPromptFromKeyboard = false;
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
        if (thinkingDialog != null && thinkingDialog.isShowing()) {
            thinkingDialog.dismiss();
        }
        if (listeningDialog != null && listeningDialog.isShowing()) {
            listeningDialog.dismiss();
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "onEvent");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupSpeechRecognizer();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void catchPhrases(ArrayList<String> matches, String in, String out){
        String command = matches.get(0);

            // Stop current TTS playback
            textToSpeech.stop();
            // Listen for new request
            speechRecognizer.stopListening();
            Handler handler2 = new Handler();
            handler2.postDelayed(new Runnable() {
                @Override
                public void run() {
                    textToSpeech.speak(out, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }, 400);

            Handler handler = new Handler();

            // Post a delayed runnable to start listening again after half a second
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }, 1800);
            return;



    }
    private void stopListening() {
        speechRecognizer.stopListening();
        textToSpeech.stop();
    }

    private void openKeyboard() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        isPromptFromKeyboard = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter your prompt");

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the user input and send it to the OpenAI API
                        String prompt = input.getText().toString();
                        sendRequestToOpenAI(prompt);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // Show the dialog
                AlertDialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });
    }

    private void addKeyboardButtonToLayout() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FrameLayout rootLayout = findViewById(android.R.id.content);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.TOP | Gravity.END;
                params.topMargin = 16;
                params.rightMargin = 16;
                rootLayout.addView(keyboardButton, params);
            }
        });
    }





}

