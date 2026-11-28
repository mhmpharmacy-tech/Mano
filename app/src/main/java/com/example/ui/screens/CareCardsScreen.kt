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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ConsultationPayloadMapper
import com.example.domain.model.CareCardDto
import com.example.domain.model.CareCardEventDto
import com.example.domain.util.ManoDomainLinks
import com.example.ui.components.LtrText
import com.example.ui.components.ManoCircularProgressSize
import com.example.ui.components.ManoTurquoiseLoadingRing
import com.example.ui.components.SectionCard
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SoftSage
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.ManoLinkShareHelper
import com.example.ui.util.PharmacyQrCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class CareCardEntryUi(
  val pharmacyId: String,
  val caseId: String,
  val questionSummaryFa: String,
  val status: String,
  val isReviewedAttested: Boolean,
  val card: CareCardDto?,
)

private fun buildEducationalSampleCareCard(): CareCardDto =
  CareCardDto(
    child = "کودک نمونه (۱۴ ماهه)",
    medicine = "قطره استامینوفن کودکان (نمونه ساختار)",
    strength = "۱۰۰ میلی‌گرم در هر ۱ میلی‌لیتر",
    instructions = "فقط بر اساس جدول وزن کودک با سرنگ مدرج دهانی اندازه‌گیری شود. از مصرف هم‌زمان با سایر فرآورده‌های حاوی استامینوفن پرهیز کنید و در محیط خشک نگهداری شود.",
    followUp = "بررسی تب و وضعیت عمومی کودک تا ۲۴ ساعت پس از ثبت راهنما",
    dosageForm = "قطره خوراکی",
    route = "خوراکی با سرنگ مدرج",
    doseAmount = "مطابق توصیه داروساز/وزن کودک",
    frequency = "در صورت نیاز، با فاصله حداقل ۴ تا ۶ ساعت",
    duration = "حداکثر ۴۸ تا ۷۲ ساعت تا ویزیت پزشک",
    precautions = "در نوزاد زیر ۳ ماه با تب ۳۸ درجه یا بیشتر، بی‌حالی شدید، تنگی نفس یا تشنج فوراً با اورژانس ۱۱۵ تماس بگیرید.",
    pharmacyNameFa = "نمونه آموزشی شبکه داروخانه‌های مانو",
    version = 1,
    shareToken = "SAMPLE-MANO-LOOP",
    events =
      listOf(
        CareCardEventDto(id = "s1", action = "send", text = "صدور نمونه ساختار کارت مراقبت", atMs = 0L),
        CareCardEventDto(id = "s2", action = "open", text = "مرور دستور مصرف در خانه", atMs = 0L),
        CareCardEventDto(id = "s3", action = "checkin", text = "پیگیری خانواده و گزارش علائم به داروساز", atMs = 0L),
      ),
    isEducationalSample = true,
  )

