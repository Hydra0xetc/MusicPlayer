package com.music.player;

public class FftAnalyzer {

    // Must be power-of-2 (128, 256, 512, 1024, ...)
    // Larger = better frequency resolution, but slower
    private static final int FFT_SIZE = 512;

    // Window function to reduce spectral leakage
    private static final float[] WINDOW = buildHannWindow(FFT_SIZE);

    // Precomputed roots for FFT (butterfly coefficients)
    private static final float[] COS_TABLE;
    private static final float[] SIN_TABLE;

    static {
        int levels = (int) (Math.log(FFT_SIZE) / Math.log(2));
        COS_TABLE = new float[levels];
        SIN_TABLE = new float[levels];
        for (int i = 0; i < levels; i++) {
            int len = 1 << (i + 1);
            double ang = -2.0 * Math.PI / len;
            COS_TABLE[i] = (float) Math.cos(ang);
            SIN_TABLE[i] = (float) Math.sin(ang);
        }
    }

    // Re-use arrays to avoid allocations per frame
    private final float[] re = new float[FFT_SIZE];
    private final float[] im = new float[FFT_SIZE];
    private final float[] mag = new float[FFT_SIZE / 2];

    // Number of output bins = FFT_SIZE / 2
    public static int getBinCount() {
        return FFT_SIZE / 2;
    }

    public void analyze(short[] pcm, float[] out) {
        if (pcm == null || out == null)
            return;

        int len = Math.min(pcm.length, FFT_SIZE);

        // Fill real part with windowed samples, imaginary = 0
        // Use multiplier to avoid divisions inside the loop
        final float invShortMax = 1.0f / 32768.0f;
        for (int i = 0; i < FFT_SIZE; i++) {
            if (i < len) {
                re[i] = (pcm[i] * invShortMax) * WINDOW[i];
            } else {
                re[i] = 0f;
            }
            im[i] = 0f;
        }

        // Calculate FFT in-place
        fft(re, im, FFT_SIZE);

        // Calculate magnitude for each bin
        float maxMag = 1e-6f; // avoid div-by-zero
        for (int i = 0; i < FFT_SIZE / 2; i++) {
            // Approximation sqrt might be faster but Math.sqrt is fine here
            mag[i] = (float) Math.sqrt(re[i] * re[i] + im[i] * im[i]);
            if (mag[i] > maxMag)
                maxMag = mag[i];
        }

        // Normalize to [0, 1] with log scaling so bass doesn't dominate
        int outLen = Math.min(out.length, FFT_SIZE / 2);
        final float invMaxMag = 1.0f / maxMag;
        for (int i = 0; i < outLen; i++) {
            float normalized = mag[i] * invMaxMag;
            // Log scale: looks more natural to the human ear
            out[i] = (float) (Math.log10(1 + 9 * normalized));
            out[i] = Math.max(0f, Math.min(1f, out[i]));
        }
    }

    private static void fft(float[] re, float[] im, int n) {
        // Bit-reversal permutation
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }
            j ^= bit;
            if (i < j) {
                float tmpR = re[i];
                re[i] = re[j];
                re[j] = tmpR;
                float tmpI = im[i];
                im[i] = im[j];
                im[j] = tmpI;
            }
        }

        // FFT butterfly
        int level = 0;
        for (int len = 2; len <= n; len <<= 1) {
            float wRe = COS_TABLE[level];
            float wIm = SIN_TABLE[level];
            level++;

            for (int i = 0; i < n; i += len) {
                float curRe = 1f, curIm = 0f;
                int halfLen = len >> 1;
                for (int j = 0; j < halfLen; j++) {
                    int idx1 = i + j;
                    int idx2 = i + j + halfLen;

                    float vR = re[idx2] * curRe - im[idx2] * curIm;
                    float vI = re[idx2] * curIm + im[idx2] * curRe;

                    re[idx2] = re[idx1] - vR;
                    im[idx2] = im[idx1] - vI;
                    re[idx1] += vR;
                    im[idx1] += vI;

                    float nextCurRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nextCurRe;
                }
            }
        }
    }

    private static float[] buildHannWindow(int size) {
        float[] w = new float[size];
        for (int i = 0; i < size; i++) {
            w[i] = (float) (0.5 * (1 - Math.cos(2 * Math.PI * i / (size - 1))));
        }
        return w;
    }
}
