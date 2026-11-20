package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.FeverLogCloud
import com.example.data.FirebaseFunctionsConsultationGateway
import com.example.data.ManoCloudRepository
import com.example.data.ManoThemeMode
import com.example.data.MedicationDoseLogCloud
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.MascotPose
import com.example.domain.model.TopicCategory
import com.example.domain.model.VaccineRecord
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.HighFeverClinicalAlert
import com.example.domain.util.ManoDeepLinkParser
import com.example.domain.util.PersianNormalizer
import com.example.domain.util.VaccinationScheduleEngine
import com.example.notifications.HighFeverNotificationHelper
import com.example.notifications.VaccineNotificationHelper
import com.example.notifications.VaccineReminderScheduler
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.components.AlertSeverity
import com.example.ui.components.EmptyState
import com.example.ui.components.HighFeverUrgentAlertDialog
import com.example.ui.components.ManoAlgorithmicNavIcon
import com.example.ui.components.ManoAtmosphericBackdrop
import com.example.ui.components.ManoBrandCrest
import com.example.ui.components.ManoIconMedallion
import com.example.ui.components.ManoKidWordmark
import com.example.ui.components.ManoOnboardingOverlay
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.SeverityAlert
import com.example.ui.components.TopicAccentRole
import com.example.ui.components.VisitDraftModalBottomSheet
import com.example.ui.components.resolveAccentColor
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MangoCare
import com.example.ui.theme.PlumCare
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SkyCare
import com.example.ui.navigation.AskPharmacistRoute
import com.example.ui.navigation.DevicesRoute
import com.example.ui.navigation.FaqRoute
import com.example.ui.navigation.GrowthRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.InfoRoute
import com.example.ui.navigation.MedicineDetailRoute
import com.example.ui.navigation.MedicinesRoute
import com.example.ui.navigation.MyChildRoute
import com.example.ui.navigation.NutritionRoute
import com.example.ui.navigation.Pediatric3DStudioRoute
import com.example.ui.navigation.SkinRoute
import com.example.ui.navigation.SymptomsRoute
import com.example.ui.navigation.SyrupScannerRoute
import com.example.ui.navigation.TopicDetailRoute
import com.example.ui.navigation.TopicsRoute
import com.example.ui.navigation.VaccinesRoute
import com.example.ui.screens.AskPharmacistScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.GrowthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InfoScreen
import com.example.ui.screens.MedicineDetailScreen
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.MyChildScreen
import com.example.ui.screens.NutritionAdvisorScreen
import com.example.ui.screens.Pediatric3DStudioScreen
import com.example.ui.screens.SkinScreen
import com.example.ui.screens.SymptomNavigatorScreen
import com.example.ui.screens.SyrupScannerScreen
import com.example.ui.screens.TopicDetailScreen
import com.example.ui.screens.TopicsScreen
import com.example.ui.screens.VaccinesScreen
import com.example.ui.viewmodel.CloudDataState
import com.example.ui.viewmodel.ManoCloudSyncViewModel
import com.example.ui.viewmodel.ManoNutritionViewModel
import com.example.ui.viewmodel.ManoSafetyTriageAiViewModel
import com.example.ui.viewmodel.ManoSyrupScannerViewModel
import com.example.ui.viewmodel.ManoViewModel
import com.example.ui.viewmodel.ManoVisitDraftViewModel
import com.example.widget.FeverTimelineWidgetStore
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private data class NavTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val route: Any,
  val tag: String,
  val accentRole: TopicAccentRole,
)

