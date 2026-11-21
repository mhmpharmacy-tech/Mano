package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ManoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CareCardsScreen(uid: String?) {
  key(uid) { CareCardsContent(uid) }
}

@Composable
private fun CareCardsContent(uid: String?) {
  var rows by remember { mutableStateOf(emptyList<Map<*, *>>()) }
  var staff by remember { mutableStateOf(false) }
  var refresh by remember { mutableIntStateOf(0) }
  var message by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(uid, refresh) {
    if (uid == null) return@LaunchedEffect
    try {
      val user = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
      val claims = user.getIdToken(false).await().claims
      staff = claims["manoStaffVerified"] == true && claims["manoStaffRole"] == "pharmacist"
      val name = if (staff) "getPharmacyInbox" else "listParentConsultations"
      val args = if (staff) mapOf("pharmacyId" to claims["manoPharmacyId"], "limit" to 100) else mapOf("limit" to 50)
      val result = FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name).call(args).await().data
      rows = (result as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().filter { staff || it["careCard"] != null }
      message = if (rows.isEmpty()) "هنوز کارتی موجود نیست." else ""
    } catch (e: CancellationException) { throw e } catch (e: Exception) { message = "دریافت کارت‌ها ممکن نشد." }
  }
  val spacing = ManoTheme.spacing
  Column(
    Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(spacing.screenContentPadding),
    verticalArrangement = Arrangement.spacedBy(spacing.cardItemGap),
  ) {
    Text("کارت‌های مراقبت", style = MaterialTheme.typography.headlineSmall)
    if (uid == null) Text("از قسمت حساب کاربری وارد شوید.") else TextButton(onClick = { refresh++ }) { Text("به‌روزرسانی") }
    if (message.isNotEmpty()) Text(message)
    rows.forEach { row -> key(row["pharmacyId"], row["caseId"]) {
      val card = row["careCard"] as? Map<*, *>
      var expanded by remember { mutableStateOf(false) }
      val fields = remember { mutableStateMapOf<String,String>() }
      val pending = remember { mutableMapOf<String,String>() }
      var confirmDoseReport by remember { mutableStateOf(false) }
      var confirmCloseFollowUp by remember { mutableStateOf(false) }
      fun submit(action: String, values: Map<String,String> = emptyMap()) {
        if (busy) return
        busy = true
        scope.launch {
          try {
            val args = values + mapOf("pharmacyId" to row["pharmacyId"], "caseId" to row["caseId"], "action" to action,
              "confirmed" to (action == "send" || action == "dose" || action == "close"), "requestId" to pending.getOrPut(action) { UUID.randomUUID().toString() })
            FirebaseFunctions.getInstance("us-central1").getHttpsCallable("updateCareCard").call(args).await()
            pending.remove(action)
            fields.clear()
            refresh++
          } catch (e: CancellationException) { throw e } catch (e: Exception) { message = "ثبت تأیید نشد. دوباره تلاش کنید." }
          finally { busy = false }
        }
      }
      HorizontalDivider()
      TextButton(onClick = { expanded = !expanded
        if (expanded && !staff && card != null && card["openedAt"] == null) submit("open")
      }) { Text(card?.let { "${it["child"]} · ${it["medicine"]}" } ?: row["questionSummaryFa"].toString()) }
      if (expanded && card == null && staff) {
        val reviewed = row["status"] == "REVIEWED" &&
          (row["parentResponse"] as? Map<*, *>)?.get("attestationConfirmed") == true
        if (!reviewed) {
          Text("ارسال کارت مراقبت فقط پس از ثبت پاسخ تأییدشده داروساز در پرونده امکان‌پذیر است.")
        } else {
          val labels = mapOf("child" to "نام کودک", "medicine" to "نام فرآورده", "strength" to "غلظت یا قدرت", "instructions" to "دستور مصرف", "followUp" to "موعد پیگیری")
          labels.forEach { (k,v) -> OutlinedTextField(fields[k].orEmpty(), { fields[k]=it }, label={ Text(v) }) }
          Button(enabled=!busy && labels.keys.all { !fields[it].isNullOrBlank() }, onClick={ submit("send",fields.toMap()) }) { Text("تأیید و ارسال به خانواده") }
        }
      } else if (expanded && card != null) {
        Text("${card["strength"]}\n${card["instructions"]}\nموعد پیگیری: ${card["followUp"]}")
        val labels = mapOf("send" to "ارسال کارت","open" to "مشاهدهٔ خانواده","dose" to "ثبت مصرف","question" to "سؤال خانواده","reply" to "پاسخ داروساز","checkin" to "پیگیری خانواده","close" to "پایان پیگیری")
        (card["events"] as? List<*>)?.filterIsInstance<Map<*,*>>()?.forEach { event ->
          val at = (event["at"] as? Number)?.toLong() ?: 0L
          Text("${labels[event["action"]]} · ${DateFormat.getDateTimeInstance(3,3,Locale.forLanguageTag("fa-IR")).format(Date(at))} ${event["text"]}")
        }
        if (card["closedAt"] == null) {
          if (!staff) {
            Button(enabled=!busy, onClick={ confirmDoseReport = true }) { Text("گزارش مصرف توسط والد") }
            Text("این گزارش فقط زمان ثبت را ذخیره می‌کند، نه مقدار دارو یا زمان واقعی مصرف.", style=MaterialTheme.typography.bodySmall)
            if (confirmDoseReport) AlertDialog(
              onDismissRequest={ confirmDoseReport = false },
              title={ Text("تأیید گزارش مصرف") },
              text={ Text("آیا مصرف دارو را گزارش می‌کنید؟ این ثبت به‌معنی تأیید مقدار یا ساعت واقعی مصرف توسط داروساز نیست.") },
              confirmButton={ TextButton(enabled=!busy, onClick={ confirmDoseReport=false; submit("dose") }) { Text("ثبت گزارش") } },
              dismissButton={ TextButton(onClick={ confirmDoseReport=false }) { Text("انصراف") } },
            )
          }
          OutlinedTextField(fields["text"].orEmpty(),{fields["text"]=it},label={Text(if(staff) "پاسخ به خانواده" else "سؤال یا گزارش پیگیری")})
          val actions = if(staff) listOf("reply") else listOf("question","checkin")
          actions.forEach { a -> Button(enabled=!busy && !fields["text"].isNullOrBlank(),onClick={submit(a,fields.toMap())}) {Text(labels.getValue(a))} }
          if(staff) {
            TextButton(enabled=!busy,onClick={ confirmCloseFollowUp=true }) { Text("پایان پیگیری") }
            if (confirmCloseFollowUp) AlertDialog(
              onDismissRequest={ confirmCloseFollowUp=false },
              title={ Text("تأیید پایان پیگیری") },
              text={ Text("پس از بستن کارت، ثبت پاسخ یا گزارش جدید ممکن نیست. آیا پیگیری این پرونده به پایان رسیده است؟") },
              confirmButton={ TextButton(enabled=!busy, onClick={ confirmCloseFollowUp=false; submit("close") }) { Text("بستن کارت") } },
              dismissButton={ TextButton(onClick={ confirmCloseFollowUp=false }) { Text("انصراف") } },
            )
          }
        }
      }
    } }
  }
}
