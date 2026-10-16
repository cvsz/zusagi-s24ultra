using UnrealBuildTool;

public class ZEAZHumandroid : ModuleRules
{
    public ZEAZHumandroid(ReadOnlyTargetRules Target) : base(Target)
    {
        PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;
        PublicDependencyModuleNames.AddRange(new[] {
            "Core", "CoreUObject", "Engine", "InputCore", "ProceduralMeshComponent"
        });
    }
}
