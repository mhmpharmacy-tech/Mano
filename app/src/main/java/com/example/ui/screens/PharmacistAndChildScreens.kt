package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.MascotPose
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SourceReference
import com.example.domain.util.ManoDomainLinks
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ConsentBox
import com.example.ui.components.LtrText
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.ProgressTimeline
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.components.SymptomChip
import com.example.ui.auth.CloudSyncAuthCard
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.PaleWarmSand
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.ManoLinkShareHelper
import com.example.ui.viewmodel.AskPharmacistState
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskPharmacistScreen(
  state: AskPharmacistState,
  onSelectCategory: (ConsultationCategory) -> Unit,
  onSelectAgeBand: (AgeBand) -> Unit,
  onQuestionChange: (String) -> Unit,
  onUrgentFlagChange: (Boolean) -> Unit,
  onConsentChange: (Boolean) -> Unit,
  onSubmit: () -> Unit,
  currentUser: FirebaseUser? = null,
  onCloudAuthSuccess: () -> Unit = {},
  onCloudAuthError: (String) -> Unit = {},
  onSignedOut: () -> Unit = {},
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoPoseAvatar(pose = MascotPose.LISTEN, size = 60.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("پرسش از داروساز", style = MaterialTheme.typography.headlineSmall)
                SourceBadge(reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW)
              }
              Text(
                text =
                  if (currentUser != null) {
                    "حساب گوگل شما متصل است؛ پرسش‌های غیرفوری در حساب ابری شما ذخیره می‌شوند."
                  } else {
                    "برای ذخیره و پیگیری ابری درخواست‌ها با حساب گوگل وارد شوید؛ بدون ورود نیز درخواست شما در این نشست ثبت می‌شود."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        CloudSyncAuthCard(
          currentUser = currentUser,
          titleFa = "ورود با گوگل برای پیگیری ابری پرسش‌ها",
          descriptionFa = "درخواست‌های مشاوره خود را به حساب گوگل متصل کنید تا در دستگاه‌های دیگر نیز در دسترس باشند.",
          onAuthSuccess = onCloudAuthSuccess,
          onAuthError = onCloudAuthError,
          onSignedOut = onSignedOut,
        )
      }

      item {
        SectionCard {
          Text("۱. موضوع پرسش", style = MaterialTheme.typography.titleSmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConsultationCategory.entries.forEach { cat ->
              SymptomChip(
                label = cat.labelFa,
                selected = state.category == cat,
                onClick = { onSelectCategory(cat) },
              )
            }
          }

          Text("۲. رده سنی کودک", style = MaterialTheme.typography.titleSmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgeBand.entries.forEach { band ->
              SymptomChip(
                label = band.labelFa,
                selected = state.ageBand == band,
                onClick = { onSelectAgeBand(band) },
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.hasUrgentFlags, onCheckedChange = onUrgentFlagChange)
            Text(
              text = "کودک دارای علائم فوری (تب زیر ۳ ماه، تنگی نفس، بی‌حالی یا تشنج) است",
              style = MaterialTheme.typography.bodySmall,
            )
          }

          OutlinedTextField(
            value = state.questionText,
            onValueChange = onQuestionChange,
            label = { Text("شرح پرسش غیرتشخیصی (لطفاً اطلاعات هویتی واقعی وارد نکنید)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("ask_pharmacist_question_input"),
          )

          ConsentBox(
            checked = state.consentAccepted,
            onCheckedChange = onConsentChange,
            title = "تأیید شرایط ثبت پرسش",
            description = "می‌دانم که این بخش جایگزین معاینه حضوری پزشک یا اورژانس نیست و در موارد فوری باید با ۱۱۵ تماس بگیرم.",
          )

          if (state.validationErrorFa != null) {
            SeverityAlert(
              severity = AlertSeverity.DANGER,
              title = "امکان ثبت درخواست نیست",
              body = state.validationErrorFa,
              showCautionMascot = state.hasUrgentFlags,
            )
          }

          Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("ask_pharmacist_submit"),
          ) {
            Text("ثبت پرسش")
          }
        }
      }

      if (state.requests.isNotEmpty()) {
        item {
          Text("درخواست‌های ثبت‌شده", style = MaterialTheme.typography.titleMedium)
        }
        items(state.requests, key = { it.id }) { req ->
          val stepIdx =
            when (req.status) {
              ConsultationStatus.PENDING -> 0
              ConsultationStatus.RECEIVED -> 1
              ConsultationStatus.REVIEWED -> 2
            }
          SectionCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("${req.category.labelFa} • ${req.childAgeBand.labelFa}", style = MaterialTheme.typography.titleSmall)
              SourceBadge(reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW)
            }
            Text(req.questionSummaryFa, style = MaterialTheme.typography.bodyMedium)
            ProgressTimeline(
              steps = listOf("در انتظار", "دریافت شد", "بررسی شد"),
              currentStepIndex = stepIdx,
            )
            if (req.followUp != null) {
              Text("${req.followUp.scheduleFa}: ${req.followUp.guidanceFa}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (req.status != ConsultationStatus.REVIEWED) {
              Text(
                "این وضعیت فقط پس از بررسی داروساز به‌روزرسانی می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyChildScreen(
  profile: ChildProfileDemo?,
  exportedJson: String?,
  onSaveProfile: (String, String, AgeBand, String, String, String, String, Boolean) -> Unit,
  onExportSummary: () -> Unit,
  onDeleteProfile: () -> Unit,
  onDeleteAllLocalChildData: () -> Unit,
  currentUser: FirebaseUser? = null,
  onCloudAuthSuccess: () -> Unit = {},
  onCloudAuthError: (String) -> Unit = {},
  onSignedOut: () -> Unit = {},
  onOpenNutritionAdvisor: () -> Unit = {},
  onOpenVaccinesSchedule: () -> Unit = {},
) {
  var alias by remember(profile) { mutableStateOf(profile?.aliasFa ?: "") }
  var birthOrAge by remember(profile) { mutableStateOf(profile?.birthDateOrAgeFa ?: "") }
  var ageBand by remember(profile) { mutableStateOf(profile?.ageBand ?: AgeBand.TODDLER_1_3Y) }
  var weightKg by remember(profile) { mutableStateOf(profile?.weightKgText ?: "") }
  var weightDate by remember(profile) { mutableStateOf(profile?.weightDateFa ?: "") }
  var allergies by remember(profile) { mutableStateOf(profile?.allergiesFa ?: "") }
  var currentMeds by remember(profile) { mutableStateOf(profile?.currentMedsFa ?: "") }
  var consent by remember(profile) { mutableStateOf(profile?.consentGranted ?: false) }
  var showDeleteAllLocalConfirm by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("child_profile_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoPoseAvatar(pose = MascotPose.WELCOME, size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("پروفایل فرزند من", style = MaterialTheme.typography.headlineSmall)
                SourceBadge(reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW)
              }
              Text(
                text =
                  if (currentUser != null) {
                    "حساب گوگل متصل است. داده‌های همگام‌شده در فضای ابری باقی می‌مانند و با حذف محلی پاک نمی‌شوند."
                  } else {
                    "اطلاعات در حافظه داخلی برنامه روی این دستگاه ذخیره می‌شوند؛ در صورت ورود و فعال‌شدن همگام‌سازی ممکن است به فضای ابری منتقل شوند."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        CloudSyncAuthCard(
          currentUser = currentUser,
          titleFa = "ورود با گوگل برای همگام‌سازی پروفایل کودک",
          descriptionFa = "ورود به حساب گوگل امکان همگام‌سازی پروفایل کودک را فراهم می‌کند. داده‌های ابری جدا از داده‌های محلی نگهداری می‌شوند.",
          onAuthSuccess = onCloudAuthSuccess,
          onAuthError = onCloudAuthError,
          onSignedOut = onSignedOut,
        )
      }

      item {
        SectionCard {
          OutlinedTextField(
            value = alias,
            onValueChange = { alias = it },
            label = { Text("نام مستعار کودک (مثلاً کودک ۱)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("child_alias_input"),
          )
          Text("گروه سنی:", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AgeBand.entries.forEach { band ->
              SymptomChip(label = band.labelFa, selected = ageBand == band, onClick = { ageBand = band })
            }
          }
          OutlinedTextField(
            value = birthOrAge,
            onValueChange = { birthOrAge = it },
            label = { Text("سن دقیق یا تاریخ تولد تقریبی (مثلاً ۳ سال و ۲ ماه)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = weightKg,
              onValueChange = { weightKg = it },
              label = { Text("وزن (kg)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = weightDate,
              onValueChange = { weightDate = it },
              label = { Text("تاریخ ثبت وزن") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(
            value = allergies,
            onValueChange = { allergies = it },
            label = { Text("حساسیت‌های شناخته‌شده (اختیاری)") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = currentMeds,
            onValueChange = { currentMeds = it },
            label = { Text("داروهای فعلی (اختیاری)") },
            modifier = Modifier.fillMaxWidth(),
          )

          ConsentBox(
            checked = consent,
            onCheckedChange = { consent = it },
            title = "رضایت آگاهانه استفاده از پروفایل کودک",
            description = "این اطلاعات برای نمایش و ثبت سابقه کودک نگهداری می‌شوند. دوزدهی خودکار در این نسخه غیرفعال است. در صورت ورود به حساب، ممکن است داده‌ها همگام‌سازی شوند.",
          )

          Button(
            onClick = { onSaveProfile(alias, birthOrAge, ageBand, weightKg, weightDate, allergies, currentMeds, consent) },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("save_child_profile_btn"),
          ) {
            Text("ذخیره پروفایل کودک")
          }
        }
      }

      if (profile != null) {
        item {
          SectionCard {
            Text("خلاصه پروفایل فعال: ${profile.aliasFa}", style = MaterialTheme.typography.titleMedium)
            Text("سن: ${profile.birthDateOrAgeFa} • وزن: ${profile.weightKgText.ifEmpty { "ثبت نشده" }}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = onExportSummary,
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("export_child_btn"),
              ) {
                Text("خروجی خلاصه")
              }
              Button(
                onClick = onDeleteProfile,
                colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = PureWhite),
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("delete_child_btn"),
              ) {
                Text("حذف فقط پروفایل")
              }
            }
            OutlinedButton(
              onClick = onOpenNutritionAdvisor,
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("child_profile_nutrition_btn"),
            ) {
              Text("پیشنهاد غذای سالم و مقوی بر اساس این پروفایل (هوشمند)")
            }
            OutlinedButton(
              onClick = onOpenVaccinesSchedule,
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("child_profile_vaccines_btn"),
            ) {
              Text("مدیریت برنامه واکسیناسیون و یادآور کودک")
            }
            if (exportedJson != null) {
              Surface(
                color = manoTintedSurface(PaleWarmSand, DarkGold, lightAlpha = 1f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                LtrText(text = exportedJson, modifier = Modifier.padding(12.dp))
              }
            }
          }
        }
      }

      item {
        SectionCard {
          Text("مدیریت حریم خصوصی کودک", style = MaterialTheme.typography.titleMedium)
          Text(
            "این گزینه سوابق آفلاین ثبت‌شده در مانو را پاک می‌کند؛ از جمله تب، دارو، واکسن، اسکن شربت، تغذیه و پیش‌نویس‌ها. داده‌های ابری و کش احتمالی Firebase، فایل‌های صادرشده و نسخه‌های پشتیبان با این روش حذف نمی‌شوند. برای پاک‌کردن تمام داده‌های خود برنامه می‌توان از تنظیمات ذخیره‌سازی Android استفاده کرد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (currentUser != null) {
            Text(
              "ابتدا از حساب ابری خارج شوید؛ در غیر این صورت داده‌ها ممکن است دوباره همگام‌سازی شوند. برای پاک‌کردن داده‌های ابری، باید درخواست جداگانه داشته باشید.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
          OutlinedButton(
            onClick = { showDeleteAllLocalConfirm = true },
            enabled = currentUser == null,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("delete_all_local_child_data_btn"),
          ) {
            Text("پاک‌سازی سوابق آفلاین مانو")
          }
        }
      }
    }

    if (showDeleteAllLocalConfirm) {
      AlertDialog(
        onDismissRequest = { showDeleteAllLocalConfirm = false },
        title = { Text("تأیید حذف اطلاعات محلی کودک") },
        text = {
          Text("سوابق سلامت ثبت‌شده در حافظه آفلاین مانو پاک می‌شوند و یادآورهای محلی متوقف خواهند شد. بازگرداندن این سوابق از داخل برنامه امکان‌پذیر نیست. داده‌های ابری و کش احتمالی آن، پشتیبان‌ها و فایل‌های صادرشده پاک نمی‌شوند.")
        },
        confirmButton = {
          TextButton(
            onClick = {
              showDeleteAllLocalConfirm = false
              onDeleteAllLocalChildData()
            },
            modifier = Modifier.testTag("delete_all_local_confirm_btn"),
          ) { Text("بله، داده‌های محلی حذف شوند") }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteAllLocalConfirm = false }) { Text("انصراف") }
        },
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoScreen(
  initialSection: String,
  sources: List<SourceReference>,
  onTestDeepLink: (String) -> Unit = {},
  onShowFeedback: (String) -> Unit = {},
) {
  var activeTab by remember(initialSection) { mutableStateOf(initialSection) }
  val context = LocalContext.current
  val fingerprints = remember(context) { ManoLinkShareHelper.readAppSigningSha256Fingerprints(context) }
  val assetLinksJson =
    remember(context.packageName, fingerprints) {
      ManoDomainLinks.buildAssetLinksJson(
        packageName = context.packageName,
        sha256Fingerprints = fingerprints,
        includeReleasePlaceholder = true,
      )
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("info_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text("درباره مانو، منابع علمی و اتصال دامنه", style = MaterialTheme.typography.headlineMedium)
      }

      item {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          listOf(
            "about" to "ماموریت و تحریریه",
            "domain" to "دامنه و لینک‌ها",
            "sources" to "منابع علمی",
            "privacy" to "حریم خصوصی و امنیت",
          ).forEach { (key, label) ->
            SymptomChip(
              label = label,
              selected = activeTab == key,
              onClick = { activeTab = key },
              modifier = Modifier.testTag("info_tab_$key"),
            )
          }
        }
      }

      when (activeTab) {
        "domain" -> {
          item {
            SectionCard(modifier = Modifier.testTag("info_domain_section")) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("دامنه‌های متصل به اپلیکیشن (App Links)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              }
              Text(
                text = "لینک‌های /topics، /medicines، /vaccines و سایر بخش‌های مانو روی هر دو دامنه manokid.ir و manokid.com مستقیم در اپلیکیشن باز می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
              )

              ManoDomainLinks.PRIMARY_DOMAINS.forEach { domain ->
                val url = ManoDomainLinks.homeUrl(domain)
                Surface(
                  color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.55f, darkAlpha = 0.16f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, manoBorderColor()),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      LtrText(text = url, color = MaterialTheme.colorScheme.primary)
                      Text("پشتیبانی از زیردامنه www.$domain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      OutlinedButton(
                        onClick = {
                          ManoLinkShareHelper.copyToClipboard(context, domain, url)
                          onShowFeedback("نشانی $domain کپی شد.")
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                      ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی آدرس", modifier = Modifier.size(16.dp))
                      }
                      OutlinedButton(
                        onClick = {
                          ManoLinkShareHelper.openExternalBrowserUrl(context, url)
                            .onFailure {
                              ManoLinkShareHelper.copyToClipboard(context, domain, url)
                              onShowFeedback("مرورگر در دسترس نیست؛ نشانی کپی شد.")
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                      ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "باز کردن در مرورگر", modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                }
              }
            }
          }

          item {
            SectionCard {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("مسیرهای مستقیم پشتیبانی‌شده و تست فوری", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              }
              val sampleRoutes =
                listOf(
                  "برنامه واکسیناسیون" to ManoDomainLinks.vaccinesUrl(),
                  "راهنمای تب نوزاد" to ManoDomainLinks.topicUrl("infant-fever-under-3-months"),
                  "کارت قطره استامینوفن" to ManoDomainLinks.medicineUrl("acetaminophen-pediatric"),
                  "خط زمانی تب و دارو" to ManoDomainLinks.medicinesUrl("timeline"),
                  "چک‌لیست ایمنی دارو" to ManoDomainLinks.medicinesUrl("checklist"),
                  "مشاور تغذیه هوشمند" to ManoDomainLinks.nutritionUrl(),
                )
              sampleRoutes.forEach { (labelFa, sampleUrl) ->
                HorizontalDivider(color = manoBorderColor())
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(labelFa, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    LtrText(text = sampleUrl, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  TextButton(
                    onClick = { onTestDeepLink(sampleUrl) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Text("تست مسیر")
                  }
                }
              }
            }
          }

          item {
            SectionCard(modifier = Modifier.testTag("info_assetlinks_card")) {
              val sageAccent = manoSemanticAccent(SageCare, DarkSage)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = sageAccent)
                Text("تأیید دامنه در هاست (assetlinks.json)", style = MaterialTheme.typography.titleMedium, color = sageAccent)
              }
              Text(
                text = "برای باز شدن خودکار لینک‌ها بدون پرسش مرورگر، فایل زیر را بدون ریدایرکت (HTTPS + application/json) در مسیر /.well-known/assetlinks.json روی هر دو دامنه قرار دهید و اثر انگشت SHA-256 کلید انتشار (Play App Signing) را جایگزین کنید:",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(
                text = "شناسه بسته اپلیکیشن: ${context.packageName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              Button(
                onClick = {
                  ManoLinkShareHelper.copyToClipboard(context, "assetlinks.json", assetLinksJson)
                  onShowFeedback("فایل assetlinks.json در حافظه کپی شد.")
                },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("copy_assetlinks_btn"),
              ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("کپی کامل assetlinks.json")
              }
              Surface(
                color = manoCardSurface(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, manoBorderColor()),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Box(modifier = Modifier.padding(12.dp)) {
                  LtrText(text = assetLinksJson)
                }
              }
            }
          }
        }

        "sources" -> {
          item {
            SectionCard {
              Text("مراجع پایه برای راستی‌آزمایی بالینی", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text(
                text = "لینک‌های زیر نقطه شروع بازبینی تحریریه هستند؛ متون راهنما تا امضای داروساز/متخصص اطفال با برچسب «نیازمند تأیید علمی» منتشر می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
              )
              sources.forEach { src ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text("${src.publisher} — ${src.title}", style = MaterialTheme.typography.titleSmall)
                Text(src.publishedOrUpdated, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LtrText(text = src.url)
              }
            }
          }
          item {
            SectionCard {
              Text("چک‌لیست بازبینی علمی محتوا", style = MaterialTheme.typography.titleMedium)
              listOf(
                "تناسب منبع با پرسش و سن کودک",
                "شفافیت قدرت شواهد و منع مصرف‌ها",
                "وجود علائم هشدار (Red Flags) پیش از توصیه مراقبت خانگی",
                "وضوح فارسی بدون جمله‌بندی گمراه‌کننده",
                "تعیین تاریخ بازبینی بعدی و بومی‌سازی برای داروهای ایران",
                "جایگاه امضای مسئول علمی: [تأیید نهایی ثبت نشده — در انتظار امضای واقعی مسئول علمی]",
              ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
          }
        }

        "privacy" -> {
          item {
            SectionCard {
              Text("اصول حریم خصوصی و معماری امنیت تولیدی", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text("• هیچ ردیاب تبلیغاتی یا تحلیل‌گر کودک در اپلیکیشن فعال نیست.", style = MaterialTheme.typography.bodyMedium)
              Text("• فقط نشان‌گذاری مقالات و فیلتر سنی غیرحساس در حافظه محلی ذخیره می‌شود.", style = MaterialTheme.typography.bodyMedium)
              Text("• در نسخه تولیدی، پرونده سلامت کودک و مشاوره‌ها نیازمند احراز هویت امن، RBAC، رمزنگاری در حال انتقال و ذخیره‌سازی، ثبت ممیزی و انطباق با قوانین سلامت/دارو در ایران است.", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        else -> {
          item {
            SectionCard {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ManoPoseAvatar(pose = MascotPose.EXPLAIN, size = 60.dp)
                Column(modifier = Modifier.weight(1f)) {
                  Text("هویت مانو کیدز (MANO Kids)", style = MaterialTheme.typography.titleMedium)
                  Text("همراه علمی، آرام و قابل‌اعتماد برای والدین فارسی‌زبان", style = MaterialTheme.typography.bodySmall)
                }
              }
              Text(
                text = "مانو جایگزین پزشک یا داروساز نیست و تشخیص صادر نمی‌کند. هدف مانو کمک به والدین برای درک نشانه‌های شایع، شناخت به‌موقع علائم خطر، استفاده ایمن‌تر از داروها با سرنگ mL و ارتباط شفاف با داروساز است.",
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                text = "ماژول فروشگاه آینده (فروشگاه) کاملاً مجزا از راهنمای بالینی طراحی می‌شود و دسترسی به هشدارها یا مقالات آموزشی هرگز وابسته به خرید نخواهد بود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                LtrText(text = "manokid.ir | manokid.com")
                TextButton(
                  onClick = { activeTab = "domain" },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Text("مدیریت اتصال دامنه")
                }
              }
            }
          }
        }
      }
    }
  }
}
