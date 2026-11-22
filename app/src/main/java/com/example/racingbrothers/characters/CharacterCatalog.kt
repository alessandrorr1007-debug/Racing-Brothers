package com.example.racingbrothers.characters

/**
 * 3D cartoon character primitive part shared between the 3D rotatable character preview
 * and the libGDX 3D cockpit driver mesh rendered inside the car during the race.
 */
data class CharacterPart(
    val id: String,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val width: Float,
    val height: Float,
    val length: Float,
    val argb: Long
)

data class CharacterSpec(
    val id: String,
    val displayName: String,
    val callsign: String,
    val styleTag: String,
    val description: String,
    val primaryColorArgb: Long,
    val accentColorArgb: Long,
    val parts: List<CharacterPart>
)

object CharacterCatalog {
    val characters: List<CharacterSpec> = listOf(
        // 1. Nico "Turbo" — Aero electric-cyan helmet with twin lightning crest & cobalt/white suit
        CharacterSpec(
            id = "nico_turbo",
            displayName = "Nico",
            callsign = "TURBO",
            styleTag = "Piloto Circuito Pro",
            description = "Casco aerodinámico con cresta relámpago y mono azul eléctrico.",
            primaryColorArgb = 0xFF00B4FF,
            accentColorArgb = 0xFFF3F7FC,
            parts = listOf(
                // Boots & legs
                CharacterPart("leg_l", -0.18f, 0.34f, 0f, 0.24f, 0.68f, 0.26f, 0xFF152542),
                CharacterPart("leg_r", 0.18f, 0.34f, 0f, 0.24f, 0.68f, 0.26f, 0xFF152542),
                CharacterPart("boot_l", -0.18f, 0.08f, 0.06f, 0.26f, 0.16f, 0.38f, 0xFF00B4FF),
                CharacterPart("boot_r", 0.18f, 0.08f, 0.06f, 0.26f, 0.16f, 0.38f, 0xFF00B4FF),
                // Torso racing suit
                CharacterPart("torso", 0f, 1.00f, 0f, 0.66f, 0.68f, 0.38f, 0xFF0F2C59),
                CharacterPart("chest_stripe", 0f, 1.04f, 0.20f, 0.26f, 0.56f, 0.04f, 0xFF00B4FF),
                CharacterPart("collar", 0f, 1.38f, 0f, 0.42f, 0.12f, 0.36f, 0xFFF3F7FC),
                // Arms & gloves
                CharacterPart("arm_l", -0.46f, 1.00f, 0.04f, 0.20f, 0.58f, 0.22f, 0xFF00B4FF),
                CharacterPart("arm_r", 0.46f, 1.00f, 0.04f, 0.20f, 0.58f, 0.22f, 0xFF00B4FF),
                CharacterPart("glove_l", -0.46f, 0.66f, 0.08f, 0.22f, 0.16f, 0.24f, 0xFFF3F7FC),
                CharacterPart("glove_r", 0.46f, 0.66f, 0.08f, 0.22f, 0.16f, 0.24f, 0xFFF3F7FC),
                // Stylized cartoon oversized racing helmet
                CharacterPart("helmet", 0f, 1.72f, 0f, 0.64f, 0.60f, 0.66f, 0xFFF3F7FC),
                CharacterPart("visor", 0f, 1.74f, 0.32f, 0.54f, 0.24f, 0.10f, 0xFF00E5FF),
                CharacterPart("crest_fin", 0f, 2.08f, -0.04f, 0.12f, 0.22f, 0.56f, 0xFF00B4FF),
                CharacterPart("chin_guard", 0f, 1.50f, 0.28f, 0.50f, 0.14f, 0.16f, 0xFF0C1D38)
            )
        ),
        // 2. Kira "Apex" — Sleek crimson & carbon cyber-racer helmet with twin aero ears & gold visor
        CharacterSpec(
            id = "kira_apex",
            displayName = "Kira",
            callsign = "APEX",
            styleTag = "Especialista Nocturna",
            description = "Casco carmesí con aletas gemelas, visor dorado y chaqueta técnica.",
            primaryColorArgb = 0xFFFF2E4C,
            accentColorArgb = 0xFFFFD54F,
            parts = listOf(
                CharacterPart("leg_l", -0.16f, 0.34f, 0f, 0.22f, 0.68f, 0.24f, 0xFF1A1E2B),
                CharacterPart("leg_r", 0.16f, 0.34f, 0f, 0.22f, 0.68f, 0.24f, 0xFF1A1E2B),
                CharacterPart("boot_l", -0.16f, 0.08f, 0.06f, 0.24f, 0.16f, 0.36f, 0xFFFF2E4C),
                CharacterPart("boot_r", 0.16f, 0.08f, 0.06f, 0.24f, 0.16f, 0.36f, 0xFFFF2E4C),
                CharacterPart("torso", 0f, 0.98f, 0f, 0.60f, 0.64f, 0.36f, 0xFF232A3E),
                CharacterPart("jacket_v", 0f, 1.02f, 0.19f, 0.44f, 0.44f, 0.05f, 0xFFFF2E4C),
                CharacterPart("shoulder_l", -0.38f, 1.24f, 0f, 0.24f, 0.16f, 0.34f, 0xFFFFD54F),
                CharacterPart("shoulder_r", 0.38f, 1.24f, 0f, 0.24f, 0.16f, 0.34f, 0xFFFFD54F),
                CharacterPart("arm_l", -0.42f, 0.96f, 0.04f, 0.18f, 0.54f, 0.20f, 0xFF1E2436),
                CharacterPart("arm_r", 0.42f, 0.96f, 0.04f, 0.18f, 0.54f, 0.20f, 0xFF1E2436),
                CharacterPart("glove_l", -0.42f, 0.64f, 0.08f, 0.20f, 0.16f, 0.22f, 0xFFFF2E4C),
                CharacterPart("glove_r", 0.42f, 0.64f, 0.08f, 0.20f, 0.16f, 0.22f, 0xFFFF2E4C),
                // Helmet with twin aero cat-ear vents
                CharacterPart("helmet", 0f, 1.70f, 0f, 0.62f, 0.58f, 0.64f, 0xFFFF2E4C),
                CharacterPart("visor", 0f, 1.72f, 0.32f, 0.54f, 0.26f, 0.09f, 0xFFFFD54F),
                CharacterPart("aero_ear_l", -0.24f, 2.06f, -0.04f, 0.16f, 0.22f, 0.24f, 0xFF182032),
                CharacterPart("aero_ear_r", 0.24f, 2.06f, -0.04f, 0.16f, 0.22f, 0.24f, 0xFF182032),
                CharacterPart("rear_tail", 0f, 1.62f, -0.38f, 0.22f, 0.34f, 0.18f, 0xFF00B4FF)
            )
        ),
        // 3. Beto "Torque" — Broad-shouldered retro-futuristic veteran with amber goggles & open-face helmet
        CharacterSpec(
            id = "beto_torque",
            displayName = "Beto",
            callsign = "TORQUE",
            styleTag = "Veterano de Asfalto",
            description = "Estilo robusto con gafas tácticas ámbar, casco abierto y hombreras reforzadas.",
            primaryColorArgb = 0xFFFFB300,
            accentColorArgb = 0xFF00E676,
            parts = listOf(
                CharacterPart("leg_l", -0.20f, 0.34f, 0f, 0.28f, 0.68f, 0.28f, 0xFF1E2D42),
                CharacterPart("leg_r", 0.20f, 0.34f, 0f, 0.28f, 0.68f, 0.28f, 0xFF1E2D42),
                CharacterPart("boot_l", -0.20f, 0.08f, 0.06f, 0.30f, 0.18f, 0.40f, 0xFF334155),
                CharacterPart("boot_r", 0.20f, 0.08f, 0.06f, 0.30f, 0.18f, 0.40f, 0xFF334155),
                CharacterPart("torso", 0f, 1.02f, 0f, 0.78f, 0.72f, 0.44f, 0xFF18304F),
                CharacterPart("belt", 0f, 0.72f, 0f, 0.80f, 0.12f, 0.46f, 0xFFFFB300),
                CharacterPart("pad_l", -0.46f, 1.32f, 0f, 0.26f, 0.20f, 0.42f, 0xFFFFB300),
                CharacterPart("pad_r", 0.46f, 1.32f, 0f, 0.26f, 0.20f, 0.42f, 0xFFFFB300),
                CharacterPart("arm_l", -0.52f, 0.98f, 0.04f, 0.24f, 0.56f, 0.24f, 0xFF284366),
                CharacterPart("arm_r", 0.52f, 0.98f, 0.04f, 0.24f, 0.56f, 0.24f, 0xFF284366),
                CharacterPart("glove_l", -0.52f, 0.64f, 0.08f, 0.24f, 0.18f, 0.26f, 0xFF00E676),
                CharacterPart("glove_r", 0.52f, 0.64f, 0.08f, 0.24f, 0.18f, 0.26f, 0xFF00E676),
                // Open-face cartoon helmet + stylized face + glowing amber goggles
                CharacterPart("face", 0f, 1.66f, 0.14f, 0.52f, 0.46f, 0.40f, 0xFFE8B796),
                CharacterPart("helmet_shell", 0f, 1.80f, -0.04f, 0.68f, 0.44f, 0.66f, 0xFF273952),
                CharacterPart("helmet_stripe", 0f, 2.04f, -0.04f, 0.22f, 0.08f, 0.66f, 0xFFFFB300),
                CharacterPart("goggle_l", -0.15f, 1.76f, 0.36f, 0.22f, 0.18f, 0.10f, 0xFFFFB300),
                CharacterPart("goggle_r", 0.15f, 1.76f, 0.36f, 0.22f, 0.18f, 0.10f, 0xFFFFB300),
                CharacterPart("mustache", 0f, 1.54f, 0.36f, 0.34f, 0.10f, 0.08f, 0xFF3E2723)
            )
        )
    )

    fun getCharacter(id: String): CharacterSpec = characters.firstOrNull { it.id == id } ?: characters.first()
}
