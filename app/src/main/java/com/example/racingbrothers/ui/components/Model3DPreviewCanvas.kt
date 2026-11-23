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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.racingbrothers.characters.CharacterSpec
import com.example.racingbrothers.vehicles.LocalCorner3D
import com.example.racingbrothers.vehicles.PartColorRole
import com.example.racingbrothers.vehicles.VehicleAppearance
import com.example.racingbrothers.vehicles.VehiclePaintColor
import com.example.racingbrothers.vehicles.VehicleSpec
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

private enum class PreviewMaterialKind {
    METALLIC_PAINT,
    CARBON_FIBER,
    CHROME_METAL,
    GLASS,
    BRAKE_RED,
    EMISSIVE,
    MATTE_TRIM
}

private data class RenderBox3D(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val corners: Array<LocalCorner3D>,
    val color: Color,
    val pitchDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val material: PreviewMaterialKind = PreviewMaterialKind.MATTE_TRIM,
    val glowRadius: Float = 0f
)

private data class RenderWheel3D(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val width: Float,
    val radius: Float,
    val rimColor: Color,
    val accentColor: Color,
    val isFront: Boolean = false,
    val neonTreadColor: Color? = null,
    val brakeCaliperColor: Color = Color(0xFFE81932)
)

private data class ProjectedPoly(
    val points: List<Offset>,
    val depth: Float,
    val fillColor: Color,
    val strokeColor: Color,
    val strokeWidth: Float = 0.8f
)

private data class ProjectedBloom(
    val center: Offset,
    val depth: Float,
    val radiusPx: Float,
    val color: Color
)

private fun simpleBoxCorners(w: Float, h: Float, l: Float): Array<LocalCorner3D> {
    val hw = w * 0.5f
    val hh = h * 0.5f
    val hl = l * 0.5f
    return arrayOf(
        LocalCorner3D(-hw, -hh, -hl),
        LocalCorner3D(hw, -hh, -hl),
        LocalCorner3D(hw, hh, -hl),
        LocalCorner3D(-hw, hh, -hl),
        LocalCorner3D(-hw, -hh, hl),
        LocalCorner3D(hw, -hh, hl),
        LocalCorner3D(hw, hh, hl),
        LocalCorner3D(-hw, hh, hl)
    )
}

@Composable
fun Vehicle3DPreview(
    vehicle: VehicleSpec,
    paintColor: VehiclePaintColor,
    driver: CharacterSpec? = null,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
    interactiveDrag: Boolean = true,
    initialYawDeg: Float = 36f,
    initialPitchDeg: Float = 15f,
    zoomScale: Float = 1.0f,
    garageLighting: Boolean = true,
    showTurntable: Boolean = true,
    drawLocalBackdrop: Boolean = false,
    appearance: VehicleAppearance = VehicleAppearance()
) {
    val scene = remember(vehicle.id, paintColor.id, driver?.id, appearance) {
        val boxes = mutableListOf<RenderBox3D>()
        val wheels = mutableListOf<RenderWheel3D>()
        val neonColor = Color(vehicle.neonAccentArgb)
        for (part in appearance.applyTo(vehicle)) {
            if (part.isWheel) {
                wheels.add(
                    RenderWheel3D(
                        cx = part.centerX,
                        cy = part.centerY,
                        cz = part.centerZ,
                        width = part.width,
                        radius = part.height * 0.5f,
                        rimColor = Color(appearance.rimArgb),
                        accentColor = Color(appearance.rimAccentArgb),
                        isFront = part.isFrontWheel,
                        neonTreadColor = if (vehicle.neonWheelTread) neonColor else null
                    )
                )
            } else {
                val color = resolveVehiclePartColor(part.role, paintColor, vehicle.neonAccentArgb)
                val material = when (part.role) {
                    PartColorRole.PRIMARY_PAINT -> PreviewMaterialKind.METALLIC_PAINT
                    PartColorRole.CARBON_FIBER -> PreviewMaterialKind.CARBON_FIBER
                    PartColorRole.CHROME_METAL, PartColorRole.WHEEL_RIM -> PreviewMaterialKind.CHROME_METAL
                    PartColorRole.GLASS_TINT -> PreviewMaterialKind.GLASS
                    PartColorRole.BRAKE_ACCENT -> PreviewMaterialKind.BRAKE_RED
                    PartColorRole.HEADLIGHT, PartColorRole.TAILLIGHT, PartColorRole.ACCENT_GLOW -> PreviewMaterialKind.EMISSIVE
                    PartColorRole.SECONDARY_TRIM, PartColorRole.TIRE_RUBBER -> PreviewMaterialKind.MATTE_TRIM
                }
                val glowRadius = when {
                    part.role == PartColorRole.HEADLIGHT && part.id.startsWith("headlight") -> 0.54f
                    part.role == PartColorRole.TAILLIGHT && (part.id.startsWith("taillight") || part.id == "rain_light") -> 0.44f
                    part.role == PartColorRole.ACCENT_GLOW && part.id == "nose_emblem_glow" -> 0.34f
                    else -> 0f
                }
                boxes.add(
                    RenderBox3D(
                        cx = part.centerX,
                        cy = part.centerY,
                        cz = part.centerZ,
                        corners = part.computeLocalCorners(),
                        color = color,
                        pitchDeg = part.pitchDeg,
                        yawDeg = part.yawDeg,
                        rollDeg = part.rollDeg,
                        material = material,
                        glowRadius = glowRadius
                    )
                )
            }
        }
        if (driver != null) {
            val scale = 0.52f
            for (dp in driver.parts) {
                if (dp.centerY < 0.85f) continue
                boxes.add(
                    RenderBox3D(
                        cx = dp.centerX * scale,
                        cy = vehicle.cockpitY + (dp.centerY - 0.88f) * scale,
                        cz = vehicle.cockpitZ + dp.centerZ * scale,
                        corners = simpleBoxCorners(dp.width * scale, dp.height * scale, dp.length * scale),
                        color = Color(dp.argb),
                        material = if (dp.id.contains("visor") || dp.id.contains("goggle")) {
                            PreviewMaterialKind.EMISSIVE
                        } else {
                            PreviewMaterialKind.MATTE_TRIM
                        }
                    )
                )
            }
        }
        boxes to wheels
    }

    Interactive3DViewport(
        boxes = scene.first,
        wheels = scene.second,
        centerYOffset = 0.56f,
        cameraDistance = 6.85f / zoomScale,
        modifier = modifier,
        autoRotate = autoRotate,
        interactiveDrag = interactiveDrag,
        initialYawDeg = initialYawDeg,
        initialPitchDeg = initialPitchDeg,
        garageLighting = garageLighting,
        showTurntable = showTurntable,
        drawLocalBackdrop = drawLocalBackdrop
    )
}

