package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MascotPose
import com.example.domain.model.VaccineDashboardSummary
import com.example.domain.model.VaccineFilter
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.model.VaccineResolvedItem
import com.example.domain.model.VaccineStatus
import com.example.domain.util.ManoDomainLinks
import com.example.domain.util.VaccinationScheduleCatalog
import com.example.domain.util.VaccinationScheduleEngine
import com.example.ui.components.AlertSeverity
import com.example.ui.components.FlowchartDownConnector
import com.example.ui.components.LtrText
import com.example.ui.components.ManoBannerFrame
import com.example.ui.components.ManoCircularProgressSize
import com.example.ui.components.ManoCompactScreenHeader
import com.example.ui.components.ManoHealthProgressRing
import com.example.ui.components.ManoHealthRingTone
import com.example.ui.components.ManoIconMedallion
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MangoCare
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.SkyCare
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.ManoLinkShareHelper
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaccinesScreen(
  summary: VaccineDashboardSummary,
  birthDateEpochMs: Long?,
  reminderSettings: VaccineReminderSettings,
  childProfile: ChildProfileDemo?,
  hasNotificationPermission: Boolean,
  isCloudSignedIn: Boolean,
  onSaveBirthDate: (Long?) -> Unit,
  onUpdateReminderSettings: (enabled: Boolean, leadDays: Int) -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onSendPreviewReminder: () -> Unit,
  onSaveVaccineRecord: (VaccineRecord) -> Unit,
  onDeleteVaccineRecord: (String) -> Unit,
  onFeedback: (String) -> Unit,
) {
  var activeFilter by remember { mutableStateOf(VaccineFilter.ALL) }
  var birthDateInput by remember(birthDateEpochMs) {
    mutableStateOf(birthDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) } ?: "")
  }
  var birthDateErrorFa by remember(birthDateEpochMs) { mutableStateOf<String?>(null) }
  var showBirthDatePicker by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<VaccineResolvedItem?>(null) }

  val filteredItems =
    remember(summary.items, activeFilter) {
      VaccinationScheduleEngine.filterItems(summary.items, activeFilter)
    }

  val isDark = LocalManoDarkTheme.current
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)
  val mangoAccent = manoSemanticAccent(MangoCare, DarkGold)
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val skyAccent = manoSemanticAccent(SkyCare, DarkSky)
  val progressRatio =
    if (summary.totalCoreCount > 0) {
      (summary.administeredCoreCount.toFloat() / summary.totalCoreCount.toFloat()).coerceIn(0f, 1f)
    } else {
      0f
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).imePadding().testTag("vaccines_list"),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // COMPACT HERO & PROGRESS OVERVIEW
      item {
        val context = LocalContext.current
        val vaccinesUrl = ManoDomainLinks.vaccinesUrl()
        Surface(
          color = if (isDark) MaterialTheme.colorScheme.surface else PureWhite,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth().testTag("vaccine_overview_card"),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ManoHealthProgressRing(
                progress = progressRatio,
                size = ManoCircularProgressSize.SMALL,
                tone = if (progressRatio >= 1f) ManoHealthRingTone.EMERALD else ManoHealthRingTone.TURQUOISE,
                subLabel = "تکمیل",
                contentDescriptionFa =
                  "پیشرفت واکسن‌های اصلی: ${VaccinationScheduleEngine.toPersianDigits(summary.administeredCoreCount.toString())} از ${VaccinationScheduleEngine.toPersianDigits(summary.totalCoreCount.toString())} نوبت",
                modifier = Modifier.testTag("vaccine_progress_ring"),
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = stringResource(R.string.vaccines_screen_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    "${VaccinationScheduleEngine.toPersianDigits(summary.administeredCoreCount.toString())} از ${VaccinationScheduleEngine.toPersianDigits(summary.totalCoreCount.toString())} نوبت اصلی" +
                      if (isCloudSignedIn) " • همگام ابری" else "",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              IconButton(
                onClick = {
                  val shareBody = ManoDomainLinks.buildVaccinesShareText(summary, childProfile?.aliasFa)
                  ManoLinkShareHelper.shareTextWithLink(context, "برنامه واکسیناسیون مانو کید", shareBody)
                    .onFailure {
                      ManoLinkShareHelper.copyToClipboard(context, "لینک برنامه واکسن", vaccinesUrl)
                      onFeedback("لینک برنامه واکسیناسیون کپی شد.")
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_share_btn"),
              ) {
                Icon(Icons.Rounded.Share, contentDescription = "اشتراک‌گذاری برنامه واکسن", modifier = Modifier.size(20.dp))
              }
              IconButton(
                onClick = {
                  ManoLinkShareHelper.copyToClipboard(context, "لینک برنامه واکسن", vaccinesUrl)
                  onFeedback("لینک manokid.ir/vaccines کپی شد.")
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_copy_link_btn"),
              ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی لینک واکسیناسیون", modifier = Modifier.size(20.dp))
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              VaccineStatPill(
                label = "تزریق‌شده: ${VaccinationScheduleEngine.toPersianDigits(summary.administeredCoreCount.toString())}",
                accent = sageAccent,
              )
              VaccineStatPill(
                label = "نزدیک موعد: ${VaccinationScheduleEngine.toPersianDigits(summary.dueSoonCount.toString())}",
                accent = mangoAccent,
              )
              VaccineStatPill(
                label = "پیگیری: ${VaccinationScheduleEngine.toPersianDigits(summary.overdueCount.toString())}",
                accent = if (summary.overdueCount > 0) dangerAccent else skyAccent,
              )
            }

            summary.nextUpcoming?.let { next ->
              Surface(
                color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.55f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("vaccine_next_upcoming_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  val dateText =
                    next.effectiveDueDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) }
                      ?: "در انتظار تاریخ تولد"
                  Text(
                    text = "نوبت پیش‌رو: ${next.definition.milestoneLabelFa} • $dateText",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }
      }

      // COMPACT BIRTH DATE STRIP
      item {
        SectionCard(modifier = Modifier.testTag("vaccine_birth_date_card")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = birthDateInput,
              onValueChange = {
                birthDateInput = it
                birthDateErrorFa = null
              },
              label = { Text("۱. تاریخ تولد (۱۴۰۴/۰۵/۱۲)") },
              singleLine = true,
              isError = birthDateErrorFa != null,
              leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).testTag("vaccine_birthdate_input"),
            )
            Button(
              onClick = {
                val parsed = VaccinationScheduleEngine.parseFlexibleDateToEpochMs(birthDateInput)
                if (parsed == null) {
                  val msg = "فرمت تاریخ معتبر نیست؛ نمونه شمسی: ۱۴۰۴/۰۵/۱۲"
                  birthDateErrorFa = msg
                  onFeedback(msg)
                } else {
                  birthDateErrorFa = null
                  onSaveBirthDate(parsed)
                }
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_save_birthdate_btn"),
            ) {
              Text("ثبت")
            }
            IconButton(
              onClick = { showBirthDatePicker = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_pick_birthdate_btn"),
            ) {
              Icon(Icons.Rounded.EditCalendar, contentDescription = "انتخاب از تقویم")
            }
          }
          if (birthDateErrorFa != null) {
            Text(birthDateErrorFa.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
          }
          if (birthDateEpochMs != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "ثبت‌شده: ${VaccinationScheduleEngine.formatPersianDate(birthDateEpochMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = sageAccent,
              )
              TextButton(
                onClick = {
                  birthDateInput = ""
                  onSaveBirthDate(null)
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_clear_birthdate_btn"),
              ) {
                Text("پاک کردن")
              }
            }
          }
        }
      }

      // COMPACT REMINDER STRIP
      item {
        SectionCard(modifier = Modifier.testTag("vaccine_reminder_card")) {
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
                imageVector = if (reminderSettings.enabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                contentDescription = null,
                tint = if (reminderSettings.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text =
                  if (reminderSettings.enabled) {
                    "۲. یادآور ${VaccinationScheduleEngine.toPersianDigits(reminderSettings.leadDaysBefore.toString())} روز قبل"
                  } else {
                    "۲. یادآور پیش از موعد خاموش"
                  },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            Switch(
              checked = reminderSettings.enabled,
              onCheckedChange = { enabled ->
                onUpdateReminderSettings(enabled, reminderSettings.leadDaysBefore)
              },
              modifier = Modifier.testTag("vaccine_reminder_switch"),
            )
          }

          if (reminderSettings.enabled) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              VaccinationScheduleEngine.LEAD_DAY_OPTIONS.forEach { days ->
                val selected = reminderSettings.leadDaysBefore == days
                FilterChip(
                  selected = selected,
                  onClick = { onUpdateReminderSettings(true, days) },
                  label = { Text("${VaccinationScheduleEngine.toPersianDigits(days.toString())} روز") },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_lead_days_$days"),
                )
              }
              FilledTonalButton(
                onClick = {
                  if (!hasNotificationPermission) onRequestNotificationPermission() else onSendPreviewReminder()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_test_reminder_btn"),
              ) {
                Text("تست اعلان", style = MaterialTheme.typography.labelSmall)
              }
            }

            if (!hasNotificationPermission) {
              SeverityAlert(
                severity = AlertSeverity.CAUTION,
                title = "نیاز به مجوز نمایش اعلان",
                body = "برای دریافت یادآورهای واکسیناسیون، مجوز اعلان را فعال کنید.",
                actionLabel = "فعال‌سازی",
                onAction = onRequestNotificationPermission,
                modifier = Modifier.testTag("vaccine_permission_banner"),
              )
            }
          }
        }
      }

      // FILTER CHIPS + FLOWCHART CONNECTOR
      item {
        FlowchartDownConnector(labelFa = "فلوچارت نوبت‌های واکسیناسیون (لمس هر نوبت برای ثبت)")
      }
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          VaccineFilter.entries.forEach { filter ->
            FilterChip(
              selected = activeFilter == filter,
              onClick = { activeFilter = filter },
              label = { Text(filter.labelFa) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("vaccine_filter_${filter.name}"),
            )
          }
        }
      }

      // 2-COLUMN COMPACT VACCINE FLOWCHART BOARD
      if (filteredItems.isEmpty()) {
        item {
          SectionCard {
            Text(
              text = "در این فیلتر نوبت واکسنی یافت نشد.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        items(filteredItems.chunked(2), key = { row -> row.joinToString("_") { it.definition.id } }) { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            pair.forEach { item ->
              Box(modifier = Modifier.weight(1f)) {
                VaccineItemCard(
                  item = item,
                  isDark = isDark,
                  onOpenEdit = { editingItem = item },
                  onQuickMarkAdministeredToday = {
                    val todayMs = VaccinationScheduleEngine.startOfDayEpochMs(System.currentTimeMillis())
                    val updated =
                      (item.record ?: VaccineRecord(id = item.definition.id, vaccineId = item.definition.id)).copy(
                        customDueDateEpochMs = item.record?.customDueDateEpochMs ?: item.effectiveDueDateEpochMs,
                        administeredDateEpochMs = todayMs,
                        isDeferred = false,
                        updatedAtMs = System.currentTimeMillis(),
                      )
                    onSaveVaccineRecord(updated)
                  },
                )
              }
            }
            if (pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }

      item { Spacer(Modifier.height(8.dp)) }
    }
  }

  if (showBirthDatePicker) {
    val pickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = birthDateEpochMs ?: System.currentTimeMillis(),
      )
    DatePickerDialog(
      onDismissRequest = { showBirthDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            showBirthDatePicker = false
            pickerState.selectedDateMillis?.let { utcMs ->
              val localDayMs = utcPickerMillisToLocalDayMs(utcMs)
              birthDateInput = VaccinationScheduleEngine.formatPersianDate(localDayMs)
              onSaveBirthDate(localDayMs)
            }
          },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("تأیید تاریخ")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showBirthDatePicker = false },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("انصراف")
        }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  editingItem?.let { target ->
    VaccineEditDialog(
      item = target,
      onDismiss = { editingItem = null },
      onSave = { updatedRecord ->
        onSaveVaccineRecord(updatedRecord)
        editingItem = null
      },
      onResetRecord = {
        onDeleteVaccineRecord(target.definition.id)
        editingItem = null
      },
      onInvalidDateFeedback = onFeedback,
    )
  }
}

@Composable
private fun VaccineStatPill(
  label: String,
  accent: androidx.compose.ui.graphics.Color,
) {
  Surface(
    color = accent.copy(alpha = 0.13f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = accent,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun VaccineItemCard(
  item: VaccineResolvedItem,
  isDark: Boolean,
  onOpenEdit: () -> Unit,
  onQuickMarkAdministeredToday: () -> Unit,
) {
  val statusAccent =
    when (item.status) {
      VaccineStatus.ADMINISTERED -> manoSemanticAccent(SageCare, DarkSage)
      VaccineStatus.DUE_SOON -> manoSemanticAccent(MangoCare, DarkGold)
      VaccineStatus.OVERDUE -> manoSemanticAccent(DangerCrimson, DarkCoral)
      VaccineStatus.DEFERRED -> manoSemanticAccent(CoralCare, DarkCoral)
      VaccineStatus.UPCOMING -> manoSemanticAccent(SkyCare, DarkSky)
    }
  val cardColor = if (isDark) MaterialTheme.colorScheme.surface else PureWhite
  val borderColor = manoBorderColor()
  val dueLabel =
    item.effectiveDueDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) }
      ?: "ثبت‌نشده"

  Surface(
    onClick = onOpenEdit,
    color = cardColor,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.2.dp, if (item.status == VaccineStatus.DUE_SOON || item.status == VaccineStatus.OVERDUE) statusAccent.copy(alpha = 0.65f) else borderColor),
    modifier = Modifier.fillMaxWidth().testTag("vaccine_card_${item.definition.id}"),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector =
            when (item.status) {
              VaccineStatus.ADMINISTERED -> Icons.Rounded.CheckCircle
              VaccineStatus.OVERDUE -> Icons.Rounded.WarningAmber
              else -> Icons.Rounded.Vaccines
            },
          contentDescription = null,
          tint = statusAccent,
          modifier = Modifier.size(18.dp),
        )
        Surface(
          color = statusAccent.copy(alpha = 0.13f),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = item.status.labelFa,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = statusAccent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Text(
        text = item.definition.milestoneLabelFa,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = item.definition.vaccinesIncludedFa,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = dueLabel,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = statusAccent,
        maxLines = 1,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        OutlinedButton(
          onClick = onOpenEdit,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("vaccine_edit_btn_${item.definition.id}"),
        ) {
          Text("ثبت", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        if (item.status != VaccineStatus.ADMINISTERED) {
          Button(
            onClick = onQuickMarkAdministeredToday,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("vaccine_quick_mark_${item.definition.id}"),
          ) {
            Text("شد", style = MaterialTheme.typography.labelSmall, maxLines = 1)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaccineEditDialog(
  item: VaccineResolvedItem,
  onDismiss: () -> Unit,
  onSave: (VaccineRecord) -> Unit,
  onResetRecord: () -> Unit,
  onInvalidDateFeedback: (String) -> Unit,
) {
  var dueDateText by remember(item) {
    mutableStateOf(
      (item.record?.customDueDateEpochMs ?: item.effectiveDueDateEpochMs)
        ?.let { VaccinationScheduleEngine.formatPersianDate(it) } ?: ""
    )
  }
  var adminDateText by remember(item) {
    mutableStateOf(
      item.record?.administeredDateEpochMs
        ?.let { VaccinationScheduleEngine.formatPersianDate(it) } ?: ""
    )
  }
  var facilityText by remember(item) { mutableStateOf(item.record?.facilityOrLotFa ?: "") }
  var noteText by remember(item) { mutableStateOf(item.record?.noteFa ?: "") }
  var isDeferred by remember(item) { mutableStateOf(item.record?.isDeferred ?: false) }
  var pickTargetField by remember { mutableStateOf<String?>(null) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "ثبت تاریخ و جزئیات — ${item.definition.milestoneLabelFa}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = item.definition.titleFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = dueDateText,
        onValueChange = { dueDateText = it },
        label = { Text("تاریخ موعد مقرر (شمسی یا میلادی)") },
        placeholder = { Text("۱۴۰۴/۰۷/۱۰") },
        singleLine = true,
        trailingIcon = {
          TextButton(onClick = { pickTargetField = "due" }) {
            Text("تقویم")
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("vaccine_dialog_due_input"),
      )

      OutlinedTextField(
        value = adminDateText,
        onValueChange = { adminDateText = it },
        label = { Text("تاریخ انجام تزریق (خالی = هنوز تزریق نشده)") },
        placeholder = { Text("۱۴۰۴/۰۷/۱۲") },
        singleLine = true,
        trailingIcon = {
          TextButton(onClick = { pickTargetField = "admin" }) {
            Text("تقویم")
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("vaccine_dialog_admin_input"),
      )

      OutlinedButton(
        onClick = {
          adminDateText = VaccinationScheduleEngine.formatPersianDate(System.currentTimeMillis())
          isDeferred = false
        },
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("vaccine_dialog_admin_today_btn"),
      ) {
        Text("درج تاریخ امروز به عنوان تاریخ تزریق")
      }

      OutlinedTextField(
        value = facilityText,
        onValueChange = { facilityText = it },
        label = { Text("نام مرکز بهداشت یا شماره سری ساخت (اختیاری)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("vaccine_dialog_facility_input"),
      )

      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        label = { Text("یادداشت والد (مثلاً واکنش تب یا توصیه پزشک)") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("vaccine_dialog_note_input"),
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Checkbox(
          checked = isDeferred,
          onCheckedChange = { isDeferred = it },
          modifier = Modifier.testTag("vaccine_dialog_deferred_checkbox"),
        )
        Spacer(Modifier.width(6.dp))
        Text("تعویق موقت با نظر پزشک", style = MaterialTheme.typography.bodyMedium)
      }

      if (item.record != null) {
        TextButton(
          onClick = onResetRecord,
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("vaccine_dialog_reset_btn"),
        ) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("حذف سابقه ثبت‌شده این واکسن")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("بستن")
        }
        Button(
          onClick = {
            val parsedDue =
              if (dueDateText.isBlank()) null
              else {
                VaccinationScheduleEngine.parseFlexibleDateToEpochMs(dueDateText) ?: run {
                  onInvalidDateFeedback("فرمت «تاریخ موعد» معتبر نیست.")
                  return@Button
                }
              }
            val parsedAdmin =
              if (adminDateText.isBlank()) null
              else {
                VaccinationScheduleEngine.parseFlexibleDateToEpochMs(adminDateText) ?: run {
                  onInvalidDateFeedback("فرمت «تاریخ تزریق» معتبر نیست.")
                  return@Button
                }
              }
            onSave(
              VaccineRecord(
                id = item.definition.id,
                vaccineId = item.definition.id,
                customDueDateEpochMs = parsedDue,
                administeredDateEpochMs = parsedAdmin,
                isDeferred = isDeferred && parsedAdmin == null,
                facilityOrLotFa = facilityText.trim(),
                noteFa = noteText.trim(),
                updatedAtMs = System.currentTimeMillis(),
                isSyncedCloud = item.record?.isSyncedCloud ?: false,
              )
            )
          },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("vaccine_dialog_save_btn"),
        ) {
          Text("ذخیره تغییرات")
        }
      }

      Spacer(Modifier.height(20.dp))
    }
  }

  if (pickTargetField != null) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    DatePickerDialog(
      onDismissRequest = { pickTargetField = null },
      confirmButton = {
        TextButton(
          onClick = {
            val target = pickTargetField
            pickTargetField = null
            pickerState.selectedDateMillis?.let { utcMs ->
              val formatted = VaccinationScheduleEngine.formatPersianDate(utcPickerMillisToLocalDayMs(utcMs))
              if (target == "due") dueDateText = formatted else adminDateText = formatted
            }
          }
        ) {
          Text("تأیید")
        }
      },
      dismissButton = {
        TextButton(onClick = { pickTargetField = null }) { Text("انصراف") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}

private fun utcPickerMillisToLocalDayMs(utcMillis: Long): Long {
  val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
  val localCal =
    Calendar.getInstance().apply {
      clear()
      set(
        utcCal.get(Calendar.YEAR),
        utcCal.get(Calendar.MONTH),
        utcCal.get(Calendar.DAY_OF_MONTH),
        0,
        0,
        0,
      )
      set(Calendar.MILLISECOND, 0)
    }
  return localCal.timeInMillis
}
