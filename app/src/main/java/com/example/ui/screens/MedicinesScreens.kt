package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Button
import com.example.ui.components.ManoCircularProgressSize
import com.example.ui.components.ManoTurquoiseLoadingRing
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MedicationDoseLogCloud
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MascotPose
import com.example.domain.model.MedicationCheckCategory
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MedicationSafetyCategory
import com.example.domain.model.MedicationSafetyChecklistCatalog
import com.example.domain.model.SymptomFlow
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.ManoDomainLinks
import com.example.domain.util.MedicationOverlapEngine
import com.example.ui.auth.CloudSyncAuthCard
import com.example.ui.util.ManoLinkShareHelper
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ClinicalBulletList
import com.example.ui.components.ClinicalNote
import com.example.ui.components.EmptyState
import com.example.ui.components.FeverDoseTimelineCard
import com.example.ui.components.InteractiveOralSyringeLabCard
import com.example.ui.components.LtrText
import com.example.ui.components.ManoFramedIllustration
import com.example.ui.components.MedicationChecklistActionsCard
import com.example.ui.components.MedicationChecklistCategoryHeader
import com.example.ui.components.MedicationChecklistHeaderCard
import com.example.ui.components.MedicationChecklistStepCard
import com.example.ui.components.MedicineCard
import com.example.ui.components.PharmacistCtaCard
import com.example.ui.components.ScientificHealthProductComparisonSection
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.viewmodel.CloudDataState
import com.google.firebase.auth.FirebaseUser

