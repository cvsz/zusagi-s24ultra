#!/usr/bin/env python3
"""Static validation for the source-only Unreal project scaffold."""

import ast
import json
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parent
PROJECT = ROOT / "ZEAZHumandroid"


class UnrealProjectValidation(unittest.TestCase):
    def test_project_descriptor(self):
        descriptor = json.loads((PROJECT / "ZEAZHumandroid.uproject").read_text())
        self.assertEqual(descriptor["EngineAssociation"], "5.8")
        self.assertIn("ZEAZHumandroid", [module["Name"] for module in descriptor["Modules"]])
        enabled = {plugin["Name"] for plugin in descriptor["Plugins"] if plugin["Enabled"]}
        self.assertIn("ProceduralMeshComponent", enabled)

    def test_android_mobile_render_configuration(self):
        engine = (PROJECT / "Config/DefaultEngine.ini").read_text()
        profiles = (PROJECT / "Config/DefaultDeviceProfiles.ini").read_text()
        self.assertIn("MinSDKVersion=28", engine)
        self.assertIn("bBuildForArm64=True", engine)
        self.assertIn("r.Mobile.ShadingPath=0", engine)
        self.assertIn("r.DynamicGlobalIlluminationMethod=0", engine)
        self.assertIn("t.MaxFPS=60", profiles)
        self.assertIn("t.MaxFPS=30", profiles)

    def test_renderer_exposes_bounded_state_and_motion_controls(self):
        header = (PROJECT / "Source/ZEAZHumandroid/Public/HumandroidAvatarPawn.h").read_text()
        source = (PROJECT / "Source/ZEAZHumandroid/Private/HumandroidAvatarPawn.cpp").read_text()
        for state in ("Idle", "Listening", "Thinking", "Streaming", "Speaking", "Error", "Disconnected"):
            self.assertIn(state, header)
        self.assertIn("FMath::Clamp(NormalizedAmplitude, 0.0f, 1.0f)", source)
        self.assertIn("SetReducedMotion", header)
        self.assertIn("CreateMeshSection", source)

    def test_editor_material_script_parses(self):
        source = (PROJECT / "Content/Python/create_humandroid_material.py").read_text()
        ast.parse(source)
        self.assertIn("MP_EMISSIVE_COLOR", source)
        self.assertIn("MaterialExpressionFresnel", source)
        self.assertIn('"Elapsed"', source)

    def test_rendering_layer_has_no_provider_or_execution_credentials(self):
        source_files = list((PROJECT / "Source").rglob("*.h")) + list((PROJECT / "Source").rglob("*.cpp"))
        source = "\n".join(path.read_text() for path in source_files)
        self.assertNotRegex(source, re.compile(r"(?i)(bearer\s+[A-Za-z0-9._-]{12,}|sk-[A-Za-z0-9]{16,})"))
        self.assertNotIn("HttpModule", source)
        self.assertNotIn("FPlatformProcess::CreateProc", source)

    def test_android_ipc_is_package_scoped_and_signature_protected(self):
        bridge = (ROOT.parent / "android/app/src/main/java/dev/zeaz/ultimate/HumandroidRendererBridge.java").read_text()
        manifest = (ROOT.parent / "android/app/src/main/AndroidManifest.xml").read_text()
        upl = (PROJECT / "Source/ZEAZHumandroid/Humandroid_UPL.xml").read_text()
        native = (PROJECT / "Source/ZEAZHumandroid/Private/ZEAZHumandroid.cpp").read_text()
        self.assertIn('.setPackage(RENDERER_PACKAGE)', bridge)
        self.assertIn('context.sendBroadcast(event, SIGNATURE_PERMISSION)', bridge)
        self.assertIn('android:protectionLevel="signature"', manifest)
        self.assertIn('addPermission android:name="dev.zeaz.ultimate.permission.HUMANDROID_RENDER_EVENT"', upl)
        self.assertIn('registerReceiver(zeazStateReceiver, filter, ZEAZ_PERMISSION', upl)
        self.assertIn('Context.RECEIVER_EXPORTED', upl)
        self.assertIn('nativeHumandroidAssistantEvent', native)
        self.assertIn('ENamedThreads::GameThread', native)
        self.assertNotIn('Bearer ', native)


if __name__ == "__main__":
    unittest.main(verbosity=2)
