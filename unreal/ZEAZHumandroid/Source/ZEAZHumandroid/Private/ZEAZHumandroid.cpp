#include "Modules/ModuleManager.h"

#if PLATFORM_ANDROID
#include "Android/AndroidJNI.h"
#include "Async/Async.h"
#include "Engine/Engine.h"
#include "EngineUtils.h"
#include "HumandroidAvatarPawn.h"

extern "C" JNIEXPORT void JNICALL
Java_com_epicgames_unreal_GameActivity_nativeHumandroidAssistantEvent(
    JNIEnv* Env, jobject /*Activity*/, jstring State, jfloat AudioAmplitude, jlong Sequence)
{
    if (Env == nullptr || State == nullptr || Sequence <= 0) return;

    const char* UtfChars = Env->GetStringUTFChars(State, nullptr);
    if (UtfChars == nullptr) return;
    const FString EventName(UTF8_TO_TCHAR(UtfChars));
    Env->ReleaseStringUTFChars(State, UtfChars);

    const float SafeAmplitude = FMath::IsFinite(AudioAmplitude)
        ? FMath::Clamp(AudioAmplitude, 0.0f, 1.0f) : 0.0f;
    const uint64 EventSequence = static_cast<uint64>(Sequence);

    AsyncTask(ENamedThreads::GameThread, [EventName, SafeAmplitude, EventSequence]()
    {
        static uint64 LastAppliedSequence = 0;
        if (EventSequence <= LastAppliedSequence || !GEngine) return;
        LastAppliedSequence = EventSequence;

        for (const FWorldContext& Context : GEngine->GetWorldContexts())
        {
            UWorld* World = Context.World();
            if (!World || (World->WorldType != EWorldType::Game && World->WorldType != EWorldType::GamePreview)) continue;
            for (TActorIterator<AHumandroidAvatarPawn> It(World); It; ++It)
            {
                It->ApplyAssistantEvent(FName(*EventName), SafeAmplitude);
            }
        }
    });
}
#endif

IMPLEMENT_PRIMARY_GAME_MODULE(FDefaultGameModuleImpl, ZEAZHumandroid, "ZEAZHumandroid");
