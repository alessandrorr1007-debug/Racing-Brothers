package com.example.racingbrothers.vehicles

/**
 * Local-space 3D vertex shared between the libGDX race mesh builder and the Compose 3D preview.
 */
data class LocalCorner3D(val x: Float, val y: Float, val z: Float)

/**
 * Sculptable 8-corner body panel shared by the libGDX engine and the interactive 3D preview.
 * Coordinates are in vehicle-local meters: +X right, +Y up, +Z forward.
 */
data class MeshBoxPart(
    val id: String,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val width: Float,
    val height: Float,
    val length: Float,
    val role: PartColorRole,
    val pitchDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val isWheel: Boolean = false,
    val isFrontWheel: Boolean = false,
    val topWidthScale: Float = 1f,
    val topLengthScale: Float = 1f,
    val frontWidthScale: Float = 1f,
    val frontHeightScale: Float = 1f,
    val rearWidthScale: Float = 1f,
    val rearHeightScale: Float = 1f,
    val topOffsetZ: Float = 0f,
    val bottomOffsetYFront: Float = 0f,
    val bottomOffsetYRear: Float = 0f
) {
    /**
     * Returns the 8 local-space vertices in consistent order:
     * 0: (-x,-y,-z) rear-bottom-left
     * 1: (+x,-y,-z) rear-bottom-right
     * 2: (+x,+y,-z) rear-top-right
     * 3: (-x,+y,-z) rear-top-left
     * 4: (-x,-y,+z) front-bottom-left
     * 5: (+x,-y,+z) front-bottom-right
     * 6: (+x,+y,+z) front-top-right
     * 7: (-x,+y,+z) front-top-left
     */
    fun computeLocalCorners(): Array<LocalCorner3D> {
        val hw = width * 0.5f
        val hh = height * 0.5f
        val hl = length * 0.5f

        fun corner(sx: Float, sy: Float, sz: Float): LocalCorner3D {
            val endW = if (sz > 0f) frontWidthScale else rearWidthScale
            val endH = if (sz > 0f) frontHeightScale else rearHeightScale
            val wScale = endW * (if (sy > 0f) topWidthScale else 1f)
            val lScale = if (sy > 0f) topLengthScale else 1f
            val x = sx * hw * wScale
            val y = if (sy < 0f) {
                -hh + (if (sz > 0f) bottomOffsetYFront else bottomOffsetYRear)
            } else {
                -hh + height * endH
            }
            val z = sz * hl * lScale + (if (sy > 0f) topOffsetZ else 0f)
            return LocalCorner3D(x, y, z)
        }

        return arrayOf(
            corner(-1f, -1f, -1f),
            corner(1f, -1f, -1f),
            corner(1f, 1f, -1f),
            corner(-1f, 1f, -1f),
            corner(-1f, -1f, 1f),
            corner(1f, -1f, 1f),
            corner(1f, 1f, 1f),
            corner(-1f, 1f, 1f)
        )
    }
}

enum class PartColorRole {
    PRIMARY_PAINT,
    SECONDARY_TRIM,
    CARBON_FIBER,
    CHROME_METAL,
    GLASS_TINT,
    HEADLIGHT,
    TAILLIGHT,
    BRAKE_ACCENT,
    TIRE_RUBBER,
    WHEEL_RIM,
    ACCENT_GLOW
}

data class VehiclePaintColor(
    val id: String,
    val displayName: String,
    val argb: Long
)

