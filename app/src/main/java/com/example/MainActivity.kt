package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.domain.util.configureAppCheck
import com.example.ui.ManoApp
import com.example.ui.theme.ManoKidsTheme

class MainActivity : ComponentActivity() {
  private var openTimelineOnLaunch by mutableStateOf(false)
  private var openVaccinesOnLaunch by mutableStateOf(false)
  private var openTreatmentPlanOnLaunch by mutableStateOf(false)
  private var pendingDeepLinkUri by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mandatory App Check initialization BEFORE any Compose UI, ViewModel, or Firebase call
    configureAppCheck(this, intent)

    handleIncomingIntent(intent)
    enableEdgeToEdge()

    setContent {
      ManoKidsTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          ManoApp(
            openTimelineOnLaunch = openTimelineOnLaunch,
            onConsumedTimelineLaunch = { openTimelineOnLaunch = false },
            openVaccinesOnLaunch = openVaccinesOnLaunch,
            onConsumedVaccinesLaunch = { openVaccinesOnLaunch = false },
            openTreatmentPlanOnLaunch = openTreatmentPlanOnLaunch,
            onConsumedTreatmentPlanLaunch = { openTreatmentPlanOnLaunch = false },
            pendingDeepLinkUri = pendingDeepLinkUri,
            onConsumedDeepLinkUri = { pendingDeepLinkUri = null },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent == null) return
    val target = intent.getStringExtra(EXTRA_MANO_LAUNCH_TARGET)
    when (target) {
      LAUNCH_TARGET_TIMELINE -> openTimelineOnLaunch = true
      LAUNCH_TARGET_VACCINES -> openVaccinesOnLaunch = true
      LAUNCH_TARGET_TREATMENT_PLAN -> openTreatmentPlanOnLaunch = true
    }
    intent.dataString?.takeIf { it.isNotBlank() }?.let { uri ->
      pendingDeepLinkUri = uri
    }
  }

  companion object {
    const val EXTRA_MANO_LAUNCH_TARGET = "mano_launch_target"
    const val LAUNCH_TARGET_TIMELINE = "timeline"
    const val LAUNCH_TARGET_VACCINES = "vaccines"
    const val LAUNCH_TARGET_TREATMENT_PLAN = "treatment_plan"
  }
}
