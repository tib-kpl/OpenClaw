package com.openclaw.game;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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

    // Import of CLAW.REZ through the system file picker, see importGameFile()
    private static final int REQUEST_IMPORT_FILE = 4242;
    private static final Object sImportLock = new Object();
    private static Boolean sImportResult;
    private static String sImportDestination;

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

    // Called from native code when CLAW.REZ is missing. Lets the user pick the file
    // (Downloads, SD card, cloud...) and copies it into the app files directory.
    // Blocks the calling (native) thread until the file is copied or the user cancels.
    public static boolean importGameFile(String destinationPath) {
        final SDLActivity activity = mSingleton;
        if (activity == null) {
            return false;
        }

        synchronized (sImportLock) {
            sImportResult = null;
            sImportDestination = destinationPath;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    activity.startActivityForResult(intent, REQUEST_IMPORT_FILE);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open file picker", e);
                    finishImport(false);
                }
            }
        });

        synchronized (sImportLock) {
            while (sImportResult == null) {
                try {
                    sImportLock.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            return sImportResult;
        }
    }

    private static void finishImport(boolean success) {
        synchronized (sImportLock) {
            sImportResult = success;
            sImportLock.notifyAll();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_IMPORT_FILE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            finishImport(false);
            return;
        }

        final Uri uri = data.getData();
        final String destinationPath;
        synchronized (sImportLock) {
            destinationPath = sImportDestination;
        }

        // The file is big (~120 MB), do not copy it on the UI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                File destination = new File(destinationPath);
                File tempFile = new File(destinationPath + ".tmp");
                boolean success = false;
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024 * 1024];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    success = true;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to import " + uri, e);
                }
                if (success) {
                    success = tempFile.renameTo(destination);
                }
                if (!success) {
                    tempFile.delete();
                }
                finishImport(success);
            }
        }).start();
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
