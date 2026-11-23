package com.example.racingbrothers.game

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Minimal, dependency-free glTF 2.0 / GLB reader used by the Compose showroom previews.
 *
 * It intentionally avoids org.json and Android APIs so the same code runs in plain JVM unit
 * tests, Robolectric and on device. Race rendering uses gdx-gltf (GPU path); this parser only
 * extracts bind-pose triangles + PBR base colors so the 2D software viewport can draw the real
 * Blender meshes instead of the legacy procedural boxes.
 */
data class GlbVertex(
    val x: Float, val y: Float, val z: Float,
    val nx: Float, val ny: Float, val nz: Float
)

data class GlbTriangle(
    val a: GlbVertex,
    val b: GlbVertex,
    val c: GlbVertex,
    val argb: Long,
    val materialName: String
)

data class GlbNodeMeshes(
    val name: String,
    val triangles: List<GlbTriangle>
)

data class GlbModel(
    val triangles: List<GlbTriangle>,
    val nodes: List<GlbNodeMeshes>,
    val animationNames: List<String>,
    val minX: Float, val maxX: Float,
    val minY: Float, val maxY: Float,
    val minZ: Float, val maxZ: Float
) {
    val triangleCount: Int get() = triangles.size

    companion object {
        private const val GLB_MAGIC = 0x46546C67 // 'glTF'
        private const val JSON_CHUNK = 0x4E4F534A
        private const val BIN_CHUNK = 0x004E4942

        fun from(bytes: ByteArray?): GlbModel? = try {
            if (bytes == null || bytes.size < 20) null else parse(bytes)
        } catch (t: Throwable) {
            null
        }

        private fun parse(bytes: ByteArray): GlbModel? {
            val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (header.getInt(0) != GLB_MAGIC || header.getInt(4) != 2) return null
            val declaredLength = header.getInt(8)
            if (declaredLength < 20 || declaredLength > bytes.size) return null

            var offset = 12
            var jsonValue: Any? = null
            var binary: ByteArray? = null
            while (offset + 8 <= bytes.size) {
                val length = header.getInt(offset)
                val type = header.getInt(offset + 4)
                offset += 8
                if (length < 0 || offset + length > bytes.size) break
                when (type) {
                    JSON_CHUNK -> jsonValue = MiniJson(String(bytes, offset, length, Charsets.UTF_8)).parse()
                    BIN_CHUNK -> binary = bytes.copyOfRange(offset, offset + length)
                }
                offset += length
            }
            val root = jsonValue.asMap() ?: return null
            val buffer = binary ?: return null

            val accessors = root.list("accessors")
            val bufferViews = root.list("bufferViews")
            val materials = root.list("materials")
            val meshes = root.list("meshes")
            val nodes = root.list("nodes")

            fun accessorValues(index: Int): FloatArray? {
                val acc = accessors.getOrNull(index).asMap() ?: return null
                val viewIndex = acc.int("bufferView", -1)
                val view = bufferViews.getOrNull(viewIndex).asMap() ?: return null
                val componentType = acc.int("componentType")
                val componentCount = when (acc.string("type")) {
                    "SCALAR" -> 1
                    "VEC2" -> 2
                    "VEC3" -> 3
                    "VEC4" -> 4
                    "MAT4" -> 16
                    else -> 1
                }
                val count = acc.int("count")
                if (count < 0 || count > 2_000_000) return null
                val componentBytes = when (componentType) {
                    5120, 5121 -> 1
                    5122, 5123 -> 2
                    5125, 5126 -> 4
                    else -> return null
                }
                val stride = view.int("byteStride", componentCount * componentBytes)
                if (stride < componentCount * componentBytes) return null
                val start = view.int("byteOffset") + acc.int("byteOffset")
                val out = FloatArray(count * componentCount)
                val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until count) {
                    for (c in 0 until componentCount) {
                        val p = start + i * stride + c * componentBytes
                        if (p < 0 || p + componentBytes > buffer.size) return null
                        out[i * componentCount + c] = when (componentType) {
                            5120 -> bb.get(p).toFloat() / 127f
                            5121 -> (bb.get(p).toInt() and 0xFF).toFloat() / 255f
                            5122 -> bb.getShort(p).toFloat() / 32767f
                            5123 -> (bb.getShort(p).toInt() and 0xFFFF).toFloat()
                            5125 -> (java.lang.Integer.toUnsignedLong(bb.getInt(p))).toFloat()
                            5126 -> bb.getFloat(p)
                            else -> 0f
                        }
                    }
                }
                return out
            }

            val materialCache = HashMap<Int, Pair<Long, String>>()
            fun materialInfo(index: Int): Pair<Long, String> = materialCache.getOrPut(index) {
                val material = materials.getOrNull(index).asMap()
                val pbr = material?.get("pbrMetallicRoughness").asMap()
                val factor = pbr?.list("baseColorFactor")
                fun linearToSrgb(v: Double): Float {
                    val c = v.coerceIn(0.0, 1.0).toFloat()
                    val srgb = if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f
                    return (srgb.coerceIn(0f, 1f) * 255f).toInt().toFloat()
                }
                val r = linearToSrgb(factor?.double(0, 0.8) ?: 0.8)
                val g = linearToSrgb(factor?.double(1, 0.8) ?: 0.8)
                val b = linearToSrgb(factor?.double(2, 0.8) ?: 0.8)
                val argb = (0xFFL shl 24) or
                    ((r.toLong() and 0xFF) shl 16) or
                    ((g.toLong() and 0xFF) shl 8) or
                    (b.toLong() and 0xFF)
                argb to (material?.string("name") ?: "material_$index")
            }

            val worldMatrices = HashMap<Int, FloatArray>()
            fun localMatrix(node: Map<*, *>): FloatArray {
                node.list("matrix")?.let { values ->
                    if (values.size >= 16) {
                        val m = FloatArray(16)
                        for (i in 0 until 16) m[i] = values[i].double().toFloat()
                        return m
                    }
                }
                val t = node.list("translation")
                val r = node.list("rotation")
                val s = node.list("scale")
                val tx = t?.double(0, 0.0)?.toFloat() ?: 0f
                val ty = t?.double(1, 0.0)?.toFloat() ?: 0f
                val tz = t?.double(2, 0.0)?.toFloat() ?: 0f
                val x = r?.double(0, 0.0)?.toFloat() ?: 0f
                val y = r?.double(1, 0.0)?.toFloat() ?: 0f
                val z = r?.double(2, 0.0)?.toFloat() ?: 0f
                val w = r?.double(3, 1.0)?.toFloat() ?: 1f
                val sx = s?.double(0, 1.0)?.toFloat() ?: 1f
                val sy = s?.double(1, 1.0)?.toFloat() ?: 1f
                val sz = s?.double(2, 1.0)?.toFloat() ?: 1f
                val m = FloatArray(16)
                m[0] = (1f - 2f * (y * y + z * z)) * sx
                m[1] = 2f * (x * y + w * z) * sx
                m[2] = 2f * (x * z - w * y) * sx
                m[4] = 2f * (x * y - w * z) * sy
                m[5] = (1f - 2f * (x * x + z * z)) * sy
                m[6] = 2f * (y * z + w * x) * sy
                m[8] = 2f * (x * z + w * y) * sz
                m[9] = 2f * (y * z - w * x) * sz
                m[10] = (1f - 2f * (x * x + y * y)) * sz
                m[12] = tx
                m[13] = ty
                m[14] = tz
                m[15] = 1f
                return m
            }

            fun multiply(a: FloatArray, b: FloatArray): FloatArray {
                val out = FloatArray(16)
                for (col in 0 until 4) {
                    for (row in 0 until 4) {
                        var sum = 0f
                        for (k in 0 until 4) sum += a[k * 4 + row] * b[col * 4 + k]
                        out[col * 4 + row] = sum
                    }
                }
                return out
            }

            val sceneIndex = root.int("scene")
            val sceneNodes = root.list("scenes").getOrNull(sceneIndex).asMap()?.list("nodes")
                ?: nodes.indices.toList()
            val visiting = HashSet<Int>()
            fun visitNode(index: Int, parentWorld: FloatArray, out: MutableList<GlbNodeMeshes>, all: MutableList<GlbTriangle>) {
                if (index !in nodes.indices || !visiting.add(index)) return
                val node = nodes[index].asMap() ?: return
                val world = multiply(parentWorld, localMatrix(node))
                worldMatrices[index] = world
                val meshIndex = node.int("mesh", -1)
                if (meshIndex >= 0) {
                    val mesh = meshes.getOrNull(meshIndex).asMap()
                    val nodeTriangles = ArrayList<GlbTriangle>()
                    for (primitive in mesh?.list("primitives").orEmpty()) {
                        val prim = primitive.asMap() ?: continue
                        val attrs = prim.get("attributes").asMap()
                        val positions = accessorValues(attrs?.int("POSITION", -1) ?: -1)
                        if (positions == null) continue
                        val normalData = accessorValues(attrs?.int("NORMAL", -1) ?: -1)
                        val indexData = accessorValues(prim.int("indices", -1))
                        val (color, materialName) = materialInfo(prim.int("material", 0))
                        val vertexCount = positions.size / 3
                        val indexCount = if (indexData != null) indexData.size else vertexCount
                        var i = 0
                        while (i + 2 < indexCount) {
                            fun vertexAt(slot: Int): GlbVertex {
                                val vi = if (indexData != null) indexData[i + slot].toInt() else i + slot
                                if (vi < 0 || vi >= vertexCount) return GlbVertex(0f, 0f, 0f, 0f, 1f, 0f)
                                val px = positions[vi * 3]
                                val py = positions[vi * 3 + 1]
                                val pz = positions[vi * 3 + 2]
                                val wx = world[0] * px + world[4] * py + world[8] * pz + world[12]
                                val wy = world[1] * px + world[5] * py + world[9] * pz + world[13]
                                val wz = world[2] * px + world[6] * py + world[10] * pz + world[14]
                                var nx = if (normalData != null && vi * 3 + 2 < normalData.size) normalData[vi * 3] else 0f
                                var ny = if (normalData != null && vi * 3 + 2 < normalData.size) normalData[vi * 3 + 1] else 1f
                                var nz = if (normalData != null && vi * 3 + 2 < normalData.size) normalData[vi * 3 + 2] else 0f
                                val twx = world[0] * nx + world[4] * ny + world[8] * nz
                                val twy = world[1] * nx + world[5] * ny + world[9] * nz
                                val twz = world[2] * nx + world[6] * ny + world[10] * nz
                                nx = twx; ny = twy; nz = twz
                                val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
                                if (len > 0.0001f) {
                                    nx /= len; ny /= len; nz /= len
                                } else {
                                    nx = 0f; ny = 1f; nz = 0f
                                }
                                return GlbVertex(wx, wy, wz, nx, ny, nz)
                            }
                            nodeTriangles.add(GlbTriangle(vertexAt(0), vertexAt(1), vertexAt(2), color, materialName))
                            i += 3
                        }
                    }
                    if (nodeTriangles.isNotEmpty()) {
                        out.add(GlbNodeMeshes(node.string("name", "mesh_$index"), nodeTriangles))
                        all.addAll(nodeTriangles)
                    }
                }
                for (child in node.list("children").orEmpty()) {
                    visitNode(child.double().toInt(), world, out, all)
                }
                visiting.remove(index)
            }

            val identity = FloatArray(16).apply { this[0] = 1f; this[5] = 1f; this[10] = 1f; this[15] = 1f }
            val nodeMeshes = ArrayList<GlbNodeMeshes>()
            val triangles = ArrayList<GlbTriangle>()
            for (rootIndex in sceneNodes) {
                visitNode(rootIndex.double().toInt(), identity, nodeMeshes, triangles)
            }
            if (triangles.isEmpty()) return null

            var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
            for (triangle in triangles) {
                for (v in arrayOf(triangle.a, triangle.b, triangle.c)) {
                    if (v.x < minX) minX = v.x
                    if (v.x > maxX) maxX = v.x
                    if (v.y < minY) minY = v.y
                    if (v.y > maxY) maxY = v.y
                    if (v.z < minZ) minZ = v.z
                    if (v.z > maxZ) maxZ = v.z
                }
            }
            val animationNames = root.list("animations").orEmpty().mapIndexed { idx, anim ->
                anim.asMap()?.string("name", "animation_$idx") ?: "animation_$idx"
            }
            return GlbModel(triangles, nodeMeshes, animationNames, minX, maxX, minY, maxY, minZ, maxZ)
        }
    }
}