@Composable
fun Character3DPreview(
    character: CharacterSpec,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
    interactiveDrag: Boolean = true,
    initialYawDeg: Float = 24f,
    initialPitchDeg: Float = 12f,
    showTurntable: Boolean = true
) {
    val boxes = remember(character.id) {
        character.parts.map { part ->
            RenderBox3D(
                cx = part.centerX,
                cy = part.centerY,
                cz = part.centerZ,
                corners = simpleBoxCorners(part.width, part.height, part.length),
                color = Color(part.argb),
                material = when {
                    part.id.contains("visor") || part.id.contains("goggle") -> PreviewMaterialKind.EMISSIVE
                    part.id.contains("helmet") -> PreviewMaterialKind.METALLIC_PAINT
                    else -> PreviewMaterialKind.MATTE_TRIM
                }
            )
        }
    }

    Interactive3DViewport(
        boxes = boxes,
        wheels = emptyList(),
        centerYOffset = 1.06f,
        cameraDistance = 4.5f,
        modifier = modifier,
        autoRotate = autoRotate,
        interactiveDrag = interactiveDrag,
        initialYawDeg = initialYawDeg,
        initialPitchDeg = initialPitchDeg,
        garageLighting = true,
        showTurntable = showTurntable,
        drawLocalBackdrop = false
    )
}

