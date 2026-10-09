#!/bin/sh
# Downloads SDL2 and its satellite libraries sources needed by the Android build
# into android/deps. Run once before building the APK.
set -e

SDL2_VERSION=2.30.9
SDL2_IMAGE_VERSION=2.8.2
SDL2_MIXER_VERSION=2.8.0
SDL2_TTF_VERSION=2.22.0
SDL2_GFX_VERSION=1.0.4

DEPS_DIR="$(cd "$(dirname "$0")" && pwd)/deps"
mkdir -p "$DEPS_DIR"
cd "$DEPS_DIR"

clone() {
    # $1 = repo name, $2 = tag, $3 = target directory
    if [ ! -d "$3" ]; then
        git clone --depth 1 --branch "$2" "https://github.com/libsdl-org/$1.git" "$3"
    fi
}

clone SDL release-$SDL2_VERSION SDL2
clone SDL_image release-$SDL2_IMAGE_VERSION SDL2_image
clone SDL_mixer release-$SDL2_MIXER_VERSION SDL2_mixer
clone SDL_ttf release-$SDL2_TTF_VERSION SDL2_ttf
(cd SDL2_ttf && git submodule update --init --depth 1 external/freetype)

if [ ! -d SDL2_gfx ]; then
    curl -fL --retry 3 -o SDL2_gfx.tar.gz \
        "https://downloads.sourceforge.net/project/sdl2gfx/SDL2_gfx-$SDL2_GFX_VERSION.tar.gz"
    tar xzf SDL2_gfx.tar.gz
    mv "SDL2_gfx-$SDL2_GFX_VERSION" SDL2_gfx
    rm SDL2_gfx.tar.gz
fi

echo "Android dependencies ready in $DEPS_DIR"
