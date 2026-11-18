package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.domain.model.MascotPose

/**
 * High-precision 3D-shaded Compose Canvas renderer for the official MANO V1.0 Robot Mascot
 * inspired directly by the MANO V1.0 Character Reference Sheet (روبرو | سه‌رخ):
 * - Creamy pearl-white 3D oval head & plump torso with warm subsurface shading
 * - Vibrant Warm Orange-Amber metallic visor bezel (#FF7A18 -> #FFB300)
 * - Deep glossy obsidian screen with curved top glass reflection
 * - Luminous glowing ring-eyes with crisp white top-left catchlight dots & sweet glowing smile
 * - Lush single 3D organic leaf sprout on top of the head with golden-orange sunlit rim
 * - Side 3D ear pods with orange-amber trim
 * - Iconic 3D Orange-Gold "M+" emblem with diamond droplet on the chest
 * - Pose-specific expressive gestures for WELCOME, LISTEN, EXPLAIN, CAUTION, SAVED, and WAIT
 */
@Composable
fun ManoV1OrangeCharacterCanvas(
  pose: MascotPose,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val orangeHighlight = Color(0xFFFFC247)
    val orangeCore = Color(0xFFFF7A18)
    val orangeDeep = Color(0xFFD64E00)
    val emeraldGlow = Color(0xFF3EF598)
    val emeraldCore = Color(0xFF14B866)
    val emeraldDeep = Color(0xFF0A753F)
    val leafLight = Color(0xFFA6E22E)
    val leafMid = Color(0xFF68B818)
    val leafDark = Color(0xFF3E780A)

    val eyeGlowColor =
      when (pose) {
        MascotPose.CAUTION -> Color(0xFFFFA726)
        else -> emeraldGlow
      }

    // 1. Warm Orange & Soft Sky-Mint radial aura behind character
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              orangeCore.copy(alpha = 0.22f),
              Color(0xFF8CE8FF).copy(alpha = 0.18f),
              Color.Transparent,
            ),
          center = Offset(w * 0.50f, h * 0.48f),
          radius = w * 0.48f,
        ),
      center = Offset(w * 0.50f, h * 0.48f),
      radius = w * 0.48f,
    )

    // 2. Soft floor contact shadow
    drawOval(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0x38522600), Color.Transparent),
          center = Offset(w * 0.50f, h * 0.91f),
          radius = w * 0.28f,
        ),
      topLeft = Offset(w * 0.24f, h * 0.87f),
      size = Size(w * 0.52f, h * 0.08f),
    )

    // 3. Cute 3D Rounded Boots / Feet + Orange-Emerald Ankle Rings
    listOf(w * 0.34f, w * 0.52f).forEach { footLeft ->
      drawRoundRect(
        color = emeraldCore,
        topLeft = Offset(footLeft + w * 0.015f, h * 0.80f),
        size = Size(w * 0.11f, h * 0.035f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
      )
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3ECE4), Color(0xFFDDD2C5)),
            start = Offset(footLeft, h * 0.82f),
            end = Offset(footLeft + w * 0.14f, h * 0.91f),
          ),
        topLeft = Offset(footLeft, h * 0.825f),
        size = Size(w * 0.14f, h * 0.075f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
    }

    // 4. Left & Right Segmented Arms (Right arm waves/presents in WELCOME/EXPLAIN/LISTEN)
    // Left resting arm
    rotate(degrees = 14f, pivot = Offset(w * 0.26f, h * 0.60f)) {
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBE2D8)),
            start = Offset(w * 0.16f, h * 0.58f),
            end = Offset(w * 0.28f, h * 0.78f),
          ),
        topLeft = Offset(w * 0.17f, h * 0.58f),
        size = Size(w * 0.11f, h * 0.19f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f),
      )
      // Orange-gold + green wrist cuff
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(orangeHighlight, orangeCore, emeraldCore)),
        topLeft = Offset(w * 0.17f, h * 0.70f),
        size = Size(w * 0.11f, h * 0.026f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    // Right expressive waving / presenting arm (سه‌رخ)
    val rightArmAngle =
      when (pose) {
        MascotPose.WELCOME, MascotPose.EXPLAIN -> -38f
        MascotPose.CAUTION -> -48f
        MascotPose.LISTEN -> -24f
        else -> -18f
      }
    rotate(degrees = rightArmAngle, pivot = Offset(w * 0.74f, h * 0.60f)) {
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBE2D8)),
            start = Offset(w * 0.72f, h * 0.55f),
            end = Offset(w * 0.84f, h * 0.76f),
          ),
        topLeft = Offset(w * 0.72f, h * 0.56f),
        size = Size(w * 0.11f, h * 0.19f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f),
      )
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(emeraldCore, orangeCore, orangeHighlight)),
        topLeft = Offset(w * 0.72f, h * 0.68f),
        size = Size(w * 0.11f, h * 0.026f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    // 5. Plump Creamy Pearl-White 3D Torso
    drawOval(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFFFFFFFF),
              Color(0xFFFBF7F2),
              Color(0xFFEAE0D5),
            ),
          center = Offset(w * 0.45f, h * 0.64f),
          radius = w * 0.26f,
        ),
      topLeft = Offset(w * 0.27f, h * 0.55f),
      size = Size(w * 0.46f, h * 0.29f),
    )
    // Shoulder emerald-orange rings
    drawArc(
      color = emeraldCore,
      startAngle = 290f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(w * 0.25f, h * 0.57f),
      size = Size(w * 0.08f, h * 0.14f),
      style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
    )
    drawArc(
      color = emeraldCore,
      startAngle = 110f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(w * 0.67f, h * 0.57f),
      size = Size(w * 0.08f, h * 0.14f),
      style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
    )

    // Neck Metallic Orange-Amber & Emerald Collar Ring
    drawRoundRect(
      brush =
        Brush.horizontalGradient(
          colors = listOf(orangeDeep, orangeCore, orangeHighlight, orangeCore, orangeDeep)
        ),
      topLeft = Offset(w * 0.39f, h * 0.535f),
      size = Size(w * 0.22f, h * 0.036f),
      cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
    )
    drawRoundRect(
      color = emeraldCore,
      topLeft = Offset(w * 0.38f, h * 0.562f),
      size = Size(w * 0.24f, h * 0.018f),
      cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
    )

    // 6. Iconic "M+" Chest Emblem (Warm Metallic Orange-Gold M + Diamond Droplet + Emerald Plus)
    val mPath =
      Path().apply {
        moveTo(w * 0.415f, h * 0.735f)
        lineTo(w * 0.415f, h * 0.650f)
        lineTo(w * 0.475f, h * 0.705f)
        lineTo(w * 0.535f, h * 0.650f)
        lineTo(w * 0.535f, h * 0.735f)
      }
    drawPath(
      path = mPath,
      brush =
        Brush.verticalGradient(
          colors = listOf(orangeHighlight, orangeCore, orangeDeep),
          startY = h * 0.64f,
          endY = h * 0.74f,
        ),
      style = Stroke(width = w * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    // Diamond droplet above center of M
    val diamondPath =
      Path().apply {
        moveTo(w * 0.475f, h * 0.612f)
        lineTo(w * 0.498f, h * 0.638f)
        lineTo(w * 0.475f, h * 0.662f)
        lineTo(w * 0.452f, h * 0.638f)
        close()
      }
    drawPath(
      path = diamondPath,
      brush = Brush.verticalGradient(listOf(orangeHighlight, orangeCore)),
    )
    // "+" Medical Cross on top-right of M
    val plusCx = w * 0.575f
    val plusCy = h * 0.642f
    val plusArm = w * 0.024f
    drawLine(
      color = emeraldCore,
      start = Offset(plusCx - plusArm, plusCy),
      end = Offset(plusCx + plusArm, plusCy),
      strokeWidth = w * 0.018f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = emeraldCore,
      start = Offset(plusCx, plusCy - plusArm),
      end = Offset(plusCx, plusCy + plusArm),
      strokeWidth = w * 0.018f,
      cap = StrokeCap.Round,
    )

    // 7. Side 3D Ear Pods with Orange-Amber Accent Ring
    listOf(w * 0.09f, w * 0.81f).forEach { earLeft ->
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE5DBD0)),
            start = Offset(earLeft, h * 0.30f),
            end = Offset(earLeft + w * 0.10f, h * 0.46f),
          ),
        topLeft = Offset(earLeft, h * 0.31f),
        size = Size(w * 0.10f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = orangeCore.copy(alpha = 0.75f),
        topLeft = Offset(earLeft + w * 0.02f, h * 0.335f),
        size = Size(w * 0.06f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        style = Stroke(width = w * 0.012f),
      )
    }

    // 8. Lush Single 3D Top Leaf Sprout (tilted right like the reference sheet)
    val stemPath =
      Path().apply {
        moveTo(w * 0.48f, h * 0.19f)
        quadraticBezierTo(w * 0.47f, h * 0.13f, w * 0.51f, h * 0.10f)
      }
    drawPath(
      path = stemPath,
      color = leafDark,
      style = Stroke(width = w * 0.026f, cap = StrokeCap.Round),
    )
    val leafPath =
      Path().apply {
        moveTo(w * 0.48f, h * 0.14f)
        cubicTo(w * 0.47f, h * 0.05f, w * 0.58f, h * 0.02f, w * 0.67f, h * 0.04f)
        cubicTo(w * 0.65f, h * 0.13f, w * 0.56f, h * 0.17f, w * 0.48f, h * 0.14f)
        close()
      }
    drawPath(
      path = leafPath,
      brush =
        Brush.linearGradient(
          colors = listOf(leafLight, leafMid, leafDark),
          start = Offset(w * 0.48f, h * 0.03f),
          end = Offset(w * 0.66f, h * 0.16f),
        ),
    )
    // Golden-orange sunlit rim & leaf central vein
    drawPath(
      path = leafPath,
      color = orangeHighlight.copy(alpha = 0.65f),
      style = Stroke(width = w * 0.010f),
    )
    val veinPath =
      Path().apply {
        moveTo(w * 0.495f, h * 0.132f)
        quadraticBezierTo(w * 0.56f, h * 0.09f, w * 0.64f, h * 0.055f)
      }
    drawPath(
      path = veinPath,
      color = Color(0xFFD9F99D).copy(alpha = 0.85f),
      style = Stroke(width = w * 0.011f, cap = StrokeCap.Round),
    )

    // 9. Big Rounded Pearl-White 3D Robot Head
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFFFFFFFF),
              Color(0xFFFCF9F5),
              Color(0xFFE9DFD3),
            ),
          center = Offset(w * 0.42f, h * 0.26f),
          radius = w * 0.42f,
        ),
      topLeft = Offset(w * 0.14f, h * 0.17f),
      size = Size(w * 0.72f, h * 0.38f),
      cornerRadius = CornerRadius(w * 0.20f, w * 0.20f),
    )
    drawRoundRect(
      color = Color(0xFFFFFFFF).copy(alpha = 0.85f),
      topLeft = Offset(w * 0.14f, h * 0.17f),
      size = Size(w * 0.72f, h * 0.38f),
      cornerRadius = CornerRadius(w * 0.20f, w * 0.20f),
      style = Stroke(width = w * 0.012f),
    )

    // 10. Vibrant Warm Metallic Orange-Amber 3D Visor Bezel (#FF7A18 -> #FFB300)
    drawRoundRect(
      brush =
        Brush.linearGradient(
          colors =
            listOf(
              orangeHighlight,
              orangeCore,
              orangeDeep,
              orangeCore,
              orangeHighlight,
            ),
          start = Offset(w * 0.20f, h * 0.21f),
          end = Offset(w * 0.80f, h * 0.52f),
        ),
      topLeft = Offset(w * 0.20f, h * 0.215f),
      size = Size(w * 0.60f, h * 0.295f),
      cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
    )
    // Specular orange-gold outer bevel highlight
    drawRoundRect(
      color = Color(0xFFFFE599).copy(alpha = 0.85f),
      topLeft = Offset(w * 0.208f, h * 0.222f),
      size = Size(w * 0.584f, h * 0.281f),
      cornerRadius = CornerRadius(w * 0.145f, w * 0.145f),
      style = Stroke(width = w * 0.010f),
    )

    // 11. Deep Glossy Obsidian Visor Screen
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFF1A2826),
              Color(0xFF0C1514),
              Color(0xFF050A09),
            ),
          center = Offset(w * 0.50f, h * 0.36f),
          radius = w * 0.32f,
        ),
      topLeft = Offset(w * 0.232f, h * 0.242f),
      size = Size(w * 0.536f, h * 0.241f),
      cornerRadius = CornerRadius(w * 0.125f, w * 0.125f),
    )

    // Top-left curved glass reflection on black visor
    val glassReflection =
      Path().apply {
        moveTo(w * 0.27f, h * 0.29f)
        quadraticBezierTo(w * 0.38f, h * 0.255f, w * 0.54f, h * 0.268f)
      }
    drawPath(
      path = glassReflection,
      color = Color.White.copy(alpha = 0.24f),
      style = Stroke(width = w * 0.020f, cap = StrokeCap.Round),
    )

    // 12. Adorable Glowing Ring-Eyes with White Top-Left Catchlights (Exact MANO V1.0 Signature)
    val leftEyeCenter = Offset(w * 0.375f, h * 0.355f)
    val rightEyeCenter = Offset(w * 0.625f, h * 0.355f)
    val eyeRadius = w * 0.056f

    listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
      // Soft outer neon bloom
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(eyeGlowColor.copy(alpha = 0.48f), Color.Transparent),
            center = center,
            radius = eyeRadius * 1.65f,
          ),
        center = center,
        radius = eyeRadius * 1.65f,
      )
      // Glowing neon ring
      drawCircle(
        color = eyeGlowColor,
        center = center,
        radius = eyeRadius,
        style = Stroke(width = w * 0.022f),
      )
      // Dark pupil core
      drawCircle(
        color = Color(0xFF07110E),
        center = center,
        radius = eyeRadius * 0.72f,
      )
      // Crisp white top-left specular catchlight dot
      drawCircle(
        color = Color.White,
        center = Offset(center.x - eyeRadius * 0.32f, center.y - eyeRadius * 0.32f),
        radius = eyeRadius * 0.26f,
      )
    }

    // Warm orange rosy digital cheeks inside visor
    drawOval(
      color = orangeCore.copy(alpha = 0.35f),
      topLeft = Offset(w * 0.275f, h * 0.395f),
      size = Size(w * 0.065f, h * 0.032f),
    )
    drawOval(
      color = orangeCore.copy(alpha = 0.35f),
      topLeft = Offset(w * 0.660f, h * 0.395f),
      size = Size(w * 0.065f, h * 0.032f),
    )

    // 13. Cute Glowing Smile Curve between/below the eyes
    val smilePath =
      Path().apply {
        when (pose) {
          MascotPose.CAUTION -> {
            moveTo(w * 0.455f, h * 0.415f)
            quadraticBezierTo(w * 0.50f, h * 0.398f, w * 0.545f, h * 0.415f)
          }
          else -> {
            moveTo(w * 0.445f, h * 0.395f)
            quadraticBezierTo(w * 0.50f, h * 0.438f, w * 0.555f, h * 0.395f)
          }
        }
      }
    drawPath(
      path = smilePath,
      color = eyeGlowColor,
      style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
    )

    // 14. Pose-specific mini floating badge (top-right corner)
    val badgeCenter = Offset(w * 0.83f, h * 0.17f)
    val badgeColor =
      when (pose) {
        MascotPose.CAUTION -> orangeCore
        MascotPose.SAVED -> emeraldCore
        else -> orangeCore
      }
    drawCircle(
      color = badgeColor.copy(alpha = 0.22f),
      center = badgeCenter,
      radius = w * 0.085f,
    )
    drawCircle(
      color = badgeColor,
      center = badgeCenter,
      radius = w * 0.055f,
    )
    drawCircle(
      color = Color.White,
      center = Offset(badgeCenter.x - w * 0.015f, badgeCenter.y - w * 0.015f),
      radius = w * 0.016f,
    )
  }
}

