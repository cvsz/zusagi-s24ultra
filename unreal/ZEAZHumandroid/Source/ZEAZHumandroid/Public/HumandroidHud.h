#pragma once

#include "CoreMinimal.h"
#include "GameFramework/HUD.h"
#include "HumandroidHud.generated.h"

UCLASS()
class ZEAZHUMANDROID_API AHumandroidHud : public AHUD
{
    GENERATED_BODY()

public:
    virtual void DrawHUD() override;

private:
    float Elapsed = 0.0f;
};