@Composable
fun CareCardsScreen(
  uid: String?,
  onAskPharmacist: () -> Unit = {},
  onShowFeedback: (String) -> Unit = {},
) {
  key(uid) {
    CareCardsContent(
      uid = uid,
      onAskPharmacist = onAskPharmacist,
      onShowFeedback = onShowFeedback,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareCardsContent(
  uid: String?,
  onAskPharmacist: () -> Unit,
  onShowFeedback: (String) -> Unit,
) {
  var rows by remember { mutableStateOf(emptyList<CareCardEntryUi>()) }
  var staff by remember { mutableStateOf(false) }
  var refresh by remember { mutableIntStateOf(0) }
  var loading by remember { mutableStateOf(uid != null) }
  var message by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val colors = ManoTheme.colors
  val isDark = LocalManoDarkTheme.current
  val spacing = ManoTheme.spacing
  val forestAccent = if (isDark) OrbitCyan else DeepForest
  val sageAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)

  LaunchedEffect(uid, refresh) {
    if (uid == null) {
      loading = false
      rows = emptyList()
      message = ""
      return@LaunchedEffect
    }
    loading = true
    try {
      val user = FirebaseAuth.getInstance().currentUser ?: run {
        loading = false
        return@LaunchedEffect
      }
      val claims = user.getIdToken(false).await().claims
      staff = claims["manoStaffVerified"] == true && claims["manoStaffRole"] == "pharmacist"
      val name = if (staff) "getPharmacyInbox" else "listParentConsultations"
      val args = if (staff) mapOf("pharmacyId" to claims["manoPharmacyId"], "limit" to 100) else mapOf("limit" to 50)
      val result = FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name).call(args).await().data
      val parsed =
        (result as? List<*>).orEmpty()
          .filterIsInstance<Map<*, *>>()
          .mapNotNull { row ->
            val pharmacyId = (row["pharmacyId"] as? String)?.trim().orEmpty()
            val caseId = (row["caseId"] as? String)?.trim().orEmpty()
            if (pharmacyId.isBlank() || caseId.isBlank()) return@mapNotNull null
            val card = ConsultationPayloadMapper.parseCareCard(row["careCard"] as? Map<*, *>)
            if (!staff && card == null) return@mapNotNull null
            val parentResponse = row["parentResponse"] as? Map<*, *>
            val isReviewed = row["status"] == "REVIEWED" && parentResponse?.get("attestationConfirmed") == true
            CareCardEntryUi(
              pharmacyId = pharmacyId,
              caseId = caseId,
              questionSummaryFa = (row["questionSummaryFa"] as? String).orEmpty(),
              status = (row["status"] as? String).orEmpty(),
              isReviewedAttested = isReviewed,
              card = card,
            )
          }
      rows = parsed
      message = if (parsed.isEmpty()) "هنوز کارت مراقبت فعالی برای این حساب ثبت نشده است." else ""
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      message = "دریافت کارت‌ها از سرور ممکن نشد؛ اتصال اینترنت را بررسی کنید."
    } finally {
      loading = false
    }
  }

  val sampleCard = remember { buildEducationalSampleCareCard() }

  Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("care_cards_list"),
      contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
      item {
        SectionCard(modifier = Modifier.testTag("care_cards_hero")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(62.dp)
                  .clip(CircleShape)
                  .background(if (isDark) colors.elevated else SoftSage)
                  .border(1.2.dp, forestAccent.copy(alpha = 0.35f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                painter = painterResource(id = R.drawable.mano_orbit_calm),
                contentDescription = "ربات مانو کنار کارت مراقبت داروخانه",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(54.dp),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              LtrText(text = "MANO Care Card • Pharmacy ↔ Home Loop", color = forestAccent)
              Text(
                text = "کارت مراقبت مانو",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.ink,
              )
              Text(
                text = "پیوند دیجیتال بین داروساز و خانواده: دستور روشن مصرف، علائم هشدار و خط زمانی پیگیری.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("۱. بررسی داروساز", "۲. صدور کارت مراقبت", "۳. اجرای ایمن در خانه", "۴. پیگیری خانواده").forEach { stage ->
              Surface(
                color = if (isDark) colors.elevated else SoftLavender.copy(alpha = 0.72f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, manoBorderColor()),
              ) {
                Text(
                  text = stage,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = colors.ink,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }

          if (uid == null) {
            Surface(
              color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.9f, darkAlpha = 0.16f),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth().testTag("care_cards_guest_notice"),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "حالت مهمان — نمایش نمونه آموزشی",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = cautionAccent,
                )
                Text(
                  text = "برای مشاهده کارت‌های واقعی صادرشده توسط داروساز و ثبت پیگیری خانواده، از بخش «سؤال از داروساز» یا «فرزند من» با حساب گوگل وارد شوید.",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.ink,
                )
                OutlinedButton(
                  onClick = onAskPharmacist,
                  modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("care_cards_go_pharmacist_btn"),
                ) {
                  Text("ورود و ارتباط با داروساز")
                }
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (staff) "نقش فعال: داروساز تاییدشده" else "نقش فعال: والد / مراقب",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = forestAccent,
              )
              OutlinedButton(
                onClick = { refresh++ },
                enabled = !loading && !busy,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("care_cards_refresh_btn"),
              ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("به‌روزرسانی")
              }
            }
          }
        }
      }

      if (loading) {
        item {
          SectionCard {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              ManoTurquoiseLoadingRing(
                size = ManoCircularProgressSize.COMPACT,
                contentDescriptionFa = "در حال دریافت کارت‌های مراقبت",
              )
              Text("در حال دریافت کارت‌های مراقبت از سرور...", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      if (message.isNotEmpty()) {
        item {
          SectionCard {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
          }
        }
      }

      if (uid == null || (!loading && rows.isEmpty() && !staff)) {
        item(key = "educational_sample_care_card") {
          CareCardPresentation(
            card = sampleCard,
            staff = false,
            busy = false,
            defaultExpanded = true,
            onOpen = {},
            onSubmitAction = { _, _ -> },
            onShowFeedback = onShowFeedback,
            onAskPharmacist = onAskPharmacist,
          )
        }
      }

      itemsIndexed(rows, key = { _, item -> "${item.pharmacyId}_${item.caseId}" }) { _, row ->
        val card = row.card
        var expanded by remember(row.pharmacyId, row.caseId) { mutableStateOf(card != null) }
        val fields = remember(row.pharmacyId, row.caseId, card?.version) {
          mutableStateMapOf<String, String>().apply {
            if (card != null) {
              put("child", card.child)
              put("medicine", card.medicine)
              put("strength", card.strength)
              put("dosageForm", card.dosageForm)
              put("route", card.route)
              put("doseAmount", card.doseAmount)
              put("frequency", card.frequency)
              put("duration", card.duration)
              put("instructions", card.instructions)
              put("precautions", card.precautions)
              put("followUp", card.followUp)
              put("pharmacyNameFa", card.pharmacyNameFa)
            }
          }
        }
        val pending = remember(row.pharmacyId, row.caseId) { mutableMapOf<String, String>() }
        var showReviseEditor by remember(row.pharmacyId, row.caseId) { mutableStateOf(false) }

        fun submit(action: String, values: Map<String, String> = emptyMap()) {
          if (busy) return
          busy = true
          scope.launch {
            try {
              val requestId = pending.getOrPut(action) { UUID.randomUUID().toString() }
              val payload =
                ConsultationPayloadMapper.buildCareCardPayload(
                  pharmacyId = row.pharmacyId,
                  caseId = row.caseId,
                  action = action,
                  requestId = requestId,
                  confirmed = action in setOf("send", "revise", "dose", "close"),
                  child = values["child"],
                  medicine = values["medicine"],
                  strength = values["strength"],
                  instructions = values["instructions"],
                  followUp = values["followUp"],
                  dosageForm = values["dosageForm"],
                  route = values["route"],
                  doseAmount = values["doseAmount"],
                  frequency = values["frequency"],
                  duration = values["duration"],
                  precautions = values["precautions"],
                  pharmacyNameFa = values["pharmacyNameFa"],
                  text = values["text"],
                )
              FirebaseFunctions.getInstance("us-central1").getHttpsCallable("updateCareCard").call(payload).await()
              pending.remove(action)
              showReviseEditor = false
              if (action !in setOf("send", "revise")) fields.remove("text")
              message = ""
              refresh++
            } catch (e: CancellationException) {
              throw e
            } catch (_: Exception) {
              message = "ثبت عملیات روی کارت مراقبت انجام نشد. دوباره تلاش کنید."
            } finally {
              busy = false
            }
          }
        }

        if (card == null && staff) {
          SectionCard(modifier = Modifier.testTag("staff_pending_care_card_${row.caseId}")) {
            Text(
              text = row.questionSummaryFa.ifBlank { "پرونده ${row.caseId}" },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            if (!row.isReviewedAttested) {
              Surface(
                color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.9f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "ارسال کارت مراقبت فقط پس از ثبت پاسخ تأییدشده داروساز در پرونده (وضعیت بررسی‌شده + امضای علمی) امکان‌پذیر است.",
                  style = MaterialTheme.typography.bodySmall,
                  color = cautionAccent,
                  modifier = Modifier.padding(12.dp),
                )
              }
            } else {
              CareCardStaffComposer(
                titleFa = "صدور کارت مراقبت ساختاریافته برای خانواده",
                submitLabelFa = "تأیید و ارسال کارت به خانواده",
                fields = fields,
                busy = busy,
                onSubmit = { submit("send", fields.toMap()) },
              )
            }
          }
        } else if (card != null) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CareCardPresentation(
              card = card,
              staff = staff,
              busy = busy,
              defaultExpanded = expanded,
              onOpen = {
                expanded = true
                if (!staff && card.openedAtMs == null) submit("open")
              },
              onSubmitAction = { action, values -> submit(action, values) },
              onToggleRevise = { showReviseEditor = !showReviseEditor },
              onShowFeedback = onShowFeedback,
              onAskPharmacist = onAskPharmacist,
            )
            if (staff && showReviseEditor && card.closedAtMs == null) {
              SectionCard {
                CareCardStaffComposer(
                  titleFa = "اصلاح و انتشار نسخه ${card.version + 1} کارت مراقبت",
                  submitLabelFa = "ثبت ویرایش جدید کارت مراقبت",
                  fields = fields,
                  busy = busy,
                  onSubmit = { submit("revise", fields.toMap()) },
                )
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
private fun CareCardPresentation(
  card: CareCardDto,
  staff: Boolean,
  busy: Boolean,
  defaultExpanded: Boolean,
  onOpen: () -> Unit,
  onSubmitAction: (String, Map<String, String>) -> Unit,
  onToggleRevise: () -> Unit = {},
  onShowFeedback: (String) -> Unit,
  onAskPharmacist: () -> Unit,
) {
  val context = LocalContext.current
  val colors = ManoTheme.colors
  val isDark = LocalManoDarkTheme.current
  val sageAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)

  var expanded by remember(card.child, card.medicine, card.version) { mutableStateOf(defaultExpanded) }
  var replyOrQuestionText by remember(card.child, card.medicine, card.version) { mutableStateOf("") }
  var confirmDoseReport by remember { mutableStateOf(false) }
  var confirmCloseFollowUp by remember { mutableStateOf(false) }
  var showQrSharePreview by remember { mutableStateOf(false) }

  SectionCard(
    modifier = Modifier.testTag(if (card.isEducationalSample) "care_card_educational_sample" else "care_card_live_card"),
  ) {
    // Provenance & Version Strip
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (card.isEducationalSample) {
        Surface(
          color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.95f, darkAlpha = 0.18f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.45f)),
          modifier = Modifier.testTag("care_card_sample_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = cautionAccent, modifier = Modifier.size(14.dp))
            Text("نمونه آموزشی • بدون نسخه فعال", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = cautionAccent)
          }
        }
      } else {
        Surface(
          color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.95f, darkAlpha = 0.18f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.45f)),
          modifier = Modifier.testTag("care_card_live_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = sageAccent, modifier = Modifier.size(14.dp))
            Text("تأییدشده توسط داروساز", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = sageAccent)
          }
        }
      }

      Surface(
        color = if (isDark) colors.elevated else SoftSage,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, manoBorderColor()),
      ) {
        Text(
          text = "نسخه ${card.version}",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = colors.ink,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      if (card.closedAtMs != null) {
        Surface(
          color = colors.border.copy(alpha = 0.45f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "پیگیری بسته شده",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }

    // Main Card Title
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = card.medicine,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = colors.ink,
        )
        Text(
          text = "${card.child}${card.strength.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()}",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.muted,
        )
        if (card.pharmacyNameFa.isNotBlank()) {
          Text(
            text = "داروخانه: ${card.pharmacyNameFa}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDark) OrbitCyan else DeepForest,
          )
        }
      }
      TextButton(
        onClick = {
          val next = !expanded
          expanded = next
          if (next) onOpen()
        },
      ) {
        Text(if (expanded) "بستن جزئیات" else "مشاهده کارت")
      }
    }

    if (expanded) {
      // Structured Dosage Details Grid
      val specPairs =
        listOf(
          "شکل دارویی" to card.dosageForm,
          "روش مصرف" to card.route,
          "مقدار هر نوبت" to card.doseAmount,
          "فاصله تکرار" to card.frequency,
          "مدت دوره" to card.duration,
          "موعد پیگیری" to card.followUp,
        ).filter { it.second.isNotBlank() }

      if (specPairs.isNotEmpty()) {
        Surface(
          color = colors.background,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, manoBorderColor()),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            specPairs.chunked(2).forEach { pair ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (label, value) ->
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted)
                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = colors.ink)
                  }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
              }
            }
          }
        }
      }

      // Soft Sage Instructions Box
      Surface(
        color = manoTintedSurface(SoftSage, DarkSage, lightAlpha = 0.92f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = "دستور مصرف و مراقبت روزانه",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = sageAccent,
          )
          Text(card.instructions, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
        }
      }

      // Critical Red Precautions Box
      val precautionText =
        card.precautions.ifBlank {
          "در صورت بروز علائم هشدار (تنگی نفس، بی‌حالی شدید، تشنج یا تب ۳۸ درجه در نوزاد زیر ۳ ماه) بلافاصله با اورژانس ۱۱۵ تماس بگیرید."
        }
      Surface(
        color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.92f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, dangerAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = "هشدارهای ایمنی و زمان مراجعه فوری",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = dangerAccent,
          )
          Text(precautionText, style = MaterialTheme.typography.bodySmall, color = colors.ink)
        }
      }

      // Share Token / Family Care Hand-off Preview
      if (card.shareToken.isNotBlank()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { showQrSharePreview = !showQrSharePreview },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showQrSharePreview) "بستن کارت اشتراک" else "پیش‌نمایش کارت اشتراک")
          }
          OutlinedButton(
            onClick = {
              val summary =
                buildString {
                  appendLine(if (card.isEducationalSample) "نمونه آموزشی کارت مراقبت مانو" else "کارت مراقبت مانو کید")
                  appendLine("کودک: ${card.child}")
                  appendLine("فرآورده: ${card.medicine} (${card.strength})")
                  appendLine("دستور: ${card.instructions}")
                  appendLine("پیگیری: ${card.followUp}")
                }
              val copied = ManoLinkShareHelper.copyToClipboard(context, "MANO Care Card", summary, sensitive = true)
              onShowFeedback(if (copied) "خلاصه کارت در حافظه موقت کپی شد؛ با احتیاط به اشتراک بگذارید." else "کپی خلاصه ممکن نشد.")
            },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("کپی خلاصه")
          }
        }
        if (showQrSharePreview) {
          val qrUrl = "${ManoDomainLinks.homeUrl()}/care-card/${card.shareToken}"
          val qrBitmap = remember(qrUrl) { PharmacyQrCard.createQrBitmap(qrUrl) }
          Surface(
            color = colors.background,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, manoBorderColor()),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (qrBitmap != null) {
                Image(
                  bitmap = qrBitmap.asImageBitmap(),
                  contentDescription = "کد QR کارت مراقبت مانو",
                  modifier = Modifier.size(164.dp),
                )
              }
              LtrText(text = "Card Token: ${card.shareToken}", color = colors.ink)
              Text(
                text = "پیش‌نمایش معرفی داخلی مراقب؛ لینک وب تا زمان راه‌اندازی و امضای دامنه فعال نیست.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
              )
            }
          }
        }
      }

      // Timeline Audit Events
      if (card.events.isNotEmpty()) {
        HorizontalDivider(color = manoBorderColor())
        Text(
          text = "سیر مراقبت و پیگیری کارت",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = colors.ink,
        )
        val actionLabels = remember {
          mapOf(
            "send" to "ارسال کارت",
            "revise" to "بازنگری کارت",
            "open" to "مشاهده خانواده",
            "dose" to "گزارش مصرف والد",
            "question" to "پرسش خانواده",
            "reply" to "پاسخ داروساز",
            "checkin" to "پیگیری خانواده",
            "close" to "پایان پیگیری",
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          card.events.forEach { ev ->
            val timeText =
              if (ev.atMs > 0L) {
                DateFormat.getDateTimeInstance(3, 3, Locale.forLanguageTag("fa-IR")).format(Date(ev.atMs))
              } else {
                "الگوی نمونه"
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (ev.action == "close") colors.muted else sageAccent,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = "${actionLabels[ev.action] ?: ev.action} • $timeText${ev.text.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.ink,
              )
            }
          }
        }
      }

      // Live Interaction Controls vs Guest CTA
      if (card.isEducationalSample) {
        Button(
          onClick = onAskPharmacist,
          colors = ButtonDefaults.buttonColors(containerColor = DeepForest, contentColor = PureWhite),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("care_card_sample_ask_btn"),
        ) {
          Text("درخواست کارت اختصاصی از داروساز")
        }
      } else if (card.closedAtMs == null) {
        HorizontalDivider(color = manoBorderColor())
        if (!staff) {
          Button(
            enabled = !busy,
            onClick = { confirmDoseReport = true },
            colors = ButtonDefaults.buttonColors(containerColor = DeepForest, contentColor = PureWhite),
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
          ) {
            Text("گزارش مصرف توسط والد")
          }
          Text(
            text = "این گزارش فقط زمان ثبت را ذخیره می‌کند و جایگزین تأیید مقدار مصرف یا ساعت دقیق درمان توسط داروساز نیست.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
          )
          if (confirmDoseReport) {
            AlertDialog(
              onDismissRequest = { confirmDoseReport = false },
              title = { Text("تأیید گزارش مصرف") },
              text = { Text("آیا مصرف دارو را گزارش می‌کنید؟ این ثبت به‌معنی تأیید مقدار یا ساعت واقعی مصرف توسط داروساز نیست.") },
              confirmButton = {
                TextButton(
                  enabled = !busy,
                  onClick = {
                    confirmDoseReport = false
                    onSubmitAction("dose", emptyMap())
                  },
                ) { Text("ثبت گزارش") }
              },
              dismissButton = { TextButton(onClick = { confirmDoseReport = false }) { Text("انصراف") } },
            )
          }
        }

        OutlinedTextField(
          value = replyOrQuestionText,
          onValueChange = { replyOrQuestionText = it },
          label = { Text(if (staff) "پاسخ یا توصیه تکمیلی داروساز" else "سؤال یا گزارش وضعیت کودک") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        if (staff) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              enabled = !busy && replyOrQuestionText.isNotBlank(),
              onClick = {
                val txt = replyOrQuestionText
                replyOrQuestionText = ""
                onSubmitAction("reply", mapOf("text" to txt))
              },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) { Text("ثبت پاسخ") }
            OutlinedButton(
              enabled = !busy,
              onClick = onToggleRevise,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("ویرایش کارت")
            }
          }
          TextButton(
            enabled = !busy,
            onClick = { confirmCloseFollowUp = true },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
          ) {
            Text("پایان پیگیری و بستن کارت", color = dangerAccent)
          }
          if (confirmCloseFollowUp) {
            AlertDialog(
              onDismissRequest = { confirmCloseFollowUp = false },
              title = { Text("تأیید پایان پیگیری") },
              text = { Text("پس از بستن کارت، ثبت پاسخ یا گزارش جدید ممکن نیست. آیا پیگیری این پرونده به پایان رسیده است؟") },
              confirmButton = {
                TextButton(
                  enabled = !busy,
                  onClick = {
                    confirmCloseFollowUp = false
                    onSubmitAction("close", emptyMap())
                  },
                ) { Text("بستن کارت") }
              },
              dismissButton = { TextButton(onClick = { confirmCloseFollowUp = false }) { Text("انصراف") } },
            )
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              enabled = !busy && replyOrQuestionText.isNotBlank(),
              onClick = {
                val txt = replyOrQuestionText
                replyOrQuestionText = ""
                onSubmitAction("checkin", mapOf("text" to txt))
              },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) { Text("گزارش پیگیری") }
            OutlinedButton(
              enabled = !busy && replyOrQuestionText.isNotBlank(),
              onClick = {
                val txt = replyOrQuestionText
                replyOrQuestionText = ""
                onSubmitAction("question", mapOf("text" to txt))
              },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) { Text("پرسش جدید") }
          }
        }
      }
    }
  }
}