@Composable
private fun Interactive3DViewport(
    boxes: List<RenderBox3D>,
    wheels: List<RenderWheel3D>,
    centerYOffset: Float,
    cameraDistance: Float,
    modifier: Modifier,
    autoRotate: Boolean,
    interactiveDrag: Boolean,
    initialYawDeg: Float,
    initialPitchDeg: Float,
    garageLighting: Boolean,
    showTurntable: Boolean,
    drawLocalBackdrop: Boolean
) {
    var yawDeg by remember(initialYawDeg) { mutableFloatStateOf(initialYawDeg) }
    var pitchDeg by remember(initialPitchDeg) { mutableFloatStateOf(initialPitchDeg) }

    val turntableRotation = if (autoRotate) {
        rememberInfiniteTransition(label = "Vehicle turntable").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(15_000, easing = LinearEasing)),
            label = "Turntable rotation"
        )
    } else null

    val dragModifier = if (interactiveDrag) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                yawDeg = (yawDeg + dragAmount.x * 0.55f) % 360f
                pitchDeg = (pitchDeg + dragAmount.y * 0.25f).coerceIn(5f, 40f)
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
        val cy = h * if (showTurntable) 0.52f else 0.54f
        val focal = minOf(w, h) * if (showTurntable) 1.24f else 1.38f

        if (drawLocalBackdrop) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF040916), Color(0xFF0E1B34), Color(0xFF040812))))
        }

        val currentYawDeg = yawDeg + (turntableRotation?.value ?: 0f)

        if (showTurntable) {
            val baseRadiusX = minOf(w * 0.45f, h * 0.62f)
            val baseRadiusY = baseRadiusX * 0.27f
            drawNeonTurntablePedestal(
                cx = cx,
                cy = cy + minOf(w, h) * 0.22f,
                radiusX = baseRadiusX,
                radiusY = baseRadiusY,
                rotationDeg = currentYawDeg
            )
        } else {
            // Grounded soft shadow for compact 3D car cards
            val shadowRx = w * 0.40f
            val shadowRy = h * 0.14f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent),
                    center = Offset(cx, cy + h * 0.24f),
                    radius = shadowRx
                ),
                topLeft = Offset(cx - shadowRx, cy + h * 0.24f - shadowRy),
                size = Size(shadowRx * 2f, shadowRy * 2f)
            )
        }

        val yawRad = Math.toRadians(currentYawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)

        val keyBlueLightDir = normalize(Vec3(-0.50f, 0.82f, -0.34f))
        val rimRedLightDir = normalize(Vec3(0.76f, 0.24f, 0.44f))
        val fillTopDir = normalize(Vec3(0.06f, 0.96f, -0.24f))

        fun toView(wx: Float, wy: Float, wz: Float): Vec3 {
            val yRel = wy - centerYOffset
            val rx = wx * cosY + wz * sinY
            val rz0 = -wx * sinY + wz * cosY
            val ry = yRel * cosP - rz0 * sinP
            val rz = yRel * sinP + rz0 * cosP + cameraDistance
            return Vec3(rx, ry, rz)
        }

        fun project(v: Vec3): Offset = Offset(cx + (v.x / v.z) * focal, cy - (v.y / v.z) * focal)

        val polys = ArrayList<ProjectedPoly>(boxes.size * 6 + wheels.size * 64)
        val blooms = ArrayList<ProjectedBloom>(10)

        // 1. Project sculpted 8-corner body parts with metallic clearcoat, carbon, chrome, glass & emissive shading
        for (box in boxes) {
            val hasRotation = box.pitchDeg != 0f || box.yawDeg != 0f || box.rollDeg != 0f
            val viewCorners = if (!hasRotation) {
                Array(8) { idx ->
                    val c = box.corners[idx]
                    toView(c.x + box.cx, c.y + box.cy, c.z + box.cz)
                }
            } else {
                val pRad = Math.toRadians(box.pitchDeg.toDouble()).toFloat()
                val yRad = Math.toRadians(box.yawDeg.toDouble()).toFloat()
                val rRad = Math.toRadians(box.rollDeg.toDouble()).toFloat()
                val cp = cos(pRad)
                val sp = sin(pRad)
                val cyb = cos(yRad)
                val syb = sin(yRad)
                val cr = cos(rRad)
                val sr = sin(rRad)
                Array(8) { idx ->
                    val c = box.corners[idx]
                    // Ry(yaw) * Rx(pitch) * Rz(roll) — matches libGDX Quaternion.setEulerAngles(yaw, pitch, roll)
                    val x1 = c.x * cr - c.y * sr
                    val y1 = c.x * sr + c.y * cr
                    val z1 = c.z
                    val y2 = y1 * cp - z1 * sp
                    val z2 = y1 * sp + z1 * cp
                    val x3 = x1 * cyb + z2 * syb
                    val z3 = -x1 * syb + z2 * cyb
                    toView(x3 + box.cx, y2 + box.cy, z3 + box.cz)
                }
            }

            if (box.glowRadius > 0f && showTurntable) {
                val centerV = toView(box.cx, box.cy, box.cz)
                if (centerV.z > 0.6f && centerV.z < cameraDistance + 1.55f) {
                    blooms.add(
                        ProjectedBloom(
                            center = project(centerV),
                            depth = centerV.z,
                            radiusPx = (box.glowRadius / centerV.z) * focal,
                            color = box.color
                        )
                    )
                }
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

                val toCam = normalize(Vec3(-v0.x, -v0.y, -v0.z))
                val ndv = dot(normal, toCam)
                if (ndv <= 0f) continue

                val avgDepth = (v0.z + v1.z + v2.z + v3.z) * 0.25f

                val fill = shadeMaterial(
                    baseColor = box.color,
                    material = box.material,
                    normal = normal,
                    toCam = toCam,
                    ndv = ndv,
                    keyBlueLightDir = keyBlueLightDir,
                    rimRedLightDir = rimRedLightDir,
                    fillTopDir = fillTopDir,
                    garageLighting = garageLighting
                )

                val edgeColor = when (box.material) {
                    PreviewMaterialKind.EMISSIVE -> Color.White.copy(alpha = 0.62f)
                    PreviewMaterialKind.METALLIC_PAINT -> fill.copy(
                        red = (fill.red * 0.72f).coerceIn(0f, 1f),
                        green = (fill.green * 0.76f).coerceIn(0f, 1f),
                        blue = (fill.blue * 0.85f).coerceIn(0f, 1f),
                        alpha = 0.48f
                    )
                    PreviewMaterialKind.GLASS -> NeonCyan.copy(alpha = 0.32f)
                    PreviewMaterialKind.CHROME_METAL -> Color.White.copy(alpha = 0.42f)
                    PreviewMaterialKind.CARBON_FIBER -> Color(0xFF28374E).copy(alpha = 0.45f)
                    PreviewMaterialKind.BRAKE_RED -> Color(0xFF4A0610).copy(alpha = 0.45f)
                    PreviewMaterialKind.MATTE_TRIM -> Color.Black.copy(alpha = 0.28f)
                }

                polys.add(
                    ProjectedPoly(
                        points = listOf(project(v0), project(v1), project(v2), project(v3)),
                        depth = avgDepth,
                        fillColor = fill,
                        strokeColor = edgeColor,
                        strokeWidth = when (box.material) {
                            PreviewMaterialKind.METALLIC_PAINT, PreviewMaterialKind.GLASS -> 0.65f
                            PreviewMaterialKind.CHROME_METAL -> 0.75f
                            else -> 0.9f
                        }
                    )
                )
            }
        }

        // 2. Project detailed 16-sided cylindrical 3D racing wheels with showroom steer,
        //    rounded tire shading, brake rotors & crimson calipers, 10-spoke alloys,
        //    crimson center-lock hubs, and glowing neon tread bands on Formula Aero
        val wheelSides = 16
        val showFrontSteer = showTurntable
        for (wheel in wheels) {
            val halfW = wheel.width * 0.5f
            val isLeft = wheel.cx < 0f
            val steerDeg = if (showFrontSteer && wheel.isFront) 14f else 0f
            val steerRad = Math.toRadians(steerDeg.toDouble()).toFloat()
            val cosS = cos(steerRad)
            val sinS = sin(steerRad)

            fun wheelToView(lx: Float, ly: Float, lz: Float): Vec3 {
                val rx = lx * cosS + lz * sinS
                val rz = -lx * sinS + lz * cosS
                return toView(wheel.cx + rx, wheel.cy + ly, wheel.cz + rz)
            }

            val negRing = Array(wheelSides) { i ->
                val a = (2.0 * PI * i / wheelSides).toFloat()
                wheelToView(-halfW, sin(a) * wheel.radius, cos(a) * wheel.radius)
            }
            val posRing = Array(wheelSides) { i ->
                val a = (2.0 * PI * i / wheelSides).toFloat()
                wheelToView(halfW, sin(a) * wheel.radius, cos(a) * wheel.radius)
            }

            // Tire tread cylinder segments with proper radial lighting & optional neon tread bands
            val neonColor = wheel.neonTreadColor
            for (i in 0 until wheelSides) {
                val next = (i + 1) % wheelSides
                val v0 = negRing[i]
                val v1 = posRing[i]
                val v2 = posRing[next]
                val v3 = negRing[next]
                if (v0.z <= 0.35f || v1.z <= 0.35f || v2.z <= 0.35f || v3.z <= 0.35f) continue

                val e1 = Vec3(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z)
                val e2 = Vec3(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z)
                val radialNorm = normalize(cross(e1, e2))
                val toCam = normalize(Vec3(-v0.x, -v0.y, -v0.z))
                val ndv = dot(radialNorm, toCam)
                // Flip normal if wound inward; cull back tread faces
                val nrm = if (ndv < 0f) Vec3(-radialNorm.x, -radialNorm.y, -radialNorm.z) else radialNorm
                val key = dot(nrm, keyBlueLightDir).coerceIn(0f, 1f)
                val rim = dot(nrm, rimRedLightDir).coerceIn(0f, 1f)
                val depth = (v0.z + v1.z + v2.z + v3.z) * 0.25f

                val treadLum = 0.08f + key * 0.09f + ((i % 2) * 0.015f)
                val treadColor = Color(
                    red = (treadLum + rim * 0.06f).coerceIn(0f, 1f),
                    green = (treadLum + key * 0.03f).coerceIn(0f, 1f),
                    blue = (treadLum + 0.025f + key * 0.07f).coerceIn(0f, 1f),
                    alpha = 1f
                )
                polys.add(
                    ProjectedPoly(
                        points = listOf(project(v0), project(v1), project(v2), project(v3)),
                        depth = depth,
                        fillColor = treadColor,
                        strokeColor = Color.Black.copy(alpha = 0.38f),
                        strokeWidth = 0.65f
                    )
                )

                // Twin glowing neon tread bands with subtle futuristic chevron stagger (Reference 2!)
                if (neonColor != null) {
                    val a0 = (2.0 * PI * i / wheelSides).toFloat()
                    val a1 = (2.0 * PI * next / wheelSides).toFloat()
                    val neonR = wheel.radius * 1.016f
                    val zig0 = if (i % 2 == 0) 0.045f * wheel.width else -0.045f * wheel.width
                    val zig1 = if (next % 2 == 0) 0.045f * wheel.width else -0.045f * wheel.width
                    val bandHalf = wheel.width * 0.062f
                    for (bandSign in listOf(-1f, 1f)) {
                        val baseCenter = bandSign * wheel.width * 0.24f
                        val x0L = baseCenter + zig0 - bandHalf
                        val x0R = baseCenter + zig0 + bandHalf
                        val x1L = baseCenter + zig1 - bandHalf
                        val x1R = baseCenter + zig1 + bandHalf
                        val n0 = wheelToView(x0L, sin(a0) * neonR, cos(a0) * neonR)
                        val n1 = wheelToView(x0R, sin(a0) * neonR, cos(a0) * neonR)
                        val n2 = wheelToView(x1R, sin(a1) * neonR, cos(a1) * neonR)
                        val n3 = wheelToView(x1L, sin(a1) * neonR, cos(a1) * neonR)
                        if (n0.z > 0.35f && n1.z > 0.35f && n2.z > 0.35f && n3.z > 0.35f) {
                            polys.add(
                                ProjectedPoly(
                                    points = listOf(project(n0), project(n1), project(n2), project(n3)),
                                    depth = (n0.z + n1.z + n2.z + n3.z) * 0.25f - 0.012f,
                                    fillColor = neonColor,
                                    strokeColor = Color.White.copy(alpha = 0.55f),
                                    strokeWidth = 0.55f
                                )
                            )
                        }
                    }
                }
            }

            // Neon under-tire glow pool for Formula Aero on turntable
            if (neonColor != null && showTurntable) {
                val contactV = wheelToView(0f, -wheel.radius * 0.92f, 0f)
                if (contactV.z > 0.5f) {
                    blooms.add(
                        ProjectedBloom(
                            center = project(contactV),
                            depth = contactV.z + 0.1f,
                            radiusPx = (wheel.radius * 1.15f / contactV.z) * focal,
                            color = neonColor
                        )
                    )
                }
            }

            // Compute outward/inward wheel face visibility
            val centerV = wheelToView(0f, 0f, 0f)
            val outSign = if (isLeft) -1f else 1f
            val outFaceX = outSign * halfW
            val outFaceCenter = wheelToView(outFaceX, 0f, 0f)
            val outNormal = normalize(
                Vec3(
                    outFaceCenter.x - centerV.x,
                    outFaceCenter.y - centerV.y,
                    outFaceCenter.z - centerV.z
                )
            )
            val toOutCam = normalize(Vec3(-outFaceCenter.x, -outFaceCenter.y, -outFaceCenter.z))
            val outFacing = dot(outNormal, toOutCam)

            val outSideRing = if (isLeft) negRing else posRing
            val inSideRing = if (isLeft) posRing else negRing

            // Inward sidewall disc (keeps open-wheel tires solid from 360° views)
            if (outFacing <= 0.05f && inSideRing.all { it.z > 0.35f }) {
                val inDepth = inSideRing.map { it.z }.average().toFloat() - 0.015f
                polys.add(
                    ProjectedPoly(
                        points = inSideRing.map { project(it) },
                        depth = inDepth,
                        fillColor = Color(0xFF10141E),
                        strokeColor = Color(0xFF070A10),
                        strokeWidth = 0.9f
                    )
                )
            }

            // Outward racing wheel face: sidewall -> accent lip -> dark barrel -> brake rotor -> crimson caliper -> 10 alloy spokes -> crimson center-lock hub
            if (outFacing > -0.02f && outSideRing.all { it.z > 0.35f }) {
                val sideDepth = outSideRing.map { it.z }.average().toFloat() - 0.018f
                val faceShade = shadeMaterial(
                    baseColor = wheel.rimColor,
                    material = PreviewMaterialKind.CHROME_METAL,
                    normal = outNormal,
                    toCam = toOutCam,
                    ndv = outFacing.coerceAtLeast(0.05f),
                    keyBlueLightDir = keyBlueLightDir,
                    rimRedLightDir = rimRedLightDir,
                    fillTopDir = fillTopDir,
                    garageLighting = garageLighting
                )

                // 1. Tire sidewall
                polys.add(
                    ProjectedPoly(
                        points = outSideRing.map { project(it) },
                        depth = sideDepth,
                        fillColor = Color(0xFF131722),
                        strokeColor = Color(0xFF080B12),
                        strokeWidth = 1.0f
                    )
                )

                // 2. Outer metallic rim barrel + colored accent lip ring
                val lipX = outFaceX + outSign * 0.008f
                val lipRadius = wheel.radius * 0.74f
                val lipPts = List(wheelSides) { i ->
                    val a = (2.0 * PI * i / wheelSides).toFloat()
                    project(wheelToView(lipX, sin(a) * lipRadius, cos(a) * lipRadius))
                }
                polys.add(
                    ProjectedPoly(
                        points = lipPts,
                        depth = sideDepth - 0.014f,
                        fillColor = Color(0xFF121926),
                        strokeColor = wheel.accentColor,
                        strokeWidth = if (showTurntable) 2.4f else 1.5f
                    )
                )

                // 3. Recessed brushed-steel brake rotor disc (visible behind spokes!)
                val rotorX = outFaceX - outSign * 0.012f
                val rotorRadius = wheel.radius * 0.52f
                val rotorPts = List(12) { i ->
                    val a = (2.0 * PI * i / 12f).toFloat()
                    project(wheelToView(rotorX, sin(a) * rotorRadius, cos(a) * rotorRadius))
                }
                polys.add(
                    ProjectedPoly(
                        points = rotorPts,
                        depth = sideDepth - 0.020f,
                        fillColor = Color(0xFF3A4658),
                        strokeColor = Color(0xFF222B3A),
                        strokeWidth = 0.8f
                    )
                )

                // 4. Anodized crimson brake caliper arc wedge on upper-rear quadrant
                val caliperX = outFaceX + outSign * 0.004f
                val calInnerR = wheel.radius * 0.34f
                val calOuterR = wheel.radius * 0.56f
                val calStart = 2.15f
                val calEnd = 3.25f
                val calSteps = 4
                val calPts = buildList {
                    for (s in 0..calSteps) {
                        val a = calStart + (calEnd - calStart) * (s.toFloat() / calSteps)
                        add(project(wheelToView(caliperX, sin(a) * calOuterR, cos(a) * calOuterR)))
                    }
                    for (s in calSteps downTo 0) {
                        val a = calStart + (calEnd - calStart) * (s.toFloat() / calSteps)
                        add(project(wheelToView(caliperX, sin(a) * calInnerR, cos(a) * calInnerR)))
                    }
                }
                polys.add(
                    ProjectedPoly(
                        points = calPts,
                        depth = sideDepth - 0.026f,
                        fillColor = wheel.brakeCaliperColor,
                        strokeColor = Color(0xFF6B0918),
                        strokeWidth = 0.8f
                    )
                )

                // 5. Crisp 10-spoke GT3 motorsport star alloys (5 twin-spoke pairs)
                val spokeX = outFaceX + outSign * 0.018f
                val innerR = wheel.radius * 0.16f
                val outerR = wheel.radius * 0.70f
                val pairCount = 5
                val twinOffset = 0.135f
                val spokeHalfAng = 0.052f
                for (p in 0 until pairCount) {
                    val baseAngle = (2.0 * PI * p / pairCount).toFloat() + 0.18f
                    for (sub in listOf(-twinOffset, twinOffset)) {
                        val aMid = baseAngle + sub
                        val p0 = project(wheelToView(spokeX, sin(aMid - spokeHalfAng * 1.35f) * innerR, cos(aMid - spokeHalfAng * 1.35f) * innerR))
                        val p1 = project(wheelToView(spokeX, sin(aMid + spokeHalfAng * 1.35f) * innerR, cos(aMid + spokeHalfAng * 1.35f) * innerR))
                        val p2 = project(wheelToView(spokeX, sin(aMid + spokeHalfAng) * outerR, cos(aMid + spokeHalfAng) * outerR))
                        val p3 = project(wheelToView(spokeX, sin(aMid - spokeHalfAng) * outerR, cos(aMid - spokeHalfAng) * outerR))
                        polys.add(
                            ProjectedPoly(
                                points = listOf(p0, p1, p2, p3),
                                depth = sideDepth - 0.034f,
                                fillColor = faceShade,
                                strokeColor = Color(0xFF0E1624).copy(alpha = 0.55f),
                                strokeWidth = 0.65f
                            )
                        )
                    }
                }

                // 6. Anodized crimson center-lock hub nut + silver lock core
                val hubX = outFaceX + outSign * 0.026f
                val hubRadius = wheel.radius * 0.18f
                val hubPts = List(6) { i ->
                    val a = (2.0 * PI * i / 6f).toFloat()
                    project(wheelToView(hubX, sin(a) * hubRadius, cos(a) * hubRadius))
                }
                polys.add(
                    ProjectedPoly(
                        points = hubPts,
                        depth = sideDepth - 0.042f,
                        fillColor = wheel.brakeCaliperColor,
                        strokeColor = Color(0xFF220910),
                        strokeWidth = 0.9f
                    )
                )
                val capRadius = wheel.radius * 0.075f
                val capPts = List(6) { i ->
                    val a = (2.0 * PI * i / 6f).toFloat()
                    project(wheelToView(hubX + outSign * 0.005f, sin(a) * capRadius, cos(a) * capRadius))
                }
                polys.add(
                    ProjectedPoly(
                        points = capPts,
                        depth = sideDepth - 0.048f,
                        fillColor = Color(0xFFE5EEF9),
                        strokeColor = Color(0xFF1A2233),
                        strokeWidth = 0.6f
                    )
                )
            }
        }

        // Painter's algorithm: farthest first
        polys.sortByDescending { it.depth }

        val path = Path()
        for (poly in polys) {
            if (poly.points.size < 3) continue
            path.reset()
            path.moveTo(poly.points[0].x, poly.points[0].y)
            for (i in 1 until poly.points.size) {
                path.lineTo(poly.points[i].x, poly.points[i].y)
            }
            path.close()
            drawPath(path = path, color = poly.fillColor)
            drawPath(path = path, color = poly.strokeColor, style = Stroke(width = poly.strokeWidth))
        }

        // Soft glowing headlight/taillight & Formula neon tire bloom overlays
        for (bloom in blooms) {
            if (bloom.radiusPx > 4f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f),
                            bloom.color.copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        center = bloom.center,
                        radius = bloom.radiusPx
                    ),
                    radius = bloom.radiusPx,
                    center = bloom.center
                )
            }
        }
    }
}

