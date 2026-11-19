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
 * Calmed, humanized, matte-ceramic MANO Kid Canvas renderer:
 * - Warm creamy ivory matte porcelain body (no harsh plastic specular glare)
 * - Soothing Seafoam-Aqua & Champagne-Pearl trim replacing all jarring orange tones
 * - Deep velvety midnight-teal face screen with soft sky-mint eyes, double catchlights,
 *   warm rose blush cheeks, and comforting smile tailored to each MascotPose
 * - Cozy knitted seafoam scarf collar + delicate sage leaf sprout + champagne M+ emblem
 */
@Composable
fun ManoCalmCharacterCanvas(
  pose: MascotPose,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val champagneHighlight = Color(0xFFFAF3DF)
    val champagneCore = Color(0xFFD6BE84)
    val champagneDeep = Color(0xFFB19557)
    val seafoamLight = Color(0xFF9DF2DF)
    val seafoamCore = Color(0xFF28B5A0)
    val seafoamDeep = Color(0xFF0E7D70)
    val leafLight = Color(0xFFAFE87A)
    val leafMid = Color(0xFF6BBF4E)
    val leafDark = Color(0xFF2C7F4D)
    val skyMintGlow = Color(0xFF8EFCE8)
    val roseBlush = Color(0xFFFF9DB2)

    val eyeGlowColor =
      when (pose) {
        MascotPose.CAUTION -> Color(0xFFFDE08E)
        else -> skyMintGlow
      }

    // 1. Serene sky-mint ambient halo behind character
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              seafoamLight.copy(alpha = 0.24f),
              Color(0xFFBFE9FF).copy(alpha = 0.20f),
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
          colors = listOf(Color(0x280C3D47), Color.Transparent),
          center = Offset(w * 0.50f, h * 0.91f),
          radius = w * 0.28f,
        ),
      topLeft = Offset(w * 0.24f, h * 0.87f),
      size = Size(w * 0.52f, h * 0.08f),
    )

    // 3. Soft rounded boots with calm seafoam ankle cuffs
    listOf(w * 0.34f, w * 0.52f).forEach { footLeft ->
      drawRoundRect(
        color = seafoamCore,
        topLeft = Offset(footLeft + w * 0.015f, h * 0.80f),
        size = Size(w * 0.11f, h * 0.035f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
      )
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF6F1E9), Color(0xFFE5DDD2)),
            start = Offset(footLeft, h * 0.82f),
            end = Offset(footLeft + w * 0.14f, h * 0.91f),
          ),
        topLeft = Offset(footLeft, h * 0.825f),
        size = Size(w * 0.14f, h * 0.075f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
    }

    // 4. Gentle Arms (Left resting, Right soothing wave/guide)
    rotate(degrees = 14f, pivot = Offset(w * 0.26f, h * 0.60f)) {
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFE9DF)),
            start = Offset(w * 0.16f, h * 0.58f),
            end = Offset(w * 0.28f, h * 0.78f),
          ),
        topLeft = Offset(w * 0.17f, h * 0.58f),
        size = Size(w * 0.11f, h * 0.19f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f),
      )
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(seafoamLight, seafoamCore, seafoamDeep)),
        topLeft = Offset(w * 0.17f, h * 0.70f),
        size = Size(w * 0.11f, h * 0.026f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    val rightArmAngle =
      when (pose) {
        MascotPose.WELCOME, MascotPose.EXPLAIN -> -36f
        MascotPose.CAUTION -> -44f
        MascotPose.LISTEN -> -22f
        else -> -18f
      }
    rotate(degrees = rightArmAngle, pivot = Offset(w * 0.74f, h * 0.60f)) {
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFE9DF)),
            start = Offset(w * 0.72f, h * 0.55f),
            end = Offset(w * 0.84f, h * 0.76f),
          ),
        topLeft = Offset(w * 0.72f, h * 0.56f),
        size = Size(w * 0.11f, h * 0.19f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f),
      )
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(seafoamDeep, seafoamCore, seafoamLight)),
        topLeft = Offset(w * 0.72f, h * 0.68f),
        size = Size(w * 0.11f, h * 0.026f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    // 5. Soft Creamy Matte Pearl Torso
    drawOval(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFFFFFFFF),
              Color(0xFFFBF8F3),
              Color(0xFFEDE6DA),
            ),
          center = Offset(w * 0.45f, h * 0.64f),
          radius = w * 0.26f,
        ),
      topLeft = Offset(w * 0.27f, h * 0.55f),
      size = Size(w * 0.46f, h * 0.29f),
    )

    // Cozy Knitted Seafoam-Mint Neck Scarf (replaces metallic collar)
    drawRoundRect(
      brush =
        Brush.horizontalGradient(
          colors = listOf(seafoamDeep, seafoamCore, seafoamLight, seafoamCore, seafoamDeep)
        ),
      topLeft = Offset(w * 0.34f, h * 0.525f),
      size = Size(w * 0.32f, h * 0.058f),
      cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
    )
    // Cute hanging scarf tail on right side
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(seafoamCore, seafoamDeep)),
      topLeft = Offset(w * 0.56f, h * 0.55f),
      size = Size(w * 0.072f, h * 0.105f),
      cornerRadius = CornerRadius(w * 0.022f, w * 0.022f),
    )

    // 6. Soft Champagne-Gold & Seafoam "M+" Chest Emblem
    val mPath =
      Path().apply {
        moveTo(w * 0.405f, h * 0.742f)
        lineTo(w * 0.405f, h * 0.658f)
        lineTo(w * 0.465f, h * 0.712f)
        lineTo(w * 0.525f, h * 0.658f)
        lineTo(w * 0.525f, h * 0.742f)
      }
    drawPath(
      path = mPath,
      brush =
        Brush.verticalGradient(
          colors = listOf(champagneHighlight, champagneCore, champagneDeep),
          startY = h * 0.64f,
          endY = h * 0.74f,
        ),
      style = Stroke(width = w * 0.028f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    // Gentle heart-drop dot above M
    val diamondPath =
      Path().apply {
        moveTo(w * 0.465f, h * 0.620f)
        lineTo(w * 0.486f, h * 0.644f)
        lineTo(w * 0.465f, h * 0.666f)
        lineTo(w * 0.444f, h * 0.644f)
        close()
      }
    drawPath(
      path = diamondPath,
      brush = Brush.verticalGradient(listOf(champagneHighlight, champagneCore)),
    )
    // "+" Cross in calm seafoam
    val plusCx = w * 0.568f
    val plusCy = h * 0.652f
    val plusArm = w * 0.022f
    drawLine(
      color = seafoamCore,
      start = Offset(plusCx - plusArm, plusCy),
      end = Offset(plusCx + plusArm, plusCy),
      strokeWidth = w * 0.017f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = seafoamCore,
      start = Offset(plusCx, plusCy - plusArm),
      end = Offset(plusCx, plusCy + plusArm),
      strokeWidth = w * 0.017f,
      cap = StrokeCap.Round,
    )

    // 7. Soft Side Ear Pads with gentle seafoam accent ring
    listOf(w * 0.09f, w * 0.81f).forEach { earLeft ->
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBE4D9)),
            start = Offset(earLeft, h * 0.30f),
            end = Offset(earLeft + w * 0.10f, h * 0.46f),
          ),
        topLeft = Offset(earLeft, h * 0.31f),
        size = Size(w * 0.10f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = seafoamCore.copy(alpha = 0.65f),
        topLeft = Offset(earLeft + w * 0.02f, h * 0.335f),
        size = Size(w * 0.06f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        style = Stroke(width = w * 0.012f),
      )
    }

    // 8. Tender Twin Sprout Leaves on Head
    val stemPath =
      Path().apply {
        moveTo(w * 0.49f, h * 0.188f)
        quadraticBezierTo(w * 0.48f, h * 0.125f, w * 0.515f, h * 0.095f)
      }
    drawPath(
      path = stemPath,
      color = leafDark,
      style = Stroke(width = w * 0.024f, cap = StrokeCap.Round),
    )
    val leafPath =
      Path().apply {
        moveTo(w * 0.495f, h * 0.138f)
        cubicTo(w * 0.485f, h * 0.050f, w * 0.59f, h * 0.020f, w * 0.68f, h * 0.042f)
        cubicTo(w * 0.66f, h * 0.128f, w * 0.57f, h * 0.168f, w * 0.495f, h * 0.138f)
        close()
      }
    drawPath(
      path = leafPath,
      brush =
        Brush.linearGradient(
          colors = listOf(leafLight, leafMid, leafDark),
          start = Offset(w * 0.49f, h * 0.03f),
          end = Offset(w * 0.66f, h * 0.16f),
        ),
    )
    // Smaller balancing left sprout leaf
    val smallLeaf =
      Path().apply {
        moveTo(w * 0.49f, h * 0.14f)
        cubicTo(w * 0.44f, h * 0.075f, w * 0.36f, h * 0.065f, w * 0.335f, h * 0.095f)
        cubicTo(w * 0.36f, h * 0.145f, w * 0.43f, h * 0.16f, w * 0.49f, h * 0.14f)
        close()
      }
    drawPath(
      path = smallLeaf,
      brush = Brush.linearGradient(listOf(leafLight, leafDark)),
    )

    // 9. Rounded Matte Pearl-Cream Head
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFFFFFFFF),
              Color(0xFFFCF9F4),
              Color(0xFFEDE6DB),
            ),
          center = Offset(w * 0.42f, h * 0.26f),
          radius = w * 0.42f,
        ),
      topLeft = Offset(w * 0.14f, h * 0.17f),
      size = Size(w * 0.72f, h * 0.38f),
      cornerRadius = CornerRadius(w * 0.20f, w * 0.20f),
    )

    // 10. Calming Seafoam-Aqua Inner Visor Frame (zero orange!)
    drawRoundRect(
      brush =
        Brush.linearGradient(
          colors = listOf(seafoamLight, seafoamCore, seafoamDeep, seafoamCore),
          start = Offset(w * 0.20f, h * 0.21f),
          end = Offset(w * 0.80f, h * 0.52f),
        ),
      topLeft = Offset(w * 0.20f, h * 0.215f),
      size = Size(w * 0.60f, h * 0.295f),
      cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
    )

    // 11. Warm Midnight-Teal Velvety Face Screen (approachable, softer than harsh obsidian)
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xFF1B404A),
              Color(0xFF102A31),
              Color(0xFF0A1B20),
            ),
          center = Offset(w * 0.50f, h * 0.36f),
          radius = w * 0.32f,
        ),
      topLeft = Offset(w * 0.228f, h * 0.240f),
      size = Size(w * 0.544f, h * 0.245f),
      cornerRadius = CornerRadius(w * 0.13f, w * 0.13f),
    )

    // Soft glass highlight
    val glassReflection =
      Path().apply {
        moveTo(w * 0.27f, h * 0.29f)
        quadraticBezierTo(w * 0.38f, h * 0.255f, w * 0.54f, h * 0.268f)
      }
    drawPath(
      path = glassReflection,
      color = Color.White.copy(alpha = 0.22f),
      style = Stroke(width = w * 0.018f, cap = StrokeCap.Round),
    )

    // 12. Warm Soulful Soft Glowing Eyes + Happy Crescent Expressions
    val leftEyeCenter = Offset(w * 0.375f, h * 0.352f)
    val rightEyeCenter = Offset(w * 0.625f, h * 0.352f)
    val eyeRadius = w * 0.056f

    listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(eyeGlowColor.copy(alpha = 0.42f), Color.Transparent),
            center = center,
            radius = eyeRadius * 1.65f,
          ),
        center = center,
        radius = eyeRadius * 1.65f,
      )
      if (pose == MascotPose.WELCOME || pose == MascotPose.SAVED) {
        // Warm soft filled dome eyes (gentle, soulful, cute!)
        drawCircle(
          color = eyeGlowColor,
          center = center,
          radius = eyeRadius * 0.88f,
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.92f),
          center = Offset(center.x - eyeRadius * 0.28f, center.y - eyeRadius * 0.28f),
          radius = eyeRadius * 0.30f,
        )
      } else {
        drawCircle(
          color = eyeGlowColor,
          center = center,
          radius = eyeRadius,
          style = Stroke(width = w * 0.022f),
        )
        drawCircle(
          color = Color(0xFF0A2026),
          center = center,
          radius = eyeRadius * 0.72f,
        )
        drawCircle(
          color = Color.White,
          center = Offset(center.x - eyeRadius * 0.32f, center.y - eyeRadius * 0.32f),
          radius = eyeRadius * 0.26f,
        )
      }
    }

    // Soft petal-rose comforting digital cheeks
    drawOval(
      color = roseBlush.copy(alpha = 0.48f),
      topLeft = Offset(w * 0.270f, h * 0.392f),
      size = Size(w * 0.070f, h * 0.036f),
    )
    drawOval(
      color = roseBlush.copy(alpha = 0.48f),
      topLeft = Offset(w * 0.660f, h * 0.392f),
      size = Size(w * 0.070f, h * 0.036f),
    )

    // 13. Comforting gentle smile curve
    val smilePath =
      Path().apply {
        when (pose) {
          MascotPose.CAUTION -> {
            moveTo(w * 0.455f, h * 0.415f)
            quadraticBezierTo(w * 0.50f, h * 0.402f, w * 0.545f, h * 0.415f)
          }
          else -> {
            moveTo(w * 0.440f, h * 0.395f)
            quadraticBezierTo(w * 0.50f, h * 0.442f, w * 0.560f, h * 0.395f)
          }
        }
      }
    drawPath(
      path = smilePath,
      color = eyeGlowColor,
      style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
    )

    // 14. Floating comforting aura spark on top-right
    val badgeCenter = Offset(w * 0.83f, h * 0.17f)
    val badgeColor =
      when (pose) {
        MascotPose.CAUTION -> Color(0xFFF0B44D)
        else -> seafoamCore
      }
    drawCircle(color = badgeColor.copy(alpha = 0.22f), center = badgeCenter, radius = w * 0.082f)
    drawCircle(color = badgeColor, center = badgeCenter, radius = w * 0.052f)
    drawCircle(
      color = Color.White,
      center = Offset(badgeCenter.x - w * 0.014f, badgeCenter.y - w * 0.014f),
      radius = w * 0.015f,
    )
  }
}

