@tool
extends EditorExportPlugin


func _get_name() -> String:
	return "StepEngineExportPlugin"


func _supports_platform(platform: EditorExportPlatform) -> bool:
	return platform.get_os_name() == "Android"


func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
	var variant := "debug" if debug else "release"
	var path := "addons/step_engine/bin/%s/StepCounterPlugin-%s.aar" % [variant, variant]
	if FileAccess.file_exists("res://" + path):
		return PackedStringArray([path])
	push_warning(
		"StepEngine: AAR not found at res://%s. " % path +
		"See addons/step_engine/android/BUILD_INSTRUCTIONS.md"
	)
	return PackedStringArray()


func _get_android_dependencies(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
	return PackedStringArray()
