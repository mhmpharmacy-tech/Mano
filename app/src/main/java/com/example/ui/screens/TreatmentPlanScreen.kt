package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.PediatricTreatmentPlan
import com.example.domain.model.TreatmentDosageForm
import com.example.domain.model.TreatmentMedicationItem
import com.example.domain.model.TreatmentPlanFormatter
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkErrorContainer
import com.example.ui.theme.DarkOnErrorContainer
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RxDarkCard
import com.example.ui.theme.RxDarkCardHighlight
import com.example.ui.theme.RxDarkGold
import com.example.ui.theme.RxDarkHeader
import com.example.ui.theme.RxDarkSurface
import com.example.ui.theme.RxDarkTextMuted
import com.example.ui.theme.RxDarkTextPrimary
import com.example.ui.theme.RxEmeraldDeep
import com.example.ui.theme.RxEmeraldMid
import com.example.ui.theme.RxGoldClassic
import com.example.ui.theme.RxInkObsidian
import com.example.ui.theme.RxIvoryPaper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreatmentPlanScreen(
  plan: PediatricTreatmentPlan,
  childProfile: ChildProfileDemo?,
  onToggleDoseSlot: (medicationId: String, slotIndex: Int) -> Unit,
  onSaveMedication: (TreatmentMedicationItem) -> Unit,
  onDeleteMedication: (medicationId: String) -> Unit,
  onUpdateHeader: (pharmacyNameFa: String, pharmacistBadgeFa: String, prescriptionDateFa: String, storageInstructionFa: String) -> Unit,
  onResetDefault: () -> Unit,
  onToggleSmartReminders: (Boolean) -> Unit,
  onShareForCaregiver: () -> Unit,
  onAskPharmacist: () -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  var editingMedication by remember { mutableStateOf<TreatmentMedicationItem?>(null) }
  var showAddMedicationSheet by remember { mutableStateOf(false) }
  var showHeaderEditor by remember { mutableStateOf(false) }

  val cardShellBg = if (isDark) RxDarkSurface else RxIvoryPaper
  val cardShellBorder = if (isDark) RxDarkGold.copy(alpha = 0.28f) else RxEmeraldDeep.copy(alpha = 0.22f)
  val headerBg = if (isDark) RxDarkHeader else RxEmeraldDeep
  val goldAccent = if (isDark) RxDarkGold else RxGoldClassic
  val primaryText = if (isDark) RxDarkTextPrimary else RxInkObsidian
  val mutedText = if (isDark) RxDarkTextMuted else RxEmeraldDeep.copy(alpha = 0.78f)
  val itemCardBg = if (isDark) RxDarkCard else PureWhite
  val itemCardBorder = if (isDark) RxDarkGold.copy(alpha = 0.20f) else RxEmeraldDeep.copy(alpha = 0.16f)

  val childSummaryFa = remember(plan, childProfile) { TreatmentPlanFormatter.resolveChildDisplaySummary(plan, childProfile) }
  val allergySummaryFa = remember(childProfile) { TreatmentPlanFormatter.resolveAllergySummary(childProfile) }
  val medCountFa = TreatmentPlanFormatter.toPersianDigits(plan.medications.size.toString())
  val completedToday = plan.completedTodaySlots
  val totalToday = plan.totalDailySlots.coerceAtLeast(1)
  val progressFraction = (completedToday.toFloat() / totalToday.toFloat()).coerceIn(0f, 1f)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("treatment_plan_screen"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Unified Luxury Pediatric Treatment Plan Card Container
      Surface(
          color = cardShellBg,
          shape = RoundedCornerShape(28.dp),
          border = BorderStroke(1.6.dp, cardShellBorder),
          shadowElevation = if (isDark) 4.dp else 8.dp,
          modifier = Modifier.fillMaxWidth().testTag("treatment_plan_card"),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Emerald + Gold Header (Pharmacy Identity + Pharmacist Seal + MANO Mascot + Child Profile)
            Column(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .background(headerBg)
                  .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              // Row 1: Pharmacy name + Verified Pharmacist Seal + Edit Header trigger
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Box(
                    modifier =
                      Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(goldAccent),
                  )
                  Text(
                    text = plan.pharmacyNameFa,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RxIvoryPaper,
                    maxLines = 2,
                  )
                }

                IconButton(
                  onClick = { showHeaderEditor = true },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("rx_edit_header_btn"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "ویرایش مشخصات داروخانه و نسخه",
                    tint = goldAccent,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }

              // Pharmacist Verification Pill + Prescription Validity
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  color = RxInkObsidian.copy(alpha = 0.36f),
                  shape = RoundedCornerShape(50),
                  border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.55f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Verified,
                      contentDescription = null,
                      tint = goldAccent,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = plan.pharmacistBadgeFa,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = goldAccent,
                    )
                  }
                }
                Surface(
                  color = RxIvoryPaper.copy(alpha = 0.10f),
                  shape = RoundedCornerShape(50),
                ) {
                  Text(
                    text = plan.prescriptionDateFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = RxIvoryPaper.copy(alpha = 0.88f),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                  )
                }
              }

              HorizontalDivider(color = RxIvoryPaper.copy(alpha = 0.14f))

              // Row 2: Real MANO Kid Mascot Avatar + Child Profile + Medication Count Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(54.dp)
                      .clip(RoundedCornerShape(16.dp))
                      .background(Brush.linearGradient(listOf(goldAccent, RxIvoryPaper)))
                      .padding(2.dp),
                ) {
                  Box(
                    modifier =
                      Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RxEmeraldMid),
                    contentAlignment = Alignment.Center,
                  ) {
                    Image(
                      painter = painterResource(id = R.drawable.img_mano_orbital_mascot),
                      contentDescription = "کاراکتر مانو کید، همراه برنامه درمانی کودک",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }

                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = "برنامه درمانی اختصاصی کودک",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = goldAccent,
                  )
                  Text(
                    text = childSummaryFa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = RxIvoryPaper,
                    maxLines = 2,
                    modifier = Modifier.testTag("rx_child_summary"),
                  )
                }

                Surface(
                  color = goldAccent,
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text(
                    text = "$medCountFa قلم دارو",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = RxInkObsidian,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("rx_med_count_badge"),
                  )
                }
              }

              // Row 3: Allergy Safety Pill + Today's Dose Adherence Bar
              Surface(
                color = RxIvoryPaper.copy(alpha = 0.09f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, RxIvoryPaper.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Shield,
                      contentDescription = null,
                      tint = goldAccent,
                      modifier = Modifier.size(15.dp),
                    )
                    Text(
                      text = allergySummaryFa,
                      style = MaterialTheme.typography.labelSmall,
                      color = RxIvoryPaper.copy(alpha = 0.94f),
                      fontWeight = FontWeight.Medium,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "پیشرفت مصرف امروز",
                      style = MaterialTheme.typography.labelSmall,
                      color = RxIvoryPaper.copy(alpha = 0.82f),
                    )
                    Text(
                      text =
                        "${TreatmentPlanFormatter.toPersianDigits(completedToday.toString())} از ${
                          TreatmentPlanFormatter.toPersianDigits(plan.totalDailySlots.toString())
                        } نوبت ثبت‌شده",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = goldAccent,
                      modifier = Modifier.testTag("rx_daily_progress_label"),
                    )
                  }
                  LinearProgressIndicator(
                    progress = { progressFraction },
                    color = goldAccent,
                    trackColor = RxIvoryPaper.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                  )
                }
              }
            }

            // Gold 4dp luxury accent divider
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(goldAccent))

            // 2. Upgraded Pediatric Safety Measure Guide (Syringe-first, NO household spoons!)
            Column(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .background(if (isDark) RxDarkCardHighlight else RxEmeraldDeep.copy(alpha = 0.06f))
                  .padding(horizontal = 14.dp, vertical = 10.dp)
                  .testTag("rx_measure_safety_bar"),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Straighten,
                  contentDescription = null,
                  tint = if (isDark) goldAccent else RxEmeraldDeep,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "راهنمای ایمن پیمانه (استاندارد کودکان):",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isDark) RxDarkTextPrimary else RxEmeraldDeep,
                )
              }
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                MeasureGuideChip(
                  text = "سرنگ مدرج = دقیق‌ترین ابزار",
                  highlighted = false,
                  isDark = isDark,
                )
                MeasureGuideChip(
                  text = "۱ سی‌سی = ۲۰ قطره (قطره‌چکان استاندارد)",
                  highlighted = false,
                  isDark = isDark,
                )
                MeasureGuideChip(
                  text = "از قاشق خانگی استفاده نکنید",
                  highlighted = true,
                  isDark = isDark,
                )
              }
            }

            HorizontalDivider(color = cardShellBorder.copy(alpha = 0.6f))

            // 3. Body: Numbered Medication Cards (Unclipped Badges + Full Strength Names + Daily Check-Offs)
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              if (plan.medications.isEmpty()) {
                Surface(
                  color = itemCardBg,
                  shape = RoundedCornerShape(18.dp),
                  border = BorderStroke(1.dp, itemCardBorder),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Medication,
                      contentDescription = null,
                      tint = goldAccent,
                      modifier = Modifier.size(34.dp),
                    )
                    Text(
                      text = "هنوز دارویی در این نسخه ثبت نشده است",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = primaryText,
                      textAlign = TextAlign.Center,
                    )
                    Text(
                      text = "با دکمه زیر داروی نسخه پزشک را اضافه کنید یا نسخه نمونه را بازیابی نمایید.",
                      style = MaterialTheme.typography.bodySmall,
                      color = mutedText,
                      textAlign = TextAlign.Center,
                    )
                  }
                }
              } else {
                plan.medications.forEachIndexed { index, med ->
                  TreatmentMedicationCard(
                    index = index + 1,
                    item = med,
                    isDark = isDark,
                    cardBg = itemCardBg,
                    cardBorder = itemCardBorder,
                    goldAccent = goldAccent,
                    primaryText = primaryText,
                    mutedText = mutedText,
                    onToggleSlot = { slotIdx -> onToggleDoseSlot(med.id, slotIdx) },
                    onEdit = { editingMedication = med },
                    onDelete = { onDeleteMedication(med.id) },
                  )
                }
              }

              // Add Medication Button
              OutlinedButton(
                onClick = { showAddMedicationSheet = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.4.dp, if (isDark) goldAccent.copy(alpha = 0.55f) else RxEmeraldDeep.copy(alpha = 0.35f)),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) goldAccent else RxEmeraldDeep,
                  ),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("rx_add_medication_btn"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("افزودن داروی جدید به نسخه", fontWeight = FontWeight.Bold)
              }

              // 4. Unified Storage Instruction Box
              Surface(
                color = if (isDark) RxDarkGold.copy(alpha = 0.12f) else RxGoldClassic.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.50f)),
                modifier = Modifier.fillMaxWidth().testTag("rx_storage_box"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = null,
                    tint = if (isDark) goldAccent else RxEmeraldDeep,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                      text = "شرایط نگهداری داروهای این نسخه:",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.ExtraBold,
                      color = if (isDark) goldAccent else RxEmeraldDeep,
                    )
                    Text(
                      text = plan.storageInstructionFa,
                      style = MaterialTheme.typography.bodySmall,
                      color = primaryText.copy(alpha = 0.88f),
                      lineHeight = 19.sp,
                    )
                  }
                }
              }

              // 5. Emergency Allergy Stop Banner (Critical Clinical Safety Upgrade)
              Surface(
                color = if (isDark) DarkErrorContainer else DangerSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, (if (isDark) DarkCoral else DangerCrimson).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("rx_allergy_stop_banner"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Emergency,
                    contentDescription = null,
                    tint = if (isDark) DarkCoral else DangerCrimson,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "هشدار قطع فوری: در صورت بروز کهیر، تورم لب یا پلک، خس‌خس یا تنگی نفس، مصرف دارو را متوقف کرده و فوراً با اورژانس ۱۱۵ یا پزشک تماس بگیرید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) DarkOnErrorContainer else DangerCrimson,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                  )
                }
              }

              // 6. Parent Action Footer (Smart Reminders + Ask Pharmacist + Share for Caregiver)
              Button(
                onClick = { onToggleSmartReminders(!plan.smartRemindersEnabled) },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = if (plan.smartRemindersEnabled) RxEmeraldMid else if (isDark) goldAccent else RxEmeraldDeep,
                    contentColor = if (plan.smartRemindersEnabled) RxIvoryPaper else if (isDark) RxDarkHeader else RxIvoryPaper,
                  ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("rx_smart_reminder_btn"),
              ) {
                Icon(
                  imageVector = if (plan.smartRemindersEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                  contentDescription = null,
                  modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text =
                    if (plan.smartRemindersEnabled) "یادآور هوشمند فعال است (برای لغو لمس کنید)"
                    else "تنظیم یادآور هوشمند برای همه داروها",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onAskPharmacist,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.4.dp, if (isDark) goldAccent.copy(alpha = 0.50f) else RxEmeraldDeep.copy(alpha = 0.30f)),
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = if (isDark) RxDarkCard else PureWhite,
                      contentColor = if (isDark) RxDarkTextPrimary else RxEmeraldDeep,
                    ),
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("rx_ask_pharmacist_btn"),
                ) {
                  Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("پرسش از داروساز", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                  onClick = onShareForCaregiver,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.4.dp, goldAccent.copy(alpha = 0.60f)),
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = if (isDark) RxDarkCard else PureWhite,
                      contentColor = if (isDark) goldAccent else RxEmeraldDeep,
                    ),
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("rx_share_caregiver_btn"),
                ) {
                  Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("ارسال برای مراقب", fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }
            }
          }
        }
      Spacer(modifier = Modifier.height(72.dp))
    }
  }

  if (showAddMedicationSheet || editingMedication != null) {
    TreatmentMedicationEditorSheet(
      existing = editingMedication,
      onDismiss = {
        showAddMedicationSheet = false
        editingMedication = null
      },
      onSave = { item ->
        onSaveMedication(item)
        showAddMedicationSheet = false
        editingMedication = null
      },
    )
  }

  if (showHeaderEditor) {
    TreatmentHeaderEditorSheet(
      plan = plan,
      onDismiss = { showHeaderEditor = false },
      onSave = onUpdateHeader,
      onResetToSample = onResetDefault,
    )
  }
}

