package com.example.racingbrothers.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.example.racingbrothers.audio.ArcadeSoundManager
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.camera.CameraRig
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.controls.ArcadeCarPhysics
import com.example.racingbrothers.controls.CarPhysicsState
import com.example.racingbrothers.controls.RacingInputState
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.ProtocolConstants
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.tracks.TrackMarker
import com.example.racingbrothers.vehicles.BalancedV1Stats
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.racingbrothers.vehicles.VehicleAppearance
import net.mgsx.gltf.scene3d.scene.SceneManager
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
    val fps: Int = 60,
    val nitroCharge: Float = 1f,
    val nitroActive: Boolean = false,
    val playerCount: Int = ProtocolConstants.DEFAULT_ROOM_CAPACITY,
    val circuit: CircuitId = CircuitId.SPEEDWAY,
    val markers: List<TrackMarker> = emptyList()
)

/**
 * Core libGDX 3D racing loop for Racing Brothers V1.
 * Drives local physics, smoothly interpolates the remote online players, validates checkpoints
 * with the Cloudflare backend, and renders every connected car + driver on Circuito Neon Bay.
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
    private lateinit var flameModel: Model
    private lateinit var smokeModel: Model
    private lateinit var flames: List<ModelInstance>
    private lateinit var smoke: List<ModelInstance>
    private var effectsTime = 0f
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment

    private var trackWorld: GdxTrackWorld? = null
    private var localCarRig: GdxVehicleRig? = null
    private val rivals = mutableMapOf<String, RivalCar>()

    // Blender GLB pipeline: GPU assets shared by all instances, procedural rigs remain fallback.
    private var glbAssets: GlbAssetCache? = null
    private var sceneManager: SceneManager? = null
    private var localGlbRig: GlbVehicleRig? = null
    private val glbRivals = mutableMapOf<String, GlbVehicleRig>()

    private var track = CircuitTrack.forId(CircuitId.SPEEDWAY)
    private var physics = ArcadeCarPhysics(track)
    private val cameraRig = CameraRig(initialCameraMode)

    private var localState: CarPhysicsState = physics.createStartingState(0)

    private data class RivalCar(
        val rig: GdxVehicleRig?,
        var x: Float,
        var z: Float,
        var heading: Float,
        var speed: Float = 0f,
        var steer: Float = 0f,
        var wheelSpin: Float = 0f
    )

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
        val effectMeshes = ModelBuilder()
        val effectAttributes = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        flameModel = effectMeshes.createCone(
            0.35f, 1.45f, 0.35f, 6,
            Material(ColorAttribute.createEmissive(Color(0.08f, 0.55f, 1f, 1f))),
            effectAttributes
        )
        smokeModel = effectMeshes.createSphere(
            0.8f, 0.5f, 0.8f, 5, 3,
            Material(ColorAttribute.createDiffuse(Color(0.36f, 0.40f, 0.47f, 1f))),
            effectAttributes
        )
        flames = List(ProtocolConstants.MAX_PLAYERS_PER_ROOM) { ModelInstance(flameModel) }
        smoke = List(ProtocolConstants.MAX_PLAYERS_PER_ROOM * 2) { ModelInstance(smokeModel) }

        camera = PerspectiveCamera(
            initialCameraMode.fieldOfView,
            Gdx.graphics.width.toFloat().coerceAtLeast(1f),
            Gdx.graphics.height.toFloat().coerceAtLeast(1f)
        ).apply {
            near = 0.4f
            far = if (graphicsQuality == GraphicsQuality.HIGH) 360f else 240f
        }

        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.44f, 0.52f, 0.66f, 1f))
            // Crisp cool-white stadium overhead key light for metallic clearcoat highlights
            add(DirectionalLight().set(0.95f, 0.97f, 1.0f, -0.52f, -0.82f, -0.32f))
            if (graphicsQuality == GraphicsQuality.HIGH) {
                // Electric-blue stadium rim fill + subtle crimson back-rim for neon arcade depth
                add(DirectionalLight().set(0.22f, 0.64f, 1.0f, 0.62f, -0.32f, 0.62f))
                add(DirectionalLight().set(0.42f, 0.12f, 0.22f, -0.45f, -0.22f, 0.78f))
            }
        }

        glbAssets = GlbAssetCache()
        sceneManager = SceneManager(32).apply {
            setCamera(camera)
            environment = this@RacingBrothersGdxGame.environment
        }

        resetGridPositions()
    }

    private fun buildGlbRig(vehicleId: String, characterId: String, lod: Int): GlbVehicleRig? {
        val assets = glbAssets ?: return null
        val carAsset = assets.car(vehicleId, lod) ?: return null
        return try {
            GlbVehicleRig(
                carAsset = carAsset,
                lod = lod,
                pilotAsset = assets.pilot(characterId),
                cockpitY = VehicleCatalog.getVehicle(vehicleId).cockpitY,
                seatZ = BlenderAssets.cars[vehicleId]?.seatZ ?: -0.18f
            )
        } catch (t: Throwable) {
            Gdx.app?.error("RacingBrothersGdxGame", "GLB rig failed for $vehicleId: ${t.message}")
            null
        }
    }

    private fun registerGlbRig(rig: GlbVehicleRig) {
        rig.scenes.forEach { sceneManager?.addScene(it) }
    }

    private fun unregisterGlbRig(rig: GlbVehicleRig) {
        rig.scenes.forEach { sceneManager?.removeScene(it) }
    }

    private fun resetGridPositions() {
        localCarRig?.dispose()
        localCarRig = null
        rivals.values.forEach { it.rig?.dispose() }
        rivals.clear()
        localGlbRig?.let { unregisterGlbRig(it) }
        glbRivals.values.forEach { unregisterGlbRig(it) }
        localGlbRig = null
        glbRivals.clear()
        val session = sessionManager.state.value
        val resolvedTrack = CircuitTrack.forId(session.circuit)
        if (trackWorld == null || track !== resolvedTrack) {
            trackWorld?.dispose()
            track = resolvedTrack
            physics = ArcadeCarPhysics(track)
            trackWorld = GdxMeshFactory.buildTrackWorld(graphicsQuality, track)
        }
        val local = session.localPlayer
        val localVehicleId = local?.vehicleId ?: "vortex_rs"
        val localCharacterId = BlenderAssets.wirePilot(local?.characterId ?: "nico_turbo")
        val localLod = if (graphicsQuality == GraphicsQuality.HIGH) 0 else 1
        localGlbRig = buildGlbRig(localVehicleId, localCharacterId, localLod)
        if (localGlbRig != null) {
            registerGlbRig(localGlbRig!!)
        } else {
            localCarRig = GdxMeshFactory.buildVehicleRig(
                VehicleCatalog.getVehicle(localVehicleId),
                VehicleCatalog.getPaintColor(local?.colorId ?: "electric_cobalt"),
                CharacterCatalog.getCharacter(local?.characterId ?: "nico_turbo"),
                local?.appearance ?: VehicleAppearance()
            )
        }
        for (player in session.players) {
            if (player.playerId == session.localPlayerId) continue
            val (x, z, heading) = track.getStartSlot(player.slot)
            val rivalLod = if (graphicsQuality == GraphicsQuality.LOW) 2 else 1
            val glbRig = buildGlbRig(player.vehicleId, BlenderAssets.wirePilot(player.characterId), rivalLod)
            val proceduralRig = if (glbRig == null) {
                GdxMeshFactory.buildVehicleRig(
                    VehicleCatalog.getVehicle(player.vehicleId),
                    VehicleCatalog.getPaintColor(player.colorId),
                    CharacterCatalog.getCharacter(player.characterId),
                    player.appearance
                )
            } else null
            if (glbRig != null) {
                registerGlbRig(glbRig)
                glbRivals[player.playerId] = glbRig
            }
            rivals[player.playerId] = RivalCar(proceduralRig, x, z, heading)
        }
        localState = physics.createStartingState(session.localSlot)
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
        val closestRival = session.players.asSequence()
            .filter { it.connected }
            .mapNotNull { rivals[it.playerId] }
            .minByOrNull { (it.x - localState.x) * (it.x - localState.x) + (it.z - localState.z) * (it.z - localState.z) }
        val (updatedState, checkpointEvent) = physics.step(
            state = localState,
            input = input,
            dtSeconds = dt,
            canDrive = canDrive,
            opponentX = closestRival?.x,
            opponentZ = closestRival?.z
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
        updateRivals(session, dt)

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
                nextCheckpoint = localState.nextCheckpoint,
                nitroActive = localState.nitroActive,
                drifting = localState.drifting
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
        // Advance glTF animation clips first, then overwrite the wheel node pose manually.
        sceneManager?.update(dt)
        localGlbRig?.applyTransform(
            localState.x, localState.z, localState.headingRad,
            localState.steerAngleDeg, localState.wheelSpinDeg
        )
        for ((id, glbRig) in glbRivals) {
            val rival = rivals[id] ?: continue
            glbRig.applyTransform(rival.x, rival.z, rival.heading, rival.steer, rival.wheelSpin)
        }
        // 6. Render 3D scene (Deep midnight night-circuit sky)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        when (track.id) {
            CircuitId.MOUNTAIN -> Gdx.gl.glClearColor(0.20f, 0.32f, 0.42f, 1f)
            CircuitId.NIGHT_CITY -> Gdx.gl.glClearColor(0.042f, 0.021f, 0.095f, 1f)
            else -> Gdx.gl.glClearColor(0.024f, 0.039f, 0.075f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        trackWorld?.let { world ->
            modelBatch.render(world.scenery, environment)
            for (arch in world.checkpointArches) {
                modelBatch.render(arch, environment)
            }
        }
        if (localGlbRig == null) localCarRig?.let { rig ->
            modelBatch.render(rig.bodyInstance, environment)
            modelBatch.render(rig.driverInstance, environment)
            for (w in rig.wheels) {
                modelBatch.render(w.instance, environment)
            }
        }
        effectsTime = (effectsTime + dt) % 12f
        if (canDrive) {
            renderDrivingEffects(session.localSlot, localState.x, localState.z, localState.headingRad, localState.nitroActive, localState.drifting)
        }
        for (player in session.players) {
            val rival = rivals[player.playerId] ?: continue
            if (glbRivals[player.playerId] == null) rival.rig?.let { rig ->
                modelBatch.render(rig.bodyInstance, environment)
                modelBatch.render(rig.driverInstance, environment)
                for (wheel in rig.wheels) modelBatch.render(wheel.instance, environment)
            }
            val telemetry = session.remoteTelemetryByPlayerId[player.playerId] ?: continue
            if (session.phase == RoomPhase.RACING && player.connected &&
                System.currentTimeMillis() - telemetry.receivedAtMs < 300L
            ) {
                renderDrivingEffects(player.slot, rival.x, rival.z, rival.heading, telemetry.nitroActive, telemetry.drifting)
            }
        }
        modelBatch.end()
        // glTF cars/pilots use the PBR SceneManager batch; depth is shared with the track above.
        sceneManager?.render()

        // 7. Publish HUD snapshot at ~15 Hz so Compose HUD stays crisp with minimal recomposition
        hudAccumulatorSec += dt
        if (hudAccumulatorSec >= 0.066f) {
            hudAccumulatorSec = 0f
            val localProgress = track.computeRaceProgress(
                completedLaps = localState.completedLaps,
                nextCheckpoint = localState.nextCheckpoint,
                x = localState.x,
                z = localState.z
            )
            val rank = 1 + rivals.count { (playerId, rival) ->
                if (session.players.none { it.playerId == playerId && it.connected }) return@count false
                val telemetry = session.remoteTelemetryByPlayerId[playerId] ?: return@count false
                val progress = track.computeRaceProgress(
                    telemetry.completedLaps, telemetry.nextCheckpoint, rival.x, rival.z
                )
                progress > localProgress
            }
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
                    fps = Gdx.graphics.framesPerSecond,
                    nitroCharge = localState.nitroCharge,
                    nitroActive = localState.nitroActive,
                    playerCount = session.players.count { it.connected }.takeIf { it > 0 } ?: session.roomCapacity,
                    circuit = track.id,
                    markers = buildList {
                        add(TrackMarker(localState.x, localState.z, local = true))
                        for (player in session.players) {
                            val rival = rivals[player.playerId] ?: continue
                            if (player.connected) add(TrackMarker(rival.x, rival.z, local = false))
                        }
                    }
                )
            )
        }
    }

    private fun renderDrivingEffects(index: Int, x: Float, z: Float, heading: Float, nitro: Boolean, drifting: Boolean) {
        if (index !in flames.indices) return
        val forwardX = sin(heading)
        val forwardZ = cos(heading)
        if (nitro) {
            val flame = flames[index]
            flame.transform.setToTranslation(x - forwardX * 2.65f, 0.32f, z - forwardZ * 2.65f)
                .rotate(0f, 1f, 0f, Math.toDegrees(heading.toDouble()).toFloat())
                .rotate(1f, 0f, 0f, -90f)
                .scale(1f, 0.82f + 0.18f * sin(effectsTime * 35f), 1f)
            modelBatch.render(flame, environment)
        }
        if (drifting && graphicsQuality != GraphicsQuality.LOW) {
            for (particle in 0..1) {
                val puff = smoke[index * 2 + particle]
                val age = (effectsTime * 3f + particle * 0.5f) % 1f
                val offset = 2f + age * 2.4f
                val side = if (particle == 0) -0.72f else 0.72f
                val size = 0.6f + age * 0.95f
                puff.transform.setToTranslation(
                    x - forwardX * offset + forwardZ * side,
                    0.22f + age * 0.56f,
                    z - forwardZ * offset - forwardX * side
                ).scale(size, size, size)
                modelBatch.render(puff, environment)
            }
        }
    }

    private fun updateRivals(session: MultiplayerUiState, dt: Float) {
        val now = System.currentTimeMillis()
        val alpha = (1f - kotlin.math.exp(-12f * dt)).coerceIn(0.05f, 1f)
        for ((id, rival) in rivals) {
            val telemetry = session.remoteTelemetryByPlayerId[id]
            if (telemetry != null) {
                val ageSec = (now - telemetry.receivedAtMs).coerceIn(0L, 160L) / 1000f
                val targetX = telemetry.x + sin(telemetry.headingRad) * telemetry.speedMps * ageSec
                val targetZ = telemetry.z + cos(telemetry.headingRad) * telemetry.speedMps * ageSec
                rival.x += (targetX - rival.x) * alpha
                rival.z += (targetZ - rival.z) * alpha
                val angle = ArcadeCarPhysics.normalizeAngle(telemetry.headingRad - rival.heading)
                rival.heading = ArcadeCarPhysics.normalizeAngle(rival.heading + angle * alpha)
                val targetSpeed = if (now - telemetry.receivedAtMs < 300L) telemetry.speedMps else 0f
                rival.speed += (targetSpeed - rival.speed) * alpha
                rival.steer += (telemetry.steerAngleDeg - rival.steer) * alpha
                rival.wheelSpin = (rival.wheelSpin + rival.speed * dt / 0.36f * (180f / Math.PI.toFloat())) % 360f
            }
            rival.rig?.updateTransform(rival.x, rival.z, rival.heading, rival.steer, rival.wheelSpin)
        }
    }

    override fun dispose() {
        modelBatch.dispose()
        flameModel.dispose()
        smokeModel.dispose()
        trackWorld?.dispose()
        localCarRig?.dispose()
        rivals.values.forEach { it.rig?.dispose() }
        sceneManager?.dispose()
        glbAssets?.dispose()
    }
}
