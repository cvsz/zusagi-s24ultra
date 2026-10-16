package dev.zeaz.ultimate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class MainActivityInstrumentedTest {
  @Test public void assistantStateMachineAcceptsVoiceAndRequestLifecycle() {
    AssistantStateMachine machine = new AssistantStateMachine();
    assertEquals(AssistantStateMachine.State.INITIALIZING, machine.state());
    assertTrue(machine.transition(AssistantStateMachine.Event.INITIALIZATION_COMPLETE));
    assertEquals(AssistantStateMachine.State.DISCONNECTED, machine.state());
    assertTrue(machine.transition(AssistantStateMachine.Event.LISTENING_STARTED));
    assertTrue(machine.transition(AssistantStateMachine.Event.TRANSCRIPTION_STARTED));
    assertTrue(machine.transition(AssistantStateMachine.Event.TRANSCRIPTION_COMPLETED));
    assertTrue(machine.transition(AssistantStateMachine.Event.REQUEST_STARTED));
    assertTrue(machine.transition(AssistantStateMachine.Event.STREAM_STARTED));
    assertTrue(machine.transition(AssistantStateMachine.Event.RESPONSE_COMPLETED));
    assertEquals(AssistantStateMachine.State.IDLE, machine.state());
  }

  @Test public void assistantStateMachineRejectsIllegalTransitionsAndRestoresPausedWork() {
    AssistantStateMachine machine = new AssistantStateMachine();
    assertFalse(machine.transition(AssistantStateMachine.Event.REQUEST_STARTED));
    assertEquals(AssistantStateMachine.State.INITIALIZING, machine.state());
    assertTrue(machine.transition(AssistantStateMachine.Event.INITIALIZATION_COMPLETE));
    assertTrue(machine.transition(AssistantStateMachine.Event.REQUEST_STARTED));
    assertTrue(machine.transition(AssistantStateMachine.Event.PAUSE));
    assertEquals(AssistantStateMachine.State.PAUSED, machine.state());
    assertTrue(machine.transition(AssistantStateMachine.Event.RESPONSE_COMPLETED));
    assertEquals(AssistantStateMachine.State.PAUSED, machine.state());
    assertTrue(machine.transition(AssistantStateMachine.Event.RESUME));
    assertEquals(AssistantStateMachine.State.IDLE, machine.state());
  }

  @Test public void activityUsesDarkViewportAndReadableHeader() throws Exception {
    Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    try {
      View viewport = findTag(activity.getWindow().getDecorView(), "zeaz:viewport");
      View content = findTag(activity.getWindow().getDecorView(), "zeaz:content");
      View title = findTag(activity.getWindow().getDecorView(), "zeaz:title");
      assertNotNull("The scrollable app viewport must be present", viewport);
      assertNotNull("The app content root must be present", content);
      assertNotNull("The app title must be present", title);
      assertEquals(Color.rgb(10, 15, 27), backgroundColor(viewport));
      assertEquals(Color.rgb(10, 15, 27), backgroundColor(content));
      assertEquals("Your AI companion", ((TextView) title).getText().toString());
      assertFalse("The title must not display an escaped newline",
          ((TextView) title).getText().toString().contains("\\n"));
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        assertTrue("System bar insets must move content below the status bar",
            viewport.getPaddingTop() > 0);
      }
    } finally {
      activity.finish();
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
  }

  @Test public void analysisRejectsIncompleteScoreObjects() throws Exception {
    try {
      IntelligenceClient.formatResponse(
          "{\"scores\":{\"risk\":0},\"action\":\"HOLD\",\"rug_risk\":\"LOW\"}",
          "ZEAZ", "testnet");
      fail("Missing score values must not be presented as zero");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid score: opportunity", expected.getMessage());
    }
  }

  @Test public void analysisFormatsCompleteNumericScores() throws Exception {
    String result = IntelligenceClient.formatResponse(
        "{\"scores\":{\"opportunity\":12,\"risk\":34,\"confidence\":56},"
            + "\"action\":\"HOLD\",\"rug_risk\":\"LOW\"}",
        "ZEAZ", "testnet");
    assertTrue(result.contains("Opportunity 12"));
    assertTrue(result.contains("Risk 34"));
    assertTrue(result.contains("Confidence 56"));
    assertTrue(result.contains("Decision: HOLD"));
  }

  @Test public void analysisRejectsNonTextDecisionLabels() throws Exception {
    try {
      IntelligenceClient.formatResponse(
          "{\"scores\":{\"opportunity\":12,\"risk\":34,\"confidence\":56},"
              + "\"action\":7,\"rug_risk\":\"LOW\"}",
          "ZEAZ", "testnet");
      fail("A non-text decision label must not be rendered");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid field: action", expected.getMessage());
    }
  }

  @Test public void analysisResponseSizeIsBounded() throws Exception {
    byte[] body = new byte[9];
    try {
      IntelligenceClient.readBounded(new ByteArrayInputStream(body), 8);
      fail("Oversized response must be rejected");
    } catch (IOException expected) {
      assertEquals("Response too large", expected.getMessage());
    }
  }

  @Test public void humanoidHudRendersOriginalCyanAndGoldContours() {
    JarvisHudView hud = new JarvisHudView(InstrumentationRegistry.getInstrumentation().getTargetContext());
    hud.setState(JarvisHudView.State.IDLE);
    hud.measure(View.MeasureSpec.makeMeasureSpec(340, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(220, View.MeasureSpec.EXACTLY));
    hud.layout(0, 0, 340, 220);
    hud.setResumed(false);
    Bitmap bitmap = Bitmap.createBitmap(340, 220, Bitmap.Config.ARGB_8888);
    hud.draw(new Canvas(bitmap));
    int cyanPixels = 0;
    int goldPixels = 0;
    for (int y = 0; y < bitmap.getHeight(); y++) {
      for (int x = 0; x < bitmap.getWidth(); x++) {
        int pixel = bitmap.getPixel(x, y);
        int red = Color.red(pixel), green = Color.green(pixel), blue = Color.blue(pixel);
        if (blue > 190 && green > 130 && red < 130) cyanPixels++;
        if (red > 180 && green > 105 && green < 230 && blue > 35 && blue < 160) goldPixels++;
      }
    }
    bitmap.recycle();
    assertTrue("The HUD should draw cyan humanoid contours", cyanPixels > 100);
    assertTrue("The HUD should draw gold facial contours", goldPixels > 10);
  }

  @Test public void humanoidPoseChangesOverTime() {
    float initialPose = JarvisHudView.HumanoidMesh.headYawAt(0f, JarvisHudView.State.IDLE);
    float laterPose = JarvisHudView.HumanoidMesh.headYawAt(2f, JarvisHudView.State.IDLE);
    assertTrue("The avatar should change its head pose over time",
        Math.abs(laterPose - initialPose) > 0.05f);
    float listeningPose = JarvisHudView.HumanoidMesh.headYawAt(1f, JarvisHudView.State.LISTENING);
    float thinkingPose = JarvisHudView.HumanoidMesh.headYawAt(1f, JarvisHudView.State.THINKING);
    assertTrue("Listening and thinking should have distinct motion profiles",
        Math.abs(listeningPose - thinkingPose) > 0.05f);
  }

  private static int backgroundColor(View view) {
    assertTrue("Expected a solid-color background", view.getBackground() instanceof ColorDrawable);
    return ((ColorDrawable) view.getBackground()).getColor();
  }

  private static View findTag(View view, String tag) {
    if (tag.equals(view.getTag())) return view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int index = 0; index < group.getChildCount(); index++) {
        View match = findTag(group.getChildAt(index), tag);
        if (match != null) return match;
      }
    }
    return null;
  }
}
