package com.carleinarhellenes.stepengine

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class StepCounterPlugin(godot: Godot) : GodotPlugin(godot), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterPlugin"
        private const val PERMISSION = "android.permission.ACTIVITY_RECOGNITION"
        private const val PERMISSION_REQUEST_CODE = 1337

        // Returned by checkPermission()
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = 1
        const val PERMISSION_DENIED_RATIONALE = 2
    }

    override fun getPluginName() = "StepCounterPlugin"

    private val act = activity ?: throw IllegalStateException("Activity is null")
    private val sensorManager: SensorManager =
        act.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    // Raw sensor value when startListening() was first called this session
    private var rawBaseline: Long = -1L
    // Steps accumulated this session (raw - baseline)
    private var sessionSteps: Long = 0L

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        return mutableSetOf(
            // Emitted on every sensor update. raw_count is steps since last device reboot.
            SignalInfo("steps_changed", Long::class.javaObjectType),
            // Emitted after requestPermission() completes.
            // result: 0=granted, 1=denied, 2=denied_rationale
            SignalInfo("permission_result", Int::class.javaObjectType, String::class.java, Int::class.javaObjectType)
        )
    }

    /** Returns true if the device has a hardware step counter sensor. */
    @UsedByGodot
    fun isStepCounterAvailable(): Boolean = stepSensor != null

    /**
     * Returns 0=GRANTED, 1=DENIED, 2=DENIED_RATIONALE.
     * Call before startListening() to ensure permission is granted.
     */
    @UsedByGodot
    fun checkPermission(): Int {
        return when (act.checkSelfPermission(PERMISSION)) {
            PackageManager.PERMISSION_GRANTED -> PERMISSION_GRANTED
            else -> if (act.shouldShowRequestPermissionRationale(PERMISSION))
                PERMISSION_DENIED_RATIONALE else PERMISSION_DENIED
        }
    }

    /** Triggers the Android system permission dialog. Result comes via permission_result signal. */
    @UsedByGodot
    fun requestPermission() {
        act.requestPermissions(arrayOf(PERMISSION), PERMISSION_REQUEST_CODE)
    }

    /** Begin counting steps. Sets the session baseline on the first sensor reading. */
    @UsedByGodot
    fun startListening() {
        if (stepSensor == null) {
            Log.w(TAG, "No step counter sensor on this device")
            return
        }
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d(TAG, "Listening started")
    }

    /** Stop listening to the step counter sensor. */
    @UsedByGodot
    fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Listening stopped")
    }

    /** Returns steps counted since startListening() was called (or since resetSteps()). */
    @UsedByGodot
    fun getSessionSteps(): Long = sessionSteps

    /** Resets the session baseline so steps restart from 0. */
    @UsedByGodot
    fun resetSteps() {
        rawBaseline = -1L
        sessionSteps = 0L
    }

    // --- SensorEventListener ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val raw = event.values[0].toLong()
        if (rawBaseline < 0L) {
            rawBaseline = raw
        }
        sessionSteps = raw - rawBaseline
        emitSignal("steps_changed", raw)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- Lifecycle ---

    override fun onMainRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ) {
        super.onMainRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE || permissions.isNullOrEmpty()) return
        val result = if (grantResults?.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            PERMISSION_GRANTED else PERMISSION_DENIED
        emitSignal("permission_result", 0, permissions.first(), result)
    }

    override fun onMainDestroy() {
        stopListening()
        super.onMainDestroy()
    }
}