data class VehicleAppearance(
    val rims: Int = 0,
    val spoiler: Int = 0,
    val decals: Int = 0
) {
    val rimArgb: Long
        get() = when (rims) {
            1 -> 0xFFFFC94A
            2 -> 0xFF56667D
            else -> 0xFFDCE8F8
        }

    val rimAccentArgb: Long
        get() = when (rims) {
            1 -> 0xFFFFE57F
            2 -> 0xFF00D4FF
            else -> 0xFFFF2442
        }

    fun applyTo(vehicle: VehicleSpec): List<MeshBoxPart> {
        val rearWingIds = setOf(
            "roof_spoiler", "gt_wing", "gt_wing_flap", "wing_strut_l", "wing_strut_r",
            "ducktail", "rear_wing", "rear_wing_flap", "endplate_l", "endplate_r",
            "wing_endplate_l", "wing_endplate_r", "wing_end_accent_l", "wing_end_accent_r"
        )
        return buildList {
            addAll(vehicle.parts.filterNot { spoiler != 0 && it.id in rearWingIds })
            if (spoiler == 2) {
                val rearWheelZ = vehicle.parts.filter { it.isWheel }.minOf { it.centerZ }
                val rearZ = rearWheelZ - 0.72f
                for (side in listOf(-1f, 1f)) {
                    // Swan-neck carbon aero pylons
                    add(
                        MeshBoxPart(
                            id = "sport_wing_post_$side",
                            centerX = side * 0.46f,
                            centerY = 0.90f,
                            centerZ = rearZ + 0.08f,
                            width = 0.06f,
                            height = 0.50f,
                            length = 0.22f,
                            role = PartColorRole.CARBON_FIBER,
                            pitchDeg = -14f,
                            topOffsetZ = -0.10f
                        )
                    )
                    // Sculpted GT race endplates with crimson edge trim
                    add(
                        MeshBoxPart(
                            id = "sport_wing_endplate_$side",
                            centerX = side * 1.02f,
                            centerY = 1.15f,
                            centerZ = rearZ - 0.04f,
                            width = 0.045f,
                            height = 0.28f,
                            length = 0.48f,
                            role = PartColorRole.CARBON_FIBER,
                            frontHeightScale = 0.65f
                        )
                    )
                    add(
                        MeshBoxPart(
                            id = "sport_wing_endtrim_$side",
                            centerX = side * 1.045f,
                            centerY = 1.16f,
                            centerZ = rearZ - 0.05f,
                            width = 0.02f,
                            height = 0.22f,
                            length = 0.36f,
                            role = PartColorRole.BRAKE_ACCENT,
                            frontHeightScale = 0.65f
                        )
                    )
                }
                // Main swept carbon airfoil + top Gurney lip
                add(
                    MeshBoxPart(
                        id = "sport_wing",
                        centerX = 0f,
                        centerY = 1.16f,
                        centerZ = rearZ - 0.04f,
                        width = 2.04f,
                        height = 0.065f,
                        length = 0.36f,
                        role = PartColorRole.CARBON_FIBER,
                        pitchDeg = -8f,
                        frontHeightScale = 0.55f
                    )
                )
                add(
                    MeshBoxPart(
                        id = "sport_wing_flap",
                        centerX = 0f,
                        centerY = 1.22f,
                        centerZ = rearZ - 0.16f,
                        width = 1.98f,
                        height = 0.04f,
                        length = 0.10f,
                        role = PartColorRole.PRIMARY_PAINT,
                        pitchDeg = -18f
                    )
                )
            }
            if (decals != 0) {
                val stripeRole = if (decals == 1) PartColorRole.HEADLIGHT else PartColorRole.CARBON_FIBER
                val hood = vehicle.parts.firstOrNull { it.id in setOf("hood", "long_hood", "front_nose", "nose_cone") }
                if (hood != null) {
                    val stripeSpacing = if (vehicle.id == "formula_aero") 0.10f else 0.18f
                    val stripeW = if (vehicle.id == "formula_aero") 0.075f else 0.145f
                    for (side in listOf(-1f, 1f)) {
                        add(
                            MeshBoxPart(
                                id = "stripe_hood_$side",
                                centerX = side * stripeSpacing,
                                centerY = hood.centerY + hood.height * 0.5f * ((1f + hood.frontHeightScale) * 0.5f) + 0.015f,
                                centerZ = hood.centerZ + hood.topOffsetZ * 0.45f,
                                width = stripeW,
                                height = 0.018f,
                                length = hood.length * hood.topLengthScale * 0.90f,
                                role = stripeRole,
                                pitchDeg = hood.pitchDeg + (1f - hood.frontHeightScale) * 10f
                            )
                        )
                    }
                }
                val roof = vehicle.parts.firstOrNull { it.id == "roof" }
                if (roof != null) {
                    for (side in listOf(-1f, 1f)) {
                        add(
                            MeshBoxPart(
                                id = "stripe_roof_$side",
                                centerX = side * 0.17f,
                                centerY = roof.centerY + roof.height * 0.5f + 0.014f,
                                centerZ = roof.centerZ,
                                width = 0.135f,
                                height = 0.014f,
                                length = roof.length * roof.topLengthScale * 0.92f,
                                role = stripeRole
                            )
                        )
                    }
                }
                val deck = vehicle.parts.firstOrNull { it.id in setOf("rear_deck", "engine_deck") }
                if (deck != null) {
                    for (side in listOf(-1f, 1f)) {
                        add(
                            MeshBoxPart(
                                id = "stripe_deck_$side",
                                centerX = side * 0.17f,
                                centerY = deck.centerY + deck.height * 0.5f + 0.014f,
                                centerZ = deck.centerZ,
                                width = 0.135f,
                                height = 0.014f,
                                length = deck.length * 0.84f,
                                role = stripeRole,
                                pitchDeg = deck.pitchDeg
                            )
                        )
                    }
                }
            }
        }
    }
}

data class VehicleSpec(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String,
    val cockpitY: Float,
    val cockpitZ: Float,
    val parts: List<MeshBoxPart>,
    val neonWheelTread: Boolean = false,
    val neonAccentArgb: Long = 0xFF00D4FF
)

/**
 * Balanced V1 performance profile shared across all four vehicles to guarantee fair races.
 */
object BalancedV1Stats {
    const val MAX_SPEED_MPS = 56f          // ~201 km/h arcade display scale
    const val MAX_REVERSE_MPS = 14f
    const val ACCELERATION_MPS2 = 24f
    const val BRAKE_DECEL_MPS2 = 36f
    const val COAST_DRAG_MPS2 = 9.5f
    const val STEER_RATE_RAD_PER_SEC = 2.25f
}

object VehicleCatalog {
    val paintColors: List<VehiclePaintColor> = listOf(
        VehiclePaintColor("electric_cobalt", "Azul Eléctrico", 0xFF0E74FF),
        VehiclePaintColor("crimson_ignite", "Rojo Ignición", 0xFFEB1C3F),
        VehiclePaintColor("titanium_frost", "Titanio Polar", 0xFFE4ECF7),
        VehiclePaintColor("solar_amber", "Ámbar Solar", 0xFFFFB800),
        VehiclePaintColor("venom_emerald", "Verde Neón", 0xFF00E676),
        VehiclePaintColor("carbon_stealth", "Grafito Carbono", 0xFF3E5472)
    )

