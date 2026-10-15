package dev.zeaz.ultimate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.provider.Settings;

/**
 * Lightweight animated, depth-inspired AI HUD. Rendering is decorative only:
 * no camera access, background service, microphone recording, or network calls.
 */
public final class JarvisHudView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private long epoch = android.os.SystemClock.uptimeMillis();
    private boolean active = true;
    private boolean processing = false;
    private boolean resumed = false;

    public JarvisHudView(Context context) {
        super(context);
        setContentDescription("Animated ZEAZ holographic AI interface");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setProcessing(boolean value) {
        processing = value;
        invalidate();
    }

    public void setResumed(boolean value) {
        resumed = value;
        if (value) postInvalidateOnAnimation();
    }

    private boolean motionEnabled() {
        try {
            return Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (SecurityException ex) {
            return false;
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        active = true;
        epoch = android.os.SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        active = false;
        super.onDetachedFromWindow();
    }

    private void ring(Canvas canvas, float x, float y, float rx, float ry,
                      int color, float stroke, float start, float sweep) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(stroke);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        oval.set(x - rx, y - ry, x + rx, y + ry);
        canvas.drawArc(oval, start, sweep, false, paint);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        float x = width / 2f, y = height * 0.49f;
        float unit = Math.min(width / 340f, height / 220f);
        float angle = motionEnabled() ? ((android.os.SystemClock.uptimeMillis() - epoch) % 24000L) * 360f / 24000f : 0f;
        float pulse = (float) Math.sin(Math.toRadians(angle * 3)) * 0.5f + 0.5f;
        int cyan = Color.rgb(61, 221, 255);
        int blue = Color.rgb(83, 112, 255);

        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{Color.rgb(12, 33, 60), Color.rgb(9, 17, 35), Color.rgb(14, 27, 52)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(0, 0, width, height, 22 * unit, 22 * unit, paint);
        paint.setShader(null);

        // Perspective grid gives depth without OpenGL or remote assets.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.7f * unit);
        paint.setColor(Color.argb(45, 67, 196, 255));
        for (int i = -5; i <= 5; i++) {
            canvas.drawLine(x + i * 27 * unit, height, x + i * 6 * unit, y + 18 * unit, paint);
        }
        for (int i = 1; i <= 4; i++) {
            float v = y + 23 * unit + i * i * 6 * unit;
            canvas.drawLine(12 * unit, v, width - 12 * unit, v, paint);
        }

        for (int i = 3; i >= 0; i--) {
            int alpha = (int) ((23 + 16 * pulse) / (i + 1));
            ring(canvas, x, y, (72 + i * 10) * unit, (51 + i * 7) * unit,
                    Color.argb(alpha, 57, 206, 255), (3 + i) * unit, 0, 360);
        }
        ring(canvas, x, y, 99 * unit, 66 * unit, Color.argb(100, 61, 221, 255),
                1.1f * unit, angle, 228);
        ring(canvas, x, y, 90 * unit, 59 * unit, Color.argb(170, 101, 161, 255),
                2.2f * unit, -angle * 1.3f, 108);
        ring(canvas, x, y, 76 * unit, 51 * unit, cyan, 2.3f * unit, angle * 1.8f, 82);
        ring(canvas, x, y, 76 * unit, 51 * unit, blue, 2.3f * unit, angle * 1.8f + 180, 82);
        ring(canvas, x, y, 59 * unit, 39 * unit, Color.argb(160, 57, 216, 255),
                1f * unit, 0, 360);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(75, 28, 165, 234));
        canvas.drawCircle(x, y, (31 + 3 * pulse) * unit, paint);
        paint.setColor(Color.argb(110, 58, 217, 255));
        canvas.drawCircle(x, y, 22 * unit, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.BOLD));
        paint.setTextSize(14 * unit);
        canvas.drawText("ZEAZ", x, y + 5 * unit, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(9 * unit);
        paint.setColor(Color.argb(220, 159, 235, 255));
        canvas.drawText(processing ? "PROCESSING" : "AI CORE ONLINE", x, y + 98 * unit, paint);
        if (active && resumed && motionEnabled() && getWindowVisibility() == VISIBLE && isShown()) {
            postInvalidateDelayed(40);
        }
    }
}
