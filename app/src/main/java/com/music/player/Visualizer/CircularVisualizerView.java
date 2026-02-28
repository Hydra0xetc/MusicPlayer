package com.music.player.Visualizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.PorterDuff.Mode;

import com.music.player.FftAnalyzer;
import com.music.player.FileLogger;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

public class CircularVisualizerView extends TextureView implements TextureView.SurfaceTextureListener {
    private final String TAG = "CircularVisualizerView";

    private static final int BAR_COUNT = 80;
    private static final float INNER_RATIO = 0.53f;
    private static final float MAX_BAR_RATIO = 0.60f;
    private static final float BAR_FILL_RATIO = 0.45f;
    private static final float ALBUM_ART_SCALE = 1.25f;
    private static final float SMOOTHING = 0.55f;
    private static final float DECAY_SPEED = 0.12f;
    private static final int COLOR_LOW = 0xFF00BCD4; // Cyan
    private static final int COLOR_HIGH = 0xFF03DAC6; // Teal

    // Pre-extracted color components
    private static final int R_LOW = (COLOR_LOW >> 16) & 0xFF;
    private static final int G_LOW = (COLOR_LOW >> 8) & 0xFF;
    private static final int B_LOW = COLOR_LOW & 0xFF;
    private static final int R_DIFF = ((COLOR_HIGH >> 16) & 0xFF) - R_LOW;
    private static final int G_DIFF = ((COLOR_HIGH >> 8) & 0xFF) - G_LOW;
    private static final int B_DIFF = (COLOR_HIGH & 0xFF) - B_LOW;

    private PcmVisualizerSource pcmSource;
    private final FftAnalyzer fftAnalyzer = new FftAnalyzer();
    private final short[] pcmSnapshot = new short[512];
    private final float[] fftMagnitudes = new float[FftAnalyzer.getBinCount()];
    private final float[] smoothedMagnitudes = new float[BAR_COUNT];

    // Pre-calculated values for performance
    private final float[] cosAngles = new float[BAR_COUNT / 2];
    private final float[] sinAngles = new float[BAR_COUNT / 2];
    private final int[] binStarts = new int[BAR_COUNT / 2];
    private final int[] binEnds = new int[BAR_COUNT / 2];

    private FileLogger fileLogger;
    private volatile boolean isPlaying = false;
    private volatile Bitmap albumArtBmp = null;

    private RenderThread renderThread;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CircularVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CircularVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
        setOpaque(false);

        bgCircle.setColor(0xCC0D0D1A);
        bgCircle.setStyle(Paint.Style.FILL);

        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeCap(Paint.Cap.ROUND);

