package com.music.player;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

public class PlaybackUIController {
    private final Activity activity;
    private final MusicServiceWrapper serviceWrapper;
    private final Handler mainHandler;
    private final Handler seekbarUpdateHandler = new Handler(Looper.getMainLooper());

    private TextView tvStatus, tvSongTitle, tvCurrentTime, tvTotalTime;
    private ImageView ivMainAlbumArt;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnShuffle, btnRepeat, btnSettings, btnSearch;
    private SeekBar seekBar;
    private EditText etSearch;

    private LinearLayout topPane;
    private RelativeLayout bottomPane;
    private View dragHandle;
    private LinearLayout mainControlsLayout;
    private View headerLayout, playbackProgressLayout;

    private Animation blinkAnimation;
    private float initialTouchY, initialTopPaneWeight, initialBottomPaneWeight;
    private int parentHeight;

    private static final float[] SNAP_TARGET_TOP_WEIGHTS = {10f, 60f, 100f};
    private static final long SNAP_ANIMATION_DURATION = 200;

    public interface MusicServiceWrapper {
        MusicService getService();
        boolean isBound();
    }

    public PlaybackUIController(Activity activity, MusicServiceWrapper serviceWrapper) {
        this.activity = activity;
        this.serviceWrapper = serviceWrapper;
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initViews();
        setupButtons();
        setupSeekBar();
        setupDragHandle();
    }

    private void initViews() {
        tvStatus = activity.findViewById(R.id.tvStatus);
        tvSongTitle = activity.findViewById(R.id.tvSongTitle);
        tvSongTitle.setSelected(true);
        ivMainAlbumArt = activity.findViewById(R.id.ivMainAlbumArt);
        btnSettings = activity.findViewById(R.id.btnSettings);
        btnPlayPause = activity.findViewById(R.id.btnPlayPause);
        btnPrev = activity.findViewById(R.id.btnPrev);
        btnNext = activity.findViewById(R.id.btnNext);
        btnShuffle = activity.findViewById(R.id.btnShuffle);
        btnRepeat = activity.findViewById(R.id.btnRepeat);
        btnSearch = activity.findViewById(R.id.btnSearch);
        seekBar = activity.findViewById(R.id.seekBar);
        etSearch = activity.findViewById(R.id.etSearch);
        tvCurrentTime = activity.findViewById(R.id.tvCurrentTime);
        tvTotalTime = activity.findViewById(R.id.tvTotalTime);
        blinkAnimation = AnimationUtils.loadAnimation(activity, R.anim.blink);

        topPane = activity.findViewById(R.id.top_pane);
        bottomPane = activity.findViewById(R.id.bottom_pane);
        dragHandle = activity.findViewById(R.id.drag_handle);
        mainControlsLayout = activity.findViewById(R.id.main_controls_layout);
        headerLayout = activity.findViewById(R.id.header_layout);
        playbackProgressLayout = activity.findViewById(R.id.playback_progress_layout);
    }

