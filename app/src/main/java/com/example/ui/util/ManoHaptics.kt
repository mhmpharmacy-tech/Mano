package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.data.ManoHapticIntensity

/**
 * Unified tactile haptic feedback utility calibrated for real Android devices,
 * honoring the user's selected [ManoHapticIntensity] (OFF / LIGHT / MEDIUM / STRONG).
 */
object ManoHaptics {

  @Suppress("DEPRECATION")
  fun perform(
    context: Context,
    view: View? = null,
    intensity: ManoHapticIntensity = ManoHapticIntensity.MEDIUM,
    isPrimaryInteraction: Boolean = false,
  ) {
    if (intensity == ManoHapticIntensity.OFF) return

    // Prefer calibrated VibratorEffect when available so intensity levels feel distinctly tuned on physical phones
    val vibratedWithHardware =
      runCatching {
        val vibrator =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
          } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
          }

        if (vibrator?.hasVibrator() == true) {
          when (intensity) {
            ManoHapticIntensity.OFF -> false
            ManoHapticIntensity.LIGHT -> {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
              } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10L, 65))
              } else {
                vibrator.vibrate(10L)
              }
              true
            }
            ManoHapticIntensity.MEDIUM -> {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectId =
                  if (isPrimaryInteraction) VibrationEffect.EFFECT_CLICK
                  else VibrationEffect.EFFECT_TICK
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
              } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                  VibrationEffect.createOneShot(if (isPrimaryInteraction) 18L else 14L, 140),
                )
              } else {
                vibrator.vibrate(if (isPrimaryInteraction) 18L else 14L)
              }
              true
            }
            ManoHapticIntensity.STRONG -> {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectId =
                  if (isPrimaryInteraction) VibrationEffect.EFFECT_HEAVY_CLICK
                  else VibrationEffect.EFFECT_DOUBLE_CLICK
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
              } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                  VibrationEffect.createOneShot(if (isPrimaryInteraction) 32L else 24L, 235),
                )
              } else {
                vibrator.vibrate(if (isPrimaryInteraction) 32L else 24L)
              }
              true
            }
          }
        } else {
          false
        }
      }.getOrDefault(false)

    if (vibratedWithHardware || view == null) return

    val feedbackConstant =
      when (intensity) {
        ManoHapticIntensity.OFF -> return
        ManoHapticIntensity.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
        ManoHapticIntensity.MEDIUM ->
          if (isPrimaryInteraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HapticFeedbackConstants.CONTEXT_CLICK
          } else {
            HapticFeedbackConstants.KEYBOARD_TAP
          }
        ManoHapticIntensity.STRONG -> HapticFeedbackConstants.LONG_PRESS
      }

    view.performHapticFeedback(
      feedbackConstant,
      HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
    )
  }
}
