package com.example.texttospeech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.texttospeech.databinding.ActivityMainBinding;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    final String TAG = "dev";
    final int calendarIntentCode = 12412;

    TextToSpeech tts;
    final int VOICE_RECOGNITION = 2;
    Intent intent;
    int mostRecentUtteranceID = 0;
    String speechInput = "";

    private static final String channelID = "primary_notifications";
    private NotificationManager notificationManager;
    private int notificationID = 0;

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
                speakText(getString(R.string.speech_format), true);
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

        createNotificationChannel();
    }

    public void speakText(String TTS, final boolean fireSpeechRecognition) {
        tts.speak(TTS, TextToSpeech.QUEUE_ADD, null, mostRecentUtteranceID++ + "");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
            }
            @Override
            public void onDone(String s) {
                if(fireSpeechRecognition)
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
            if(eventTime.contains("p")) time += 12;
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
        int eventTime = getTime(inputs[inputs.length-2] + inputs[inputs.length-1]);

        Log.d(TAG, "parseInput: " + eventName + ", " + month + ", " + day + ", " + eventTime);

        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, eventTime);
        cal.set(Calendar.MINUTE, 0);

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("beginTime", cal.getTimeInMillis());
        intent.putExtra("title", eventName);
        startActivityForResult(intent, calendarIntentCode);

        scheduleNotification(eventName, cal.getTimeInMillis() - 86400000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == VOICE_RECOGNITION) {
            if(resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                float[] confidence = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);

                for(String s: results)
                    Log.d(TAG, "Speech: " + s);

//                speechInput = results.get(0);
//                parseInput();

            }
            speechInput = "Meeting november 15 at 1 a.m.";
            parseInput();
        }
        else if(requestCode == calendarIntentCode){
            speakText("Event added successfully", false);
            Toast.makeText(this, "Event added Successfully!", Toast.LENGTH_LONG).show();
        }
        
    }

    private void createNotificationChannel() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // create channel on sdk >= 26
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelID, "Event Reminders", NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notify me about calendar events");

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private NotificationCompat.Builder getNotificationBuilder(String desc){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, notificationID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, channelID)
                .setContentTitle("Event Reminder")
                .setContentText(desc)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentIntent(pendingNotificationIntent)
                .setAutoCancel(true);
        return notifyBuilder;
    };

    private void scheduleNotification(String desc, long notificationTime) {
        NotificationCompat.Builder notifyBuilder = getNotificationBuilder(desc);
        Notification notification = notifyBuilder.build();

        Intent notificationIntent = new Intent(this, MyNotificationPublisher.class);
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION_ID, notificationID);
        notificationIntent.putExtra(MyNotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
    }
}