/**
 * Renders the 6 MANO Squad members as authentic MANO V1.0 3D-shaded robots
 * (creamy pearl-white body, vibrant Warm Orange-Amber visor bezel, deep black visor screen,
 * glowing ring-eyes with white catchlights, top leaf sprout, ear pods, and Orange-Gold M+ emblem)
 * paired with each member's role-specific accent trim and 3D clinical prop.
 */
@Composable
fun ManoV1SquadCharacterCanvas(
  member: ManoSquadMember,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  val roleAccent = member.accentColor(isDark)
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val orangeHighlight = Color(0xFFFFC857)
    val orangeCore = Color(0xFFFF7A18)
    val orangeDeep = Color(0xFFD64E00)
    val emeraldGlow = Color(0xFF3EF598)
    val leafLight = Color(0xFFA6E22E)
    val leafMid = Color(0xFF68B818)
    val leafDark = Color(0xFF3E780A)

    // 1. Warm Orange + Role Accent radial backlight aura
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              orangeCore.copy(alpha = 0.25f),
              roleAccent.copy(alpha = 0.20f),
              Color.Transparent,
            ),
          center = Offset(w * 0.48f, h * 0.48f),
          radius = w * 0.46f,
        ),
      center = Offset(w * 0.48f, h * 0.48f),
      radius = w * 0.46f,
    )

    // 2. Soft floor contact shadow
    drawOval(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0x364A2000), Color.Transparent),
          center = Offset(w * 0.47f, h * 0.90f),
          radius = w * 0.26f,
        ),
      topLeft = Offset(w * 0.22f, h * 0.86f),
      size = Size(w * 0.50f, h * 0.08f),
    )

    // 3. Cute Rounded 3D Robot Boots with Orange + Role Accent Ankle Rings
    listOf(w * 0.31f, w * 0.49f).forEach { footLeft ->
      drawRoundRect(
        color = roleAccent,
        topLeft = Offset(footLeft + w * 0.015f, h * 0.79f),
        size = Size(w * 0.11f, h * 0.032f),
        cornerRadius = CornerRadius(w * 0.015f, w * 0.015f),
      )
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF3ECE4), Color(0xFFDDD2C5)),
            start = Offset(footLeft, h * 0.81f),
            end = Offset(footLeft + w * 0.14f, h * 0.90f),
          ),
        topLeft = Offset(footLeft, h * 0.815f),
        size = Size(w * 0.14f, h * 0.072f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
    }

    // 4. Left Arm & Right 3/4 Waving Arm (سه‌رخ) with Orange-Amber & Role Cuff Rings
    rotate(degrees = 15f, pivot = Offset(w * 0.24f, h * 0.59f)) {
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBE2D8)),
            start = Offset(w * 0.14f, h * 0.57f),
            end = Offset(w * 0.26f, h * 0.77f),
          ),
        topLeft = Offset(w * 0.15f, h * 0.57f),
        size = Size(w * 0.105f, h * 0.185f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(orangeHighlight, orangeCore, roleAccent)),
        topLeft = Offset(w * 0.15f, h * 0.69f),
        size = Size(w * 0.105f, h * 0.025f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    rotate(degrees = -36f, pivot = Offset(w * 0.70f, h * 0.59f)) {
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBE2D8)),
            start = Offset(w * 0.68f, h * 0.54f),
            end = Offset(w * 0.80f, h * 0.74f),
          ),
        topLeft = Offset(w * 0.68f, h * 0.55f),
        size = Size(w * 0.105f, h * 0.185f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(roleAccent, orangeCore, orangeHighlight)),
        topLeft = Offset(w * 0.68f, h * 0.67f),
        size = Size(w * 0.105f, h * 0.025f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    // 5. Creamy Pearl-White 3D Torso + Orange-Gold Collar + M+ Diamond Chest Emblem
    drawOval(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFFFFFFF), Color(0xFFFBF7F2), Color(0xFFEAE0D5)),
          center = Offset(w * 0.43f, h * 0.63f),
          radius = w * 0.25f,
        ),
      topLeft = Offset(w * 0.25f, h * 0.54f),
      size = Size(w * 0.44f, h * 0.28f),
    )
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(orangeDeep, orangeCore, orangeHighlight, orangeCore, orangeDeep)),
      topLeft = Offset(w * 0.36f, h * 0.525f),
      size = Size(w * 0.22f, h * 0.035f),
      cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
    )
    drawRoundRect(
      color = roleAccent,
      topLeft = Offset(w * 0.35f, h * 0.552f),
      size = Size(w * 0.24f, h * 0.018f),
      cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
    )

    // M+ Chest Logo in Warm Metallic Orange-Gold
    val mPath =
      Path().apply {
        moveTo(w * 0.39f, h * 0.725f)
        lineTo(w * 0.39f, h * 0.640f)
        lineTo(w * 0.45f, h * 0.695f)
        lineTo(w * 0.51f, h * 0.640f)
        lineTo(w * 0.51f, h * 0.725f)
      }
    drawPath(
      path = mPath,
      brush = Brush.verticalGradient(listOf(orangeHighlight, orangeCore, orangeDeep)),
      style = Stroke(width = w * 0.030f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val diamondPath =
      Path().apply {
        moveTo(w * 0.45f, h * 0.602f)
        lineTo(w * 0.472f, h * 0.628f)
        lineTo(w * 0.45f, h * 0.652f)
        lineTo(w * 0.428f, h * 0.628f)
        close()
      }
    drawPath(path = diamondPath, brush = Brush.verticalGradient(listOf(orangeHighlight, orangeCore)))

    val plusCx = w * 0.55f
    val plusCy = h * 0.635f
    val plusArm = w * 0.022f
    drawLine(
      color = roleAccent,
      start = Offset(plusCx - plusArm, plusCy),
      end = Offset(plusCx + plusArm, plusCy),
      strokeWidth = w * 0.018f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = roleAccent,
      start = Offset(plusCx, plusCy - plusArm),
      end = Offset(plusCx, plusCy + plusArm),
      strokeWidth = w * 0.018f,
      cap = StrokeCap.Round,
    )

    // 6. Side 3D Ear Pods
    listOf(w * 0.07f, w * 0.77f).forEach { earLeft ->
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE5DBD0)),
            start = Offset(earLeft, h * 0.30f),
            end = Offset(earLeft + w * 0.095f, h * 0.45f),
          ),
        topLeft = Offset(earLeft, h * 0.305f),
        size = Size(w * 0.095f, h * 0.145f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = orangeCore,
        topLeft = Offset(earLeft + w * 0.018f, h * 0.33f),
        size = Size(w * 0.058f, h * 0.095f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        style = Stroke(width = w * 0.012f),
      )
    }

    // 7. Signature Lush Top Leaf Sprout with Warm Golden-Orange Rim
    val stemPath =
      Path().apply {
        moveTo(w * 0.455f, h * 0.185f)
        quadraticBezierTo(w * 0.445f, h * 0.125f, w * 0.485f, h * 0.095f)
      }
    drawPath(path = stemPath, color = leafDark, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))
    val leafPath =
      Path().apply {
        moveTo(w * 0.455f, h * 0.135f)
        cubicTo(w * 0.445f, h * 0.045f, w * 0.555f, h * 0.02f, w * 0.645f, h * 0.04f)
        cubicTo(w * 0.625f, h * 0.125f, w * 0.535f, h * 0.165f, w * 0.455f, h * 0.135f)
        close()
      }
    drawPath(
      path = leafPath,
      brush =
        Brush.linearGradient(
          colors = listOf(leafLight, leafMid, leafDark),
          start = Offset(w * 0.45f, h * 0.03f),
          end = Offset(w * 0.64f, h * 0.16f),
        ),
    )
    drawPath(path = leafPath, color = orangeHighlight.copy(alpha = 0.70f), style = Stroke(width = w * 0.010f))

    // 8. Rounded Pearl-White 3D Head + Vibrant Warm Orange-Amber Metallic Visor Frame
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFFFFFFF), Color(0xFFFCF9F5), Color(0xFFE9DFD3)),
          center = Offset(w * 0.40f, h * 0.25f),
          radius = w * 0.40f,
        ),
      topLeft = Offset(w * 0.12f, h * 0.165f),
      size = Size(w * 0.69f, h * 0.375f),
      cornerRadius = CornerRadius(w * 0.19f, w * 0.19f),
    )
    drawRoundRect(
      brush =
        Brush.linearGradient(
          colors = listOf(orangeHighlight, orangeCore, orangeDeep, orangeCore, orangeHighlight),
          start = Offset(w * 0.18f, h * 0.20f),
          end = Offset(w * 0.75f, h * 0.51f),
        ),
      topLeft = Offset(w * 0.178f, h * 0.21f),
      size = Size(w * 0.574f, h * 0.288f),
      cornerRadius = CornerRadius(w * 0.145f, w * 0.145f),
    )

    // 9. Glossy Obsidian Visor Screen + Glowing Ring-Eyes with White Catchlights + Smile
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF1A2826), Color(0xFF0C1514), Color(0xFF050A09)),
          center = Offset(w * 0.465f, h * 0.35f),
          radius = w * 0.30f,
        ),
      topLeft = Offset(w * 0.21f, h * 0.236f),
      size = Size(w * 0.51f, h * 0.236f),
      cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
    )
    val glassReflection =
      Path().apply {
        moveTo(w * 0.245f, h * 0.285f)
        quadraticBezierTo(w * 0.35f, h * 0.25f, w * 0.51f, h * 0.262f)
      }
    drawPath(
      path = glassReflection,
      color = Color.White.copy(alpha = 0.24f),
      style = Stroke(width = w * 0.018f, cap = StrokeCap.Round),
    )

    val leftEye = Offset(w * 0.35f, h * 0.348f)
    val rightEye = Offset(w * 0.585f, h * 0.348f)
    val eyeR = w * 0.054f
    listOf(leftEye, rightEye).forEach { center ->
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(emeraldGlow.copy(alpha = 0.46f), Color.Transparent),
            center = center,
            radius = eyeR * 1.6f,
          ),
        center = center,
        radius = eyeR * 1.6f,
      )
      drawCircle(
        color = emeraldGlow,
        center = center,
        radius = eyeR,
        style = Stroke(width = w * 0.022f),
      )
      drawCircle(color = Color(0xFF07110E), center = center, radius = eyeR * 0.72f)
      drawCircle(
        color = Color.White,
        center = Offset(center.x - eyeR * 0.32f, center.y - eyeR * 0.32f),
        radius = eyeR * 0.26f,
      )
    }

    val smilePath =
      Path().apply {
        moveTo(w * 0.415f, h * 0.388f)
        quadraticBezierTo(w * 0.468f, h * 0.43f, w * 0.52f, h * 0.388f)
      }
    drawPath(
      path = smilePath,
      color = emeraldGlow,
      style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
    )

    // 10. Role-Specific 3D Floating Clinical Badge on Bottom-Right
    val propCenter = Offset(w * 0.80f, h * 0.76f)
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(roleAccent, orangeCore),
          center = Offset(propCenter.x - w * 0.02f, propCenter.y - h * 0.02f),
          radius = w * 0.12f,
        ),
      center = propCenter,
      radius = w * 0.105f,
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.92f),
      center = propCenter,
      radius = w * 0.105f,
      style = Stroke(width = w * 0.014f),
    )
    // Crisp white medical cross / star inside the floating role orb
    val rArm = w * 0.042f
    drawLine(
      color = Color.White,
      start = Offset(propCenter.x - rArm, propCenter.y),
      end = Offset(propCenter.x + rArm, propCenter.y),
      strokeWidth = w * 0.022f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color.White,
      start = Offset(propCenter.x, propCenter.y - rArm),
      end = Offset(propCenter.x, propCenter.y + rArm),
      strokeWidth = w * 0.022f,
      cap = StrokeCap.Round,
    )
  }
}
