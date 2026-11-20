package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.SyrupDosageForm
import com.example.domain.model.SyrupIngredientKey
import com.example.domain.model.SyrupScanConfidence
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.ui.components.AlertSeverity
import com.example.ui.components.LtrText
import com.example.ui.components.ManoBannerFrame
import com.example.ui.components.ManoIconMedallion
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.SageCare
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoElevatedSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.SyrupImageHelper
import com.example.ui.viewmodel.SyrupScannerUiState
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyrupScannerScreen(
  scannerUiState: SyrupScannerUiState,
  savedScans: List<SavedSyrupScan>,
  childProfile: ChildProfileDemo?,
  childBirthDateEpochMs: Long? = null,
  isCloudSignedIn: Boolean,
  onAnalyzeBitmap: (Bitmap) -> Unit,
  onStartManualFallback: () -> Unit,
  onEditDrugName: (String) -> Unit,
  onEditIngredient: (SyrupIngredientKey) -> Unit,
  onEditDosageForm: (SyrupDosageForm) -> Unit,
  onAdjustStrength: (mgAmount: Double, perMlAmount: Double) -> Unit,
  onSelectCalibratedPreset: (LiquidFormulationId) -> Unit,
  onSetParentVerified: (Boolean) -> Unit,
  onRefreshChildContext: () -> Unit,
  onConfirmSaveAndOpen3D: (formulationId: String, drugNameFa: String, targetMl: Float) -> Unit,
  onConfirmSaveOnly: () -> Unit,
  onLoadSavedScan: (SavedSyrupScan) -> Unit,
  onDeleteSavedScan: (SavedSyrupScan) -> Unit,
  onResetScanner: () -> Unit,
  onOpenMyChildProfile: () -> Unit,
  onOpenMedicinesSyringe: () -> Unit,
  onOpenAskPharmacist: () -> Unit,
  onFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val borderColor = manoBorderColor()
  val cardSurface = manoCardSurface()
  val elevatedSurface = manoElevatedSurface()
  val primaryAccent = MaterialTheme.colorScheme.primary
  val skyAccent = manoSemanticAccent(SkyCare, DarkSky)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val coralAccent = manoSemanticAccent(CoralCare, DarkCoral)

  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  LaunchedEffect(childProfile, childBirthDateEpochMs) {
    onRefreshChildContext()
  }

  val takePictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
      val uri = pendingCameraUri
      pendingCameraUri = null
      if (!saved || uri == null) {
        if (!saved) {
          onFeedback("عکسبرداری لغو شد.")
        }
        return@rememberLauncherForActivityResult
      }
      scope.launch {
        val decoded = SyrupImageHelper.decodeScaledBitmap(context, uri)
        if (decoded != null) {
          onAnalyzeBitmap(decoded)
        } else {
          onFeedback("خواندن تصویر گرفته شده ناموفق بود؛ لطفاً دوباره عکس بگیرید یا از گالری انتخاب کنید.")
        }
      }
    }

  val pickMediaLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        val decoded = SyrupImageHelper.decodeScaledBitmap(context, uri)
        if (decoded != null) {
          onAnalyzeBitmap(decoded)
        } else {
          onFeedback("بارگذاری تصویر از گالری ناموفق بود. لطفاً تصویر واضح‌تری انتخاب کنید.")
        }
      }
    }

  fun launchCameraCapture() {
    runCatching {
      val uri = SyrupImageHelper.createTempScannerImageUri(context)
      pendingCameraUri = uri
      takePictureLauncher.launch(uri)
    }.onFailure { err ->
      pendingCameraUri = null
      val msg =
        if (err is ActivityNotFoundException) {
          "دوربین در این دستگاه در دسترس نیست؛ لطفاً عکس برچسب را از گالری انتخاب کنید."
        } else {
          "باز کردن دوربین انجام نشد: ${err.localizedMessage ?: "خطای دستگاه"}"
        }
      onFeedback(msg)
    }
  }

  val ageMonths =
    remember(childProfile, childBirthDateEpochMs) {
      LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile, childBirthDateEpochMs)
    }
  val weightKg = remember(childProfile) { LiquidDoseVisualizerEngine.initialWeightKgFromProfile(childProfile) }
  val childContextLine =
    remember(childProfile, ageMonths, weightKg) {
      if (childProfile != null) {
        "${childProfile.aliasFa} • ${LiquidDoseVisualizerEngine.formatAgeFa(ageMonths)} • وزن ${LiquidDoseVisualizerEngine.formatKg(weightKg)}"
      } else {
        "پروفایل کودک ثبت نشده است."
      }
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("syrup_scanner_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // HERO BANNER & CHILD CONTEXT CARD
      item {
        Surface(
          color = cardSurface,
          shape = RoundedCornerShape(22.dp),
          border = BorderStroke(1.dp, borderColor),
          modifier = Modifier.fillMaxWidth().testTag("syrup_scanner_hero_card"),
        ) {
          Column(
            modifier =
              Modifier
                .background(
                  Brush.linearGradient(
                    colors =
                      listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f),
                      )
                  )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            ManoBannerFrame(
              resId = R.drawable.img_syrup_scanner_banner,
              contentDescription = "مانو با اسکنر هوشمند برچسب شربت و سرنگ مدرج کودک",
              aspectRatio = 16f / 9f,
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              ManoIconMedallion(
                icon = Icons.Rounded.QrCodeScanner,
                accentColor = skyAccent,
                iconKind = PediatricIconKind.MEDICINE,
                containerSize = 50.dp,
                iconSize = 26.dp,
                cornerRadius = 15.dp,
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                  color = primaryAccent.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = stringResource(R.string.syrup_scanner_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
                Text(
                  text = stringResource(R.string.syrup_scanner_title),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.syrup_scanner_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Surface(
              color = elevatedSurface,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, borderColor),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = childContextLine,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = onOpenMyChildProfile,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("scanner_open_child_profile_btn"),
                ) {
                  Text("تنظیم پروفایل", style = MaterialTheme.typography.labelMedium)
                }
              }
            }

            if (childProfile == null) {
              SeverityAlert(
                severity = AlertSeverity.CAUTION,
                title = "ثبت سن و وزن برای دقت دوز",
                body = "برای جلوگیری از محاسبه بر پایه مقادیر پیش‌فرض، پیش از تایید دوز، پروفایل سن و وزن کودک را تکمیل کنید.",
                modifier = Modifier.testTag("scanner_missing_profile_caution"),
              )
            }
          }
        }
      }

      // CAPTURE / PICKER ACTION CARD
      item {
        SectionCard {
          Text(
            text = "۱. تصویر برچسب یا جعبه دارو را بگیرید",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "نور کافی روی قسمت غلظت (مثلاً 120 mg / 5 mL یا 100 mg / mL) بتابانید؛ در محیط کم‌نور، فلش دوربین را روشن کنید و زاویه بطری را برای حذف بازتاب نور کمی کج نگه دارید تا برچسب فارسی دقیق‌تر خوانده شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { launchCameraCapture() },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("scanner_camera_btn"),
            ) {
              Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("عکس با دوربین", maxLines = 2, textAlign = TextAlign.Center)
            }
            OutlinedButton(
              onClick = {
                pickMediaLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("scanner_gallery_btn"),
            ) {
              Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("انتخاب از گالری", maxLines = 2, textAlign = TextAlign.Center)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = onStartManualFallback,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("scanner_manual_fallback_btn"),
            ) {
              Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("انتخاب/ورود دستی غلظت بطری")
            }
            if (scannerUiState !is SyrupScannerUiState.Idle) {
              TextButton(
                onClick = onResetScanner,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("scanner_reset_btn"),
              ) {
                Text("اسکن جدید")
              }
            }
          }
        }
      }

      // ANALYZING / ERROR / VERIFICATION WORKBENCH
      when (scannerUiState) {
        is SyrupScannerUiState.Idle -> {
          item {
            SeverityAlert(
              severity = AlertSeverity.INFO,
              title = "تطبیق اجباری توسط والد پیش از پر شدن سرنگ",
              body = "تشخیص تصویری فقط کمک‌آموزشی است. قبل از انتقال خودکار عدد mL به سرنگ سه‌بعدی، می‌توانید نام دارو و غلظت (mg و mL) را مستقیماً ویرایش و تایید کنید.",
              showCautionMascot = false,
            )
          }
        }

        is SyrupScannerUiState.Analyzing -> {
          item {
            SectionCard(modifier = Modifier.testTag("scanner_loading_card")) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                    Text("در حال خواندن برچسب با تصویرخوان هوشمند...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  }
                  Text(
                    text = "در حال تفکیک قطره ۱۰۰ mg/mL، شربت ۱۲۰/۵ و سوسپانسیون‌های ۱۶۰/۵ یا ایبوپروفن...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
              ) {
                Image(
                  bitmap = scannerUiState.previewBitmap.asImageBitmap(),
                  contentDescription = "پیش‌نمایش تصویر در حال تحلیل",
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(10.dp)),
                )
              }
            }
          }
        }

        is SyrupScannerUiState.Error -> {
          item {
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("scanner_error_card"),
            ) {
              SeverityAlert(
                severity = AlertSeverity.DANGER,
                title = "خطا در تحلیل تصویر برچسب",
                body = scannerUiState.messageFa,
                showCautionMascot = true,
                actionLabel = "انتخاب دستی غلظت بطری",
                onAction = onStartManualFallback,
              )
              if (scannerUiState.previewBitmap != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  Button(
                    onClick = { onAnalyzeBitmap(scannerUiState.previewBitmap) },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("scanner_retry_btn"),
                  ) {
                    Text("تلاش مجدد با همین تصویر")
                  }
                }
              }
            }
          }
        }

        is SyrupScannerUiState.Review -> {
          val ext = scannerUiState.extraction
          item {
            SectionCard(modifier = Modifier.testTag("scanner_review_card")) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "۲. بازبینی و ویرایش غلظت خوانده‌شده",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f),
                )
                val (badgeBg, badgeTint) =
                  when (ext.confidence) {
                    SyrupScanConfidence.HIGH ->
                      manoTintedSurface(SuccessSurface, DarkSage) to successAccent
                    SyrupScanConfidence.MEDIUM ->
                      manoTintedSurface(InfoSurface, DarkSky) to manoSemanticAccent(InfoBlue, DarkSky)
                    SyrupScanConfidence.LOW ->
                      manoTintedSurface(CautionSurface, DarkGold) to cautionAccent
                  }
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                  Text(
                    text = ext.confidence.labelFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeTint,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }

              if (scannerUiState.previewBitmap != null) {
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, borderColor),
                  modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                ) {
                  Image(
                    bitmap = scannerUiState.previewBitmap.asImageBitmap(),
                    contentDescription = "تصویر برچسب بطری دارو",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(10.dp)),
                  )
                }
              }

              OutlinedTextField(
                value = ext.drugNameFa,
                onValueChange = onEditDrugName,
                label = { Text("نام دارو (قابل ویرایش توسط والد)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("scanner_drug_name_input"),
              )

              Text(
                text = "فرمولاسیون‌های استاندارد کالیبره‌شده مانو:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                LiquidFormulationId.entries.forEach { preset ->
                  FilterChip(
                    selected = ext.matchedFormulation == preset,
                    onClick = { onSelectCalibratedPreset(preset) },
                    label = { Text(preset.shortLabelFa, style = MaterialTheme.typography.labelSmall) },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                      ),
                    modifier =
                      Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("scanner_preset_${preset.id}"),
                  )
                }
              }

              Text(
                text = "ماده موثره و شکل دارو:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                SyrupIngredientKey.entries.forEach { key ->
                  FilterChip(
                    selected = ext.ingredientKey == key,
                    onClick = { onEditIngredient(key) },
                    label = { Text(key.labelFa, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("scanner_ing_${key.name.lowercase()}"),
                  )
                }
              }
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                SyrupDosageForm.entries.forEach { form ->
                  FilterChip(
                    selected = ext.dosageForm == form,
                    onClick = { onEditDosageForm(form) },
                    label = { Text(form.labelFa, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("scanner_form_${form.name.lowercase()}"),
                  )
                }
              }

              // Strength Adjuster Card
              Surface(
                color = elevatedSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "غلظت خوانده‌شده روی بطری:",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LtrText(
                      text = ext.concentrationDisplayFa,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.testTag("scanner_concentration_label"),
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    StrengthStepperBox(
                      label = "صورت (mg)",
                      valueText = "${ext.mgAmount.toInt()} mg",
                      onMinus = {
                        val next = (ext.mgAmount - 5.0).coerceAtLeast(1.0)
                        onAdjustStrength(next, ext.perMlAmount)
                      },
                      onPlus = {
                        val next = (ext.mgAmount + 5.0).coerceAtMost(1000.0)
                        onAdjustStrength(next, ext.perMlAmount)
                      },
                      testTagPrefix = "scanner_mg",
                      modifier = Modifier.weight(1f),
                    )
                    StrengthStepperBox(
                      label = "مخرج (mL)",
                      valueText = "${String.format(Locale.US, "%.1f", ext.perMlAmount)} mL",
                      onMinus = {
                        val step = if (ext.perMlAmount <= 1.0) 0.5 else 1.0
                        val next = (ext.perMlAmount - step).coerceAtLeast(0.5)
                        onAdjustStrength(ext.mgAmount, next)
                      },
                      onPlus = {
                        val step = if (ext.perMlAmount < 1.0) 0.5 else 1.0
                        val next = (ext.perMlAmount + step).coerceAtMost(50.0)
                        onAdjustStrength(ext.mgAmount, next)
                      },
                      testTagPrefix = "scanner_per_ml",
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }

              // Mix-up or Unverified Guardrail Banner
              if (ext.mixUpWarningFa != null || ext.dosePreview?.isBlocked == true) {
                val warnTitle =
                  when {
                    ext.dosePreview?.isBlocked == true -> ext.dosePreview.alertTitleFa
                    ext.warningSeverity == AlertSeverity.DANGER -> "هشدار حیاتی تطبیق غلظت!"
                    else -> "نکته ایمنی بطری دارو"
                  }
                val warnBody =
                  if (ext.dosePreview?.isBlocked == true) {
                    ext.dosePreview.alertBodyFa
                  } else {
                    ext.mixUpWarningFa ?: ext.safetyNoteFa
                  }
                SeverityAlert(
                  severity = ext.warningSeverity,
                  title = warnTitle,
                  body = warnBody,
                  showCautionMascot = ext.warningSeverity == AlertSeverity.DANGER,
                  modifier = Modifier.testTag("scanner_mixup_alert"),
                )
              }

              // Calculated Pediatric Syringe Target Preview
              val preview = ext.dosePreview
              if (preview != null) {
                val previewSurface =
                  if (preview.isBlocked) manoTintedSurface(DangerSurface, DarkCoral, darkAlpha = 0.20f)
                  else manoTintedSurface(SuccessSurface, DarkSage, darkAlpha = 0.20f)
                val previewTint =
                  if (preview.isBlocked) DangerCrimson else successAccent
                Surface(
                  color = previewSurface,
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, previewTint.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth().testTag("scanner_dose_preview_card"),
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = if (preview.isBlocked) "دوز خانگی قفل‌شده:" else "مقدار هدف پیشنهادی سرنگ:",
                        style = MaterialTheme.typography.labelLarge,
                        color = previewTint,
                        fontWeight = FontWeight.Bold,
                      )
                      LtrText(
                        text = LiquidDoseVisualizerEngine.formatMl(preview.recommendedMl),
                        color = previewTint,
                        modifier = Modifier.testTag("scanner_calculated_ml_label"),
                      )
                    }
                    Text(
                      text =
                        if (preview.isBlocked) preview.alertBodyFa
                        else "${preview.formulation.titleFa} • محدوده ایمن هر نوبت: ${LiquidDoseVisualizerEngine.formatMl(preview.minSafeMl)} تا ${LiquidDoseVisualizerEngine.formatMl(preview.maxSafeMl)} (${preview.recommendedMg} mg)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }

              // Mandatory Parent Verification Checkbox
              Surface(
                color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.55f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable { onSetParentVerified(!scannerUiState.parentVerified) }
                    .minimumInteractiveComponentSize()
                    .testTag("scanner_parent_confirm_row"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                    checked = scannerUiState.parentVerified,
                    onCheckedChange = onSetParentVerified,
                    modifier = Modifier.testTag("scanner_parent_confirm_checkbox"),
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text = "نام دارو و اعداد غلظت (${ext.concentrationDisplayFa}) را با برچسب واقعی بطری تطبیق داده و تایید می‌کنم.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                  )
                }
              }

              // Primary Confirm & 3D Studio Auto-Fill CTAs
              if (ext.matchedFormulation == null) {
                Button(
                  onClick = onOpenAskPharmacist,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .testTag("scanner_ask_pharmacist_btn"),
                ) {
                  Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("پرسش از داروساز درباره این بطری")
                }
              } else {
                Button(
                  onClick = {
                    val ml = preview?.recommendedMl ?: 0f
                    onConfirmSaveAndOpen3D(
                      ext.matchedFormulation.id,
                      ext.drugNameFa,
                      ml,
                    )
                  },
                  enabled = scannerUiState.parentVerified && ext.canAutoFillSyringe,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .testTag("scanner_autofill_3d_btn"),
                ) {
                  Icon(Icons.Rounded.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  val labelMl = preview?.recommendedMl ?: 0f
                  Text(
                    text =
                      if (ext.canAutoFillSyringe) {
                        "تایید و پر کردن سرنگ سه‌بعدی (${LiquidDoseVisualizerEngine.formatMl(labelMl)})"
                      } else {
                        "پر کردن سرنگ در دسترس نیست؛ سن، وزن و غلظت را بررسی کنید"
                      },
                    fontWeight = FontWeight.Bold,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onConfirmSaveOnly,
                  enabled = scannerUiState.parentVerified,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("scanner_save_only_btn"),
                ) {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("ذخیره در لیست")
                }
                OutlinedButton(
                  onClick = onOpenMedicinesSyringe,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("scanner_open_medicines_btn"),
                ) {
                  Icon(Icons.Rounded.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("آزمایشگاه سرنگ")
                }
              }
            }
          }
        }
      }

      // SAVED / SYNCED SYRUP SHELF
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "بطری‌های تایید و ذخیره‌شده (${savedScans.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (isCloudSignedIn) "همگام با ابر مانو" else "ذخیره آفلاین روی دستگاه",
            style = MaterialTheme.typography.labelSmall,
            color = if (isCloudSignedIn) successAccent else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (savedScans.isEmpty()) {
        item {
          SectionCard(modifier = Modifier.testTag("scanner_saved_empty_card")) {
            Text(
              text = "هنوز اسکن تاییدشده‌ای ذخیره نشده است. پس از عکسبرداری و تایید والد، بطری‌ها برای استفاده سریع بعدی اینجا ذخیره می‌شوند.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        items(savedScans, key = { it.id }) { scan ->
          Surface(
            color = cardSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable { onLoadSavedScan(scan) }
                .testTag("saved_scan_open_${scan.id}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              ManoIconMedallion(
                icon = Icons.Rounded.MedicalServices,
                accentColor = if (scan.mgPerMl >= 80.0) coralAccent else primaryAccent,
                iconKind = PediatricIconKind.MEDICINE,
                containerSize = 42.dp,
                iconSize = 20.dp,
                cornerRadius = 12.dp,
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = scan.drugNameFa,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  LtrText(
                    text = scan.concentrationDisplayFa.ifBlank { "${scan.mgAmount.toInt()} mg / ${scan.perMlAmount} mL" },
                    color = primaryAccent,
                  )
                  Icon(
                    imageVector = if (scan.isSyncedCloud) Icons.Rounded.CloudDone else Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = if (scan.isSyncedCloud) successAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = if (scan.isSyncedCloud) "همگام‌سازی ابری" else "ذخیره محلی",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (scan.isSyncedCloud) successAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              IconButton(
                onClick = { onDeleteSavedScan(scan) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("saved_scan_delete_${scan.id}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "حذف اسکن ${scan.drugNameFa}",
                  tint = coralAccent,
                )
              }
            }
          }
        }
      }

      item { Spacer(Modifier.height(20.dp)) }
    }
  }
}

@Composable
private fun StrengthStepperBox(
  label: String,
  valueText: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
  testTagPrefix: String,
  modifier: Modifier = Modifier,
) {
  val borderColor = manoBorderColor()
  Surface(
    color = manoCardSurface(),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        IconButton(
          onClick = onMinus,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("${testTagPrefix}_minus"),
        ) {
          Icon(Icons.Rounded.Remove, contentDescription = "کاهش $label")
        }
        LtrText(text = valueText, color = MaterialTheme.colorScheme.onSurface)
        IconButton(
          onClick = onPlus,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("${testTagPrefix}_plus"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "افزایش $label")
        }
      }
    }
  }
}