    private void setupButtons() {
        btnPlayPause.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            if (serviceWrapper.isBound()) serviceWrapper.getService().togglePlayPause();
        });
        btnPrev.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            if (serviceWrapper.isBound()) serviceWrapper.getService().playPrevious();
        });
        btnNext.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            if (serviceWrapper.isBound()) serviceWrapper.getService().playNext();
        });
        btnShuffle.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            toggleShuffle();
        });
        btnRepeat.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            toggleRepeat();
        });
        btnSettings.setOnClickListener(v -> {
            v.startAnimation(blinkAnimation);
            activity.startActivity(new Intent(activity, SettingsActivity.class));
        });
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceWrapper.isBound()) {
                    tvCurrentTime.setText(formatDuration(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (serviceWrapper.isBound()) {
                    serviceWrapper.getService().seekTo(seekBar.getProgress());
                }
                seekbarUpdateHandler.post(updateSeekBarRunnable);
            }
        });
    }

    public void updateUI(MusicFile currentMusic, boolean isPlaying) {
        if (currentMusic != null) {
            tvSongTitle.setText(currentMusic.getTitle());
            tvTotalTime.setText(formatDuration(currentMusic.getDuration()));
            seekBar.setMax((int) currentMusic.getDuration());
            loadAlbumArtAsync(currentMusic);
        } else {
            tvSongTitle.setText(Constant.NO_SONG);
        }
        updatePlayState(isPlaying);
        updateShuffleButton();
        updateRepeatButton();
        
        if (serviceWrapper.isBound()) {
            long currentPos = serviceWrapper.getService().getCurrentPosition();
            seekBar.setProgress((int) currentPos);
            tvCurrentTime.setText(formatDuration(currentPos));
        }
    }

    public void updatePlayState(boolean isPlaying) {
        if (isPlaying) {
            tvStatus.setText("Playing");
            tvStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_small, 0, 0, 0);
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            seekbarUpdateHandler.post(updateSeekBarRunnable);
        } else {
            tvStatus.setText("Paused");
            tvStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_small, 0, 0, 0);
            btnPlayPause.setImageResource(R.drawable.ic_play);
            seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
        }
        enableControls(serviceWrapper.isBound() && serviceWrapper.getService().isReady());
    }

    public void enableControls(boolean enable) {
        btnPlayPause.setEnabled(enable);
        if (btnPrev != null) btnPrev.setEnabled(enable);
        if (btnNext != null) btnNext.setEnabled(enable);
    }

    private void toggleShuffle() {
        if (serviceWrapper.isBound()) {
            serviceWrapper.getService().toggleShuffle();
            updateShuffleButton();
        }
    }

    private void toggleRepeat() {
        if (serviceWrapper.isBound()) {
            serviceWrapper.getService().cycleRepeatMode();
            updateRepeatButton();
        }
    }

    public void updateShuffleButton() {
        if (serviceWrapper.isBound() && btnShuffle != null) {
            btnShuffle.setAlpha(serviceWrapper.getService().isShuffleEnabled() ? 1.0f : 0.4f);
        }
    }

    public void updateRepeatButton() {
        if (!serviceWrapper.isBound() || btnRepeat == null) return;
        switch (serviceWrapper.getService().getRepeatMode()) {
            case OFF:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setAlpha(0.4f);
                break;
            case ALL:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setAlpha(1.0f);
                break;
            case ONE:
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnRepeat.setAlpha(1.0f);
                break;
        }
    }

    private String formatDuration(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (serviceWrapper.isBound() && serviceWrapper.getService().isPlaying()) {
                long currentPosition = serviceWrapper.getService().getCurrentPosition();
                seekBar.setProgress((int) currentPosition);
                tvCurrentTime.setText(formatDuration(currentPosition));
                seekbarUpdateHandler.postDelayed(this, 1000);
            }
        }
    };

    private void loadAlbumArtAsync(final MusicFile musicFile) {
        new Thread(() -> {
            byte[] albumArt = musicFile.getAlbumArt();
            final Bitmap bitmap = (albumArt != null) ? BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length) : null;
            mainHandler.post(() -> {
                if (bitmap != null) ivMainAlbumArt.setImageBitmap(bitmap);
                else ivMainAlbumArt.setImageResource(R.mipmap.ic_launcher);
            });
        }).start();
    }

    private void setupDragHandle() {
        dragHandle.post(new Runnable() {
            @Override
            public void run() {
                parentHeight = ((View) topPane.getParent()).getHeight();
                if (parentHeight == 0) {
                    dragHandle.postDelayed(this, 100);
                    return;
                }
                dragHandle.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        final LinearLayout.LayoutParams topParams = (LinearLayout.LayoutParams) topPane.getLayoutParams();
                        final LinearLayout.LayoutParams bottomParams = (LinearLayout.LayoutParams) bottomPane.getLayoutParams();
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialTouchY = event.getRawY();
                                initialTopPaneWeight = topParams.weight;
                                initialBottomPaneWeight = bottomParams.weight;
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                float deltaY = event.getRawY() - initialTouchY;
                                float weightChange = (deltaY / (float) parentHeight) * (initialTopPaneWeight + initialBottomPaneWeight);
                                float newTopWeight = Math.max(1f, Math.min(99f, initialTopPaneWeight + weightChange));
                                topParams.weight = newTopWeight;
                                bottomParams.weight = (initialTopPaneWeight + initialBottomPaneWeight) - newTopWeight;
                                topPane.setLayoutParams(topParams);
                                bottomPane.setLayoutParams(bottomParams);
                                updateTopPaneContentVisibility(newTopWeight);
                                return true;
                            case MotionEvent.ACTION_UP:
                                snapToTarget(topParams, bottomParams, initialTopPaneWeight + initialBottomPaneWeight);
                                return true;
                        }
                        return false;
                    }
                });
                updateTopPaneContentVisibility(((LinearLayout.LayoutParams) topPane.getLayoutParams()).weight);
            }
        });
    }

    private void snapToTarget(LinearLayout.LayoutParams topParams, LinearLayout.LayoutParams bottomParams, float totalWeight) {
        float currentTopWeight = topParams.weight;
        float closestTargetWeight = SNAP_TARGET_TOP_WEIGHTS[0];
        float minDifference = Math.abs(currentTopWeight - closestTargetWeight);

        for (int i = 1; i < SNAP_TARGET_TOP_WEIGHTS.length; i++) {
            float target = SNAP_TARGET_TOP_WEIGHTS[i];
            float diff = Math.abs(currentTopWeight - target);
            if (diff < minDifference) {
                minDifference = diff;
                closestTargetWeight = target;
            }
        }

        ValueAnimator animator = ValueAnimator.ofFloat(currentTopWeight, closestTargetWeight);
        animator.setDuration(SNAP_ANIMATION_DURATION);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedWeight = (float) animation.getAnimatedValue();
            topParams.weight = animatedWeight;
            bottomParams.weight = totalWeight - animatedWeight;
            topPane.setLayoutParams(topParams);
            bottomPane.setLayoutParams(bottomParams);
            updateTopPaneContentVisibility(animatedWeight);
        });
        animator.start();
    }

    public void expandToTop() {
        if (topPane == null || bottomPane == null) return;
        
        final LinearLayout.LayoutParams topParams = (LinearLayout.LayoutParams) topPane.getLayoutParams();
        final LinearLayout.LayoutParams bottomParams = (LinearLayout.LayoutParams) bottomPane.getLayoutParams();
        float currentTopWeight = topParams.weight;
        float targetTopWeight = SNAP_TARGET_TOP_WEIGHTS[0]; // 10f
        float totalWeight = currentTopWeight + bottomParams.weight;

        ValueAnimator animator = ValueAnimator.ofFloat(currentTopWeight, targetTopWeight);
        animator.setDuration(SNAP_ANIMATION_DURATION);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedWeight = (float) animation.getAnimatedValue();
            topParams.weight = animatedWeight;
            bottomParams.weight = totalWeight - animatedWeight;
            topPane.setLayoutParams(topParams);
            bottomPane.setLayoutParams(bottomParams);
            updateTopPaneContentVisibility(animatedWeight);
        });
        animator.start();
    }

    private void updateTopPaneContentVisibility(float currentTopWeight) {
        int contentVisibility = (currentTopWeight <= 25f) ? View.GONE : View.VISIBLE;
        
        int searchBtnVisibility = (currentTopWeight < 90f) ? View.VISIBLE : View.GONE;
        
        ivMainAlbumArt.setVisibility(contentVisibility);
        if (mainControlsLayout != null) {
            mainControlsLayout.setVisibility(View.VISIBLE);
        }
        
        if (headerLayout != null) {
            headerLayout.setVisibility(View.VISIBLE);
        }
        
        if (playbackProgressLayout != null) {
            playbackProgressLayout.setVisibility(View.VISIBLE);
        }
        
        if (btnSearch != null) {
            btnSearch.setVisibility(searchBtnVisibility);
        }
        
        // When player is expanded (weight > 30), make sure search bar is hidden
        if (currentTopWeight > 30f && etSearch != null && etSearch.getVisibility() == View.VISIBLE) {
            etSearch.setVisibility(View.GONE);
            etSearch.setText(Constant.EMPTY_STRING);
            // Filter reset is handled by TextWatcher
        }
    }

    public void onMusicFinished() {
        seekbarUpdateHandler.removeCallbacks(updateSeekBarRunnable);
        seekBar.setProgress(0);
        tvCurrentTime.setText(formatDuration(0));
    }
}
