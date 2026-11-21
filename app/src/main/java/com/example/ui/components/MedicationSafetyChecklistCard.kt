package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MedicationCheckCategory
import com.example.domain.model.MedicationSafetyCheckItem
import com.example.domain.model.MedicationSafetyChecklistCatalog
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationSafetyChecklistPanel(
  checkedIds: Set<String>,
  childProfile: ChildProfileDemo?,
  timelineSnapshot: FeverDoseTimelineSnapshot,
  onToggleItem: (String) -> Unit,
  onResetChecklist: () -> Unit,
  onOpenDoseLogbook: () -> Unit,
  onOpenSyringeLab: () -> Unit,
  onOpenOverlapChecker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().testTag("med_safety_checklist_panel"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    MedicationChecklistHeaderCard(
      checkedIds = checkedIds,
      childProfile = childProfile,
      timelineSnapshot = timelineSnapshot,
    )
    MedicationCheckCategory.entries.forEach { category ->
      MedicationChecklistCategoryCard(
        category = category,
        checkedIds = checkedIds,
        onToggleItem = onToggleItem,
        onOpenDoseLogbook = onOpenDoseLogbook,
        onOpenSyringeLab = onOpenSyringeLab,
        onOpenOverlapChecker = onOpenOverlapChecker,
      )
    }
    MedicationChecklistActionsCard(
      checkedIds = checkedIds,
      onResetChecklist = onResetChecklist,
      onOpenDoseLogbook = onOpenDoseLogbook,
      onOpenSyringeLab = onOpenSyringeLab,
      onOpenOverlapChecker = onOpenOverlapChecker,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationChecklistHeaderCard(
  checkedIds: Set<String>,
  childProfile: ChildProfileDemo?,
  timelineSnapshot: FeverDoseTimelineSnapshot,
  modifier: Modifier = Modifier,
) {
  val progress = remember(checkedIds) { MedicationSafetyChecklistCatalog.evaluateProgress(checkedIds) }
  val animatedFraction by animateFloatAsState(
    targetValue = progress.progressFraction,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "medChecklistProgress",
  )

  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val cardSurface = manoCardSurface()
  val borderColor = manoBorderColor()
  val statusAccent = if (progress.isAllCriticalReady) successAccent else MaterialTheme.colorScheme.primary
  val statusBg =
    if (progress.isAllCriticalReady) manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.50f, darkAlpha = 0.16f)
    else cardSurface
  Surface(
    color = statusBg,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.35f)),
    modifier = modifier.fillMaxWidth().testTag("med_safety_checklist_header"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ManoIconMedallion(
          icon = if (progress.isAllCriticalReady) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
          accentColor = statusAccent,
          iconKind = null,
          containerSize = 46.dp,
          iconSize = 24.dp,
          cornerRadius = 14.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "چک‌لیست تعاملی ایمنی مصرف دارو",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = statusAccent,
          )
          Text(
            text = progress.statusHeadlineFa,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("med_safety_status_headline"),
          )
        }
        ManoHealthProgressRing(
          progress = animatedFraction,
          size = ManoCircularProgressSize.LARGE,
          tone = if (progress.isAllCriticalReady) ManoHealthRingTone.EMERALD else ManoHealthRingTone.TURQUOISE,
          valueText = "${progress.checkedCount}/${progress.totalCount}",
          subLabel = if (progress.isAllCriticalReady) "آماده" else "ایمنی",
          contentDescriptionFa = "وضعیت چک‌لیست ایمنی دارو: ${progress.checkedCount} از ${progress.totalCount}",
          modifier = Modifier.testTag("med_safety_count_badge"),
        )
      }

      LinearProgressIndicator(
        progress = { animatedFraction },
        color = statusAccent,
        trackColor = statusAccent.copy(alpha = 0.14f),
        strokeCap = StrokeCap.Round,
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).testTag("med_safety_progress_bar"),
      )

      Text(
        text = progress.statusSublineFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Surface(
        color = manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.45f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().testTag("med_safety_live_context_card"),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "داده‌های زنده برای تطبیق سریع:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = infoAccent,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val weightText = childProfile?.weightKgText?.takeIf { it.isNotBlank() }?.let { "وزن کودک: $it کیلوگرم" }
              ?: "وزن در پروفایل ثبت نشده"
            ChecklistInfoPill(weightText)
            val allergyText = childProfile?.allergiesFa?.takeIf { it.isNotBlank() }?.let { "حساسیت: $it" }
              ?: "حساسیت دارویی ثبت نشده"
            ChecklistInfoPill(allergyText)
            ChecklistInfoPill(
              text = "استامینوفن: ${timelineSnapshot.acetaminophenStatus.dosesLast24h} نوبت (۲۴س)",
              accent = if (timelineSnapshot.acetaminophenStatus.minutesRemainingSafeInterval == 0L) sageAccent else cautionAccent,
            )
            ChecklistInfoPill(
              text = "ایبوپروفن: ${timelineSnapshot.ibuprofenStatus.dosesLast24h} نوبت (۲۴س)",
              accent = if (timelineSnapshot.ibuprofenStatus.minutesRemainingSafeInterval == 0L) sageAccent else cautionAccent,
            )
          }
        }
      }
    }
  }
}