private fun Any?.asMap(): Map<*, *>? = this as? Map<*, *>

private fun Any?.double(): Double = (this as? Number)?.toDouble() ?: 0.0

private fun Any?.asList(): List<*>? = this as? List<*>

private fun Map<*, *>.list(key: String): List<*> = this[key].asList() ?: emptyList<Any?>()

private fun Map<*, *>.string(key: String, fallback: String = ""): String = this[key] as? String ?: fallback

private fun Map<*, *>.int(key: String, fallback: Int = 0): Int = (this[key] as? Number)?.toInt() ?: fallback

private fun Map<*, *>.double(key: String, fallback: Double = 0.0): Double = (this[key] as? Number)?.toDouble() ?: fallback

private fun List<*>.double(index: Int, fallback: Double = 0.0): Double = this.getOrNull(index)?.double() ?: fallback

/** Tiny recursive-descent JSON reader: objects, arrays, strings, numbers, booleans and null. */
private class MiniJson(private val source: String) {
    private var pos = 0

    fun parse(): Any? {
        val value = readValue()
        skipWhitespace()
        return value
    }

    private fun skipWhitespace() {
        while (pos < source.length && source[pos].isWhitespace()) pos++
    }

    private fun readValue(): Any? {
        skipWhitespace()
        if (pos >= source.length) return null
        return when (source[pos]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> { pos += 4; true }
            'f' -> { pos += 5; false }
            'n' -> { pos += 4; null }
            else -> readNumber()
        }
    }