@Composable
private fun MeasureGuideChip(
  text: String,
  highlighted: Boolean,
  isDark: Boolean,
) {
  val bg =
    when {
      highlighted && isDark -> DarkErrorContainer
      highlighted -> DangerSurface
      isDark -> RxDarkCard
      else -> PureWhite
    }
  val border =
    when {
      highlighted && isDark -> DarkCoral.copy(alpha = 0.55f)
      highlighted -> DangerCrimson.copy(alpha = 0.35f)
      isDark -> RxDarkGold.copy(alpha = 0.25f)
      else -> RxEmeraldDeep.copy(alpha = 0.16f)
    }
  val textColor =
    when {
      highlighted && isDark -> DarkOnErrorContainer
      highlighted -> DangerCrimson
      isDark -> RxDarkTextPrimary
      else -> RxEmeraldDeep
    }

  Surface(
    color = bg,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, border),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TreatmentMedicationCard(
  index: Int,
  item: TreatmentMedicationItem,
  isDark: Boolean,
  cardBg: Color,
  cardBorder: Color,
  goldAccent: Color,
  primaryText: Color,
  mutedText: Color,
  onToggleSlot: (Int) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val numberFa = TreatmentPlanFormatter.toPersianDigits(index.toString())
  val formIcon = resolveDosageFormIcon(item.form)

  Surface(
    color = cardBg,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.4.dp, cardBorder),
    shadowElevation = if (isDark) 1.dp else 2.dp,
    modifier = Modifier.fillMaxWidth().testTag("rx_med_card_${item.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        // Integrated Number Badge + Medical Form Thumbnail (Zero Clipping!)
        Box(
          modifier =
            Modifier
              .size(62.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(if (isDark) RxDarkCardHighlight else RxIvoryPaper)
              .border(1.dp, if (isDark) goldAccent.copy(alpha = 0.28f) else RxEmeraldDeep.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(top = 6.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = formIcon,
              contentDescription = item.form.labelFa,
              tint = if (isDark) goldAccent else RxEmeraldMid,
              modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = item.form.labelFa.substringBefore(" /"),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              fontWeight = FontWeight.Bold,
              color = mutedText,
              maxLines = 1,
            )
          }

          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .align(Alignment.TopStart)
                .size(22.dp)
                .clip(RoundedCornerShape(bottomEnd = 10.dp))
                .background(if (isDark) goldAccent else RxEmeraldDeep),
          ) {
            Text(
              text = numberFa,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = if (isDark) RxDarkHeader else goldAccent,
            )
          }
        }

        // Drug Title (Full Wrapping - Never Truncated!), Food Tag & Actions
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = item.nameWithStrengthFa,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = if (isDark) RxDarkTextPrimary else RxEmeraldDeep,
              maxLines = 2,
              modifier = Modifier.weight(1f),
            )
            Row {
              IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp).testTag("rx_edit_med_${item.id}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "ویرایش ${item.nameWithStrengthFa}",
                  tint = mutedText,
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp).testTag("rx_delete_med_${item.id}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "حذف ${item.nameWithStrengthFa}",
                  tint = if (isDark) DarkCoral else DangerCrimson,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          // Food Timing Pill
          Surface(
            color = if (isDark) RxDarkGold.copy(alpha = 0.16f) else RxGoldClassic.copy(alpha = 0.20f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = item.foodRelationFa,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isDark) goldAccent else RxEmeraldDeep,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      // Dose & Schedule High-Contrast Pills
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          color = if (isDark) RxEmeraldMid else RxEmeraldDeep,
          shape = RoundedCornerShape(10.dp),
        ) {
          Text(
            text = item.doseAmountFa,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = RxIvoryPaper,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
        Surface(
          color = if (isDark) RxDarkCardHighlight else RxIvoryPaper,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, if (isDark) goldAccent.copy(alpha = 0.25f) else RxEmeraldDeep.copy(alpha = 0.20f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Schedule,
              contentDescription = null,
              tint = if (isDark) goldAccent else RxEmeraldDeep,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = item.scheduleLabelFa,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = if (isDark) RxDarkTextPrimary else RxEmeraldDeep,
            )
          }
        }
      }

      // Pharmacist Counseling / Warning Note
      if (item.counselingNoteFa.isNotBlank()) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = if (item.isWarningHighlight) Icons.Rounded.WarningAmber else Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = goldAccent,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
          )
          Text(
            text = item.counselingNoteFa,
            style = MaterialTheme.typography.bodySmall,
            color = primaryText.copy(alpha = 0.80f),
            lineHeight = 18.sp,
          )
        }
      }

      // Interactive Daily Dose Check-Off Chips
      val slots = item.dailyDoseSlots.coerceIn(1, 6)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        for (slot in 1..slots) {
          val checkedTime = item.completedSlotTimesFa[slot]
          val isDone = checkedTime != null
          val slotFa = TreatmentPlanFormatter.toPersianDigits(slot.toString())
          Surface(
            onClick = { onToggleSlot(slot) },
            color =
              if (isDone) {
                if (isDark) RxEmeraldMid else RxEmeraldDeep
              } else {
                if (isDark) RxDarkSurface else RxIvoryPaper
              },
            shape = RoundedCornerShape(12.dp),
            border =
              BorderStroke(
                width = 1.dp,
                color = if (isDone) goldAccent else if (isDark) goldAccent.copy(alpha = 0.30f) else RxEmeraldDeep.copy(alpha = 0.24f),
              ),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("rx_dose_slot_${item.id}_$slot"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (isDone) goldAccent else mutedText.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = if (isDone) "نوبت $slotFa • $checkedTime" else "ثبت نوبت $slotFa",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isDone) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isDone) RxIvoryPaper else primaryText,
              )
            }
          }
        }
      }
    }
  }
}

private fun resolveDosageFormIcon(form: TreatmentDosageForm): ImageVector =
  when (form) {
    TreatmentDosageForm.SYRUP_SUSPENSION -> Icons.Rounded.LocalPharmacy
    TreatmentDosageForm.DROPS -> Icons.Rounded.WaterDrop
    TreatmentDosageForm.NASAL_SPRAY -> Icons.Rounded.Air
    TreatmentDosageForm.OINTMENT_CREAM -> Icons.Rounded.Healing
    TreatmentDosageForm.TABLET_SACHET -> Icons.Rounded.Medication
  }
