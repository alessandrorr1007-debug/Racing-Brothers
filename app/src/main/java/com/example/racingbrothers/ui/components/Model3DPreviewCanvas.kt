package com.example.racingbrothers.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.racingbrothers.characters.CharacterSpec
import com.example.racingbrothers.vehicles.PartColorRole
import com.example.racingbrothers.vehicles.VehiclePaintColor
import com.example.racingbrothers.vehicles.VehicleSpec
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class RenderBox3D(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val w: Float,
    val h: Float,
    val l: Float,
    val color: Color,
    val pitchDeg: Float = 0f,
    val emissive: Boolean = false,
    val topWidthScale: Float = 1f,
    val topLengthScale: Float = 1f
)

private data class ProjectedQuad(
    val p0: Offset,
    val p1: Offset,
    val p2: Offset,
    val p3: Offset,
    val depth: Float,
    val fillColor: Color,
    val strokeColor: Color
)

@Composable
fun Vehicle3DPreview(
    vehicle: VehicleSpec,
    paintColor: VehiclePaintColor,
    driver: CharacterSpec? = null,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
    interactiveDrag: Boolean = true,
    initialYawDeg: Float = 38f,
    initialPitchDeg: Float = 18f,
    zoomScale: Float = 1.0f,
    garageLighting: Boolean = false
) {
    val boxes = remember(vehicle.id, paintColor.id, driver?.id) {
        buildList {
            for (part in vehicle.parts) {
                val color = resolveVehiclePartColor(part.role, paintColor)
                add(
                    RenderBox3D(
                        cx = part.centerX,
                        cy = part.centerY,
                        cz = part.centerZ,
                        w = part.width,
                        h = part.height,
                        l = part.length,
                        color = color,
                        pitchDeg = part.pitchDeg,
                        topWidthScale = part.topWidthScale,
                        topLengthScale = part.topLengthScale,
                        emissive = part.role == PartColorRole.HEADLIGHT ||
                            part.role == PartColorRole.TAILLIGHT ||
                            part.role == PartColorRole.ACCENT_GLOW
                    )
                )
            }
            if (driver != null) {
                val scale = 0.56f
                for (dp in driver.parts) {
                    if (dp.centerY < 0.78f) continue
                    add(
                        RenderBox3D(
                            cx = dp.centerX * scale,
                            cy = vehicle.cockpitY + (dp.centerY - 0.85f) * scale,
                            cz = vehicle.cockpitZ + dp.centerZ * scale,
                            w = dp.width * scale,
                            h = dp.height * scale,
                            l = dp.length * scale,
                            color = Color(dp.argb)
                        )
                    )
                }
            }
        }
    }

    Interactive3DViewport(
        boxes = boxes,
        centerYOffset = 0.62f,
        cameraDistance = 7.2f / zoomScale,
        modifier = modifier,
        autoRotate = autoRotate,
        interactiveDrag = interactiveDrag,
        initialYawDeg = initialYawDeg,
        initialPitchDeg = initialPitchDeg,
        garageLighting = garageLighting
    )
}

@Composable
fun Character3DPreview(
    character: CharacterSpec,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
    interactiveDrag: Boolean = true,
    initialYawDeg: Float = 24f,
    initialPitchDeg: Float = 12f
) {
    val boxes = remember(character.id) {
        character.parts.map { part ->
            RenderBox3D(
                cx = part.centerX,
                cy = part.centerY,
                cz = part.centerZ,
                w = part.width,
                h = part.height,
                l = part.length,
                color = Color(part.argb),
                emissive = part.id.contains("visor") || part.id.contains("goggle")
            )
        }
    }

    Interactive3DViewport(
        boxes = boxes,
        centerYOffset = 1.08f,
        cameraDistance = 4.6f,
        modifier = modifier,
        autoRotate = autoRotate,
        interactiveDrag = interactiveDrag,
        initialYawDeg = initialYawDeg,
        initialPitchDeg = initialPitchDeg
    )
}

