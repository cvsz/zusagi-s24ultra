#include "HumandroidGameMode.h"
#include "HumandroidAvatarPawn.h"
#include "HumandroidHud.h"

AHumandroidGameMode::AHumandroidGameMode()
{
    DefaultPawnClass = AHumandroidAvatarPawn::StaticClass();
    HUDClass = AHumandroidHud::StaticClass();
}
