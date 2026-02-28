package com.music.player.Visualizer;

import com.music.player.FileLogger;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class PcmVisualizerSource {

    private static final String TAG = "PcmVisualizerSource";

    // Ring buffer size in samples (must be power-of-2 >= FFT_SIZE)
    private static final int RING_BUFFER_SIZE = 2048;

    // Decode thread reads up to MAX_DECODE_AHEAD_MS ahead of current playback
    // to avoid decoding too far ahead of the player.
    private static final long MAX_DECODE_AHEAD_MS = 300;

    private final short[] ringBuffer = new short[RING_BUFFER_SIZE];
    private volatile int writePos = 0;
    private final Object bufferLock = new Object();

    private Thread decodeThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean paused = false;

    private String currentPath = null;
    private volatile long playbackMs = 0;

    private final FileLogger fileLogger;

    public interface PositionProvider {
        long getCurrentPositionMs();
    }

    private PositionProvider positionProvider;

    public PcmVisualizerSource(FileLogger fileLogger) {
        this.fileLogger = fileLogger;
    }

    public void setPositionProvider(PositionProvider provider) {
        this.positionProvider = provider;
    }

    public synchronized void start(String filePath) {
        stop();
        currentPath = filePath;
        running.set(true);
        paused = false;

        decodeThread = new Thread(this::decodeLoop, "PcmDecodeThread");
        decodeThread.setPriority(Thread.MIN_PRIORITY); // Avoid interfering with UI/audio
        decodeThread.start();
        fileLogger.i(TAG, "Started decoding: " + filePath);
    }

    public void setPaused(boolean isPaused) {
        this.paused = isPaused;
    }

    public synchronized void stop() {
        running.set(false);
        if (decodeThread != null) {
            decodeThread.interrupt();
            try {
                decodeThread.join(300);
            } catch (InterruptedException ignored) {
            }
            decodeThread = null;
        }
        clearBuffer();
        currentPath = null;
        playbackMs = 0;
    }

    public void getLatestSamples(short[] out) {
        synchronized (bufferLock) {
            int len = Math.min(out.length, RING_BUFFER_SIZE);
            int start = (writePos - len + RING_BUFFER_SIZE) % RING_BUFFER_SIZE;

            if (start + len <= RING_BUFFER_SIZE) {
                System.arraycopy(ringBuffer, start, out, 0, len);
            } else {
                int firstPart = RING_BUFFER_SIZE - start;
                System.arraycopy(ringBuffer, start, out, 0, firstPart);
                System.arraycopy(ringBuffer, 0, out, firstPart, len - firstPart);
            }
        }
    }

    private void decodeLoop() {
        MediaExtractor extractor = null;
        MediaCodec codec = null;

        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(currentPath);

            // Find audio track
            int audioTrack = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat fmt = extractor.getTrackFormat(i);
                String mime = fmt.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrack = i;
                    break;
                }
            }

            if (audioTrack < 0) {
                fileLogger.e(TAG, "No audio track found");
                return;
            }

            extractor.selectTrack(audioTrack);
            MediaFormat format = extractor.getTrackFormat(audioTrack);
            String mime = format.getString(MediaFormat.KEY_MIME);

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            fileLogger.i(TAG, "Codec started: " + mime);

            decodeFrames(extractor, codec);

        } catch (IOException e) {
            fileLogger.e(TAG, "Decode error: " + e);
        } catch (InterruptedException e) {
            fileLogger.i(TAG, "Decode thread interrupted");
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                    codec.release();
                } catch (Exception e) {
                    fileLogger.e(TAG, "Unexpected error: " + e);
                }
            }
            if (extractor != null) {
                try {
                    extractor.release();
                } catch (Exception e) {
                    fileLogger.e(TAG, "Unexpected error: " + e);
                }
            }
            fileLogger.i(TAG, "Decode thread finished");
        }
    }

    private void decodeFrames(MediaExtractor extractor, MediaCodec codec)
            throws InterruptedException {

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        boolean outputDone = false;

        // Last presentation time decoded (microseconds)
        long lastDecodedUs = 0;

        while (running.get()) {

            // Update playback position from provider
            if (positionProvider != null) {
                playbackMs = positionProvider.getCurrentPositionMs();
            }

            // Sleep to save CPU when paused
            if (paused) {
                Thread.sleep(50);
                continue;
            }

            // If playback position jumps significantly (seek/loop),
            // synchronize the decoder
            long lastDecodedMs = lastDecodedUs / 1000;
            if (Math.abs(playbackMs - lastDecodedMs) > 1000) {
                extractor.seekTo(playbackMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                codec.flush();
                lastDecodedUs = extractor.getSampleTime();
                inputDone = false;
                outputDone = false;
                continue;
            }

            if (outputDone) {
                Thread.sleep(100);
                continue;
            }

            // Throttle: don't decode too far ahead of player position
            long aheadMs = (lastDecodedUs / 1000) - playbackMs;
            if (aheadMs > MAX_DECODE_AHEAD_MS) {
                Thread.sleep(20);
                continue;
            }

            // input: feed compressed data to codec
            if (!inputDone) {
                int inIdx = codec.dequeueInputBuffer(5000);
                if (inIdx >= 0) {
                    ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                    if (inBuf != null) {
                        inBuf.clear();
                        int sampleSize = extractor.readSampleData(inBuf, 0);
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long presentationUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inIdx, 0, sampleSize, presentationUs, 0);
                            extractor.advance();
                        }
                    }
                }
            }

            // output: retrieve PCM from codec
            int outIdx = codec.dequeueOutputBuffer(info, 5000);
            if (outIdx >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }

                ByteBuffer outBuf = codec.getOutputBuffer(outIdx);
                if (outBuf != null && info.size > 0) {
                    outBuf.position(info.offset);
                    outBuf.limit(info.offset + info.size);
                    ShortBuffer shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

                    lastDecodedUs = info.presentationTimeUs;
                    writeSamples(shorts);
                }

                codec.releaseOutputBuffer(outIdx, false);
            }
        }
    }

    private void writeSamples(ShortBuffer src) {
        int count = src.remaining();
        if (count == 0)
            return;

        // Skip older samples if there are too many, we only need the latest for the
        // visualizer
        if (count > RING_BUFFER_SIZE) {
            src.position(src.position() + (count - RING_BUFFER_SIZE));
            count = RING_BUFFER_SIZE;
        }

        synchronized (bufferLock) {
            int firstPart = Math.min(count, RING_BUFFER_SIZE - writePos);
            src.get(ringBuffer, writePos, firstPart);

            if (count > firstPart) {
                src.get(ringBuffer, 0, count - firstPart);
            }

            writePos = (writePos + count) % RING_BUFFER_SIZE;
        }
    }

    private void clearBuffer() {
        synchronized (bufferLock) {
            java.util.Arrays.fill(ringBuffer, (short) 0);
            writePos = 0;
        }
    }
}
