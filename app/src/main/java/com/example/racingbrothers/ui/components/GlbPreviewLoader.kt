package com.example.racingbrothers.ui.components

import android.content.Context
import com.example.racingbrothers.game.BlenderAssets
import com.example.racingbrothers.game.GlbModel
import com.example.racingbrothers.game.GlbTriangle
import com.example.racingbrothers.game.GlbVertex
import java.util.concurrent.ConcurrentHashMap

/** Asset-backed preview meshes, parsed once and reused by every Compose 3D viewport. */
object GlbPreviewLoader {
    private val cache = ConcurrentHashMap<String, GlbModel?>()

    fun car(context: Context, vehicleId: String, lod: Int = 2): GlbModel? {
        val path = BlenderAssets.cars[vehicleId]?.path(lod) ?: return null
        return load(context, path)
    }

    fun pilot(context: Context, characterId: String): GlbModel? {
        val file = BlenderAssets.pilots[characterId] ?: return null
        return load(context, "models/pilots/$file.glb")
    }

    private fun load(context: Context, path: String): GlbModel? {
        if (cache.containsKey(path)) return cache[path]
        val parsed = try {
            context.assets.open(path).use { it.readBytes() }.let { GlbModel.from(it) }
        } catch (t: Throwable) {
            null
        }
        cache[path] = parsed
        return parsed
    }

    /** Repositions a pilot mesh into the cockpit; used when the showroom displays a driver. */
    fun GlbModel.placed(scale: Float, tx: Float, ty: Float, tz: Float): List<GlbTriangle> {
        fun move(vertex: GlbVertex) = GlbVertex(
            vertex.x * scale + tx,
            vertex.y * scale + ty,
            vertex.z * scale + tz,
            vertex.nx, vertex.ny, vertex.nz
        )
        return triangles.map { GlbTriangle(move(it.a), move(it.b), move(it.c), it.argb, it.materialName) }
    }
}
