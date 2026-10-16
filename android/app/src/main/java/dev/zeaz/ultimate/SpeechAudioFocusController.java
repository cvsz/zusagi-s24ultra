package dev.zeaz.ultimate;

/** Owns one short-lived audio-focus lease for assistant speech playback. */
final class SpeechAudioFocusController {
    interface AudioFocusPort {
        boolean request(Runnable onFocusLost);
        void abandon();
    }

    private final AudioFocusPort port;
    private final Runnable onInterrupted;
    private boolean acquired;
    private boolean interruptionDelivered;

    SpeechAudioFocusController(AudioFocusPort port, Runnable onInterrupted) {
        if (port == null || onInterrupted == null) throw new IllegalArgumentException("Focus dependencies required");
        this.port = port;
        this.onInterrupted = onInterrupted;
    }

    boolean acquire() {
        if (acquired) return true;
        try {
            acquired = port.request(this::onFocusLost);
        } catch (RuntimeException ex) {
            android.util.Log.w("SpeechAudioFocus", "Audio focus request failed", ex);
            acquired = false;
        }
        interruptionDelivered = false;
        return acquired;
    }

    void onFocusLost() {
        if (!acquired || interruptionDelivered) return;
        interruptionDelivered = true;
        onInterrupted.run();
    }

    void release() {
        if (!acquired) return;
        acquired = false;
        interruptionDelivered = false;
        try {
            port.abandon();
        } catch (RuntimeException ex) {
            android.util.Log.w("SpeechAudioFocus", "Audio focus release failed", ex);
        }
    }

    boolean isAcquired() {
        return acquired;
    }
}
