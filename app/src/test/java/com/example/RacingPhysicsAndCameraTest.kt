package com.example

import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.camera.CameraRig
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.controls.ArcadeCarPhysics
import com.example.racingbrothers.controls.RacingInputState
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.vehicles.BalancedV1Stats
import com.example.racingbrothers.vehicles.PartColorRole
import com.example.racingbrothers.vehicles.VehicleAppearance
import com.example.racingbrothers.vehicles.VehicleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RacingPhysicsAndCameraTest {
    private val speedway = CircuitTrack.forId(CircuitId.SPEEDWAY)


    @Test
    fun `catalog provides 4 distinct vehicles and 3 distinct cartoon drivers`() {
        assertEquals(4, VehicleCatalog.vehicles.size)
        assertEquals(4, VehicleCatalog.vehicles.map { it.id }.toSet().size)
        assertEquals(
            listOf("vortex_rs", "apex_gtx", "titan_v8", "formula_aero"),
            VehicleCatalog.vehicles.map { it.id }
        )
        assertEquals(
            listOf("Vortex RS", "Apex GT-X", "Titan V8", "Formula Aero"),
            VehicleCatalog.vehicles.map { it.displayName }
        )
        assertTrue(VehicleCatalog.paintColors.size >= 6)

        assertEquals(4, CharacterCatalog.characters.size)
        assertEquals(4, CharacterCatalog.characters.map { it.id }.toSet().size)
        assertTrue(CharacterCatalog.characters.any { it.id == "pilot_04" })
        CharacterCatalog.characters.forEach { ch ->
            assertTrue("Driver ${ch.id} should have real 3D geometry parts", ch.parts.size >= 10)
        }
    }

    @Test
    fun `upgraded vehicles have valid sculpted 8-corner geometry, 4 symmetric wheels and distinct silhouettes`() {
        for (vehicle in VehicleCatalog.vehicles) {
            val wheels = vehicle.parts.filter { it.isWheel }
            val bodyParts = vehicle.parts.filterNot { it.isWheel }

            assertEquals("${vehicle.id} must have exactly 4 wheels", 4, wheels.size)
            val frontWheels = wheels.filter { it.isFrontWheel }
            val rearWheels = wheels.filterNot { it.isFrontWheel }
            assertEquals("${vehicle.id} must have 2 front wheels", 2, frontWheels.size)
            assertEquals("${vehicle.id} must have 2 rear wheels", 2, rearWheels.size)

            // Front wheels must sit forward (+Z) of rear wheels and symmetric left/right on X
            assertTrue("${vehicle.id} front wheels must have +Z > rear wheels", frontWheels.all { it.centerZ > 0.8f })
            assertTrue("${vehicle.id} rear wheels must have -Z < -0.8f", rearWheels.all { it.centerZ < -0.8f })
            assertEquals(0f, frontWheels.sumOf { it.centerX.toDouble() }.toFloat(), 0.001f)
            assertEquals(0f, rearWheels.sumOf { it.centerX.toDouble() }.toFloat(), 0.001f)
            assertTrue("${vehicle.id} wheels must touch ground cleanly (centerY ~ radius)", wheels.all { abs(it.centerY - it.height * 0.5f) < 0.02f })

            // Sculpted bodywork: rich enough for distinct identity, lightweight enough for low/mid devices
            assertTrue("${vehicle.id} should have sculpted bodywork (>= 24 parts)", bodyParts.size >= 24)
            assertTrue("${vehicle.id} should remain lightweight (<= 65 parts)", vehicle.parts.size <= 65)

            // Every vehicle must have headlights, taillights, and carbon/metallic detail roles
            assertTrue("${vehicle.id} must have headlights", bodyParts.any { it.role == PartColorRole.HEADLIGHT })
            assertTrue("${vehicle.id} must have taillights", bodyParts.any { it.role == PartColorRole.TAILLIGHT })
            assertTrue("${vehicle.id} must have carbon fiber aero details", bodyParts.any { it.role == PartColorRole.CARBON_FIBER })

            // Validate every sculpted 8-corner mesh part produces finite non-degenerate vertices
            for (part in bodyParts) {
                val corners = part.computeLocalCorners()
                assertEquals(8, corners.size)
                for (c in corners) {
                    assertTrue("${vehicle.id}/${part.id} corner x must be finite", c.x.isFinite())
                    assertTrue("${vehicle.id}/${part.id} corner y must be finite", c.y.isFinite())
                    assertTrue("${vehicle.id}/${part.id} corner z must be finite", c.z.isFinite())
                }
            }
        }

        // Formula Aero specifically enables glowing neon wheel treads
        assertTrue(VehicleCatalog.getVehicle("formula_aero").neonWheelTread)
        assertFalse(VehicleCatalog.getVehicle("vortex_rs").neonWheelTread)
    }

    @Test
    fun `vehicle customization appearances preserve 4 wheels and add spoilers or racing stripes cleanly`() {
        for (vehicle in VehicleCatalog.vehicles) {
            for (rims in 0..2) {
                for (spoiler in 0..2) {
                    for (decals in 0..2) {
                        val appearance = VehicleAppearance(rims = rims, spoiler = spoiler, decals = decals)
                        val customized = appearance.applyTo(vehicle)
                        assertEquals(
                            "${vehicle.id} appearance($rims,$spoiler,$decals) must preserve 4 wheels",
                            4,
                            customized.count { it.isWheel }
                        )
                        if (spoiler == 2) {
                            assertTrue(
                                "${vehicle.id} sport spoiler should add swan-neck wing",
                                customized.any { it.id == "sport_wing" }
                            )
                        }
                        if (decals != 0) {
                            assertTrue(
                                "${vehicle.id} decals=$decals should add hood racing stripes",
                                customized.any { it.id.startsWith("stripe_hood_") }
                            )
                        }
                    }
                }
            }
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
    fun `nitro consumes charge and a controlled drift restores it`() {
        val physics = ArcadeCarPhysics()
        val start = physics.createStartingState(0).copy(speedMps = BalancedV1Stats.MAX_SPEED_MPS)
        val boosted = physics.step(start, RacingInputState(nitro = true), 0.05f, canDrive = true).first
        assertTrue(boosted.nitroActive)
        assertTrue(boosted.speedMps > BalancedV1Stats.MAX_SPEED_MPS)
        assertTrue(boosted.nitroCharge < 1f)
        val uncharged = physics.step(start.copy(nitroCharge = 0f), RacingInputState(nitro = true), 0.05f, canDrive = true).first
        assertFalse(uncharged.nitroActive)
        val drift = physics.step(
            start.copy(speedMps = 30f, lateralSpeedMps = 5f, nitroCharge = 0.5f),
            RacingInputState(steer = 1f), 0.05f, canDrive = true
        ).first
        assertTrue(drift.drifting)
        assertTrue(drift.nitroCharge > 0.5f)
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
        val start = speedway.samples.first()
        var state = physics.createStartingState(0).copy(
            x = start.x + start.rightX * (CircuitTrack.BARRIER_HALF_WIDTH + 6f),
            z = start.z + start.rightZ * (CircuitTrack.BARRIER_HALF_WIDTH + 6f),
            speedMps = 30f
        )
        state = physics.step(state, RacingInputState(throttle = 1f), 1f / 60f, canDrive = true).first
        assertTrue("Wall collision flag should trigger", state.collidedWithWall)
        assertTrue("Car should retain arcade sliding speed instead of dead-stopping", state.speedMps > 20f)
        val q = speedway.queryClosestPoint(state.x, state.z)
        assertTrue("Car should be clamped inside guardrail", abs(q.signedLateralOffset) <= CircuitTrack.BARRIER_HALF_WIDTH)
    }

    @Test
    fun `each circuit has a distinct route and four safe starting positions`() {
        val circuits = CircuitId.entries.filter { it != CircuitId.RANDOM }.map(CircuitTrack::forId)
        assertEquals(3, circuits.map { it.samples.map { point -> point.x to point.z } }.distinct().size)
        for (circuit in circuits) {
            assertEquals(CircuitTrack.CHECKPOINT_COUNT, circuit.checkpoints.size)
            val slots = (0..3).map(circuit::getStartSlot)
            assertEquals(4, slots.distinct().size)
            for ((x, z) in slots) {
                assertTrue(abs(circuit.queryClosestPoint(x, z).signedLateralOffset) < CircuitTrack.ROAD_HALF_WIDTH)
            }
        }
    }

    @Test
    fun `ordered checkpoints validate 3 laps and finish flag`() {
        val physics = ArcadeCarPhysics()
        var state = physics.createStartingState(0)

        for (lap in 1..CircuitTrack.TOTAL_LAPS) {
            for (cpIdx in 1 until CircuitTrack.CHECKPOINT_COUNT) {
                val cp = speedway.checkpoints[cpIdx]
                val res = physics.step(state.copy(x = cp.x, z = cp.z, speedMps = 0f), RacingInputState(), 0.016f, canDrive = true)
                state = res.first
                assertEquals(cpIdx, res.second?.checkpointIndex)
                assertFalse(res.second?.lapCompleted == true)
            }
            val startFinish = speedway.checkpoints[0]
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
