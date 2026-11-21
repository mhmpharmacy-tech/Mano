package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MinimalTeal
import com.example.ui.theme.MinimalTealDark
import com.example.ui.theme.PureWhite

@Immutable
data class ManoTiltState(
  val tiltX: Float = 0f, // Normalized horizontal tilt (-1f..1f)
  val tiltY: Float = 0f, // Normalized vertical tilt (-1f..1f)
)

private val DefaultTiltStateHolder: State<ManoTiltState> = mutableStateOf(ManoTiltState())

/**
 * Provides a shared, lifecycle-aware, zero-recomposition hardware tilt state
 * to frosted-glass banners across the app.
 */
val LocalManoDeviceTilt = staticCompositionLocalOf<State<ManoTiltState>> { DefaultTiltStateHolder }

private fun isReducedMotionEnabled(context: Context): Boolean {
  if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return true
  return runCatching {
    val scale =
      Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
      )
    scale <= 0f
  }.getOrDefault(false)
}

/**
 * Registers a single permission-free [Sensor.TYPE_GRAVITY] (with [Sensor.TYPE_ACCELEROMETER]
 * fallback) listener for the active Activity lifecycle. Automatically pauses on background
 * and disables when Android's Reduced Motion setting is active.
 */
@Composable
fun ManoDeviceTiltProvider(
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
  val tiltState = remember { mutableStateOf(ManoTiltState()) }

  DisposableEffect(context, lifecycleOwner, reducedMotion) {
    if (reducedMotion) {
      tiltState.value = ManoTiltState()
      return@DisposableEffect onDispose {}
    }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val tiltSensor =
      sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    if (sensorManager == null || tiltSensor == null) {
      return@DisposableEffect onDispose {}
    }

    var smoothX = 0f
    var smoothY = 0f
    var registered = false

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val ax = event.values.getOrNull(0) ?: return
          val ay = event.values.getOrNull(1) ?: return
          // Calibrated around natural comfortable handheld reading posture (~40° pitch, ay ≈ 6.2 m/s²)
          val rawX = (-ax / 5.2f).coerceIn(-1f, 1f)
          val rawY = ((ay - 6.2f) / 5.2f).coerceIn(-1f, 1f)
          // Low-pass exponential smoothing for serene, liquid-like clinical feel
          smoothX += (rawX - smoothX) * 0.14f
          smoothY += (rawY - smoothY) * 0.14f
          tiltState.value = ManoTiltState(tiltX = smoothX, tiltY = smoothY)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    fun startListening() {
      if (!registered) {
        registered = sensorManager.registerListener(listener, tiltSensor, SensorManager.SENSOR_DELAY_GAME)
      }
    }

    fun stopListening() {
      if (registered) {
        sensorManager.unregisterListener(listener)
        registered = false
      }
    }

    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_RESUME -> startListening()
          Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stopListening()
          else -> Unit
        }
      }

    lifecycleOwner.lifecycle.addObserver(observer)
    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      startListening()
    }

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      stopListening()
    }
  }

  CompositionLocalProvider(LocalManoDeviceTilt provides tiltState) {
    content()
  }
}

/**
 * Applies a calm, tactile floating frosted-glass effect driven by:
 * 1. Live viewport scroll position (subtle vertical float & micro-pitch shift while scrolling)
 * 2. Live phone tilt from [LocalManoDeviceTilt] (gentle micro-3D perspective & horizontal drift)
 * 3. Whisper-soft ambient buoyancy cycle (weightless living presence)
 * 4. Moving frosted-glass pastel specular sheen overlaid in the Draw phase (zero recompositions)
 *
 * Automatically settles to static placement when Reduced Motion is enabled.
 */
fun Modifier.manoFloatingGlass(
  intensity: Float = 1f,
  enableSheen: Boolean = true,
  onScrollNormalized: ((Float) -> Unit)? = null,
): Modifier =
  composed {
    val context = LocalContext.current
    val isDark = LocalManoDarkTheme.current
    val tiltState = LocalManoDeviceTilt.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }

    if (reducedMotion) {
      return@composed this
    }

    val clampedIntensity = intensity.coerceIn(0.25f, 1.35f)
    var targetScrollNormalized by remember { mutableFloatStateOf(0f) }

    // Smooth spring-damped scroll response so rapid flings settle like frosted glass on water
    val smoothScrollNorm by
      animateFloatAsState(
        targetValue = targetScrollNormalized,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 135f),
        label = "manoFloatingGlassScrollSpring",
      )

    // Subtle serene ambient floating breath (5.6s full sine-like loop)
    val infiniteTransition = rememberInfiniteTransition(label = "manoFloatingGlassBreath")
    val ambientFloat by
      infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "manoFloatingGlassBreathPhase",
      )

    val sheenTint = if (isDark) MinimalTealDark else MinimalTeal

    this
      .onGloballyPositioned { coords ->
        val rootHeight = coords.findRootCoordinates().size.height.toFloat()
        if (rootHeight > 1f) {
          val centerY = coords.positionInRoot().y + (coords.size.height * 0.5f)
          // Normalized -1f (top of viewport) .. 0f (center) .. +1f (bottom of viewport)
          val norm = (((centerY / rootHeight) - 0.5f) * 2f).coerceIn(-1.2f, 1.2f)
          targetScrollNormalized = norm
          onScrollNormalized?.invoke(norm)
        }
      }
      .graphicsLayer {
        val tilt = tiltState.value
        val scrollLiftPx = (-smoothScrollNorm * 3.4f * clampedIntensity).dp.toPx()
        val tiltShiftXPx = (tilt.tiltX * 3.2f * clampedIntensity).dp.toPx()
        val tiltShiftYPx = (tilt.tiltY * 2.4f * clampedIntensity).dp.toPx()
        val ambientLiftPx = (ambientFloat * 1.45f * clampedIntensity).dp.toPx()

        translationX = tiltShiftXPx
        translationY = scrollLiftPx + tiltShiftYPx + ambientLiftPx
        rotationX = ((-tilt.tiltY * 1.6f) + (smoothScrollNorm * 1.15f)) * clampedIntensity
        rotationY = (tilt.tiltX * 2.0f) * clampedIntensity
        cameraDistance = 14f * density
      }
      .then(
        if (enableSheen) {
          Modifier.drawWithContent {
            drawContent()
            val tilt = tiltState.value
            val w = size.width
            val h = size.height
            if (w > 1f && h > 1f) {
              // Soft pastel frosted light glint gliding organically with tilt + scroll
              val sheenCenterX = w * (0.50f + tilt.tiltX * 0.28f - smoothScrollNorm * 0.08f)
              val sheenCenterY = h * (0.20f + tilt.tiltY * 0.20f - smoothScrollNorm * 0.14f)
              val sheenRadius = (w * 0.72f).coerceAtLeast(1f)
              val peakAlpha = (if (isDark) 0.10f else 0.19f) * clampedIntensity
              val mistAlpha = (if (isDark) 0.05f else 0.08f) * clampedIntensity
              drawRect(
                brush =
                  Brush.radialGradient(
                    colors =
                      listOf(
                        PureWhite.copy(alpha = peakAlpha),
                        sheenTint.copy(alpha = mistAlpha),
                        Color.Transparent,
                      ),
                    center = Offset(sheenCenterX, sheenCenterY),
                    radius = sheenRadius,
                  )
              )
            }
          }
        } else {
          Modifier
        }
      )
  }
