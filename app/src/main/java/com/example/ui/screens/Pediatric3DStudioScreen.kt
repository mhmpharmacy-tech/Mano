package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import java.util.Locale
import kotlin.math.abs

private const val ASSET_VIRTUAL_HOST = "appassets.androidplatform.net"
private const val STUDIO_ENTRY_URL = "https://$ASSET_VIRTUAL_HOST/assets/three/index.html"

private data class GuidedWorkbenchStep(
  val stepNumber: Int,
  val shortTitleFa: String,
  val instructionFa: String,
)

private val guidedWorkbenchSteps =
  listOf(
    GuidedWorkbenchStep(
      stepNumber = 1,
      shortTitleFa = "۱. آماده‌سازی",
      instructionFa = "دستان را بشویید و غلظت بطری دارو (mg/mL) را با سن و وزن کودک تطبیق دهید.",
    ),
    GuidedWorkbenchStep(
      stepNumber = 2,
      shortTitleFa = "۲. تکان بطری",
      instructionFa = "پیش از کشیدن شربت، بطری را حدود ۲۰ ثانیه به آرامی تکان دهید تا سوسپانسیون یکنواخت شود.",
    ),
    GuidedWorkbenchStep(
      stepNumber = 3,
      shortTitleFa = "۳. خط هدف سرنگ",
      instructionFa = "لغزنده سرنگ را حرکت دهید تا پیستون دقیقاً روی حلقه سبز هدف قرار بگیرد و حباب هوا خارج شود.",
    ),
    GuidedWorkbenchStep(
      stepNumber = 4,
      shortTitleFa = "۴. تب و خوراندن",
      instructionFa = "دمای دماسنج دیجیتال را چک کنید و دارو را آرام و جرعه‌جرعه به کناره داخلی گونه کودک بدهید.",
    ),
  )

