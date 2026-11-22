package com.example.racingbrothers.tracks

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPoint(
    val x: Float,
    val z: Float,
    val dirX: Float,
    val dirZ: Float,
    val rightX: Float,
    val rightZ: Float,
    val cumulativeDist: Float
)

data class TrackMarker(val x: Float, val z: Float, val local: Boolean)

data class TrackCheckpoint(
    val index: Int,
    val x: Float,
    val z: Float,
    val dirX: Float,
    val dirZ: Float,
    val radius: Float
)

data class TrackQueryResult(
    val segmentIndex: Int,
    val signedLateralOffset: Float,
    val closestX: Float,
    val closestZ: Float,
    val tangentX: Float,
    val tangentZ: Float,
    val rightX: Float,
    val rightZ: Float,
    val distAlongTrack: Float
)

data class SceneryProp(
    val x: Float,
    val z: Float,
    val yawDeg: Float,
    val kind: PropKind
)

enum class PropKind {
    FLOODLIGHT_TOWER,
    GRANDSTAND,
    TREE_CLUSTER,
    CHEVRON_SIGN
}

enum class CircuitId(val wireId: String) {
    SPEEDWAY("speedway"),
    MOUNTAIN("mountain"),
    NIGHT_CITY("night_city"),
    RANDOM("random");

    companion object {
        fun fromWire(value: String): CircuitId = entries.firstOrNull { it.wireId == value } ?: SPEEDWAY
    }
}

/**
 * Circuito Neon Bay — Closed arcade circuit with straights, sweeping turns, an S-chicane,
 * hairpin, curbs, guardrails, 6 ordered checkpoints, and 3-lap race rules.
 *
 * The checkpoint coordinates & radii here are mirrored verbatim in the Cloudflare Worker
 * backend validation module so client physics and server validation stay 100% in sync.
 */
class CircuitTrack private constructor(val id: CircuitId, private val controlPoints: List<Pair<Float, Float>>) {
    companion object {
        const val TOTAL_LAPS = 3
        const val ROAD_HALF_WIDTH = 9.5f
        const val BARRIER_HALF_WIDTH = 11.2f
        const val CHECKPOINT_COUNT = 6

        private val speedway = CircuitTrack(CircuitId.SPEEDWAY, listOf(
            0f to 0f, 0f to 85f, 28f to 148f, 98f to 164f, 162f to 132f,
            178f to 62f, 132f to 16f, 166f to -46f, 128f to -114f, 56f to -118f, 10f to -76f
        ))
        private val mountain = CircuitTrack(CircuitId.MOUNTAIN, listOf(
            0f to 0f, 0f to 68f, 29f to 143f, 92f to 185f, 182f to 161f,
            220f to 70f, 166f to 17f, 213f to -65f, 162f to -138f, 67f to -162f, 8f to -80f
        ))
        private val nightCity = CircuitTrack(CircuitId.NIGHT_CITY, listOf(
            0f to 0f, 0f to 114f, 36f to 165f, 135f to 165f, 194f to 121f,
            194f to 57f, 252f to 12f, 251f to -103f, 207f to -142f, 59f to -142f, 9f to -83f
        ))

        fun forId(id: CircuitId): CircuitTrack = when (id) {
            CircuitId.SPEEDWAY, CircuitId.RANDOM -> speedway
            CircuitId.MOUNTAIN -> mountain
            CircuitId.NIGHT_CITY -> nightCity
        }
    }

    val samples: List<TrackPoint> by lazy { buildCatmullRomTrack(samplesPerSpan = 12) }
    val totalLength: Float by lazy {
        val last = samples.last()
        val first = samples.first()
        last.cumulativeDist + hypot(first.x - last.x, first.z - last.z)
    }

    val checkpoints: List<TrackCheckpoint> by lazy {
        val count = CHECKPOINT_COUNT
        val step = samples.size / count
        (0 until count).map { idx ->
            val pt = samples[(idx * step) % samples.size]
            TrackCheckpoint(
                index = idx,
                x = pt.x,
                z = pt.z,
                dirX = pt.dirX,
                dirZ = pt.dirZ,
                radius = 15.0f
            )
        }
    }

