package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ConsultationPayloadMapper
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.ReviewStatus
import com.example.domain.model.StaffAccessStatus
import com.example.domain.model.StaffConsultationCase
import com.example.ui.components.AlertSeverity
import com.example.ui.components.LtrText
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.components.SymptomChip
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkSage
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PlumCare
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PharmacistStaffPanelContent(
  currentUser: FirebaseUser?,
  staffAccess: StaffAccessStatus,
  inboxCases: List<StaffConsultationCase>,
  isLoadingInbox: Boolean,
  inFlightCaseId: String?,
  staffErrorFa: String?,
  onRefreshStaffInbox: (forceTokenRefresh: Boolean, statusFilter: ConsultationStatus?) -> Unit,
  onAcknowledgeReceived: (pharmacyId: String, caseId: String, internalNote: String?) -> Unit,
  onSubmitReview: (pharmacyId: String, caseId: String, internalNote: String?, parentReply: String, pharmacistAttestation: Boolean) -> Unit,
) {
  var statusFilter by rememberSaveable { mutableStateOf<ConsultationStatus?>(null) }
  val isDark = LocalManoDarkTheme.current
  val connectAccent = if (isDark) DarkPlum else PlumCare
  val sageAccent = manoSemanticAccent(SuccessEmerald, DarkSage)

  Column(
    modifier = Modifier.fillMaxWidth().testTag("pharmacist_staff_panel"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Verification Status & Authority Banner
    SectionCard(modifier = Modifier.testTag("staff_verification_card")) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = if (staffAccess.isVerifiedPharmacist) sageAccent else connectAccent,
          )
          Text(
            text =
              if (staffAccess.isVerifiedPharmacist) "پنل اختصاصی داروساز تاییدشده"
              else "احراز صلاحیت سروری داروساز",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        SourceBadge(
          reviewStatus = if (staffAccess.isVerifiedPharmacist) ReviewStatus.APPROVED else ReviewStatus.NEEDS_CLINICAL_REVIEW,
        )
      }

      if (currentUser == null) {
        SeverityAlert(
          severity = AlertSeverity.INFO,
          title = "ابتدا با حساب گوگل سازمانی داروساز وارد شوید",
          body =
            "دسترسی به صندوق پرونده‌های داروخانه و ثبت پاسخ علمی تنها با احراز هویت گوگل و گواهی سروری تاییدشده داروساز امکان‌پذیر است.",
        )
      } else if (!staffAccess.isVerifiedPharmacist) {
        SeverityAlert(
          severity = AlertSeverity.CAUTION,
          title = "گواهی سروری داروساز روی این حساب فعال نیست",
          body =
            "حساب شما (${currentUser.email ?: currentUser.uid}) هنوز نقش داروساز تاییدشده در سرور را دریافت نکرده است. پس از فعال‌سازی صلاحیت داروساز توسط مدیر سامانه، دکمه بازخوانی مجوز را بزنید.",
        )
        Text(
          text = "مرجع تاییدیه علمی محتوا: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_IDENTITY_FA}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
          onClick = { onRefreshStaffInbox(true, statusFilter) },
          enabled = !isLoadingInbox,
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("refresh_staff_claims_btn"),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text("بازخوانی مجوز و توکن داروساز از سرور")
        }
      } else {
        Surface(
          color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.72f, darkAlpha = 0.16f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "دسترسی داروساز تایید شد • ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_IDENTITY_FA}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = sageAccent,
            )
            LtrText(text = "Pharmacy ID: ${staffAccess.pharmacyId} | UID: ${currentUser.uid.take(12)}...")
          }
        }

        // Status filter chips & refresh
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SymptomChip(
            label = "همه پرونده‌ها",
            selected = statusFilter == null,
            onClick = {
              statusFilter = null
              onRefreshStaffInbox(false, null)
            },
            modifier = Modifier.testTag("staff_filter_all"),
          )
          ConsultationStatus.entries.forEach { st ->
            SymptomChip(
              label = st.labelFa,
              selected = statusFilter == st,
              onClick = {
                statusFilter = st
                onRefreshStaffInbox(false, st)
              },
              modifier = Modifier.testTag("staff_filter_${st.name}"),
            )
          }
        }

        OutlinedButton(
          onClick = { onRefreshStaffInbox(true, statusFilter) },
          enabled = !isLoadingInbox,
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("staff_refresh_inbox_btn"),
        ) {
          if (isLoadingInbox) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("در حال دریافت صندوق داروخانه...")
          } else {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("به‌روزرسانی صندوق پرونده‌ها")
          }
        }
      }

      if (staffErrorFa != null) {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "خطا در عملیات داروساز",
          body = staffErrorFa,
        )
      }
    }

    if (staffAccess.isVerifiedPharmacist) {
      if (inboxCases.isEmpty() && !isLoadingInbox) {
        SectionCard {
          Text("پرونده‌ای در این فیلتر یافت نشد", style = MaterialTheme.typography.titleSmall)
          Text(
            text = "پرسش‌های غیرفوری والدین پس از عبور از گذرنامه تریاژ سرور در این صندوق نمایش داده می‌شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        inboxCases.forEach { item ->
          StaffInboxCaseCard(
            item = item,
            isBusy = inFlightCaseId == item.caseId,
            connectAccent = connectAccent,
            sageAccent = sageAccent,
            isDark = isDark,
            onAcknowledgeReceived = { note -> onAcknowledgeReceived(item.pharmacyId, item.caseId, note) },
            onSubmitReview = { note, reply, attestation ->
              onSubmitReview(item.pharmacyId, item.caseId, note, reply, attestation)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun StaffInboxCaseCard(
  item: StaffConsultationCase,
  isBusy: Boolean,
  connectAccent: androidx.compose.ui.graphics.Color,
  sageAccent: androidx.compose.ui.graphics.Color,
  isDark: Boolean,
  onAcknowledgeReceived: (internalNote: String?) -> Unit,
  onSubmitReview: (internalNote: String?, parentReply: String, pharmacistAttestation: Boolean) -> Unit,
) {
  var internalNoteInput by remember(item.caseId, item.status) { mutableStateOf("") }
  var parentReplyInput by remember(item.caseId, item.status) { mutableStateOf("") }
  var attested by remember(item.caseId, item.status) { mutableStateOf(false) }

  SectionCard(modifier = Modifier.testTag("staff_case_card_${item.caseId}")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "${item.category.labelFa} • ${item.childAgeBand.labelFa}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "${item.createdAtLabelFa} • کد جریان: ${item.symptomFlowId}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Surface(
        color =
          if (item.status == ConsultationStatus.REVIEWED) sageAccent.copy(alpha = 0.14f)
          else connectAccent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, (if (item.status == ConsultationStatus.REVIEWED) sageAccent else connectAccent).copy(alpha = 0.35f)),
      ) {
        Text(
          text = item.status.labelFa,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (item.status == ConsultationStatus.REVIEWED) sageAccent else connectAccent,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    Text(
      text = item.questionSummaryFa,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )

    if (item.internalNotes.isNotEmpty()) {
      HorizontalDivider(color = manoBorderColor())
      Text("یادداشت‌های داخلی کادر درمان:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      item.internalNotes.forEach { n ->
        Text(
          text = "• [${n.fromStatus.labelFa} ← ${n.toStatus.labelFa}] ${n.noteText}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    when (item.status) {
      ConsultationStatus.PENDING -> {
        OutlinedTextField(
          value = internalNoteInput,
          onValueChange = { internalNoteInput = it },
          label = { Text("یادداشت داخلی پذیرش (اختیاری — محرمانه برای کادر داروخانه)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("staff_internal_note_${item.caseId}"),
        )
        Button(
          onClick = { onAcknowledgeReceived(internalNoteInput) },
          enabled = !isBusy,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = connectAccent,
              contentColor = if (isDark) DarkBackground else PureWhite,
            ),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("staff_receive_btn_${item.caseId}"),
        ) {
          if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
          }
          Text("پذیرش پرونده و ثبت وضعیت «دریافت شده»")
        }
      }

      ConsultationStatus.RECEIVED -> {
        OutlinedTextField(
          value = internalNoteInput,
          onValueChange = { internalNoteInput = it },
          label = { Text("یادداشت داخلی کادر درمان (اختیاری — به والد نمایش داده نمی‌شود)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("staff_internal_note_${item.caseId}"),
        )
        OutlinedTextField(
          value = parentReplyInput,
          onValueChange = { parentReplyInput = it },
          label = { Text("پاسخ علمی غیرتشخیصی برای والد (حداقل ۱۰ کاراکتر)") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("staff_parent_reply_${item.caseId}"),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Checkbox(
            checked = attested,
            onCheckedChange = { attested = it },
            modifier = Modifier.testTag("staff_attest_checkbox_${item.caseId}"),
          )
          Text(
            text = "تأییدیه حرفه‌ای داروساز: این راهنما غیرتشخیصی و متناسب با ایمنی دارویی کودک تهیه شده است و با مسئولیت علمی ثبت می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        val canSubmitReply =
          !isBusy && attested && parentReplyInput.trim().length >= ConsultationPayloadMapper.MIN_PARENT_REPLY_LENGTH
        Button(
          onClick = { onSubmitReview(internalNoteInput, parentReplyInput, attested) },
          enabled = canSubmitReply,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = sageAccent,
              contentColor = if (isDark) DarkBackground else PureWhite,
            ),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("staff_review_submit_btn_${item.caseId}"),
        ) {
          if (isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
          }
          Text("ثبت پاسخ نهایی و قفل کردن پرونده")
        }
      }

      ConsultationStatus.REVIEWED -> {
        Surface(
          color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.65f, darkAlpha = 0.14f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = sageAccent, modifier = Modifier.size(16.dp))
              Text("پاسخ منتشرشده برای والد (قفل‌شده):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = sageAccent)
            }
            Text(
              text = item.parentReplyFa ?: "بررسی داروساز ثبت شده است.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}