        precomputeValues();
    }

    private void precomputeValues() {
        int half = BAR_COUNT / 2;
        int totalBins = FftAnalyzer.getBinCount();
        float step = (float) Math.log(totalBins + 1) / half;

        for (int i = 0; i < half; i++) {
            // Precompute angles
            float angle = (float) (Math.PI / 2.0 + (i * Math.PI / half));
            cosAngles[i] = (float) Math.cos(angle);
            sinAngles[i] = (float) Math.sin(angle);

            // Precompute bin mapping (logarithmic scale)
            binStarts[i] = (int) (Math.exp(i * step) - 1);
            binEnds[i] = Math.max(binStarts[i] + 1, (int) (Math.exp((i + 1) * step) - 1));
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startRenderThread();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopRenderThread();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void setLogger(FileLogger fileLogger) {
        this.fileLogger = fileLogger;
    }

    public void setPcmSource(PcmVisualizerSource source) {
        this.pcmSource = source;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (pcmSource != null)
            pcmSource.setPaused(!playing);
    }

    public void setAlbumArt(Bitmap bmp) {
        this.albumArtBmp = bmp;
    }

    private void startRenderThread() {
        stopRenderThread();
        renderThread = new RenderThread();
        renderThread.start();
    }

    private void stopRenderThread() {
        if (renderThread != null) {
            renderThread.quit();
            try {
                renderThread.join(400);
            } catch (InterruptedException ignored) {
            }
            renderThread = null;
        }
    }

    private class RenderThread extends Thread {
        private static final long TARGET_FRAME_MS = 1000 / 60;
        private volatile boolean running = true;

        void quit() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                long frameStart = System.currentTimeMillis();
                Canvas canvas = null;
                try {
                    canvas = lockCanvas();
                    if (canvas != null) {
                        updateFftAndSmoothing();
                        drawFrame(canvas);
                    }
                } catch (Exception e) {
                    fileLogger.e(TAG, "Render error: " + e);
                } finally {
                    if (canvas != null) {
                        try {
                            unlockCanvasAndPost(canvas);
                        } catch (Exception e) {
                            fileLogger.e(TAG, "Failed to unlockCanvasAndPost: " + e);
                        }
                    }
                }
                long sleep = TARGET_FRAME_MS - (System.currentTimeMillis() - frameStart);
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        fileLogger.e(TAG, "Unexpected error: " + e);
                    }
                }
            }
        }
    }

    private void updateFftAndSmoothing() {
        if (isPlaying && pcmSource != null) {
            pcmSource.getLatestSamples(pcmSnapshot);

            fftAnalyzer.analyze(pcmSnapshot, fftMagnitudes);

            int half = BAR_COUNT / 2;
            int totalBins = fftMagnitudes.length;

            for (int i = 0; i < half; i++) {
                int binStart = binStarts[i];
                int binEnd = binEnds[i];

                float sum = 0;
                int count = 0;
                for (int b = binStart; b < binEnd && b < totalBins; b++) {
                    sum += fftMagnitudes[b];
                    count++;
                }

                float target = count > 0 ? sum / count : 0;

                if (i < half / 4) {
                    target *= 1.3f;
                }

                float boost = 1.0f + 0.5f * ((float) i / half);
                target *= boost;

                float current = smoothedMagnitudes[i];
                float attackSpeed = SMOOTHING * 1.8f;
                float releaseSpeed = SMOOTHING * 0.4f;
                float speed = target > current ? attackSpeed : releaseSpeed;

                smoothedMagnitudes[i] = current + (target - current) * speed;
            }

            for (int i = 1; i < half - 1; i++) {
                smoothedMagnitudes[i] = (smoothedMagnitudes[i - 1] + 2f * smoothedMagnitudes[i]
                        + smoothedMagnitudes[i + 1]) / 4f;
            }
        } else {
            for (int i = 0; i < BAR_COUNT; i++) {
                smoothedMagnitudes[i] *= (1f - DECAY_SPEED);
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawColor(0, Mode.CLEAR);

        final float w = canvas.getWidth();
        final float h = canvas.getHeight();
        final float cx = w / 2f;
        final float cy = h / 2f;

        final float halfMin = Math.min(w, h) / 2f;
        final float innerRadius = halfMin * INNER_RATIO;
        final float maxBarLen = halfMin * MAX_BAR_RATIO;

        final float barWidth = (float) (2 * Math.PI * (innerRadius + maxBarLen * 0.5f) / BAR_COUNT)
                * BAR_FILL_RATIO;
        barPaint.setStrokeWidth(Math.max(barWidth, 3f));

        int half = BAR_COUNT / 2;
        for (int i = 0; i < half; i++) {
            float height = smoothedMagnitudes[i];
            float outerR = innerRadius + Math.max(height * maxBarLen, 4f);
            int color = lerpColor(COLOR_LOW, COLOR_HIGH, height);
            barPaint.setColor(color);

            float cosA = cosAngles[i];
            float sinA = sinAngles[i];

            // Right side
            float x1 = cx + cosA * innerRadius;
            float y1 = cy + sinA * innerRadius;
            float x2 = cx + cosA * outerR;
            float y2 = cy + sinA * outerR;
            canvas.drawLine(x1, y1, x2, y2, barPaint);

            // Left side (symmetry)
            if (i > 0) {
                float x1L = cx - cosA * innerRadius;
                float x2L = cx - cosA * outerR;
                canvas.drawLine(x1L, y1, x2L, y2, barPaint);
            }
        }

        canvas.drawCircle(cx, cy, innerRadius + 2f, bgCircle);

        float artRadius = innerRadius - 4f;
        Bitmap bmp = albumArtBmp;
        if (bmp != null && !bmp.isRecycled()) {
            drawCircularBitmap(canvas, bmp, cx, cy, artRadius);
        } else {
            drawDefaultMusicIcon(canvas, cx, cy, artRadius);
        }
    }

    private void drawCircularBitmap(Canvas canvas, Bitmap bmp, float cx, float cy, float radius) {
        int saveCount = canvas.save();

        Path clip = new Path();
        clip.addCircle(cx, cy, radius, Path.Direction.CW);
        canvas.clipPath(clip);

        float bmpW = bmp.getWidth();
        float bmpH = bmp.getHeight();
        float diameter = radius * 2f;
        float scale = Math.max(diameter / bmpW, diameter / bmpH) * ALBUM_ART_SCALE;
        float drawW = bmpW * scale;
        float drawH = bmpH * scale;

        RectF dst = new RectF(
                cx - drawW / 2f,
                cy - drawH / 2f,
                cx + drawW / 2f,
                cy + drawH / 2f);
        canvas.drawBitmap(bmp, null, dst, imagePaint);

        canvas.restoreToCount(saveCount);

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);
        ringPaint.setColor(0x66FFFFFF);
        canvas.drawCircle(cx, cy, radius, ringPaint);
    }

    private void drawDefaultMusicIcon(Canvas canvas, float cx, float cy, float radius) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFF161630);
        canvas.drawCircle(cx, cy, radius, bgPaint);

        Drawable drawable = getContext().getDrawable(com.music.player.R.mipmap.ic_launcher);
        if (drawable != null) {
            int saveCount = canvas.save();

            Path clip = new Path();
            clip.addCircle(cx, cy, radius, Path.Direction.CW);
            canvas.clipPath(clip);

            float intrinsicW = drawable.getIntrinsicWidth();
            float intrinsicH = drawable.getIntrinsicHeight();
            if (intrinsicW <= 0) {
                intrinsicW = radius * 2;
            }

            if (intrinsicH <= 0) {
                intrinsicH = radius * 2;
            }

            float diameter = radius * 2f;
            float scale = Math.max(diameter / intrinsicW, diameter / intrinsicH) * ALBUM_ART_SCALE;
            int drawW = (int) (intrinsicW * scale);
            int drawH = (int) (intrinsicH * scale);

            int left = (int) (cx - drawW / 2f);
            int top = (int) (cy - drawH / 2f);

            drawable.setBounds(left, top, left + drawW, top + drawH);
            drawable.draw(canvas);

            canvas.restoreToCount(saveCount);
        }

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);
        ringPaint.setColor(0x44FFFFFF);
        canvas.drawCircle(cx, cy, radius, ringPaint);
    }

    private static int lerpColor(int a, int b, float t) {
        if (t <= 0f)
            return a;
        if (t >= 1f)
            return b;
        return 0xFF000000
                | ((int) (R_LOW + R_DIFF * t) << 16)
                | ((int) (G_LOW + G_DIFF * t) << 8)
                | ((int) (B_LOW + B_DIFF * t));
    }
}