private data class DrawerNavItem(
  val label: String,
  val icon: ImageVector,
  val route: Any,
  val accentRole: TopicAccentRole,
  val iconKind: PediatricIconKind? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManoApp(
  viewModel: ManoViewModel = viewModel(),
  openTimelineOnLaunch: Boolean = false,
  onConsumedTimelineLaunch: () -> Unit = {},
  openVaccinesOnLaunch: Boolean = false,
  onConsumedVaccinesLaunch: () -> Unit = {},
  pendingDeepLinkUri: String? = null,
  onConsumedDeepLinkUri: () -> Unit = {},
  forceShowOnboarding: Boolean = false,
) {
  val context = LocalContext.current
  val auth =
    remember(context) {
      runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
          FirebaseApp.initializeApp(context)
        }
        Firebase.auth
      }.getOrNull()
    }
  val currentUser: FirebaseUser? by
    (auth?.authStateFlow() ?: remember { flowOf<FirebaseUser?>(null) })
      .collectAsStateWithLifecycle(initialValue = auth?.currentUser)

  val navController = rememberNavController()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val localChildProfile by viewModel.localChildProfile.collectAsStateWithLifecycle()
  val localFeverLogs by viewModel.localFeverLogs.collectAsStateWithLifecycle()
  val localDoseLogs by viewModel.localDoseLogs.collectAsStateWithLifecycle()
  val checkedMilestoneIds by viewModel.checkedMilestoneIds.collectAsStateWithLifecycle()
  val localBookmarkedSlugs by viewModel.bookmarkedSlugs.collectAsStateWithLifecycle()
  val selectedAgeBand by viewModel.selectedAgeBand.collectAsStateWithLifecycle()
  val localVisitDrafts by viewModel.localVisitDrafts.collectAsStateWithLifecycle()
  val savedNutritionPlans by viewModel.savedNutritionPlans.collectAsStateWithLifecycle()
  val childHeightCm by viewModel.childHeightCm.collectAsStateWithLifecycle()
  val checkedMedSafetyIds by viewModel.checkedMedSafetyIds.collectAsStateWithLifecycle()
  val childBirthDateEpochMs by viewModel.childBirthDateEpochMs.collectAsStateWithLifecycle()
  val localVaccineRecords by viewModel.localVaccineRecords.collectAsStateWithLifecycle()
  val vaccineReminderSettings by viewModel.vaccineReminderSettings.collectAsStateWithLifecycle()
  val notifiedVaccineReminderKeys by viewModel.notifiedVaccineReminderKeys.collectAsStateWithLifecycle()
  val savedGalacticSelection by viewModel.savedGalacticSelection.collectAsStateWithLifecycle()
  val localSyrupScans by viewModel.savedSyrupScans.collectAsStateWithLifecycle()
  val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
  val hapticIntensity by viewModel.hapticIntensity.collectAsStateWithLifecycle()
  val webGlQuality by viewModel.webGlQuality.collectAsStateWithLifecycle()
  val visitDraftVm: ManoVisitDraftViewModel = viewModel()
  val visitDraftUiState by visitDraftVm.uiState.collectAsStateWithLifecycle()
  val visitDraftPdfState by visitDraftVm.pdfState.collectAsStateWithLifecycle()
  val nutritionVm: ManoNutritionViewModel = viewModel()
  val nutritionUiState by nutritionVm.uiState.collectAsStateWithLifecycle()
  val syrupScannerVm: ManoSyrupScannerViewModel = viewModel()
  val syrupScannerUiState by syrupScannerVm.uiState.collectAsStateWithLifecycle()
  val safetyTriageAiVm: ManoSafetyTriageAiViewModel = viewModel()
  val safetyChecklistUiState by safetyTriageAiVm.checklistState.collectAsStateWithLifecycle()
  val documentedQaUiState by safetyTriageAiVm.qaState.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var showGlobalSearchSheet by remember { mutableStateOf(false) }
  var showVisitDraftSheet by remember { mutableStateOf(false) }
  var manualOnboardingOpen by remember { mutableStateOf(forceShowOnboarding) }
  var dismissedAutoOnboarding by remember { mutableStateOf(false) }
  val isRobolectricRuntime = remember { Build.FINGERPRINT.equals("robolectric", ignoreCase = true) }

  LaunchedEffect(auth) {
    if (auth == null) return@LaunchedEffect
    val credentialManager = CredentialManager.create(context)
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {},
      onUnauthenticated = {},
      scope = scope,
    )
  }

  // Strictly auth-gated Cloud Sync ViewModel: only instantiated and observed while currentUser != null
  val cloudVm: ManoCloudSyncViewModel? =
    if (auth != null && currentUser?.uid != null) {
      val uid = currentUser!!.uid
      key(uid) {
        viewModel(
          key = uid,
          factory =
            viewModelFactory {
              initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val functionsGateway =
                  runCatching { FirebaseFunctionsConsultationGateway(FirebaseFunctions.getInstance("us-central1")) }.getOrNull()
                ManoCloudSyncViewModel(ManoCloudRepository(db, auth, functionsGateway), uid)
              }
            },
        )
      }
    } else {
      null
    }

  val cloudToast by cloudVm?.cloudToastFa?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
  val cloudProfileState by cloudVm?.childProfileState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<ChildProfileDemo?>>(CloudDataState.Success(null)) }
  val cloudDoseLogsState by cloudVm?.doseLogsState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<List<MedicationDoseLogCloud>>>(CloudDataState.Success(emptyList())) }
  val cloudFeverLogsState by cloudVm?.feverLogsState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<List<FeverLogCloud>>>(CloudDataState.Success(emptyList())) }
  val cloudConsultationsState by cloudVm?.consultationsState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<List<com.example.domain.model.ConsultationRequestDemo>>>(CloudDataState.Success(emptyList())) }
  val serverConsultations by cloudVm?.serverConsultations?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(emptyList()) }
  val acceptingPharmacies by cloudVm?.acceptingPharmacies?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(emptyList()) }
  val selectedPharmacyId by cloudVm?.selectedPharmacyId?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(null) }
  val isSubmittingConsultation by cloudVm?.isSubmittingConsultation?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(false) }
  val serverConsultationErrorFa by cloudVm?.serverConsultationErrorFa?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(null) }
  val staffAccessStatus by cloudVm?.staffAccessStatus?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(com.example.domain.model.StaffAccessStatus()) }
  val staffInboxCases by cloudVm?.staffInboxCases?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(emptyList()) }
  val isLoadingStaffInbox by cloudVm?.isLoadingStaffInbox?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(false) }
  val staffActionInFlightCaseId by cloudVm?.staffActionInFlightCaseId?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(null) }
  val staffErrorFa by cloudVm?.staffErrorFa?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(null) }
  val cloudBookmarks by cloudVm?.bookmarkedSlugsState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(emptySet<String>()) }
  val cloudVisitDraftsState by cloudVm?.visitDraftsState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<List<com.example.domain.model.DoctorVisitDraft>>>(CloudDataState.Success(emptyList())) }
  val cloudVaccineRecordsState by cloudVm?.vaccineRecordsState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<List<VaccineRecord>>>(CloudDataState.Success(emptyList())) }
  val cloudSyrupScansState by cloudVm?.syrupScansState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf<CloudDataState<List<com.example.domain.model.SavedSyrupScan>>>(CloudDataState.Success(emptyList())) }

  val effectiveBookmarks = if (currentUser != null) (localBookmarkedSlugs + cloudBookmarks) else localBookmarkedSlugs
  val effectiveChildProfile =
    if (currentUser != null) (cloudProfileState as? CloudDataState.Success)?.data else localChildProfile
  val effectiveConsultations =
    if (currentUser != null) {
      val legacyList = (cloudConsultationsState as? CloudDataState.Success)?.data ?: emptyList()
      val merged = (serverConsultations + legacyList).distinctBy { it.id }
      uiState.askPharmacist.copy(requests = merged)
    } else {
      uiState.askPharmacist
    }

  val activeCloudDoseLogs =
    if (currentUser != null) (cloudDoseLogsState as? CloudDataState.Success)?.data ?: emptyList() else emptyList()
  val activeCloudFeverLogs =
    if (currentUser != null) (cloudFeverLogsState as? CloudDataState.Success)?.data ?: emptyList() else emptyList()
  val activeCloudVisitDrafts =
    if (currentUser != null) (cloudVisitDraftsState as? CloudDataState.Success)?.data ?: emptyList() else emptyList()
  val activeCloudVaccineRecords =
    if (currentUser != null) (cloudVaccineRecordsState as? CloudDataState.Success)?.data ?: emptyList() else emptyList()
  val activeCloudSyrupScans =
    if (currentUser != null) (cloudSyrupScansState as? CloudDataState.Success)?.data ?: emptyList() else emptyList()
  val effectiveSyrupScans =
    remember(activeCloudSyrupScans, localSyrupScans) {
      (activeCloudSyrupScans + localSyrupScans)
        .distinctBy { it.id }
        .sortedByDescending { it.updatedAtMs }
    }
  LaunchedEffect(currentUser?.uid, cloudVaccineRecordsState) {
    val activeState = cloudVaccineRecordsState
    if (currentUser != null && activeState is CloudDataState.Success) {
      viewModel.mirrorCloudVaccineRecords(activeState.data)
    }
  }
  LaunchedEffect(currentUser?.uid, cloudSyrupScansState) {
    val activeState = cloudSyrupScansState
    if (currentUser != null && activeState is CloudDataState.Success) {
      viewModel.mirrorCloudSyrupScans(activeState.data)
    }
  }
  LaunchedEffect(currentUser?.uid, cloudProfileState, localChildProfile, localFeverLogs, localDoseLogs) {
    val profileState = cloudProfileState
    val activeCloudVm = cloudVm
    if (currentUser != null && activeCloudVm != null && profileState is CloudDataState.Success) {
      activeCloudVm.migrateOfflineDataIfNeeded(
        localProfile = localChildProfile,
        localFevers = localFeverLogs,
        localDoses = localDoseLogs,
        cloudHasProfile = profileState.data != null,
        onMigratedSuccess = { viewModel.clearLocalTimelineLogsAfterCloudSync() },
      )
    }
  }
  LaunchedEffect(effectiveChildProfile?.aliasFa) {
    viewModel.setChildAliasFa(effectiveChildProfile?.aliasFa)
  }
  val vaccineSummary =
    remember(activeCloudVaccineRecords, localVaccineRecords, childBirthDateEpochMs, vaccineReminderSettings) {
      VaccinationScheduleEngine.resolveSchedule(
        cloudRecords = activeCloudVaccineRecords,
        localRecords = localVaccineRecords,
        birthDateEpochMs = childBirthDateEpochMs,
        reminderSettings = vaccineReminderSettings,
      )
    }
  val effectiveSavedVisitDrafts =
    remember(activeCloudVisitDrafts, localVisitDrafts) {
      (activeCloudVisitDrafts + localVisitDrafts)
        .distinctBy { it.id }
        .sortedByDescending { it.createdAtMs }
    }
  val activeSymptomFlow =
    remember(uiState.symptomWizard.selectedSymptomId) {
      viewModel.allSymptomFlows.firstOrNull { it.id == uiState.symptomWizard.selectedSymptomId }
    }
  val timelineSnapshot =
    remember(activeCloudFeverLogs, localFeverLogs, activeCloudDoseLogs, localDoseLogs) {
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = activeCloudFeverLogs,
        localFeverLogs = localFeverLogs,
        cloudDoseLogs = activeCloudDoseLogs,
        localDoseLogs = localDoseLogs,
      )
    }

  var openMedicinesTimelineTab by remember { mutableStateOf(openTimelineOnLaunch) }
  var openMedicinesChecklistTab by remember { mutableStateOf(false) }
  var requestedMedicinesTabKey by remember { mutableStateOf<String?>(null) }

  val validArticleSlugs = remember(viewModel.allArticles) { viewModel.allArticles.map { it.slug }.toSet() }
  val validMedicationSlugs = remember(viewModel.allMedications) { viewModel.allMedications.map { it.slug }.toSet() }
  val validSymptomIds = remember(viewModel.allSymptomFlows) { viewModel.allSymptomFlows.map { it.id }.toSet() }

  fun handleDomainDeepLink(rawUri: String) {
    val resolution =
      ManoDeepLinkParser.resolve(
        rawUriString = rawUri,
        validArticleSlugs = validArticleSlugs,
        validMedicationSlugs = validMedicationSlugs,
        validSymptomIds = validSymptomIds,
      ) ?: return

    if (resolution.resetTopicFilters) {
      viewModel.setTopicSearchQuery(resolution.topicQuery.orEmpty())
      viewModel.setTopicCategory(
        resolution.topicCategoryId?.let { catId -> TopicCategory.entries.firstOrNull { it.id == catId } }
      )
    } else {
      resolution.topicQuery?.let { viewModel.setTopicSearchQuery(it) }
      resolution.topicCategoryId?.let { catId ->
        viewModel.setTopicCategory(TopicCategory.entries.firstOrNull { it.id == catId })
      }
    }
    if (resolution.resetFaqFilters) {
      viewModel.setFaqSearchQuery(resolution.faqQuery.orEmpty())
      viewModel.setFaqCategory(null)
    } else {
      resolution.faqQuery?.let { viewModel.setFaqSearchQuery(it) }
    }
    resolution.symptomId?.let { viewModel.startSymptomFlow(it) }
    resolution.medicineTabKey?.let { tabKey -> requestedMedicinesTabKey = tabKey }

    navController.navigate(resolution.route) { launchSingleTop = true }
    resolution.fallbackNoticeFa?.let { notice -> viewModel.showToast(notice) }
  }

  LaunchedEffect(openTimelineOnLaunch) {
    if (openTimelineOnLaunch) {
      openMedicinesTimelineTab = true
      navController.navigate(MedicinesRoute) { launchSingleTop = true }
      onConsumedTimelineLaunch()
    }
  }

  LaunchedEffect(openVaccinesOnLaunch) {
    if (openVaccinesOnLaunch) {
      navController.navigate(VaccinesRoute) { launchSingleTop = true }
      onConsumedVaccinesLaunch()
    }
  }

  LaunchedEffect(pendingDeepLinkUri) {
    val uri = pendingDeepLinkUri
    if (!uri.isNullOrBlank()) {
      handleDomainDeepLink(uri)
      onConsumedDeepLinkUri()
    }
  }

  LaunchedEffect(timelineSnapshot) {
    FeverTimelineWidgetStore.saveAndUpdateWidgets(context, timelineSnapshot)
  }

  var activeHighFeverDialog by remember { mutableStateOf<HighFeverClinicalAlert?>(null) }
  var lastTriggeredHighFeverKey by remember { mutableStateOf<String?>(null) }
  var hasNotificationPermission by remember {
    mutableStateOf(HighFeverNotificationHelper.hasNotificationPermission(context))
  }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted
      if (granted) {
        activeHighFeverDialog?.let { alert ->
          HighFeverNotificationHelper.postHighFeverAlertNotification(context, alert)
        }
        val pendingVaccines =
          VaccinationScheduleEngine.findUnnotifiedReminders(
            summary = vaccineSummary,
            settings = vaccineReminderSettings,
            alreadyNotifiedKeys = notifiedVaccineReminderKeys,
          )
        if (pendingVaccines.isNotEmpty()) {
          val posted =
            VaccineNotificationHelper.postVaccineReminderNotification(
              context = context,
              dueItems = pendingVaccines,
              childAliasFa = effectiveChildProfile?.aliasFa,
            )
          if (posted) {
            val keys = pendingVaccines.mapNotNull { VaccinationScheduleEngine.reminderKeyFor(it) }.toSet()
            viewModel.markVaccineReminderKeysNotified(keys)
          }
        }
        viewModel.showToast("اعلان‌های فوری و یادآوری واکسیناسیون فعال شد.")
      }
    }

  LaunchedEffect(vaccineSummary, vaccineReminderSettings, notifiedVaccineReminderKeys, hasNotificationPermission) {
    VaccineReminderScheduler.syncSchedule(
      context = context,
      summary = vaccineSummary,
      settings = vaccineReminderSettings,
      alreadyNotifiedKeys = notifiedVaccineReminderKeys,
      hasNotificationPermission = hasNotificationPermission,
    )
    if (hasNotificationPermission) {
      val pendingVaccines =
        VaccinationScheduleEngine.findUnnotifiedReminders(
          summary = vaccineSummary,
          settings = vaccineReminderSettings,
          alreadyNotifiedKeys = notifiedVaccineReminderKeys,
        )
      if (pendingVaccines.isNotEmpty()) {
        val posted =
          VaccineNotificationHelper.postVaccineReminderNotification(
            context = context,
            dueItems = pendingVaccines,
            childAliasFa = effectiveChildProfile?.aliasFa,
          )
        if (posted) {
          val keys = pendingVaccines.mapNotNull { VaccinationScheduleEngine.reminderKeyFor(it) }.toSet()
          viewModel.markVaccineReminderKeysNotified(keys)
        }
      }
    }
  }

  LaunchedEffect(timelineSnapshot.tempPoints, effectiveChildProfile) {
    val latestPoint = timelineSnapshot.tempPoints.lastOrNull()
    val alert = FeverDoseTimelineEngine.evaluateHighFeverAlert(timelineSnapshot, effectiveChildProfile)
    if (latestPoint != null && alert != null && latestPoint.tempCelsius > FeverDoseTimelineEngine.HIGH_FEVER_ALERT_THRESHOLD_C) {
      val eventKey = "${latestPoint.id}_${latestPoint.timestampMs}"
      if (eventKey != lastTriggeredHighFeverKey) {
        lastTriggeredHighFeverKey = eventKey
        activeHighFeverDialog = alert
        HighFeverNotificationHelper.postHighFeverAlertNotification(context, alert)
      }
    }
  }

  fun toggleBookmarkSynced(slug: String) {
    val isCurrentlyBookmarked = slug in effectiveBookmarks
    viewModel.toggleBookmark(slug)
    if (currentUser != null) {
      cloudVm?.setBookmark(slug, !isCurrentlyBookmarked)
    }
  }

  LaunchedEffect(uiState.toastMessageFa) {
    val msg = uiState.toastMessageFa
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  LaunchedEffect(cloudToast) {
    val msg = cloudToast
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      cloudVm?.clearCloudToast()
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = navBackStackEntry?.destination
  val isHome = currentDest?.hasRoute<HomeRoute>() != false

  val primaryTabs =
    remember {
      listOf(
        NavTab("خانه", Icons.Rounded.Home, Icons.Outlined.Home, HomeRoute, "nav_home", TopicAccentRole.TEAL),
        NavTab("علائم", Icons.Rounded.HealthAndSafety, Icons.Outlined.HealthAndSafety, SymptomsRoute(), "nav_symptoms", TopicAccentRole.CORAL),
        NavTab("داروها", Icons.Rounded.MedicalServices, Icons.Outlined.MedicalServices, MedicinesRoute, "nav_medicines", TopicAccentRole.SAGE),
        NavTab("رشد", Icons.Rounded.ChildCare, Icons.Outlined.ChildCare, GrowthRoute, "nav_growth", TopicAccentRole.SKY),
        NavTab("پرسش‌ها", Icons.AutoMirrored.Rounded.Help, Icons.AutoMirrored.Outlined.HelpOutline, FaqRoute(), "nav_faq", TopicAccentRole.PLUM),
      )
    }

  var homeScrollToTopTrigger by remember { mutableIntStateOf(0) }

  fun navigateTopLevel(route: Any) {
    if (route is HomeRoute) {
      homeScrollToTopTrigger++
      val poppedToHome = navController.popBackStack<HomeRoute>(inclusive = false)
      if (!poppedToHome) {
        navController.navigate(HomeRoute) {
          popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
          launchSingleTop = true
        }
      }
      return
    }
    val poppedWithinTab =
      when {
        route is MedicinesRoute &&
          (currentDest?.hasRoute<MedicineDetailRoute>() == true || currentDest?.hasRoute<SyrupScannerRoute>() == true) ->
          navController.popBackStack<MedicinesRoute>(inclusive = false)
        route is GrowthRoute &&
          (currentDest?.hasRoute<NutritionRoute>() == true || currentDest?.hasRoute<VaccinesRoute>() == true) ->
          navController.popBackStack<GrowthRoute>(inclusive = false)
        else -> false
      }
    if (!poppedWithinTab) {
      navController.navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
          val isDrawerDark = LocalManoDarkTheme.current
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
              color = if (isDrawerDark) Color(0xFF132238) else Color(0xFFEAF5F7),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
              shadowElevation = 3.dp,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
                ManoBrandCrest(size = 38.dp, isDark = isDrawerDark)
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              ManoKidWordmark(isDark = isDrawerDark, fontSize = 17.sp)
              Text("همراه مطمئن سلامت کودک و خانواده", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Spacer(Modifier.height(12.dp))
          HorizontalDivider()
          Spacer(Modifier.height(8.dp))

          val drawerLinks =
            listOf(
              DrawerNavItem("موضوعات سلامت", Icons.Rounded.HealthAndSafety, TopicsRoute(), TopicAccentRole.CORAL, PediatricIconKind.URGENT),
              DrawerNavItem("داروهای کودک", Icons.Rounded.MedicalServices, MedicinesRoute, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
              DrawerNavItem("اسکنر برچسب شربت و قطره", Icons.Rounded.QrCodeScanner, SyrupScannerRoute, TopicAccentRole.TEAL, PediatricIconKind.SCANNER),
              DrawerNavItem("پوست و پوشک", Icons.Rounded.Spa, SkinRoute, TopicAccentRole.PLUM, PediatricIconKind.DIAPER),
              DrawerNavItem("وسایل مراقبتی (نبولایزر/بخور)", Icons.Rounded.MedicalServices, DevicesRoute, TopicAccentRole.SKY, PediatricIconKind.DEVICE),
              DrawerNavItem("آزمایشگاه سه‌بعدی مانو (3D)", Icons.Rounded.ViewInAr, Pediatric3DStudioRoute(), TopicAccentRole.AQUA, PediatricIconKind.COMPARE_LAB),
              DrawerNavItem("رشد و تکامل", Icons.Rounded.ChildCare, GrowthRoute, TopicAccentRole.SAGE, PediatricIconKind.GROWTH),
              DrawerNavItem("برنامه واکسیناسیون و یادآور", Icons.Rounded.Vaccines, VaccinesRoute, TopicAccentRole.SAGE, PediatricIconKind.VACCINE),
              DrawerNavItem("غذای سالم و مقوی کودک", Icons.Rounded.Restaurant, NutritionRoute, TopicAccentRole.MANGO, PediatricIconKind.SUPPLEMENTS),
              DrawerNavItem("پرسش‌های پرتکرار (FAQ)", Icons.AutoMirrored.Rounded.Help, FaqRoute(), TopicAccentRole.PLUM, PediatricIconKind.FAQ),
              DrawerNavItem("ارتباط با داروساز", Icons.Rounded.LocalPharmacy, AskPharmacistRoute(), TopicAccentRole.SAGE, PediatricIconKind.PHARMACY),
              DrawerNavItem("فرزند من", Icons.Rounded.PersonOutline, MyChildRoute, TopicAccentRole.MANGO, PediatricIconKind.FAMILY),
              DrawerNavItem("منابع علمی و حریم خصوصی", Icons.Rounded.HealthAndSafety, InfoRoute("sources"), TopicAccentRole.GOLD, PediatricIconKind.SAFETY_SHIELD),
              DrawerNavItem("اتصال دامنه (manokid.ir)", Icons.Rounded.Language, InfoRoute("domain"), TopicAccentRole.SKY, PediatricIconKind.FOLLOW_UP),
              DrawerNavItem("تنظیمات دستگاه و انتشار", Icons.Rounded.Tune, InfoRoute("release"), TopicAccentRole.AQUA, PediatricIconKind.DEVICE),
            )

          drawerLinks.forEach { item ->
            val isItemSelected =
              when (item.route) {
                is TopicsRoute -> currentDest?.hasRoute<TopicsRoute>() == true
                is MedicinesRoute -> currentDest?.hasRoute<MedicinesRoute>() == true
                is SyrupScannerRoute -> currentDest?.hasRoute<SyrupScannerRoute>() == true
                is SkinRoute -> currentDest?.hasRoute<SkinRoute>() == true
                is DevicesRoute -> currentDest?.hasRoute<DevicesRoute>() == true
                is Pediatric3DStudioRoute -> currentDest?.hasRoute<Pediatric3DStudioRoute>() == true
                is GrowthRoute -> currentDest?.hasRoute<GrowthRoute>() == true
                is VaccinesRoute -> currentDest?.hasRoute<VaccinesRoute>() == true
                is NutritionRoute -> currentDest?.hasRoute<NutritionRoute>() == true
                is FaqRoute -> currentDest?.hasRoute<FaqRoute>() == true
                is AskPharmacistRoute -> currentDest?.hasRoute<AskPharmacistRoute>() == true
                is MyChildRoute -> currentDest?.hasRoute<MyChildRoute>() == true
                is InfoRoute -> currentDest?.hasRoute<InfoRoute>() == true
                else -> false
              }
            NavigationDrawerItem(
              label = { Text(item.label) },
              icon = {
                ManoIconMedallion(
                  icon = item.icon,
                  accentColor = item.accentRole.resolveAccentColor(),
                  iconKind = item.iconKind,
                  containerSize = 38.dp,
                  iconSize = 22.dp,
                  cornerRadius = 12.dp,
                )
              },
              selected = isItemSelected,
              onClick = {
                scope.launch { drawerState.close() }
                navController.navigate(item.route) { launchSingleTop = true }
              },
            )
          }

          NavigationDrawerItem(
            label = { Text("راه‌اندازی سریع کودک (۳ مرحله)") },
            icon = {
              ManoIconMedallion(
                icon = Icons.Rounded.AutoAwesome,
                accentColor = TopicAccentRole.TEAL.resolveAccentColor(),
                iconKind = PediatricIconKind.FAMILY,
                containerSize = 38.dp,
                iconSize = 22.dp,
                cornerRadius = 12.dp,
              )
            },
            selected = false,
            onClick = {
              scope.launch { drawerState.close() }
              manualOnboardingOpen = true
            },
            modifier = Modifier.testTag("drawer_onboarding_item"),
          )

          Spacer(Modifier.height(10.dp))
          HorizontalDivider()
          Spacer(Modifier.height(10.dp))
          Text(
            text = "ظاهر برنامه (حالت روز و شب)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp).testTag("drawer_theme_selector"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(
              Triple(ManoThemeMode.SYSTEM, Icons.Rounded.BrightnessAuto, "drawer_theme_system"),
              Triple(ManoThemeMode.LIGHT, Icons.Rounded.LightMode, "drawer_theme_light"),
              Triple(ManoThemeMode.DARK, Icons.Rounded.DarkMode, "drawer_theme_dark"),
            ).forEach { (mode, icon, tag) ->
              val selected = themeMode == mode
              val chipColor =
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              val chipBorder =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
              val contentColor =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
              Surface(
                onClick = { viewModel.setThemeMode(mode) },
                color = chipColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, chipBorder),
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag(tag),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(icon, contentDescription = mode.labelFa, tint = contentColor, modifier = Modifier.size(18.dp))
                  Text(
                    text = when (mode) {
                      ManoThemeMode.SYSTEM -> "سیستم"
                      ManoThemeMode.LIGHT -> "روز"
                      ManoThemeMode.DARK -> "شب"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
      }
    },
  ) {
    ManoAtmosphericBackdrop {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp
        val isDark = LocalManoDarkTheme.current
        val topChipBg = if (isDark) DarkSurface else PureWhite
        val topChipBorder = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f) else HairlineBorder

        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            TopAppBar(
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = Color.Transparent,
                  titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
              navigationIcon = {
                if (!isHome) {
                  IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("topbar_back"),
                  ) {
                    Surface(
                      color = topChipBg,
                      shape = RoundedCornerShape(14.dp),
                      border = BorderStroke(1.dp, topChipBorder),
                      modifier =
                        Modifier
                          .size(40.dp)
                          .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت", tint = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                } else {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("topbar_menu"),
                  ) {
                    Surface(
                      color = topChipBg,
                      shape = RoundedCornerShape(14.dp),
                      border = BorderStroke(1.dp, topChipBorder),
                      modifier =
                        Modifier
                          .size(40.dp)
                          .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "فهرست دسترسی سریع", tint = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                }
              },
              title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Surface(
                    color = if (isDark) Color(0xFF132238) else Color(0xFFEAF5F7),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)),
                    modifier =
                      Modifier
                        .size(38.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(3.dp)) {
                      ManoBrandCrest(size = 32.dp, isDark = isDark)
                    }
                  }
                  ManoKidWordmark(isDark = isDark, fontSize = 16.sp)
                }
              },
              actions = {
                IconButton(
                  onClick = { viewModel.toggleDayNightTheme(isDark) },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("topbar_theme_toggle"),
                ) {
                  Surface(
                    color = topChipBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, topChipBorder),
                    modifier =
                      Modifier
                        .size(40.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (isDark) "تغییر به حالت روز" else "تغییر به حالت شب",
                        tint = if (isDark) MangoCare else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                }
                IconButton(
                  onClick = { showGlobalSearchSheet = true },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("topbar_search"),
                ) {
                  Surface(
                    color = topChipBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, topChipBorder),
                    modifier =
                      Modifier
                        .size(40.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Search, contentDescription = "جست‌وجوی سراسری", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                  }
                }
                IconButton(
                  onClick = { navController.navigate(MyChildRoute) { launchSingleTop = true } },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("topbar_my_child"),
                ) {
                  Surface(
                    color = topChipBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.40f) else HairlineBorder),
                    modifier =
                      Modifier
                        .size(40.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = ShadowSlate.copy(alpha = 0.14f)),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.PersonOutline, contentDescription = "پروفایل فرزند من", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    }
                  }
                }
              },
            )
          },
          bottomBar = {
            if (!useNavigationRail) {
              ManoFloatingBottomNavBar(
                tabs = primaryTabs,
                isTabSelected = { tab ->
                  when (tab.route) {
                    is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                    is SymptomsRoute -> currentDest?.hasRoute<SymptomsRoute>() == true
                    is MedicinesRoute ->
                      currentDest?.hasRoute<MedicinesRoute>() == true ||
                        currentDest?.hasRoute<MedicineDetailRoute>() == true ||
                        currentDest?.hasRoute<SyrupScannerRoute>() == true
                    is GrowthRoute ->
                      currentDest?.hasRoute<GrowthRoute>() == true ||
                        currentDest?.hasRoute<NutritionRoute>() == true ||
                        currentDest?.hasRoute<VaccinesRoute>() == true
                    is FaqRoute -> currentDest?.hasRoute<FaqRoute>() == true
                    else -> false
                  }
                },
                onSelectTab = { tab -> navigateTopLevel(tab.route) },
              )
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavigationRail) {
              NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                modifier = Modifier.fillMaxHeight(),
              ) {
                primaryTabs.forEach { tab ->
                  val selected =
                    when (tab.route) {
                      is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                      is SymptomsRoute -> currentDest?.hasRoute<SymptomsRoute>() == true
                      is MedicinesRoute ->
                        currentDest?.hasRoute<MedicinesRoute>() == true ||
                          currentDest?.hasRoute<MedicineDetailRoute>() == true ||
                          currentDest?.hasRoute<SyrupScannerRoute>() == true
                      is GrowthRoute ->
                        currentDest?.hasRoute<GrowthRoute>() == true ||
                          currentDest?.hasRoute<NutritionRoute>() == true ||
                          currentDest?.hasRoute<VaccinesRoute>() == true
                      is FaqRoute -> currentDest?.hasRoute<FaqRoute>() == true
                      else -> false
                    }
                  val railAccent = tab.accentRole.resolveAccentColor()
                  val railProgress by
                    animateFloatAsState(
                      targetValue = if (selected) 1f else 0f,
                      animationSpec = spring(),
                      label = "railProgress_${tab.tag}",
                    )
                  NavigationRailItem(
                    selected = selected,
                    onClick = { navigateTopLevel(tab.route) },
                    icon = {
                      ManoAlgorithmicNavIcon(
                        tabTag = tab.tag,
                        selectionProgress = railProgress,
                        accentColor = railAccent,
                        inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                      )
                    },
                    label = { Text(tab.label) },
                    colors =
                      NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      ),
                    modifier = Modifier.testTag(tab.tag),
                  )
                }
              }
            }

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            NavHost(navController = navController, startDestination = HomeRoute) {
              composable<HomeRoute> {
                HomeScreen(
                  selectedAgeBand = selectedAgeBand,
                  bookmarkedSlugs = effectiveBookmarks,
                  articles = viewModel.allArticles,
                  onSelectAgeBand = viewModel::selectAgeBand,
                  onToggleBookmark = ::toggleBookmarkSynced,
                  onNavigateSymptoms = { id ->
                    viewModel.startSymptomFlow(id)
                    navController.navigate(SymptomsRoute(id))
                  },
                  onNavigateTopics = { catId, q ->
                    viewModel.setTopicSearchQuery(q)
                    viewModel.setTopicCategory(catId?.let { id -> TopicCategory.entries.firstOrNull { it.id == id } })
                    navController.navigate(TopicsRoute(catId, q))
                  },
                  onNavigateTopicDetail = { slug -> navController.navigate(TopicDetailRoute(slug)) },
                  onNavigateMedicines = { navController.navigate(MedicinesRoute) },
                  onNavigateGrowth = { navController.navigate(GrowthRoute) },
                  onNavigateSkin = { navController.navigate(SkinRoute) },
                  onNavigateDevices = { navController.navigate(DevicesRoute) },
                  onNavigateAskPharmacist = { navController.navigate(AskPharmacistRoute()) },
                  onNavigateMyChild = { navController.navigate(MyChildRoute) },
                  onNavigateInfo = { section -> navController.navigate(InfoRoute(section)) },
                  onOpenGlobalSearch = { showGlobalSearchSheet = true },
                  hasPlayedHeroIntro = uiState.hasPlayedHeroIntro,
                  onHeroIntroPlayed = viewModel::markHeroIntroPlayed,
                  onNavigate3DStudio = { scene -> navController.navigate(Pediatric3DStudioRoute(scene)) },
                  timelineSnapshot = timelineSnapshot,
                  onQuickSaveFever = { tempC, caregiver ->
                    val timeNow = FeverDoseTimelineEngine.formatHourMinute(System.currentTimeMillis())
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      activeCloudVm.addFeverLog(tempC.toDouble(), "زیربغلی", timeNow, caregiver, "ثبت سریع از صفحه خانه") {}
                    } else {
                      viewModel.addLocalFeverLog(tempC, "زیربغلی", timeNow, caregiver, "ثبت سریع از صفحه خانه")
                    }
                  },
                  onOpenFeverTimeline = {
                    openMedicinesTimelineTab = true
                    navController.navigate(MedicinesRoute) { launchSingleTop = true }
                  },
                  onNavigateNutrition = { navController.navigate(NutritionRoute) { launchSingleTop = true } },
                  checkedMedSafetyCount = checkedMedSafetyIds.size,
                  onOpenMedicationChecklist = {
                    openMedicinesChecklistTab = true
                    navController.navigate(MedicinesRoute) { launchSingleTop = true }
                  },
                  vaccineBadgeLabelFa =
                    vaccineSummary.nextUpcoming?.let { next ->
                      "${next.definition.milestoneLabelFa} • ${VaccinationScheduleEngine.formatRelativeDueLabelFa(next)}"
                    } ?: "",
                  onNavigateVaccines = { navController.navigate(VaccinesRoute) { launchSingleTop = true } },
                  savedGalacticRingName = savedGalacticSelection?.ringName,
                  savedGalacticTopicId = savedGalacticSelection?.topicId,
                  onSaveGalacticSelection = viewModel::saveGalacticOrbitSelection,
                  onNavigateSyrupScanner = { navController.navigate(SyrupScannerRoute) { launchSingleTop = true } },
                  savedSyrupScansCount = effectiveSyrupScans.size,
                  onNavigateEcosystemPillar = { pillar ->
                    when (pillar) {
                      com.example.ui.components.ManoEcosystemPillar.DOCUMENTED_QA -> {
                        navController.navigate(FaqRoute()) { launchSingleTop = true }
                      }
                      com.example.ui.components.ManoEcosystemPillar.MANO_SAFETY -> {
                        requestedMedicinesTabKey = "overlap"
                        navController.navigate(MedicinesRoute) { launchSingleTop = true }
                      }
                      com.example.ui.components.ManoEcosystemPillar.PRODUCT_COMPARISON -> {
                        requestedMedicinesTabKey = "comparison"
                        navController.navigate(MedicinesRoute) { launchSingleTop = true }
                      }
                      com.example.ui.components.ManoEcosystemPillar.MANO_CONNECT -> {
                        navController.navigate(AskPharmacistRoute(initialMode = "parent")) { launchSingleTop = true }
                      }
                      com.example.ui.components.ManoEcosystemPillar.MANO_FOLLOW_UP -> {
                        navController.navigate(AskPharmacistRoute(initialMode = "followup")) { launchSingleTop = true }
                      }
                      com.example.ui.components.ManoEcosystemPillar.MANO_PHARMACY -> {
                        navController.navigate(AskPharmacistRoute(initialMode = "staff")) { launchSingleTop = true }
                      }
                    }
                  },
                  scrollToTopTrigger = homeScrollToTopTrigger,
                  hapticIntensity = hapticIntensity,
                )
              }

              composable<TopicsRoute> { entry ->
                val args = entry.toRoute<TopicsRoute>()
                LaunchedEffect(args) {
                  if (args.query.isNotBlank()) viewModel.setTopicSearchQuery(args.query)
                  if (args.categoryId != null) {
                    viewModel.setTopicCategory(TopicCategory.entries.firstOrNull { it.id == args.categoryId })
                  }
                }
                TopicsScreen(
                  query = uiState.topicSearchQuery,
                  selectedCategory = uiState.topicCategory,
                  selectedAgeBand = selectedAgeBand,
                  articles = viewModel.filteredArticles(selectedAgeBand),
                  bookmarkedSlugs = effectiveBookmarks,
                  onQueryChange = viewModel::setTopicSearchQuery,
                  onCategorySelect = viewModel::setTopicCategory,
                  onAgeSelect = viewModel::selectAgeBand,
                  onToggleBookmark = ::toggleBookmarkSynced,
                  onOpenArticle = { slug -> navController.navigate(TopicDetailRoute(slug)) },
                  onNavigateSkin = { navController.navigate(SkinRoute) },
                  onNavigateDevices = { navController.navigate(DevicesRoute) },
                  onNavigate3DStudio = { navController.navigate(Pediatric3DStudioRoute()) },
                  onShowFeedback = viewModel::showToast,
                )
              }

              composable<TopicDetailRoute> { entry ->
                val args = entry.toRoute<TopicDetailRoute>()
                val article = viewModel.articleBySlug(args.slug)
                val related =
                  article?.relatedSlugs?.mapNotNull { viewModel.articleBySlug(it) } ?: emptyList()
                TopicDetailScreen(
                  article = article,
                  relatedArticles = related,
                  isBookmarked = args.slug in effectiveBookmarks,
                  onToggleBookmark = { toggleBookmarkSynced(args.slug) },
                  onBack = { navController.popBackStack() },
                  onNextStepCta = { key ->
                    when {
                      key.startsWith("symptoms:") -> {
                        val sId = key.removePrefix("symptoms:")
                        viewModel.startSymptomFlow(sId)
                        navController.navigate(SymptomsRoute(sId))
                      }
                      key.startsWith("faq:") -> {
                        val q = key.removePrefix("faq:")
                        viewModel.setFaqSearchQuery(q)
                        navController.navigate(FaqRoute(q))
                      }
                      key == "medicines" -> navController.navigate(MedicinesRoute)
                      key == "skin" -> navController.navigate(SkinRoute)
                      key == "growth" -> navController.navigate(GrowthRoute)
                      else -> navController.navigate(SymptomsRoute())
                    }
                  },
                  onOpenArticle = { slug -> navController.navigate(TopicDetailRoute(slug)) },
                  onShowFeedback = viewModel::showToast,
                )
              }

              composable<SymptomsRoute> { entry ->
                val args = entry.toRoute<SymptomsRoute>()
                LaunchedEffect(args.symptomId) {
                  if (uiState.symptomWizard.step == 0 && args.symptomId.isNotBlank()) {
                    viewModel.selectWizardSymptom(args.symptomId)
                  }
                }
                SymptomNavigatorScreen(
                  wizard = uiState.symptomWizard,
                  flows = viewModel.allSymptomFlows,
                  onSelectSymptom = viewModel::selectWizardSymptom,
                  onSelectAgeBand = viewModel::selectWizardAgeBand,
                  onUpdateTemp = viewModel::updateWizardTemperature,
                  onUpdateAgeMonths = viewModel::updateWizardAgeMonths,
                  onUpdateDuration = viewModel::updateWizardDurationHours,
                  onToggleRedFlag = viewModel::toggleWizardRedFlag,
                  onNext = viewModel::wizardNextStep,
                  onBack = viewModel::wizardPreviousStep,
                  onReset = { viewModel.resetWizard(); safetyTriageAiVm.resetChecklist() },
                  onOpenArticle = { slug -> navController.navigate(TopicDetailRoute(slug)) },
                  onNavigateAskPharmacist = { navController.navigate(AskPharmacistRoute()) },
                  timelineSnapshot = timelineSnapshot,
                  savedVisitDraftsCount = effectiveSavedVisitDrafts.size,
                  onOpenVisitDraftStudio = { showVisitDraftSheet = true },
                  safetyChecklistState = safetyChecklistUiState,
                  onGenerateSafetyChecklist = { primaryFlow, coSymptomsFa ->
                    safetyTriageAiVm.generatePediatricianSafetyChecklist(
                      primaryFlow = primaryFlow,
                      coSelectedSymptomsFa = coSymptomsFa,
                      wizard = uiState.symptomWizard,
                      childProfile = effectiveChildProfile,
                    )
                  },
                  onToggleSafetyChecklistItem = safetyTriageAiVm::toggleChecklistItem,
                )
              }

              composable<MedicinesRoute> {
                MedicinesScreen(
                  query = uiState.medicineSearchQuery,
                  medications = viewModel.filteredMedications(),
                  allMedicationsCatalog = viewModel.allMedications,
                  onQueryChange = viewModel::setMedicineSearchQuery,
                  onOpenMedication = { slug -> navController.navigate(MedicineDetailRoute(slug)) },
                  onNavigateDevices = { navController.navigate(DevicesRoute) },
                  onNavigateAskPharmacist = { navController.navigate(AskPharmacistRoute("otc")) },
                  selectedCategory = uiState.medicineCategory,
                  onCategorySelect = viewModel::setMedicineCategory,
                  childProfile = effectiveChildProfile,
                  childBirthDateEpochMs = childBirthDateEpochMs,
                  currentUser = currentUser,
                  doseLogsState = cloudDoseLogsState,
                  timelineSnapshot = timelineSnapshot,
                  onAddDoseLog = { slug, nameFa, formFa, volumeMl, timeLabel, noteFa, onDone ->
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      activeCloudVm.addDoseLog(slug, nameFa, formFa, volumeMl, timeLabel, noteFa, onDone)
                    } else {
                      viewModel.addLocalDoseLog(slug, nameFa, formFa, volumeMl, timeLabel, noteFa)
                      onDone()
                    }
                  },
                  onDeleteDoseLog = { logId, isCloud ->
                    if (isCloud) cloudVm?.deleteDoseLog(logId) else viewModel.deleteLocalDoseLog(logId)
                  },
                  onAddFeverLog = { tempCelsius, siteFa, timeLabelFa, caregiverFa, noteFa, onDone ->
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      activeCloudVm.addFeverLog(tempCelsius.toDouble(), siteFa, timeLabelFa, caregiverFa, noteFa, onDone)
                    } else {
                      viewModel.addLocalFeverLog(tempCelsius, siteFa, timeLabelFa, caregiverFa, noteFa)
                      onDone()
                    }
                  },
                  onDeleteFeverLog = { logId, isCloud ->
                    if (isCloud) cloudVm?.deleteFeverLog(logId) else viewModel.deleteLocalFeverLog(logId)
                  },
                  onOpen3DWithDose = { recommendedMl ->
                    val hasFever = (timelineSnapshot.latestTempCelsius ?: 0f) >= 38.0f
                    navController.navigate(
                      Pediatric3DStudioRoute(
                        initialScene = "workbench",
                        prefillMl = recommendedMl,
                        prefillFever = hasFever,
                      )
                    )
                  },
                  onOpenUrgentDoctorGuide = { navController.navigate(TopicDetailRoute("when-to-see-doctor")) },
                  selectedSymptomFlow = activeSymptomFlow,
                  savedVisitDraftsCount = effectiveSavedVisitDrafts.size,
                  onOpenVisitDraftStudio = { showVisitDraftSheet = true },
                  startOnTimeline = openMedicinesTimelineTab,
                  onTimelineStartConsumed = { openMedicinesTimelineTab = false },
                  checkedMedSafetyIds = checkedMedSafetyIds,
                  onToggleMedSafetyCheck = viewModel::toggleMedSafetyCheckItem,
                  onResetMedSafetyChecklist = viewModel::resetMedSafetyChecklist,
                  startOnChecklist = openMedicinesChecklistTab,
                  onChecklistStartConsumed = { openMedicinesChecklistTab = false },
                  requestedSubTabKey = requestedMedicinesTabKey,
                  onSubTabKeyConsumed = { requestedMedicinesTabKey = null },
                  onCloudAuthSuccess = { viewModel.showToast("اتصال به حساب گوگل انجام شد.") },
                  onCloudAuthError = { msg -> viewModel.showToast(msg) },
                  onSignedOut = { viewModel.showToast("از حساب گوگل خارج شدید.") },
                )
              }

              composable<MedicineDetailRoute> { entry ->
                val args = entry.toRoute<MedicineDetailRoute>()
                MedicineDetailScreen(
                  medication = viewModel.medicationBySlug(args.slug),
                  onBack = { navController.popBackStack() },
                  onNavigateAskPharmacist = { navController.navigate(AskPharmacistRoute("otc")) },
                  onShowFeedback = viewModel::showToast,
                )
              }

              composable<GrowthRoute> {
                GrowthScreen(
                  selectedAgeBand = selectedAgeBand,
                  milestones = viewModel.milestonesForAge(selectedAgeBand),
                  checkedMilestoneIds = checkedMilestoneIds,
                  onSelectAgeBand = viewModel::selectAgeBand,
                  onToggleMilestone = viewModel::toggleMilestoneChecked,
                  onOpenGrowthArticle = { navController.navigate(TopicDetailRoute("growth-and-development")) },
                  onOpen3DWeighingGuide = { navController.navigate(Pediatric3DStudioRoute("weighing")) },
                  childProfile = effectiveChildProfile,
                  savedHeightCm = childHeightCm,
                  savedNutritionPlansCount = savedNutritionPlans.size,
                  onOpenNutritionAdvisor = { navController.navigate(NutritionRoute) { launchSingleTop = true } },
                  vaccineProgressSummaryFa =
                    vaccineSummary.nextUpcoming?.let { next ->
                      "${next.definition.milestoneLabelFa}: ${VaccinationScheduleEngine.formatRelativeDueLabelFa(next)}"
                    } ?: "",
                  onOpenVaccinesSchedule = { navController.navigate(VaccinesRoute) { launchSingleTop = true } },
                )
              }

              composable<VaccinesRoute> {
                VaccinesScreen(
                  summary = vaccineSummary,
                  birthDateEpochMs = childBirthDateEpochMs,
                  reminderSettings = vaccineReminderSettings,
                  childProfile = effectiveChildProfile,
                  hasNotificationPermission = hasNotificationPermission,
                  isCloudSignedIn = currentUser != null,
                  onSaveBirthDate = viewModel::setChildBirthDateEpochMs,
                  onUpdateReminderSettings = viewModel::updateVaccineReminderSettings,
                  onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                  },
                  onSendPreviewReminder = {
                    val previewTargets =
                      vaccineSummary.dueForReminder.ifEmpty {
                        listOfNotNull(vaccineSummary.nextUpcoming ?: vaccineSummary.items.firstOrNull())
                      }
                    val posted =
                      VaccineNotificationHelper.postVaccineReminderNotification(
                        context = context,
                        dueItems = previewTargets,
                        childAliasFa = effectiveChildProfile?.aliasFa,
                        isPreview = true,
                      )
                    viewModel.showToast(
                      if (posted) "اعلان یادآور واکسن به نوار اعلان ارسال شد."
                      else "برای نمایش اعلان، مجوز اعلان سیستم را فعال کنید."
                    )
                  },
                  onSaveVaccineRecord = { record ->
                    viewModel.saveLocalVaccineRecord(record)
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      activeCloudVm.upsertVaccineRecord(record)
                    }
                  },
                  onDeleteVaccineRecord = { vaccineId ->
                    viewModel.deleteLocalVaccineRecord(vaccineId)
                    cloudVm?.deleteVaccineRecord(vaccineId)
                  },
                  onFeedback = viewModel::showToast,
                )
              }

              composable<NutritionRoute> {
                NutritionAdvisorScreen(
                  nutritionUiState = nutritionUiState,
                  savedPlans = savedNutritionPlans,
                  childProfile = effectiveChildProfile,
                  selectedAgeBand = selectedAgeBand,
                  savedHeightCm = childHeightCm,
                  checkedMilestones = viewModel.checkedMilestonesList(),
                  timelineSnapshot = timelineSnapshot,
                  onSaveHeightCm = viewModel::setChildHeightCm,
                  onGeneratePlan = { ageBand, ageDetailFa, weightKgText, heightCmText, allergiesFa, appetiteStatus, availableIngredientsFa ->
                    nutritionVm.generateNutritionPlan(
                      childProfile = effectiveChildProfile,
                      ageBand = ageBand,
                      ageDetailFa = ageDetailFa,
                      weightKgText = weightKgText,
                      heightCmText = heightCmText,
                      allergiesFa = allergiesFa,
                      checkedMilestones = viewModel.checkedMilestonesList(),
                      timelineSnapshot = timelineSnapshot,
                      appetiteStatus = appetiteStatus,
                      availableIngredientsFa = availableIngredientsFa,
                      onSavePlan = viewModel::saveNutritionPlan,
                    )
                  },
                  onLoadSavedPlan = nutritionVm::loadSavedPlan,
                  onDeleteSavedPlan = { id ->
                    viewModel.deleteNutritionPlan(id)
                    val activeReady = nutritionUiState as? com.example.ui.viewmodel.NutritionUiState.Ready
                    if (activeReady?.plan?.id == id) {
                      nutritionVm.resetToIdle()
                    }
                  },
                  onOpenMyChildProfile = { navController.navigate(MyChildRoute) { launchSingleTop = true } },
                  onOpen3DWeighingGuide = { navController.navigate(Pediatric3DStudioRoute("weighing")) { launchSingleTop = true } },
                  onFeedback = viewModel::showToast,
                )
              }

              composable<SkinRoute> {
                SkinScreen(
                  onNavigateSymptomDiaper = {
                    viewModel.startSymptomFlow("diaper_rash")
                    navController.navigate(SymptomsRoute("diaper_rash"))
                  },
                  onOpenDiaperArticle = { navController.navigate(TopicDetailRoute("diaper-rash-care")) },
                  onNavigateAskPharmacist = { navController.navigate(AskPharmacistRoute("skin")) },
                )
              }

              composable<DevicesRoute> {
                DevicesScreen(
                  guides = viewModel.allDeviceGuides,
                  onNavigateSymptoms = { navController.navigate(SymptomsRoute()) },
                  childProfile = effectiveChildProfile,
                  childBirthDateEpochMs = childBirthDateEpochMs,
                  onNavigate3DStudio = { navController.navigate(Pediatric3DStudioRoute("workbench")) },
                  onOpen3DWithDose = { recommendedMl ->
                    val hasFever = (timelineSnapshot.latestTempCelsius ?: 0f) >= 38.0f
                    navController.navigate(
                      Pediatric3DStudioRoute(
                        initialScene = "workbench",
                        prefillMl = recommendedMl,
                        prefillFever = hasFever,
                      )
                    )
                  },
                )
              }

              composable<Pediatric3DStudioRoute> { entry ->
                val args = entry.toRoute<Pediatric3DStudioRoute>()
                Pediatric3DStudioScreen(
                  initialScene = args.initialScene,
                  prefillMl = args.prefillMl,
                  prefillFever = args.prefillFever,
                  prefillFormulationId = args.prefillFormulationId,
                  prefillScannedDrugFa = args.prefillScannedDrugFa,
                  childProfile = effectiveChildProfile,
                  childBirthDateEpochMs = childBirthDateEpochMs,
                  latestTempCelsius = timelineSnapshot.latestTempCelsius,
                  hapticIntensity = hapticIntensity,
                  webGlQuality = webGlQuality,
                  onBack = { navController.popBackStack() },
                )
              }

              composable<SyrupScannerRoute> {
                SyrupScannerScreen(
                  scannerUiState = syrupScannerUiState,
                  savedScans = effectiveSyrupScans,
                  childProfile = effectiveChildProfile,
                  childBirthDateEpochMs = childBirthDateEpochMs,
                  isCloudSignedIn = currentUser != null,
                  onAnalyzeBitmap = { bmp -> syrupScannerVm.analyzeBitmap(bmp, effectiveChildProfile, childBirthDateEpochMs) },
                  onStartManualFallback = { syrupScannerVm.startManualFallback(effectiveChildProfile, birthDateEpochMs = childBirthDateEpochMs) },
                  onEditDrugName = { name -> syrupScannerVm.editExtraction(drugNameFa = name, childProfile = effectiveChildProfile, birthDateEpochMs = childBirthDateEpochMs) },
                  onEditIngredient = { ing -> syrupScannerVm.editExtraction(ingredientKey = ing, childProfile = effectiveChildProfile, birthDateEpochMs = childBirthDateEpochMs) },
                  onEditDosageForm = { form -> syrupScannerVm.editExtraction(dosageForm = form, childProfile = effectiveChildProfile, birthDateEpochMs = childBirthDateEpochMs) },
                  onAdjustStrength = { mg, perMl ->
                    syrupScannerVm.editExtraction(mgAmount = mg, perMlAmount = perMl, childProfile = effectiveChildProfile, birthDateEpochMs = childBirthDateEpochMs)
                  },
                  onSelectCalibratedPreset = { preset ->
                    syrupScannerVm.selectCalibratedPreset(preset, effectiveChildProfile, childBirthDateEpochMs)
                  },
                  onSetParentVerified = syrupScannerVm::setParentVerified,
                  onRefreshChildContext = { syrupScannerVm.refreshChildContext(effectiveChildProfile, childBirthDateEpochMs) },
                  onConfirmSaveAndOpen3D = { formulationId, drugNameFa, targetMl ->
                    val confirmedScan = syrupScannerVm.buildConfirmedSavedScan()
                    if (confirmedScan != null) {
                      viewModel.saveLocalSyrupScan(confirmedScan, toastFa = "")
                      val activeCloudVm = cloudVm
                      if (currentUser != null && activeCloudVm != null) {
                        activeCloudVm.upsertSyrupScan(confirmedScan)
                      }
                    }
                    val hasFever = (timelineSnapshot.latestTempCelsius ?: 0f) >= 38.0f
                    navController.navigate(
                      Pediatric3DStudioRoute(
                        initialScene = "workbench",
                        prefillMl = targetMl,
                        prefillFever = hasFever,
                        prefillFormulationId = formulationId,
                        prefillScannedDrugFa = drugNameFa,
                      )
                    ) {
                      launchSingleTop = true
                    }
                  },
                  onConfirmSaveOnly = {
                    val confirmedScan = syrupScannerVm.buildConfirmedSavedScan()
                    if (confirmedScan != null) {
                      viewModel.saveLocalSyrupScan(confirmedScan)
                      val activeCloudVm = cloudVm
                      if (currentUser != null && activeCloudVm != null) {
                        activeCloudVm.upsertSyrupScan(confirmedScan)
                      }
                    }
                  },
                  onLoadSavedScan = { scan -> syrupScannerVm.loadSavedScan(scan, effectiveChildProfile, childBirthDateEpochMs) },
                  onDeleteSavedScan = { scan ->
                    viewModel.deleteLocalSyrupScan(scan.id)
                    cloudVm?.deleteSyrupScan(scan.id)
                  },
                  onResetScanner = syrupScannerVm::reset,
                  onOpenMyChildProfile = { navController.navigate(MyChildRoute) { launchSingleTop = true } },
                  onOpenMedicinesSyringe = {
                    requestedMedicinesTabKey = "syringe"
                    navController.navigate(MedicinesRoute) { launchSingleTop = true }
                  },
                  onOpenAskPharmacist = { navController.navigate(AskPharmacistRoute("otc")) { launchSingleTop = true } },
                  onFeedback = viewModel::showToast,
                )
              }

              composable<FaqRoute> { entry ->
                val args = entry.toRoute<FaqRoute>()
                LaunchedEffect(args.query) {
                  if (args.query.isNotBlank()) viewModel.setFaqSearchQuery(args.query)
                }
                FaqScreen(
                  query = uiState.faqSearchQuery,
                  selectedCategory = uiState.faqCategory,
                  selectedAgeBand = selectedAgeBand,
                  faqs = viewModel.filteredFaqs(selectedAgeBand),
                  expandedIds = uiState.expandedFaqIds,
                  onQueryChange = viewModel::setFaqSearchQuery,
                  onCategorySelect = viewModel::setFaqCategory,
                  onAgeSelect = viewModel::selectAgeBand,
                  onToggleExpand = viewModel::toggleFaqExpanded,
                  onNavigateSymptoms = { navController.navigate(SymptomsRoute()) },
                  documentedQaState = documentedQaUiState,
                  onAskDocumentedQuestion = { questionFa ->
                    safetyTriageAiVm.askDocumentedQuestion(
                      questionFa = questionFa,
                      ageBand = selectedAgeBand ?: effectiveChildProfile?.ageBand,
                      relatedFaqs = viewModel.filteredFaqs(selectedAgeBand).ifEmpty { viewModel.filteredFaqs(null) },
                    )
                  },
                )
              }

              composable<AskPharmacistRoute> { entry ->
                val args = entry.toRoute<AskPharmacistRoute>()
                LaunchedEffect(args.categoryId) {
                  val mappedCategory =
                    when (args.categoryId?.trim()?.lowercase()) {
                      "otc", "medicines", "drug" -> ConsultationCategory.OTC
                      "skin", "diaper" -> ConsultationCategory.SKIN
                      "scalp", "hair", "scalp_hair" -> ConsultationCategory.SCALP_HAIR
                      "supplement", "vitamin", "nutrition" -> ConsultationCategory.SUPPLEMENT
                      "symptoms", "fever" -> ConsultationCategory.SYMPTOMS
                      else -> null
                    }
                  if (mappedCategory != null) {
                    viewModel.setAskCategory(mappedCategory)
                  }
                }
                AskPharmacistScreen(
                  state = effectiveConsultations,
                  onSelectCategory = viewModel::setAskCategory,
                  onSelectAgeBand = viewModel::setAskAgeBand,
                  onQuestionChange = { text ->
                    cloudVm?.clearServerConsultationError()
                    viewModel.setAskQuestion(text)
                  },
                  onUrgentFlagChange = { urgent ->
                    cloudVm?.clearServerConsultationError()
                    viewModel.setAskUrgentFlags(urgent)
                  },
                  onConsentChange = { accepted ->
                    cloudVm?.clearServerConsultationError()
                    viewModel.setAskConsent(accepted)
                  },
                  onSubmit = {
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      val validated = viewModel.validateAskPharmacistForm()
                      if (validated != null) {
                        activeCloudVm.submitTrustedConsultation(
                          category = validated.first,
                          ageBand = validated.second,
                          questionSummaryFa = validated.third,
                          hasUrgentFlag = uiState.askPharmacist.hasUrgentFlags,
                          consentAccepted = uiState.askPharmacist.consentAccepted,
                          onSuccess = { viewModel.clearAskPharmacistQuestion() },
                        )
                      }
                    } else {
                      viewModel.submitAskPharmacistDemo()
                    }
                  },
                  onAdvanceStatus = { id, _ ->
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      activeCloudVm.refreshServerConsultations(silent = false)
                    } else {
                      viewModel.advanceDemoRequestStatus(id)
                    }
                  },
                  currentUser = currentUser,
                  acceptingPharmacies = acceptingPharmacies,
                  selectedPharmacyId = selectedPharmacyId,
                  onSelectPharmacy = { pharmacyId -> cloudVm?.selectPharmacy(pharmacyId) },
                  isSubmitting = isSubmittingConsultation,
                  serverErrorFa = serverConsultationErrorFa,
                  onCloudAuthSuccess = { viewModel.showToast("اتصال به حساب گوگل انجام شد.") },
                  onCloudAuthError = { msg -> viewModel.showToast(msg) },
                  onSignedOut = { viewModel.showToast("از حساب گوگل خارج شدید.") },
                  staffAccess = staffAccessStatus,
                  staffInboxCases = staffInboxCases,
                  isLoadingStaffInbox = isLoadingStaffInbox,
                  staffActionInFlightCaseId = staffActionInFlightCaseId,
                  staffErrorFa = staffErrorFa,
                  onRefreshStaffInbox = { forceRefresh, statusFilter ->
                    cloudVm?.refreshStaffStatusAndInbox(forceTokenRefresh = forceRefresh, statusFilter = statusFilter)
                  },
                  onAcknowledgeStaffCaseReceived = { pharmacyId, caseId, note ->
                    cloudVm?.acknowledgeStaffCaseReceived(pharmacyId, caseId, note)
                  },
                  onSubmitStaffCaseReview = { pharmacyId, caseId, note, reply, attestation ->
                    cloudVm?.submitStaffCaseReview(pharmacyId, caseId, note, reply, attestation)
                  },
                  initialMode = args.initialMode,
                  onShowFeedback = viewModel::showToast,
                )
              }

              composable<MyChildRoute> {
                MyChildScreen(
                  profile = effectiveChildProfile,
                  exportedJson = uiState.exportedProfileJson,
                  onSaveProfile = { alias, birthOrAge, band, weightKg, weightDate, allergies, meds, consent ->
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      if (!consent) {
                        viewModel.showToast("ذخیره پروفایل فقط پس از تأیید رضایت والد امکان‌پذیر است.")
                      } else if (alias.isBlank()) {
                        viewModel.showToast("لطفاً یک نام مستعار وارد کنید.")
                      } else if (weightKg.isNotBlank() && (com.example.domain.util.ClinicalNumberInput.decimal(weightKg)?.let { it > 0 && it <= 250 } != true || weightDate.isBlank())) {
                        viewModel.showToast("وزن معتبر و تاریخ اندازه‌گیری را وارد کنید.")
                      } else {
                        activeCloudVm.saveChildProfile(
                          ChildProfileDemo(
                            aliasFa = alias.trim(),
                            birthDateOrAgeFa = birthOrAge.trim().ifEmpty { band.labelFa },
                            ageMonthsExact = com.example.domain.util.LiquidDoseVisualizerEngine.parseAgeMonthsFromText(birthOrAge),
                            ageBand = band,
                            weightKgText = weightKg.trim(),
                            weightDateFa = weightDate.trim(),
                            allergiesFa = allergies.trim().ifEmpty { "ثبت نشده" },
                            currentMedsFa = meds.trim().ifEmpty { "ثبت نشده" },
                            consentGranted = true,
                          )
                        )
                      }
                    } else {
                      viewModel.saveChildProfileDemo(alias, birthOrAge, band, weightKg, weightDate, allergies, meds, consent)
                    }
                  },
                  onExportSummary = viewModel::exportChildProfileSummary,
                  onDeleteProfile = {
                    val activeCloudVm = cloudVm
                    if (currentUser != null && activeCloudVm != null) {
                      activeCloudVm.deleteChildProfile()
                    } else {
                      viewModel.deleteChildProfileDemo()
                    }
                  },
                  currentUser = currentUser,
                  onCloudAuthSuccess = { viewModel.showToast("اتصال به حساب گوگل انجام شد.") },
                  onCloudAuthError = { msg -> viewModel.showToast(msg) },
                  onSignedOut = { viewModel.showToast("از حساب گوگل خارج شدید.") },
                  onOpenNutritionAdvisor = { navController.navigate(NutritionRoute) { launchSingleTop = true } },
                  onOpenVaccinesSchedule = { navController.navigate(VaccinesRoute) { launchSingleTop = true } },
                )
              }

              composable<InfoRoute> { entry ->
                val args = entry.toRoute<InfoRoute>()
                InfoScreen(
                  initialSection = args.initialSection,
                  sources = viewModel.allSources,
                  onTestDeepLink = ::handleDomainDeepLink,
                  onShowFeedback = viewModel::showToast,
                  hapticIntensity = hapticIntensity,
                  webGlQuality = webGlQuality,
                  onSelectHapticIntensity = viewModel::setHapticIntensity,
                  onSelectWebGlQuality = viewModel::setWebGlQuality,
                  onLaunchOnboarding = { manualOnboardingOpen = true },
                )
              }
            }
          }
        }
      }

      val shouldShowFirstRunOnboarding =
        manualOnboardingOpen ||
          (!isRobolectricRuntime && !dismissedAutoOnboarding && onboardingCompleted == false && effectiveChildProfile == null)
      if (shouldShowFirstRunOnboarding) {
        ManoOnboardingOverlay(
          existingProfile = effectiveChildProfile,
          existingBirthDateEpochMs = childBirthDateEpochMs,
          existingHeightCm = childHeightCm,
          onComplete = { alias, birthMs, band, weightKg, heightCm ->
            manualOnboardingOpen = false
            dismissedAutoOnboarding = true
            viewModel.completeOnboarding(
              aliasFa = alias,
              birthDateEpochMs = birthMs,
              ageBand = band,
              weightKgText = weightKg,
              heightCmText = heightCm,
              onProfileReady = { profile ->
                if (currentUser != null && cloudVm != null) {
                  cloudVm.saveChildProfile(profile)
                }
              },
            )
          },
          onSkip = {
            manualOnboardingOpen = false
            dismissedAutoOnboarding = true
            viewModel.skipOnboarding()
          },
        )
      }

      if (activeHighFeverDialog != null) {
        val alertToShow = activeHighFeverDialog!!
        val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission
        HighFeverUrgentAlertDialog(
          alert = alertToShow,
          canRequestSystemNotifications = needsPerm,
          onRequestSystemNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          },
          onOpenWhenToSeeDoctor = { navController.navigate(TopicDetailRoute("when-to-see-doctor")) },
          onDismiss = { activeHighFeverDialog = null },
        )
      }

      if (showVisitDraftSheet) {
        VisitDraftModalBottomSheet(
          onDismiss = { showVisitDraftSheet = false },
          draftUiState = visitDraftUiState,
          pdfState = visitDraftPdfState,
          savedDrafts = effectiveSavedVisitDrafts,
          snapshot = timelineSnapshot,
          childProfile = effectiveChildProfile,
          selectedSymptomFlow = activeSymptomFlow,
          symptomWizard = uiState.symptomWizard,
          isCloudSignedIn = currentUser != null,
          onGenerateAndSave = { parentNote ->
            val activeCloudVm = cloudVm
            val isSignedIn = currentUser != null && activeCloudVm != null
            visitDraftVm.generateAndSaveVisitDraft(
              snapshot = timelineSnapshot,
              childProfile = effectiveChildProfile,
              selectedSymptomFlow = activeSymptomFlow,
              symptomWizard = uiState.symptomWizard,
              parentNoteFa = parentNote,
              isCloudSignedIn = isSignedIn,
              onPersistNewDraft = { titleFa, symptomCtx, timelineSum, draftText, noteFa, modelName, onSavedId ->
                if (activeCloudVm != null && currentUser != null) {
                  activeCloudVm.addVisitDraft(titleFa, symptomCtx, timelineSum, draftText, noteFa, modelName, onSavedId)
                } else {
                  viewModel.saveLocalVisitDraft(
                    titleFa = titleFa,
                    symptomContextFa = symptomCtx,
                    timelineSummaryFa = timelineSum,
                    draftTextFa = draftText,
                    parentNoteFa = noteFa,
                    modelName = modelName,
                    onSavedId = onSavedId,
                  )
                }
              },
            )
          },
          onEditDraft = { newTitle, newBody ->
            visitDraftVm.editActiveDraft(titleFa = newTitle, draftTextFa = newBody)
          },
          onSaveEdits = { ready ->
            val activeCloudVm = cloudVm
            val id = ready.draftId
            if (ready.isCloudSynced && activeCloudVm != null && id != null) {
              activeCloudVm.updateVisitDraft(id, ready.titleFa, ready.draftTextFa, ready.parentNoteFa) {
                visitDraftVm.markActiveDraftSaved(id, isCloudSynced = true)
              }
            } else {
              viewModel.saveLocalVisitDraft(
                titleFa = ready.titleFa,
                symptomContextFa = ready.symptomContextFa,
                timelineSummaryFa = ready.timelineSummaryFa,
                draftTextFa = ready.draftTextFa,
                parentNoteFa = ready.parentNoteFa,
                modelName = com.example.ui.viewmodel.VISIT_DRAFT_GEMINI_MODEL,
                existingId = id,
              ) { savedId ->
                visitDraftVm.markActiveDraftSaved(savedId, isCloudSynced = false)
              }
            }
          },
          onSavePdfToCache = {
            visitDraftVm.exportActiveDraftToCachePdf(
              context = context,
              childProfile = effectiveChildProfile,
              snapshot = timelineSnapshot,
              forceRegenerate = true,
            ) { cached ->
              viewModel.showToast("فایل PDF در حافظه موقت ذخیره شد (${cached.fileName}).")
            }
          },
          onPrintPdf = {
            visitDraftVm.exportActiveDraftToCachePdf(
              context = context,
              childProfile = effectiveChildProfile,
              snapshot = timelineSnapshot,
            ) { cached ->
              val titleFa = (visitDraftUiState as? com.example.ui.viewmodel.VisitDraftUiState.Ready)?.titleFa ?: "گزارش ویزیت مانو کید"
              com.example.ui.util.VisitDraftPdfHelper.printCachedPdf(context, cached, titleFa)
                .onFailure { err ->
                  viewModel.showToast("راه‌اندازی چاپ انجام نشد: ${err.localizedMessage ?: "خطای دستگاه"}")
                }
            }
          },
          onSharePdf = {
            visitDraftVm.exportActiveDraftToCachePdf(
              context = context,
              childProfile = effectiveChildProfile,
              snapshot = timelineSnapshot,
            ) { cached ->
              val titleFa = (visitDraftUiState as? com.example.ui.viewmodel.VisitDraftUiState.Ready)?.titleFa ?: "گزارش ویزیت مانو کید"
              com.example.ui.util.VisitDraftPdfHelper.shareCachedPdf(context, cached, titleFa)
                .onFailure { err ->
                  viewModel.showToast("اشتراک‌گذاری PDF انجام نشد: ${err.localizedMessage ?: "اپلیکیشن مناسب یافت نشد"}")
                }
            }
          },
          onOpenCachedPdf = { cached ->
            com.example.ui.util.VisitDraftPdfHelper.openCachedPdf(context, cached)
              .onFailure {
                viewModel.showToast("نمایشگر PDF روی این دستگاه یافت نشد؛ می‌توانید فایل را چاپ یا ارسال کنید.")
              }
          },
          onLoadDraft = { draft ->
            visitDraftVm.loadSavedDraft(draft)
          },
          onDeleteDraft = { draft ->
            if (draft.isSyncedCloud) {
              cloudVm?.deleteVisitDraft(draft.id)
            } else {
              viewModel.deleteLocalVisitDraft(draft.id)
            }
            val activeReady = visitDraftUiState as? com.example.ui.viewmodel.VisitDraftUiState.Ready
            if (activeReady?.draftId == draft.id) {
              visitDraftVm.resetToIdle()
            }
          },
          onFeedback = viewModel::showToast,
        )
      }

      if (showGlobalSearchSheet) {
        val (matchedArticles, matchedFaqs, matchedMeds) = viewModel.globalSearchResults(selectedAgeBand)
        val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val quickQueries = remember { listOf("تب کودک", "استامینوفن", "سرفه", "واکسن", "پوشک") }
        val resultItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
        val resultBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ModalBottomSheet(
          onDismissRequest = { showGlobalSearchSheet = false },
          sheetState = searchSheetState,
        ) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("جست‌وجوی سریع مانو", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              IconButton(
                onClick = { showGlobalSearchSheet = false },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "بستن جست‌وجو")
              }
            }

            OutlinedTextField(
              value = uiState.globalSearchQuery,
              onValueChange = viewModel::setGlobalSearchQuery,
              label = { Text("جست‌وجو: تب نوزاد، شیرخشک، استامینوفن، سرفه، یبوست...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              trailingIcon = {
                if (uiState.globalSearchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = { viewModel.setGlobalSearchQuery("") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(Icons.Rounded.Close, contentDescription = "پاک کردن عبارت جست‌وجو")
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
            )

            if (PersianNormalizer.isHighRiskQuery(uiState.globalSearchQuery)) {
              SeverityAlert(
                severity = AlertSeverity.DANGER,
                title = "توجه فوری در موضوعات پرخطر",
                body = "در نوزاد زیر ۳ ماه با تب ۳۸ درجه، تنگی نفس یا بی‌حالی شدید بلافاصله به اورژانس ۱۱۵ مراجعه کنید.",
                showCautionMascot = true,
              )
            }

            if (uiState.globalSearchQuery.isBlank()) {
              Text(
                text = "عبارت خود را بنویسید یا یکی از جست‌وجوهای پرتکرار را انتخاب کنید:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                quickQueries.forEach { sample ->
                  SuggestionChip(
                    onClick = { viewModel.setGlobalSearchQuery(sample) },
                    label = { Text(sample, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  )
                }
              }
            } else if (matchedArticles.isEmpty() && matchedFaqs.isEmpty() && matchedMeds.isEmpty()) {
              EmptyState(
                title = "نتیجه‌ای یافت نشد",
                subtitle = "عبارت کوتاه‌تری بنویسید یا وارد فهرست پرسش‌های پرتکرار شوید.",
                pose = MascotPose.WAIT,
              )
            } else {
              if (matchedArticles.isNotEmpty()) {
                Text("مقالات مرتبط", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                matchedArticles.forEach { art ->
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = resultBorder,
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                          showGlobalSearchSheet = false
                          navController.navigate(TopicDetailRoute(art.slug))
                        },
                  ) {
                    ListItem(
                      colors = resultItemColors,
                      headlineContent = { Text(art.titleFa, fontWeight = FontWeight.SemiBold) },
                      supportingContent = { Text(art.category.labelFa) },
                    )
                  }
                }
              }
              if (matchedFaqs.isNotEmpty()) {
                Text("پرسش‌های پرتکرار", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                matchedFaqs.forEach { faq ->
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = resultBorder,
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                          showGlobalSearchSheet = false
                          viewModel.setFaqSearchQuery(uiState.globalSearchQuery)
                          navController.navigate(FaqRoute(uiState.globalSearchQuery))
                        },
                  ) {
                    ListItem(
                      colors = resultItemColors,
                      headlineContent = { Text(faq.questionFa, fontWeight = FontWeight.SemiBold) },
                      supportingContent = { Text(faq.summaryFa, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                  }
                }
              }
              if (matchedMeds.isNotEmpty()) {
                Text("داروهای کودک", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                matchedMeds.forEach { med ->
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = resultBorder,
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                          showGlobalSearchSheet = false
                          navController.navigate(MedicineDetailRoute(med.slug))
                        },
                  ) {
                    ListItem(
                      colors = resultItemColors,
                      headlineContent = { Text(med.nameFa, fontWeight = FontWeight.SemiBold) },
                      supportingContent = { Text(med.genericNameEn) },
                    )
                  }
                }
              }
            }
            Spacer(Modifier.height(24.dp))
          }
        }
      }
      }
    }
  }
}

