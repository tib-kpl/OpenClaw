#include "BaseGameApp.h"
#include "MainLoop.h"
#include <fstream>

#if !(defined(__ANDROID__) || defined(__WINDOWS__))
#include <pwd.h>
#include <unistd.h>
#endif

#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#endif

#ifdef __ANDROID__
#include <unistd.h>

// Copies a file packaged in the APK assets into the current directory
static bool ExtractAndroidAsset(const char* fileName, bool overwrite)
{
    if (!overwrite && TryToFindFile(fileName))
    {
        return true;
    }

    SDL_RWops* src = SDL_RWFromFile(fileName, "rb");
    if (src == NULL)
    {
        LOG_ERROR("Missing APK asset: " + std::string(fileName));
        return false;
    }

    std::ofstream dst(fileName, std::ios::binary | std::ios::trunc);
    char buffer[64 * 1024];
    size_t read;
    while ((read = SDL_RWread(src, buffer, 1, sizeof(buffer))) > 0)
    {
        dst.write(buffer, read);
    }
    SDL_RWclose(src);

    return dst.good();
}
#endif

int RunGameEngine(int argc, char** argv)
{
    if (SDL_SetThreadPriority(SDL_THREAD_PRIORITY_HIGH) != 0)
    {
        LOG_WARNING("Failed to set high priority class to this process");
    }

    std::string userDirectory = "";

#if defined(__ANDROID__)
    // All game files live in the app's external files directory
    // (Android/data/<package>/files) where the user has to copy CLAW.REZ.
    // It does not require any storage permission.
    const char* externalPath = SDL_AndroidGetExternalStoragePath();
    if (externalPath == NULL)
    {
        LOG_ERROR("Failed to get external storage path: " + std::string(SDL_GetError()));
        return -1;
    }
    userDirectory = std::string(externalPath) + "/";
    if (chdir(userDirectory.c_str()) != 0)
    {
        LOG_ERROR("Failed to change directory to: " + userDirectory);
        return -1;
    }

    // Files shipped with the APK. User editable ones are not overwritten.
    ExtractAndroidAsset("ASSETS.ZIP", true);
    ExtractAndroidAsset("clacon.ttf", true);
    ExtractAndroidAsset("console02.tga", true);
    ExtractAndroidAsset("config.xml", false);
    ExtractAndroidAsset("SAVES.XML", false);

    if (!TryToFindFile("CLAW.REZ"))
    {
        std::string message = "CLAW.REZ from the original Captain Claw game was not found.\n\n"
            "Copy it to:\n" + userDirectory;
        SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, "OpenClaw", message.c_str(), NULL);
        return -1;
    }
#elif defined(__WINDOWS__)
    userDirectory = "";
#else
    const char* homedir;

    if ((homedir = getenv("HOME")) == NULL) 
    {
        homedir = getpwuid(getuid())->pw_dir;
    }
    assert(homedir != NULL);

    userDirectory = std::string(homedir) + "/.config/openclaw/";

#endif

    LOG("Looking for: " + userDirectory + "config.xml");

    // Temporary hack - always prefer config in the same folder as binary to default config
    if (TryToFindFile("config.xml"))
    {
        // File was found in a working directory
        userDirectory = "";
    }

    LOG("Expecting config.xml in path: " + userDirectory + "config.xml");

    // Load options
    if (!g_pApp->LoadGameOptions(std::string(userDirectory + "config.xml").c_str()))
    {
        LOG_ERROR("Could not load game options. Exiting.");
        return -1;
    }

    g_pApp->GetGameConfig()->userDirectory = userDirectory;

    std::string savesFilePath = g_pApp->GetGameConfig()->userDirectory + g_pApp->GetGameConfig()->savesFile;
    LOG("Loaded with:\n\tConfig File: " + userDirectory + "config.xml" + "\n\tSaves File: " + savesFilePath);

    // Initialize game instance
    if (!g_pApp->Initialize(argc, argv))
    {
        LOG_ERROR("Failed to initialize. Exiting.");
        return -1;
    }

    // Run the game
    return g_pApp->Run();
}

bool TryToFindFile(const char *path) {
    {
        std::ifstream f(path);
        if (f.good()) {
            return true;
        }
    }
#ifdef __EMSCRIPTEN__
    // File does not exist. Try to download
    emscripten_wget(path, path);
    {
        // Check is file downloaded
        std::ifstream f(path);
        if (f.good()) {
            return true;
        }
    }
#endif
    return false;
}
