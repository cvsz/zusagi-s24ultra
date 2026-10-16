#include "HumandroidAvatarPawn.h"

#include "Camera/CameraComponent.h"
#include "Components/SceneComponent.h"
#include "Materials/MaterialInstanceDynamic.h"
#include "Materials/Material.h"
#include "ProceduralMeshComponent.h"

namespace
{
constexpr float Pi = UE_PI;
constexpr float Tau = 2.0f * UE_PI;
}

AHumandroidAvatarPawn::AHumandroidAvatarPawn()
{
    PrimaryActorTick.bCanEverTick = true;
    SceneRoot = CreateDefaultSubobject<USceneComponent>(TEXT("SceneRoot"));
    SetRootComponent(SceneRoot);

    AvatarMesh = CreateDefaultSubobject<UProceduralMeshComponent>(TEXT("HumandroidWireframe"));
    AvatarMesh->SetupAttachment(SceneRoot);
    AvatarMesh->SetCollisionEnabled(ECollisionEnabled::NoCollision);
    AvatarMesh->SetCastShadow(false);

    Camera = CreateDefaultSubobject<UCameraComponent>(TEXT("Camera"));
    Camera->SetupAttachment(SceneRoot);
    Camera->SetRelativeLocation(FVector(0.0f, -510.0f, 120.0f));
    Camera->SetRelativeRotation(FRotator(-4.0f, 90.0f, 0.0f));
    AutoPossessPlayer = EAutoReceiveInput::Player0;
}

void AHumandroidAvatarPawn::BeginPlay()
{
    Super::BeginPlay();
    BuildOriginalAvatar();
    SetVisualState(EHumandroidVisualState::Idle);
}

void AHumandroidAvatarPawn::Tick(float DeltaSeconds)
{
    Super::Tick(DeltaSeconds);
    Elapsed += DeltaSeconds;
    StateTime += DeltaSeconds;

    const float MotionScale = bReducedMotion ? 0.0f : 1.0f;
    const float Pulse = bReducedMotion ? 0.5f : 0.5f + 0.5f * FMath::Sin(Elapsed * 2.1f);
    const float Breath = MotionScale * FMath::Sin(Elapsed * 1.2f) * 1.3f;
    const float Speak = VisualState == EHumandroidVisualState::Speaking
        ? SpeechAmplitude * 4.0f : 0.0f;
    CurrentColor = FMath::Lerp(CurrentColor, TargetColor, 1.0f - FMath::Exp(-DeltaSeconds * 6.0f));
    SetActorLocation(FVector(0.0f, 0.0f, Breath + Speak));
    SetActorRotation(FRotator(0.0f, MotionScale * FMath::Sin(Elapsed * 0.42f) * 3.0f, 0.0f));

    if (HoloMaterial)
    {
        const float StatePulse = (VisualState == EHumandroidVisualState::Listening
            || VisualState == EHumandroidVisualState::Speaking) ? Pulse : 0.72f;
        HoloMaterial->SetVectorParameterValue(TEXT("HoloColor"), CurrentColor);
        HoloMaterial->SetScalarParameterValue(TEXT("Pulse"), StatePulse);
        HoloMaterial->SetScalarParameterValue(TEXT("MotionScale"), MotionScale);
        HoloMaterial->SetScalarParameterValue(TEXT("AudioAmplitude"), SpeechAmplitude);
        HoloMaterial->SetScalarParameterValue(TEXT("Elapsed"), bReducedMotion ? 0.0f : Elapsed);
    }
}

void AHumandroidAvatarPawn::SetVisualState(EHumandroidVisualState NewState)
{
    if (VisualState == NewState) return;
    VisualState = NewState;
    StateTime = 0.0f;
    switch (VisualState)
    {
    case EHumandroidVisualState::Listening: TargetColor = FLinearColor(0.12f, 1.0f, 0.62f); break;
    case EHumandroidVisualState::Thinking: TargetColor = FLinearColor(0.08f, 0.53f, 1.0f); break;
    case EHumandroidVisualState::Streaming: TargetColor = FLinearColor(0.0f, 0.82f, 1.0f); break;
    case EHumandroidVisualState::Speaking: TargetColor = FLinearColor(1.0f, 0.62f, 0.12f); break;
    case EHumandroidVisualState::Error: TargetColor = FLinearColor(1.0f, 0.12f, 0.2f); break;
    case EHumandroidVisualState::Disconnected: TargetColor = FLinearColor(0.32f, 0.38f, 0.46f); break;
    default: TargetColor = FLinearColor(0.02f, 0.75f, 1.0f); break;
    }
    ApplyStateMaterial();
}

