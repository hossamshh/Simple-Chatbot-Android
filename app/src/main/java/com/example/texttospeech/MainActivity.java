package com.example.texttospeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.texttospeech.databinding.ActivityMainBinding;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    final String TAG = "dev";

    TextToSpeech tts;
    final int VOICE_RECOGNITION = 2;
    Intent intent;
    int mostRecentUtteranceID = 0;
    String speechInput = "";
    TextView textView;
    String channelID = "channel1";
    int notificationID = 1;
    Notification.Builder builder;
    NotificationManagerCompat notificationManager;
    ConstraintLayout constraintLayout;
    Button btn;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.button01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakText(getString(R.string.speech_format));
            }
        });

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setPitch((float)1);
                    tts.setSpeechRate((float)1);
                }
            }
        });

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak!");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
    }

    public void speakText(String TTS) {
        tts.speak(TTS, TextToSpeech.QUEUE_ADD, null, mostRecentUtteranceID++ + "");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
            }
            @Override
            public void onDone(String s) {
                startActivityForResult(intent, VOICE_RECOGNITION);
            }
            @Override
            public void onError(String s) {

            }
        });
    }

    private int getMonthNumber(String month) {
        if(month.equals("january"))
            return 0;
        else if(month.equals("february"))
            return 1;
        else if(month.equals("march"))
            return 2;
        else if(month.equals("april"))
            return 3;
        else if(month.equals("may"))
            return 4;
        else if(month.equals("june"))
            return 5;
        else if(month.equals("july"))
            return 6;
        else if(month.equals("august"))
            return 7;
        else if(month.equals("september"))
            return 8;
        else if(month.equals("october"))
            return 9;
        else if(month.equals("november"))
            return 10;
        else
            return 11;
    }

    private int getDayNumber(String day) {
        Pattern regex = Pattern.compile("\\d+");
        Matcher matcher = regex.matcher(day);
        if(matcher.find())
            return Integer.parseInt(matcher.group());
        else return 0;
    }

    private int getTime(String eventTime) {
        Pattern regex = Pattern.compile("\\d+");
        Matcher matcher = regex.matcher(eventTime);
        if(matcher.find()){
            int time = Integer.parseInt(matcher.group());
            if(eventTime.charAt(eventTime.length()-2) == 'p') time += 12;
            return time;
        }
        else return 0;
    }

    public void parseInput() {
        String[] inputs = speechInput.split(" ");
        int parsingIndex = 0;
        String eventName = inputs[parsingIndex++];
        int month = getMonthNumber(inputs[parsingIndex++]);
        int day = getDayNumber(inputs[parsingIndex++]);
        int eventTime = getTime(inputs[inputs.length-1]);

        Log.d(TAG, "parseInput: " + eventName + ", " + month + ", " + day + ", " + eventTime);

        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, eventTime);
        cal.set(Calendar.MINUTE, 0);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("beginTime", cal.getTimeInMillis());
        intent.putExtra("title", eventName);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == VOICE_RECOGNITION) {
            if(resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                float[] confidence = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);

                for(String s: results)
                    Log.d("dev", "Speech: " + s);

            }
        }
        speechInput = "Meeting december 2nd at 5pm";
        parseInput();
    }
}