@Composable
fun MedicationChecklistCategoryHeader(
  category: MedicationCheckCategory,
  checkedIds: Set<String>,
  modifier: Modifier = Modifier,
) {
  val catItems = remember(category) { MedicationSafetyChecklistCatalog.items.filter { it.category == category } }
  val catDone = catItems.count { it.id in checkedIds }
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    modifier = modifier.fillMaxWidth().testTag("med_check_category_${category.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = category.labelFa,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = "$catDone/${catItems.size}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (catDone == catItems.size) manoSemanticAccent(SuccessEmerald, DarkSage) else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun MedicationChecklistStepCard(
  item: MedicationSafetyCheckItem,
  checked: Boolean,
  onToggle: () -> Unit,
  onOpenDoseLogbook: () -> Unit,
  onOpenSyringeLab: () -> Unit,
  onOpenOverlapChecker: () -> Unit,
  modifier: Modifier = Modifier,
  expanded: Boolean? = null,
  onToggleExpanded: (() -> Unit)? = null,
) {
  val helperAction: Pair<String, () -> Unit>? =
    when (item.id) {
      "check_concentration_match", "check_shake_and_oral_syringe" ->
        "تطبیق با سرنگ mL" to onOpenSyringeLab
      "check_no_hidden_overlap" ->
        "بررسی تداخل دو دارو" to onOpenOverlapChecker
      "check_record_and_childproof_cap" ->
        "ثبت در خط زمانی" to onOpenDoseLogbook
      else -> null
    }
  ChecklistStepRowCard(
    item = item,
    checked = checked,
    onToggle = onToggle,
    helperActionLabel = helperAction?.first,
    onHelperAction = helperAction?.second,
    externalExpanded = expanded,
    onExternalToggleExpanded = onToggleExpanded,
    modifier = modifier,
  )
}

@Composable
fun MedicationChecklistCategoryCard(
  category: MedicationCheckCategory,
  checkedIds: Set<String>,
  onToggleItem: (String) -> Unit,
  onOpenDoseLogbook: () -> Unit,
  onOpenSyringeLab: () -> Unit,
  onOpenOverlapChecker: () -> Unit,
  modifier: Modifier = Modifier,
  expandedTipIds: Set<String>? = null,
  onToggleTip: ((String) -> Unit)? = null,
) {
  val catItems = remember(category) { MedicationSafetyChecklistCatalog.items.filter { it.category == category } }
  val catDone = catItems.count { it.id in checkedIds }
  Surface(
    color = manoCardSurface(),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, manoBorderColor()),
    modifier = modifier.fillMaxWidth().testTag("med_check_category_${category.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = category.labelFa,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "$catDone/${catItems.size}",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (catDone == catItems.size) manoSemanticAccent(SuccessEmerald, DarkSage) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      catItems.forEach { item ->
        MedicationChecklistStepCard(
          item = item,
          checked = item.id in checkedIds,
          onToggle = { onToggleItem(item.id) },
          onOpenDoseLogbook = onOpenDoseLogbook,
          onOpenSyringeLab = onOpenSyringeLab,
          onOpenOverlapChecker = onOpenOverlapChecker,
          expanded = expandedTipIds?.contains(item.id),
          onToggleExpanded = onToggleTip?.let { toggle -> { toggle(item.id) } },
        )
      }
    }
  }
}

