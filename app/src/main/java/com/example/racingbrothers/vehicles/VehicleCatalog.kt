package com.example.racingbrothers.vehicles

/**
 * 3D box primitive shared between the libGDX 3D engine and the interactive 3D Compose preview.
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
    val isWheel: Boolean = false,
    val isFrontWheel: Boolean = false
)

enum class PartColorRole {
    PRIMARY_PAINT,
    SECONDARY_TRIM,
    GLASS_TINT,
    HEADLIGHT,
    TAILLIGHT,
    TIRE_RUBBER,
    WHEEL_RIM,
    ACCENT_GLOW
}

data class VehiclePaintColor(
    val id: String,
    val displayName: String,
    val argb: Long
)

data class VehicleSpec(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String,
    val cockpitY: Float,
    val cockpitZ: Float,
    val parts: List<MeshBoxPart>
)

/**
 * Balanced V1 performance profile shared across all four vehicles to guarantee fair 1 vs 1 races.
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
        VehiclePaintColor("electric_cobalt", "Azul Eléctrico", 0xFF00B4FF),
        VehiclePaintColor("crimson_ignite", "Rojo Ignición", 0xFFFF2E4C),
        VehiclePaintColor("titanium_frost", "Titanio Polar", 0xFFDCE6F2),
        VehiclePaintColor("solar_amber", "Ámbar Solar", 0xFFFFB300),
        VehiclePaintColor("venom_emerald", "Verde Neón", 0xFF00E676),
        VehiclePaintColor("carbon_stealth", "Grafito Carbono", 0xFF3E526E)
    )

    val vehicles: List<VehicleSpec> = listOf(
        // 1. Deportivo compacto (Hot-hatch sport coupe, short wheelbase, roof wing)
        VehicleSpec(
            id = "vortex_rs",
            displayName = "Vortex RS",
            category = "Deportivo compacto",
            description = "Chasis compacto de ataque urbano con alerón de techo y líneas ágiles.",
            cockpitY = 0.72f,
            cockpitZ = -0.10f,
            parts = listOf(
                MeshBoxPart("chassis", 0f, 0.44f, 0f, 1.88f, 0.48f, 3.85f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("hood", 0f, 0.66f, 1.05f, 1.72f, 0.22f, 1.35f, PartColorRole.PRIMARY_PAINT, pitchDeg = 6f),
                MeshBoxPart("cabin", 0f, 1.02f, -0.25f, 1.56f, 0.54f, 1.85f, PartColorRole.GLASS_TINT),
                MeshBoxPart("roof", 0f, 1.30f, -0.32f, 1.50f, 0.08f, 1.65f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("roof_spoiler", 0f, 1.36f, -1.28f, 1.64f, 0.10f, 0.38f, PartColorRole.SECONDARY_TRIM, pitchDeg = -10f),
                MeshBoxPart("front_bumper", 0f, 0.24f, 1.92f, 1.92f, 0.20f, 0.24f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("rear_diffuser", 0f, 0.24f, -1.92f, 1.84f, 0.22f, 0.20f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("side_glow_l", -0.96f, 0.22f, 0f, 0.06f, 0.07f, 2.50f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("side_glow_r", 0.96f, 0.22f, 0f, 0.06f, 0.07f, 2.50f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("headlight_l", -0.62f, 0.52f, 1.94f, 0.42f, 0.14f, 0.08f, PartColorRole.HEADLIGHT),
                MeshBoxPart("headlight_r", 0.62f, 0.52f, 1.94f, 0.42f, 0.14f, 0.08f, PartColorRole.HEADLIGHT),
                MeshBoxPart("taillight_l", -0.62f, 0.58f, -1.94f, 0.48f, 0.14f, 0.08f, PartColorRole.TAILLIGHT),
                MeshBoxPart("taillight_r", 0.62f, 0.58f, -1.94f, 0.48f, 0.14f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 wheels
                MeshBoxPart("wheel_fl", -0.96f, 0.34f, 1.18f, 0.30f, 0.68f, 0.68f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 0.96f, 0.34f, 1.18f, 0.30f, 0.68f, 0.68f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -0.96f, 0.34f, -1.18f, 0.32f, 0.68f, 0.68f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 0.96f, 0.34f, -1.18f, 0.32f, 0.68f, 0.68f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        ),
        // 2. Superdeportivo (Low wide wedge silhouette, sculpted rear deck, GT wing)
        VehicleSpec(
            id = "apex_gtx",
            displayName = "Apex GT-X",
            category = "Superdeportivo",
            description = "Perfil ultrabajo de motor central con difusor activo y alerón GT.",
            cockpitY = 0.62f,
            cockpitZ = 0.15f,
            parts = listOf(
                MeshBoxPart("chassis", 0f, 0.36f, 0f, 2.12f, 0.38f, 4.45f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("front_nose", 0f, 0.32f, 1.85f, 1.96f, 0.24f, 1.05f, PartColorRole.PRIMARY_PAINT, pitchDeg = 10f),
                MeshBoxPart("cabin", 0f, 0.80f, 0.10f, 1.48f, 0.42f, 1.65f, PartColorRole.GLASS_TINT),
                MeshBoxPart("engine_deck", 0f, 0.64f, -1.15f, 1.88f, 0.26f, 1.55f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("intake_l", -1.04f, 0.48f, -0.35f, 0.24f, 0.30f, 1.30f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("intake_r", 1.04f, 0.48f, -0.35f, 0.24f, 0.30f, 1.30f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("wing_strut_l", -0.68f, 0.88f, -1.96f, 0.08f, 0.36f, 0.26f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("wing_strut_r", 0.68f, 0.88f, -1.96f, 0.08f, 0.36f, 0.26f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("gt_wing", 0f, 1.08f, -2.02f, 2.08f, 0.08f, 0.42f, PartColorRole.PRIMARY_PAINT, pitchDeg = -6f),
                MeshBoxPart("front_splitter", 0f, 0.16f, 2.30f, 2.10f, 0.08f, 0.34f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("headlight_l", -0.72f, 0.42f, 2.26f, 0.48f, 0.10f, 0.12f, PartColorRole.HEADLIGHT),
                MeshBoxPart("headlight_r", 0.72f, 0.42f, 2.26f, 0.48f, 0.10f, 0.12f, PartColorRole.HEADLIGHT),
                MeshBoxPart("taillight_bar", 0f, 0.50f, -2.23f, 1.80f, 0.10f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 wide wheels
                MeshBoxPart("wheel_fl", -1.06f, 0.34f, 1.36f, 0.34f, 0.68f, 0.68f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.06f, 0.34f, 1.36f, 0.34f, 0.68f, 0.68f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.08f, 0.36f, -1.34f, 0.38f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.08f, 0.36f, -1.34f, 0.38f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        ),
        // 3. Muscle car (Long bold hood, V8 supercharger scoop, muscular rear haunches)
        VehicleSpec(
            id = "titan_v8",
            displayName = "Titan V8",
            category = "Muscle car",
            description = "Capó largo con toma de aire V8 sobrealimentada y postura ancha clásica.",
            cockpitY = 0.72f,
            cockpitZ = -0.35f,
            parts = listOf(
                MeshBoxPart("chassis", 0f, 0.46f, 0f, 2.04f, 0.52f, 4.55f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("long_hood", 0f, 0.76f, 1.12f, 1.88f, 0.18f, 1.90f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("v8_blower", 0f, 0.94f, 1.18f, 0.64f, 0.24f, 0.78f, PartColorRole.WHEEL_RIM),
                MeshBoxPart("blower_intake", 0f, 0.98f, 1.58f, 0.56f, 0.14f, 0.10f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("cabin", 0f, 1.04f, -0.45f, 1.62f, 0.50f, 1.58f, PartColorRole.GLASS_TINT),
                MeshBoxPart("roof", 0f, 1.30f, -0.48f, 1.56f, 0.08f, 1.30f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("rear_fender_l", -0.96f, 0.64f, -1.25f, 0.26f, 0.24f, 1.60f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("rear_fender_r", 0.96f, 0.64f, -1.25f, 0.26f, 0.24f, 1.60f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("ducktail", 0f, 0.80f, -2.18f, 1.96f, 0.18f, 0.22f, PartColorRole.SECONDARY_TRIM, pitchDeg = -20f),
                MeshBoxPart("front_grille", 0f, 0.44f, 2.28f, 1.84f, 0.30f, 0.08f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("headlight_l", -0.68f, 0.50f, 2.30f, 0.38f, 0.18f, 0.08f, PartColorRole.HEADLIGHT),
                MeshBoxPart("headlight_r", 0.68f, 0.50f, 2.30f, 0.38f, 0.18f, 0.08f, PartColorRole.HEADLIGHT),
                MeshBoxPart("taillight_l", -0.68f, 0.54f, -2.29f, 0.52f, 0.18f, 0.08f, PartColorRole.TAILLIGHT),
                MeshBoxPart("taillight_r", 0.68f, 0.54f, -2.29f, 0.52f, 0.18f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 meaty wheels
                MeshBoxPart("wheel_fl", -1.04f, 0.36f, 1.38f, 0.34f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.04f, 0.36f, 1.38f, 0.34f, 0.72f, 0.72f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.06f, 0.40f, -1.32f, 0.42f, 0.80f, 0.80f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.06f, 0.40f, -1.32f, 0.42f, 0.80f, 0.80f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        ),
        // 4. Deportivo de competición (Prototype / Formula Aero racer with front wing, shark fin, open canopy)
        VehicleSpec(
            id = "formula_aero",
            displayName = "Formula Aero",
            category = "Deportivo de competición",
            description = "Prototipo de pista con alerón delantero, aleta dorsal y aerodinámica extrema.",
            cockpitY = 0.60f,
            cockpitZ = -0.10f,
            parts = listOf(
                MeshBoxPart("monocoque", 0f, 0.38f, 0.15f, 1.18f, 0.40f, 4.35f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("nose_cone", 0f, 0.32f, 2.05f, 0.72f, 0.24f, 0.95f, PartColorRole.PRIMARY_PAINT, pitchDeg = 8f),
                MeshBoxPart("front_wing", 0f, 0.18f, 2.38f, 2.22f, 0.10f, 0.46f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("front_wing_glow", 0f, 0.24f, 2.48f, 2.18f, 0.05f, 0.10f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("sidepod_l", -0.84f, 0.38f, -0.35f, 0.56f, 0.36f, 2.05f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("sidepod_r", 0.84f, 0.38f, -0.35f, 0.56f, 0.36f, 2.05f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("cockpit_rim", 0f, 0.64f, -0.05f, 0.96f, 0.22f, 1.25f, PartColorRole.GLASS_TINT),
                MeshBoxPart("shark_fin", 0f, 0.88f, -1.22f, 0.10f, 0.58f, 1.55f, PartColorRole.SECONDARY_TRIM),
                MeshBoxPart("rear_wing", 0f, 1.12f, -2.06f, 2.14f, 0.12f, 0.48f, PartColorRole.PRIMARY_PAINT),
                MeshBoxPart("endplate_l", -1.08f, 0.94f, -2.04f, 0.08f, 0.48f, 0.56f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("endplate_r", 1.08f, 0.94f, -2.04f, 0.08f, 0.48f, 0.56f, PartColorRole.ACCENT_GLOW),
                MeshBoxPart("headlight_l", -0.84f, 0.44f, 0.70f, 0.36f, 0.10f, 0.10f, PartColorRole.HEADLIGHT),
                MeshBoxPart("headlight_r", 0.84f, 0.44f, 0.70f, 0.36f, 0.10f, 0.10f, PartColorRole.HEADLIGHT),
                MeshBoxPart("rain_light", 0f, 0.42f, -2.18f, 0.22f, 0.20f, 0.08f, PartColorRole.TAILLIGHT),
                // 4 exposed racing slicks
                MeshBoxPart("wheel_fl", -1.04f, 0.35f, 1.48f, 0.38f, 0.70f, 0.70f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_fr", 1.04f, 0.35f, 1.48f, 0.38f, 0.70f, 0.70f, PartColorRole.TIRE_RUBBER, isWheel = true, isFrontWheel = true),
                MeshBoxPart("wheel_rl", -1.10f, 0.38f, -1.42f, 0.44f, 0.76f, 0.76f, PartColorRole.TIRE_RUBBER, isWheel = true),
                MeshBoxPart("wheel_rr", 1.10f, 0.38f, -1.42f, 0.44f, 0.76f, 0.76f, PartColorRole.TIRE_RUBBER, isWheel = true)
            )
        )
    )

    fun getVehicle(id: String): VehicleSpec = vehicles.firstOrNull { it.id == id } ?: vehicles.first()
    fun getPaintColor(id: String): VehiclePaintColor = paintColors.firstOrNull { it.id == id } ?: paintColors.first()
}
