package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs

@Immutable
data class ManoTiltState(
  val tiltX: Float = 0f, // Normalized horizontal tilt (-1f..1f)
  val tiltY: Float = 0f, // Normalized vertical tilt (-1f..1f)
)

private val DefaultTiltStateHolder: State<ManoTiltState> = mutableStateOf(ManoTiltState())

/**
 * Provides a shared, lifecycle-aware, zero-recomposition hardware tilt state
 * to frosted-glass cards on physical devices while staying completely idle on
 * emulators and reduced-motion environments.
 */
val LocalManoDeviceTilt = staticCompositionLocalOf<State<ManoTiltState>> { DefaultTiltStateHolder }

internal fun isEmulatorRuntime(): Boolean {
  val fp = Build.FINGERPRINT.orEmpty().lowercase()
  val model = Build.MODEL.orEmpty().lowercase()
  val hardware = Build.HARDWARE.orEmpty().lowercase()
  val product = Build.PRODUCT.orEmpty().lowercase()
  val brand = Build.BRAND.orEmpty().lowercase()
  return fp == "robolectric" ||
    fp.startsWith("generic") ||
    fp.contains("emulator") ||
    model.contains("emulator") ||
    model.contains("android sdk built for") ||
    hardware.contains("goldfish") ||
    hardware.contains("ranchu") ||
    product.contains("sdk_gphone") ||
    product.contains("emulator") ||
    product.contains("google_sdk") ||
    (brand.startsWith("generic") && Build.DEVICE.orEmpty().lowercase().startsWith("generic"))
}

internal fun isReducedMotionOrEmulator(context: Context): Boolean {
  if (isEmulatorRuntime()) return true
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
 * Registers a single permission-free, throttled [Sensor.TYPE_GRAVITY] (with
 * [Sensor.TYPE_ACCELEROMETER] fallback) listener on physical handhelds only.
 * Completely skipped on emulators and when Android Reduced Motion is enabled so
 * streaming previews render frame 1 with zero sensor overhead.
 */
@Composable
fun ManoDeviceTiltProvider(
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val skipSensors = remember(context) { isReducedMotionOrEmulator(context) }
  val tiltState = remember { mutableStateOf(ManoTiltState()) }

  DisposableEffect(context, lifecycleOwner, skipSensors) {
    if (skipSensors) {
      tiltState.value = ManoTiltState()
      return@DisposableEffect onDispose {}
    }

    val sensorManager =
      runCatching { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }.getOrNull()
    val tiltSensor =
      sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    if (sensorManager == null || tiltSensor == null) {
      return@DisposableEffect onDispose {}
    }

    var smoothX = 0f
    var smoothY = 0f
    var lastEmitMs = 0L
    var registered = false

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          val now = SystemClock.elapsedRealtime()
          if (now - lastEmitMs < 48L) return
          val ax = event.values.getOrNull(0) ?: return
          val ay = event.values.getOrNull(1) ?: return
          val rawX = (-ax / 5.2f).coerceIn(-1f, 1f)
          val rawY = ((ay - 6.2f) / 5.2f).coerceIn(-1f, 1f)
          smoothX += (rawX - smoothX) * 0.16f
          smoothY += (rawY - smoothY) * 0.16f
          val prev = tiltState.value
          if (abs(smoothX - prev.tiltX) > 0.015f || abs(smoothY - prev.tiltY) > 0.015f) {
            lastEmitMs = now
            tiltState.value = ManoTiltState(tiltX = smoothX, tiltY = smoothY)
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    fun startListening() {
      if (!registered) {
        registered =
          runCatching {
            sensorManager.registerListener(listener, tiltSensor, SensorManager.SENSOR_DELAY_UI)
          }.getOrDefault(false)
      }
    }

    fun stopListening() {
      if (registered) {
        runCatching { sensorManager.unregisterListener(listener) }
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
 * Lightweight, draw-phase-only frosted tilt effect for cards.
 * - Zero infinite animations (no 60 FPS idle recomposition/GPU drain).
 * - Zero layout-feedback loops (no state mutation inside onGloballyPositioned).
 * - Complete no-op on emulators and Reduced Motion environments for instant first paint.
 */
fun Modifier.manoFloatingGlass(
  intensity: Float = 1f,
  @Suppress("UNUSED_PARAMETER") enableSheen: Boolean = true,
  @Suppress("UNUSED_PARAMETER") onScrollNormalized: ((Float) -> Unit)? = null,
): Modifier =
  composed {
    val context = LocalContext.current
    val skipMotion = remember(context) { isReducedMotionOrEmulator(context) }
    if (skipMotion) {
      return@composed this
    }

    val tiltState = LocalManoDeviceTilt.current
    val clampedIntensity = intensity.coerceIn(0.25f, 1.25f)

    this.graphicsLayer {
      val tilt = tiltState.value
      translationX = (tilt.tiltX * 2.4f * clampedIntensity).dp.toPx()
      translationY = (tilt.tiltY * 1.8f * clampedIntensity).dp.toPx()
      rotationX = (-tilt.tiltY * 1.1f) * clampedIntensity
      rotationY = (tilt.tiltX * 1.4f) * clampedIntensity
      cameraDistance = 14f * density
    }
  }