@Composable
private fun Interactive3DViewport(
    boxes: List<RenderBox3D>,
    centerYOffset: Float,
    cameraDistance: Float,
    modifier: Modifier,
    autoRotate: Boolean,
    interactiveDrag: Boolean,
    initialYawDeg: Float,
    initialPitchDeg: Float,
    garageLighting: Boolean = false
) {
    var yawDeg by remember { mutableFloatStateOf(initialYawDeg) }
    var pitchDeg by remember { mutableFloatStateOf(initialPitchDeg) }

    val turntableRotation = if (autoRotate) {
        rememberInfiniteTransition(label = "Vehicle turntable").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(12_857, easing = LinearEasing)),
            label = "Turntable rotation"
        )
    } else null

    val dragModifier = if (interactiveDrag) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                yawDeg = (yawDeg + dragAmount.x * 0.55f) % 360f
                pitchDeg = (pitchDeg + dragAmount.y * 0.25f).coerceIn(4f, 44f)
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(dragModifier)) {
        val w = size.width
        val h = size.height
        if (w <= 4f || h <= 4f) return@Canvas

        val cx = w * 0.5f
        val cy = h * 0.54f
        val focal = minOf(w, h) * 1.18f
        val redLed = Color(0xFFFF2458)
        if (garageLighting) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF030714), Color(0xFF14213B), Color(0xFF050913))))
            for ((x, led) in listOf(0.18f to NeonCyan, 0.82f to redLed)) {
                val fixture = Offset(w * x, h * 0.22f)
                drawCircle(
                    Brush.radialGradient(listOf(led.copy(alpha = 0.26f), Color.Transparent), fixture, w * 0.36f),
                    radius = w * 0.36f,
                    center = fixture
                )
                drawLine(led, fixture - Offset(w * 0.09f, 0f), fixture + Offset(w * 0.09f, 0f), h * 0.014f)
                drawOval(
                    Brush.radialGradient(listOf(led.copy(alpha = 0.22f), Color.Transparent), Offset(w * x, h * 0.86f), w * 0.21f),
                    topLeft = Offset(w * (x - 0.21f), h * 0.75f),
                    size = Size(w * 0.42f, h * 0.21f)
                )
            }
        }

        // Glowing motorsport turntable pedestal under the 3D model
        drawTurntablePedestal(cx = cx, cy = cy + minOf(w, h) * 0.24f, radiusX = minOf(w, h) * 0.42f, radiusY = minOf(w, h) * 0.13f)

        val yawRad = Math.toRadians((yawDeg + (turntableRotation?.value ?: 0f)).toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)

        val lightDir = normalize(Vec3(0.45f, 0.78f, -0.52f))
        val rimDir = normalize(Vec3(-0.6f, 0.25f, 0.75f))

        val quads = ArrayList<ProjectedQuad>(boxes.size * 6)

        for (box in boxes) {
            val hw = box.w * 0.5f
            val hh = box.h * 0.5f
            val hl = box.l * 0.5f
            val topW = hw * box.topWidthScale
            val topL = hl * box.topLengthScale
            val localCorners = arrayOf(
                Vec3(-hw, -hh, -hl), Vec3(hw, -hh, -hl), Vec3(topW, hh, -topL), Vec3(-topW, hh, -topL),
                Vec3(-hw, -hh, hl), Vec3(hw, -hh, hl), Vec3(topW, hh, topL), Vec3(-topW, hh, topL)
            )
            val boxPitchRad = Math.toRadians(box.pitchDeg.toDouble()).toFloat()
            val cbp = cos(boxPitchRad)
            val sbp = sin(boxPitchRad)

            val viewCorners = Array(8) { idx ->
                val c = localCorners[idx]
                // Local box pitch around X
                val py = c.y * cbp - c.z * sbp
                val pz = c.y * sbp + c.z * cbp
                // World position relative to pivot
                val wx = c.x + box.cx
                val wy = py + box.cy - centerYOffset
                val wz = pz + box.cz
                // Rotate by turntable yaw (Y) then camera pitch (X)
                val rx = wx * cosY + wz * sinY
                val rz0 = -wx * sinY + wz * cosY
                val ry = wy * cosP - rz0 * sinP
                val rz = wy * sinP + rz0 * cosP + cameraDistance
                Vec3(rx, ry, rz)
            }

            for (face in CUBE_FACES) {
                val v0 = viewCorners[face[0]]
                val v1 = viewCorners[face[1]]
                val v2 = viewCorners[face[2]]
                val v3 = viewCorners[face[3]]

                if (v0.z <= 0.35f || v1.z <= 0.35f || v2.z <= 0.35f || v3.z <= 0.35f) continue

                val e1 = Vec3(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z)
                val e2 = Vec3(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z)
                val normal = normalize(cross(e1, e2))

                // Back-face culling in camera space (camera at origin looking +Z)
                val toCam = Vec3(-v0.x, -v0.y, -v0.z)
                if (dot(normal, toCam) <= 0f) continue

                val p0 = Offset(cx + (v0.x / v0.z) * focal, cy - (v0.y / v0.z) * focal)
                val p1 = Offset(cx + (v1.x / v1.z) * focal, cy - (v1.y / v1.z) * focal)
                val p2 = Offset(cx + (v2.x / v2.z) * focal, cy - (v2.y / v2.z) * focal)
                val p3 = Offset(cx + (v3.x / v3.z) * focal, cy - (v3.y / v3.z) * focal)

                val avgDepth = (v0.z + v1.z + v2.z + v3.z) * 0.25f

                val fill = if (box.emissive) {
                    box.color
                } else {
                    val keyLight = dot(normal, lightDir).coerceIn(0f, 1f)
                    val rimLight = dot(normal, rimDir).coerceIn(0f, 1f) * 0.28f
                    val intensity = (0.38f + keyLight * 0.62f).coerceIn(0.28f, 1.05f)
                    Color(
                        red = (box.color.red * intensity + (if (garageLighting) 0.85f else 0.04f) * rimLight).coerceIn(0f, 1f),
                        green = (box.color.green * intensity + 0.35f * rimLight).coerceIn(0f, 1f),
                        blue = (box.color.blue * intensity + 0.65f * rimLight).coerceIn(0f, 1f),
                        alpha = 1f
                    )
                }

                val edgeColor = if (box.emissive) {
                    Color.White.copy(alpha = 0.45f)
                } else {
                    Color.Black.copy(alpha = 0.26f)
                }

                quads.add(ProjectedQuad(p0, p1, p2, p3, avgDepth, fill, edgeColor))
            }
        }

        // Painter's algorithm: farthest first
        quads.sortByDescending { it.depth }

        val path = Path()
        for (q in quads) {
            path.reset()
            path.moveTo(q.p0.x, q.p0.y)
            path.lineTo(q.p1.x, q.p1.y)
            path.lineTo(q.p2.x, q.p2.y)
            path.lineTo(q.p3.x, q.p3.y)
            path.close()
            drawPath(path = path, color = q.fillColor)
            drawPath(path = path, color = q.strokeColor, style = Stroke(width = 1.2f))
        }
    }
}

