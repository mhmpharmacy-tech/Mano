package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ManoThemeMode
import com.example.domain.util.ManoDomainLinks
import com.example.notifications.TreatmentPlanNotificationHelper
import com.example.notifications.VaccineNotificationHelper
import com.example.ui.ManoApp
import com.example.ui.auth.configureAppCheck
import com.example.ui.theme.ManoKidsTheme
import com.example.ui.viewmodel.ManoViewModel
import com.example.widget.FeverTimelineWidgetStore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  private val manoViewModel: ManoViewModel by viewModels()
  private var openTimelineState by mutableStateOf(false)
  private var openVaccinesState by mutableStateOf(false)
  private var openTreatmentPlanState by mutableStateOf(false)
  private var pendingDeepLinkUriState by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val launchIntent = intent
    openTimelineState = launchIntent?.getBooleanExtra(FeverTimelineWidgetStore.EXTRA_OPEN_TIMELINE, false) == true
    openVaccinesState = launchIntent?.getBooleanExtra(VaccineNotificationHelper.EXTRA_OPEN_VACCINES, false) == true
    openTreatmentPlanState = launchIntent?.getBooleanExtra(TreatmentPlanNotificationHelper.EXTRA_OPEN_TREATMENT_PLAN, false) == true
    pendingDeepLinkUriState = extractDeepLinkUri(launchIntent)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    )
    setContent {
      LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
          runCatching { configureAppCheck(applicationContext, launchIntent) }
        }
      }

      val themeMode by manoViewModel.themeMode.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val isDarkTheme =
        when (themeMode) {
          ManoThemeMode.SYSTEM -> systemDark
          ManoThemeMode.LIGHT -> false
          ManoThemeMode.DARK -> true
        }

      LaunchedEffect(isDarkTheme) {
        enableEdgeToEdge(
          statusBarStyle =
            if (isDarkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
          navigationBarStyle =
            if (isDarkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
      }

      ManoKidsTheme(darkTheme = isDarkTheme) {
        ManoApp(
          viewModel = manoViewModel,
          openTimelineOnLaunch = openTimelineState,
          onConsumedTimelineLaunch = { openTimelineState = false },
          openVaccinesOnLaunch = openVaccinesState,
          onConsumedVaccinesLaunch = { openVaccinesState = false },
          openTreatmentPlanOnLaunch = openTreatmentPlanState,
          onConsumedTreatmentPlanLaunch = { openTreatmentPlanState = false },
          pendingDeepLinkUri = pendingDeepLinkUriState,
          onConsumedDeepLinkUri = { pendingDeepLinkUriState = null },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra(FeverTimelineWidgetStore.EXTRA_OPEN_TIMELINE, false)) {
      openTimelineState = true
    }
    if (intent.getBooleanExtra(VaccineNotificationHelper.EXTRA_OPEN_VACCINES, false)) {
      openVaccinesState = true
    }
    if (intent.getBooleanExtra(TreatmentPlanNotificationHelper.EXTRA_OPEN_TREATMENT_PLAN, false)) {
      openTreatmentPlanState = true
    }
    extractDeepLinkUri(intent)?.let { uri ->
      pendingDeepLinkUriState = uri
    }
  }

  private fun extractDeepLinkUri(intent: Intent?): String? {
    if (intent?.action != Intent.ACTION_VIEW) return null
    val uri = intent.data ?: return null
    return if (ManoDomainLinks.isSupportedHost(uri.host)) uri.toString() else null
  }
}
