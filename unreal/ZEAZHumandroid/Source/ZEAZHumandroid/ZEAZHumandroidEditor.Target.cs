using UnrealBuildTool;

public class ZEAZHumandroidEditorTarget : TargetRules
{
    public ZEAZHumandroidEditorTarget(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Editor;
        DefaultBuildSettings = BuildSettingsVersion.V5;
        IncludeOrderVersion = EngineIncludeOrderVersion.Unreal5_8;
        ExtraModuleNames.Add("ZEAZHumandroid");
    }
}
