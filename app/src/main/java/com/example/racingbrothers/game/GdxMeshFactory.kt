package com.example.racingbrothers.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.example.racingbrothers.characters.CharacterSpec
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.tracks.PropKind
import com.example.racingbrothers.vehicles.MeshBoxPart
import com.example.racingbrothers.vehicles.PartColorRole
import com.example.racingbrothers.vehicles.VehiclePaintColor
import com.example.racingbrothers.vehicles.VehicleSpec
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

data class GdxWheelRig(
    val instance: ModelInstance,
    val offsetX: Float,
    val offsetY: Float,
    val offsetZ: Float,
    val isFront: Boolean
)

data class GdxVehicleRig(
    val bodyInstance: ModelInstance,
    val driverInstance: ModelInstance,
    val wheels: List<GdxWheelRig>,
    val cockpitY: Float,
    val cockpitZ: Float,
    val disposables: List<Model>
) {
    fun updateTransform(
        x: Float,
        z: Float,
        headingRad: Float,
        steerAngleDeg: Float,
        wheelSpinDeg: Float
    ) {
        val headingDeg = (headingRad * 180.0 / PI).toFloat()
        bodyInstance.transform
            .idt()
            .translate(x, 0f, z)
            .rotate(0f, 1f, 0f, headingDeg)

        driverInstance.transform
            .idt()
            .translate(x, 0f, z)
            .rotate(0f, 1f, 0f, headingDeg)
            .translate(0f, cockpitY, cockpitZ)
            .scale(0.62f, 0.62f, 0.62f)

        for (wheel in wheels) {
            val steer = if (wheel.isFront) steerAngleDeg else 0f
            wheel.instance.transform
                .idt()
                .translate(x, 0f, z)
                .rotate(0f, 1f, 0f, headingDeg)
                .translate(wheel.offsetX, wheel.offsetY, wheel.offsetZ)
                .rotate(0f, 1f, 0f, steer)
                .rotate(1f, 0f, 0f, wheelSpinDeg)
                .rotate(0f, 0f, 1f, 90f)
        }
    }

    fun dispose() {
        disposables.forEach { it.dispose() }
    }
}

data class GdxTrackWorld(
    val staticInstances: List<ModelInstance>,
    val checkpointArches: List<ModelInstance>,
    val disposables: List<Model>
) {
    fun dispose() {
        disposables.forEach { it.dispose() }
    }
}

/**
 * Procedural libGDX 3D mesh factory.
 * Builds original low-poly vehicles, cartoon 3D drivers, and the Neon Bay racing circuit
 * using libGDX ModelBuilder with zero external runtime downloads.
 */