private fun DrawScope.drawTurntablePedestal(cx: Float, cy: Float, radiusX: Float, radiusY: Float) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                ElectricBlue.copy(alpha = 0.34f),
                ElectricBlue.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radiusX * 1.15f
        ),
        topLeft = Offset(cx - radiusX * 1.15f, cy - radiusY * 1.35f),
        size = androidx.compose.ui.geometry.Size(radiusX * 2.3f, radiusY * 2.7f)
    )
    drawOval(
        color = NeonCyan.copy(alpha = 0.55f),
        topLeft = Offset(cx - radiusX, cy - radiusY),
        size = androidx.compose.ui.geometry.Size(radiusX * 2f, radiusY * 2f),
        style = Stroke(width = 2.2f)
    )
    drawOval(
        color = ElectricBlue.copy(alpha = 0.28f),
        topLeft = Offset(cx - radiusX * 0.72f, cy - radiusY * 0.72f),
        size = androidx.compose.ui.geometry.Size(radiusX * 1.44f, radiusY * 1.44f),
        style = Stroke(width = 1.4f)
    )
}

private fun resolveVehiclePartColor(role: PartColorRole, paint: VehiclePaintColor): Color = when (role) {
    PartColorRole.PRIMARY_PAINT -> Color(paint.argb)
    PartColorRole.SECONDARY_TRIM -> Color(0xFF1A2436)
    PartColorRole.GLASS_TINT -> Color(0xFF284366)
    PartColorRole.HEADLIGHT -> Color(0xFFE6F8FF)
    PartColorRole.TAILLIGHT -> Color(0xFFFF2E4C)
    PartColorRole.TIRE_RUBBER -> Color(0xFF14171D)
    PartColorRole.WHEEL_RIM -> Color(0xFFC4D1E2)
    PartColorRole.ACCENT_GLOW -> ElectricBlue
}

// Counter-clockwise wound cube faces for outward normals
private val CUBE_FACES = arrayOf(
    intArrayOf(0, 3, 2, 1), // -Z back
    intArrayOf(5, 6, 7, 4), // +Z front
    intArrayOf(4, 7, 3, 0), // -X left
    intArrayOf(1, 2, 6, 5), // +X right
    intArrayOf(3, 7, 6, 2), // +Y top
    intArrayOf(4, 0, 1, 5)  // -Y bottom
)

private fun cross(a: Vec3, b: Vec3): Vec3 = Vec3(
    a.y * b.z - a.z * b.y,
    a.z * b.x - a.x * b.z,
    a.x * b.y - a.y * b.x
)

private fun dot(a: Vec3, b: Vec3): Float = a.x * b.x + a.y * b.y + a.z * b.z

private fun normalize(v: Vec3): Vec3 {
    val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z).coerceAtLeast(1e-5f)
    return Vec3(v.x / len, v.y / len, v.z / len)
}