@Composable
private fun ManoFloatingBottomNavBar(
  tabs: List<NavTab>,
  isTabSelected: (NavTab) -> Boolean,
  onSelectTab: (NavTab) -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  val pillShape = RoundedCornerShape(32.dp)
  val borderColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f) else HairlineBorder

  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = if (isDark) DarkSurface else PureWhite,
      shape = pillShape,
      border = BorderStroke(1.dp, borderColor),
      modifier =
        Modifier
          .fillMaxWidth()
          .shadow(
            elevation = 14.dp,
            shape = pillShape,
            ambientColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else ShadowSlate.copy(alpha = 0.16f),
            spotColor = if (isDark) SkyCare.copy(alpha = 0.24f) else ShadowSlate.copy(alpha = 0.14f),
          ),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEach { tab ->
          val selected = isTabSelected(tab)
          val tabAccent = tab.accentRole.resolveAccentColor()
          val selectionProgress by
            animateFloatAsState(
              targetValue = if (selected) 1f else 0f,
              animationSpec = spring(),
              label = "navTabProgress_${tab.tag}",
            )
          val iconScale by
            animateFloatAsState(
              targetValue = if (selected) 1.12f else 1.0f,
              animationSpec = spring(),
              label = "navTabScale_${tab.tag}",
            )
          val activePillBrush =
            Brush.horizontalGradient(
              colors =
                listOf(
                  tabAccent.copy(alpha = 0.22f),
                  tabAccent.copy(alpha = 0.12f),
                )
            )
          val itemShape = RoundedCornerShape(24.dp)

          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .weight(1f)
                .clip(itemShape)
                .then(if (selected) Modifier.background(activePillBrush) else Modifier)
                .clickable { onSelectTab(tab) }
                .minimumInteractiveComponentSize()
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .testTag(tab.tag),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              ManoAlgorithmicNavIcon(
                tabTag = tab.tag,
                selectionProgress = selectionProgress,
                accentColor = tabAccent,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp).scale(iconScale),
              )
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) tabAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
