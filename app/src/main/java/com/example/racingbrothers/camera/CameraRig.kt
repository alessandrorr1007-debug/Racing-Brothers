package com.example.racingbrothers.camera

import kotlin.math.cos
import kotlin.math.sin

/**
 * Extensible camera profile architecture.
 * V1 ships with CLOSE_CHASE and WIDE_CHASE; V2+ can register additional profiles
 * (e.g., HOOD_CAM, COCKPIT_CAM, DYNAMIC_ORBIT) without altering the follow rig math.
 */
enum class CameraMode(
    val id: String,
    val displayName: String,
    val followDistance: Float,
    val followHeight: Float,
    val lookAheadDistance: Float,
    val lookAtHeight: Float,
    val fieldOfView: Float,
    val positionSmoothness: Float,
    val lookSmoothness: Float
) {
    CLOSE_CHASE(
        id = "close_chase",
        displayName = "Cercana",
        followDistance = 8.2f,
        followHeight = 3.4f,
        lookAheadDistance = 9.0f,
        lookAtHeight = 1.2f,
        fieldOfView = 67f,
        positionSmoothness = 9.5f,
        lookSmoothness = 12.0f
    ),
    WIDE_CHASE(
        id = "wide_chase",
        displayName = "Lejana",
        followDistance = 15.2f,
        followHeight = 7.4f,
        lookAheadDistance = 14.0f,
        lookAtHeight = 1.0f,
        fieldOfView = 72f,
        positionSmoothness = 7.5f,
        lookSmoothness = 10.0f
    );

    fun next(): CameraMode {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        fun fromId(id: String): CameraMode = entries.firstOrNull { it.id == id } ?: CLOSE_CHASE
    }
}

data class CameraPose(
    val camX: Float,
    val camY: Float,
    val camZ: Float,
    val lookX: Float,
    val lookY: Float,
    val lookZ: Float,
    val fov: Float
)

class CameraRig(initialMode: CameraMode = CameraMode.CLOSE_CHASE) {
    var mode: CameraMode = initialMode
        private set

    private var initialized = false
    private var curX = 0f
    private var curY = 4f
    private var curZ = -8f
    private var curLookX = 0f
    private var curLookY = 1f
    private var curLookZ = 5f
    private var curFov = initialMode.fieldOfView

    fun setMode(newMode: CameraMode) {
        mode = newMode
    }

    fun toggleMode(): CameraMode {
        mode = mode.next()
        return mode
    }

    fun snapToTarget(targetX: Float, targetZ: Float, headingRad: Float) {
        val fwdX = sin(headingRad)
        val fwdZ = cos(headingRad)
        curX = targetX - fwdX * mode.followDistance
        curY = mode.followHeight
        curZ = targetZ - fwdZ * mode.followDistance
        curLookX = targetX + fwdX * mode.lookAheadDistance
        curLookY = mode.lookAtHeight
        curLookZ = targetZ + fwdZ * mode.lookAheadDistance
        curFov = mode.fieldOfView
        initialized = true
    }

    fun update(
        targetX: Float,
        targetZ: Float,
        headingRad: Float,
        speedRatio: Float,
        dt: Float
    ): CameraPose {
        if (!initialized) {
            snapToTarget(targetX, targetZ, headingRad)
        }
        val clampedDt = dt.coerceIn(0.001f, 0.05f)
        val fwdX = sin(headingRad)
        val fwdZ = cos(headingRad)

        // Subtle speed pull-back and FOV boost for arcade velocity feel
        val speedBoostDist = speedRatio.coerceIn(0f, 1f) * 1.4f
        val desX = targetX - fwdX * (mode.followDistance + speedBoostDist)
        val desY = mode.followHeight + speedRatio.coerceIn(0f, 1f) * 0.25f
        val desZ = targetZ - fwdZ * (mode.followDistance + speedBoostDist)

        val desLookX = targetX + fwdX * mode.lookAheadDistance
        val desLookY = mode.lookAtHeight
        val desLookZ = targetZ + fwdZ * mode.lookAheadDistance
        val desFov = mode.fieldOfView + speedRatio.coerceIn(0f, 1f) * 6.5f

        val posAlpha = 1f - kotlin.math.exp(-mode.positionSmoothness * clampedDt)
        val lookAlpha = 1f - kotlin.math.exp(-mode.lookSmoothness * clampedDt)

        curX += (desX - curX) * posAlpha
        curY += (desY - curY) * posAlpha
        curZ += (desZ - curZ) * posAlpha

        curLookX += (desLookX - curLookX) * lookAlpha
        curLookY += (desLookY - curLookY) * lookAlpha
        curLookZ += (desLookZ - curLookZ) * lookAlpha
        curFov += (desFov - curFov) * posAlpha

        return CameraPose(curX, curY, curZ, curLookX, curLookY, curLookZ, curFov)
    }
}