/**
 * Compatibility alias delegating to the calmed humanized [ManoCalmCharacterCanvas] (zero harsh orange).
 */
@Composable
fun ManoV1OrangeCharacterCanvas(
  pose: MascotPose,
  modifier: Modifier = Modifier,
) {
  ManoCalmCharacterCanvas(pose = pose, modifier = modifier)
}

/**
 * Renders the 6 MANO Squad members as serene matte-porcelain pediatric companions
 * with role-specific calming accent scarves, aqua-mint soulful eyes, and a floating clinical badge.
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

    val champagneHighlight = Color(0xFFFAF3DF)
    val champagneCore = Color(0xFFD6BE84)
    val champagneDeep = Color(0xFFB19557)
    val skyMintGlow = Color(0xFF8EFCE8)
    val leafLight = Color(0xFFAFE87A)
    val leafMid = Color(0xFF6BBF4E)
    val leafDark = Color(0xFF2C7F4D)
    val roseBlush = Color(0xFFFF9DB2)

    // 1. Role Accent + Soft Sky halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              roleAccent.copy(alpha = 0.24f),
              skyMintGlow.copy(alpha = 0.16f),
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
          colors = listOf(Color(0x280C3D47), Color.Transparent),
          center = Offset(w * 0.47f, h * 0.90f),
          radius = w * 0.26f,
        ),
      topLeft = Offset(w * 0.22f, h * 0.86f),
      size = Size(w * 0.50f, h * 0.08f),
    )

    // 3. Cute Rounded Boots with Role Accent Ankle Rings
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
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF6F1E9), Color(0xFFE5DDD2)),
            start = Offset(footLeft, h * 0.81f),
            end = Offset(footLeft + w * 0.14f, h * 0.90f),
          ),
        topLeft = Offset(footLeft, h * 0.815f),
        size = Size(w * 0.14f, h * 0.072f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
    }

    // 4. Gentle Arms with Role Cuff Trim
    rotate(degrees = 15f, pivot = Offset(w * 0.24f, h * 0.59f)) {
      drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEFE9DF))),
        topLeft = Offset(w * 0.15f, h * 0.57f),
        size = Size(w * 0.105f, h * 0.185f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = roleAccent,
        topLeft = Offset(w * 0.15f, h * 0.69f),
        size = Size(w * 0.105f, h * 0.025f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    rotate(degrees = -34f, pivot = Offset(w * 0.70f, h * 0.59f)) {
      drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEFE9DF))),
        topLeft = Offset(w * 0.68f, h * 0.55f),
        size = Size(w * 0.105f, h * 0.185f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = roleAccent,
        topLeft = Offset(w * 0.68f, h * 0.67f),
        size = Size(w * 0.105f, h * 0.025f),
        cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
      )
    }

    // 5. Creamy Pearl Torso + Role Scarf Collar + Champagne M+ Emblem
    drawOval(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFFFFFFF), Color(0xFFFBF8F3), Color(0xFFEDE6DA)),
          center = Offset(w * 0.43f, h * 0.63f),
          radius = w * 0.25f,
        ),
      topLeft = Offset(w * 0.25f, h * 0.54f),
      size = Size(w * 0.44f, h * 0.28f),
    )
    drawRoundRect(
      color = roleAccent,
      topLeft = Offset(w * 0.34f, h * 0.525f),
      size = Size(w * 0.26f, h * 0.045f),
      cornerRadius = CornerRadius(w * 0.022f, w * 0.022f),
    )

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
      brush = Brush.verticalGradient(listOf(champagneHighlight, champagneCore, champagneDeep)),
      style = Stroke(width = w * 0.028f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val diamondPath =
      Path().apply {
        moveTo(w * 0.45f, h * 0.602f)
        lineTo(w * 0.472f, h * 0.628f)
        lineTo(w * 0.45f, h * 0.652f)
        lineTo(w * 0.428f, h * 0.628f)
        close()
      }
    drawPath(path = diamondPath, brush = Brush.verticalGradient(listOf(champagneHighlight, champagneCore)))

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

    // 6. Ear Pads
    listOf(w * 0.07f, w * 0.77f).forEach { earLeft ->
      drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEBE4D9))),
        topLeft = Offset(earLeft, h * 0.305f),
        size = Size(w * 0.095f, h * 0.145f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
      drawRoundRect(
        color = roleAccent.copy(alpha = 0.70f),
        topLeft = Offset(earLeft + w * 0.018f, h * 0.33f),
        size = Size(w * 0.058f, h * 0.095f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        style = Stroke(width = w * 0.012f),
      )
    }

    // 7. Top Leaf Sprout
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
      brush = Brush.linearGradient(listOf(leafLight, leafMid, leafDark)),
    )

    // 8. Matte Pearl-Cream Head + Role-Tinted Visor Frame
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFFFFFFFF), Color(0xFFFCF9F4), Color(0xFFEDE6DB)),
          center = Offset(w * 0.40f, h * 0.25f),
          radius = w * 0.40f,
        ),
      topLeft = Offset(w * 0.12f, h * 0.165f),
      size = Size(w * 0.69f, h * 0.375f),
      cornerRadius = CornerRadius(w * 0.19f, w * 0.19f),
    )
    drawRoundRect(
      brush = Brush.linearGradient(listOf(roleAccent.copy(alpha = 0.85f), roleAccent)),
      topLeft = Offset(w * 0.178f, h * 0.21f),
      size = Size(w * 0.574f, h * 0.288f),
      cornerRadius = CornerRadius(w * 0.145f, w * 0.145f),
    )

    // 9. Velvety Midnight-Teal Visor Screen + Warm Soulful Eyes + Cheeks + Smile
    drawRoundRect(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0xFF1B404A), Color(0xFF102A31), Color(0xFF0A1B20)),
          center = Offset(w * 0.465f, h * 0.35f),
          radius = w * 0.30f,
        ),
      topLeft = Offset(w * 0.21f, h * 0.236f),
      size = Size(w * 0.51f, h * 0.236f),
      cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
    )

    val leftEye = Offset(w * 0.35f, h * 0.345f)
    val rightEye = Offset(w * 0.585f, h * 0.345f)
    val eyeR = w * 0.048f
    listOf(leftEye, rightEye).forEach { center ->
      drawCircle(color = skyMintGlow.copy(alpha = 0.28f), center = center, radius = eyeR * 1.45f)
      drawCircle(color = skyMintGlow, center = center, radius = eyeR)
      drawCircle(
        color = Color.White,
        center = Offset(center.x - eyeR * 0.28f, center.y - eyeR * 0.28f),
        radius = eyeR * 0.30f,
      )
    }

    drawOval(color = roseBlush.copy(alpha = 0.45f), topLeft = Offset(w * 0.255f, h * 0.388f), size = Size(w * 0.062f, h * 0.032f))
    drawOval(color = roseBlush.copy(alpha = 0.45f), topLeft = Offset(w * 0.618f, h * 0.388f), size = Size(w * 0.062f, h * 0.032f))

    val smilePath =
      Path().apply {
        moveTo(w * 0.415f, h * 0.390f)
        quadraticBezierTo(w * 0.468f, h * 0.432f, w * 0.52f, h * 0.390f)
      }
    drawPath(
      path = smilePath,
      color = skyMintGlow,
      style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
    )

    // 10. Floating Calming Clinical Badge on Bottom-Right
    val propCenter = Offset(w * 0.80f, h * 0.76f)
    drawCircle(
      brush = Brush.radialGradient(listOf(roleAccent, roleAccent.copy(alpha = 0.85f))),
      center = propCenter,
      radius = w * 0.105f,
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.92f),
      center = propCenter,
      radius = w * 0.105f,
      style = Stroke(width = w * 0.014f),
    )
    val rArm = w * 0.042f
    drawLine(color = Color.White, start = Offset(propCenter.x - rArm, propCenter.y), end = Offset(propCenter.x + rArm, propCenter.y), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
    drawLine(color = Color.White, start = Offset(propCenter.x, propCenter.y - rArm), end = Offset(propCenter.x, propCenter.y + rArm), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
  }
}
