//#define SDL_MAIN_HANDLED
//#pragma comment(linker, "/SUBSYSTEM:windows /ENTRY:mainCRTStartup")

#include "Engine/GameApp/MainLoop.h"
#include "ClawGameApp.h"

ClawGameApp testApp;

int main(int argc, char* argv[])
{
#ifdef _CRTDBG_MAP_ALLOC
    _CrtSetReportMode( _CRT_WARN, _CRTDBG_MODE_FILE );
    _CrtSetReportFile( _CRT_WARN, _CRTDBG_FILE_STDOUT );
    _CrtSetReportMode( _CRT_ERROR, _CRTDBG_MODE_FILE );
    _CrtSetReportFile( _CRT_ERROR, _CRTDBG_FILE_STDOUT );
//    _CrtSetReportMode( _CRT_ASSERT, _CRTDBG_MODE_FILE );
//    _CrtSetReportFile( _CRT_ASSERT, _CRTDBG_FILE_STDOUT );
    _CrtSetDbgFlag ( _CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF );
#endif
    return RunGameEngine(argc, argv);
}
