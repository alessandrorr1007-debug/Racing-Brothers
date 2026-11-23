package com.example

import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.game.BlenderAssets
import com.example.racingbrothers.game.GlbModel
import com.example.racingbrothers.vehicles.VehicleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Verifies the Blender GLB pipeline without needing an Android device or Robolectric. */
class GlbAssetsTest {
    private val assetRoot = File("src/main/assets")

    @Test
    fun `vehicle LODs parse and decrease in detail while keeping four wheel nodes`() {
        assertTrue("GLB assets missing at ${assetRoot.absolutePath}", assetRoot.exists())
        assertEquals(4, VehicleCatalog.vehicles.size)
        for (id in VehicleCatalog.vehicles.map { it.id }) {
            val car = BlenderAssets.cars.getValue(id)
            val lods = listOf(0, 1, 2).map { level ->
                val file = File(assetRoot, car.path(level))
                assertTrue("${car.path(level)} missing", file.exists())
                GlbModel.from(file.readBytes()) ?: error("Unable to parse ${car.path(level)}")
            }
            assertTrue("$id LOD0 must be denser than LOD1: ${lods[0].triangleCount} vs ${lods[1].triangleCount}",
                lods[0].triangleCount > lods[1].triangleCount)
            assertTrue("$id LOD1 must be denser than LOD2: ${lods[1].triangleCount} vs ${lods[2].triangleCount}",
                lods[1].triangleCount > lods[2].triangleCount)
            val wheelNodes = lods[1].nodes.map { it.name }
            for (wheel in listOf("Wheel_Front_L", "Wheel_Front_R", "Wheel_Rear_L", "Wheel_Rear_R")) {
                assertTrue("$id missing $wheel node", wheelNodes.any { it.equals(wheel, ignoreCase = true) })
            }
            assertTrue("$id GLB body should sit near ground level: ${lods[1].minY}", lods[1].minY < 0.2f)
            assertTrue("$id GLB should face +Z: maxZ=${lods[1].maxZ}", lods[1].maxZ > 2f)
        }
    }

    @Test
    fun `all four pilots parse with Driving and Exhibition clips and unique wire IDs`() {
        assertEquals(4, CharacterCatalog.characters.size)
        val ids = CharacterCatalog.characters.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
        assertTrue(ids.contains("pilot_04"))
        for (id in ids) {
            assertEquals("wire ID $id must survive the protocol", id, BlenderAssets.wirePilot(id))
            val file = File(assetRoot, "models/pilots/${BlenderAssets.pilots.getValue(id)}.glb")
            assertTrue(file.absolutePath, file.exists())
            val model = GlbModel.from(file.readBytes()) ?: error("Unable to parse ${file.name}")
            assertTrue("${file.name} has no triangles", model.triangleCount > 1000)
            assertTrue("${file.name} missing Driving: ${model.animationNames}",
                model.animationNames.any { it.equals("Driving", ignoreCase = true) })
            assertTrue("${file.name} missing Exhibition: ${model.animationNames}",
                model.animationNames.any { it.equals("Exhibition", ignoreCase = true) })
            assertTrue("${file.name} pilot height ${model.maxY}", model.maxY > 1.7f && model.maxY < 2f)
        }
    }
}