private fun shadeMaterial(
    baseColor: Color,
    material: PreviewMaterialKind,
    normal: Vec3,
    toCam: Vec3,
    ndv: Float,
    keyBlueLightDir: Vec3,
    rimRedLightDir: Vec3,
    fillTopDir: Vec3,
    garageLighting: Boolean
): Color {
    if (material == PreviewMaterialKind.EMISSIVE) return baseColor

    val keyBlue = dot(normal, keyBlueLightDir).coerceIn(0f, 1f)
    val topFill = dot(normal, fillTopDir).coerceIn(0f, 1f)
    val rimRed = dot(normal, rimRedLightDir).coerceIn(0f, 1f)

    val halfKey = normalize(
        Vec3(
            keyBlueLightDir.x + toCam.x,
            keyBlueLightDir.y + toCam.y,
            keyBlueLightDir.z + toCam.z
        )
    )
    val halfRim = normalize(
        Vec3(
            rimRedLightDir.x + toCam.x,
            rimRedLightDir.y + toCam.y,
            rimRedLightDir.z + toCam.z
        )
    )
    val ndhKey = dot(normal, halfKey).coerceIn(0f, 1f)
    val ndhRim = dot(normal, halfRim).coerceIn(0f, 1f)
    val fresnelBase = (1f - ndv.coerceIn(0f, 1f)).pow(3f)

    val garageScale = if (garageLighting) 1f else 0.38f

    return when (material) {
        PreviewMaterialKind.METALLIC_PAINT -> {
            // Dual-lobe metallic clearcoat (broad metallic sheen + crisp studio highlight)
            val broadSpec = ndhKey.pow(8f) * 0.24f
            val sharpSpec = ndhKey.pow(28f) * 0.56f
            val rimSpec = ndhRim.pow(18f) * 0.28f * garageScale
            val fresnel = fresnelBase * 0.36f
            val baseIntensity = (0.32f + keyBlue * 0.58f + topFill * 0.24f).coerceIn(0.24f, 1.14f)
            val blueTint = (keyBlue * 0.14f + fresnel * 0.46f) * garageScale
            val redTint = (rimRed * 0.44f + fresnel * 0.28f) * garageScale
            Color(
                red = (baseColor.red * baseIntensity + redTint * 0.96f + broadSpec * baseColor.red + sharpSpec + rimSpec).coerceIn(0f, 1f),
                green = (baseColor.green * baseIntensity + blueTint * 0.55f + redTint * 0.12f + broadSpec * baseColor.green + sharpSpec + rimSpec * 0.2f).coerceIn(0f, 1f),
                blue = (baseColor.blue * baseIntensity + blueTint * 1.08f + redTint * 0.34f + broadSpec * baseColor.blue + sharpSpec * 1.06f + rimSpec * 0.35f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        PreviewMaterialKind.GLASS -> {
            // Smoked sapphire canopy with bright overhead studio reflection streak
            val spec = ndhKey.pow(24f) * 0.78f + ndhRim.pow(16f) * 0.28f * garageScale
            val fresnel = fresnelBase * 0.52f
            val baseIntensity = 0.30f + topFill * 0.32f + keyBlue * 0.26f
            Color(
                red = (baseColor.red * baseIntensity + spec * 0.92f + fresnel * 0.22f * garageScale).coerceIn(0f, 1f),
                green = (baseColor.green * baseIntensity + spec * 0.97f + fresnel * 0.44f * garageScale).coerceIn(0f, 1f),
                blue = (baseColor.blue * baseIntensity + spec * 1.05f + fresnel * 0.76f * garageScale).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        PreviewMaterialKind.CHROME_METAL -> {
            // High-reflectance brushed titanium / chrome metal
            val spec = ndhKey.pow(20f) * 0.68f + ndhRim.pow(14f) * 0.34f * garageScale
            val fresnel = fresnelBase * 0.28f
            val baseIntensity = (0.42f + keyBlue * 0.44f + topFill * 0.26f).coerceIn(0.32f, 1.08f)
            Color(
                red = (baseColor.red * baseIntensity + spec + rimRed * 0.22f * garageScale).coerceIn(0f, 1f),
                green = (baseColor.green * baseIntensity + spec + fresnel * 0.20f).coerceIn(0f, 1f),
                blue = (baseColor.blue * baseIntensity + spec * 1.05f + fresnel * 0.35f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        PreviewMaterialKind.CARBON_FIBER -> {
            // Sleek dark satin carbon fiber that catches crisp specular highlights
            val spec = ndhKey.pow(14f) * 0.28f + ndhRim.pow(12f) * 0.14f * garageScale
            val fresnel = fresnelBase * 0.16f
            val baseIntensity = 0.48f + keyBlue * 0.46f + topFill * 0.24f
            Color(
                red = (baseColor.red * baseIntensity + spec * 0.85f + rimRed * 0.16f * garageScale).coerceIn(0f, 1f),
                green = (baseColor.green * baseIntensity + spec * 0.92f + fresnel * 0.18f).coerceIn(0f, 1f),
                blue = (baseColor.blue * baseIntensity + spec * 1.05f + fresnel * 0.34f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        PreviewMaterialKind.BRAKE_RED -> {
            // Glossy anodized racing crimson
            val spec = ndhKey.pow(18f) * 0.45f
            val baseIntensity = 0.42f + keyBlue * 0.48f + topFill * 0.20f
            Color(
                red = (baseColor.red * baseIntensity + spec).coerceIn(0f, 1f),
                green = (baseColor.green * baseIntensity + spec * 0.72f).coerceIn(0f, 1f),
                blue = (baseColor.blue * baseIntensity + spec * 0.72f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        else -> {
            val baseIntensity = (0.36f + keyBlue * 0.52f + topFill * 0.20f).coerceIn(0.24f, 1.08f)
            val blueTint = keyBlue * 0.08f * garageScale
            val redTint = rimRed * 0.22f * garageScale
            Color(
                red = (baseColor.red * baseIntensity + redTint * 0.85f).coerceIn(0f, 1f),
                green = (baseColor.green * baseIntensity + blueTint * 0.50f + redTint * 0.10f).coerceIn(0f, 1f),
                blue = (baseColor.blue * baseIntensity + blueTint * 0.95f + redTint * 0.25f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
    }
}

private fun DrawScope.drawNeonTurntablePedestal(
    cx: Float,
    cy: Float,
    radiusX: Float,
    radiusY: Float,
    rotationDeg: Float
) {
    // 1. Wide ambient floor reflection (blue + crimson tint)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                ElectricBlue.copy(alpha = 0.34f),
                ElectricBlue.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = Offset(cx, cy + radiusY * 0.25f),
            radius = radiusX * 1.32f
        ),
        topLeft = Offset(cx - radiusX * 1.32f, cy - radiusY * 1.25f),
        size = Size(radiusX * 2.64f, radiusY * 2.95f)
    )

    // 2. 3D raised metallic pedestal rim thickness
    val rimThickness = (radiusY * 0.30f).coerceIn(6f, 16f)
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF101D35), Color(0xFF040812)),
            startY = cy - radiusY,
            endY = cy + radiusY + rimThickness
        ),
        topLeft = Offset(cx - radiusX, cy - radiusY + rimThickness),
        size = Size(radiusX * 2f, radiusY * 2f)
    )

    // 3. Dark reflective turntable deck surface
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF142544),
                Color(0xFF091326),
                Color(0xFF050A16)
            ),
            center = Offset(cx, cy),
            radius = radiusX
        ),
        topLeft = Offset(cx - radiusX, cy - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f)
    )

    // 4. Crimson subtle reflection on right edge of turntable deck
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(NeonCrimson.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(cx + radiusX * 0.55f, cy),
            radius = radiusX * 0.55f
        ),
        topLeft = Offset(cx - radiusX, cy - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f)
    )

    // 5. Outer glowing continuous cyan ring + segmented bright LED strips along pedestal edge
    drawOval(
        color = ElectricBlue.copy(alpha = 0.35f),
        topLeft = Offset(cx - radiusX * 1.02f, cy - radiusY * 1.02f + rimThickness * 0.45f),
        size = Size(radiusX * 2.04f, radiusY * 2.04f),
        style = Stroke(width = 7f)
    )
    val arcTopLeft = Offset(cx - radiusX, cy - radiusY + rimThickness * 0.45f)
    val arcSize = Size(radiusX * 2f, radiusY * 2f)
    val segCount = 6
    val step = 360f / segCount
    for (i in 0 until segCount) {
        val startAngle = (i * step + rotationDeg * 0.5f) % 360f
        drawArc(
            color = NeonCyan.copy(alpha = 0.92f),
            startAngle = startAngle,
            sweepAngle = step * 0.64f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 3.4f, cap = StrokeCap.Round)
        )
    }

    // 6. Inner technical ring & soft car contact shadow
    drawOval(
        color = ElectricBlue.copy(alpha = 0.30f),
        topLeft = Offset(cx - radiusX * 0.78f, cy - radiusY * 0.78f),
        size = Size(radiusX * 1.56f, radiusY * 1.56f),
        style = Stroke(width = 1.4f)
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.68f), Color.Black.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radiusX * 0.68f
        ),
        topLeft = Offset(cx - radiusX * 0.68f, cy - radiusY * 0.62f),
        size = Size(radiusX * 1.36f, radiusY * 1.24f)
    )
}

private fun resolveVehiclePartColor(
    role: PartColorRole,
    paint: VehiclePaintColor,
    neonAccentArgb: Long
): Color = when (role) {
    PartColorRole.PRIMARY_PAINT -> Color(paint.argb)
    PartColorRole.SECONDARY_TRIM -> Color(0xFF151E2E)
    PartColorRole.CARBON_FIBER -> Color(0xFF18202D)
    PartColorRole.CHROME_METAL -> Color(0xFFC5D2E3)
    PartColorRole.GLASS_TINT -> Color(0xFF152A44)
    PartColorRole.HEADLIGHT -> Color(0xFFEAF9FF)
    PartColorRole.TAILLIGHT -> Color(0xFFFF2447)
    PartColorRole.BRAKE_ACCENT -> Color(0xFFE81932)
    PartColorRole.TIRE_RUBBER -> Color(0xFF141822)
    PartColorRole.WHEEL_RIM -> Color(0xFFC8D7EA)
    PartColorRole.ACCENT_GLOW -> Color(neonAccentArgb)
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
