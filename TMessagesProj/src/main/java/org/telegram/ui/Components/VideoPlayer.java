/*
 * This is the source code of Supergram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.Supergram.ui.Components;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.view.TextureView;

import com.baranak.tsupergran.ApplicationLoader;
import com.baranak.tsupergran.exoplayer2.DefaultLoadControl;
import com.baranak.tsupergran.exoplayer2.ExoPlaybackException;
import com.baranak.tsupergran.exoplayer2.ExoPlayer;
import com.baranak.tsupergran.exoplayer2.ExoPlayerFactory;
import com.baranak.tsupergran.exoplayer2.SimpleExoPlayer;
import com.baranak.tsupergran.exoplayer2.Timeline;
import com.baranak.tsupergran.exoplayer2.extractor.DefaultExtractorsFactory;
import com.baranak.tsupergran.exoplayer2.source.ExtractorMediaSource;
import com.baranak.tsupergran.exoplayer2.source.LoopingMediaSource;
import com.baranak.tsupergran.exoplayer2.source.MediaSource;
import com.baranak.tsupergran.exoplayer2.source.MergingMediaSource;
import com.baranak.tsupergran.exoplayer2.source.TrackGroupArray;
import com.baranak.tsupergran.exoplayer2.source.dash.DashMediaSource;
import com.baranak.tsupergran.exoplayer2.source.dash.DefaultDashChunkSource;
import com.baranak.tsupergran.exoplayer2.source.hls.HlsMediaSource;
import com.baranak.tsupergran.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.baranak.tsupergran.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.baranak.tsupergran.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.baranak.tsupergran.exoplayer2.trackselection.DefaultTrackSelector;
import com.baranak.tsupergran.exoplayer2.trackselection.MappingTrackSelector;
import com.baranak.tsupergran.exoplayer2.trackselection.TrackSelection;
import com.baranak.tsupergran.exoplayer2.trackselection.TrackSelectionArray;
import com.baranak.tsupergran.exoplayer2.upstream.DataSource;
import com.baranak.tsupergran.exoplayer2.upstream.DefaultBandwidthMeter;
import com.baranak.tsupergran.exoplayer2.upstream.DefaultDataSourceFactory;
import com.baranak.tsupergran.exoplayer2.upstream.DefaultHttpDataSourceFactory;

@SuppressLint("NewApi")
public class VideoPlayer implements ExoPlayer.EventListener, SimpleExoPlayer.VideoListener {

    public interface RendererBuilder {
        void buildRenderers(VideoPlayer player);
        void cancel();
    }

    public interface VideoPlayerDelegate {
        void onStateChanged(boolean playWhenReady, int playbackState);
        void onError(Exception e);
        void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio);
        void onRenderedFirstFrame();
        void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture);
        boolean onSurfaceDestroyed(SurfaceTexture surfaceTexture);
    }

    private SimpleExoPlayer player;
    private MappingTrackSelector trackSelector;
    private Handler mainHandler;
    private DataSource.Factory mediaDataSourceFactory;
    private TextureView textureView;
    private boolean autoplay;

    private VideoPlayerDelegate delegate;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    public VideoPlayer() {
        mediaDataSourceFactory = new DefaultDataSourceFactory(ApplicationLoader.applicationContext, BANDWIDTH_METER, new DefaultHttpDataSourceFactory("Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20150101 Firefox/47.0 (Chrome)", BANDWIDTH_METER));

        mainHandler = new Handler();

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        lastReportedPlaybackState = ExoPlayer.STATE_IDLE;
    }

    private void ensurePleyaerCreated() {
        if (player == null) {
            player = ExoPlayerFactory.newSimpleInstance(ApplicationLoader.applicationContext, trackSelector, new DefaultLoadControl(), null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF);
            player.addListener(this);
            player.setVideoListener(this);
            player.setVideoTextureView(textureView);
            player.setPlayWhenReady(autoplay);
        }
    }

    public void preparePlayerLoop(Uri videoUri, String videoType, Uri audioUri, String audioType) {
        ensurePleyaerCreated();
        MediaSource mediaSource1 = null, mediaSource2 = null;
        for (int a = 0; a < 2; a++) {
            MediaSource mediaSource;
            String type;
            Uri uri;
            if (a == 0) {
                type = videoType;
                uri = videoUri;
            } else {
                type = audioType;
                uri = audioUri;
            }
            switch (type) {
                case "dash":
                    mediaSource = new DashMediaSource(uri, mediaDataSourceFactory, new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
                    break;
                case "hls":
                    mediaSource = new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null);
                    break;
                case "ss":
                    mediaSource = new SsMediaSource(uri, mediaDataSourceFactory, new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
                    break;
                default:
                    mediaSource = new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(), mainHandler, null);
                    break;
            }
            mediaSource = new LoopingMediaSource(mediaSource);
            if (a == 0) {
                mediaSource1 = mediaSource;
            } else {
                mediaSource2 = mediaSource;
            }
        }
        MediaSource mediaSource = new MergingMediaSource(mediaSource1, mediaSource2);
        player.prepare(mediaSource1, true, true);
    }

    public void preparePlayer(Uri uri, String type) {
        ensurePleyaerCreated();
        MediaSource mediaSource;
        switch (type) {
            case "dash":
                mediaSource = new DashMediaSource(uri, mediaDataSourceFactory, new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
                break;
            case "hls":
                mediaSource = new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null);
                break;
            case "ss":
                mediaSource = new SsMediaSource(uri, mediaDataSourceFactory, new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
                break;
            default:
                mediaSource = new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(), mainHandler, null);
                break;
        }
        player.prepare(mediaSource, true, true);
    }

    public boolean isPlayerPrepared() {
        return player != null;
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void setTextureView(TextureView texture) {
        textureView = texture;
        if (player == null) {
            return;
        }
        player.setVideoTextureView(textureView);
    }

    public void play() {
        if (player == null) {
            return;
        }
        player.setPlayWhenReady(true);
    }

    public void pause() {
        if (player == null) {
            return;
        }
        player.setPlayWhenReady(false);
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        autoplay = playWhenReady;
        if (player == null) {
            return;
        }
        player.setPlayWhenReady(playWhenReady);
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public void setMute(boolean value) {
        if (player == null) {
            return;
        }
        if (value) {
            player.setVolume(0.0f);
        } else {
            player.setVolume(1.0f);
        }
    }

    public void seekTo(long positionMs) {
        if (player == null) {
            return;
        }
        player.seekTo(positionMs);
    }

    public void setDelegate(VideoPlayerDelegate videoPlayerDelegate) {
        delegate = videoPlayerDelegate;
    }

    public int getBufferedPercentage() {
        return player != null ? player.getBufferedPercentage() : 0;
    }

    public long getBufferedPosition() {
        return player != null ? player.getBufferedPosition() : 0;
    }

    public boolean isPlaying() {
        return player != null && player.getPlayWhenReady();
    }

    public boolean isBuffering() {
        return player != null && lastReportedPlaybackState == ExoPlayer.STATE_BUFFERING;
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        maybeReportPlayerState();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        delegate.onError(error);
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        delegate.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
    }

    @Override
    public void onRenderedFirstFrame() {
        delegate.onRenderedFirstFrame();
    }

    @Override
    public boolean onSurfaceDestroyed(SurfaceTexture surfaceTexture) {
        return delegate.onSurfaceDestroyed(surfaceTexture);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        delegate.onSurfaceTextureUpdated(surfaceTexture);
    }

    private void maybeReportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = player.getPlaybackState();
        if (lastReportedPlayWhenReady != playWhenReady || lastReportedPlaybackState != playbackState) {
            delegate.onStateChanged(playWhenReady, playbackState);
            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
        }
    }
}
