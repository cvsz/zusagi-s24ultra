import unreal


ASSET_PATH = "/Game/Humandroid"
ASSET_NAME = "M_HumandroidGlow"


def make_expression(material, expression_class, x, y):
    return unreal.MaterialEditingLibrary.create_material_expression(
        material, expression_class, x, y
    )


def connect(source, target, input_name, output_name=""):
    unreal.MaterialEditingLibrary.connect_material_expressions(
        source, output_name, target, input_name
    )


def create_material():
    tools = unreal.AssetToolsHelpers.get_asset_tools()
    existing = unreal.EditorAssetLibrary.load_asset(f"{ASSET_PATH}/{ASSET_NAME}")
    if existing:
        material = existing
    else:
        material = tools.create_asset(
            ASSET_NAME,
            ASSET_PATH,
            unreal.Material,
            unreal.MaterialFactoryNew(),
        )
    if not material:
        raise RuntimeError("Could not create the Humandroid hologram material")

    material.set_editor_property("shading_model", unreal.MaterialShadingModel.MSM_UNLIT)
    material.set_editor_property("blend_mode", unreal.BlendMode.BLEND_OPAQUE)
    material.set_editor_property("two_sided", True)
    unreal.MaterialEditingLibrary.delete_all_material_expressions(material)

    vertex = make_expression(material, unreal.MaterialExpressionVertexColor, -900, 0)
    holo = make_expression(material, unreal.MaterialExpressionVectorParameter, -900, 180)
    holo.set_editor_property("parameter_name", "HoloColor")
    holo.set_editor_property("default_value", unreal.LinearColor(0.15, 0.82, 1.0, 1.0))

    color = make_expression(material, unreal.MaterialExpressionMultiply, -620, 30)
    connect(vertex, color, "A", "RGB")
    connect(holo, color, "B")

    fresnel = make_expression(material, unreal.MaterialExpressionFresnel, -620, 300)
    fresnel.set_editor_property("exponent", 3.0)
    edge = make_expression(material, unreal.MaterialExpressionMultiply, -380, 260)
    connect(fresnel, edge, "A")
    edge_gain = make_expression(material, unreal.MaterialExpressionConstant, -600, 440)
    edge_gain.set_editor_property("r", 3.0)
    connect(edge_gain, edge, "B")
    brightness = make_expression(material, unreal.MaterialExpressionAdd, -150, 60)
    connect(edge, brightness, "A")
    base_gain = make_expression(material, unreal.MaterialExpressionConstant, -370, -80)
    base_gain.set_editor_property("r", 1.8)
    connect(base_gain, brightness, "B")
    output_color = make_expression(material, unreal.MaterialExpressionMultiply, 80, 40)
    connect(color, output_color, "A")
    connect(brightness, output_color, "B")

    pulse = make_expression(material, unreal.MaterialExpressionScalarParameter, -380, -260)
    pulse.set_editor_property("parameter_name", "Pulse")
    pulse.set_editor_property("default_value", 0.8)
    pulse_color = make_expression(material, unreal.MaterialExpressionMultiply, 300, 40)
    connect(output_color, pulse_color, "A")
    connect(pulse, pulse_color, "B")

    audio = make_expression(material, unreal.MaterialExpressionScalarParameter, -380, -450)
    audio.set_editor_property("parameter_name", "AudioAmplitude")
    audio.set_editor_property("default_value", 0.0)
    audio_gain = make_expression(material, unreal.MaterialExpressionMultiply, -120, -400)
    connect(audio, audio_gain, "A")
    audio_gain_value = make_expression(material, unreal.MaterialExpressionConstant, -340, -600)
    audio_gain_value.set_editor_property("r", 0.35)
    connect(audio_gain_value, audio_gain, "B")
    audio_factor = make_expression(material, unreal.MaterialExpressionAdd, 100, -360)
    connect(audio_gain, audio_factor, "A")
    audio_base = make_expression(material, unreal.MaterialExpressionConstant, -120, -560)
    audio_base.set_editor_property("r", 1.0)
    connect(audio_base, audio_factor, "B")
    audio_color = make_expression(material, unreal.MaterialExpressionMultiply, 480, 40)
    connect(pulse_color, audio_color, "A")
    connect(audio_factor, audio_color, "B")

    world = make_expression(material, unreal.MaterialExpressionWorldPosition, -900, 640)
    mask = make_expression(material, unreal.MaterialExpressionComponentMask, -680, 640)
    mask.set_editor_property("b", True)
    connect(world, mask, "Input")
    frequency = make_expression(material, unreal.MaterialExpressionMultiply, -450, 640)
    connect(mask, frequency, "A")
    frequency_value = make_expression(material, unreal.MaterialExpressionConstant, -660, 800)
    frequency_value.set_editor_property("r", 0.035)
    connect(frequency_value, frequency, "B")
    elapsed = make_expression(material, unreal.MaterialExpressionScalarParameter, -680, 980)
    elapsed.set_editor_property("parameter_name", "Elapsed")
    elapsed.set_editor_property("default_value", 0.0)
    scan_phase = make_expression(material, unreal.MaterialExpressionAdd, -220, 640)
    connect(frequency, scan_phase, "A")
    connect(elapsed, scan_phase, "B")
    scan = make_expression(material, unreal.MaterialExpressionSine, 0, 640)
    connect(scan_phase, scan, "Input")
    scan_gain = make_expression(material, unreal.MaterialExpressionMultiply, 200, 640)
    connect(scan, scan_gain, "A")
    scan_weight = make_expression(material, unreal.MaterialExpressionConstant, 0, 800)
    scan_weight.set_editor_property("r", 0.12)
    connect(scan_weight, scan_gain, "B")
    scan_level = make_expression(material, unreal.MaterialExpressionAdd, 400, 600)
    connect(scan_gain, scan_level, "A")
    scan_base = make_expression(material, unreal.MaterialExpressionConstant, 200, 820)
    scan_base.set_editor_property("r", 0.95)
    connect(scan_base, scan_level, "B")
    final = make_expression(material, unreal.MaterialExpressionMultiply, 620, 100)
    connect(audio_color, final, "A")
    connect(scan_level, final, "B")
    unreal.MaterialEditingLibrary.connect_material_property(
        final, "", unreal.MaterialProperty.MP_EMISSIVE_COLOR
    )

    unreal.MaterialEditingLibrary.recompile_material(material)
    unreal.EditorAssetLibrary.save_loaded_asset(material)
    unreal.log(f"Created {ASSET_PATH}/{ASSET_NAME}")


create_material()
