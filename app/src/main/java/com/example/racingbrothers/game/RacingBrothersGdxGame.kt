package com.example.racingbrothers.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.example.racingbrothers.audio.ArcadeSoundManager
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.camera.CameraRig
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.controls.ArcadeCarPhysics
import com.example.racingbrothers.controls.CarPhysicsState
import com.example.racingbrothers.controls.RacingInputState
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.vehicles.BalancedV1Stats
import com.example.racingbrothers.vehicles.VehicleCatalog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class RaceHudSnapshot(
    val speedKmh: Int = 0,
    val currentLap: Int = 1,
    val totalLaps: Int = CircuitTrack.TOTAL_LAPS,
    val positionRank: Int = 1,
    val elapsedMs: Long = 0L,
    val cameraMode: CameraMode = CameraMode.CLOSE_CHASE,
    val fps: Int = 60
)

/**
 * Core libGDX 3D racing loop for Racing Brothers V1.
 * Drives local physics, smoothly interpolates the remote online opponent, validates checkpoints
 * with the Cloudflare backend, and renders both cars + drivers on Circuito Neon Bay.
 */
class RacingBrothersGdxGame(
    private val sessionManager: MultiplayerSessionManager,
    private val initialCameraMode: CameraMode,
    private val graphicsQuality: GraphicsQuality,
    private val soundManager: ArcadeSoundManager,
    private val inputProvider: () -> RacingInputState,
    private val onHudUpdated: (RaceHudSnapshot) -> Unit
) : ApplicationAdapter() {

    private lateinit var modelBatch: ModelBatch
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment

    private var trackWorld: GdxTrackWorld? = null
    private var localCarRig: GdxVehicleRig? = null
    private var remoteCarRig: GdxVehicleRig? = null

    private val physics = ArcadeCarPhysics()
    private val cameraRig = CameraRig(initialCameraMode)

    private var localState: CarPhysicsState = physics.createStartingState(0)

    // Smoothly interpolated remote opponent visual state
    private var remoteX = 0f
    private var remoteZ = 0f
    private var remoteHeading = 0f
    private var remoteSpeed = 0f
    private var remoteSteer = 0f
    private var remoteWheelSpin = 0f
    private var remoteInitialized = false

    private var lastPhase: RoomPhase = RoomPhase.IDLE
    private var lastCountdownBeep: Int = -1
    private var raceClockStartMs: Long = 0L
    private var telemetryAccumulatorSec = 0f
    private var hudAccumulatorSec = 0f

    @Volatile
    private var pendingCameraToggle = false

    fun requestCameraToggle() {
        pendingCameraToggle = true
    }

    fun getCurrentCameraMode(): CameraMode = cameraRig.mode

    override fun create() {
        modelBatch = ModelBatch()

        camera = PerspectiveCamera(
            initialCameraMode.fieldOfView,
            Gdx.graphics.width.toFloat().coerceAtLeast(1f),
            Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        ).apply {
            near = 0.4f
            far = if (graphicsQuality == GraphicsQuality.HIGH) 360f else 240f
        }

        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.46f, 0.54f, 0.68f, 1f))
            add(DirectionalLight().set(0.88f, 0.93f, 1.0f, -0.55f, -0.85f, -0.35f))
            if (graphicsQuality == GraphicsQuality.HIGH) {
                // Electric-blue stadium rim fill light
                add(DirectionalLight().set(0.18f, 0.58f, 0.95f, 0.6f, -0.35f, 0.65f))
            }
        }

        trackWorld = GdxMeshFactory.buildTrackWorld(graphicsQuality)
        rebuildVehicleRigs()
        resetGridPositions()
    }

    private fun rebuildVehicleRigs() {
        localCarRig?.dispose()
        remoteCarRig?.dispose()

        val uiState = sessionManager.state.value
        val localPlayer = uiState.localPlayer
        val opponent = uiState.opponentPlayer

        val localVehicle = VehicleCatalog.getVehicle(localPlayer?.vehicleId ?: "vortex_rs")
        val localColor = VehicleCatalog.getPaintColor(localPlayer?.colorId ?: "electric_cobalt")
        val localChar = CharacterCatalog.getCharacter(localPlayer?.characterId ?: "nico_turbo")
        localCarRig = GdxMeshFactory.buildVehicleRig(localVehicle, localColor, localChar)

        val remoteVehicle = VehicleCatalog.getVehicle(opponent?.vehicleId ?: "apex_gtx")
        val remoteColor = VehicleCatalog.getPaintColor(opponent?.colorId ?: "crimson_ignite")
        val remoteChar = CharacterCatalog.getCharacter(opponent?.characterId ?: "kira_apex")
        remoteCarRig = GdxMeshFactory.buildVehicleRig(remoteVehicle, remoteColor, remoteChar)
    }

    private fun resetGridPositions() {
        val uiState = sessionManager.state.value
        val localSlot = uiState.localSlot.coerceIn(0, 1)
        val remoteSlot = if (localSlot == 0) 1 else 0

        localState = physics.createStartingState(localSlot)
        val (rx, rz, rHeading) = CircuitTrack.getStartSlot(remoteSlot)
        remoteX = rx
        remoteZ = rz
        remoteHeading = rHeading
        remoteSpeed = 0f
        remoteSteer = 0f
        remoteWheelSpin = 0f
        remoteInitialized = true

        cameraRig.snapToTarget(localState.x, localState.z, localState.headingRad)
        raceClockStartMs = 0L
    }

    override fun resize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            camera.viewportWidth = width.toFloat()
            camera.viewportHeight = height.toFloat()
            camera.update()
        }
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime.coerceIn(0.001f, 0.05f)
        val session = sessionManager.state.value

        if (pendingCameraToggle) {
            pendingCameraToggle = false
            cameraRig.toggleMode()
            soundManager.playUiClick()
        }

        // Detect phase transitions (e.g. rematch countdown reset or race start)
        if (session.phase != lastPhase) {
            if (session.phase == RoomPhase.COUNTDOWN) {
                resetGridPositions()
                lastCountdownBeep = -1
            } else if (session.phase == RoomPhase.RACING) {
                raceClockStartMs = System.currentTimeMillis()
                soundManager.playCountdownTick(isGo = true)
            } else if (session.phase == RoomPhase.FINISHED) {
                soundManager.playFinishFanfare()
            }
            lastPhase = session.phase
        }

        if (session.phase == RoomPhase.COUNTDOWN) {
            val count = session.countdownValue ?: -1
            if (count in 1..3 && count != lastCountdownBeep) {
                lastCountdownBeep = count
                soundManager.playCountdownTick(isGo = false)
            }
        }

        // 1. Update local arcade car physics
        val canDrive = session.phase == RoomPhase.RACING && !localState.finished
        val input = inputProvider()
        val (updatedState, checkpointEvent) = physics.step(
            state = localState,
            input = input,
            dtSeconds = dt,
            canDrive = canDrive,
            opponentX = if (remoteInitialized) remoteX else null,
            opponentZ = if (remoteInitialized) remoteZ else null
        )
        localState = updatedState

        // Notify backend authoritatively whenever a checkpoint is crossed
        if (checkpointEvent != null) {
            sessionManager.sendCheckpointPass(
                checkpointIndex = checkpointEvent.checkpointIndex,
                x = localState.x,
                z = localState.z
            )
            if (checkpointEvent.lapCompleted) {
                soundManager.playCheckpoint()
            }
        }

        // 2. Smoothly interpolate remote opponent telemetry (with capped dead-reckoning)
        updateRemoteOpponent(session, dt)

        // 3. Throttled WebSocket state broadcast (~18 Hz) to stay well within Cloudflare Free limits
        telemetryAccumulatorSec += dt
        if (session.phase == RoomPhase.RACING && telemetryAccumulatorSec >= 0.055f) {
            telemetryAccumulatorSec = 0f
            sessionManager.sendTelemetry(
                x = localState.x,
                z = localState.z,
                headingRad = localState.headingRad,
                speedMps = localState.speedMps,
                steerAngleDeg = localState.steerAngleDeg,
                completedLaps = localState.completedLaps,
                nextCheckpoint = localState.nextCheckpoint
            )
        }

        // 4. Update smooth follow camera
        val speedRatio = (abs(localState.speedMps) / BalancedV1Stats.MAX_SPEED_MPS).coerceIn(0f, 1f)
        val pose = cameraRig.update(
            targetX = localState.x,
            targetZ = localState.z,
            headingRad = localState.headingRad,
            speedRatio = speedRatio,
            dt = dt
        )
        camera.fieldOfView = pose.fov
        camera.position.set(pose.camX, pose.camY, pose.camZ)
        camera.lookAt(pose.lookX, pose.lookY, pose.lookZ)
        camera.up.set(0f, 1f, 0f)
        camera.update()

        // 5. Apply transforms to 3D rigs
        localCarRig?.updateTransform(
            x = localState.x,
            z = localState.z,
            headingRad = localState.headingRad,
            steerAngleDeg = localState.steerAngleDeg,
            wheelSpinDeg = localState.wheelSpinDeg
        )
        remoteCarRig?.updateTransform(
            x = remoteX,
            z = remoteZ,
            headingRad = remoteHeading,
            steerAngleDeg = remoteSteer,
            wheelSpinDeg = remoteWheelSpin
        )

        // 6. Render 3D scene (Deep midnight night-circuit sky)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.024f, 0.039f, 0.075f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        trackWorld?.let { world ->
            for (inst in world.staticInstances) {
                modelBatch.render(inst, environment)
            }
            for (arch in world.checkpointArches) {
                modelBatch.render(arch, environment)
            }
        }
        localCarRig?.let { rig ->
            modelBatch.render(rig.bodyInstance, environment)
            modelBatch.render(rig.driverInstance, environment)
            for (w in rig.wheels) {
                modelBatch.render(w.instance, environment)
            }
        }
        remoteCarRig?.let { rig ->
            modelBatch.render(rig.bodyInstance, environment)
            modelBatch.render(rig.driverInstance, environment)
            for (w in rig.wheels) {
                modelBatch.render(w.instance, environment)
            }
        }
        modelBatch.end()

        // 7. Publish HUD snapshot at ~15 Hz so Compose HUD stays crisp with minimal recomposition
        hudAccumulatorSec += dt
        if (hudAccumulatorSec >= 0.066f) {
            hudAccumulatorSec = 0f
            val localProgress = CircuitTrack.computeRaceProgress(
                completedLaps = localState.completedLaps,
                nextCheckpoint = localState.nextCheckpoint,
                x = localState.x,
                z = localState.z
            )
            val remoteTelemetry = session.remoteTelemetry
            val remoteProgress = if (remoteTelemetry != null) {
                CircuitTrack.computeRaceProgress(
                    completedLaps = remoteTelemetry.completedLaps,
                    nextCheckpoint = remoteTelemetry.nextCheckpoint,
                    x = remoteX,
                    z = remoteZ
                )
            } else {
                0f
            }
            val rank = if (localProgress >= remoteProgress) 1 else 2
            val elapsed = if (session.phase == RoomPhase.RACING && raceClockStartMs > 0L) {
                (System.currentTimeMillis() - raceClockStartMs).coerceAtLeast(0L)
            } else 0L

            onHudUpdated(
                RaceHudSnapshot(
                    speedKmh = (abs(localState.speedMps) * 3.6f).toInt(),
                    currentLap = (localState.completedLaps + 1).coerceAtMost(CircuitTrack.TOTAL_LAPS),
                    totalLaps = CircuitTrack.TOTAL_LAPS,
                    positionRank = rank,
                    elapsedMs = elapsed,
                    cameraMode = cameraRig.mode,
                    fps = Gdx.graphics.framesPerSecond
                )
            )
        }
    }

    private fun updateRemoteOpponent(session: com.example.racingbrothers.networking.MultiplayerUiState, dt: Float) {
        val telemetry = session.remoteTelemetry ?: return
        val ageSec = ((System.currentTimeMillis() - telemetry.receivedAtMs).coerceIn(0L, 160L)) / 1000f
        val targetX = telemetry.x + sin(telemetry.headingRad) * telemetry.speedMps * ageSec
        val targetZ = telemetry.z + cos(telemetry.headingRad) * telemetry.speedMps * ageSec

        val alpha = (1f - kotlin.math.exp(-12f * dt)).coerceIn(0.05f, 1f)
        remoteX += (targetX - remoteX) * alpha
        remoteZ += (targetZ - remoteZ) * alpha
        val angleDelta = ArcadeCarPhysics.normalizeAngle(telemetry.headingRad - remoteHeading)
        remoteHeading = ArcadeCarPhysics.normalizeAngle(remoteHeading + angleDelta * alpha)
        remoteSpeed += (telemetry.speedMps - remoteSpeed) * alpha
        remoteSteer += (telemetry.steerAngleDeg - remoteSteer) * alpha
        remoteWheelSpin = (remoteWheelSpin + (remoteSpeed * dt / 0.36f) * (180f / Math.PI.toFloat())) % 360f
        remoteInitialized = true
    }

    override fun dispose() {
        modelBatch.dispose()
        trackWorld?.dispose()
        localCarRig?.dispose()
        remoteCarRig?.dispose()
    }
}