    val vehicles: List<VehicleSpec> = listOf(
        // 1. Vortex RS — Widebody GT3 track coupe inspired by the blue race GT reference
        VehicleSpec(
            id = "vortex_rs",
            displayName = "Vortex RS",
            category = "Deportivo GT",
            description = "Coupé GT3 de carrocería ancha con splitter de carbono, tomas en capó, escape lateral y alerón swan-neck.",
            cockpitY = 0.72f,
            cockpitZ = -0.18f,
            neonWheelTread = false,
            neonAccentArgb = 0xFF00D4FF,
            parts = listOf(
                // Sculpted core tub & cinched sports waist
                MeshBoxPart("chassis", 0f, 0.36f, 0.02f, 1.96f, 0.36f, 4.26f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.94f, frontWidthScale = 0.96f, rearWidthScale = 0.98f, frontHeightScale = 0.78f),
                MeshBoxPart("upper_belt", 0f, 0.60f, -0.06f, 1.88f, 0.24f, 3.88f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.88f, topLengthScale = 0.90f, frontHeightScale = 0.68f),
                // Long sculpted sloping GT3 hood + recessed central extractor + side louvers
                MeshBoxPart("hood", 0f, 0.64f, 1.24f, 1.66f, 0.22f, 1.60f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.86f, frontWidthScale = 0.88f, frontHeightScale = 0.38f, topOffsetZ = -0.10f),
                MeshBoxPart("hood_vent_center", 0f, 0.71f, 1.38f, 0.66f, 0.055f, 0.42f, PartColorRole.CARBON_FIBER, pitchDeg = 10f, topWidthScale = 0.86f),
                MeshBoxPart("hood_louver_l", -0.56f, 0.75f, 0.96f, 0.28f, 0.045f, 0.48f, PartColorRole.CARBON_FIBER, pitchDeg = 8f, yawDeg = -6f),
                MeshBoxPart("hood_louver_r", 0.56f, 0.75f, 0.96f, 0.28f, 0.045f, 0.48f, PartColorRole.CARBON_FIBER, pitchDeg = 8f, yawDeg = 6f),
                // Muscular flared widebody front & rear fenders
                MeshBoxPart("fender_fl", -0.95f, 0.52f, 1.24f, 0.30f, 0.36f, 1.22f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.72f, topLengthScale = 0.82f, frontHeightScale = 0.66f),
                MeshBoxPart("fender_fr", 0.95f, 0.52f, 1.24f, 0.30f, 0.36f, 1.22f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.72f, topLengthScale = 0.82f, frontHeightScale = 0.66f),
                MeshBoxPart("fender_rl", -0.99f, 0.56f, -1.24f, 0.34f, 0.40f, 1.38f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.72f, topLengthScale = 0.84f, rearHeightScale = 0.84f),
                MeshBoxPart("fender_rr", 0.99f, 0.56f, -1.24f, 0.34f, 0.40f, 1.38f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.72f, topLengthScale = 0.84f, rearHeightScale = 0.84f),
                // Swept aerodynamic fastback glass greenhouse & low coupe roof + telemetry antenna
                MeshBoxPart("cabin", 0f, 0.94f, -0.22f, 1.56f, 0.44f, 2.08f, PartColorRole.GLASS_TINT, topWidthScale = 0.70f, topLengthScale = 0.58f, topOffsetZ = -0.24f),
                MeshBoxPart("roof", 0f, 1.18f, -0.44f, 1.10f, 0.065f, 1.14f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.90f, topLengthScale = 0.90f),
                MeshBoxPart("roof_antenna", 0f, 1.32f, -0.32f, 0.025f, 0.22f, 0.025f, PartColorRole.CHROME_METAL),
                MeshBoxPart("rear_deck", 0f, 0.76f, -1.58f, 1.66f, 0.16f, 0.94f, PartColorRole.PRIMARY_PAINT, pitchDeg = -5f, topWidthScale = 0.90f, rearWidthScale = 0.92f),
                // Carbon race mirrors
                MeshBoxPart("mirror_l", -0.92f, 0.84f, 0.38f, 0.24f, 0.09f, 0.16f, PartColorRole.CARBON_FIBER, yawDeg = 10f),
                MeshBoxPart("mirror_r", 0.92f, 0.84f, 0.38f, 0.24f, 0.09f, 0.16f, PartColorRole.CARBON_FIBER, yawDeg = -10f),
                // Aggressive front GT3 intake grille, wide carbon splitter, diagonal chrome splitter rods & red tow hooks
                MeshBoxPart("front_grille", 0f, 0.32f, 2.13f, 1.64f, 0.24f, 0.10f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.92f),
                MeshBoxPart("front_bumper", 0f, 0.15f, 2.16f, 2.08f, 0.075f, 0.40f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.96f),
                MeshBoxPart("canard_l", -1.04f, 0.28f, 1.98f, 0.12f, 0.045f, 0.34f, PartColorRole.CARBON_FIBER, pitchDeg = 14f, rollDeg = -18f),
                MeshBoxPart("canard_r", 1.04f, 0.28f, 1.98f, 0.12f, 0.045f, 0.34f, PartColorRole.CARBON_FIBER, pitchDeg = 14f, rollDeg = 18f),
                MeshBoxPart("splitter_rod_l", -0.44f, 0.26f, 2.26f, 0.025f, 0.22f, 0.025f, PartColorRole.CHROME_METAL, pitchDeg = -22f, rollDeg = 12f),
                MeshBoxPart("splitter_rod_r", 0.44f, 0.26f, 2.26f, 0.025f, 0.22f, 0.025f, PartColorRole.CHROME_METAL, pitchDeg = -22f, rollDeg = -12f),
                MeshBoxPart("tow_hook_l", -0.64f, 0.28f, 2.20f, 0.06f, 0.11f, 0.06f, PartColorRole.BRAKE_ACCENT),
                MeshBoxPart("tow_hook_r", 0.64f, 0.28f, 2.20f, 0.06f, 0.11f, 0.06f, PartColorRole.BRAKE_ACCENT),
                // Carbon side skirts, side-exit metallic GT3 exhausts & cyan underglow strip
                MeshBoxPart("side_skirt_l", -1.02f, 0.16f, -0.02f, 0.16f, 0.08f, 2.16f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("side_skirt_r", 1.02f, 0.16f, -0.02f, 0.16f, 0.08f, 2.16f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("side_exhaust_l", -1.07f, 0.23f, 0.42f, 0.12f, 0.09f, 0.14f, PartColorRole.CHROME_METAL, yawDeg = -18f),
                MeshBoxPart("side_exhaust_r", 1.07f, 0.23f, 0.42f, 0.12f, 0.09f, 0.14f, PartColorRole.CHROME_METAL, yawDeg = 18f),
                MeshBoxPart("side_glow_l", -1.05f, 0.13f, -0.02f, 0.04f, 0.035f, 2.02f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("side_glow_r", 1.05f, 0.13f, -0.02f, 0.04f, 0.035f, 2.02f, PartColorRole.ACCENT_GLOW),
                // Sculpted swan-neck GT3 rear wing with endplates
                MeshBoxPart("wing_strut_l", -0.46f, 0.96f, -1.86f, 0.055f, 0.38f, 0.20f, PartColorRole.CARBON_FIBER, pitchDeg = -14f, topOffsetZ = -0.10f),
                MeshBoxPart("wing_strut_r", 0.46f, 0.96f, -1.86f, 0.055f, 0.38f, 0.20f, PartColorRole.CARBON_FIBER, pitchDeg = -14f, topOffsetZ = -0.10f),
                MeshBoxPart("roof_spoiler", 0f, 1.15f, -1.96f, 1.94f, 0.065f, 0.34f, PartColorRole.PRIMARY_PAINT, pitchDeg = -7f, frontHeightScale = 0.55f),
                MeshBoxPart("wing_endplate_l", -0.98f, 1.13f, -1.96f, 0.045f, 0.25f, 0.42f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.62f),
                MeshBoxPart("wing_endplate_r", 0.98f, 1.13f, -1.96f, 0.045f, 0.25f, 0.42f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.62f),
                MeshBoxPart("wing_end_accent_l", -1.005f, 1.14f, -1.97f, 0.02f, 0.18f, 0.30f, PartColorRole.BRAKE_ACCENT, frontHeightScale = 0.62f),
                MeshBoxPart("wing_end_accent_r", 1.005f, 1.14f, -1.97f, 0.02f, 0.18f, 0.30f, PartColorRole.BRAKE_ACCENT, frontHeightScale = 0.62f),
                // Multi-fin carbon rear diffuser + twin rear exhaust tips
                MeshBoxPart("rear_diffuser", 0f, 0.20f, -2.10f, 1.88f, 0.22f, 0.22f, PartColorRole.CARBON_FIBER, bottomOffsetYRear = 0.06f),
                MeshBoxPart("exhaust_l", -0.54f, 0.22f, -2.20f, 0.24f, 0.09f, 0.10f, PartColorRole.CHROME_METAL),
                MeshBoxPart("exhaust_r", 0.54f, 0.22f, -2.20f, 0.24f, 0.09f, 0.10f, PartColorRole.CHROME_METAL),
                // Razor-sharp swept LED headlights (Reference 1) + twin crimson blade taillights
                MeshBoxPart("headlight_l", -0.68f, 0.49f, 2.06f, 0.50f, 0.095f, 0.16f, PartColorRole.HEADLIGHT, yawDeg = -18f, pitchDeg = 6f),
                MeshBoxPart("headlight_r", 0.68f, 0.49f, 2.06f, 0.50f, 0.095f, 0.16f, PartColorRole.HEADLIGHT, yawDeg = 18f, pitchDeg = 6f),
                MeshBoxPart("drl_l", -0.74f, 0.43f, 2.11f, 0.44f, 0.04f, 0.10f, PartColorRole.ACCENT_GLOW, yawDeg = -18f),
                MeshBoxPart("drl_r", 0.74f, 0.43f, 2.11f, 0.44f, 0.04f, 0.10f, PartColorRole.ACCENT_GLOW, yawDeg = 18f),
                MeshBoxPart("taillight_l", -0.64f, 0.58f, -2.14f, 0.54f, 0.11f, 0.08f, PartColorRole.TAILLIGHT, yawDeg = 8f),
                MeshBoxPart("taillight_r", 0.64f, 0.58f, -2.14f, 0.54f, 0.11f, 0.08f, PartColorRole.TAILLIGHT, yawDeg = -8f),
                MeshBoxPart("taillight_center", 0f, 0.58f, -2.16f, 0.74f, 0.045f, 0.06f, PartColorRole.TAILLIGHT),
                // 4 prominent deep-dish GT3 racing wheels
                MeshBoxPart("wheel_fl", -1.00f, 0.36f, 1.26f, 0.34f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.00f, 0.36f, 1.26f, 0.34f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.03f, 0.375f, -1.26f, 0.38f, 0.75f, 0.75f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.03f, 0.375f, -1.26f, 0.38f, 0.75f, 0.75f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        ),

        // 2. Apex GT-X — Ultra-low mid-engine cyber-hypercar with wedge nose, C-blade intakes & flying buttresses
        VehicleSpec(
            id = "apex_gtx",
            displayName = "Apex GT-X",
            category = "Superdeportivo",
            description = "Hiperdeportivo de motor central con morro en cuña, tomas C-blade neón, cubierta de motor y alerón activo.",
            cockpitY = 0.62f,
            cockpitZ = 0.16f,
            neonWheelTread = false,
            neonAccentArgb = 0xFF00E5FF,
            parts = listOf(
                // Ultra-low sculpted carbon-underbody hypercar tub
                MeshBoxPart("chassis", 0f, 0.32f, 0f, 2.12f, 0.32f, 4.52f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.90f, frontWidthScale = 0.92f, frontHeightScale = 0.68f),
                MeshBoxPart("front_nose", 0f, 0.38f, 1.72f, 1.88f, 0.28f, 1.38f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.84f, frontWidthScale = 0.80f, frontHeightScale = 0.34f, topOffsetZ = -0.14f),
                MeshBoxPart("front_hood_duct", 0f, 0.44f, 1.78f, 0.78f, 0.05f, 0.56f, PartColorRole.CARBON_FIBER, pitchDeg = 14f, frontWidthScale = 0.78f),
                // Sculpted high-crest front & rear hypercar haunches
                MeshBoxPart("fender_fl", -0.98f, 0.48f, 1.34f, 0.32f, 0.30f, 1.24f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.68f, topLengthScale = 0.80f, frontHeightScale = 0.55f),
                MeshBoxPart("fender_fr", 0.98f, 0.48f, 1.34f, 0.32f, 0.30f, 1.24f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.68f, topLengthScale = 0.80f, frontHeightScale = 0.55f),
                MeshBoxPart("fender_rl", -1.03f, 0.54f, -1.28f, 0.36f, 0.38f, 1.54f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.70f, topLengthScale = 0.84f, rearHeightScale = 0.86f),
                MeshBoxPart("fender_rr", 1.03f, 0.54f, -1.28f, 0.36f, 0.38f, 1.54f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.70f, topLengthScale = 0.84f, rearHeightScale = 0.86f),
                // Forward-pushed fighter-jet teardrop canopy + dorsal roof scoop
                MeshBoxPart("cabin", 0f, 0.76f, 0.16f, 1.44f, 0.42f, 1.88f, PartColorRole.GLASS_TINT, topWidthScale = 0.64f, topLengthScale = 0.52f, topOffsetZ = -0.18f),
                MeshBoxPart("roof", 0f, 0.98f, 0.04f, 0.96f, 0.06f, 0.92f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.88f, topLengthScale = 0.86f),
                MeshBoxPart("roof_scoop", 0f, 1.06f, -0.36f, 0.32f, 0.11f, 0.66f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.80f, rearHeightScale = 0.45f),
                // Flying aero buttresses + louvered mid-engine deck with glowing power core strips
                MeshBoxPart("buttress_l", -0.64f, 0.76f, -1.08f, 0.14f, 0.26f, 1.32f, PartColorRole.PRIMARY_PAINT, frontHeightScale = 1.0f, rearHeightScale = 0.36f),
                MeshBoxPart("buttress_r", 0.64f, 0.76f, -1.08f, 0.14f, 0.26f, 1.32f, PartColorRole.PRIMARY_PAINT, frontHeightScale = 1.0f, rearHeightScale = 0.36f),
                MeshBoxPart("engine_deck", 0f, 0.60f, -1.16f, 1.24f, 0.18f, 1.56f, PartColorRole.CARBON_FIBER, topWidthScale = 0.86f, rearHeightScale = 0.72f),
                MeshBoxPart("engine_core_l", -0.22f, 0.70f, -1.12f, 0.08f, 0.03f, 1.10f, PartColorRole.ACCENT_GLOW, pitchDeg = -4f),
                MeshBoxPart("engine_core_r", 0.22f, 0.70f, -1.12f, 0.08f, 0.03f, 1.10f, PartColorRole.ACCENT_GLOW, pitchDeg = -4f),
                // Sculpted C-blade side intakes with glowing cyan aero trim
                MeshBoxPart("intake_l", -1.06f, 0.44f, -0.22f, 0.24f, 0.32f, 1.32f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.70f, topWidthScale = 0.84f),
                MeshBoxPart("intake_r", 1.06f, 0.44f, -0.22f, 0.24f, 0.32f, 1.32f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.70f, topWidthScale = 0.84f),
                MeshBoxPart("intake_glow_l", -1.12f, 0.46f, 0.24f, 0.04f, 0.24f, 0.10f, PartColorRole.ACCENT_GLOW, pitchDeg = -18f),
                MeshBoxPart("intake_glow_r", 1.12f, 0.46f, 0.24f, 0.04f, 0.24f, 0.10f, PartColorRole.ACCENT_GLOW, pitchDeg = -18f),
                MeshBoxPart("mirror_l", -0.94f, 0.70f, 0.66f, 0.24f, 0.075f, 0.14f, PartColorRole.CARBON_FIBER, yawDeg = 14f),
                MeshBoxPart("mirror_r", 0.94f, 0.70f, 0.66f, 0.24f, 0.075f, 0.14f, PartColorRole.CARBON_FIBER, yawDeg = -14f),
                // Active dual-element hypercar GT wing
                MeshBoxPart("wing_strut_l", -0.60f, 0.86f, -1.96f, 0.065f, 0.40f, 0.22f, PartColorRole.CARBON_FIBER, pitchDeg = -12f),
                MeshBoxPart("wing_strut_r", 0.60f, 0.86f, -1.96f, 0.065f, 0.40f, 0.22f, PartColorRole.CARBON_FIBER, pitchDeg = -12f),
                MeshBoxPart("gt_wing", 0f, 1.06f, -2.04f, 2.14f, 0.065f, 0.40f, PartColorRole.PRIMARY_PAINT, pitchDeg = -7f, frontHeightScale = 0.55f),
                MeshBoxPart("gt_wing_flap", 0f, 1.12f, -2.18f, 2.08f, 0.04f, 0.11f, PartColorRole.CARBON_FIBER, pitchDeg = -16f),
                MeshBoxPart("wing_endplate_l", -1.07f, 1.04f, -2.04f, 0.05f, 0.24f, 0.48f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.58f),
                MeshBoxPart("wing_endplate_r", 1.07f, 1.04f, -2.04f, 0.05f, 0.24f, 0.48f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.58f),
                // Razor front carbon splitter with neon laser lip & sculpted multi-strake rear diffuser
                MeshBoxPart("front_splitter", 0f, 0.13f, 2.30f, 2.16f, 0.07f, 0.42f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.92f),
                MeshBoxPart("splitter_glow", 0f, 0.11f, 2.45f, 1.96f, 0.035f, 0.07f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("rear_diffuser", 0f, 0.18f, -2.22f, 2.00f, 0.22f, 0.26f, PartColorRole.CARBON_FIBER, bottomOffsetYRear = 0.07f),
                MeshBoxPart("exhaust_center", 0f, 0.34f, -2.30f, 0.62f, 0.12f, 0.12f, PartColorRole.CHROME_METAL),
                // Cybernetic swept LED headlights & full-width taillight lightbar
                MeshBoxPart("headlight_l", -0.76f, 0.40f, 2.22f, 0.52f, 0.08f, 0.18f, PartColorRole.HEADLIGHT, yawDeg = -22f, pitchDeg = 10f),
                MeshBoxPart("headlight_r", 0.76f, 0.40f, 2.22f, 0.52f, 0.08f, 0.18f, PartColorRole.HEADLIGHT, yawDeg = 22f, pitchDeg = 10f),
                MeshBoxPart("drl_l", -0.80f, 0.32f, 2.26f, 0.38f, 0.035f, 0.10f, PartColorRole.ACCENT_GLOW, yawDeg = -22f),
                MeshBoxPart("drl_r", 0.80f, 0.32f, 2.26f, 0.38f, 0.035f, 0.10f, PartColorRole.ACCENT_GLOW, yawDeg = 22f),
                MeshBoxPart("taillight_bar", 0f, 0.52f, -2.26f, 1.96f, 0.085f, 0.08f, PartColorRole.TAILLIGHT),
                MeshBoxPart("taillight_fin_l", -0.92f, 0.44f, -2.26f, 0.07f, 0.20f, 0.08f, PartColorRole.TAILLIGHT),
                MeshBoxPart("taillight_fin_r", 0.92f, 0.44f, -2.26f, 0.07f, 0.20f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 ultra-wide staggered hypercar wheels
                MeshBoxPart("wheel_fl", -1.07f, 0.36f, 1.38f, 0.36f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.07f, 0.36f, 1.38f, 0.36f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.10f, 0.38f, -1.36f, 0.42f, 0.76f, 0.76f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.10f, 0.38f, -1.36f, 0.42f, 0.76f, 0.76f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        ),

        // 3. Titan V8 — Cyberpunk widebody muscle beast with towering supercharger, side pipes & staggered drag slicks
        VehicleSpec(
            id = "titan_v8",
            displayName = "Titan V8",
            category = "Muscle Car",
            description = "Muscle widebody con compresor V8 cromado, escapes laterales, persianas traseras y llantas drag anchas.",
            cockpitY = 0.74f,
            cockpitZ = -0.36f,
            neonWheelTread = false,
            neonAccentArgb = 0xFF00D4FF,
            parts = listOf(
                // Broad muscular chiseled lower body
                MeshBoxPart("chassis", 0f, 0.44f, 0f, 2.06f, 0.46f, 4.60f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.92f, topLengthScale = 0.96f),
                MeshBoxPart("long_hood", 0f, 0.74f, 1.16f, 1.84f, 0.20f, 2.00f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.90f, frontHeightScale = 0.68f, frontWidthScale = 0.94f),
                MeshBoxPart("hood_bulge", 0f, 0.85f, 1.14f, 0.92f, 0.08f, 1.42f, PartColorRole.CARBON_FIBER, topWidthScale = 0.80f, frontHeightScale = 0.72f),
                // Exposed chrome V8 supercharger blower + triple crimson intake butterflies
                MeshBoxPart("v8_blower", 0f, 0.98f, 1.16f, 0.66f, 0.24f, 0.82f, PartColorRole.CHROME_METAL, topWidthScale = 0.92f),
                MeshBoxPart("v8_scoop", 0f, 1.10f, 1.24f, 0.60f, 0.10f, 0.62f, PartColorRole.CARBON_FIBER, frontHeightScale = 1.15f, rearHeightScale = 0.65f),
                MeshBoxPart("blower_intake", 0f, 1.07f, 1.56f, 0.54f, 0.13f, 0.08f, PartColorRole.BRAKE_ACCENT),
                // Chopped fastback cabin + rear window louvers
                MeshBoxPart("cabin", 0f, 1.02f, -0.44f, 1.64f, 0.46f, 1.72f, PartColorRole.GLASS_TINT, topWidthScale = 0.76f, topLengthScale = 0.68f, topOffsetZ = -0.14f),
                MeshBoxPart("roof", 0f, 1.26f, -0.54f, 1.24f, 0.075f, 1.16f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.92f),
                MeshBoxPart("rear_louvers", 0f, 0.96f, -1.26f, 1.32f, 0.14f, 0.58f, PartColorRole.CARBON_FIBER, pitchDeg = -28f, topWidthScale = 0.88f),
                MeshBoxPart("rear_deck", 0f, 0.77f, -1.72f, 1.86f, 0.16f, 0.96f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.94f),
                // Bolted widebody muscular front & rear haunches
                MeshBoxPart("fender_fl", -0.99f, 0.56f, 1.40f, 0.30f, 0.34f, 1.28f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.74f, frontHeightScale = 0.82f),
                MeshBoxPart("fender_fr", 0.99f, 0.56f, 1.40f, 0.30f, 0.34f, 1.28f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.74f, frontHeightScale = 0.82f),
                MeshBoxPart("rear_fender_l", -1.03f, 0.64f, -1.26f, 0.34f, 0.36f, 1.70f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.74f, topLengthScale = 0.88f),
                MeshBoxPart("rear_fender_r", 1.03f, 0.64f, -1.26f, 0.34f, 0.36f, 1.70f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.74f, topLengthScale = 0.88f),
                MeshBoxPart("mirror_l", -0.96f, 0.88f, 0.16f, 0.22f, 0.09f, 0.15f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("mirror_r", 0.96f, 0.88f, 0.16f, 0.22f, 0.09f, 0.15f, PartColorRole.CARBON_FIBER),
                // Aggressive kicked-up carbon ducktail spoiler + rear diffuser
                MeshBoxPart("ducktail", 0f, 0.88f, -2.20f, 1.98f, 0.20f, 0.28f, PartColorRole.CARBON_FIBER, pitchDeg = -24f),
                MeshBoxPart("rear_diffuser", 0f, 0.22f, -2.26f, 1.90f, 0.20f, 0.20f, PartColorRole.CARBON_FIBER),
                // Deep recessed muscle front grille + chin splitter with crimson lip
                MeshBoxPart("front_grille", 0f, 0.48f, 2.30f, 1.90f, 0.28f, 0.10f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("front_chin", 0f, 0.18f, 2.30f, 2.12f, 0.09f, 0.32f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("chin_accent", 0f, 0.15f, 2.43f, 1.96f, 0.04f, 0.07f, PartColorRole.BRAKE_ACCENT),
                // Chunky chrome side-pipe exhausts + cyan under-rocker neon line
                MeshBoxPart("side_exhaust_l", -1.08f, 0.20f, -0.06f, 0.12f, 0.10f, 1.62f, PartColorRole.CHROME_METAL),
                MeshBoxPart("side_exhaust_r", 1.08f, 0.20f, -0.06f, 0.12f, 0.10f, 1.62f, PartColorRole.CHROME_METAL),
                MeshBoxPart("side_glow_l", -1.04f, 0.14f, -0.06f, 0.04f, 0.035f, 1.60f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("side_glow_r", 1.04f, 0.14f, -0.06f, 0.04f, 0.035f, 1.60f, PartColorRole.ACCENT_GLOW),
                // Aggressive quad halo LED headlights + classic triple-bar crimson muscle taillights
                MeshBoxPart("headlight_l", -0.70f, 0.50f, 2.34f, 0.44f, 0.14f, 0.08f, PartColorRole.HEADLIGHT, yawDeg = -8f),
                MeshBoxPart("headlight_r", 0.70f, 0.50f, 2.34f, 0.44f, 0.14f, 0.08f, PartColorRole.HEADLIGHT, yawDeg = 8f),
                MeshBoxPart("brow_glow", 0f, 0.60f, 2.32f, 1.68f, 0.035f, 0.06f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("taillight_l", -0.66f, 0.55f, -2.32f, 0.58f, 0.16f, 0.08f, PartColorRole.TAILLIGHT),
                MeshBoxPart("taillight_r", 0.66f, 0.55f, -2.32f, 0.58f, 0.16f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 staggered meaty muscle drag wheels
                MeshBoxPart("wheel_fl", -1.06f, 0.37f, 1.40f, 0.36f, 0.74f, 0.74f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.06f, 0.37f, 1.40f, 0.36f, 0.74f, 0.74f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.09f, 0.41f, -1.34f, 0.46f, 0.82f, 0.82f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.09f, 0.41f, -1.34f, 0.46f, 0.82f, 0.82f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        ),

        // 4. Formula Aero — Futuristic neon open-wheel formula racer inspired directly by Reference Image 2
        VehicleSpec(
            id = "formula_aero",
            displayName = "Formula Aero",
            category = "Prototipo de Competición",
            description = "Monoplaza futurista con suspensión visible, cabina aerodinámica, exo-estructura trasera y neumáticos neón.",
            cockpitY = 0.58f,
            cockpitZ = -0.10f,
            neonWheelTread = true,
            neonAccentArgb = 0xFFFF9100,
            parts = listOf(
                // Slender central survival monocoque & sharply tapered formula nose cone
                MeshBoxPart("monocoque", 0f, 0.38f, 0.05f, 0.92f, 0.36f, 3.60f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.80f, frontWidthScale = 0.68f, rearWidthScale = 0.84f),
                MeshBoxPart("nose_cone", 0f, 0.34f, 1.88f, 0.62f, 0.24f, 1.38f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.72f, frontWidthScale = 0.34f, frontHeightScale = 0.44f, bottomOffsetYFront = 0.04f),
                // Exposed glass front suspension bay + crimson pushrod dampers (Reference 2 signature!)
                MeshBoxPart("front_bay_glass", 0f, 0.43f, 1.42f, 0.34f, 0.07f, 0.76f, PartColorRole.GLASS_TINT, pitchDeg = 6f),
                MeshBoxPart("damper_l", -0.10f, 0.44f, 1.42f, 0.05f, 0.05f, 0.54f, PartColorRole.BRAKE_ACCENT, pitchDeg = 6f, yawDeg = 6f),
                MeshBoxPart("damper_r", 0.10f, 0.44f, 1.42f, 0.05f, 0.05f, 0.54f, PartColorRole.BRAKE_ACCENT, pitchDeg = 6f, yawDeg = -6f),
                MeshBoxPart("nose_emblem_glow", 0f, 0.36f, 2.28f, 0.18f, 0.04f, 0.22f, PartColorRole.ACCENT_GLOW, pitchDeg = 8f),
                // Multi-element sculpted carbon front wing + chrome winglets + raised outer endplates
                MeshBoxPart("front_wing", 0f, 0.15f, 2.40f, 2.24f, 0.07f, 0.48f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.86f),
                MeshBoxPart("front_flap_l", -0.78f, 0.23f, 2.34f, 0.62f, 0.045f, 0.26f, PartColorRole.CHROME_METAL, pitchDeg = -14f, yawDeg = 10f),
                MeshBoxPart("front_flap_r", 0.78f, 0.23f, 2.34f, 0.62f, 0.045f, 0.26f, PartColorRole.CHROME_METAL, pitchDeg = -14f, yawDeg = -10f),
                MeshBoxPart("front_wing_end_l", -1.12f, 0.26f, 2.38f, 0.055f, 0.24f, 0.52f, PartColorRole.PRIMARY_PAINT, frontHeightScale = 0.55f),
                MeshBoxPart("front_wing_end_r", 1.12f, 0.26f, 2.38f, 0.055f, 0.24f, 0.52f, PartColorRole.PRIMARY_PAINT, frontHeightScale = 0.55f),
                MeshBoxPart("front_wing_glow", 0f, 0.19f, 2.54f, 1.98f, 0.035f, 0.06f, PartColorRole.ACCENT_GLOW),
                // Exposed angled double-wishbone front & rear suspension arms + crimson coilover struts
                MeshBoxPart("wishbone_fl_top", -0.68f, 0.42f, 1.48f, 0.76f, 0.045f, 0.10f, PartColorRole.CARBON_FIBER, yawDeg = 14f),
                MeshBoxPart("wishbone_fl_bot", -0.68f, 0.26f, 1.48f, 0.76f, 0.045f, 0.10f, PartColorRole.CARBON_FIBER, yawDeg = -10f),
                MeshBoxPart("wishbone_fr_top", 0.68f, 0.42f, 1.48f, 0.76f, 0.045f, 0.10f, PartColorRole.CARBON_FIBER, yawDeg = -14f),
                MeshBoxPart("wishbone_fr_bot", 0.68f, 0.26f, 1.48f, 0.76f, 0.045f, 0.10f, PartColorRole.CARBON_FIBER, yawDeg = 10f),
                MeshBoxPart("shock_fl", -0.52f, 0.38f, 1.38f, 0.06f, 0.22f, 0.06f, PartColorRole.BRAKE_ACCENT, rollDeg = 42f),
                MeshBoxPart("shock_fr", 0.52f, 0.38f, 1.38f, 0.06f, 0.22f, 0.06f, PartColorRole.BRAKE_ACCENT, rollDeg = -42f),
                MeshBoxPart("wishbone_rl", -0.74f, 0.38f, -1.42f, 0.76f, 0.055f, 0.12f, PartColorRole.CHROME_METAL, yawDeg = -12f),
                MeshBoxPart("wishbone_rr", 0.74f, 0.38f, -1.42f, 0.76f, 0.055f, 0.12f, PartColorRole.CHROME_METAL, yawDeg = 12f),
                // Sculpted undercut aerodynamic sidepods + carbon ground-effect floor
                MeshBoxPart("sidepod_l", -0.76f, 0.40f, -0.34f, 0.66f, 0.34f, 2.02f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.84f, frontWidthScale = 0.68f, rearWidthScale = 0.76f, bottomOffsetYFront = 0.08f),
                MeshBoxPart("sidepod_r", 0.76f, 0.40f, -0.34f, 0.66f, 0.34f, 2.02f, PartColorRole.PRIMARY_PAINT, topWidthScale = 0.84f, frontWidthScale = 0.68f, rearWidthScale = 0.76f, bottomOffsetYFront = 0.08f),
                MeshBoxPart("sidepod_intake_l", -0.74f, 0.38f, 0.68f, 0.42f, 0.22f, 0.16f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("sidepod_intake_r", 0.74f, 0.38f, 0.68f, 0.42f, 0.22f, 0.16f, PartColorRole.CARBON_FIBER),
                MeshBoxPart("floor_plank", 0f, 0.14f, -0.30f, 2.08f, 0.055f, 2.74f, PartColorRole.CARBON_FIBER, frontWidthScale = 0.88f),
                // Sleek closed-canopy smoked glass cockpit + glowing halo accent
                MeshBoxPart("cockpit_rim", 0f, 0.62f, -0.02f, 0.78f, 0.26f, 1.48f, PartColorRole.GLASS_TINT, topWidthScale = 0.62f, topLengthScale = 0.62f, topOffsetZ = -0.14f),
                MeshBoxPart("cockpit_glow", 0f, 0.56f, 0.70f, 0.22f, 0.045f, 0.20f, PartColorRole.ACCENT_GLOW),
                // Architectural metallic titanium/chrome Y-brace rear exo-frame (Reference 2 signature!)
                MeshBoxPart("airbox", 0f, 0.82f, -0.82f, 0.36f, 0.24f, 0.56f, PartColorRole.CHROME_METAL, topWidthScale = 0.66f, frontHeightScale = 0.90f, rearHeightScale = 0.55f),
                MeshBoxPart("airbox_glow", 0f, 0.82f, -0.52f, 0.16f, 0.14f, 0.06f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("exo_brace_l", -0.52f, 0.72f, -1.44f, 0.12f, 0.09f, 1.06f, PartColorRole.CHROME_METAL, yawDeg = -22f, pitchDeg = -8f),
                MeshBoxPart("exo_brace_r", 0.52f, 0.72f, -1.44f, 0.12f, 0.09f, 1.06f, PartColorRole.CHROME_METAL, yawDeg = 22f, pitchDeg = -8f),
                MeshBoxPart("shark_fin", 0f, 0.76f, -1.42f, 0.07f, 0.42f, 1.24f, PartColorRole.CARBON_FIBER, rearHeightScale = 0.55f),
                // Wide low-slung futuristic rear wing + glowing endplate trim + FIA rain light
                MeshBoxPart("rear_wing", 0f, 0.94f, -2.06f, 2.08f, 0.09f, 0.46f, PartColorRole.CARBON_FIBER, pitchDeg = -7f, frontHeightScale = 0.58f),
                MeshBoxPart("endplate_l", -1.04f, 0.76f, -2.04f, 0.065f, 0.48f, 0.56f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.72f),
                MeshBoxPart("endplate_r", 1.04f, 0.76f, -2.04f, 0.065f, 0.48f, 0.56f, PartColorRole.CARBON_FIBER, frontHeightScale = 0.72f),
                MeshBoxPart("wing_end_accent_l", -1.07f, 0.78f, -2.04f, 0.025f, 0.36f, 0.38f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("wing_end_accent_r", 1.07f, 0.78f, -2.04f, 0.025f, 0.36f, 0.38f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("headlight_l", -0.74f, 0.48f, 0.70f, 0.34f, 0.075f, 0.10f, PartColorRole.HEADLIGHT, yawDeg = -12f),
                MeshBoxPart("headlight_r", 0.74f, 0.48f, 0.70f, 0.34f, 0.075f, 0.10f, PartColorRole.HEADLIGHT, yawDeg = 12f),
                MeshBoxPart("rain_light", 0f, 0.38f, -2.18f, 0.20f, 0.22f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 exposed wide racing slicks with glowing neon tread rings!
                MeshBoxPart("wheel_fl", -1.08f, 0.37f, 1.50f, 0.42f, 0.74f, 0.74f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.08f, 0.37f, 1.50f, 0.42f, 0.74f, 0.74f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.14f, 0.40f, -1.44f, 0.48f, 0.80f, 0.80f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.14f, 0.40f, -1.44f, 0.48f, 0.80f, 0.80f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        )
    )

    fun getVehicle(id: String): VehicleSpec = vehicles.firstOrNull { it.id == id } ?: vehicles.first()
    fun getPaintColor(id: String): VehiclePaintColor = paintColors.firstOrNull { it.id == id } ?: paintColors.first()
}
