package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.PediatricTreatmentPlan
import com.example.domain.model.TreatmentDosageForm
import com.example.domain.model.TreatmentMedicationItem
import com.example.domain.model.TreatmentPlanFormatter
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RxDarkCard
import com.example.ui.theme.RxDarkGold
import com.example.ui.theme.RxDarkHeader
import com.example.ui.theme.RxDarkSurface
import com.example.ui.theme.RxDarkTextPrimary
import com.example.ui.theme.RxEmeraldDeep
import com.example.ui.theme.RxGoldClassic
import com.example.ui.theme.RxIvoryPaper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TreatmentMedicationEditorSheet(
  existing: TreatmentMedicationItem?,
  onDismiss: () -> Unit,
  onSave: (TreatmentMedicationItem) -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var nameWithStrength by remember(existing) { mutableStateOf(existing?.nameWithStrengthFa.orEmpty()) }
  var form by remember(existing) { mutableStateOf(existing?.form ?: TreatmentDosageForm.SYRUP_SUSPENSION) }
  var doseAmount by remember(existing) { mutableStateOf(existing?.doseAmountFa.orEmpty()) }
  var intervalHours by remember(existing) { mutableIntStateOf(existing?.intervalHours ?: 8) }
  var scheduleLabel by remember(existing) {
    mutableStateOf(existing?.scheduleLabelFa ?: "هر ${TreatmentPlanFormatter.toPersianDigits("8")} ساعت")
  }
  var foodRelation by remember(existing) { mutableStateOf(existing?.foodRelationFa ?: "همراه غذا یا پس از غذا") }
  var counselingNote by remember(existing) { mutableStateOf(existing?.counselingNoteFa.orEmpty()) }
  var dailySlots by remember(existing) { mutableIntStateOf(existing?.dailyDoseSlots ?: 3) }
  var validationErrorFa by remember { mutableStateOf<String?>(null) }

  val primaryAccent = if (isDark) RxDarkGold else RxEmeraldDeep
  val primaryButtonBg = if (isDark) RxDarkGold else RxEmeraldDeep
  val primaryButtonFg = if (isDark) RxDarkHeader else RxIvoryPaper
  val sheetBg = if (isDark) RxDarkSurface else RxIvoryPaper
  val headingColor = if (isDark) RxDarkTextPrimary else RxEmeraldDeep

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = sheetBg,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .testTag("treatment_med_editor_sheet")
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = if (existing == null) "افزودن دارو به برنامه درمانی" else "ویرایش قلم دارویی",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = headingColor,
      )

      OutlinedTextField(
        value = nameWithStrength,
        onValueChange = {
          nameWithStrength = it
          validationErrorFa = null
        },
        label = { Text("نام و غلظت دارو (مثلاً شربت استامینوفن ۱۶۰/۵)") },
        singleLine = false,
        maxLines = 2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rx_med_name_input"),
      )

      Text(
        text = "شکل دارویی",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = headingColor,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        TreatmentDosageForm.entries.forEach { option ->
          val selected = form == option
          FilterChip(
            selected = selected,
            onClick = { form = option },
            label = { Text(option.labelFa) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = primaryAccent.copy(alpha = 0.18f),
                selectedLabelColor = primaryAccent,
              ),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("rx_form_${option.name}"),
          )
        }
      }

      OutlinedTextField(
        value = doseAmount,
        onValueChange = {
          doseAmount = it
          validationErrorFa = null
        },
        label = { Text("دوز دقیق هر نوبت (${form.defaultUnitHintFa})") },
        placeholder = { Text("مثلاً ۵ سی‌سی با سرنگ مدرج") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rx_med_dose_input"),
      )

      Text(
        text = "فاصله مصرف (ساعت)",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = headingColor,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(4 to 6, 6 to 4, 8 to 3, 12 to 2, 24 to 1).forEach { (hours, suggestedSlots) ->
          val selected = intervalHours == hours
          val hoursFa = TreatmentPlanFormatter.toPersianDigits(hours.toString())
          FilterChip(
            selected = selected,
            onClick = {
              intervalHours = hours
              dailySlots = suggestedSlots
              scheduleLabel = "هر $hoursFa ساعت"
            },
            label = { Text("هر $hoursFa ساعت") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = primaryAccent.copy(alpha = 0.18f),
                selectedLabelColor = primaryAccent,
              ),
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }

      OutlinedTextField(
        value = scheduleLabel,
        onValueChange = { scheduleLabel = it },
        label = { Text("زمان‌بندی نمایشی (مثلاً هر ۲۴ ساعت، ساعت ۱۲ ظهر)") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rx_med_schedule_input"),
      )

      Text(
        text = "ارتباط با غذا یا زمان مصرف",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = headingColor,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(
          "همراه غذا یا پس از غذا",
          "فرقی ندارد (ناشتا یا با غذا)",
          "با معده خالی",
          "قبل از خواب و هنگام گرفتگی بینی",
        ).forEach { preset ->
          FilterChip(
            selected = foodRelation == preset,
            onClick = { foodRelation = preset },
            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = RxGoldClassic.copy(alpha = 0.22f),
                selectedLabelColor = headingColor,
              ),
          )
        }
      }

      Surface(
        color = if (isDark) RxDarkCard else PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "تعداد نوبت تیک روزانه",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = headingColor,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { dailySlots = (dailySlots - 1).coerceAtLeast(1) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Rounded.Remove, contentDescription = "کاهش تعداد نوبت", tint = primaryAccent)
            }
            Text(
              text = "${TreatmentPlanFormatter.toPersianDigits(dailySlots.toString())} نوبت",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = headingColor,
            )
            IconButton(
              onClick = { dailySlots = (dailySlots + 1).coerceAtMost(6) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "افزایش تعداد نوبت", tint = primaryAccent)
            }
          }
        }
      }

      OutlinedTextField(
        value = counselingNote,
        onValueChange = { counselingNote = it },
        label = { Text("نکته ایمنی یا راهنمای آماده‌سازی داروساز") },
        placeholder = { Text("مثلاً پیش از هر مصرف خوب تکان دهید و دوره درمان را کامل کنید.") },
        minLines = 2,
        maxLines = 3,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rx_med_note_input"),
      )

      validationErrorFa?.let { err ->
        Text(
          text = err,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("انصراف")
        }
        Button(
          onClick = {
            val cleanName = nameWithStrength.trim()
            val cleanDose = doseAmount.trim()
            if (cleanName.isBlank()) {
              validationErrorFa = "لطفاً نام و غلظت دارو را وارد کنید."
              return@Button
            }
            if (cleanDose.isBlank()) {
              validationErrorFa = "لطفاً دوز دقیق هر نوبت را وارد کنید."
              return@Button
            }
            val cleanSchedule =
              scheduleLabel.trim().ifBlank {
                "هر ${TreatmentPlanFormatter.toPersianDigits(intervalHours.toString())} ساعت"
              }
            val preservedSlots =
              existing?.completedSlotTimesFa?.filterKeys { it in 1..dailySlots } ?: emptyMap()
            onSave(
              TreatmentMedicationItem(
                id = existing?.id ?: "rx_${UUID.randomUUID().toString().take(8)}",
                nameWithStrengthFa = cleanName,
                form = form,
                doseAmountFa = cleanDose,
                intervalHours = intervalHours,
                scheduleLabelFa = cleanSchedule,
                foodRelationFa = foodRelation.trim().ifBlank { "طبق دستور داروساز" },
                counselingNoteFa = counselingNote.trim(),
                isWarningHighlight = existing?.isWarningHighlight ?: true,
                dailyDoseSlots = dailySlots,
                completedSlotTimesFa = preservedSlots,
              )
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = primaryButtonBg, contentColor = primaryButtonFg),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1.4f).minimumInteractiveComponentSize().testTag("rx_med_save_btn"),
        ) {
          Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("ذخیره در نسخه", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TreatmentHeaderEditorSheet(
  plan: PediatricTreatmentPlan,
  onDismiss: () -> Unit,
  onSave: (pharmacyNameFa: String, pharmacistBadgeFa: String, prescriptionDateFa: String, storageInstructionFa: String) -> Unit,
  onResetToSample: () -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var pharmacyName by remember(plan) { mutableStateOf(plan.pharmacyNameFa) }
  var pharmacistBadge by remember(plan) { mutableStateOf(plan.pharmacistBadgeFa) }
  var prescriptionDate by remember(plan) { mutableStateOf(plan.prescriptionDateFa) }
  var storageInstruction by remember(plan) { mutableStateOf(plan.storageInstructionFa) }

  val sheetBg = if (isDark) RxDarkSurface else RxIvoryPaper
  val headingColor = if (isDark) RxDarkTextPrimary else RxEmeraldDeep
  val primaryButtonBg = if (isDark) RxDarkGold else RxEmeraldDeep
  val primaryButtonFg = if (isDark) RxDarkHeader else RxIvoryPaper

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = sheetBg,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 20.dp)
          .testTag("treatment_header_editor_sheet"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "ویرایش مشخصات داروخانه و شرایط نگهداری",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = headingColor,
      )

      OutlinedTextField(
        value = pharmacyName,
        onValueChange = { pharmacyName = it },
        label = { Text("نام داروخانه یا کلینیک") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rx_pharmacy_name_input"),
      )

      OutlinedTextField(
        value = pharmacistBadge,
        onValueChange = { pharmacistBadge = it },
        label = { Text("مهر تأیید و شماره نظام داروساز") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("rx_pharmacist_badge_input"),
      )

      OutlinedTextField(
        value = prescriptionDate,
        onValueChange = { prescriptionDate = it },
        label = { Text("تاریخ یا وضعیت اعتبار نسخه") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = storageInstruction,
        onValueChange = { storageInstruction = it },
        label = { Text("شرایط یکپارچه نگهداری داروهای این نسخه") },
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedButton(
        onClick = {
          onResetToSample()
          onDismiss()
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("rx_reset_default_btn"),
      ) {
        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("بازیابی نسخه نمونه اصلاح‌شده بالینی")
      }

      Spacer(Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("انصراف")
        }
        Button(
          onClick = {
            onSave(pharmacyName, pharmacistBadge, prescriptionDate, storageInstruction)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = primaryButtonBg, contentColor = primaryButtonFg),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1.4f).minimumInteractiveComponentSize().testTag("rx_header_save_btn"),
        ) {
          Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("ذخیره مشخصات", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
