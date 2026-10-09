package com.openclaw.game;

import org.libsdl.app.SDLActivity;

public class OpenClawActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        // SDL2_image, SDL2_mixer, SDL2_ttf and SDL2_gfx are linked statically into libopenclaw.so
        return new String[] {
            "SDL2",
            "openclaw"
        };
    }
}