private enum class MedicinesSubTab(val labelFa: String, val tag: String) {
  KNOWLEDGE_BASE("پایگاه دانش", "med_tab_kb"),
  SYRINGE_DOSE_LAB("سرنگ دوز وزن/سن", "med_tab_syringe"),
  OVERLAP_CHECKER("بررسی هشدارهای دارویی", "med_tab_overlap"),
  PRODUCT_COMPARISON("مقایسه علمی محصولات", "med_tab_comparison"),
  DOSE_LOGBOOK("خط زمانی تب و دارو (۲۴س)", "med_tab_logbook"),
  SAFETY_CHECKLIST("چک‌لیست ایمنی دارو", "med_tab_checklist"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicinesScreen(
  query: String,
  medications: List<MedicationMonograph>,
  allMedicationsCatalog: List<MedicationMonograph> = medications,
  onQueryChange: (String) -> Unit,
  onOpenMedication: (String) -> Unit,
  onNavigateDevices: () -> Unit,
  onNavigateAskPharmacist: () -> Unit,
  selectedCategory: MedicationSafetyCategory? = null,
  onCategorySelect: (MedicationSafetyCategory?) -> Unit = {},
  childProfile: ChildProfileDemo? = null,
  childBirthDateEpochMs: Long? = null,
  currentUser: FirebaseUser? = null,
  doseLogsState: CloudDataState<List<MedicationDoseLogCloud>> = CloudDataState.Success(emptyList()),
  timelineSnapshot: FeverDoseTimelineSnapshot =
    FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList()),
  onAddDoseLog: (slug: String, nameFa: String, formFa: String, volumeMl: String, timeLabel: String, noteFa: String, onDone: () -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
  onDeleteDoseLog: (id: String, isCloud: Boolean) -> Unit = { _, _ -> },
  onAddFeverLog: (tempCelsius: Float, siteFa: String, timeLabelFa: String, caregiverFa: String, noteFa: String, onDone: () -> Unit) -> Unit = { _, _, _, _, _, _ -> },
  onDeleteFeverLog: (id: String, isCloud: Boolean) -> Unit = { _, _ -> },
  onOpen3DWithDose: (Float) -> Unit = {},
  onOpenUrgentDoctorGuide: () -> Unit = {},
  selectedSymptomFlow: SymptomFlow? = null,
  savedVisitDraftsCount: Int = 0,
  onOpenVisitDraftStudio: () -> Unit = {},
  startOnTimeline: Boolean = false,
  onTimelineStartConsumed: () -> Unit = {},
  checkedMedSafetyIds: Set<String> = emptySet(),
  onToggleMedSafetyCheck: (String) -> Unit = {},
  onResetMedSafetyChecklist: () -> Unit = {},
  startOnChecklist: Boolean = false,
  onChecklistStartConsumed: () -> Unit = {},
  requestedSubTabKey: String? = null,
  onSubTabKeyConsumed: () -> Unit = {},
  onCloudAuthSuccess: () -> Unit = {},
  onCloudAuthError: (String) -> Unit = {},
  onSignedOut: () -> Unit = {},
  openTemperatureEntry: Boolean = false,
) {
  val checklistProgress = remember(checkedMedSafetyIds) { MedicationSafetyChecklistCatalog.evaluateProgress(checkedMedSafetyIds) }
  var focusTemperatureEntry by rememberSaveable { mutableStateOf(openTemperatureEntry) }
  var activeSubTab by rememberSaveable {
    mutableStateOf(
      when {
        startOnChecklist -> MedicinesSubTab.SAFETY_CHECKLIST
        openTemperatureEntry || startOnTimeline -> MedicinesSubTab.DOSE_LOGBOOK
        requestedSubTabKey == "checklist" -> MedicinesSubTab.SAFETY_CHECKLIST
        requestedSubTabKey == "timeline" -> MedicinesSubTab.DOSE_LOGBOOK
        requestedSubTabKey == "syringe" -> MedicinesSubTab.SYRINGE_DOSE_LAB
        requestedSubTabKey == "overlap" -> MedicinesSubTab.OVERLAP_CHECKER
        requestedSubTabKey == "comparison" -> MedicinesSubTab.PRODUCT_COMPARISON
        else -> MedicinesSubTab.KNOWLEDGE_BASE
      }
    )
  }

  LaunchedEffect(startOnTimeline) {
    if (startOnTimeline) {
      activeSubTab = MedicinesSubTab.DOSE_LOGBOOK
      onTimelineStartConsumed()
    }
  }

  LaunchedEffect(startOnChecklist) {
    if (startOnChecklist) {
      activeSubTab = MedicinesSubTab.SAFETY_CHECKLIST
      onChecklistStartConsumed()
    }
  }

  LaunchedEffect(requestedSubTabKey) {
    if (!requestedSubTabKey.isNullOrBlank()) {
      activeSubTab =
        when (requestedSubTabKey.lowercase()) {
          "timeline" -> MedicinesSubTab.DOSE_LOGBOOK
          "checklist" -> MedicinesSubTab.SAFETY_CHECKLIST
          "syringe" -> MedicinesSubTab.SYRINGE_DOSE_LAB
          "overlap" -> MedicinesSubTab.OVERLAP_CHECKER
          "comparison" -> MedicinesSubTab.PRODUCT_COMPARISON
          else -> MedicinesSubTab.KNOWLEDGE_BASE
        }
      onSubTabKeyConsumed()
    }
  }

  // Overlap checker selections
  val catalog = remember(allMedicationsCatalog) { allMedicationsCatalog }
  var firstMedIndex by remember(catalog) { mutableIntStateOf(0) }
  var secondMedIndex by remember(catalog) { mutableIntStateOf(if (catalog.size > 3) 3 else 0) }

  // Checklist expanded rationale IDs preserved across LazyColumn item recycling
  var expandedChecklistTipIds by remember { mutableStateOf(emptySet<String>()) }

  // Dose log form state
  var selectedLogMedSlug by remember(catalog) { mutableStateOf(catalog.firstOrNull()?.slug ?: "acetaminophen-pediatric") }
  var dosageFormInput by remember { mutableStateOf("شربت خوراکی با سرنگ مدرج mL") }
  var volumeMlInput by remember { mutableStateOf("") }
  var timeLabelInput by remember { mutableStateOf("امروز") }
  var logNoteInput by remember { mutableStateOf("") }
  var logFormError by remember { mutableStateOf<String?>(null) }

  val quickSearchSuggestions =
    remember {
      listOf("استامینوفن", "ایبوپروفن", "آنتی‌بیوتیک", "ORS", "قطره آهن", "شربت سرفه")
    }
  val cardSurface = manoCardSurface()
  val borderColor = manoBorderColor()
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val coralAccent = manoSemanticAccent(CoralCare, DarkCoral)
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      state = rememberLazyListState(initialFirstVisibleItemIndex = if (openTemperatureEntry) 1 else 0),
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).imePadding().testTag("medicines_list"),
      contentPadding = ManoTheme.spacing.screenContentPadding,
      verticalArrangement = Arrangement.spacedBy(ManoTheme.spacing.sectionGap),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              ManoFramedIllustration(
                resId = R.drawable.img_mano_orbit_hero,
                contentDescription = "ربات مانو کید برای ایمنی داروهای کودک",
                width = 90.dp,
                aspectRatio = 1.0f,
                accentColor = MaterialTheme.colorScheme.primary,
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LtrText(text = "MANO Safety", color = MaterialTheme.colorScheme.primary)
                Text("داروهای کودک و ایمنی مصرف", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = "پایگاه دانش داروها، بررسی هشدارهای دارویی، مقایسه علمی محصولات سلامت و سرنگ mL.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = { activeSubTab = MedicinesSubTab.SAFETY_CHECKLIST },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("medicines_open_checklist_btn"),
              ) {
                Icon(
                  imageVector = if (checklistProgress.isAllCriticalReady) Icons.Rounded.CheckCircle else Icons.Rounded.Checklist,
                  contentDescription = null,
                  tint = if (checklistProgress.isAllCriticalReady) successAccent else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "چک‌لیست ایمنی (${checklistProgress.checkedCount}/${checklistProgress.totalCount})",
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = { activeSubTab = MedicinesSubTab.PRODUCT_COMPARISON },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("medicines_open_comparison_btn"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "مقایسه علمی محصولات",
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          // Mode Switcher FlowRow (all tabs always composed & accessible without horizontal clipping)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().testTag("medicines_mode_tabs"),
          ) {
            MedicinesSubTab.entries.forEach { tab ->
              val icon =
                when (tab) {
                  MedicinesSubTab.KNOWLEDGE_BASE -> Icons.AutoMirrored.Rounded.MenuBook
                  MedicinesSubTab.SYRINGE_DOSE_LAB -> Icons.Rounded.Vaccines
                  MedicinesSubTab.OVERLAP_CHECKER -> Icons.Rounded.Shield
                  MedicinesSubTab.PRODUCT_COMPARISON -> Icons.AutoMirrored.Rounded.CompareArrows
                  MedicinesSubTab.DOSE_LOGBOOK -> Icons.Rounded.EditNote
                  MedicinesSubTab.SAFETY_CHECKLIST -> Icons.Rounded.Checklist
                }
              FilterChip(
                selected = activeSubTab == tab,
                onClick = { activeSubTab = tab },
                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(tab.labelFa, style = MaterialTheme.typography.labelMedium, fontWeight = if (activeSubTab == tab) FontWeight.Bold else FontWeight.Normal) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    containerColor = cardSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                  ),
                modifier = Modifier.minimumInteractiveComponentSize().testTag(tab.tag),
              )
            }
          }
        }
      }

      when (activeSubTab) {
        MedicinesSubTab.KNOWLEDGE_BASE -> {
          item {
            SeverityAlert(
              severity = AlertSeverity.CAUTION,
              title = "شکل و غلظت دارو را بررسی کنید",
              body =
                "پیش از استفاده از راهنمای دوز یا تب‌بر، حتماً نام دارو، غلظت روی بطری (mg/mL) و وزن واقعی کودک را بررسی و با پزشک یا داروساز تطبیق دهید.",
              actionLabel = "مکمل‌ها و بهداشت کودک",
              onAction = onNavigateDevices,
            )
          }

          item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("جست‌وجوی نام دارو (مثلاً استامینوفن، آموکسی‌سیلین، آهن)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                  if (query.isNotEmpty()) {
                    IconButton(
                      onClick = { onQueryChange("") },
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("medicines_search_clear"),
                    ) {
                      Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جست‌وجو")
                    }
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("medicines_search_input"),
              )

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("medicines_quick_chips"),
              ) {
                items(quickSearchSuggestions) { term ->
                  SuggestionChip(
                    onClick = { onQueryChange(term) },
                    label = { Text(term, style = MaterialTheme.typography.labelMedium) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.testTag("quick_med_chip_$term"),
                  )
                }
              }

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("medicines_category_chips"),
              ) {
                item {
                  FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("همه داروها") },
                    colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                    modifier = Modifier.testTag("med_cat_all"),
                  )
                }
                items(MedicationSafetyCategory.entries) { cat ->
                  FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategorySelect(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat.labelFa) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                    modifier = Modifier.testTag("med_cat_${cat.id}"),
                  )
                }
              }

              Text(
                text =
                  if (query.isBlank() && selectedCategory == null) {
                    "${medications.size} راهنمای ایمنی دارو در پایگاه دانش"
                  } else {
                    "${medications.size} نتیجه یافت شد"
                  },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (medications.isEmpty()) {
            item {
              com.example.ui.components.ManoSectionEmptyStateCard(
                domain = com.example.ui.components.ManoEmptyStateDomain.MEDICINES,
                titleFa = "دارویی با این عبارت پیدا نشد",
                subtitleFa = "عبارت جست‌وجو یا فیلتر دسته‌بندی را پاک کنید یا سوال دارویی خود را برای داروساز بفرستید.",
                nextStepsFa = listOf(
                  "فیلتر جست‌وجو را پاک کنید تا همه راهنماهای دارویی نمایش داده شوند.",
                  "نام ماده مؤثر (مثل استامینوفن یا ایبوپروفن) را امتحان کنید یا از داروساز بپرسید.",
                ),
                primaryActionLabelFa = "نمایش همه داروها",
                onPrimaryAction = {
                  onQueryChange("")
                  onCategorySelect(null)
                },
                secondaryActionLabelFa = "پرسش از داروساز",
                onSecondaryAction = onNavigateAskPharmacist,
              )
            }
          } else {
            items(medications, key = { it.slug }) { med ->
              MedicineCard(medication = med, onOpen = { onOpenMedication(med.slug) })
            }
          }
        }

        MedicinesSubTab.SYRINGE_DOSE_LAB -> {
          if (!checklistProgress.isAllCriticalReady) {
            item {
              Surface(
                color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.55f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("syringe_checklist_gate_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Shield,
                      contentDescription = null,
                      tint = cautionAccent,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "چک‌لیست ایمنی پیش از دارو: ${checklistProgress.checkedCount}/${checklistProgress.totalCount}",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = cautionAccent,
                    )
                  }
                  OutlinedButton(
                    onClick = { activeSubTab = MedicinesSubTab.SAFETY_CHECKLIST },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("syringe_open_checklist_btn"),
                  ) {
                    Text("بررسی چک‌لیست", maxLines = 2)
                  }
                }
              }
            }
          }
          item {
            InteractiveOralSyringeLabCard(
              childProfile = childProfile,
              childBirthDateEpochMs = childBirthDateEpochMs,
              onOpen3DWithDose = onOpen3DWithDose,
              onLogRecommendedDose = { medFormName, mlText ->
                volumeMlInput = mlText
                dosageFormInput = medFormName
                logFormError = null
                activeSubTab = MedicinesSubTab.DOSE_LOGBOOK
              },
            )
          }
        }

        MedicinesSubTab.OVERLAP_CHECKER -> {
          if (catalog.isNotEmpty()) {
            val medA = catalog[firstMedIndex.coerceIn(catalog.indices)]
            val medB = catalog[secondMedIndex.coerceIn(catalog.indices)]
            val result = MedicationOverlapEngine.evaluatePair(medA, medB)

            item {
              SectionCard(modifier = Modifier.testTag("overlap_checker_card")) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Box(
                    modifier =
                      Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(coralAccent)
                  )
                  Text("بررسی هم‌پوشانی و تداخل دو دارو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                  text = "دو دارو را انتخاب کنید تا هشدارهای تکراری (مثل استامینوفن پنهان در شربت سرماخوردگی) یا فاصله زمانی ایمن را ببینید.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("داروی اول:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  catalog.forEachIndexed { idx, med ->
                    FilterChip(
                      selected = firstMedIndex == idx,
                      onClick = { firstMedIndex = idx },
                      label = { Text(med.nameFa.substringBefore(" ("), style = MaterialTheme.typography.labelMedium) },
                      colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                      modifier = Modifier.testTag("overlap_first_${med.slug}"),
                    )
                  }
                }

                HorizontalDivider(color = borderColor)

                Text("داروی دوم:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  catalog.forEachIndexed { idx, med ->
                    FilterChip(
                      selected = secondMedIndex == idx,
                      onClick = { secondMedIndex = idx },
                      label = { Text(med.nameFa.substringBefore(" ("), style = MaterialTheme.typography.labelMedium) },
                      colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                      modifier = Modifier.testTag("overlap_second_${med.slug}"),
                    )
                  }
                }
              }
            }

            item {
              SeverityAlert(
                severity = result.severity,
                title = result.titleFa,
                body = result.summaryFa + "\n\n" + result.actionStepsFa.joinToString("\n• ", prefix = "• "),
                showCautionMascot = result.severity == AlertSeverity.DANGER,
                modifier = Modifier.testTag("overlap_result_alert"),
              )
            }
          }
        }

        MedicinesSubTab.PRODUCT_COMPARISON -> {
          item {
            ScientificHealthProductComparisonSection()
          }
        }

        MedicinesSubTab.DOSE_LOGBOOK -> {
          item {
            FeverDoseTimelineCard(
              focusTemperatureEntry = focusTemperatureEntry,
              onTemperatureEntryFocused = { focusTemperatureEntry = false },
              snapshot = timelineSnapshot,
              childProfile = childProfile,
              isCloudSynced = currentUser != null,
              onSaveTempLog = onAddFeverLog,
              onDeleteTempLog = onDeleteFeverLog,
              onDeleteDoseLog = onDeleteDoseLog,
              onOpenUrgentDoctorGuide = onOpenUrgentDoctorGuide,
              selectedSymptomFlow = selectedSymptomFlow,
              savedVisitDraftsCount = savedVisitDraftsCount,
              onOpenVisitDraftStudio = onOpenVisitDraftStudio,
            )
          }

          item {
            val selectedMed = catalog.firstOrNull { it.slug == selectedLogMedSlug } ?: catalog.firstOrNull()
            SectionCard(modifier = Modifier.testTag("dose_log_form_card")) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                  modifier =
                    Modifier
                      .width(4.dp)
                      .height(26.dp)
                      .clip(RoundedCornerShape(50))
                      .background(sageAccent)
                )
                Text("ثبت نوبت مصرف دارو (برای نمودار و جلوگیری از دوز تکراری)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }

              Text("انتخاب دارو:", style = MaterialTheme.typography.labelLarge)
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(catalog, key = { it.slug }) { med ->
                  FilterChip(
                    selected = selectedLogMedSlug == med.slug,
                    onClick = { selectedLogMedSlug = med.slug },
                    label = { Text(med.nameFa.substringBefore(" (")) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                  )
                }
              }

              OutlinedTextField(
                value = volumeMlInput,
                onValueChange = {
                  volumeMlInput = it
                  logFormError = null
                },
                label = { Text("مقدار داده‌شده توسط والد (مثلاً 2.5 mL یا ۱۰ قطره)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("dose_log_volume_input"),
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                  value = dosageFormInput,
                  onValueChange = { dosageFormInput = it },
                  label = { Text("شکل دارو") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("dose_log_form_input"),
                )
                OutlinedTextField(
                  value = timeLabelInput,
                  onValueChange = { timeLabelInput = it },
                  label = { Text("ساعت مصرف") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("dose_log_time_input"),
                )
              }

              OutlinedTextField(
                value = logNoteInput,
                onValueChange = { logNoteInput = it },
                label = { Text("توضیح کوتاه یا نام والد (مثلاً با هماهنگی مادر)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("dose_log_note_input"),
              )

              if (logFormError != null) {
                Text(logFormError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
              }

              Surface(
                color =
                  if (checklistProgress.isAllCriticalReady) manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.50f, darkAlpha = 0.16f)
                  else manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.55f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border =
                  BorderStroke(
                    1.dp,
                    if (checklistProgress.isAllCriticalReady) successAccent.copy(alpha = 0.35f)
                    else cautionAccent.copy(alpha = 0.35f),
                  ),
                modifier = Modifier.fillMaxWidth().testTag("dose_log_checklist_gate_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = if (checklistProgress.isAllCriticalReady) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                      contentDescription = null,
                      tint = if (checklistProgress.isAllCriticalReady) successAccent else cautionAccent,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "چک‌لیست ایمنی پیش از دارو: ${checklistProgress.checkedCount}/${checklistProgress.totalCount}",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = if (checklistProgress.isAllCriticalReady) successAccent else cautionAccent,
                    )
                  }
                  OutlinedButton(
                    onClick = { activeSubTab = MedicinesSubTab.SAFETY_CHECKLIST },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("dose_log_open_checklist_btn"),
                  ) {
                    Text(if (checklistProgress.isAllCriticalReady) "مرور" else "بررسی چک‌لیست", maxLines = 2)
                  }
                }
              }

              Button(
                onClick = {
                  val vol = volumeMlInput.trim()
                  if (vol.isBlank()) {
                    logFormError = "لطفاً مقدار mL یا قطره داده‌شده را وارد کنید."
                  } else if (timeLabelInput.isNotBlank() && timeLabelInput.trim() != "امروز" &&
                    FeverDoseTimelineEngine.resolveDoseTimeMs(timeLabelInput) == null) {
                    logFormError = "ساعت مصرف را به شکل معتبر HH:mm در ۲۴ ساعت اخیر وارد کنید؛ ساعت نامعتبر مبنای هشدار دوز نیست."
                  } else {
                    val medName = selectedMed?.nameFa ?: "داروی ثبت‌شده"
                    val timeNow = FeverDoseTimelineEngine.formatHourMinute(System.currentTimeMillis())
                    onAddDoseLog(
                      selectedLogMedSlug,
                      medName,
                      dosageFormInput.trim().ifEmpty { "داروی خوراکی" },
                      vol,
                      if (timeLabelInput.trim() == "امروز" || timeLabelInput.isBlank()) timeNow else timeLabelInput.trim(),
                      logNoteInput.trim(),
                    ) {
                      volumeMlInput = ""
                      logNoteInput = ""
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("dose_log_save_btn"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (currentUser != null) "ثبت نوبت و همگام‌سازی ابری" else "ثبت نوبت در خط زمانی نشست")
              }
            }
          }

          if (currentUser != null && doseLogsState is CloudDataState.Loading) {
            item {
              Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                ManoTurquoiseLoadingRing(
                  size = ManoCircularProgressSize.MEDIUM,
                  contentDescriptionFa = "در حال بارگذاری دفترچه ابری دارو",
                )
              }
            }
          } else if (currentUser != null && doseLogsState is CloudDataState.Error) {
            item {
              SeverityAlert(
                severity = AlertSeverity.DANGER,
                title = "خطا در بارگذاری دفترچه ابری",
                body = doseLogsState.message,
              )
            }
          } else {
            val timelineDoses = timelineSnapshot.dosePoints.reversed()
            if (timelineDoses.isEmpty()) {
              item {
                EmptyState(
                  title = "هنوز نوبت مصرفی در ۲۴ ساعت اخیر ثبت نشده است",
                  subtitle = "با ثبت هر نوبت، شمارش معکوس فاصله ایمن ۴ تا ۶ ساعته به صورت خودکار فعال می‌شود.",
                  pose = MascotPose.SAVED,
                )
              }
            } else {
              items(timelineDoses, key = { it.id }) { log ->
                SectionCard(modifier = Modifier.testTag("dose_log_item_${log.id}")) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(log.medicationNameFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                      Text(
                        text = "${log.volumeMlText} • ${log.dosageFormFa} • ساعت ${log.givenAtLabelFa}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      if (log.noteFa.isNotBlank()) {
                        Text(log.noteFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                    IconButton(
                      onClick = { onDeleteDoseLog(log.id, log.isSyncedCloud) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف رکورد مصرف", tint = MaterialTheme.colorScheme.error)
                    }
                  }
                }
              }
            }
          }

          item {
            CloudSyncAuthCard(
              currentUser = currentUser,
              titleFa = "همگام‌سازی زنده مادر و پدر در فضای ابری",
              descriptionFa = "برای همگام‌سازی خودکار تب و نوبت‌های دارو بین دو گوشی والدین و حفظ دائمی گزارش، با حساب گوگل وارد شوید.",
              onAuthSuccess = onCloudAuthSuccess,
              onAuthError = onCloudAuthError,
              onSignedOut = onSignedOut,
            )
          }
        }

        MedicinesSubTab.SAFETY_CHECKLIST -> {
          item {
            MedicationChecklistHeaderCard(
              checkedIds = checkedMedSafetyIds,
              childProfile = childProfile,
              timelineSnapshot = timelineSnapshot,
            )
          }
          MedicationCheckCategory.entries.forEach { category ->
            item(key = "cat_header_${category.id}") {
              MedicationChecklistCategoryHeader(
                category = category,
                checkedIds = checkedMedSafetyIds,
              )
            }
            val categorySteps = MedicationSafetyChecklistCatalog.items.filter { it.category == category }
            items(categorySteps, key = { it.id }) { step ->
              MedicationChecklistStepCard(
                item = step,
                checked = step.id in checkedMedSafetyIds,
                onToggle = { onToggleMedSafetyCheck(step.id) },
                onOpenDoseLogbook = { activeSubTab = MedicinesSubTab.DOSE_LOGBOOK },
                onOpenSyringeLab = { activeSubTab = MedicinesSubTab.SYRINGE_DOSE_LAB },
                onOpenOverlapChecker = { activeSubTab = MedicinesSubTab.OVERLAP_CHECKER },
                expanded = step.id in expandedChecklistTipIds,
                onToggleExpanded = {
                  expandedChecklistTipIds =
                    if (step.id in expandedChecklistTipIds) expandedChecklistTipIds - step.id
                    else expandedChecklistTipIds + step.id
                },
              )
            }
          }
          item {
            MedicationChecklistActionsCard(
              checkedIds = checkedMedSafetyIds,
              onResetChecklist = onResetMedSafetyChecklist,
              onOpenDoseLogbook = { activeSubTab = MedicinesSubTab.DOSE_LOGBOOK },
              onOpenSyringeLab = { activeSubTab = MedicinesSubTab.SYRINGE_DOSE_LAB },
              onOpenOverlapChecker = { activeSubTab = MedicinesSubTab.OVERLAP_CHECKER },
            )
          }
        }
      }

      item {
        PharmacistCtaCard(
          title = "پرسش درباره غلظت یا تداخل دارویی",
          onClick = onNavigateAskPharmacist,
        )
      }
    }
  }
}

@Composable
fun MedicineDetailScreen(
  medication: MedicationMonograph?,
  onBack: () -> Unit,
  onNavigateAskPharmacist: () -> Unit,
  onShowFeedback: (String) -> Unit = {},
) {
  if (medication == null) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
      EmptyState(title = "کارت دارو یافت نشد", subtitle = "به فهرست داروها برگردید.", actionLabel = "بازگشت", onAction = onBack)
    }
    return
  }

  val context = LocalContext.current
  val canonicalUrl = ManoDomainLinks.medicineUrl(medication.slug)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("medicine_detail_list"),
      contentPadding = ManoTheme.spacing.screenContentPadding,
      verticalArrangement = Arrangement.spacedBy(ManoTheme.spacing.sectionGap),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("بازگشت")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = medication.category.labelFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            SourceBadge(reviewStatus = medication.review.status)
            IconButton(
              onClick = {
                val shareBody = ManoDomainLinks.buildMedicineShareText(medication)
                ManoLinkShareHelper.shareTextWithLink(context, medication.nameFa, shareBody)
                  .onFailure {
                    ManoLinkShareHelper.copyToClipboard(context, medication.nameFa, canonicalUrl)
                    onShowFeedback("لینک کارت دارو در حافظه کپی شد.")
                  }
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("medicine_share_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = "اشتراک‌گذاری لینک دارو",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(medication.nameFa, style = MaterialTheme.typography.headlineMedium)
          LtrText(text = medication.genericNameEn, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Surface(
            color = manoCardSurface(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, manoBorderColor()),
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable {
                  ManoLinkShareHelper.copyToClipboard(context, medication.nameFa, canonicalUrl)
                  onShowFeedback("لینک مستقیم کارت دارو کپی شد.")
                }
                .minimumInteractiveComponentSize()
                .testTag("medicine_domain_link_pill"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Box(modifier = Modifier.weight(1f)) {
                LtrText(text = canonicalUrl.removePrefix("https://"), color = MaterialTheme.colorScheme.primary)
              }
              Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی لینک دارو", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.CAUTION,
          title = "هشدار شکل و غلظت دارو",
          body = medication.dosageFormAlertFa,
          modifier = Modifier.testTag("medicine_detail_concentration_alert"),
        )
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "چه مواردی را بدون ارزیابی پزشک نباید داد؟ (هشدار مصرف)",
          body = medication.whatNotToGiveFa.joinToString("\n\n• ", prefix = "• "),
          showCautionMascot = true,
          modifier = Modifier.testTag("medicine_detail_warnings"),
        )
      }

      item {
        SectionCard {
          Text("کاربرد و حداقل سن برچسب", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          ClinicalBulletList(
            items = listOf(
              "کاربرد: ${medication.treatsFa}",
              "حداقل سن: ${medication.labeledMinimumAgeFa}",
            ),
            bulletColor = MaterialTheme.colorScheme.primary,
          )
        }
      }

      item {
        SectionCard(modifier = Modifier.testTag("medicine_detail_precautions")) {
          Text("اصول زمان‌بندی، احتیاط‌ها و عوارض شایع", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          ClinicalBulletList(
            items = medication.timingAndPrecautionsFa,
            bulletColor = MaterialTheme.colorScheme.primary,
          )
          Text("تداخلات مهم:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
          ClinicalBulletList(
            items = medication.interactionCautionsFa,
            bulletColor = manoSemanticAccent(CautionAmber, DarkGold),
          )
          Text("عوارض شایع:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
          ClinicalBulletList(
            items = medication.commonSideEffectsFa,
            bulletColor = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      if (medication.storageAndShelfLifeFa.isNotBlank()) {
        item {
          SectionCard(modifier = Modifier.testTag("medicine_detail_storage")) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("شرایط نگهداری و مصرف پس از باز شدن", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(medication.storageAndShelfLifeFa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      item {
        SectionCard {
          Text("نکات کاربردی هنگام امتناع کودک و زمان مراجعه", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          ClinicalBulletList(
            items = medication.refusalTipsFa,
            bulletColor = MaterialTheme.colorScheme.primary,
          )
          Text("چه زمانی مشاوره فوری لازم است:", style = MaterialTheme.typography.titleSmall, color = manoSemanticAccent(CoralCare, DarkCoral))
          ClinicalBulletList(
            items = medication.whenToSeekAdviceFa,
            bulletColor = manoSemanticAccent(CoralCare, DarkCoral),
          )
        }
      }

      // Locked Educational Dosing-Workflow Preview
      item {
        val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
        Surface(
          color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.7f, darkAlpha = 0.16f),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth().testTag("locked_dose_preview"),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = cautionAccent)
              Text(
                text = "شرایط و الزامات محاسبه ایمن دوز (راهنمای آموزشی)",
                style = MaterialTheme.typography.titleMedium,
                color = cautionAccent,
              )
            }
            Text(
              text = "برای پیشگیری از خطای دوز و غلظت، پیش از استفاده از مقادیر مرجع، بررسی موارد زیر الزامی است:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            medication.lockedDoseRule.requiredInputsFa.forEach { req ->
              Text("✓ $req", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("موارد نیازمند احتیاط والد:", style = MaterialTheme.typography.labelLarge, color = cautionAccent)
            ClinicalBulletList(
              items = medication.lockedDoseRule.blockingReasonsFa,
              bulletColor = cautionAccent,
            )
          }
        }
      }

      item {
        PharmacistCtaCard(
          title = "تطبیق غلظت این دارو با داروساز",
          onClick = onNavigateAskPharmacist,
        )
      }

      item {
        ClinicalNote(
          title = "منابع و وضعیت بازبینی",
          items = listOf("وضعیت: ${medication.review.status.labelFa}", medication.review.notes),
          sources = medication.sources,
          reviewStatus = medication.review.status,
        )
      }
    }
  }
}
