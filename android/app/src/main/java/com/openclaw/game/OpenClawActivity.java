package com.openclaw.game;

import android.media.MediaPlayer;
import android.util.Log;

import org.libsdl.app.SDLActivity;

public class OpenClawActivity extends SDLActivity {
    private static final String TAG = "OpenClaw";

    // SDL_mixer cannot play MIDI on Android without sound patches, so the music
    // is played by the MIDI synthesizer built into Android through MediaPlayer.
    // These static methods are called from native code (see Audio.cpp).
    private static final Object sMusicLock = new Object();
    private static MediaPlayer sMusicPlayer;
    private static boolean sMusicPaused;
    private static boolean sActivityPaused;
    private static float sMusicVolume = 1.0f;

    @Override
    protected String[] getLibraries() {
        // SDL2_image, SDL2_mixer, SDL2_ttf and SDL2_gfx are linked statically into libopenclaw.so
        return new String[] {
            "SDL2",
            "openclaw"
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        synchronized (sMusicLock) {
            sActivityPaused = true;
            updateMusicState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        synchronized (sMusicLock) {
            sActivityPaused = false;
            updateMusicState();
        }
    }

    public static void playMusic(String path, boolean looping, boolean paused) {
        synchronized (sMusicLock) {
            stopMusic();
            try {
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(path);
                player.setLooping(looping);
                player.setVolume(sMusicVolume, sMusicVolume);
                player.prepare();
                sMusicPlayer = player;
                sMusicPaused = paused;
                updateMusicState();
            } catch (Exception e) {
                Log.e(TAG, "Failed to play music " + path, e);
            }
        }
    }

    public static void pauseMusic() {
        synchronized (sMusicLock) {
            sMusicPaused = true;
            updateMusicState();
        }
    }

    public static void resumeMusic() {
        synchronized (sMusicLock) {
            sMusicPaused = false;
            updateMusicState();
        }
    }

    public static void stopMusic() {
        synchronized (sMusicLock) {
            if (sMusicPlayer != null) {
                sMusicPlayer.release();
                sMusicPlayer = null;
            }
        }
    }

    public static void setMusicVolume(float volume) {
        synchronized (sMusicLock) {
            sMusicVolume = volume;
            if (sMusicPlayer != null) {
                sMusicPlayer.setVolume(volume, volume);
            }
        }
    }

    // Must be called with sMusicLock held
    private static void updateMusicState() {
        if (sMusicPlayer == null) {
            return;
        }
        try {
            boolean shouldPlay = !sMusicPaused && !sActivityPaused;
            if (shouldPlay && !sMusicPlayer.isPlaying()) {
                sMusicPlayer.start();
            } else if (!shouldPlay && sMusicPlayer.isPlaying()) {
                sMusicPlayer.pause();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Invalid music player state", e);
        }
    }
}
