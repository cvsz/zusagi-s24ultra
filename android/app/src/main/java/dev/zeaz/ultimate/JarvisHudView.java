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
import java.util.ArrayList;

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
    private final HumanoidMesh humanoidMesh = HumanoidMesh.create();
    private final Paint humanoidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        drawHumanoid3d(canvas, x, y, unit, cyan);
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

    private void drawHumanoid3d(Canvas canvas, float centerX, float centerY, float unit, int cyan) {
        float seconds = motionEnabled()
                ? (android.os.SystemClock.uptimeMillis() - epoch) / 1000f : 0f;
        humanoidMesh.project(centerX, centerY, unit, seconds, state);
        humanoidPaint.setStyle(Paint.Style.STROKE);
        humanoidPaint.setStrokeCap(Paint.Cap.ROUND);
        humanoidPaint.setStrokeWidth(0.85f * unit);
        humanoidPaint.setColor(Color.argb(165, Color.red(cyan), Color.green(cyan), Color.blue(cyan)));
        canvas.drawLines(humanoidMesh.projectedCyan, humanoidPaint);
        humanoidPaint.setStrokeWidth(1.15f * unit);
        humanoidPaint.setColor(Color.argb(215, 255, 188, 87));
        canvas.drawLines(humanoidMesh.projectedGold, humanoidPaint);
    }

    /** A small procedural 3D wireframe, projected with perspective into the native Canvas. */
    static final class HumanoidMesh {
        private static final float CAMERA_DISTANCE = 250f;
        private static final float FOCAL_LENGTH = 250f;
        private final float[] vertices;
        private final int[] cyanEdges;
        private final int[] goldEdges;
        private final byte[] cyanZones;
        final float[] projectedCyan;
        final float[] projectedGold;

        private HumanoidMesh(MeshBuilder builder) {
            vertices = toFloatArray(builder.vertices);
            cyanEdges = toIntArray(builder.cyanEdges);
            goldEdges = toIntArray(builder.goldEdges);
            cyanZones = toByteArray(builder.cyanZones);
            projectedCyan = new float[cyanEdges.length * 2];
            projectedGold = new float[goldEdges.length * 2];
        }

        static HumanoidMesh create() {
            MeshBuilder builder = new MeshBuilder();
            builder.addEllipsoid(0f, -48f, 0f, 69f, 46f, 29f, 10, 20, (byte) 0);
            builder.addEllipsoid(0f, 2f, 0f, 15f, 25f, 15f, 8, 16, (byte) 0);
            builder.addEllipsoid(0f, 54f, 0f, 29f, 36f, 23f, 12, 24, (byte) 1);
            builder.addFaceContours();
            return new HumanoidMesh(builder);
        }

        void project(float centerX, float centerY, float unit, float seconds, State state) {
            float yaw = headYawAt(seconds, state);
            float bodyYaw = 0.035f * (float) Math.sin(seconds * 0.45f);
            float breathAmplitude = state == State.SPEAKING ? 0.024f : 0.012f;
            float breathRate = state == State.SPEAKING ? 2.8f : 1.1f;
            float breathing = 1f + breathAmplitude * (float) Math.sin(seconds * breathRate);
            projectEdges(cyanEdges, cyanZones, projectedCyan,
                    centerX, centerY, unit, yaw, bodyYaw, breathing, false);
            projectEdges(goldEdges, null, projectedGold,
                    centerX, centerY, unit, yaw, bodyYaw, breathing, true);
        }

        static float headYawAt(float seconds, State state) {
            float amplitude = state == State.LISTENING ? 0.15f
                    : state == State.THINKING ? 0.06f
                    : state == State.SPEAKING ? 0.08f
                    : state == State.ERROR ? 0.035f : 0.10f;
            float rate = state == State.LISTENING ? 1.25f
                    : state == State.THINKING ? 0.42f
                    : state == State.SPEAKING ? 1.7f
                    : state == State.ERROR ? 0.25f : 0.75f;
            return amplitude * (float) Math.sin(seconds * rate);
        }

        private void projectEdges(int[] edges, byte[] zones, float[] output,
                                  float centerX, float centerY, float unit, float headYaw,
                                  float bodyYaw, float breathing, boolean gold) {
            for (int edge = 0; edge < edges.length / 2; edge++) {
                for (int end = 0; end < 2; end++) {
                    int vertex = edges[edge * 2 + end];
                    int offset = vertex * 3;
                    float x = vertices[offset];
                    float y = vertices[offset + 1];
                    float z = vertices[offset + 2];
                    boolean isHead = gold || (zones != null && zones[edge] == 1);
                    float yaw = isHead ? headYaw : bodyYaw;
                    if (isHead) {
                        float relativeY = y - 54f;
                        y = 54f + relativeY * (1f + (breathing - 1f) * 0.3f);
                    } else {
                        y *= breathing;
                    }
                    float cos = (float) Math.cos(yaw), sin = (float) Math.sin(yaw);
                    float rotatedX = x * cos + z * sin;
                    float rotatedZ = -x * sin + z * cos;
                    float perspective = FOCAL_LENGTH / (CAMERA_DISTANCE - rotatedZ);
                    float depthScale = perspective * unit;
                    int projectedOffset = edge * 4 + end * 2;
                    output[projectedOffset] = centerX + rotatedX * depthScale;
                    output[projectedOffset + 1] = centerY - y * depthScale;
                }
            }
        }

        private static float[] toFloatArray(ArrayList<Float> values) {
            float[] result = new float[values.size()];
            for (int index = 0; index < result.length; index++) result[index] = values.get(index);
            return result;
        }

        private static int[] toIntArray(ArrayList<Integer> values) {
            int[] result = new int[values.size()];
            for (int index = 0; index < result.length; index++) result[index] = values.get(index);
            return result;
        }

        private static byte[] toByteArray(ArrayList<Byte> values) {
            byte[] result = new byte[values.size()];
            for (int index = 0; index < result.length; index++) result[index] = values.get(index);
            return result;
        }
    }

    private static final class MeshBuilder {
        final ArrayList<Float> vertices = new ArrayList<>();
        final ArrayList<Integer> cyanEdges = new ArrayList<>();
        final ArrayList<Integer> goldEdges = new ArrayList<>();
        final ArrayList<Byte> cyanZones = new ArrayList<>();

        void addEllipsoid(float cx, float cy, float cz, float rx, float ry, float rz,
                          int latitudeSteps, int longitudeSteps, byte zone) {
            int[][] points = new int[latitudeSteps + 1][longitudeSteps];
            for (int latitude = 0; latitude <= latitudeSteps; latitude++) {
                double phi = -Math.PI / 2.0 + Math.PI * latitude / latitudeSteps;
                for (int longitude = 0; longitude < longitudeSteps; longitude++) {
                    double theta = 2.0 * Math.PI * longitude / longitudeSteps;
                    float x = cx + rx * (float) Math.cos(phi) * (float) Math.cos(theta);
                    float y = cy + ry * (float) Math.sin(phi);
                    float z = cz + rz * (float) Math.cos(phi) * (float) Math.sin(theta);
                    points[latitude][longitude] = addVertex(x, y, z);
                }
            }
            for (int latitude = 1; latitude < latitudeSteps; latitude++) {
                for (int longitude = 0; longitude < longitudeSteps; longitude++) {
                    addCyanEdge(points[latitude][longitude],
                            points[latitude][(longitude + 1) % longitudeSteps], zone);
                }
            }
            for (int longitude = 0; longitude < longitudeSteps; longitude++) {
                for (int latitude = 0; latitude < latitudeSteps; latitude++) {
                    addCyanEdge(points[latitude][longitude], points[latitude + 1][longitude], zone);
                }
            }
        }

        void addFaceContours() {
            final int segments = 18;
            for (int row = 0; row < 7; row++) {
                float y = 74f - row * 6.2f;
                float halfWidth = 8f + row * 1.2f;
                int previous = -1;
                for (int step = 0; step <= segments; step++) {
                    float x = -halfWidth + 2f * halfWidth * step / segments;
                    float normalized = x / 30f;
                    float normalizedY = (y - 54f) / 36f;
                    float z = 23f * (float) Math.sqrt(Math.max(0.05f,
                            1f - normalized * normalized - normalizedY * normalizedY)) + 0.8f;
                    int current = addVertex(x, y, z);
                    if (previous >= 0) {
                        goldEdges.add(previous);
                        goldEdges.add(current);
                    }
                    previous = current;
                }
            }
            for (int side = -1; side <= 1; side += 2) {
                int previous = -1;
                for (int step = 0; step <= 16; step++) {
                    float y = 76f - step * 2.8f;
                    float x = side * (11f + 0.2f * step);
                    float normalized = x / 29f;
                    float normalizedY = (y - 54f) / 36f;
                    float z = 23f * (float) Math.sqrt(Math.max(0.05f,
                            1f - normalized * normalized - normalizedY * normalizedY)) + 1.1f;
                    int current = addVertex(x, y, z);
                    if (previous >= 0) {
                        goldEdges.add(previous);
                        goldEdges.add(current);
                    }
                    previous = current;
                }
            }
        }

        private int addVertex(float x, float y, float z) {
            int index = vertices.size() / 3;
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);
            return index;
        }

        private void addCyanEdge(int first, int second, byte zone) {
            cyanEdges.add(first);
            cyanEdges.add(second);
            cyanZones.add(zone);
        }
    }
}
