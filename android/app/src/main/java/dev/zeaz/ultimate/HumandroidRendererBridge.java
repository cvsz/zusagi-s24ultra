package dev.zeaz.ultimate;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** State-only IPC contract for the optional Unreal renderer companion. */
public final class HumandroidRendererBridge {
    public static final String RENDERER_PACKAGE = "dev.zeaz.humandroid";
    public static final String ACTION_STATE = "dev.zeaz.ultimate.HUMANDROID_STATE";
    public static final String SIGNATURE_PERMISSION = "dev.zeaz.ultimate.permission.HUMANDROID_RENDER_EVENT";
    public static final String EXTRA_VERSION = "protocol_version";
    public static final String EXTRA_STATE = "visual_state";
    public static final String EXTRA_AMPLITUDE = "audio_amplitude";
    public static final String EXTRA_SEQUENCE = "sequence";
    private static final Set<String> ALLOWED_STATES = new HashSet<>(Arrays.asList(
            "IDLE", "LISTENING", "THINKING", "STREAMING", "SPEAKING", "ERROR", "DISCONNECTED"));

    private HumandroidRendererBridge() { }

    public static Intent createEventIntent(String state, float amplitude, long sequence) {
        String normalized = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported Humandroid visual state");
        }
        float safeAmplitude = Float.isFinite(amplitude) ? Math.max(0.0f, Math.min(1.0f, amplitude)) : 0.0f;
        return new Intent(ACTION_STATE)
                .setPackage(RENDERER_PACKAGE)
                .putExtra(EXTRA_VERSION, 1)
                .putExtra(EXTRA_STATE, normalized)
                .putExtra(EXTRA_AMPLITUDE, safeAmplitude)
                .putExtra(EXTRA_SEQUENCE, sequence);
    }

    public static void sendState(Context context, String state, float amplitude) {
        Intent event = createEventIntent(state, amplitude, SystemClock.elapsedRealtimeNanos());
        context.sendBroadcast(event, SIGNATURE_PERMISSION);
    }

    /** Launch is intentionally explicit and should only be called from a user gesture. */
    public static boolean launch(Context context, String state) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(RENDERER_PACKAGE);
        if (launch == null) return false;
        long sequence = SystemClock.elapsedRealtimeNanos();
        Intent initial = createEventIntent(state, 0.0f, sequence);
        launch.setAction(ACTION_STATE).putExtras(initial);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return true;
    }
}