void AHumandroidAvatarPawn::SetSpeechAmplitude(float NormalizedAmplitude)
{
    SpeechAmplitude = FMath::Clamp(NormalizedAmplitude, 0.0f, 1.0f);
}

bool AHumandroidAvatarPawn::ApplyAssistantEvent(FName EventName, float AudioAmplitude)
{
    static const TMap<FName, EHumandroidVisualState> AllowedStates = {
        {TEXT("IDLE"), EHumandroidVisualState::Idle},
        {TEXT("LISTENING"), EHumandroidVisualState::Listening},
        {TEXT("THINKING"), EHumandroidVisualState::Thinking},
        {TEXT("STREAMING"), EHumandroidVisualState::Streaming},
        {TEXT("SPEAKING"), EHumandroidVisualState::Speaking},
        {TEXT("ERROR"), EHumandroidVisualState::Error},
        {TEXT("DISCONNECTED"), EHumandroidVisualState::Disconnected}
    };
    const FName NormalizedEvent(*EventName.ToString().ToUpper());
    const EHumandroidVisualState* NextState = AllowedStates.Find(NormalizedEvent);
    if (!NextState) return false;
    SetSpeechAmplitude(AudioAmplitude);
    SetVisualState(*NextState);
    return true;
}

void AHumandroidAvatarPawn::SetReducedMotion(bool bEnabled)
{
    bReducedMotion = bEnabled;
}

void AHumandroidAvatarPawn::SetQualityProfile(int32 NewProfile)
{
    QualityProfile = FMath::Clamp(NewProfile, 0, 2);
    if (HasBegunPlay()) BuildOriginalAvatar();
}

void AHumandroidAvatarPawn::ApplyStateMaterial()
{
    if (HoloMaterial) HoloMaterial->SetVectorParameterValue(TEXT("HoloColor"), CurrentColor);
}

void AHumandroidAvatarPawn::BuildOriginalAvatar()
{
    if (!AvatarMesh) return;
    const int32 Segments = QualityProfile == 0 ? 12 : (QualityProfile == 2 ? 28 : CircumferenceSegments);
    TArray<FVector> Vertices;
    TArray<int32> Triangles;
    TArray<FVector> Normals;
    TArray<FVector2D> UVs;
    TArray<FColor> Colors;

    const FLinearColor Cyan(0.08f, 0.82f, 1.0f, 1.0f);
    const FLinearColor Gold(1.0f, 0.62f, 0.16f, 1.0f);
    const auto Ellipsoid = [&](FVector C, FVector R, FLinearColor Color, int32 Latitudes)
    {
        AddWireEllipsoid(C, R, Color, Latitudes, Segments, 1.5f,
            Vertices, Triangles, Normals, UVs, Colors);
    };

    Ellipsoid(FVector(0, 0, 150), FVector(47, 36, 57), Cyan, 10);
    Ellipsoid(FVector(0, 0, 112), FVector(36, 31, 24), Gold, 5);
    Ellipsoid(FVector(0, 0, 70), FVector(20, 21, 34), Cyan, 7);
    Ellipsoid(FVector(0, 0, 14), FVector(82, 39, 66), Cyan, 11);
    Ellipsoid(FVector(-76, 0, 45), FVector(38, 32, 25), Gold, 6);
    Ellipsoid(FVector(76, 0, 45), FVector(38, 32, 25), Gold, 6);
    Ellipsoid(FVector(-102, 0, -5), FVector(23, 24, 60), Cyan, 8);
    Ellipsoid(FVector(102, 0, -5), FVector(23, 24, 60), Cyan, 8);
    Ellipsoid(FVector(-17, -32, 161), FVector(7, 3, 3), Gold, 3);
    Ellipsoid(FVector(17, -32, 161), FVector(7, 3, 3), Gold, 3);
    Ellipsoid(FVector(0, -35, 145), FVector(4, 3, 11), Cyan, 4);
    Ellipsoid(FVector(0, -32, 127), FVector(13, 2, 2), Gold, 3);

    AvatarMesh->CreateMeshSection(0, Vertices, Triangles, Normals, UVs, Colors,
        TArray<FProcMeshTangent>(), false);
    AvatarMesh->SetCastShadow(false);
    UMaterialInterface* BaseMaterial = LoadObject<UMaterialInterface>(nullptr,
        TEXT("/Game/Humandroid/M_HumandroidGlow.M_HumandroidGlow"));
    if (!BaseMaterial) BaseMaterial = UMaterial::GetDefaultMaterial(MD_Surface);
    HoloMaterial = UMaterialInstanceDynamic::Create(BaseMaterial, this);
    if (HoloMaterial) AvatarMesh->SetMaterial(0, HoloMaterial);
}

