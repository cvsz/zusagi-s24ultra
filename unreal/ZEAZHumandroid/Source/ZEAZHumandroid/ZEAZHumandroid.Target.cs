using UnrealBuildTool;

public class ZEAZHumandroidTarget : TargetRules
{
    public ZEAZHumandroidTarget(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Game;
        DefaultBuildSettings = BuildSettingsVersion.V5;
        IncludeOrderVersion = EngineIncludeOrderVersion.Unreal5_8;
        ExtraModuleNames.Add("ZEAZHumandroid");
    }
}
