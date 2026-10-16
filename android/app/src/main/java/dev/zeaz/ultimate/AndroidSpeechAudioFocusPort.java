package dev.zeaz.ultimate;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

/** Android audio-focus adapter for short assistant speech output. */
final class AndroidSpeechAudioFocusPort implements SpeechAudioFocusController.AudioFocusPort {
    private final AudioManager audioManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AudioFocusRequest request;

    AndroidSpeechAudioFocusPort(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override public boolean request(Runnable onFocusLost) {
        if (audioManager == null || request != null) return false;
        request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setWillPauseWhenDucked(true)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(change -> {
                    if (change == AudioManager.AUDIOFOCUS_LOSS
                            || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                            || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        onFocusLost.run();
                    }
                }, mainHandler)
                .build();
        int result;
        try {
            result = audioManager.requestAudioFocus(request);
        } catch (RuntimeException ex) {
            request = null;
            throw ex;
        }
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            request = null;
            return false;
        }
        return true;
    }

    @Override public void abandon() {
        if (audioManager == null || request == null) return;
        AudioFocusRequest activeRequest = request;
        request = null;
        audioManager.abandonAudioFocusRequest(activeRequest);
    }
}