void AHumandroidAvatarPawn::AddWireEllipsoid(FVector Center, FVector Radii, FLinearColor Color,
    int32 LatitudeCount, int32 LongitudeCount, float TubeRadius,
    TArray<FVector>& Vertices, TArray<int32>& Triangles, TArray<FVector>& Normals,
    TArray<FVector2D>& UVs, TArray<FColor>& Colors)
{
    const int32 Latitudes = FMath::Max(3, LatitudeCount);
    const int32 Longitudes = FMath::Max(8, LongitudeCount);
    for (int32 Lat = 1; Lat < Latitudes; ++Lat)
    {
        const float Phi = -Pi * 0.5f + Pi * static_cast<float>(Lat) / Latitudes;
        TArray<FVector> Ring;
        Ring.Reserve(Longitudes + 1);
        for (int32 Lon = 0; Lon <= Longitudes; ++Lon)
        {
            const float Theta = Tau * static_cast<float>(Lon) / Longitudes;
            Ring.Add(Center + FVector(Radii.X * FMath::Cos(Phi) * FMath::Cos(Theta),
                Radii.Y * FMath::Cos(Phi) * FMath::Sin(Theta), Radii.Z * FMath::Sin(Phi)));
        }
        AddTube(Ring, TubeRadius, Color, Vertices, Triangles, Normals, UVs, Colors);
    }

    for (int32 Lon = 0; Lon < Longitudes; Lon += 2)
    {
        const float Theta = Tau * static_cast<float>(Lon) / Longitudes;
        TArray<FVector> Meridian;
        Meridian.Reserve(Latitudes + 1);
        for (int32 Lat = 0; Lat <= Latitudes; ++Lat)
        {
            const float Phi = -Pi * 0.5f + Pi * static_cast<float>(Lat) / Latitudes;
            Meridian.Add(Center + FVector(Radii.X * FMath::Cos(Phi) * FMath::Cos(Theta),
                Radii.Y * FMath::Cos(Phi) * FMath::Sin(Theta), Radii.Z * FMath::Sin(Phi)));
        }
        AddTube(Meridian, TubeRadius, Color, Vertices, Triangles, Normals, UVs, Colors);
    }
}

void AHumandroidAvatarPawn::AddTube(const TArray<FVector>& Path, float Radius, FLinearColor Color,
    TArray<FVector>& Vertices, TArray<int32>& Triangles, TArray<FVector>& Normals,
    TArray<FVector2D>& UVs, TArray<FColor>& Colors)
{
    if (Path.Num() < 2) return;
    constexpr int32 Sides = 4;
    for (int32 Point = 0; Point < Path.Num(); ++Point)
    {
        const FVector Tangent = (Path[FMath::Min(Point + 1, Path.Num() - 1)]
            - Path[FMath::Max(Point - 1, 0)]).GetSafeNormal();
        const FVector Axis = FVector::CrossProduct(Tangent, FVector::UpVector).GetSafeNormal(
            UE_SMALL_NUMBER, FVector::RightVector);
        const FVector Other = FVector::CrossProduct(Tangent, Axis).GetSafeNormal();
        for (int32 Side = 0; Side < Sides; ++Side)
        {
            const float Angle = Tau * static_cast<float>(Side) / Sides;
            const FVector Normal = (Axis * FMath::Cos(Angle) + Other * FMath::Sin(Angle)).GetSafeNormal();
            Vertices.Add(Path[Point] + Normal * Radius);
            Normals.Add(Normal);
            UVs.Add(FVector2D(static_cast<float>(Side) / Sides, static_cast<float>(Point)));
            Colors.Add(Color.ToFColor(false));
        }
    }
    const int32 Start = Vertices.Num() - Path.Num() * Sides;
    for (int32 Point = 0; Point < Path.Num() - 1; ++Point)
    {
        for (int32 Side = 0; Side < Sides; ++Side)
        {
            const int32 A = Start + Point * Sides + Side;
            const int32 B = Start + Point * Sides + (Side + 1) % Sides;
            const int32 C = A + Sides;
            const int32 D = B + Sides;
            Triangles.Add(A); Triangles.Add(C); Triangles.Add(B);
            Triangles.Add(B); Triangles.Add(C); Triangles.Add(D);
        }
    }
}
