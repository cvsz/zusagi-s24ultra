package dev.zeaz.ultimate;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

public final class MainActivity extends Activity {
  private static final int VOICE_REQUEST = 42;
  private EditText endpoint, token, input;
  private TextView output;
  private TextToSpeech tts;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    tts = new TextToSpeech(this, status -> { if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("th", "TH")); });
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(36,36,36,36);
    getWindow().setStatusBarColor(0xFF11121A);
    getWindow().setNavigationBarColor(0xFF11121A);
    root.setBackgroundColor(0xFF11121A);
    TextView title = new TextView(this);
    title.setText("ZUSAGI  /  ZEAZDEV\nAI Control Center");
    title.setTextColor(0xFFD0BCFF);
    title.setTextSize(27);
    title.setTypeface(null, 1);
    title.setPadding(0, 12, 0, 32);
    root.addView(title);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(0xFF11121A);
    scroll.addView(root);
    android.view.WindowInsetsController insetsController = getWindow().getInsetsController();
    if (insetsController != null) {
      insetsController.setSystemBarsAppearance(0,
          android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
          android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    }
    if (android.os.Build.VERSION.SDK_INT >= 35) {
      scroll.setOnApplyWindowInsetsListener((view, insets) -> {
        android.graphics.Insets bars = insets.getInsets(
            android.view.WindowInsets.Type.systemBars() |
            android.view.WindowInsets.Type.displayCutout());
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
        return insets;
      });
    }
    endpoint = field(root, "HTTPS gateway URL", false);
    endpoint.setText(getPreferences(MODE_PRIVATE).getString("endpoint",""));
    token = field(root, "Device access token (session only)", true);
    input = field(root, "Ask ZEAZ Ultimate", false);
    Button send = new Button(this); send.setText("Send to AI Gateway"); root.addView(send);
    Button voice = new Button(this); voice.setText("Voice Input"); root.addView(voice);
    Button speak = new Button(this); speak.setText("Speak Response"); root.addView(speak);
    output = new TextView(this); output.setText("Ready. Configure HTTPS endpoint and token."); root.addView(output);
    setContentView(scroll);
    endpoint.setTextColor(0xFFE6E1E5);
    token.setTextColor(0xFFE6E1E5);
    input.setTextColor(0xFFE6E1E5);
    output.setTextColor(0xFFE6E1E5);
    endpoint.setHintTextColor(0xFFCAC4D0);
    token.setHintTextColor(0xFFCAC4D0);
    input.setHintTextColor(0xFFCAC4D0);
    for (Button button : new Button[]{send, voice, speak}) {
      button.setAllCaps(false);
      button.setTextColor(0xFF1C1B1F);
      button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD0BCFF));
    }
    Button intel = new Button(this);
    intel.setText("Analyze Token (Advisory)");
    intel.setAllCaps(false);
    intel.setTextColor(0xFF1C1B1F);
    intel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFB4CC9B));
    root.addView(intel);
    intel.setOnClickListener(v -> analyzeToken());
    send.setOnClickListener(v -> ask());
    voice.setOnClickListener(v -> { Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"th-TH"); try {startActivityForResult(i,VOICE_REQUEST);} catch(Exception e){output.setText("Speech recognition unavailable");}});
    speak.setOnClickListener(v -> tts.speak(output.getText().toString(),TextToSpeech.QUEUE_FLUSH,null,"zeaz"));
  }
  private EditText field(LinearLayout parent,String hint,boolean secret) {
    EditText e=new EditText(this); e.setHint(hint); if(secret)e.setInputType(129); parent.addView(e); return e;
  }
  private void analyzeToken() {
    String gateway = endpoint.getText().toString().trim();
    String secret = token.getText().toString().trim();
    String value = input.getText().toString().trim();
    String[] fields = value.split(":", 3);
    if (!gateway.startsWith("https://") || secret.isEmpty() || fields.length != 3) {
      output.setText("For intelligence, enter chain:symbol:address and configure HTTPS gateway + token.");
      return;
    }
    output.setText("Analyzing token...");
    new Thread(() -> {
      try {
        String response = IntelligenceClient.analyze(gateway, secret, fields[1], fields[0], fields[2]);
        runOnUiThread(() -> output.setText(response));
      } catch (Exception e) {
        runOnUiThread(() -> output.setText("Intelligence request failed: " + e.getClass().getSimpleName()));
      }
    }).start();
  }
  private void ask() {
    String url=endpoint.getText().toString().trim(), access=token.getText().toString().trim(), prompt=input.getText().toString().trim();
    if(!url.startsWith("https://") || access.isEmpty() || prompt.isEmpty()){output.setText("HTTPS endpoint, token and prompt required");return;}
    getPreferences(MODE_PRIVATE).edit().putString("endpoint",url).apply();
    output.setText("Processing...");
    new Thread(() -> {
      HttpURLConnection conn=null;
      try {
        URL target=new URL(url.replaceAll("/+$","")+"/v1/chat");
        conn=(HttpURLConnection)target.openConnection();
        conn.setConnectTimeout(10000);conn.setReadTimeout(45000);
        conn.setRequestMethod("POST");conn.setDoOutput(true);
        conn.setRequestProperty("Authorization","Bearer "+access);
        conn.setRequestProperty("Content-Type","application/json");
        byte[] data=new JSONObject().put("message",prompt).toString().getBytes(StandardCharsets.UTF_8);
        try(OutputStream os=conn.getOutputStream()){os.write(data);}
        int code=conn.getResponseCode();
        InputStream stream=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();
        String body;
        try(InputStream is=stream){body=new String(is.readAllBytes(),StandardCharsets.UTF_8);}
        String answer=code>=200&&code<300?new JSONObject(body).optString("reply","No reply"):"HTTP "+code;
        runOnUiThread(() -> output.setText(answer));
      } catch(Exception e){runOnUiThread(() -> output.setText("Gateway request failed: "+e.getClass().getSimpleName()));}
      finally {if(conn!=null)conn.disconnect();}
    }).start();
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==VOICE_REQUEST&&result==RESULT_OK&&data!=null){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())input.setText(words.get(0));}
  }
  @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
