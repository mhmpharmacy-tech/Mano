package com.example.ui.components

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.SymptomFlow
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.theme.AquaCare
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.PlumCare
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoElevatedSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.CachedVisitPdf
import com.example.ui.viewmodel.SymptomWizardState
import com.example.ui.viewmodel.VisitDraftPdfState
import com.example.ui.viewmodel.VisitDraftUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitDraftEntryBannerCard(
  snapshot: FeverDoseTimelineSnapshot,
  selectedSymptomFlow: SymptomFlow?,
  savedDraftsCount: Int,
  buttonTag: String,
  onOpenStudio: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val spacing = ManoTheme.spacing
  val plumAccent = manoSemanticAccent(PlumCare, DarkPlum)
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)
  Surface(
    color = ManoTheme.colors.heroBannerTint,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, primary.copy(alpha = 0.24f)),
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth().manoFloatingGlass(intensity = 0.70f, enableSheen = true),
  ) {
    Column(modifier = Modifier.padding(spacing.cardPadding), verticalArrangement = Arrangement.spacedBy(spacing.cardItemGap)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
      ) {
        ManoIconMedallion(
          icon = Icons.Rounded.AutoAwesome,
          accentColor = plumAccent,
          iconKind = null,
          containerSize = 42.dp,
          iconSize = 22.dp,
          cornerRadius = 13.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.textPairGap)) {
          Text(
            text = "پیش‌نویس هوشمند ویزیت پزشک",
            style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
            color = primary,
          )
          Text(
            text = "ترکیب خودکار داده‌های خط زمانی تب، دارو و علائم در یک متن آماده برای مطب و ذخیره در بایگانی.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.chipGap),
        verticalArrangement = Arrangement.spacedBy(spacing.chipGap),
      ) {
        VisitContextPill("${snapshot.tempPoints.size} ثبت دما")
        VisitContextPill("${snapshot.dosePoints.size} نوبت دارو")
        if (selectedSymptomFlow != null) {
          VisitContextPill("علامت: ${selectedSymptomFlow.titleFa}")
        }
        if (savedDraftsCount > 0) {
          VisitContextPill("$savedDraftsCount پیش‌نویس ذخیره‌شده", accent = sageAccent)
        }
      }

      Button(
        onClick = onOpenStudio,
        colors = ButtonDefaults.buttonColors(containerColor = primary),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag(buttonTag),
      ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text =
            if (savedDraftsCount > 0) "تولید پیش‌نویس جدید / مشاهده $savedDraftsCount ذخیره"
            else "تولید و ذخیره پیش‌نویس هوشمند ویزیت",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDraftModalBottomSheet(
  onDismiss: () -> Unit,
  draftUiState: VisitDraftUiState,
  pdfState: VisitDraftPdfState = VisitDraftPdfState.Idle,
  savedDrafts: List<DoctorVisitDraft>,
  snapshot: FeverDoseTimelineSnapshot,
  childProfile: ChildProfileDemo?,
  selectedSymptomFlow: SymptomFlow?,
  symptomWizard: SymptomWizardState,
  isCloudSignedIn: Boolean,
  onGenerateAndSave: (parentNoteFa: String) -> Unit,
  onEditDraft: (titleFa: String?, bodyFa: String?) -> Unit,
  onSaveEdits: (VisitDraftUiState.Ready) -> Unit,
  onSavePdfToCache: () -> Unit = {},
  onPrintPdf: () -> Unit = {},
  onSharePdf: () -> Unit = {},
  onOpenCachedPdf: (CachedVisitPdf) -> Unit = {},
  onLoadDraft: (DoctorVisitDraft) -> Unit,
  onDeleteDraft: (DoctorVisitDraft) -> Unit,
  onFeedback: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = manoCardSurface(),
  ) {
    VisitDraftStudioContent(
      draftUiState = draftUiState,
      pdfState = pdfState,
      savedDrafts = savedDrafts,
      snapshot = snapshot,
      childProfile = childProfile,
      selectedSymptomFlow = selectedSymptomFlow,
      symptomWizard = symptomWizard,
      isCloudSignedIn = isCloudSignedIn,
      onGenerateAndSave = onGenerateAndSave,
      onEditDraft = onEditDraft,
      onSaveEdits = onSaveEdits,
      onSavePdfToCache = onSavePdfToCache,
      onPrintPdf = onPrintPdf,
      onSharePdf = onSharePdf,
      onOpenCachedPdf = onOpenCachedPdf,
      onLoadDraft = onLoadDraft,
      onDeleteDraft = onDeleteDraft,
      onFeedback = onFeedback,
      onClose = onDismiss,
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitDraftStudioContent(
  draftUiState: VisitDraftUiState,
  pdfState: VisitDraftPdfState = VisitDraftPdfState.Idle,
  savedDrafts: List<DoctorVisitDraft>,
  snapshot: FeverDoseTimelineSnapshot,
  childProfile: ChildProfileDemo?,
  selectedSymptomFlow: SymptomFlow?,
  symptomWizard: SymptomWizardState,
  isCloudSignedIn: Boolean,
  onGenerateAndSave: (parentNoteFa: String) -> Unit,
  onEditDraft: (titleFa: String?, bodyFa: String?) -> Unit,
  onSaveEdits: (VisitDraftUiState.Ready) -> Unit,
  onSavePdfToCache: () -> Unit = {},
  onPrintPdf: () -> Unit = {},
  onSharePdf: () -> Unit = {},
  onOpenCachedPdf: (CachedVisitPdf) -> Unit = {},
  onLoadDraft: (DoctorVisitDraft) -> Unit,
  onDeleteDraft: (DoctorVisitDraft) -> Unit,
  onFeedback: (String) -> Unit,
  onClose: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val primary = MaterialTheme.colorScheme.primary
  val cardSurface = manoCardSurface()
  val elevatedSurface = manoElevatedSurface()
  val borderColor = manoBorderColor()
  val plumAccent = manoSemanticAccent(PlumCare, DarkPlum)
  val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val dangerAccent = ManoTheme.colors.coral.accent
  var parentNoteInput by remember { mutableStateOf("") }

  val peakTempLabel = snapshot.peakTemp24h?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "ثبت‌نشده"

  Column(
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("visit_draft_studio_sheet"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ManoIconMedallion(
          icon = Icons.Rounded.Description,
          accentColor = plumAccent,
          iconKind = null,
          containerSize = 42.dp,
          iconSize = 22.dp,
          cornerRadius = 12.dp,
        )
        Column {
          Text("دستیار هوشمند پیش‌نویس ویزیت پزشک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = if (isCloudSignedIn) "ذخیره خودکار در فضای ابری (گوگل)" else "ذخیره خودکار در حافظه دستگاه",
            style = MaterialTheme.typography.labelSmall,
            color = if (isCloudSignedIn) successAccent else infoAccent,
          )
        }
      }
      IconButton(
        onClick = onClose,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("visit_draft_close_btn"),
      ) {
        Icon(Icons.Rounded.Close, contentDescription = "بستن پنجره پیش‌نویس")
      }
    }

    // Live Context Summary Chips from Timeline + Symptom Navigator
    Surface(
      color = manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.45f, darkAlpha = 0.16f),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, borderColor),
      modifier = Modifier.fillMaxWidth().testTag("visit_draft_context_summary"),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("داده‌های ارسالی از خط زمانی و علائم کودک:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = infoAccent)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          childProfile?.aliasFa?.takeIf { it.isNotBlank() }?.let { VisitContextPill("کودک: $it") }
          VisitContextPill("دماها: ${snapshot.tempPoints.size} (اوج $peakTempLabel)")
          VisitContextPill("داروها: ${snapshot.dosePoints.size} نوبت")
          selectedSymptomFlow?.let { VisitContextPill("علامت: ${it.titleFa}") }
          if (symptomWizard.selectedRedFlagIds.isNotEmpty()) {
            VisitContextPill("${symptomWizard.selectedRedFlagIds.size} علامت هشدار انتخاب‌شده", accent = dangerAccent)
          }
        }
      }
    }

    OutlinedTextField(
      value = parentNoteInput,
      onValueChange = { parentNoteInput = it },
      label = { Text("پرسش یا یادداشت اضافه برای پزشک (اختیاری)") },
      placeholder = { Text("مثلاً: از دیشب بی‌اشتها شده و فقط مایعات می‌خورد...") },
      minLines = 2,
      maxLines = 4,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().testTag("visit_draft_parent_note_input"),
    )

    Button(
      onClick = { onGenerateAndSave(parentNoteInput) },
      enabled = draftUiState !is VisitDraftUiState.Streaming,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("generate_save_visit_draft_btn"),
    ) {
      Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text =
          if (draftUiState is VisitDraftUiState.Streaming) "در حال تولید با دستیار هوشمند..."
          else "تولید و ذخیره پیش‌نویس هوشمند",
        fontWeight = FontWeight.Bold,
      )
    }

    when (draftUiState) {
      is VisitDraftUiState.Idle -> {
        Text(
          text = "با لمس دکمه بالا، دستیار هوشمند مانو بر اساس دماهای خط زمانی، نوبت داروها و علائم انتخاب‌شده یک گزارش ساختاریافته و غیرتشخیصی آماده ویزیت تولید و ذخیره می‌کند.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      is VisitDraftUiState.Streaming -> {
        Surface(
          color = ManoTheme.colors.streamLoadingTint,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, plumAccent.copy(alpha = 0.30f)),
          modifier = Modifier.fillMaxWidth().testTag("visit_draft_loading_card"),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              ManoTurquoiseLoadingRing(
                size = ManoCircularProgressSize.COMPACT,
                diameter = 22.dp,
                strokeWidth = 2.4.dp,
                contentDescriptionFa = "در حال نگارش پیش‌نویس هوشمند ویزیت",
              )
              Text("در حال نگارش پیش‌نویس هوشمند ویزیت...", style = MaterialTheme.typography.labelLarge, color = plumAccent)
            }
            if (draftUiState.partialTextFa.isNotBlank()) {
              Text(draftUiState.partialTextFa, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }

      is VisitDraftUiState.Error -> {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "خطا در تولید پیش‌نویس ویزیت",
          body = draftUiState.messageFa,
          showCautionMascot = true,
          actionLabel = "تلاش مجدد",
          onAction = { onGenerateAndSave(parentNoteInput) },
          modifier = Modifier.testTag("visit_draft_error_alert"),
        )
      }

      is VisitDraftUiState.Ready -> {
        Surface(
          color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.42f, darkAlpha = 0.14f),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, if (draftUiState.isSaved) successAccent.copy(alpha = 0.35f) else primary.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth().testTag("visit_draft_editor_card"),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                  imageVector = if (draftUiState.isCloudSynced) Icons.Rounded.CloudDone else Icons.Rounded.BookmarkAdded,
                  contentDescription = null,
                  tint = if (draftUiState.isSaved) successAccent else primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text =
                    if (draftUiState.isSaved) "ذخیره‌شده و آماده ارائه به پزشک"
                    else "تغییرات جدید ذخیره نشده است",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (draftUiState.isSaved) successAccent else primary,
                )
              }
            }

            OutlinedTextField(
              value = draftUiState.titleFa,
              onValueChange = { onEditDraft(it, null) },
              label = { Text("عنوان پیش‌نویس") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("visit_draft_title_input"),
            )

            OutlinedTextField(
              value = draftUiState.draftTextFa,
              onValueChange = { onEditDraft(null, it) },
              label = { Text("متن پیش‌نویس برای پزشک (قابل ویرایش)") },
              minLines = 6,
              maxLines = 14,
              modifier = Modifier.fillMaxWidth().testTag("visit_draft_body_input"),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = { onSaveEdits(draftUiState) },
                enabled = !draftUiState.isSaved || draftUiState.draftId == null,
                modifier = Modifier.weight(1.2f).minimumInteractiveComponentSize().testTag("visit_draft_save_btn"),
              ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (draftUiState.isSaved) "ذخیره شد" else "ذخیره تغییرات", maxLines = 1)
              }

              OutlinedButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString("${draftUiState.titleFa}\n\n${draftUiState.draftTextFa}"))
                  onFeedback("متن پیش‌نویس ویزیت در کلیپ‌بورد کپی شد.")
                },
                modifier = Modifier.weight(0.9f).minimumInteractiveComponentSize().testTag("visit_draft_copy_btn"),
              ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("کپی")
              }

              OutlinedButton(
                onClick = {
                  val sendIntent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, draftUiState.titleFa)
                      putExtra(Intent.EXTRA_TEXT, "${draftUiState.titleFa}\n\n${draftUiState.draftTextFa}")
                    }
                  runCatching {
                    context.startActivity(Intent.createChooser(sendIntent, "ارسال متنی پیش‌نویس ویزیت برای پزشک"))
                  }
                },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("visit_draft_share_btn"),
              ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("ارسال متن")
              }
            }

            Surface(
              color = cardSurface,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, plumAccent.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth().testTag("visit_draft_pdf_section"),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = plumAccent,
                    modifier = Modifier.size(20.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "خروجی PDF آماده چاپ و اشتراک‌گذاری",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = plumAccent,
                    )
                    Text(
                      text = "تبدیل گزارش هوشمند و داده‌های خط زمانی به فایل PDF و ذخیره در حافظه موقت دستگاه",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                val isPdfBusy = pdfState is VisitDraftPdfState.Generating
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  FilledTonalButton(
                    onClick = onSavePdfToCache,
                    enabled = !isPdfBusy,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("visit_draft_save_pdf_btn"),
                  ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ذخیره PDF در حافظه موقت", maxLines = 1)
                  }

                  OutlinedButton(
                    onClick = onPrintPdf,
                    enabled = !isPdfBusy,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("visit_draft_print_pdf_btn"),
                  ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("چاپ PDF", maxLines = 1)
                  }

                  OutlinedButton(
                    onClick = onSharePdf,
                    enabled = !isPdfBusy,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("visit_draft_share_pdf_btn"),
                  ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک‌گذاری PDF", maxLines = 1)
                  }
                }

                when (pdfState) {
                  is VisitDraftPdfState.Idle -> Unit
                  is VisitDraftPdfState.Generating -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.testTag("visit_draft_pdf_loading"),
                    ) {
                      ManoTurquoiseLoadingRing(
                        size = ManoCircularProgressSize.COMPACT,
                        diameter = 18.dp,
                        strokeWidth = 2.dp,
                        contentDescriptionFa = "در حال ساخت فایل PDF",
                      )
                      Text("در حال ساخت فایل PDF و ذخیره در حافظه موقت...", style = MaterialTheme.typography.labelSmall, color = plumAccent)
                    }
                  }
                  is VisitDraftPdfState.Ready -> {
                    Surface(
                      color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.55f, darkAlpha = 0.16f),
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, successAccent.copy(alpha = 0.35f)),
                      modifier = Modifier.fillMaxWidth().testTag("visit_draft_pdf_cached_banner"),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = "فایل PDF در حافظه موقت ذخیره شد",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = successAccent,
                          )
                          Text(
                            text = "${pdfState.cachedPdf.fileName} • ${pdfState.cachedPdf.pageCount} صفحه (${pdfState.cachedPdf.formattedSizeLabelFa()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        OutlinedButton(
                          onClick = { onOpenCachedPdf(pdfState.cachedPdf) },
                          modifier = Modifier.minimumInteractiveComponentSize().testTag("visit_draft_open_pdf_btn"),
                        ) {
                          Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "باز کردن فایل PDF", modifier = Modifier.size(15.dp))
                          Spacer(Modifier.width(4.dp))
                          Text("باز کردن", maxLines = 1)
                        }
                      }
                    }
                  }
                  is VisitDraftPdfState.Error -> {
                    Surface(
                      color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth().testTag("visit_draft_pdf_error_banner"),
                    ) {
                      Text(
                        text = pdfState.messageFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    HorizontalDivider(color = borderColor)

    Text(
      text = "پیش‌نویس‌های ذخیره‌شده برای ویزیت (${savedDrafts.size})",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = primary,
    )

    if (savedDrafts.isEmpty()) {
      Text(
        text = "هنوز پیش‌نویسی ذخیره نشده است؛ پس از تولید با دستیار هوشمند به صورت خودکار اینجا نگه داشته می‌شود.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("saved_visit_drafts_list"),
      ) {
        savedDrafts.forEach { draft ->
          Surface(
            color = elevatedSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable { onLoadDraft(draft) }
                .minimumInteractiveComponentSize()
                .testTag("saved_visit_draft_item_${draft.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(draft.titleFa, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                  text = "${draft.createdAtLabelFa.ifBlank { "ذخیره‌شده" }} • ${draft.timelineSummaryFa}",
                  style = MaterialTheme.typography.bodySmall,
                  color = primary,
                  maxLines = 2,
                )
                if (draft.symptomContextFa.isNotBlank()) {
                  Text(
                    text = draft.symptomContextFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                  )
                }
              }
              IconButton(
                onClick = { onDeleteDraft(draft) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_visit_draft_${draft.id}"),
              ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف پیش‌نویس ویزیت", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun VisitContextPill(
  text: String,
  accent: Color = MaterialTheme.colorScheme.primary,
) {
  Surface(
    color = accent.copy(alpha = 0.10f),
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = accent,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}