@Composable
fun MedicationChecklistActionsCard(
  checkedIds: Set<String>,
  onResetChecklist: () -> Unit,
  onOpenDoseLogbook: () -> Unit,
  onOpenSyringeLab: () -> Unit,
  onOpenOverlapChecker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val progress = remember(checkedIds) { MedicationSafetyChecklistCatalog.evaluateProgress(checkedIds) }
  SectionCard(modifier = modifier.testTag("med_safety_checklist_actions_card")) {
    if (!progress.isAllCriticalReady) {
      Surface(
        color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.65f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "توصیه ایمنی: پیش از ثبت یا خوراندن نوبت جدید، تمام ${progress.criticalTotalCount} گام الزامی را تیک بزنید (${progress.criticalCheckedCount}/${progress.criticalTotalCount} تاییدشده).",
          style = MaterialTheme.typography.labelSmall,
          color = manoSemanticAccent(CautionAmber, DarkGold),
          modifier = Modifier.padding(10.dp),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(
        onClick = onOpenDoseLogbook,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = if (progress.isAllCriticalReady) SuccessEmerald else MaterialTheme.colorScheme.primary,
          ),
        modifier = Modifier.weight(1.25f).minimumInteractiveComponentSize().testTag("med_checklist_go_logbook_btn"),
      ) {
        Icon(
          imageVector = if (progress.isAllCriticalReady) Icons.Rounded.CheckCircle else Icons.Rounded.EditNote,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = if (progress.isAllCriticalReady) "تایید و ثبت نوبت دارو" else "ثبت نوبت در خط زمانی",
          maxLines = 1,
        )
      }

      OutlinedButton(
        onClick = onResetChecklist,
        enabled = progress.checkedCount > 0,
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("med_checklist_reset_btn"),
      ) {
        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("نوبت جدید", maxLines = 1)
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = onOpenSyringeLab,
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("med_checklist_open_syringe_btn"),
      ) {
        Icon(Icons.Rounded.Vaccines, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("آزمایشگاه سرنگ", maxLines = 1)
      }
      OutlinedButton(
        onClick = onOpenOverlapChecker,
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("med_checklist_open_overlap_btn"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("تداخل‌سنج دارو", maxLines = 1)
      }
    }
  }
}

@Composable
private fun ChecklistStepRowCard(
  item: MedicationSafetyCheckItem,
  checked: Boolean,
  onToggle: () -> Unit,
  helperActionLabel: String?,
  onHelperAction: (() -> Unit)?,
  externalExpanded: Boolean? = null,
  onExternalToggleExpanded: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var localExpanded by rememberSaveable(item.id) { mutableStateOf(false) }
  val expanded = externalExpanded ?: localExpanded
  val toggleExpanded: () -> Unit = onExternalToggleExpanded ?: { localExpanded = !localExpanded }
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
  val cardSurface = manoCardSurface()
  val borderDefault = manoBorderColor()
  val checkedSurface = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.45f, darkAlpha = 0.16f)
  val bgColor by animateColorAsState(
    targetValue = if (checked) checkedSurface else cardSurface,
    label = "checkBg_${item.id}",
  )
  val borderColor by animateColorAsState(
    targetValue = if (checked) successAccent.copy(alpha = 0.45f) else borderDefault,
    label = "checkBorder_${item.id}",
  )

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, borderColor),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .testTag("med_check_item_${item.id}"),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onToggle() }.minimumInteractiveComponentSize(),
      ) {
        Checkbox(
          checked = checked,
          onCheckedChange = { onToggle() },
          colors =
            CheckboxDefaults.colors(
              checkedColor = successAccent,
              uncheckedColor = MaterialTheme.colorScheme.primary,
            ),
          modifier = Modifier.testTag("med_check_box_${item.id}"),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ChecklistInfoPill(
              text = item.stepNumberFa,
              accent = if (checked) successAccent else MaterialTheme.colorScheme.primary,
            )
            if (item.isCritical) {
              ChecklistInfoPill(
                text = if (checked) "تاییدشد" else "الزامی",
                accent = if (checked) successAccent else cautionAccent,
              )
            }
          }

          Text(
            text = item.titleFa,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          )

          Text(
            text = "پیشگیری از: ${item.preventsErrorFa}",
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) successAccent else infoAccent,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = toggleExpanded,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("med_check_tip_toggle_${item.id}"),
        ) {
          Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(if (expanded) "بستن راهنما" else "چرا مهم است؟", style = MaterialTheme.typography.labelSmall)
        }

        if (helperActionLabel != null && onHelperAction != null) {
          TextButton(
            onClick = onHelperAction,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("med_check_helper_${item.id}"),
          ) {
            Text(helperActionLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }
      }

      if (expanded) {
        Surface(
          color = manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.45f, darkAlpha = 0.16f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 2.dp).testTag("med_check_tip_body_${item.id}"),
        ) {
          Text(
            text = item.detailTipFa,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ChecklistInfoPill(
  text: String,
  accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
  Surface(
    color = accent.copy(alpha = 0.11f),
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = accent,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}