@Composable
private fun CareCardStaffComposer(
  titleFa: String,
  submitLabelFa: String,
  fields: MutableMap<String, String>,
  busy: Boolean,
  onSubmit: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    Text(titleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    val requiredFields =
      listOf(
        "child" to "نام یا شناسه کودک *",
        "medicine" to "نام فرآورده *",
        "strength" to "غلظت یا قدرت *",
        "instructions" to "دستور مصرف روزانه *",
        "followUp" to "موعد پیگیری *",
      )
    val optionalStructuredFields =
      listOf(
        "dosageForm" to "شکل دارویی (مثلاً شربت / قطره)",
        "route" to "روش مصرف (مثلاً خوراکی با سرنگ)",
        "doseAmount" to "مقدار هر نوبت",
        "frequency" to "فاصله تکرار",
        "duration" to "مدت دوره مصرف",
        "precautions" to "هشدارهای ایمنی و Red Flags",
        "pharmacyNameFa" to "نام داروخانه صادرکننده",
      )
    requiredFields.forEach { (key, label) ->
      OutlinedTextField(
        value = fields[key].orEmpty(),
        onValueChange = { fields[key] = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
    optionalStructuredFields.forEach { (key, label) ->
      OutlinedTextField(
        value = fields[key].orEmpty(),
        onValueChange = { fields[key] = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Button(
      enabled = !busy && requiredFields.all { !fields[it.first].isNullOrBlank() },
      onClick = onSubmit,
      colors = ButtonDefaults.buttonColors(containerColor = DeepForest, contentColor = PureWhite),
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
    ) {
      Text(submitLabelFa)
    }
  }
}
