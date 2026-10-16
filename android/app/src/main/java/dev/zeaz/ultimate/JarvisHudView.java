package dev.zeaz.ultimate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.provider.Settings;

/**
 * Lightweight animated, depth-inspired AI HUD. Rendering is decorative only:
 * no camera access, background service, microphone recording, or network calls.
 */
public final class JarvisHudView extends View {
    private static final int[] BACKDROP_COLORS = {
            Color.rgb(12, 33, 60), Color.rgb(9, 17, 35), Color.rgb(14, 27, 52)
    };
    private static final Typeface LOGO_TYPEFACE = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final android.graphics.Path shouldersPath = new android.graphics.Path();
    private final android.graphics.Path facePath = new android.graphics.Path();
    private Shader backdropShader;
    private long epoch = android.os.SystemClock.uptimeMillis();
    private boolean active = true;
    public enum State { IDLE, LISTENING, THINKING, SPEAKING, ERROR }
    private State state = State.IDLE;
    private boolean resumed = false;

    public JarvisHudView(Context context) {
        super(context);
        setContentDescription("Animated ZEAZ holographic AI interface");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setProcessing(boolean value) {
        setState(value ? State.THINKING : State.IDLE);
    }

    public void setState(State value) {
        state = value == null ? State.IDLE : value;
        setContentDescription("ZEAZ AI status: " + state.name().toLowerCase(java.util.Locale.ROOT));
        invalidate();
    }

    public State getState() { return state; }

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

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        backdropShader = width > 0 && height > 0
                ? new LinearGradient(0, 0, width, height, BACKDROP_COLORS, null, Shader.TileMode.CLAMP)
                : null;
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
        int cyan = state == State.ERROR ? Color.rgb(255, 109, 125)
                : state == State.LISTENING ? Color.rgb(79, 215, 168)
                : state == State.SPEAKING ? Color.rgb(255, 188, 87)
                : Color.rgb(61, 221, 255);
        int blue = Color.rgb(83, 112, 255);

        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(backdropShader);
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
        drawHumanoid(canvas, x, y, unit, cyan, pulse);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(LOGO_TYPEFACE);
        paint.setTextSize(14 * unit);
        canvas.drawText("ZEAZ", x, y + 5 * unit, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(9 * unit);
        paint.setColor(Color.argb(220, 159, 235, 255));
        String caption = state == State.LISTENING ? "LISTENING"
                : state == State.THINKING ? "PROCESSING"
                : state == State.SPEAKING ? "SPEAKING"
                : state == State.ERROR ? "CONNECTION ERROR" : "AI CORE ONLINE";
        canvas.drawText(caption, x, y + 98 * unit, paint);
        if (active && resumed && motionEnabled() && getWindowVisibility() == VISIBLE && isShown()) {
            postInvalidateDelayed(40);
        }
    }

    private void drawHumanoid(Canvas canvas, float x, float y, float unit, int cyan, float pulse) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // Original 2D contour bust, deliberately independent of camera and model assets.
        paint.setStrokeWidth(1.8f * unit);
        paint.setColor(Color.argb(220, Color.red(cyan), Color.green(cyan), Color.blue(cyan)));
        oval.set(x - 31 * unit, y - 57 * unit, x + 31 * unit, y + 16 * unit);
        canvas.drawOval(oval, paint);

        android.graphics.Path shoulders = shouldersPath;
        shoulders.reset();
        shoulders.moveTo(x - 31 * unit, y + 5 * unit);
        shoulders.cubicTo(x - 38 * unit, y + 18 * unit,
                x - 66 * unit, y + 13 * unit, x - 75 * unit, y + 47 * unit);
        shoulders.cubicTo(x - 80 * unit, y + 57 * unit,
                x - 79 * unit, y + 70 * unit, x - 70 * unit, y + 74 * unit);
        shoulders.lineTo(x + 70 * unit, y + 74 * unit);
        shoulders.cubicTo(x + 79 * unit, y + 70 * unit,
                x + 80 * unit, y + 57 * unit, x + 75 * unit, y + 47 * unit);
        shoulders.cubicTo(x + 66 * unit, y + 13 * unit,
                x + 38 * unit, y + 18 * unit, x + 31 * unit, y + 5 * unit);
        canvas.drawPath(shoulders, paint);

        // Parallel contour lines suggest a holographic wireframe without a 3D engine.
        for (int index = 0; index < 7; index++) {
            float offset = index * 8 * unit;
            float halfWidth = (27 - index * 2.2f) * unit;
            float lineY = y - 44 * unit + offset;
            float curve = (float) Math.sqrt(Math.max(0f, 1f - (offset - 28 * unit)
                    * (offset - 28 * unit) / (30f * 30f * unit * unit)));
            paint.setStrokeWidth((0.7f + (index == 3 ? pulse * 0.55f : 0f)) * unit);
            paint.setColor(Color.argb(75 + index * 18, Color.red(cyan),
                    Color.green(cyan), Color.blue(cyan)));
            canvas.drawArc(x - halfWidth * curve, lineY - 4 * unit,
                    x + halfWidth * curve, lineY + 4 * unit, 180, 180, false, paint);
        }

        paint.setStrokeWidth(1.2f * unit);
        paint.setColor(Color.argb(190, 255, 188, 87));
        android.graphics.Path face = facePath;
        face.reset();
        face.moveTo(x - 15 * unit, y - 23 * unit);
        face.cubicTo(x - 13 * unit, y - 8 * unit, x - 9 * unit, y + 2 * unit,
                x, y + 4 * unit);
        face.cubicTo(x + 9 * unit, y + 2 * unit, x + 13 * unit, y - 8 * unit,
                x + 15 * unit, y - 23 * unit);
        canvas.drawPath(face, paint);

        paint.setStrokeWidth(1f * unit);
        paint.setColor(Color.argb(125, 255, 188, 87));
        canvas.drawLine(x - 23 * unit, y + 30 * unit, x + 23 * unit, y + 30 * unit, paint);
        canvas.drawLine(x - 34 * unit, y + 46 * unit, x + 34 * unit, y + 46 * unit, paint);
        canvas.drawLine(x - 45 * unit, y + 62 * unit, x + 45 * unit, y + 62 * unit, paint);
    }
}