object GdxMeshFactory {
    private const val ATTRS: Long =
        (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

    fun argbToGdxColor(argb: Long): Color {
        val a = ((argb shr 24) and 0xFF) / 255f
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return Color(r, g, b, a)
    }

    private fun partColor(role: PartColorRole, paint: VehiclePaintColor): Color = when (role) {
        PartColorRole.PRIMARY_PAINT -> argbToGdxColor(paint.argb)
        PartColorRole.SECONDARY_TRIM -> Color(0.11f, 0.15f, 0.22f, 1f)
        PartColorRole.GLASS_TINT -> Color(0.16f, 0.27f, 0.42f, 1f)
        PartColorRole.HEADLIGHT -> Color(0.88f, 0.96f, 1.0f, 1f)
        PartColorRole.TAILLIGHT -> Color(1.0f, 0.18f, 0.26f, 1f)
        PartColorRole.TIRE_RUBBER -> Color(0.08f, 0.09f, 0.11f, 1f)
        PartColorRole.WHEEL_RIM -> Color(0.76f, 0.82f, 0.90f, 1f)
        PartColorRole.ACCENT_GLOW -> Color(0.0f, 0.72f, 1.0f, 1f)
    }

    fun buildVehicleRig(
        vehicle: VehicleSpec,
        paint: VehiclePaintColor,
        character: CharacterSpec
    ): GdxVehicleRig {
        val builder = ModelBuilder()
        val disposables = mutableListOf<Model>()

        // 1. Combined chassis/body model
        builder.begin()
        val bodyParts = vehicle.parts.filter { !it.isWheel }
        for (part in bodyParts) {
            val mat = Material(ColorAttribute.createDiffuse(partColor(part.role, paint)))
            val node = builder.node()
            node.id = part.id
            node.translation.set(part.centerX, part.centerY, part.centerZ)
            if (part.pitchDeg != 0f) {
                node.rotation.setEulerAngles(0f, part.pitchDeg, 0f)
            }
            val meshPart = builder.part(part.id, GL20.GL_TRIANGLES, ATTRS, mat)
            meshPart.box(part.width, part.height, part.length)
        }
        val bodyModel = builder.end()
        disposables.add(bodyModel)

        // 2. Cartoon 3D driver model (upper torso + helmet + visor + accessories visible in cockpit)
        builder.begin()
        val cockpitParts = character.parts.filter { it.centerY >= 0.75f }
        for (part in cockpitParts) {
            val mat = Material(ColorAttribute.createDiffuse(argbToGdxColor(part.argb)))
            val node = builder.node()
            node.id = "drv_${part.id}"
            node.translation.set(part.centerX, part.centerY - 0.85f, part.centerZ)
            val meshPart = builder.part("drv_${part.id}", GL20.GL_TRIANGLES, ATTRS, mat)
            meshPart.box(part.width, part.height, part.length)
        }
        val driverModel = builder.end()
        disposables.add(driverModel)

        // 3. Shared wheel model (tire + metallic rim hub)
        val wheelSpecs = vehicle.parts.filter { it.isWheel }
        val wheelRigs = mutableListOf<GdxWheelRig>()
        for (w in wheelSpecs) {
            builder.begin()
            val tireMat = Material(ColorAttribute.createDiffuse(Color(0.08f, 0.09f, 0.11f, 1f)))
            val rimMat = Material(ColorAttribute.createDiffuse(Color(0.74f, 0.82f, 0.92f, 1f)))
            val tireNode = builder.node()
            tireNode.id = "${w.id}_tire"
            val tirePart = builder.part("${w.id}_tire", GL20.GL_TRIANGLES, ATTRS, tireMat)
            tirePart.cylinder(w.height, w.width, w.length, 12)

            val rimNode = builder.node()
            rimNode.id = "${w.id}_rim"
            val rimPart = builder.part("${w.id}_rim", GL20.GL_TRIANGLES, ATTRS, rimMat)
            rimPart.cylinder(w.height * 0.56f, w.width * 1.06f, w.length * 0.56f, 10)

            val wheelModel = builder.end()
            disposables.add(wheelModel)
            wheelRigs.add(
                GdxWheelRig(
                    instance = ModelInstance(wheelModel),
                    offsetX = w.centerX,
                    offsetY = w.centerY,
                    offsetZ = w.centerZ,
                    isFront = w.isFrontWheel
                )
            )
        }

        return GdxVehicleRig(
            bodyInstance = ModelInstance(bodyModel),
            driverInstance = ModelInstance(driverModel),
            wheels = wheelRigs,
            cockpitY = vehicle.cockpitY,
            cockpitZ = vehicle.cockpitZ,
            disposables = disposables
        )
    }

    fun buildTrackWorld(quality: GraphicsQuality): GdxTrackWorld {
        val builder = ModelBuilder()
        val disposables = mutableListOf<Model>()
        val staticInstances = mutableListOf<ModelInstance>()

        // 1. Ground terrain apron
        val groundModel = builder.createBox(
            700f, 0.4f, 700f,
            Material(ColorAttribute.createDiffuse(Color(0.035f, 0.055f, 0.095f, 1f))),
            ATTRS
        )
        disposables.add(groundModel)
        val groundInst = ModelInstance(groundModel)
        groundInst.transform.setToTranslation(85f, -0.35f, 25f)
        staticInstances.add(groundInst)

        // 2. Closed asphalt loop + curbs + guardrails batched into shared models for high FPS
        val samples = CircuitTrack.samples
        val n = samples.size
        val roadHalfW = CircuitTrack.ROAD_HALF_WIDTH
        val barrierHalfW = CircuitTrack.BARRIER_HALF_WIDTH

        val asphaltMat = Material(ColorAttribute.createDiffuse(Color(0.11f, 0.13f, 0.17f, 1f)))
        val centerDashMat = Material(ColorAttribute.createDiffuse(Color(0.75f, 0.85f, 0.98f, 1f)))
        val curbRedMat = Material(ColorAttribute.createDiffuse(Color(0.9f, 0.12f, 0.18f, 1f)))
        val curbWhiteMat = Material(ColorAttribute.createDiffuse(Color(0.90f, 0.94f, 0.99f, 1f)))
        val barrierMat = Material(ColorAttribute.createDiffuse(Color(0.20f, 0.28f, 0.40f, 1f)))
        val neonRailMat = Material(ColorAttribute.createDiffuse(Color(0.0f, 0.78f, 1.0f, 1f)))

        // Build road segments in chunks of 33 to stay well within 16-bit vertex index limits
        val chunkSize = 33
        var chunkStart = 0
        while (chunkStart < n) {
            val chunkEnd = (chunkStart + chunkSize).coerceAtMost(n)
            builder.begin()
            for (i in chunkStart until chunkEnd) {
                val a = samples[i]
                val b = samples[(i + 1) % n]
                val midX = (a.x + b.x) * 0.5f
                val midZ = (a.z + b.z) * 0.5f
                val segLen = hypot(b.x - a.x, b.z - a.z) + 0.85f
                val yawDeg = (atan2(b.x - a.x, b.z - a.z) * 180.0 / PI).toFloat()

                // Asphalt deck
                val roadNode = builder.node()
                roadNode.id = "road_$i"
                roadNode.translation.set(midX, -0.06f, midZ)
                roadNode.rotation.setEulerAngles(yawDeg, 0f, 0f)
                builder.part("road_$i", GL20.GL_TRIANGLES, ATTRS, asphaltMat)
                    .box(roadHalfW * 2f, 0.12f, segLen)

                // Center dashed lane marker
                if (i % 2 == 0) {
                    val dashNode = builder.node()
                    dashNode.id = "dash_$i"
                    dashNode.translation.set(midX, 0.02f, midZ)
                    dashNode.rotation.setEulerAngles(yawDeg, 0f, 0f)
                    builder.part("dash_$i", GL20.GL_TRIANGLES, ATTRS, centerDashMat)
                        .box(0.32f, 0.03f, segLen * 0.52f)
                }

                // Alternating red and white rumble curbs
                val curbMat = if (i % 2 == 0) curbRedMat else curbWhiteMat
                val curbOffset = roadHalfW + 0.75f
                for (side in listOf(-1f, 1f)) {
                    val cNode = builder.node()
                    cNode.id = "curb_${i}_$side"
                    cNode.translation.set(
                        midX + a.rightX * curbOffset * side,
                        0.03f,
                        midZ + a.rightZ * curbOffset * side
                    )
                    cNode.rotation.setEulerAngles(yawDeg, 0f, 0f)
                    builder.part("curb_${i}_$side", GL20.GL_TRIANGLES, ATTRS, curbMat)
                        .box(1.4f, 0.12f, segLen)

                    // Guardrail wall
                    val bNode = builder.node()
                    bNode.id = "wall_${i}_$side"
                    bNode.translation.set(
                        midX + a.rightX * barrierHalfW * side,
                        0.68f,
                        midZ + a.rightZ * barrierHalfW * side
                    )
                    bNode.rotation.setEulerAngles(yawDeg, 0f, 0f)
                    builder.part("wall_${i}_$side", GL20.GL_TRIANGLES, ATTRS, barrierMat)
                        .box(0.55f, 1.25f, segLen)

                    // Top glowing neon rail on high quality or every 2nd segment on low quality
                    if (quality == GraphicsQuality.HIGH || i % 2 == 0) {
                        val rNode = builder.node()
                        rNode.id = "rail_${i}_$side"
                        rNode.translation.set(
                            midX + a.rightX * (barrierHalfW - 0.1f) * side,
                            1.36f,
                            midZ + a.rightZ * (barrierHalfW - 0.1f) * side
                        )
                        rNode.rotation.setEulerAngles(yawDeg, 0f, 0f)
                        builder.part("rail_${i}_$side", GL20.GL_TRIANGLES, ATTRS, neonRailMat)
                            .box(0.22f, 0.16f, segLen)
                    }
                }
            }
            val chunkModel = builder.end()
            disposables.add(chunkModel)
            staticInstances.add(ModelInstance(chunkModel))
            chunkStart = chunkEnd
        }

        // 3. Start / Finish checkered line & overhead LED Gantry Arch
        val start = samples.first()
        val startYaw = (atan2(start.dirX, start.dirZ) * 180.0 / PI).toFloat()
        builder.begin()
        val checkBlack = Material(ColorAttribute.createDiffuse(Color(0.05f, 0.06f, 0.09f, 1f)))
        val checkWhite = Material(ColorAttribute.createDiffuse(Color(0.95f, 0.97f, 1.0f, 1f)))
        val pillarMat = Material(ColorAttribute.createDiffuse(Color(0.18f, 0.24f, 0.34f, 1f)))
        val gantryGlow = Material(ColorAttribute.createDiffuse(Color(0.0f, 0.78f, 1.0f, 1f)))

        // Checkered tiles across the start/finish line
        val tiles = 10
        val tileW = (roadHalfW * 2f) / tiles
        for (t in 0 until tiles) {
            val offset = -roadHalfW + tileW * (t + 0.5f)
            val mat = if (t % 2 == 0) checkWhite else checkBlack
            val n1 = builder.node()
            n1.id = "chk_a_$t"
            n1.translation.set(offset, 0.04f, 0.8f)
            builder.part("chk_a_$t", GL20.GL_TRIANGLES, ATTRS, mat).box(tileW, 0.04f, 1.5f)

            val mat2 = if (t % 2 == 0) checkBlack else checkWhite
            val n2 = builder.node()
            n2.id = "chk_b_$t"
            n2.translation.set(offset, 0.04f, -0.8f)
            builder.part("chk_b_$t", GL20.GL_TRIANGLES, ATTRS, mat2).box(tileW, 0.04f, 1.5f)
        }
        // Overhead Start/Finish Gantry Arch
        val pL = builder.node()
        pL.id = "gantry_l"
        pL.translation.set(-barrierHalfW - 0.5f, 4.5f, 0f)
        builder.part("gantry_l", GL20.GL_TRIANGLES, ATTRS, pillarMat).box(1.1f, 9f, 1.4f)

        val pR = builder.node()
        pR.id = "gantry_r"
        pR.translation.set(barrierHalfW + 0.5f, 4.5f, 0f)
        builder.part("gantry_r", GL20.GL_TRIANGLES, ATTRS, pillarMat).box(1.1f, 9f, 1.4f)

        val beam = builder.node()
        beam.id = "gantry_beam"
        beam.translation.set(0f, 8.8f, 0f)
        builder.part("gantry_beam", GL20.GL_TRIANGLES, ATTRS, pillarMat).box(barrierHalfW * 2f + 2.2f, 1.6f, 1.8f)

        val led = builder.node()
        led.id = "gantry_led"
        led.translation.set(0f, 8.2f, 0f)
        builder.part("gantry_led", GL20.GL_TRIANGLES, ATTRS, gantryGlow).box(barrierHalfW * 2f, 0.4f, 2.0f)

        val startGantryModel = builder.end()
        disposables.add(startGantryModel)
        val startGantryInst = ModelInstance(startGantryModel)
        startGantryInst.transform
            .idt()
            .translate(start.x, 0f, start.z)
            .rotate(0f, 1f, 0f, startYaw)
        staticInstances.add(startGantryInst)

        // 4. Checkpoint subtle holo-arches (checkpoints 1..5)
        val checkpointArches = mutableListOf<ModelInstance>()
        builder.begin()
        val cpMat = Material(ColorAttribute.createDiffuse(Color(0.0f, 0.72f, 1.0f, 1f)))
        val cpL = builder.node()
        cpL.id = "cp_l"
        cpL.translation.set(-barrierHalfW, 3.2f, 0f)
        builder.part("cp_l", GL20.GL_TRIANGLES, ATTRS, cpMat).box(0.45f, 6.4f, 0.45f)

        val cpR = builder.node()
        cpR.id = "cp_r"
        cpR.translation.set(barrierHalfW, 3.2f, 0f)
        builder.part("cp_r", GL20.GL_TRIANGLES, ATTRS, cpMat).box(0.45f, 6.4f, 0.45f)

        val cpTop = builder.node()
        cpTop.id = "cp_top"
        cpTop.translation.set(0f, 6.3f, 0f)
        builder.part("cp_top", GL20.GL_TRIANGLES, ATTRS, cpMat).box(barrierHalfW * 2f, 0.35f, 0.35f)
        val cpModel = builder.end()
        disposables.add(cpModel)

        for (cp in CircuitTrack.checkpoints) {
            if (cp.index == 0) continue
            val inst = ModelInstance(cpModel)
            val cpYaw = (atan2(cp.dirX, cp.dirZ) * 180.0 / PI).toFloat()
            inst.transform.idt().translate(cp.x, 0f, cp.z).rotate(0f, 1f, 0f, cpYaw)
            checkpointArches.add(inst)
        }

        // 5. Scenery props (Floodlight towers, Grandstands, Chevron turn signs, Stylized trees)
        val towerModel = buildFloodlightTowerModel(builder).also { disposables.add(it) }
        val standModel = buildGrandstandModel(builder).also { disposables.add(it) }
        val signModel = buildChevronSignModel(builder).also { disposables.add(it) }
        val treeModel = buildTreeModel(builder).also { disposables.add(it) }

        for (prop in CircuitTrack.sceneryProps) {
            if (quality == GraphicsQuality.LOW && prop.kind == PropKind.TREE_CLUSTER) continue
            val model = when (prop.kind) {
                PropKind.FLOODLIGHT_TOWER -> towerModel
                PropKind.GRANDSTAND -> standModel
                PropKind.CHEVRON_SIGN -> signModel
                PropKind.TREE_CLUSTER -> treeModel
            }
            val inst = ModelInstance(model)
            inst.transform.idt().translate(prop.x, 0f, prop.z).rotate(0f, 1f, 0f, prop.yawDeg)
            staticInstances.add(inst)
        }

        val mountainModel = builder.createCone(
            1f, 1f, 1f, 6,
            Material(ColorAttribute.createDiffuse(Color(0.11f, 0.17f, 0.23f, 1f))),
            ATTRS
        ).also { disposables.add(it) }
        for (i in samples.indices step 12) {
            val point = samples[i]
            val offset = barrierHalfW + 65f
            val height = 16f + (i * 7 % 17)
            val mountain = ModelInstance(mountainModel)
            mountain.transform
                .setToTranslation(point.x + point.rightX * offset, height / 2f, point.z + point.rightZ * offset)
                .rotate(0f, 1f, 0f, i * 19f)
                .scale(42f, height, 36f)
            staticInstances.add(mountain)
        }

        return GdxTrackWorld(
            staticInstances = staticInstances,
            checkpointArches = checkpointArches,
            disposables = disposables
        )
    }

    private fun buildFloodlightTowerModel(builder: ModelBuilder): Model {
        builder.begin()
        val steel = Material(ColorAttribute.createDiffuse(Color(0.22f, 0.28f, 0.38f, 1f)))
        val lamp = Material(ColorAttribute.createDiffuse(Color(0.86f, 0.96f, 1.0f, 1f)))
        val pole = builder.node()
        pole.id = "pole"
        pole.translation.set(0f, 8.5f, 0f)
        builder.part("pole", GL20.GL_TRIANGLES, ATTRS, steel).box(0.75f, 17f, 0.75f)
        val head = builder.node()
        head.id = "lamp_head"
        head.translation.set(0f, 17.2f, 0f)
        builder.part("lamp_head", GL20.GL_TRIANGLES, ATTRS, lamp).box(4.2f, 1.2f, 1.4f)
        return builder.end()
    }

    private fun buildGrandstandModel(builder: ModelBuilder): Model {
        builder.begin()
        val base = Material(ColorAttribute.createDiffuse(Color(0.14f, 0.20f, 0.30f, 1f)))
        val canopy = Material(ColorAttribute.createDiffuse(Color(0.0f, 0.65f, 0.95f, 1f)))
        val tiers = listOf(
            Triple(0f, 1.2f, 6.5f),
            Triple(1.8f, 2.8f, 4.5f),
            Triple(3.4f, 4.4f, 2.8f)
        )
        tiers.forEachIndexed { idx, (offX, y, w) ->
            val n = builder.node()
            n.id = "tier_$idx"
            n.translation.set(offX, y, 0f)
            builder.part("tier_$idx", GL20.GL_TRIANGLES, ATTRS, base).box(w, 1.8f, 24f)
        }
        val roof = builder.node()
        roof.id = "roof"
        roof.translation.set(1.5f, 8.2f, 0f)
        builder.part("roof", GL20.GL_TRIANGLES, ATTRS, canopy).box(7.8f, 0.45f, 25f)
        return builder.end()
    }

    private fun buildChevronSignModel(builder: ModelBuilder): Model {
        builder.begin()
        val post = Material(ColorAttribute.createDiffuse(Color(0.25f, 0.32f, 0.44f, 1f)))
        val board = Material(ColorAttribute.createDiffuse(Color(0.0f, 0.78f, 1.0f, 1f)))
        val p = builder.node()
        p.id = "post"
        p.translation.set(0f, 1.3f, 0f)
        builder.part("post", GL20.GL_TRIANGLES, ATTRS, post).box(0.25f, 2.6f, 0.25f)
        val b = builder.node()
        b.id = "board"
        b.translation.set(0f, 2.5f, 0f)
        builder.part("board", GL20.GL_TRIANGLES, ATTRS, board).box(0.2f, 1.2f, 2.6f)
        return builder.end()
    }

    private fun buildTreeModel(builder: ModelBuilder): Model {
        builder.begin()
        val trunkMat = Material(ColorAttribute.createDiffuse(Color(0.24f, 0.17f, 0.12f, 1f)))
        val leafMat = Material(ColorAttribute.createDiffuse(Color(0.08f, 0.30f, 0.28f, 1f)))
        val trunk = builder.node()
        trunk.id = "trunk"
        trunk.translation.set(0f, 1.6f, 0f)
        builder.part("trunk", GL20.GL_TRIANGLES, ATTRS, trunkMat).box(0.65f, 3.2f, 0.65f)
        val crown = builder.node()
        crown.id = "crown"
        crown.translation.set(0f, 4.6f, 0f)
        builder.part("crown", GL20.GL_TRIANGLES, ATTRS, leafMat).cone(3.6f, 5.2f, 3.6f, 7)
        return builder.end()
    }
}