    val sceneryProps: List<SceneryProp> by lazy { generateScenery() }

    /**
     * Staggered grid in two rows with two cars per row.
     */
    fun getStartSlot(slotIndex: Int): Triple<Float, Float, Float> {
        val start = samples.first()
        val slot = slotIndex.coerceIn(0, 3)
        val lateral = if (slot % 2 == 0) -3.2f else 3.2f
        val backOffset = -5.5f - (slot % 2) * 1.5f - (slot / 2) * 6f
        val x = start.x + start.rightX * lateral + start.dirX * backOffset
        val z = start.z + start.rightZ * lateral + start.dirZ * backOffset
        val headingRad = atan2(start.dirX, start.dirZ)
        return Triple(x, z, headingRad)
    }

    fun queryClosestPoint(x: Float, z: Float): TrackQueryResult {
        var bestIdx = 0
        var bestDistSq = Float.MAX_VALUE
        var bestProjX = 0f
        var bestProjZ = 0f
        var bestT = 0f

        val n = samples.size
        for (i in 0 until n) {
            val a = samples[i]
            val b = samples[(i + 1) % n]
            val abX = b.x - a.x
            val abZ = b.z - a.z
            val abLenSq = abX * abX + abZ * abZ
            val t = if (abLenSq > 1e-5f) {
                (((x - a.x) * abX + (z - a.z) * abZ) / abLenSq).coerceIn(0f, 1f)
            } else 0f
            val px = a.x + abX * t
            val pz = a.z + abZ * t
            val dx = x - px
            val dz = z - pz
            val dSq = dx * dx + dz * dz
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                bestIdx = i
                bestProjX = px
                bestProjZ = pz
                bestT = t
            }
        }

        val a = samples[bestIdx]
        val b = samples[(bestIdx + 1) % n]
        val segLen = hypot(b.x - a.x, b.z - a.z)
        val signedLateral = (x - bestProjX) * a.rightX + (z - bestProjZ) * a.rightZ

