package dev.zeaz.ultimate;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowInsets;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int VOICE_REQUEST = 42;
    private static final int BACKGROUND = Color.rgb(10, 15, 27);
    private static final int PANEL = Color.rgb(24, 33, 52);
    private static final int TEXT = Color.rgb(239, 244, 255);
    private static final int SUBTLE = Color.rgb(155, 172, 196);
    private static final int ACCENT = Color.rgb(111, 128, 255);
    private static final int SUCCESS = Color.rgb(79, 215, 168);
    private EditText endpoint, token, input;
    private TextView status, output;
    private Button send, speak, intelligence;
    private TextToSpeech tts;
    private JarvisHudView hud;
    private boolean busy = false;
    private boolean speechReady = false;
    private boolean destroyed = false;
    private boolean ttsInitialized = false;
    private boolean speechActive = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, result -> runOnUiThread(() -> {
            ttsInitialized = result == TextToSpeech.SUCCESS;
            if (!destroyed && ttsInitialized && tts != null) {
                speechReady = tts.setLanguage(new Locale("th", "TH")) >= TextToSpeech.LANG_AVAILABLE;
            }
        }));
        buildScreen();
        if (tts != null) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {
                    runOnUiThread(() -> {
                        if (!destroyed && speechActive && hud != null) hud.setState(JarvisHudView.State.SPEAKING);
                    });
                }
                @Override public void onDone(String utteranceId) { resetSpeechHud(); }
                @Override public void onError(String utteranceId) { resetSpeechHud(); }
                private void resetSpeechHud() {
                    runOnUiThread(() -> {
                        if (!destroyed && hud != null) {
                            speechActive = false;
                            hud.setState(busy ? JarvisHudView.State.THINKING : JarvisHudView.State.IDLE);
                        }
                    });
                }
            });
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private GradientDrawable surface(int fill, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addSpace(LinearLayout parent, int height) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private EditText edit(String hint, boolean secret) {
        EditText view = new EditText(this);
        view.setSingleLine(true);
        view.setTextColor(TEXT);
        view.setHintTextColor(SUBTLE);
        view.setTextSize(14);
        view.setHint(hint);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackground(surface(BACKGROUND, 12));
        if (secret) view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return view;
    }

    private Button action(String title, int fill) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setBackground(surface(fill, 12));
        button.setMinHeight(dp(50));
        return button;
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        scroll.setClipToPadding(true);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setFitsSystemWindows(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right,
                        Math.max(bars.bottom, keyboard.bottom) + dp(16));
                return insets;
            });
        } else {
            scroll.setFitsSystemWindows(true);
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(24));
        scroll.addView(root);
        setContentView(scroll);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scroll.post(() -> {
                if (destroyed || !scroll.isAttachedToWindow()) return;
                android.view.WindowInsetsController controller = scroll.getWindowInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(0,
                            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
                scroll.requestApplyInsets();
            });
        }

        root.addView(text("ZEAZ  /  ULTIMATE", 13, SUCCESS, true));
        addSpace(root, 8);
        root.addView(text("Your AI companion", 29, TEXT, true));
        addSpace(root, 5);
        root.addView(text("Private gateway · Thai voice · Local AI", 13, SUBTLE, false));
        addSpace(root, 18);
        hud = new JarvisHudView(this);
        root.addView(hud, new LinearLayout.LayoutParams(-1, dp(226)));
        addSpace(root, 18);

        LinearLayout connection = new LinearLayout(this);
        connection.setOrientation(LinearLayout.VERTICAL);
        connection.setPadding(dp(16), dp(16), dp(16), dp(16));
        connection.setBackground(surface(PANEL, 18));
        root.addView(connection, new LinearLayout.LayoutParams(-1, -2));

        status = text("●  Ready to connect", 14, SUCCESS, true);
        connection.addView(status);
        addSpace(connection, 7);
        connection.addView(text("Connect using your authenticated HTTPS gateway.", 12, SUBTLE, false));
        addSpace(connection, 14);
        endpoint = edit("https://your-gateway.example.com", false);
        endpoint.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText(getPreferences(MODE_PRIVATE).getString("endpoint", ""));
        connection.addView(endpoint, new LinearLayout.LayoutParams(-1, dp(50)));
        addSpace(connection, 10);
        token = edit("Device access token (not saved)", true);
        connection.addView(token, new LinearLayout.LayoutParams(-1, dp(50)));

        addSpace(root, 24);
        root.addView(text("ASK ZEAZ", 12, SUBTLE, true));
        addSpace(root, 10);
        input = edit("What would you like to know?", false);
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));
        addSpace(root, 12);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions);
        Button voice = action("Voice input", PANEL);
        send = action("Send message", ACCENT);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(52), 1);
        half.setMargins(0, 0, dp(8), 0);
        actions.addView(voice, half);
        actions.addView(send, new LinearLayout.LayoutParams(0, dp(52), 1));
        voice.setOnClickListener(v -> startVoice());
        send.setOnClickListener(v -> ask());

        addSpace(root, 12);
        intelligence = action("Analyze token (advisory only)", PANEL);
        root.addView(intelligence, new LinearLayout.LayoutParams(-1, dp(52)));
        intelligence.setOnClickListener(v -> analyzeToken());

        addSpace(root, 24);
        root.addView(text("AI RESPONSE", 12, SUBTLE, true));
        addSpace(root, 10);
        LinearLayout response = new LinearLayout(this);
        response.setOrientation(LinearLayout.VERTICAL);
        response.setPadding(dp(16), dp(18), dp(16), dp(16));
        response.setBackground(surface(PANEL, 18));
        root.addView(response, new LinearLayout.LayoutParams(-1, -2));
        output = text("Responses will appear here. Configure the gateway above to get started.", 16, TEXT, false);
        output.setMinHeight(dp(92));
        output.setTextIsSelectable(true);
        response.addView(output);
        addSpace(response, 15);
        speak = action("Speak response in Thai", Color.rgb(48, 61, 86));
        response.addView(speak, new LinearLayout.LayoutParams(-1, dp(48)));
        speak.setOnClickListener(v -> {
            if (!speechReady || tts == null || destroyed) {
                Toast.makeText(this, "Thai text-to-speech unavailable", Toast.LENGTH_SHORT).show();
                if (hud != null) hud.setState(JarvisHudView.State.ERROR);
                return;
            }
            speechActive = true;
            if (hud != null) hud.setState(JarvisHudView.State.SPEAKING);
            int result = tts.speak(output.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, "zeaz-response");
            if (result == TextToSpeech.ERROR) {
                speechActive = false;
                if (hud != null) hud.setState(JarvisHudView.State.ERROR);
            }
        });
        addSpace(root, 20);
        root.addView(text("Gateway credentials stay in memory for this session only. Requests use HTTPS.", 12, SUBTLE, false));
        addSpace(root, 48);
    }

    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            if (hud != null) hud.setState(JarvisHudView.State.LISTENING);
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (Exception ex) {
            status.setText("●  Speech recognition unavailable");
            if (hud != null) hud.setState(JarvisHudView.State.ERROR);
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        if (hud != null && (value || !speechActive)) hud.setProcessing(value);
        send.setEnabled(!value);
        send.setAlpha(value ? 0.6f : 1f);
        if (intelligence != null) {
            intelligence.setEnabled(!value);
            intelligence.setAlpha(value ? 0.6f : 1f);
        }
    }

    private void analyzeToken() {
        if (busy) return;
        String base = endpoint.getText().toString().trim();
        String credential = token.getText().toString().trim();
        String value = input.getText().toString().trim();
        String[] fields = value.split(":", 3);
        URL target;
        try {
            target = new URL(base.replaceAll("/+$", "") + "/v1/analyze");
            if (!"https".equalsIgnoreCase(target.getProtocol())
                    || target.getHost().isEmpty()
                    || target.getUserInfo() != null
                    || (target.getPort() != -1 && target.getPort() != 443)
                    || !target.getPath().equals("/v1/analyze")
                    || target.getQuery() != null || target.getRef() != null) {
                throw new IllegalArgumentException("Invalid gateway origin");
            }
        } catch (Exception ex) {
            status.setText("●  Enter a valid HTTPS gateway origin");
            return;
        }
        if (credential.isEmpty() || fields.length != 3
                || fields[0].trim().isEmpty() || fields[1].trim().isEmpty()
                || fields[2].trim().isEmpty()) {
            status.setText("●  Enter chain:symbol:address and a device token");
            return;
        }
        setBusy(true);
        status.setText("●  Analyzing token (advisory only)...");
        output.setText("Retrieving advisory token intelligence...");
        new Thread(() -> {
            String result;
            boolean success = false;
            try {
                result = IntelligenceClient.analyze(base, credential,
                        fields[1], fields[0], fields[2]);
                success = true;
            } catch (Exception ex) {
                result = "Token analysis unavailable (" + ex.getClass().getSimpleName() + ").";
            }
            final String response = result;
            final boolean ok = success;
            runOnUiThread(() -> {
                if (destroyed || isFinishing()) return;
                output.setText(response);
                status.setText(ok ? "●  Advisory analysis received" : "●  Advisory analysis failed");
                setBusy(false);
            });
        }).start();
    }

    private void ask() {
        if (busy) return;
        String base = endpoint.getText().toString().trim();
        String credential = token.getText().toString().trim();
        String message = input.getText().toString().trim();
        URL target;
        try {
            target = new URL(base.replaceAll("/+$", "") + "/v1/chat");
            if (!"https".equalsIgnoreCase(target.getProtocol())
                    || target.getHost().isEmpty()
                    || target.getUserInfo() != null
                    || target.getPort() != -1 && target.getPort() != 443
                    || !target.getPath().equals("/v1/chat")
                    || target.getQuery() != null || target.getRef() != null) throw new IllegalArgumentException();
        } catch (Exception ex) {
            status.setText("●  Enter a valid HTTPS gateway origin");
            return;
        }
        if (credential.isEmpty() || message.isEmpty()) {
            status.setText("●  Device token and message required");
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putString("endpoint", base).apply();
        setBusy(true);
        status.setText("●  Thinking...");
        output.setText("ZEAZ is preparing a response…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            String answer;
            boolean success = false;
            try {
                connection = (HttpURLConnection) target.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(45000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + credential);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = new JSONObject().put("message", message).toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) { os.write(payload); }
                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream stream = connection.getInputStream()) {
                        byte[] buffer = new byte[262145];
                        int count = 0, read;
                        while ((read = stream.read(buffer, count, buffer.length - count)) > 0) {
                            count += read;
                            if (count >= buffer.length) throw new IllegalStateException("Response too large");
                        }
                        if (count == 0) throw new IllegalStateException("Empty response");
                        answer = new JSONObject(new String(buffer, 0, count, StandardCharsets.UTF_8)).getString("reply");
                        if (answer.trim().isEmpty()) throw new IllegalStateException("Empty reply");
                    }
                    success = true;
                } else {
                    answer = code == 401 ? "Authentication failed. Check your device token."
                            : "Gateway returned HTTP " + code + ". Check your connection.";
                }
            } catch (Exception ex) {
                answer = "Could not reach AI Gateway (" + ex.getClass().getSimpleName() + ").";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String reply = answer;
            final boolean ok = success;
            runOnUiThread(() -> {
                if (destroyed || isFinishing()) return;
                output.setText(reply);
                status.setText(ok ? "●  Connected · Response received" : "●  Gateway request failed");
                setBusy(false);
            });
        }).start();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST && hud != null) hud.setState(JarvisHudView.State.IDLE);
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) input.setText(results.get(0));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (hud != null) hud.setResumed(true);
    }

    @Override protected void onPause() {
        if (hud != null) {
            hud.setResumed(false);
            if (hud.getState() == JarvisHudView.State.LISTENING) hud.setState(JarvisHudView.State.IDLE);
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        speechActive = false;
        speechReady = false;
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        super.onDestroy();
    }
}