private val guidedWeighingSteps =
  listOf(
    GuidedWorkbenchStep(
      stepNumber = 1,
      shortTitleFa = "۱. سطح صاف و صفر",
      instructionFa = "ترازو را روی کف سخت و صاف (نه فرش) بگذارید، زیرانداز سبک را قرار دهید و عدد ترازو را صفر (0.00 kg) کنید.",
    ),
    GuidedWorkbenchStep(
      stepNumber = 2,
      shortTitleFa = "۲. لباس سبک و پوشک",
      instructionFa = "کاپشن، لباس ضخیم، کفش و پوشک خیس را از مانکن کودک جدا کنید تا وزن خالص و دقیق ثبت شود.",
    ),
    GuidedWorkbenchStep(
      stepNumber = 3,
      shortTitleFa = "۳. مرکز ترازو با مانکن",
      instructionFa = "مانکن کودک را آرام در مرکز حلقه ترازو بخوابانید یا بایستانید؛ بدون تکیه به دیوار یا گرفتن دست اطراف.",
    ),
    GuidedWorkbenchStep(
      stepNumber = 4,
      shortTitleFa = "۴. قفل عدد و ثبت وزن",
      instructionFa = "تا قفل شدن عدد روی نمایشگر (LOCKED) صبر کنید؛ وزن دقیق مبنای ایمن محاسبه دوز داروی کودک است.",
    ),
  )

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Pediatric3DStudioScreen(
  initialScene: String = "workbench",
  prefillMl: Float = 0f,
  prefillFever: Boolean = false,
  prefillFormulationId: String = "",
  prefillScannedDrugFa: String = "",
  childProfile: ChildProfileDemo? = null,
  childBirthDateEpochMs: Long? = null,
  latestTempCelsius: Float? = null,
  onBack: () -> Unit = {},
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val haptics = LocalHapticFeedback.current
  val isDark = LocalManoDarkTheme.current
  val cardSurface = manoCardSurface()
  val borderColor = manoBorderColor()
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val coralAccent = manoSemanticAccent(CoralCare, DarkCoral)

  val matchedFormulation =
    remember(prefillFormulationId) {
      LiquidFormulationId.entries.firstOrNull { it.id == prefillFormulationId }
    }
  val isDropsMode = matchedFormulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100
  val sliderMinMl = if (isDropsMode) 0.1f else 0.5f
  val sliderMaxMl = if (isDropsMode) 3.0f else 20.0f

  val syncedTargetMl: Float? =
    remember(prefillMl, childProfile, childBirthDateEpochMs, matchedFormulation) {
      when {
        prefillMl > 0f -> prefillMl.coerceIn(sliderMinMl, sliderMaxMl)
        childProfile != null || matchedFormulation != null -> {
          val ageM = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile, childBirthDateEpochMs)
          val weightKg = LiquidDoseVisualizerEngine.initialWeightKgFromProfile(childProfile)
          val eval =
            LiquidDoseVisualizerEngine.evaluate(
              formulation = matchedFormulation ?: LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
              ageMonths = ageM,
              weightKg = weightKg,
            )
          if (!eval.isBlocked && eval.recommendedMl > 0f) eval.recommendedMl.coerceIn(sliderMinMl, sliderMaxMl) else null
        }
        else -> null
      }
    }

  val childWeightKg =
    remember(childProfile) {
      val raw = childProfile?.let { LiquidDoseVisualizerEngine.initialWeightKgFromProfile(it) } ?: 11.5f
      raw.coerceIn(2.5f, 40.0f)
    }

  val defaultWeighingPosture =
    remember(childProfile) {
      when (childProfile?.ageBand) {
        AgeBand.NEWBORN,
        AgeBand.INFANT_3_12M -> "infant"
        else -> "toddler"
      }
    }

  var activeScene by remember(initialScene) {
    mutableStateOf(
      when (initialScene) {
        "mano" -> "mano"
        "weighing" -> "weighing"
        else -> "workbench"
      }
    )
  }
  var syringeMl by remember(syncedTargetMl) {
    mutableFloatStateOf(syncedTargetMl ?: 5.0f)
  }
  var feverAlert by remember(prefillFever, latestTempCelsius) {
    mutableStateOf(prefillFever || (latestTempCelsius != null && latestTempCelsius >= 38.0f))
  }
  var dockExpanded by remember(prefillMl, initialScene) {
    mutableStateOf(prefillMl > 0f || initialScene == "weighing")
  }
  var guidedStep by remember(prefillMl) {
    mutableIntStateOf(if (prefillMl > 0f) 3 else 0)
  }
  var weighingStep by remember(initialScene) {
    mutableIntStateOf(1)
  }
  var weighingPosture by remember(defaultWeighingPosture) {
    mutableStateOf(defaultWeighingPosture)
  }
  var selectedManoSquadId by remember { mutableStateOf("default") }
  var selectedPartLabelFa by remember(syncedTargetMl, childProfile, initialScene, prefillScannedDrugFa) {
    mutableStateOf(
      when {
        initialScene == "weighing" -> "راهنمای سه‌بعدی وزن‌کشی صحیح با مانکن کودک"
        prefillScannedDrugFa.isNotBlank() && syncedTargetMl != null ->
          "بطری تاییدشده: $prefillScannedDrugFa • هدف ${String.format(Locale.US, "%.1f", syncedTargetMl)} mL"
        syncedTargetMl != null && childProfile != null ->
          "همگام با ${childProfile.aliasFa}: دوز هدف ${String.format(Locale.US, "%.1f", syncedTargetMl)} mL روی سرنگ"
        syncedTargetMl != null ->
          "دوز هدف سرنگ: ${String.format(Locale.US, "%.1f", syncedTargetMl)} mL (حلقه سبز روی بدنه)"
        else ->
          "برای چرخش یا زوم، صحنه سه‌بعدی را لمس کنید"
      }
    )
  }
  var pageLoaded by remember { mutableStateOf(false) }

  val isTargetMatched =
    syncedTargetMl != null && abs(syringeMl - syncedTargetMl) <= 0.25f

  var wasMatched by remember { mutableStateOf(false) }
  LaunchedEffect(isTargetMatched) {
    if (isTargetMatched && !wasMatched) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    wasMatched = isTargetMatched
  }

  val webViewRef = remember { mutableStateOf<WebView?>(null) }

  fun runJs(script: String) {
    webViewRef.value?.post {
      webViewRef.value?.evaluateJavascript(script, null)
    }
  }

  LaunchedEffect(pageLoaded, activeScene) {
    if (pageLoaded) {
      runJs("window.Mano3D && window.Mano3D.setScene('$activeScene');")
    }
  }

  LaunchedEffect(pageLoaded, isDark) {
    if (pageLoaded) {
      runJs("window.Mano3D && window.Mano3D.setThemeMode($isDark);")
    }
  }

  LaunchedEffect(pageLoaded, syringeMl) {
    if (pageLoaded) {
      val formatted = String.format(Locale.US, "%.1f", syringeMl)
      runJs("window.Mano3D && window.Mano3D.setSyringeMl($formatted);")
    }
  }

  LaunchedEffect(pageLoaded, syncedTargetMl) {
    if (pageLoaded) {
      val targetFormatted = String.format(Locale.US, "%.1f", syncedTargetMl ?: 0f)
      runJs("window.Mano3D && window.Mano3D.setTargetMl($targetFormatted);")
    }
  }

  LaunchedEffect(pageLoaded, feverAlert, latestTempCelsius) {
    if (pageLoaded) {
      val tempArg = latestTempCelsius?.let { String.format(Locale.US, "%.1f", it) } ?: "null"
      runJs("window.Mano3D && window.Mano3D.setFeverMode($feverAlert, $tempArg);")
    }
  }

  LaunchedEffect(pageLoaded, guidedStep) {
    if (pageLoaded && guidedStep in 1..4) {
      runJs("window.Mano3D && window.Mano3D.setGuidedStep($guidedStep);")
    }
  }

  LaunchedEffect(pageLoaded, childWeightKg) {
    if (pageLoaded) {
      val weightStr = String.format(Locale.US, "%.1f", childWeightKg)
      runJs("window.Mano3D && window.Mano3D.setChildWeightKg($weightStr);")
    }
  }

  LaunchedEffect(pageLoaded, weighingPosture) {
    if (pageLoaded) {
      runJs("window.Mano3D && window.Mano3D.setWeighingPosture('$weighingPosture');")
    }
  }

  LaunchedEffect(pageLoaded, activeScene, weighingStep) {
    if (pageLoaded && activeScene == "weighing") {
      runJs("window.Mano3D && window.Mano3D.setWeighingStep($weighingStep);")
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          webViewRef.value?.onResume()
          runJs("window.Mano3D && window.Mano3D.setPaused(false);")
        }
        Lifecycle.Event.ON_PAUSE -> {
          runJs("window.Mano3D && window.Mano3D.setPaused(true);")
          webViewRef.value?.onPause()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      webViewRef.value?.apply {
        stopLoading()
        onPause()
        loadUrl("about:blank")
        clearHistory()
        removeJavascriptInterface("AndroidBridge")
        destroy()
      }
      webViewRef.value = null
    }
  }

  Box(modifier = Modifier.fillMaxSize().testTag("pediatric_3d_studio_screen")) {
    AndroidView(
      modifier = Modifier.fillMaxSize().testTag("pediatric_3d_webview"),
      factory = { ctx ->
        WebView(ctx).apply {
          setBackgroundColor(if (isDark) 0xFF08191F.toInt() else 0xFFF0F7F8.toInt())
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.allowFileAccess = false
          settings.allowContentAccess = false
          settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
          settings.setSupportMultipleWindows(false)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
          }
          settings.mediaPlaybackRequiresUserGesture = true
          settings.cacheMode = WebSettings.LOAD_DEFAULT

          addJavascriptInterface(
            object {
              @JavascriptInterface
              fun onPartSelected(partId: String) {
                post {
                  selectedPartLabelFa = partDescriptionFa(partId)
                }
              }
            },
            "AndroidBridge",
          )

          webViewClient =
            object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
              ): Boolean {
                return request?.url?.host != ASSET_VIRTUAL_HOST
              }

              override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
              ): WebResourceResponse? {
                val uri: Uri = request?.url ?: return null
                if (uri.host != ASSET_VIRTUAL_HOST) return null
                val rawPath = uri.path ?: return null
                if (!rawPath.startsWith("/assets/")) return null
                val assetPath = rawPath.removePrefix("/assets/")
                return runCatching {
                  val mime =
                    when {
                      assetPath.endsWith(".html") -> "text/html"
                      assetPath.endsWith(".js") || assetPath.endsWith(".mjs") -> "application/javascript"
                      assetPath.endsWith(".css") -> "text/css"
                      else -> "application/octet-stream"
                    }
                  val stream = ctx.assets.open(assetPath)
                  val headers = mapOf("Access-Control-Allow-Origin" to "*")
                  WebResourceResponse(mime, "UTF-8", 200, "OK", headers, stream)
                }.getOrNull()
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                pageLoaded = true
                runJs("window.Mano3D && window.Mano3D.setThemeMode($isDark);")
                runJs("window.Mano3D && window.Mano3D.setScene('$activeScene');")
              }
            }

          loadUrl(STUDIO_ENTRY_URL)
          webViewRef.value = this
        }
      },
    )

    // Top status & live child dose/temp sync pill
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Surface(
        color = if (isDark) DarkSurface.copy(alpha = 0.94f) else Color(0xECFFFFFF),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.ViewInAr,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = selectedPartLabelFa,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      if (syncedTargetMl != null || latestTempCelsius != null) {
        Surface(
          color =
            if (isTargetMatched) manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.94f, darkAlpha = 0.24f)
            else manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.94f, darkAlpha = 0.24f),
          shape = RoundedCornerShape(14.dp),
          border =
            BorderStroke(
              1.dp,
              if (isTargetMatched) successAccent.copy(alpha = 0.4f) else cautionAccent.copy(alpha = 0.4f),
            ),
          modifier = Modifier.testTag("studio_3d_target_sync_banner"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (isTargetMatched) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = successAccent, modifier = Modifier.size(15.dp))
            }
            val doseSummary =
              syncedTargetMl?.let {
                if (isTargetMatched) {
                  "تطابق دقیق با دوز هدف ${String.format(Locale.US, "%.1f", it)} mL"
                } else {
                  "دوز هدف: ${String.format(Locale.US, "%.1f", it)} mL (فعلی: ${String.format(Locale.US, "%.1f", syringeMl)})"
                }
              }
            val tempSummary =
              latestTempCelsius?.let { "آخرین تب ثبت‌شده: ${FeverDoseTimelineEngine.formatTempC(it)}" }
            Text(
              text = listOfNotNull(doseSummary, tempSummary).joinToString(" • "),
              style = MaterialTheme.typography.labelSmall,
              color = if (isTargetMatched) successAccent else cautionAccent,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (prefillScannedDrugFa.isNotBlank()) {
        Surface(
          color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.94f, darkAlpha = 0.24f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, successAccent.copy(alpha = 0.45f)),
          modifier = Modifier.testTag("studio_3d_scanned_bottle_banner"),
        ) {
          val concText = matchedFormulation?.concentrationLabelFa?.let { " • $it" }.orEmpty()
          Text(
            text = "برچسب تاییدشده اسکنر: $prefillScannedDrugFa$concText",
            style = MaterialTheme.typography.labelSmall,
            color = successAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }

    // Collapsible floating RGBA control dock (compact <= 15% collapsed height)
    Surface(
      color = if (isDark) DarkSurface.copy(alpha = 0.94f) else Color(0xEBFFFFFF),
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.dp, borderColor),
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .fillMaxWidth()
          .testTag("studio_3d_control_dock"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            item {
              FilterChip(
                selected = activeScene == "workbench",
                onClick = {
                  activeScene = "workbench"
                  selectedPartLabelFa = "میز مراقبت سه‌بعدی: سرنگ، شیشه دارو و تب‌سنج"
                },
                leadingIcon = {
                  Icon(Icons.Rounded.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("میز مراقبت", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("scene_switch_workbench"),
              )
            }
            item {
              FilterChip(
                selected = activeScene == "weighing",
                onClick = {
                  activeScene = "weighing"
                  dockExpanded = true
                  selectedPartLabelFa = "راهنمای سه‌بعدی وزن‌کشی صحیح با مانکن کودک"
                },
                leadingIcon = {
                  Icon(Icons.Rounded.MonitorWeight, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("وزن‌کشی کودک", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("scene_switch_weighing"),
              )
            }
            item {
              FilterChip(
                selected = activeScene == "mano",
                onClick = {
                  activeScene = "mano"
                  guidedStep = 0
                  selectedPartLabelFa = "شخصیت سه‌بعدی مانو و ایستگاه‌های مراقبت"
                },
                leadingIcon = {
                  Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("شخصیت مانو", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("scene_switch_mano"),
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = {
                guidedStep = 0
                runJs("window.Mano3D && window.Mano3D.resetCamera();")
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_3d_reset_camera"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "بازنشانی زاویه دوربین سه‌بعدی",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = { dockExpanded = !dockExpanded },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_3d_expand_dock"),
            ) {
              Icon(
                imageVector = if (dockExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                contentDescription = if (dockExpanded) "بستن تنظیمات ابزار" else "باز کردن تنظیمات ابزار",
              )
            }
          }
        }

        AnimatedVisibility(visible = dockExpanded) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
            if (activeScene == "workbench") {
              // 4-Step Interactive Guided Walkthrough Selector
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "آموزش گام‌به‌گام سه‌بعدی (تور ۴ مرحله‌ای):",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth().testTag("studio_3d_guided_steps_row"),
                ) {
                  items(guidedWorkbenchSteps, key = { it.stepNumber }) { stepInfo ->
                    FilterChip(
                      selected = guidedStep == stepInfo.stepNumber,
                      onClick = {
                        guidedStep = stepInfo.stepNumber
                        selectedPartLabelFa = stepInfo.shortTitleFa
                      },
                      label = { Text(stepInfo.shortTitleFa, style = MaterialTheme.typography.labelSmall) },
                      colors =
                        FilterChipDefaults.filterChipColors(
                          containerColor = cardSurface,
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("guided_step_${stepInfo.stepNumber}"),
                    )
                  }
                }
                val activeStepObj = guidedWorkbenchSteps.firstOrNull { it.stepNumber == guidedStep }
                if (activeStepObj != null) {
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("studio_3d_guided_instruction"),
                  ) {
                    Text(
                      text = activeStepObj.instructionFa,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "حجم سرنگ خوراکی: ${String.format(Locale.US, "%.1f", syringeMl)} mL",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (isTargetMatched) successAccent else MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  OutlinedButton(
                    onClick = { runJs("window.Mano3D && window.Mano3D.triggerBottleShake();") },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_3d_shake_bottle_btn"),
                  ) {
                    Icon(Icons.Rounded.Vibration, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تکان بطری", style = MaterialTheme.typography.labelSmall)
                  }
                  TextButton(onClick = { runJs("window.Mano3D && window.Mano3D.focusTarget('syringe');") }) {
                    Text("سرنگ", style = MaterialTheme.typography.labelSmall)
                  }
                  TextButton(onClick = { runJs("window.Mano3D && window.Mano3D.focusTarget('thermometer');") }) {
                    Text("تب‌سنج", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
              Slider(
                value = syringeMl,
                onValueChange = { raw ->
                  syringeMl =
                    if (isDropsMode) ((raw * 10f).toInt() / 10f).coerceIn(sliderMinMl, sliderMaxMl)
                    else ((raw * 2f).toInt() / 2f).coerceIn(sliderMinMl, sliderMaxMl)
                },
                valueRange = sliderMinMl..sliderMaxMl,
                modifier = Modifier.fillMaxWidth().testTag("studio_3d_syringe_slider"),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = null,
                    tint = if (feverAlert) coralAccent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text =
                      if (feverAlert) {
                        val tStr = latestTempCelsius?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "38.5°C"
                        "وضعیت تب دماسنج: $tStr"
                      } else {
                        "وضعیت دماسنج: دمای عادی 37.2°C"
                      },
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
                Switch(
                  checked = feverAlert,
                  onCheckedChange = { feverAlert = it },
                  modifier = Modifier.testTag("studio_3d_fever_switch"),
                )
              }
            } else if (activeScene == "weighing") {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "راهنمای ۴ مرحله‌ای وزن‌کشی با مانکن کودک:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "وزن ثبت‌شده: ${String.format(Locale.US, "%.1f", childWeightKg)} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = successAccent,
                    fontWeight = FontWeight.Bold,
                  )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                  FilterChip(
                    selected = weighingPosture == "infant",
                    onClick = { weighingPosture = "infant" },
                    label = { Text("حالت نوزاد (خوابیده)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("weighing_posture_infant"),
                  )
                  FilterChip(
                    selected = weighingPosture == "toddler",
                    onClick = { weighingPosture = "toddler" },
                    label = { Text("حالت کودک (ایستاده)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("weighing_posture_toddler"),
                  )
                }

                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth().testTag("weighing_guided_steps_row"),
                ) {
                  items(guidedWeighingSteps, key = { it.stepNumber }) { stepInfo ->
                    FilterChip(
                      selected = weighingStep == stepInfo.stepNumber,
                      onClick = {
                        weighingStep = stepInfo.stepNumber
                        selectedPartLabelFa = stepInfo.shortTitleFa
                      },
                      label = { Text(stepInfo.shortTitleFa, style = MaterialTheme.typography.labelSmall) },
                      colors =
                        FilterChipDefaults.filterChipColors(
                          containerColor = cardSurface,
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("weighing_step_${stepInfo.stepNumber}"),
                    )
                  }
                }

                val activeWeighingStepObj =
                  guidedWeighingSteps.firstOrNull { it.stepNumber == weighingStep } ?: guidedWeighingSteps.first()
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().testTag("weighing_guided_instruction"),
                ) {
                  Text(
                    text = activeWeighingStepObj.instructionFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  OutlinedButton(
                    onClick = { weighingStep = if (weighingStep <= 1) 4 else weighingStep - 1 },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("weighing_prev_step_btn"),
                  ) {
                    Text("مرحله قبل", style = MaterialTheme.typography.labelMedium)
                  }
                  TextButton(
                    onClick = { weighingStep = if (weighingStep >= 4) 1 else weighingStep + 1 },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("weighing_next_step_btn"),
                  ) {
                    Text(
                      text = if (weighingStep < 4) "مرحله بعد (${weighingStep + 1}/4)" else "شروع مجدد از مرحله ۱",
                      style = MaterialTheme.typography.labelMedium,
                    )
                  }
                }
              }
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val squadChoices =
                  listOf(
                    "default" to "مانو اصلی",
                    "mano_drug" to "Mano Drug",
                    "mano_growth" to "Mano Growth",
                    "mano_skin" to "Mano Skin",
                    "mano_nutrition" to "Mano Nutrition",
                    "mano_care" to "Mano Care",
                    "mano_connect" to "Mano Connect",
                  )
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth().testTag("studio_3d_mano_squad_row"),
                ) {
                  items(squadChoices, key = { it.first }) { (id, label) ->
                    FilterChip(
                      selected = selectedManoSquadId == id,
                      onClick = {
                        selectedManoSquadId = id
                        selectedPartLabelFa = partDescriptionFa(if (id == "default") "mano" else id)
                        runJs("window.Mano3D && window.Mano3D.selectSquadMember('$id');")
                      },
                      label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_3d_squad_$id"),
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "روی مانو یا کاراکترهای رنگی مدار ضربه بزنید تا با متریال PBR برجسته شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                  )
                  TextButton(onClick = onBack) {
                    Text("بستن")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun partDescriptionFa(partId: String): String {
  return when (partId) {
    "syringe" -> "سرنگ خوراکی مدرج mL (ساختار شفاف برای خواندن دقیق دوز)"
    "bottle" -> "بطری داروی کودک با درپوش ایمن و برچسب غلظت"
    "thermometer" -> "دماسنج دیجیتال کودک با نمایشگر تغییر وضعیت تب"
    "scale" -> "ترازوی دیجیتال کودک: باید روی کف سخت و صاف قرار گیرد"
    "scale_lcd" -> "نمایشگر ترازو: پیش از گذاشتن کودک عدد را روی 0.00 kg صفر (Tare) کنید"
    "blanket" -> "زیرانداز سبک و نرم: قبل از صفر کردن ترازو روی کفه قرار دهید"
    "mannequin" -> "مانکن سه‌بعدی کودک: با لباس سبک و در مرکز حلقه ترازو قرار می‌گیرد"
    "mano" -> "مانو اصلی (پرسلن مات و شال نعنایی): لمس شما لبخند و واکنش آرام ایجاد می‌کند"
    "mano_drug" -> "Mano Drug (سبز یشمی): پایش ایمنی دارو و دوز دقیق mL"
    "mano_growth" -> "Mano Growth (آبی): پایش رشد، قد و وزن بر اساس منحنی WHO"
    "mano_skin" -> "Mano Skin (مرجانی): مراقبت ملایم پوست و پیشگیری از سوختگی پوشک"
    "mano_nutrition" -> "Mano Nutrition (سبز زیتونی): تغذیه تکمیلی و سلامت گوارشی کودک"
    "mano_care" -> "Mano Care (فیروزه‌ای): پایش تب، علائم هشدار و خط زمانی مراقبت"
    "mano_connect" -> "Mano Connect (بنفش): ارتباط مستقیم و تایید علمی داروساز"
    "station_fever" -> "ایستگاه پایش تب و علائم هشدار"
    "station_syringe" -> "ایستگاه آموزش دوز دقیق سرنگ mL"
    "station_hydration" -> "ایستگاه آبرسانی و مایعات حمایتی"
    "station_sleep" -> "ایستگاه آرامش و روتین خواب کودک"
    else -> "نمای سه‌بعدی مانو کید"
  }
}
