#include "HumandroidHud.h"
#include "HumandroidAvatarPawn.h"
#include "Engine/Canvas.h"
#include "Engine/Font.h"
#include "Engine/Engine.h"

void AHumandroidHud::DrawHUD()
{
    Super::DrawHUD();
    if (!Canvas) return;
    const AHumandroidAvatarPawn* Avatar = Cast<AHumandroidAvatarPawn>(GetOwningPawn());
    if (!Avatar) return;

    const EHumandroidVisualState State = Avatar->GetVisualState();
    FLinearColor Color(0.1f, 0.78f, 1.0f, 0.68f);
    switch (State)
    {
    case EHumandroidVisualState::Listening: Color = FLinearColor(0.12f, 1.0f, 0.62f, 0.82f); break;
    case EHumandroidVisualState::Speaking: Color = FLinearColor(1.0f, 0.62f, 0.12f, 0.82f); break;
    case EHumandroidVisualState::Error: Color = FLinearColor(1.0f, 0.12f, 0.2f, 0.82f); break;
    case EHumandroidVisualState::Disconnected: Color = FLinearColor(0.42f, 0.48f, 0.58f, 0.72f); break;
    default: break;
    }

    Elapsed += GetWorld() ? GetWorld()->GetDeltaSeconds() : 0.0f;
    const float Scale = FMath::Min(Canvas->SizeX / 420.0f, Canvas->SizeY / 760.0f);
    const FVector2D Center(Canvas->SizeX * 0.5f, Canvas->SizeY * 0.5f);
    const bool bReducedMotion = Avatar->IsReducedMotion();
    const float Phase = bReducedMotion ? 0.0f : Elapsed * 0.24f;
    constexpr int32 Segments = 72;
    for (int32 Ring = 0; Ring < 3; ++Ring)
    {
        const float RadiusX = (115.0f + Ring * 17.0f) * Scale;
        const float RadiusY = (182.0f + Ring * 13.0f) * Scale;
        const float Direction = (Ring % 2 == 0) ? 1.0f : -1.0f;
        for (int32 Segment = 0; Segment < Segments; ++Segment)
        {
            const float A0 = (static_cast<float>(Segment) / Segments) * (2.0f * UE_PI) + Phase * Direction;
            const float A1 = (static_cast<float>(Segment + 1) / Segments) * (2.0f * UE_PI) + Phase * Direction;
            const FVector2D P0(Center.X + FMath::Cos(A0) * RadiusX, Center.Y + FMath::Sin(A0) * RadiusY);
            const FVector2D P1(Center.X + FMath::Cos(A1) * RadiusX, Center.Y + FMath::Sin(A1) * RadiusY);
            DrawLine(P0.X, P0.Y, P1.X, P1.Y, Color, Ring == 1 ? 1.2f : 0.7f);
        }
    }

    const UEnum* StateNames = StaticEnum<EHumandroidVisualState>();
    const FString Label = StateNames ? StateNames->GetNameStringByValue(static_cast<int64>(State)) : TEXT("IDLE");
    DrawText(Label, Color, 20.0f, 28.0f, GEngine ? GEngine->GetSmallFont() : nullptr, 1.2f);
}
