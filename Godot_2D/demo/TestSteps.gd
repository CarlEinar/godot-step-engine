extends Control

var _plugin = null


func _ready() -> void:
	$VBoxContainer/StartButton.pressed.connect(_on_start_pressed)
	$VBoxContainer/StopButton.pressed.connect(_on_stop_pressed)
	$VBoxContainer/SimulateButton.pressed.connect(_on_simulate_pressed)

	if OS.get_name() == "Android":
		$VBoxContainer/SimulateButton.visible = false
		_setup_plugin()
	else:
		_set_status("Desktop mode — use Simulate button")


func _setup_plugin() -> void:
	if not Engine.has_singleton("StepCounterPlugin"):
		_set_status("Plugin not loaded.\nCheck bin/ has the .aar and addon is enabled.")
		return

	_plugin = Engine.get_singleton("StepCounterPlugin")
	_plugin.steps_changed.connect(_on_steps_changed)
	_plugin.permission_result.connect(_on_permission_result)

	if not _plugin.isStepCounterAvailable():
		_set_status("No step counter sensor on this device.")
		return

	var perm := _plugin.checkPermission()
	if perm == 0:  # PERMISSION_GRANTED
		_plugin.startListening()
		_set_status("Tracking steps...")
	else:
		_plugin.requestPermission()
		_set_status("Requesting permission...\nRestart if dialog does not appear.")


func _on_start_pressed() -> void:
	if OS.get_name() != "Android" or _plugin == null:
		return
	if _plugin.checkPermission() != 0:
		_plugin.requestPermission()
		_set_status("Permission required. Grant it in Android Settings.")
		return
	_plugin.startListening()
	_set_status("Tracking steps...")


func _on_stop_pressed() -> void:
	if _plugin:
		_plugin.stopListening()
	_set_status("Stopped.")


func _on_simulate_pressed() -> void:
	# Desktop only — injects a fake step count for UI testing
	var fake := randi_range(50, 9000)
	$VBoxContainer/StepLabel.text = "Steps (simulated): %d" % fake
	_set_status("Simulated step count")


func _on_steps_changed(raw_count: int) -> void:
	var session := _plugin.getSessionSteps() if _plugin else 0
	$VBoxContainer/StepLabel.text = (
		"Session steps: %d\nRaw (since reboot): %d" % [session, raw_count]
	)


func _on_permission_result(_code: int, _permission: String, result: int) -> void:
	if result == 0:  # PERMISSION_GRANTED
		_plugin.startListening()
		_set_status("Permission granted. Tracking...")
	else:
		_set_status("Permission denied.\nGo to Android Settings → Apps → Permissions.")


func _set_status(msg: String) -> void:
	$VBoxContainer/StatusLabel.text = msg
	print("[TestSteps] ", msg)
