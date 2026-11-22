package com.example.racingbrothers.controls

import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.vehicles.BalancedV1Stats
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

/**
 * Multi-touch arcade driving inputs.
 */
data class RacingInputState(
    val throttle: Float = 0f,   // 0..1
    val brake: Float = 0f,      // 0..1 (brake when moving forward, reverse when stopped)
    val steer: Float = 0f       // -1 (left) .. +1 (right)
)

data class CarPhysicsState(
    val x: Float = 0f,
    val z: Float = 0f,
    val headingRad: Float = 0f,
    val speedMps: Float = 0f,
    val steerAngleDeg: Float = 0f,
    val wheelSpinDeg: Float = 0f,
    val completedLaps: Int = 0,
    val nextCheckpoint: Int = 1,
    val finished: Boolean = false,
    val collidedWithWall: Boolean = false
)

data class CheckpointEvent(
    val checkpointIndex: Int,
    val lapCompleted: Boolean,
    val newCompletedLaps: Int,
    val raceFinished: Boolean
)

/**
 * Deterministic arcade vehicle physics engine.
 * Designed to be easy to pick up, responsive, fun, and 100% testable on JVM without OpenGL.
 */
class ArcadeCarPhysics {

    fun createStartingState(slotIndex: Int): CarPhysicsState {
        val (x, z, heading) = CircuitTrack.getStartSlot(slotIndex)
        return CarPhysicsState(
            x = x,
            z = z,
            headingRad = heading,
            speedMps = 0f,
            steerAngleDeg = 0f,
            wheelSpinDeg = 0f,
            completedLaps = 0,
            nextCheckpoint = 1,
            finished = false
        )
    }

