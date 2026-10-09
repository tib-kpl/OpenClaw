cmake_minimum_required(VERSION 3.2)

# Build options
option(Android "Android" OFF)

option(Emscripten "Build as WASM" OFF)
option(Extern_Config "Do not embed config file" ON)
set(EMSCRIPTEN_PATH "CHANGE ME PLEASE!!!/emsdk")

project(OpenClaw)

#if(NOT CMAKE_BUILD_TYPE)
#  set(CMAKE_BUILD_TYPE Release)
#endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -g")

# CMake compiled directories with dependencies
add_subdirectory(Box2D)
add_subdirectory(libwap)
add_subdirectory(./ThirdParty/Tinyxml)
if (WIN32)
    add_subdirectory(MidiProc)
endif (WIN32)

# Android: SDL2 libraries are built from sources fetched by android/fetch-deps.sh
if(Android)
    set(ANDROID_DEPS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android/deps")
    if(NOT EXISTS "${ANDROID_DEPS_DIR}/SDL2/CMakeLists.txt")
        message(FATAL_ERROR "SDL2 sources not found. Run android/fetch-deps.sh first.")
    endif()

    # SDL2 itself must be a shared library (loaded by SDLActivity),
    # satellite libraries are linked statically into libopenclaw.so
    set(SDL_SHARED ON CACHE BOOL "" FORCE)
    set(SDL_STATIC OFF CACHE BOOL "" FORCE)
    set(SDL_TEST OFF CACHE BOOL "" FORCE)
    add_subdirectory(${ANDROID_DEPS_DIR}/SDL2 ${CMAKE_BINARY_DIR}/deps/SDL2)
    # Static satellite libraries look for this target
    add_library(SDL2::SDL2-static ALIAS SDL2)

    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

    set(SDL2IMAGE_VENDORED OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_SAMPLES OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_BACKEND_STB ON CACHE BOOL "" FORCE)
    set(SDL2IMAGE_AVIF OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_JXL OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_TIF OFF CACHE BOOL "" FORCE)
    set(SDL2IMAGE_WEBP OFF CACHE BOOL "" FORCE)
    add_subdirectory(${ANDROID_DEPS_DIR}/SDL2_image ${CMAKE_BINARY_DIR}/deps/SDL2_image)

    set(SDL2MIXER_VENDORED OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_SAMPLES OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_FLAC OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_GME OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_MOD OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_MP3 OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_OPUS OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_WAVPACK OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_MIDI_FLUIDSYNTH OFF CACHE BOOL "" FORCE)
    set(SDL2MIXER_MIDI_TIMIDITY ON CACHE BOOL "" FORCE)
    add_subdirectory(${ANDROID_DEPS_DIR}/SDL2_mixer ${CMAKE_BINARY_DIR}/deps/SDL2_mixer)

    set(SDL2TTF_VENDORED ON CACHE BOOL "" FORCE)
    set(SDL2TTF_SAMPLES OFF CACHE BOOL "" FORCE)
    set(SDL2TTF_HARFBUZZ OFF CACHE BOOL "" FORCE)
    add_subdirectory(${ANDROID_DEPS_DIR}/SDL2_ttf ${CMAKE_BINARY_DIR}/deps/SDL2_ttf)

    set(SDL2_GFX_DIR ${ANDROID_DEPS_DIR}/SDL2_gfx)
    add_library(SDL2_gfx STATIC
        ${SDL2_GFX_DIR}/SDL2_framerate.c
        ${SDL2_GFX_DIR}/SDL2_gfxPrimitives.c
        ${SDL2_GFX_DIR}/SDL2_imageFilter.c
        ${SDL2_GFX_DIR}/SDL2_rotozoom.c
    )
    target_link_libraries(SDL2_gfx PRIVATE SDL2::SDL2)

    # The code includes <SDL2/xxx.h>, so gather all public headers into an SDL2/ folder.
    # It must take precedence over the Windows headers in ThirdParty/SDL2.
    set(SDL2_HEADERS_DIR ${CMAKE_BINARY_DIR}/sdl2_headers)
    file(GLOB SDL2_PUBLIC_HEADERS
        ${ANDROID_DEPS_DIR}/SDL2/include/*.h
        ${ANDROID_DEPS_DIR}/SDL2_image/include/SDL_image.h
        ${ANDROID_DEPS_DIR}/SDL2_mixer/include/SDL_mixer.h
        ${ANDROID_DEPS_DIR}/SDL2_ttf/SDL_ttf.h
        ${SDL2_GFX_DIR}/SDL2_*.h
    )
    file(COPY ${SDL2_PUBLIC_HEADERS} DESTINATION ${SDL2_HEADERS_DIR}/SDL2)
    include_directories(BEFORE ${SDL2_HEADERS_DIR})
endif(Android)

# Directories with headers
include_directories(./Box2D/)
include_directories(./libwap/)
include_directories(./ThirdParty/)
include_directories(./ThirdParty/Tinyxml/)
if (Emscripten)
    include_directories("${EMSCRIPTEN_PATH}/upstream/emscripten/system/include/emscripten/")
endif (Emscripten)

# Directories with libs
link_directories(./libwap)
link_directories(./Box2D)
if (WIN32)
    link_directories(./ThirdParty/SDL2/lib/x86)
    link_directories(./Build_Release)
endif (WIN32)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ../Build_Release)

# Cmake target definitions
if(Android)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DANDROID")
    add_library(openclaw SHARED "")
else (Android)
    add_executable(openclaw "")
endif(Android)

add_subdirectory(OpenClaw)

# Linker settings
list(APPEND TARGET_LIBS
    libwap
    tinyxml
    Box2D
)
if (Emscripten)
    if (Extern_Config)
        set(CONFIG_PRELOAD_FILE "")
    else (Extern_Config)
        set(CONFIG_PRELOAD_FILE "--preload-file ../Build_Release/config.xml@config.xml")
    endif (Extern_Config)
    set_target_properties(openclaw PROPERTIES LINK_FLAGS "-s FETCH -s WASM=1 -s BINARYEN_METHOD='native-wasm' -s EXPORTED_FUNCTIONS='[_main]' -s USE_SDL=2 -s USE_SDL_IMAGE=2 -s SDL2_IMAGE_FORMATS='[\"pcx\",\"png\",\"tga\"]' -s USE_SDL_TTF=2 -s USE_SDL_GFX=2 -s ASYNCIFY=1 -s TOTAL_MEMORY=268435456 ${CONFIG_PRELOAD_FILE} --preload-file ../Build_Release/CLAW.REZ@CLAW.REZ --preload-file ../Build_Release/ASSETS.ZIP@ASSETS.ZIP --preload-file ../Build_Release/console02.tga@console02.tga --preload-file ../Build_Release/clacon.ttf@clacon.ttf")
else (Emscripten)
    list(APPEND TARGET_LIBS
            SDL2
            SDL2_mixer
            SDL2_ttf
            SDL2_image
            SDL2_gfx
            )
    if (WIN32)
        list(APPEND TARGET_LIBS
                Rpcrt4
                )
    else (WIN32)
        # TODO: Is it really important???
        list(APPEND TARGET_LIBS
                stdc++
                m
                )
    endif (WIN32)
    if (Android)
        list(APPEND TARGET_LIBS
                GLESv2
                GLESv1_CM
                dl
                log
                android
                )
    endif (Android)
endif (Emscripten)

target_link_libraries(openclaw ${TARGET_LIBS})
