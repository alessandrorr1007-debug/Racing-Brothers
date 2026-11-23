package com.example.racingbrothers.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import kotlin.math.PI

/**
 * Shared GPU asset cache for the Blender GLB pipeline.
 *
 * Meshes are immutable and shared by every car/pilot instance, which keeps memory flat at four
 * players. LOD1 is the base file (no suffix), LOD0 adds detail and LOD2 is the lightweight
 * distance variant. Loading is best-effort: when an asset is missing the caller keeps using the
 * original procedural rig, so tracks, physics and multiplayer are never affected.
 */
class GlbAssetCache : Disposable {
    private val cars = HashMap<Pair<String, Int>, SceneAsset?>()
    private val pilots = HashMap<String, SceneAsset?>()

    fun car(vehicleId: String, lod: Int): SceneAsset? {
        val key = vehicleId to lod
        if (!cars.containsKey(key)) {
            val path = BlenderAssets.cars[vehicleId]?.path(lod)
            cars[key] = if (path == null) null else load(path)
        }
        return cars[key]
    }

    fun pilot(characterId: String): SceneAsset? {
        val id = BlenderAssets.wirePilot(characterId)
        if (!pilots.containsKey(id)) {
            val file = BlenderAssets.pilots[id]
            pilots[id] = if (file == null) null else load("models/pilots/$file.glb")
        }
        return pilots[id]
    }

    private fun load(path: String): SceneAsset? = try {
        if (Gdx.files == null) null else GLBLoader().load(Gdx.files.internal(path), false)
    } catch (t: Throwable) {
        Gdx.app?.error("GlbAssetCache", "Unable to load $path: ${t.message}")
        null
    }

    override fun dispose() {
        cars.values.forEach { it?.dispose() }
        pilots.values.forEach { it?.dispose() }
        cars.clear()
        pilots.clear()
    }
}

private data class GlbWheelBinding(val node: Node, val baseRotation: Quaternion, val isFront: Boolean)

/**
 * One racing car assembled from the real GLB body + wheels plus an animated skinned pilot.
 * Wheel steering/spinning manipulates the exported glTF nodes relative to their bind pose.
 */
class GlbVehicleRig(
    carAsset: SceneAsset,
    val lod: Int,
    pilotAsset: SceneAsset?,
    cockpitY: Float,
    private val seatZ: Float
) {
    val carScene = Scene(carAsset.scene)
    val pilotScene: Scene? = pilotAsset?.let { Scene(it.scene) }
    val scenes: List<Scene> = listOfNotNull(carScene, pilotScene)

    private val pilotScale = 0.54f
    private val pilotBaseY = (cockpitY - 0.46f).coerceAtLeast(0.08f)

    private val wheelBindings: List<GlbWheelBinding> = listOf(
        "Wheel_Front_L" to true,
        "Wheel_Front_R" to true,
        "Wheel_Rear_L" to false,
        "Wheel_Rear_R" to false
    ).mapNotNull { (name, isFront) ->
        carScene.modelInstance.getNode(name)?.let { node ->
            GlbWheelBinding(node, Quaternion(node.rotation), isFront)
        }
    }

    init {
        pilotScene?.animationController?.let { controller ->
            val clip = pilotScene?.modelInstance?.animations?.firstOrNull { it.id.equals("Driving", ignoreCase = true) }
                ?: pilotScene?.modelInstance?.animations?.firstOrNull()
            clip?.let { controller.setAnimation(it.id, -1) }
        }
    }

    val hasWheels: Boolean get() = wheelBindings.size == 4

    /** Rear wheels only spin; front wheels steer and spin. Bind pose is preserved. */
    private fun wheelPose(wheel: GlbWheelBinding, steerQ: Quaternion, spinQ: Quaternion, noSteer: Quaternion): Quaternion =
        Quaternion(if (wheel.isFront) steerQ else noSteer).mul(wheel.baseRotation).mul(spinQ)

    /** Call after SceneManager.update(delta) so manual wheel pose wins over animation channels. */
    fun applyTransform(
        x: Float,
        z: Float,
        headingRad: Float,
        steerAngleDeg: Float,
        wheelSpinDeg: Float
    ) {
        val headingDeg = (headingRad * 180.0 / PI).toFloat()
        carScene.modelInstance.transform
            .idt()
            .translate(x, 0f, z)
            .rotate(Vector3.Y, headingDeg)

        val steerQ = Quaternion(Vector3.Y, steerAngleDeg * PI.toFloat() / 180f)
        val spinQ = Quaternion(Vector3.X, wheelSpinDeg * PI.toFloat() / 180f)
        val noSteer = Quaternion().idt()
        for (wheel in wheelBindings) {
            wheel.node.rotation.set(wheelPose(wheel, steerQ, spinQ, noSteer))
        }
        carScene.modelInstance.calculateTransforms()

        pilotScene?.let { pilot ->
            pilot.modelInstance.transform
                .idt()
                .translate(x, 0f, z)
                .rotate(Vector3.Y, headingDeg)
                .translate(0f, pilotBaseY, seatZ)
                .scale(pilotScale, pilotScale, pilotScale)
        }
    }
}