    private fun readObject(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        pos++
        skipWhitespace()
        if (pos < source.length && source[pos] == '}') { pos++; return result }
        while (pos < source.length) {
            skipWhitespace()
            val key = readString()
            skipWhitespace()
            if (pos < source.length && source[pos] == ':') pos++
            result[key] = readValue()
            skipWhitespace()
            if (pos < source.length && source[pos] == ',') { pos++; continue }
            if (pos < source.length && source[pos] == '}') pos++
            break
        }
        return result
    }

    private fun readArray(): List<Any?> {
        val result = ArrayList<Any?>()
        pos++
        skipWhitespace()
        if (pos < source.length && source[pos] == ']') { pos++; return result }
        while (pos < source.length) {
            result.add(readValue())
            skipWhitespace()
            if (pos < source.length && source[pos] == ',') { pos++; continue }
            if (pos < source.length && source[pos] == ']') pos++
            break
        }
        return result
    }

    private fun readString(): String {
        val result = StringBuilder()
        skipWhitespace()
        if (pos < source.length && source[pos] == '"') pos++
        while (pos < source.length) {
            val c = source[pos++]
            if (c == '"') break
            if (c == '\\' && pos < source.length) {
                when (val escaped = source[pos++]) {
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    'b' -> result.append('\b')
                    'f' -> result.append('\u000C')
                    'u' -> {
                        if (pos + 4 <= source.length) {
                            result.append(source.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                    }
                    else -> result.append(escaped)
                }
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }

    private fun readNumber(): Double {
        val start = pos
        while (pos < source.length && (source[pos].isDigit() || source[pos] in "+-.eE")) pos++
        return source.substring(start, pos).toDoubleOrNull() ?: 0.0
    }
}
