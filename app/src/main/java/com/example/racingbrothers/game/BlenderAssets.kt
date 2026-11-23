package com.example.racingbrothers.game

/** Wire IDs stay compatible with existing saves and room servers. */
object BlenderAssets {
    data class Car(val fileName: String, val label: String, val seatZ: Float, val tireRadius: Float) {
        fun path(lod: Int): String {
            val suffix = if (lod == 1) "" else "_LOD$lod"
            return "models/vehicles/$fileName$suffix.glb"
        }
    }

    val cars = mapOf(
        "vortex_rs" to Car("Bugatti", "Bugatti Chiron", -0.19f, 0.3395f),
        "apex_gtx" to Car("Ferrari", "Ferrari 296 GTB", -0.16f, 0.33975f),
        "titan_v8" to Car("Lamborghini", "Lamborghini Revuelto", -0.19f, 0.34675f),
        "formula_aero" to Car("McLaren", "McLaren 750S", -0.17f, 0.32705f)
    )

    val pilots = mapOf(
        "nico_turbo" to "Pilot_01",
        "kira_apex" to "Pilot_02",
        "beto_torque" to "Pilot_03",
        "pilot_04" to "Pilot_04"
    )

    /** All four pilot IDs are accepted by v1 protocol; no wire downgrade is needed. */
    fun wirePilot(id: String): String = id
}

