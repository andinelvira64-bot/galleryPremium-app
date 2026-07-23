package com.elvira.gallery;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.util.List;

public class MediaPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PHOTO = 0;
    private static final int TYPE_VIDEO = 1;

    /** How long the controls (and the shared close button) stay visible after
     *  a tap, or after the video starts playing, before auto-hiding. */
    private static final long AUTO_HIDE_DELAY_MS = 3000L;
    private static final long PROGRESS_UPDATE_MS = 400L;

    private final List<String> paths;
    private final List<Integer> types;
    private final SparseArray<VideoVH> boundVideoHolders = new SparseArray<>();

    /** Notified whenever a video page's controls show/hide, so the activity can
     *  keep the shared close (X) button in sync with them. */
    public interface ControlsVisibilityListener {
        void onControlsVisibilityChanged(int position, boolean visible);
    }

    /** Notified when the video at {@code position} finishes playing, so the
     *  activity can auto-advance the ViewPager2 to the next page. */
    public interface VideoCompletionListener {
        void onVideoCompleted(int position);
    }

    /** Notified whenever a photo page's pinch-zoom state changes, so the
     *  activity can disable ViewPager2 swiping while the user pans around a
     *  zoomed-in photo (otherwise a horizontal pan gets stolen as a page-swipe). */
    public interface ZoomStateListener {
        void onZoomStateChanged(int position, boolean zoomedIn);
    }

    /** Notified when the user taps the landscape/fullscreen button on a video
     *  page, so the activity can actually rotate the screen (the adapter has
     *  no window/activity access of its own). */
    public interface FullscreenToggleListener {
        void onFullscreenToggleRequested();
    }

    private ControlsVisibilityListener controlsVisibilityListener;
    private VideoCompletionListener videoCompletionListener;
    private ZoomStateListener zoomStateListener;
    private FullscreenToggleListener fullscreenToggleListener;

    /** Whether the activity is currently forcing landscape/fullscreen video
     *  playback. Kept here (rather than only on one ViewHolder) so that any
     *  video page - including ones the user swipes to afterwards - shows the
     *  correct enter/exit icon. */
    private boolean fullscreenActive = false;

    /** The position ViewPager2 is actually showing right now. ViewPager2 pre-binds
     *  the next page in the background for smooth swiping, but that page must NOT
     *  start playing/making sound until it's really the one on screen - otherwise
     *  its audio plays underneath the current video's audio, and by the time the
     *  user swipes to it, it may have already silently finished off-screen. */
    private int activePosition = -1;

    public MediaPagerAdapter(List<String> paths, List<Integer> types) {
        this.paths = paths;
        this.types = types;
    }

    public void setControlsVisibilityListener(ControlsVisibilityListener listener) {
        this.controlsVisibilityListener = listener;
    }

    public void setVideoCompletionListener(VideoCompletionListener listener) {
        this.videoCompletionListener = listener;
    }

    public void setZoomStateListener(ZoomStateListener listener) {
        this.zoomStateListener = listener;
    }

    public void setFullscreenToggleListener(FullscreenToggleListener listener) {
        this.fullscreenToggleListener = listener;
    }

    /** Called by the activity right after it actually rotates the screen, so
     *  every currently bound video page (only one is visible, but any others
     *  ViewPager2 keeps pre-bound in the background) shows the matching
     *  enter/exit fullscreen icon. */
    public void setFullscreenState(boolean active) {
        fullscreenActive = active;
        for (int i = 0; i < boundVideoHolders.size(); i++) {
            boundVideoHolders.valueAt(i).updateFullscreenIcon(active);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return types.get(position) == MediaItem.TYPE_VIDEO ? TYPE_VIDEO : TYPE_PHOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_VIDEO) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_page, parent, false);
            return new VideoVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo_page, parent, false);
            return new PhotoVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String path = paths.get(position);
        if (holder instanceof PhotoVH) {
            PhotoVH vh = (PhotoVH) holder;
            vh.bind(controlsVisibilityListener, zoomStateListener);
            // Signature keyed on the file's last-modified time so that a
            // crop (which overwrites the same path) is picked up immediately
            // instead of showing Glide's stale cached bitmap for that path.
            Glide.with(vh.photoView.getContext())
                    .load(path)
                    .signature(new ObjectKey(new File(path).lastModified()))
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model,
                                                     Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            vh.zoomController.resetForNewImage();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                                        Target<android.graphics.drawable.Drawable> target,
                                                        DataSource dataSource, boolean isFirstResource) {
                            vh.zoomController.resetForNewImage();
                            return false;
                        }
                    })
                    .into(vh.photoView);
        } else if (holder instanceof VideoVH) {
            VideoVH vh = (VideoVH) holder;
            boundVideoHolders.put(position, vh);
            vh.bind(position, path, controlsVisibilityListener, videoCompletionListener,
                    fullscreenToggleListener, fullscreenActive, position == activePosition);
        }
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof VideoVH) {
            VideoVH vh = (VideoVH) holder;
            vh.release();
            int key = boundVideoHolders.indexOfValue(vh);
            if (key >= 0) {
                boundVideoHolders.removeAt(key);
            }
        }
    }

    /** Call this from ViewPager2's onPageSelected. Pauses every other bound video
     *  (so a pre-loaded neighbor page never keeps playing in the background), and
     *  makes {@code position} the one allowed to actually play - starting it if
     *  it was only silently pre-loaded, or resuming it if the user swipes back to
     *  a video they'd paused earlier. */
    public void setActivePosition(int position) {
        activePosition = position;
        for (int i = 0; i < boundVideoHolders.size(); i++) {
            int key = boundVideoHolders.keyAt(i);
            VideoVH vh = boundVideoHolders.valueAt(i);
            if (key != position) {
                vh.isActive = false;
                if (vh.videoView.isPlaying()) {
                    vh.videoView.pause();
                }
            }
        }
        VideoVH activeHolder = boundVideoHolders.get(position);
        if (activeHolder != null) {
            activeHolder.becomeActive();
        }
    }

    static class PhotoVH extends RecyclerView.ViewHolder {
        ImageView photoView;
        View tapCatcher;
        boolean controlsVisible = true;
        PhotoZoomController zoomController;

        PhotoVH(View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
            tapCatcher = itemView.findViewById(R.id.tapCatcher);
        }

        void bind(ControlsVisibilityListener listener, ZoomStateListener zoomListener) {
            // Every fresh bind (including recycled views scrolled back into
            // view) starts with the back/menu buttons visible.
            controlsVisible = true;

            // The zoom controller owns tapCatcher's touch handling from here
            // on (single tap, pinch-zoom, one-finger pan once zoomed in), so
            // it's created once per holder and just reused across re-binds -
            // its resetForNewImage() (called from the Glide listener above)
            // is what snaps zoom back to "fit" for whatever photo is now shown.
            if (zoomController == null) {
                zoomController = new PhotoZoomController(tapCatcher, photoView, new PhotoZoomController.Callbacks() {
                    @Override
                    public void onZoomChanged(boolean zoomedIn) {
                        int pos = getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && zoomListener != null) {
                            zoomListener.onZoomStateChanged(pos, zoomedIn);
                        }
                    }

                    @Override
                    public void onSingleTap() {
                        int pos = getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;
                        controlsVisible = !controlsVisible;
                        if (listener != null) {
                            listener.onControlsVisibilityChanged(pos, controlsVisible);
                        }
                    }
                });
            }
        }
    }

    /** Video page holder with its own custom playback controls (play/pause, rewind
     *  10s, forward 10s, seek bar). We intentionally don't use the framework's
     *  built-in MediaController: it can't be kept in sync with our close button,
     *  and its buttons can't be restyled the way this app wants (iPhone-style
     *  everywhere except the play/pause and skip buttons, which stay white). */
    static class VideoVH extends RecyclerView.ViewHolder {
        VideoView videoView;
        View tapCatcher;
        View controlsBar;
        ImageButton btnPlayPause;
        // The audio session id currently announced to the ASsound audio engine,
        // or 0 if none is announced right now. Needed so we can tell it to
        // detach (ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) once this
        // session goes away, instead of leaking it forever.
        private int announcedAudioSessionId = 0;
        ImageButton btnRewind;
        ImageButton btnForward;
        ImageButton btnFullscreen;
        SeekBar seekBar;

        int position;
        boolean controlsVisible = true;
        boolean userSeeking = false;
        boolean isActive = false;
        boolean prepared = false;
        ControlsVisibilityListener listener;
        VideoCompletionListener completionListener;
        FullscreenToggleListener fullscreenListener;

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable hideRunnable = this::hideControls;
        final Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!userSeeking && videoView.isPlaying()) {
                    int duration = videoView.getDuration();
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(videoView.getCurrentPosition());
                    }
                }
                handler.postDelayed(this, PROGRESS_UPDATE_MS);
            }
        };

        VideoVH(View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            tapCatcher = itemView.findViewById(R.id.tapCatcher);
            controlsBar = itemView.findViewById(R.id.controlsBar);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            btnRewind = itemView.findViewById(R.id.btnRewind);
            btnForward = itemView.findViewById(R.id.btnForward);
            btnFullscreen = itemView.findViewById(R.id.btnFullscreen);
            seekBar = itemView.findViewById(R.id.seekBar);
        }

        void bind(int position, String path, ControlsVisibilityListener listener,
                  VideoCompletionListener completionListener,
                  FullscreenToggleListener fullscreenListener, boolean fullscreenActive,
                  boolean isActive) {
            this.position = position;
            this.listener = listener;
            this.completionListener = completionListener;
            this.fullscreenListener = fullscreenListener;
            this.isActive = isActive;
            this.prepared = false;
            updateFullscreenIcon(fullscreenActive);

            // Recycled view holders may already have a previous video's session
            // announced - close it before opening a new one for this video.
            closeAnnouncedAudioSession();

            videoView.setVideoURI(Uri.parse(path));
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                seekBar.setMax(mp.getDuration());
                prepared = true;
                // This is the actual fix for the equalizer having no effect:
                // the ASsound audio engine only attaches an Equalizer/LoudnessEnhancer
                // to an audio session once it receives
                // AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION for that
                // session id. Nothing was ever sending that broadcast for this
                // player's own MediaPlayer, so the service had nothing to attach
                // to and the equalizer had no effect on this app's own video/audio.
                openAudioSession(mp.getAudioSessionId());
                if (this.isActive) {
                    // This page is genuinely the one on screen right now (not
                    // just pre-loaded ahead of time by ViewPager2) - safe to
                    // auto-play with no need to tap the play button first.
                    startPlayback();
                }
            });
            videoView.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(R.drawable.ic_play_white);
                showControls();
                cancelAutoHide();
                // Auto-advance to the next photo/video. The next page only
                // actually starts playing once it truly becomes the active
                // page (see becomeActive()), so its audio never overlaps
                // with this video and it can't have silently finished
                // off-screen already. If this was the last item, the
                // activity simply won't advance and this page is left
                // paused on its last frame with controls visible.
                if (completionListener != null) {
                    completionListener.onVideoCompleted(this.position);
                }
            });

            tapCatcher.setOnClickListener(v -> toggleControls());

            btnPlayPause.setOnClickListener(v -> {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play_white);
                    cancelAutoHide();
                } else {
                    videoView.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause_white);
                    scheduleAutoHide();
                }
                showControls();
            });

            btnRewind.setOnClickListener(v -> {
                int target = Math.max(0, videoView.getCurrentPosition() - 10000);
                videoView.seekTo(target);
                seekBar.setProgress(target);
                restartAutoHideIfPlaying();
            });

            btnForward.setOnClickListener(v -> {
                int duration = videoView.getDuration();
                int target = videoView.getCurrentPosition() + 10000;
                if (duration > 0) target = Math.min(target, duration);
                videoView.seekTo(target);
                seekBar.setProgress(target);
                restartAutoHideIfPlaying();
            });

            btnFullscreen.setOnClickListener(v -> {
                if (fullscreenListener != null) {
                    fullscreenListener.onFullscreenToggleRequested();
                }
                restartAutoHideIfPlaying();
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser) videoView.seekTo(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar sb) {
                    userSeeking = true;
                    cancelAutoHide();
                }

                @Override
                public void onStopTrackingTouch(SeekBar sb) {
                    userSeeking = false;
                    restartAutoHideIfPlaying();
                }
            });

            // Reset to a clean "just opened" state: controls visible, will
            // auto-hide once playback actually starts (see onPreparedListener).
            controlsVisible = true;
            controlsBar.setAlpha(1f);
            controlsBar.setVisibility(View.VISIBLE);
            notifyVisibility(true);
            handler.removeCallbacks(progressRunnable);
            handler.post(progressRunnable);
        }

        /** Called by the adapter when ViewPager2 makes this holder's position the
         *  one actually on screen. If the video was already quietly pre-loaded in
         *  the background (prepared but never started), start it now - this is
         *  the only place a pre-loaded neighbor page is allowed to begin playing
         *  and making sound. If it's not prepared yet, mark isActive so the
         *  onPreparedListener above starts it as soon as it becomes ready. */
        void becomeActive() {
            isActive = true;
            if (prepared && !videoView.isPlaying()) {
                startPlayback();
            }
        }

        void startPlayback() {
            videoView.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause_white);
            showControls();
            scheduleAutoHide();
        }

        void toggleControls() {
            if (controlsVisible) {
                hideControls();
            } else {
                showControls();
                restartAutoHideIfPlaying();
            }
        }

        void showControls() {
            if (!controlsVisible) {
                controlsVisible = true;
                controlsBar.setVisibility(View.VISIBLE);
                controlsBar.animate().alpha(1f).setDuration(150).start();
                notifyVisibility(true);
            }
        }

        void hideControls() {
            if (controlsVisible) {
                controlsVisible = false;
                controlsBar.animate().alpha(0f).setDuration(150)
                        .withEndAction(() -> controlsBar.setVisibility(View.GONE)).start();
                notifyVisibility(false);
            }
        }

        void scheduleAutoHide() {
            handler.removeCallbacks(hideRunnable);
            handler.postDelayed(hideRunnable, AUTO_HIDE_DELAY_MS);
        }

        void cancelAutoHide() {
            handler.removeCallbacks(hideRunnable);
        }

        void restartAutoHideIfPlaying() {
            if (videoView.isPlaying()) {
                scheduleAutoHide();
            } else {
                cancelAutoHide();
            }
        }

        /** Swaps the button's icon (and content description) to reflect whether
         *  the activity currently has this video forced into landscape/fullscreen. */
        void updateFullscreenIcon(boolean fullscreenActive) {
            if (fullscreenActive) {
                btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit_white);
                btnFullscreen.setContentDescription(
                        itemView.getResources().getString(R.string.video_fullscreen_exit));
            } else {
                btnFullscreen.setImageResource(R.drawable.ic_fullscreen_white);
                btnFullscreen.setContentDescription(
                        itemView.getResources().getString(R.string.video_fullscreen_enter));
            }
        }

        void notifyVisibility(boolean visible) {
            if (listener != null) {
                listener.onControlsVisibilityChanged(position, visible);
            }
        }

        void release() {
            handler.removeCallbacksAndMessages(null);
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            closeAnnouncedAudioSession();
        }

        /**
         * Tells the ASsound audio engine that this session's audio effects are
         * live and ready to be shaped. Mirrors what any equalizer-aware player
         * (e.g. the stock Music FX integration) is expected to broadcast.
         */
        private void openAudioSession(int sessionId) {
            if (sessionId == 0) return;
            if (announcedAudioSessionId == sessionId) return; // already announced
            closeAnnouncedAudioSession();

            Context context = itemView.getContext();
            Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
            context.sendBroadcast(intent);
            announcedAudioSessionId = sessionId;
        }

        /** Tells the ASsound audio engine to detach and release effects for this session. */
        private void closeAnnouncedAudioSession() {
            if (announcedAudioSessionId == 0) return;
            Context context = itemView.getContext();
            Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, announcedAudioSessionId);
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            context.sendBroadcast(intent);
            announcedAudioSessionId = 0;
        }
    }
}
