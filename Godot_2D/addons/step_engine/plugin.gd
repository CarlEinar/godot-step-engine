@tool
extends EditorPlugin

var _export_plugin: EditorExportPlugin


func _enable_plugin() -> void:
	_export_plugin = preload("res://addons/step_engine/export_plugin.gd").new()
	add_export_plugin(_export_plugin)
	# StepManager and StepCurrency autoloads added in Phase 2


func _disable_plugin() -> void:
	remove_export_plugin(_export_plugin)
	_export_plugin = null