    fun step(
        state: CarPhysicsState,
        input: RacingInputState,
        dtSeconds: Float,
        canDrive: Boolean,
        opponentX: Float? = null,
        opponentZ: Float? = null
    ): Pair<CarPhysicsState, CheckpointEvent?> {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        val throttle = if (canDrive) input.throttle.coerceIn(0f, 1f) else 0f
        val brake = if (canDrive) input.brake.coerceIn(0f, 1f) else 0f
        val steerInput = if (canDrive) input.steer.coerceIn(-1f, 1f) else 0f

        var speed = state.speedMps

        // 1. Longitudinal acceleration, braking, reverse & natural drag
        if (throttle > 0.01f && brake < 0.01f) {
            // Progressive arcade acceleration curve (snappy launch, tapering near top speed)
            val speedRatio = (speed / BalancedV1Stats.MAX_SPEED_MPS).coerceIn(0f, 1f)
            val accelScale = 1.15f - 0.45f * speedRatio
            speed += BalancedV1Stats.ACCELERATION_MPS2 * accelScale * throttle * dt
        } else if (brake > 0.01f) {
            if (speed > 0.8f) {
                speed = (speed - BalancedV1Stats.BRAKE_DECEL_MPS2 * brake * dt).coerceAtLeast(0f)
            } else {
                // Reverse gear
                speed -= (BalancedV1Stats.ACCELERATION_MPS2 * 0.55f) * brake * dt
            }
        } else {
            // Coasting friction & aerodynamic drag
            val drag = (BalancedV1Stats.COAST_DRAG_MPS2 + abs(speed) * 0.14f) * dt
            speed = when {
                abs(speed) <= drag -> 0f
                speed > 0f -> speed - drag
                else -> speed + drag
            }
        }

        // Scrub a little speed on sharp cornering for arcade grip feel
        if (abs(steerInput) > 0.3f && abs(speed) > 12f) {
            speed -= sign(speed) * abs(steerInput) * 3.8f * dt
        }

        speed = speed.coerceIn(-BalancedV1Stats.MAX_REVERSE_MPS, BalancedV1Stats.MAX_SPEED_MPS)

        // 2. Steering (only turns when car has velocity; inverted cleanly in reverse)
        val speedFactor = (abs(speed) / 9f).coerceIn(0f, 1f)
        val highSpeedStability = 1f - 0.28f * (abs(speed) / BalancedV1Stats.MAX_SPEED_MPS).coerceIn(0f, 1f)
        val reverseDir = if (speed >= -0.05f) 1f else -1f
        // Positive steerInput = turn right -> in +X right / +Z forward, turning right decreases heading?
        // Wait: fwdX = sin(heading), fwdZ = cos(heading). Increasing heading moves fwd toward +X (right)!
        // Let's verify: heading=0 -> (0, 1). heading=+pi/2 -> (1, 0) which is +X (right in left-handed or screen-right when camera looks +Z).
        // Wait in right-handed 3D (+Y up, camera behind car looking +Z): screen right is -X!
        // Let's check: cross(forward=(0,0,1), up=(0,1,0)) = (-1, 0, 0) -> -X is right!
        // Let's be 100% consistent with right-handed OpenGL/libGDX!
        // In libGDX right-handed coordinates (+Y up):
        // Forward = (sin(h), 0, cos(h)). Up = (0, 1, 0).
        // Right = Forward x Up = (0*0 - cos*1, 0, sin*1 - 0) = (-cos(h), 0, sin(h)).
        // So turning RIGHT rotates Forward toward (-cos(h), 0, sin(h)), which means DECREASING heading h!
        val deltaHeading = -steerInput * BalancedV1Stats.STEER_RATE_RAD_PER_SEC * speedFactor * highSpeedStability * reverseDir * dt
        var heading = normalizeAngle(state.headingRad + deltaHeading)

        // 3. Integrate position
        var nextX = state.x + sin(heading) * speed * dt
        var nextZ = state.z + cos(heading) * speed * dt

        // 4. Track barrier collision & sliding response
        var hitWall = false
        val query = CircuitTrack.queryClosestPoint(nextX, nextZ)
        val maxOffset = CircuitTrack.BARRIER_HALF_WIDTH - 1.25f
        if (abs(query.signedLateralOffset) > maxOffset) {
            hitWall = true
            val clampedOffset = sign(query.signedLateralOffset) * maxOffset
            nextX = query.closestX + query.rightX * clampedOffset
            nextZ = query.closestZ + query.rightZ * clampedOffset
            // Framerate-independent arcade wall scrape: scrub speed smoothly & slide along guardrail
            val scrapeDamping = (1f - 1.8f * dt).coerceIn(0.85f, 0.99f)
            speed *= scrapeDamping
            val trackHeading = atan2(query.tangentX, query.tangentZ)
            val angleDiff = normalizeAngle(trackHeading - heading)
            heading = normalizeAngle(heading + angleDiff * (5.5f * dt).coerceAtMost(0.35f))
        }

        // 5. Simple elastic car-to-car collision separation
        if (opponentX != null && opponentZ != null) {
            val dx = nextX - opponentX
            val dz = nextZ - opponentZ
            val dist = hypot(dx, dz)
            val minSeparation = 2.65f
            if (dist in 0.001f..minSeparation) {
                val push = (minSeparation - dist) * 0.55f
                nextX += (dx / dist) * push
                nextZ += (dz / dist) * push
                speed *= 0.92f
            }
        }

        // 6. Visual wheel animation
        val targetWheelSteer = -steerInput * 26f
        val smoothSteerDeg = state.steerAngleDeg + (targetWheelSteer - state.steerAngleDeg) * (12f * dt).coerceAtMost(1f)
        val wheelRadius = 0.36f
        val wheelSpinDeg = (state.wheelSpinDeg + (speed * dt / wheelRadius) * (180f / Math.PI.toFloat())) % 360f

        // 7. Ordered checkpoint & 3-lap detection
        var completedLaps = state.completedLaps
        var nextCheckpoint = state.nextCheckpoint
        var finished = state.finished
        var checkpointEvent: CheckpointEvent? = null

        if (canDrive && !finished && CircuitTrack.isInsideCheckpoint(nextCheckpoint, nextX, nextZ)) {
            val passedIndex = nextCheckpoint
            if (passedIndex == 0) {
                // Crossed start/finish line after visiting checkpoints 1..5 in order!
                completedLaps += 1
                nextCheckpoint = 1
                if (completedLaps >= CircuitTrack.TOTAL_LAPS) {
                    finished = true
                }
                checkpointEvent = CheckpointEvent(
                    checkpointIndex = 0,
                    lapCompleted = true,
                    newCompletedLaps = completedLaps,
                    raceFinished = finished
                )
            } else {
                nextCheckpoint = (passedIndex + 1) % CircuitTrack.CHECKPOINT_COUNT
                checkpointEvent = CheckpointEvent(
                    checkpointIndex = passedIndex,
                    lapCompleted = false,
                    newCompletedLaps = completedLaps,
                    raceFinished = false
                )
            }
        }

        val updated = CarPhysicsState(
            x = nextX,
            z = nextZ,
            headingRad = heading,
            speedMps = speed,
            steerAngleDeg = smoothSteerDeg,
            wheelSpinDeg = wheelSpinDeg,
            completedLaps = completedLaps,
            nextCheckpoint = nextCheckpoint,
            finished = finished,
            collidedWithWall = hitWall
        )
        return updated to checkpointEvent
    }

    companion object {
        fun normalizeAngle(angleRad: Float): Float {
            var a = angleRad
            val twoPi = (2.0 * Math.PI).toFloat()
            val pi = Math.PI.toFloat()
            while (a > pi) a -= twoPi
            while (a < -pi) a += twoPi
            return a
        }
    }
}
