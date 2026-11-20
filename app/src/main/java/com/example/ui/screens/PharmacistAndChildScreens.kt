package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ManoHapticIntensity
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.data.ManoWebGlQuality
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.MascotPose
import com.example.domain.model.PublicPharmacyOption
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SourceReference
import com.example.domain.model.StaffAccessStatus
import com.example.domain.model.StaffConsultationCase
import com.example.domain.util.AssetLinksCheckSeverity
import com.example.domain.util.AssetLinksDomainReport
import com.example.domain.util.AssetLinksVerifier
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
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.PaleWarmSand
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.ManoHaptics
import com.example.ui.util.ManoLinkShareHelper
import com.example.ui.viewmodel.AskPharmacistState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

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
  onAdvanceStatus: (String, ConsultationStatus) -> Unit,
  currentUser: FirebaseUser? = null,
  acceptingPharmacies: List<PublicPharmacyOption> = emptyList(),
  selectedPharmacyId: String? = null,
  onSelectPharmacy: (String) -> Unit = {},
  isSubmitting: Boolean = false,
  serverErrorFa: String? = null,
  onCloudAuthSuccess: () -> Unit = {},
  onCloudAuthError: (String) -> Unit = {},
  onSignedOut: () -> Unit = {},
  staffAccess: StaffAccessStatus = StaffAccessStatus(),
  staffInboxCases: List<StaffConsultationCase> = emptyList(),
  isLoadingStaffInbox: Boolean = false,
  staffActionInFlightCaseId: String? = null,
  staffErrorFa: String? = null,
  onRefreshStaffInbox: (forceTokenRefresh: Boolean, statusFilter: ConsultationStatus?) -> Unit = { _, _ -> },
  onAcknowledgeStaffCaseReceived: (pharmacyId: String, caseId: String, internalNote: String?) -> Unit = { _, _, _ -> },
  onSubmitStaffCaseReview: (pharmacyId: String, caseId: String, internalNote: String?, parentReply: String, pharmacistAttestation: Boolean) -> Unit = { _, _, _, _, _ -> },
  initialMode: String = "parent",
  onShowFeedback: (String) -> Unit = {},
) {
  val normalizedInitial =
    remember(initialMode) {
      when (initialMode.lowercase()) {
        "staff", "pharmacy" -> "staff"
        "followup", "follow_up" -> "followup"
        else -> "parent"
      }
    }
  var activeViewMode by remember(normalizedInitial) { mutableStateOf(normalizedInitial) }
  val isStaffMode = activeViewMode == "staff"
  val isFollowUpFirst = activeViewMode == "followup"
  val context = LocalContext.current

  BackHandler(enabled = activeViewMode != "parent") {
    activeViewMode = "parent"
  }

  val renderIntakeSection: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
    item(key = "mano_connect_intake_card") {
      SectionCard(modifier = Modifier.testTag("mano_connect_intake_section")) {
        LtrText(text = "MANO Connect • Secure Family Intake", color = MaterialTheme.colorScheme.primary)
        Text("ثبت پرسش غیرفوری از داروساز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

        if (currentUser != null && acceptingPharmacies.isNotEmpty()) {
          Text("۳. داروخانه پذیرش‌گر تاییدشده", style = MaterialTheme.typography.titleSmall)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            acceptingPharmacies.forEach { pharm ->
              SymptomChip(
                label = pharm.displayNameFa,
                selected = selectedPharmacyId == pharm.pharmacyId,
                onClick = { onSelectPharmacy(pharm.pharmacyId) },
              )
            }
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

        val activeError = serverErrorFa ?: state.validationErrorFa
        if (activeError != null) {
          SeverityAlert(
            severity = AlertSeverity.DANGER,
            title = "امکان ثبت درخواست نیست",
            body = activeError,
            showCautionMascot = state.hasUrgentFlags || serverErrorFa != null,
          )
        }

        Button(
          onClick = onSubmit,
          enabled = !isSubmitting,
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("ask_pharmacist_submit"),
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("در حال بررسی تریاژ و ثبت...")
          } else {
            Text(if (currentUser != null) "ثبت امن در داروخانه" else "ثبت پرسش")
          }
        }
      }
    }
  }

  val renderFollowUpSection: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
    item(key = "mano_followup_summary_card") {
      val sageAccent = manoSemanticAccent(SageCare, DarkSage)
      val pendingCount = state.requests.count { it.status == ConsultationStatus.PENDING }
      val receivedCount = state.requests.count { it.status == ConsultationStatus.RECEIVED }
      val reviewedCount = state.requests.count { it.status == ConsultationStatus.REVIEWED }

      SectionCard(modifier = Modifier.testTag("mano_followup_section")) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LtrText(text = "MANO Follow-up • Consultation Tracker", color = sageAccent)
            Text(
              text = "پیگیری نتیجه مشاوره (MANO Follow-up)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          Surface(
            color = sageAccent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "${state.requests.size} پرونده",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = sageAccent,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
        Text(
          text = "چرخه پیگیری: ۱. ثبت و غربالگری ایمنی ← ۲. دریافت در پنل اختصاصی داروخانه ← ۳. ثبت پاسخ علمی و برنامه پیگیری داروساز.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.requests.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color = manoCardSurface(),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, manoBorderColor()),
            ) {
              Text(
                text = "در انتظار: $pendingCount",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
            Surface(
              color = manoCardSurface(),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, manoBorderColor()),
            ) {
              Text(
                text = "دریافت‌شده: $receivedCount",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
            Surface(
              color = sageAccent.copy(alpha = 0.12f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.35f)),
            ) {
              Text(
                text = "بررسی‌شده: $reviewedCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = sageAccent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }
      }
    }

    if (state.requests.isNotEmpty()) {
      items(state.requests, key = { it.id }) { req ->
        val stepIdx =
          when (req.status) {
            ConsultationStatus.PENDING -> 0
            ConsultationStatus.RECEIVED -> 1
            ConsultationStatus.REVIEWED -> 2
          }
        val sageAccent = manoSemanticAccent(SageCare, DarkSage)
        var step1Checked by remember(req.id) { mutableStateOf(false) }
        var step2Checked by remember(req.id) { mutableStateOf(false) }
        var step3Checked by remember(req.id) { mutableStateOf(false) }

        SectionCard(modifier = Modifier.testTag("followup_request_card_${req.id}")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("${req.category.labelFa} • ${req.childAgeBand.labelFa}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            SourceBadge(
              reviewStatus = if (req.status == ConsultationStatus.REVIEWED) ReviewStatus.APPROVED else ReviewStatus.NEEDS_CLINICAL_REVIEW,
            )
          }
          Text(req.questionSummaryFa, style = MaterialTheme.typography.bodyMedium)
          ProgressTimeline(
            steps = listOf("در انتظار", "دریافت شد", "بررسی شد"),
            currentStepIndex = stepIdx,
          )
          if (req.followUp != null) {
            Surface(
              color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.65f, darkAlpha = 0.16f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "نتیجه مشاوره و برنامه پیگیری (${req.followUp.scheduleFa}):",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = req.followUp.guidanceFa,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "مسئول علمی: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_IDENTITY_FA}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = manoBorderColor())
                Text(
                  text = "چک‌لیست اقدام والدین پس از مشاوره:",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = sageAccent,
                )
                listOf(
                  Triple(step1Checked, { v: Boolean -> step1Checked = v }, "غلظت دارو (mg/mL) و حجم سرنگ مدرج را با پاسخ داروساز تطبیق دادم"),
                  Triple(step2Checked, { v: Boolean -> step2Checked = v }, "زمان نوبت بعدی و پایش دمای بدن کودک را در لاگ‌بوک ثبت کردم"),
                  Triple(step3Checked, { v: Boolean -> step3Checked = v }, "علائم هشدار (Red Flags) برای مراجعه حضوری به پزشک را مرور کردم"),
                ).forEach { (checked, onCheck, labelFa) ->
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = checked, onCheckedChange = onCheck)
                    Text(text = labelFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                  }
                }
                OutlinedButton(
                  onClick = {
                    val summaryText =
                      buildString {
                        appendLine("پرونده پیگیری مشاوره دارویی مانو (MANO Follow-up)")
                        appendLine("موضوع: ${req.category.labelFa} (${req.childAgeBand.labelFa})")
                        appendLine("پرسش: ${req.questionSummaryFa}")
                        appendLine("برنامه پیگیری (${req.followUp.scheduleFa}): ${req.followUp.guidanceFa}")
                      }
                    ManoLinkShareHelper.copyToClipboard(context, "MANO Follow-up", summaryText)
                    onShowFeedback("خلاصه مشاوره و برنامه پیگیری در حافظه کپی شد.")
                  },
                  modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("followup_copy_btn_${req.id}"),
                ) {
                  Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("کپی خلاصه مشاوره و برنامه پیگیری")
                }
              }
            }
          }
          if (currentUser != null) {
            if (req.status != ConsultationStatus.REVIEWED) {
              OutlinedButton(
                onClick = { onAdvanceStatus(req.id, req.status) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("ask_pharmacist_refresh_${req.id}"),
              ) {
                Text("استعلام آخرین وضعیت از سرور")
              }
            }
          } else if (req.status != ConsultationStatus.REVIEWED) {
            OutlinedButton(
              onClick = { onAdvanceStatus(req.id, req.status) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text("پیش‌نمایش مرحله بعدی (حالت آفلاین)")
            }
          }
        }
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("ask_pharmacist_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SectionCard(modifier = Modifier.testTag("mano_connect_header_card")) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoPoseAvatar(pose = MascotPose.LISTEN, size = 60.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(50),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.VerifiedUser,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(14.dp),
                    )
                    LtrText(
                      text =
                        when (activeViewMode) {
                          "staff" -> "MANO Pharmacy"
                          "followup" -> "MANO Follow-up"
                          else -> "MANO Connect & Follow-up"
                        },
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                SourceBadge(reviewStatus = ReviewStatus.APPROVED)
              }
              Text("ارتباط امن خانواده و داروساز", style = MaterialTheme.typography.headlineSmall)
              Text(
                text =
                  if (currentUser != null) {
                    "حساب گوگل شما متصل است؛ پرسش‌های غیرفوری در کانال امن ابری (MANO Connect) ثبت و پیگیری می‌شوند."
                  } else {
                    "برای ذخیره و پیگیری ابری درخواست‌ها با حساب گوگل وارد شوید؛ بدون ورود نیز درخواست شما در این نشست ثبت می‌شود."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            SymptomChip(
              label = "بخش والدین (MANO Connect)",
              selected = activeViewMode == "parent",
              onClick = { activeViewMode = "parent" },
              modifier = Modifier.testTag("ask_mode_parent"),
            )
            SymptomChip(
              label = "پیگیری نتیجه مشاوره (MANO Follow-up)",
              selected = activeViewMode == "followup",
              onClick = { activeViewMode = "followup" },
              modifier = Modifier.testTag("ask_mode_followup"),
            )
            SymptomChip(
              label = "پنل اختصاصی داروخانه (MANO Pharmacy)",
              selected = activeViewMode == "staff",
              onClick = { activeViewMode = "staff" },
              modifier = Modifier.testTag("ask_mode_staff"),
            )
          }
        }
      }

      item {
        CloudSyncAuthCard(
          currentUser = currentUser,
          titleFa = if (isStaffMode) "ورود حساب داروساز تاییدشده" else "ورود با گوگل برای پیگیری ابری پرسش‌ها",
          descriptionFa =
            if (isStaffMode) "حساب داروساز با گواهی سروری تاییدشده و فهرست رسمی داروسازان در سرور بررسی می‌شود."
            else "درخواست‌های مشاوره خود را به حساب گوگل متصل کنید تا در دستگاه‌های دیگر نیز در دسترس باشند.",
          onAuthSuccess = onCloudAuthSuccess,
          onAuthError = onCloudAuthError,
          onSignedOut = onSignedOut,
        )
      }

      if (isStaffMode) {
        item {
          PharmacistStaffPanelContent(
            currentUser = currentUser,
            staffAccess = staffAccess,
            inboxCases = staffInboxCases,
            isLoadingInbox = isLoadingStaffInbox,
            inFlightCaseId = staffActionInFlightCaseId,
            staffErrorFa = staffErrorFa,
            onRefreshStaffInbox = onRefreshStaffInbox,
            onAcknowledgeReceived = onAcknowledgeStaffCaseReceived,
            onSubmitReview = onSubmitStaffCaseReview,
          )
        }
      } else if (isFollowUpFirst) {
        renderFollowUpSection()
        renderIntakeSection()
      } else {
        renderIntakeSection()
        renderFollowUpSection()
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

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
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
                SourceBadge(reviewStatus = ReviewStatus.APPROVED)
              }
              Text(
                text =
                  if (currentUser != null) {
                    "همگام‌سازی امن با حساب گوگل فعال است؛ اطلاعات فقط پس از رضایت والد در مسیر خصوصی کاربر ذخیره می‌شود."
                  } else {
                    "اطلاعات به‌صورت دائمی روی دستگاه ذخیره می‌شود و پس از ورود با حساب گوگل به فضای ابری منتقل می‌گردد."
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
          descriptionFa = "با اتصال حساب گوگل، پروفایل کودک به‌صورت اختصاصی و ایمن در فضای ابری ذخیره می‌شود.",
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
            description = "این داده‌ها فقط روی دستگاه و (در صورت ورود) در حساب خصوصی گوگل برای محاسبه دوز و فیلتر سنی نگهداری می‌شوند و به ابزار ردیاب تبلیغاتی ارسال نمی‌گردند.",
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
                Text("حذف کامل")
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
  hapticIntensity: ManoHapticIntensity = ManoHapticIntensity.MEDIUM,
  webGlQuality: ManoWebGlQuality = ManoWebGlQuality.AUTO,
  onSelectHapticIntensity: (ManoHapticIntensity) -> Unit = {},
  onSelectWebGlQuality: (ManoWebGlQuality) -> Unit = {},
  onLaunchOnboarding: () -> Unit = {},
) {
  var activeTab by remember(initialSection) { mutableStateOf(initialSection) }
  val context = LocalContext.current
  val hostView = LocalView.current
  val scope = rememberCoroutineScope()
  val fingerprints = remember(context) { ManoLinkShareHelper.readAppSigningSha256Fingerprints(context) }
  val assetLinksJson =
    remember(context.packageName, fingerprints) {
      ManoDomainLinks.buildAssetLinksJson(
        packageName = context.packageName,
        sha256Fingerprints = fingerprints,
        includeReleasePlaceholder = true,
      )
    }
  var isVerifyingDomains by remember { mutableStateOf(false) }
  var domainReports by remember { mutableStateOf<List<AssetLinksDomainReport>>(emptyList()) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("info_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text("درباره مانو، تنظیمات دستگاه و اتصال دامنه", style = MaterialTheme.typography.headlineMedium)
      }

      item {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          listOf(
            "about" to "ماموریت و تحریریه",
            "release" to "تنظیمات و انتشار",
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
        "release" -> {
          item {
            SectionCard(modifier = Modifier.testTag("info_haptic_tuning_card")) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("تنظیم شدت بازخورد لرزشی (Haptic)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              }
              Text(
                text = "شدت لرزش هنگام لمس شخصیت مانو در صفحه خانه و آزمایشگاه سه‌بعدی را متناسب با موتور لرزش گوشی فیزیکی خود تنظیم کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                ManoHapticIntensity.entries.forEach { level ->
                  SymptomChip(
                    label = level.labelFa,
                    selected = hapticIntensity == level,
                    onClick = {
                      onSelectHapticIntensity(level)
                      ManoHaptics.perform(context, hostView, level, isPrimaryInteraction = true)
                    },
                    modifier = Modifier.testTag("haptic_level_${level.id}"),
                  )
                }
              }
              Text(
                text = "حالت فعال: ${hapticIntensity.labelFa} — ${hapticIntensity.summaryFa}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              OutlinedButton(
                onClick = {
                  ManoHaptics.perform(context, hostView, hapticIntensity, isPrimaryInteraction = true)
                  onShowFeedback(
                    if (hapticIntensity == ManoHapticIntensity.OFF) "بازخورد لرزشی خاموش است."
                    else "لرزش با شدت «${hapticIntensity.labelFa}» اجرا شد."
                  )
                },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("test_haptic_btn"),
              ) {
                Icon(Icons.Rounded.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("تست لرزش روی دستگاه")
              }
            }
          }

          item {
            SectionCard(modifier = Modifier.testTag("info_webgl_tuning_card")) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("سرعت و کیفیت موتور سه‌بعدی (WebGL)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              }
              Text(
                text = "در گوشی‌های میان‌رده، انتخاب حالت «میان‌رده / روان» یا «خودکار» تراکم ذرات و وضوح رندر را برای نرخ فریم پایدار و مصرف باتری کمتر بهینه می‌کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                ManoWebGlQuality.entries.forEach { profile ->
                  SymptomChip(
                    label = profile.labelFa,
                    selected = webGlQuality == profile,
                    onClick = { onSelectWebGlQuality(profile) },
                    modifier = Modifier.testTag("webgl_quality_${profile.id}"),
                  )
                }
              }
              Text(
                text = "پروفایل فعال: ${webGlQuality.labelFa} — ${webGlQuality.summaryFa}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          item {
            SectionCard(modifier = Modifier.testTag("info_release_checklist_card")) {
              val sageAccent = manoSemanticAccent(SageCare, DarkSage)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = sageAccent)
                Text("آماده‌سازی انتشار و تجربه ورود اول", style = MaterialTheme.typography.titleMedium, color = sageAccent)
              }
              listOf(
                "مرور نهایی متن‌های فارسی راهنما، هشدارهای نوزاد زیر ۳ ماه و ارجاع اورژانس ۱۱۵",
                "بررسی زنده فایل /.well-known/assetlinks.json روی manokid.ir و manokid.com بدون ریدایرکت",
                "جایگزینی اثر انگشت SHA-256 کلید انتشار (Play App Signing) روی سرور دامنه",
                "تست شدت لرزش و روانی آزمایشگاه سه‌بعدی WebGL روی یک گوشی میان‌رده فیزیکی",
              ).forEach { checklistItem ->
                Text("• $checklistItem", style = MaterialTheme.typography.bodySmall)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = onLaunchOnboarding,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("info_reopen_onboarding_btn"),
                ) {
                  Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("شروع مجدد معرفی ۳ مرحله‌ای")
                }
                OutlinedButton(
                  onClick = { activeTab = "domain" },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Text("بررسی دامنه")
                }
              }
            }
          }
        }
        "domain" -> {
          item {
            SectionCard(modifier = Modifier.testTag("info_domain_section")) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.domain_links_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              }
              Text(
                text = stringResource(R.string.domain_links_subtitle),
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
              OutlinedButton(
                enabled = !isVerifyingDomains,
                onClick = {
                  isVerifyingDomains = true
                  scope.launch {
                    val reports =
                      AssetLinksVerifier.verifyAllPrimaryDomains(
                        expectedPackageName = context.packageName,
                        expectedFingerprints = fingerprints,
                      )
                    domainReports = reports
                    isVerifyingDomains = false
                    val verifiedCount = reports.count { it.severity == AssetLinksCheckSeverity.VERIFIED }
                    onShowFeedback(
                      if (verifiedCount == reports.size && reports.isNotEmpty()) {
                        "هر دو دامنه manokid.ir و manokid.com با موفقیت تایید شدند."
                      } else {
                        "نتیجه بررسی زنده سرور دریافت شد؛ جزئیات هر دامنه را ببینید."
                      }
                    )
                  }
                },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("verify_assetlinks_btn"),
              ) {
                if (isVerifyingDomains) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                  Spacer(Modifier.width(8.dp))
                  Text("در حال بررسی زنده سرورها...")
                } else {
                  Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("بررسی زنده assetlinks.json روی سرور واقعی")
                }
              }

              if (domainReports.isNotEmpty()) {
                domainReports.forEach { report ->
                  val (bgColor, accentColor, iconVec) =
                    when (report.severity) {
                      AssetLinksCheckSeverity.VERIFIED ->
                        Triple(
                          manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.85f, darkAlpha = 0.22f),
                          manoSemanticAccent(SuccessEmerald, DarkSage),
                          Icons.Rounded.CheckCircle,
                        )
                      AssetLinksCheckSeverity.WARNING ->
                        Triple(
                          manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.85f, darkAlpha = 0.22f),
                          manoSemanticAccent(CautionAmber, DarkGold),
                          Icons.Rounded.WarningAmber,
                        )
                      AssetLinksCheckSeverity.FAILED ->
                        Triple(
                          manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.85f, darkAlpha = 0.22f),
                          manoSemanticAccent(DangerCrimson, DarkCoral),
                          Icons.Rounded.ErrorOutline,
                        )
                    }
                  Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("assetlinks_report_${report.domain}"),
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Icon(iconVec, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Text(
                          text = "${report.domain} — ${report.headlineFa}",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                          color = accentColor,
                        )
                      }
                      Text(
                        text = report.detailsFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      LtrText(text = report.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
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
            SectionCard(modifier = Modifier.testTag("info_clinical_signoff_card")) {
              val sageAccent = manoSemanticAccent(SageCare, DarkSage)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = sageAccent)
                  Text("تأییدیه علمی مسئول دارویی", style = MaterialTheme.typography.titleMedium, color = sageAccent)
                }
                SourceBadge(reviewStatus = ReviewStatus.APPROVED)
              }
              Text(
                text = ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_IDENTITY_FA,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = ManoSourcesAndClinicalSeed.approvedReviewMeta.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          item {
            SectionCard {
              Text("مراجع پایه برای راستی‌آزمایی بالینی", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text(
                text = "راهنماهای علمی و دارویی مانو بر اساس مراجع بین‌المللی زیر تدوین شده و به تأیید مسئول علمی رسیده است.",
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
                "امضای علمی تأییدشده: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_IDENTITY_FA}",
              ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
          }
        }

        "privacy" -> {
          item {
            SectionCard {
              Text("حریم خصوصی و معماری امنیت فعال", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text("• هیچ ردیاب تبلیغاتی، SDK تبلیغ یا پروفایل‌سازی تجاری برای کودک در اپلیکیشن وجود ندارد.", style = MaterialTheme.typography.bodyMedium)
              Text("• همگام‌سازی ابری پرونده کودک، تب، دوز دارو، واکسن‌ها و مشاوره‌ها با ورود گوگل (Credential Manager) و قواعد ایزوله به ازای هر کاربر محافظت می‌شود.", style = MaterialTheme.typography.bodyMedium)
              Text("• تمام ارتباطات ابری با رمزنگاری HTTPS/TLS، گواهی App Check و کنترل دسترسی سروری داروسازان (RBAC) انجام می‌شود و هر زمان می‌توانید پرونده محلی یا ابری را حذف کنید.", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        else -> {
          item {
            SectionCard {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ManoPoseAvatar(pose = MascotPose.EXPLAIN, size = 60.dp)
                Column(modifier = Modifier.weight(1f)) {
                  Text("هویت مانو کید (MANO Kid)", style = MaterialTheme.typography.titleMedium)
                  Text("همراه علمی، آرام و قابل‌اعتماد برای مادران و کودکان فارسی‌زبان", style = MaterialTheme.typography.bodySmall)
                }
              }
              Text(
                text = "مانو کید جایگزین پزشک یا داروساز نیست و تشخیص صادر نمی‌کند. هدف مانو کید کمک به مادران و والدین برای درک نشانه‌های شایع، شناخت به‌موقع علائم خطر، استفاده ایمن‌تر از داروها با سرنگ mL و ارتباط شفاف با داروساز است.",
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                text = "تمام توضیحات روش ارزیابی علمی، منابع مرجع و دیدگاه خانواده‌ها برای حفظ آرامش صفحه اول در این بخش قرار داده شده است. محتوای علمی مانو کید بدون تبلیغات است و دسترسی به راهنماهای ایمنی هرگز وابسته به پرداخت نیست.",
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
          item {
            SectionCard {
              Text("اصول طراحی مراقبت‌محور برای آرامش مادر", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text("• نمایش خطرات فوری پیش از هر توصیه مراقبت خانگی", style = MaterialTheme.typography.bodySmall)
              Text("• دسترسی تک‌ضرب به مشاوره داروساز تاییدشده و پیگیری پاسخ", style = MaterialTheme.typography.bodySmall)
              Text("• تفکیک غلظت قطره و شربت کودک با خط‌کش دقیق سرنگ mL", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }
}
