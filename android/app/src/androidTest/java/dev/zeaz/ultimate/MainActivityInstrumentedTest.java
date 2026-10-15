package dev.zeaz.ultimate;

import android.app.Instrumentation;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class MainActivityInstrumentedTest {
    @Rule public ActivityTestRule<MainActivity> activity =
            new ActivityTestRule<>(MainActivity.class);

    @Test public void launchShowsChatControlsAndHud() {
        MainActivity screen = activity.getActivity();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View root = screen.getWindow().getDecorView();
            assertNotNull(root);
            assertTrue(root.findViewsWithText(new java.util.ArrayList<>(),
                    "Your AI companion", View.FIND_VIEWS_WITH_TEXT) == 0);
            assertTrue(findText(root, "Send message"));
            assertTrue(findText(root, "Voice input"));
            assertTrue(findText(root, "AI RESPONSE"));
            assertTrue(hasView(root, JarvisHudView.class));
        });
    }

    @Test public void invalidGatewayDoesNotStartNetworkRequest() {
        MainActivity screen = activity.getActivity();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View root = screen.getWindow().getDecorView();
            EditText endpoint = findEdit(root, "https://your-gateway.example.com");
            assertNotNull(endpoint);
            endpoint.setText("http://127.0.0.1:8765");
            View send = findButton(root, "Send message");
            assertNotNull(send);
            send.performClick();
            assertTrue(findText(root, "●  Enter a valid HTTPS gateway origin"));
        });
    }

    @Test public void hudLifecycleTogglesWithoutCrash() {
        MainActivity screen = activity.getActivity();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            JarvisHudView hud = findHud(screen.getWindow().getDecorView());
            assertNotNull(hud);
            hud.setProcessing(true);
            hud.setResumed(false);
            hud.setProcessing(false);
            hud.setResumed(true);
        });
    }

    private static boolean findText(View root, String value) {
        if (root instanceof TextView && value.equals(((TextView) root).getText().toString())) return true;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++)
                if (findText(group.getChildAt(i), value)) return true;
        }
        return false;
    }

    private static boolean hasView(View root, Class<?> type) {
        if (type.isInstance(root)) return true;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++)
                if (hasView(group.getChildAt(i), type)) return true;
        }
        return false;
    }

    private static JarvisHudView findHud(View root) {
        if (root instanceof JarvisHudView) return (JarvisHudView) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                JarvisHudView found = findHud(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static EditText findEdit(View root, String hint) {
        if (root instanceof EditText && hint.contentEquals(((EditText) root).getHint())) return (EditText) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findEdit(group.getChildAt(i), hint);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static View findButton(View root, String label) {
        if (root instanceof android.widget.Button && label.contentEquals(((android.widget.Button) root).getText())) return root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findButton(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
