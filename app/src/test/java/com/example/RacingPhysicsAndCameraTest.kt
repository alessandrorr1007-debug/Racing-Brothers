package com.example

import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.camera.CameraRig
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.controls.ArcadeCarPhysics
import com.example.racingbrothers.controls.RacingInputState
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.vehicles.BalancedV1Stats
import com.example.racingbrothers.vehicles.VehicleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RacingPhysicsAndCameraTest {

    @Test
    fun `catalog provides 4 distinct vehicles and 3 distinct cartoon drivers`() {
        assertEquals(4, VehicleCatalog.vehicles.size)
        assertEquals(4, VehicleCatalog.vehicles.map { it.id }.toSet().size)
        assertTrue(VehicleCatalog.paintColors.size >= 6)

        assertEquals(3, CharacterCatalog.characters.size)
        assertEquals(3, CharacterCatalog.characters.map { it.id }.toSet().size)
        CharacterCatalog.characters.forEach { ch ->
            assertTrue("Driver ${ch.id} should have real 3D geometry parts", ch.parts.size >= 10)
        }
    }

    @Test
    fun `acceleration, max speed cap, braking, reverse, steering and wheel motion work`() {
        val physics = ArcadeCarPhysics()
        var state = physics.createStartingState(0)

        // Accelerate along the main straight for 2.2 seconds
        repeat(132) {
            state = physics.step(state, RacingInputState(throttle = 1f), 1f / 60f, canDrive = true).first
        }
        assertTrue("Car should accelerate forward (got ${state.speedMps} m/s)", state.speedMps > 35f)
        assertTrue("Car speed should never exceed V1 cap", state.speedMps <= BalancedV1Stats.MAX_SPEED_MPS + 0.01f)
        assertTrue("Wheels should spin while moving", abs(state.wheelSpinDeg) > 0.1f)

        // Steer right while moving
        val headingBeforeTurn = state.headingRad
        repeat(20) {
            state = physics.step(state, RacingInputState(throttle = 0.6f, steer = 1f), 1f / 60f, canDrive = true).first
        }
        assertNotEquals("Heading should change when steering", headingBeforeTurn, state.headingRad, 0.005f)
        assertTrue("Front wheels should show steering angle", abs(state.steerAngleDeg) > 1f)

        // Brake down to zero then enter reverse
        repeat(220) {
            state = physics.step(state, RacingInputState(brake = 1f), 1f / 60f, canDrive = true).first
        }
        assertTrue("Holding brake after stop should engage reverse", state.speedMps < -1f)
        assertTrue("Reverse speed should be capped", state.speedMps >= -BalancedV1Stats.MAX_REVERSE_MPS - 0.01f)
    }

    @Test
    fun `brake brings low forward speed to zero before engaging reverse`() {
        val physics = ArcadeCarPhysics()
        val moving = physics.createStartingState(0).copy(speedMps = 0.1f)
        val brake = RacingInputState(brake = 1f)
        val stopped = physics.step(moving, brake, 1f / 60f, canDrive = true).first
        assertEquals(0f, stopped.speedMps, 0.001f)
        val reversing = physics.step(stopped, brake, 1f / 60f, canDrive = true).first
        assertTrue(reversing.speedMps < 0f)
    }

    @Test
    fun `overlapping cars separate even when their positions match`() {
        val physics = ArcadeCarPhysics()
        val stopped = physics.createStartingState(0)
        val separated = physics.step(
            stopped, RacingInputState(), 1f / 60f, canDrive = true,
            opponentX = stopped.x, opponentZ = stopped.z
        ).first
        assertTrue(kotlin.math.hypot(separated.x - stopped.x, separated.z - stopped.z) > 1f)
    }

    @Test
    fun `barrier collision keeps car within circuit bounds while allowing arcade wall slide`() {
        val physics = ArcadeCarPhysics()
        val start = CircuitTrack.samples.first()
        var state = physics.createStartingState(0).copy(
            x = start.x + start.rightX * (CircuitTrack.BARRIER_HALF_WIDTH + 6f),
            z = start.z + start.rightZ * (CircuitTrack.BARRIER_HALF_WIDTH + 6f),
            speedMps = 30f
        )
        state = physics.step(state, RacingInputState(throttle = 1f), 1f / 60f, canDrive = true).first
        assertTrue("Wall collision flag should trigger", state.collidedWithWall)
        assertTrue("Car should retain arcade sliding speed instead of dead-stopping", state.speedMps > 20f)
        val q = CircuitTrack.queryClosestPoint(state.x, state.z)
        assertTrue("Car should be clamped inside guardrail", abs(q.signedLateralOffset) <= CircuitTrack.BARRIER_HALF_WIDTH)
    }

    @Test
    fun `ordered checkpoints validate 3 laps and finish flag`() {
        val physics = ArcadeCarPhysics()
        var state = physics.createStartingState(0)

        for (lap in 1..CircuitTrack.TOTAL_LAPS) {
            for (cpIdx in 1 until CircuitTrack.CHECKPOINT_COUNT) {
                val cp = CircuitTrack.checkpoints[cpIdx]
                val res = physics.step(state.copy(x = cp.x, z = cp.z, speedMps = 0f), RacingInputState(), 0.016f, canDrive = true)
                state = res.first
                assertEquals(cpIdx, res.second?.checkpointIndex)
                assertFalse(res.second?.lapCompleted == true)
            }
            val startFinish = CircuitTrack.checkpoints[0]
            val finishRes = physics.step(
                state.copy(x = startFinish.x, z = startFinish.z, speedMps = 0f),
                RacingInputState(),
                0.016f,
                canDrive = true
            )
            state = finishRes.first
            assertEquals(lap, state.completedLaps)
            assertTrue(finishRes.second?.lapCompleted == true)
        }
        assertTrue("Race should be finished after 3 valid laps", state.finished)
    }

    @Test
    fun `camera rig toggles cleanly between CLOSE_CHASE and WIDE_CHASE and follows smoothly`() {
        val rig = CameraRig(CameraMode.CLOSE_CHASE)
        assertEquals(CameraMode.CLOSE_CHASE, rig.mode)
        rig.snapToTarget(0f, 0f, 0f)

        val closePose = rig.update(0f, 5f, 0f, 0.5f, 0.016f)
        val toggled = rig.toggleMode()
        assertEquals(CameraMode.WIDE_CHASE, toggled)

        var widePose = closePose
        repeat(60) {
            widePose = rig.update(0f, 5f, 0f, 0.5f, 0.016f)
        }
        assertTrue("Wide camera should pull higher than close camera", widePose.camY > closePose.camY + 2f)
        assertEquals(CameraMode.CLOSE_CHASE, rig.toggleMode())
    }
}
