#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Pawn.h"
#include "HumandroidAvatarPawn.generated.h"

class UCameraComponent;
class UProceduralMeshComponent;
class UMaterialInstanceDynamic;
class USceneComponent;

UENUM(BlueprintType)
enum class EHumandroidVisualState : uint8
{
    Idle, Listening, Thinking, Streaming, Speaking, Error, Disconnected
};

UCLASS()
class ZEAZHUMANDROID_API AHumandroidAvatarPawn : public APawn
{
    GENERATED_BODY()

public:
    AHumandroidAvatarPawn();

    UFUNCTION(BlueprintCallable, Category="Humandroid|State")
    void SetVisualState(EHumandroidVisualState NewState);

    UFUNCTION(BlueprintCallable, Category="Humandroid|State")
    bool ApplyAssistantEvent(FName EventName, float AudioAmplitude = 0.0f);

    UFUNCTION(BlueprintCallable, Category="Humandroid|Audio")
    void SetSpeechAmplitude(float NormalizedAmplitude);

    UFUNCTION(BlueprintCallable, Category="Humandroid|Accessibility")
    void SetReducedMotion(bool bEnabled);

    UFUNCTION(BlueprintCallable, Category="Humandroid|Quality", meta=(ClampMin="0", ClampMax="2"))
    void SetQualityProfile(int32 NewProfile);

    UFUNCTION(BlueprintPure, Category="Humandroid|State")
    EHumandroidVisualState GetVisualState() const { return VisualState; }

    UFUNCTION(BlueprintPure, Category="Humandroid|Accessibility")
    bool IsReducedMotion() const { return bReducedMotion; }

protected:
    virtual void BeginPlay() override;
    virtual void Tick(float DeltaSeconds) override;

private:
    UPROPERTY(VisibleAnywhere)
    TObjectPtr<USceneComponent> SceneRoot;

    UPROPERTY(VisibleAnywhere)
    TObjectPtr<UCameraComponent> Camera;

    UPROPERTY(VisibleAnywhere)
    TObjectPtr<UProceduralMeshComponent> AvatarMesh;

    UPROPERTY(EditAnywhere, Category="Humandroid|Quality", meta=(ClampMin="12", ClampMax="32"))
    int32 CircumferenceSegments = 20;

    UPROPERTY(EditAnywhere, Category="Humandroid|Quality", meta=(ClampMin="0", ClampMax="2"))
    int32 QualityProfile = 1;

    UPROPERTY(EditAnywhere, Category="Humandroid|Accessibility")
    bool bReducedMotion = false;

    EHumandroidVisualState VisualState = EHumandroidVisualState::Idle;
    float SpeechAmplitude = 0.0f;
    float StateTime = 0.0f;
    float Elapsed = 0.0f;
    FLinearColor CurrentColor = FLinearColor(0.02f, 0.75f, 1.0f, 1.0f);
    FLinearColor TargetColor = FLinearColor(0.02f, 0.75f, 1.0f, 1.0f);
    UPROPERTY(Transient)
    TObjectPtr<UMaterialInstanceDynamic> HoloMaterial;

    void BuildOriginalAvatar();
    void AddWireEllipsoid(FVector Center, FVector Radii, FLinearColor Color,
                         int32 LatitudeCount, int32 LongitudeCount, float TubeRadius,
                         TArray<FVector>& Vertices, TArray<int32>& Triangles,
                         TArray<FVector>& Normals, TArray<FVector2D>& UVs,
                         TArray<FColor>& Colors);
    void AddTube(const TArray<FVector>& Path, float Radius, FLinearColor Color,
                 TArray<FVector>& Vertices, TArray<int32>& Triangles,
                 TArray<FVector>& Normals, TArray<FVector2D>& UVs,
                 TArray<FColor>& Colors);
    void ApplyStateMaterial();
};