        return TrackQueryResult(
            segmentIndex = bestIdx,
            signedLateralOffset = signedLateral,
            closestX = bestProjX,
            closestZ = bestProjZ,
            tangentX = a.dirX,
            tangentZ = a.dirZ,
            rightX = a.rightX,
            rightZ = a.rightZ,
            distAlongTrack = a.cumulativeDist + segLen * bestT
        )
    }

    fun isInsideCheckpoint(checkpointIndex: Int, x: Float, z: Float): Boolean {
        val cp = checkpoints.getOrNull(checkpointIndex) ?: return false
        val dx = x - cp.x
        val dz = z - cp.z
        return (dx * dx + dz * dz) <= (cp.radius * cp.radius)
    }

    /**
     * Monotonic progress score for ranking 1st vs 2nd live during the race.
     */
    fun computeRaceProgress(completedLaps: Int, nextCheckpoint: Int, x: Float, z: Float): Float {
        val q = queryClosestPoint(x, z)
        val normalizedLoop = q.distAlongTrack / totalLength
        // Guard against wrap-around right behind the start/finish line when nextCheckpoint == 1
        val adjustedLoop = if (nextCheckpoint == 1 && normalizedLoop > 0.85f) {
            normalizedLoop - 1f
        } else if (nextCheckpoint == 0 && normalizedLoop < 0.15f) {
            normalizedLoop + 1f
        } else {
            normalizedLoop
        }
        return completedLaps.toFloat() + adjustedLoop
    }

    private fun buildCatmullRomTrack(samplesPerSpan: Int): List<TrackPoint> {
        val raw = mutableListOf<Pair<Float, Float>>()
        val n = controlPoints.size
        for (i in 0 until n) {
            val p0 = controlPoints[(i - 1 + n) % n]
            val p1 = controlPoints[i]
            val p2 = controlPoints[(i + 1) % n]
            val p3 = controlPoints[(i + 2) % n]
            for (s in 0 until samplesPerSpan) {
                val t = s.toFloat() / samplesPerSpan.toFloat()
                val x = catmullRom(p0.first, p1.first, p2.first, p3.first, t)
                val z = catmullRom(p0.second, p1.second, p2.second, p3.second, t)
                raw.add(x to z)
            }
        }

        val result = mutableListOf<TrackPoint>()
        var cumulative = 0f
        val count = raw.size
        for (i in 0 until count) {
            val prev = raw[(i - 1 + count) % count]
            val curr = raw[i]
            val next = raw[(i + 1) % count]
            if (i > 0) {
                cumulative += hypot(curr.first - prev.first, curr.second - prev.second)
            }
            val tx = next.first - prev.first
            val tz = next.second - prev.second
            val len = sqrt(tx * tx + tz * tz).coerceAtLeast(1e-4f)
            val dirX = tx / len
            val dirZ = tz / len
            // Right-handed XZ right vector: cross(dir, up=(0,1,0)) = (-dirZ, dirX)
            val rightX = -dirZ
            val rightZ = dirX
            result.add(TrackPoint(curr.first, curr.second, dirX, dirZ, rightX, rightZ, cumulative))
        }
        return result
    }

    private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * p1) +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
            )
    }

    private fun generateScenery(): List<SceneryProp> {
        val props = mutableListOf<SceneryProp>()
        val n = samples.size
        for (i in 0 until n) {
            val pt = samples[i]
            val yawDeg = (atan2(pt.dirX, pt.dirZ) * 180.0 / PI).toFloat()
            // Stadium floodlight towers around the circuit
            if (i % 14 == 0) {
                val side = if ((i / 14) % 2 == 0) 1f else -1f
                props.add(
                    SceneryProp(
                        x = pt.x + pt.rightX * (BARRIER_HALF_WIDTH + 7.5f) * side,
                        z = pt.z + pt.rightZ * (BARRIER_HALF_WIDTH + 7.5f) * side,
                        yawDeg = yawDeg,
                        kind = PropKind.FLOODLIGHT_TOWER
                    )
                )
            }
            // Grandstands near start/finish straight and north straight
            if (i in listOf(4, 12, 34, 42)) {
                props.add(
                    SceneryProp(
                        x = pt.x - pt.rightX * (BARRIER_HALF_WIDTH + 12f),
                        z = pt.z - pt.rightZ * (BARRIER_HALF_WIDTH + 12f),
                        yawDeg = yawDeg,
                        kind = PropKind.GRANDSTAND
                    )
                )
            }
            // Directional chevron signs on high-curvature turns
            val next = samples[(i + 2) % n]
            val cross = pt.dirX * next.dirZ - pt.dirZ * next.dirX
            if (kotlin.math.abs(cross) > 0.14f && i % 4 == 0) {
                val outside = if (cross > 0f) -1f else 1f
                props.add(
                    SceneryProp(
                        x = pt.x + pt.rightX * (BARRIER_HALF_WIDTH + 2.2f) * outside,
                        z = pt.z + pt.rightZ * (BARRIER_HALF_WIDTH + 2.2f) * outside,
                        yawDeg = yawDeg,
                        kind = PropKind.CHEVRON_SIGN
                    )
                )
            }
            // Decorative trees / pylons
            if (i % 7 == 3) {
                val side = if (i % 2 == 0) 1f else -1f
                props.add(
                    SceneryProp(
                        x = pt.x + pt.rightX * (BARRIER_HALF_WIDTH + 16f) * side,
                        z = pt.z + pt.rightZ * (BARRIER_HALF_WIDTH + 16f) * side,
                        yawDeg = (i * 37f) % 360f,
                        kind = PropKind.TREE_CLUSTER
                    )
                )
            }
        }
        return props
    }
}
