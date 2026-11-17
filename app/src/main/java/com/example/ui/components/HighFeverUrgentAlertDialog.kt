package com.example.ui.components

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.HighFeverClinicalAlert
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

@Composable
fun HighFeverUrgentAlertDialog(
  alert: HighFeverClinicalAlert,
  canRequestSystemNotifications: Boolean = false,
  onRequestSystemNotifications: () -> Unit = {},
  onOpenWhenToSeeDoctor: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val isDark = LocalManoDarkTheme.current
  val urgentAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val urgentSurface = manoTintedSurface(DangerSurface, DarkCoral)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = manoCardSurface(),
      border = BorderStroke(1.5.dp, urgentAccent.copy(alpha = 0.38f)),
      tonalElevation = 6.dp,
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 16.dp)
          .widthIn(max = 440.dp)
          .testTag("high_fever_urgent_dialog"),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Pinned urgent header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            color = urgentSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(48.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "هشدار فوری تب بالا",
                tint = urgentAccent,
                modifier = Modifier.size(28.dp),
              )
            }
          }
          Text(
            text = alert.titleFa,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = urgentAccent,
            modifier = Modifier.weight(1f),
          )
        }

        // Scrollable clinical details & checklist so pinned action buttons are always reachable
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .heightIn(max = 260.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = urgentSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, urgentAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "دمای ثبت‌شده: ${FeverDoseTimelineEngine.formatTempC(alert.triggerTempCelsius)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = urgentAccent,
              )
              Text(
                text = alert.bodyFa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "اقدامات فوری والدین:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
            alert.checklistFa.forEach { step ->
              Text(
                text = "• $step",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        HorizontalDivider(color = manoBorderColor())

        // Pinned footer action buttons
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          if (canRequestSystemNotifications) {
            OutlinedButton(
              onClick = onRequestSystemNotifications,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("high_fever_enable_notif_btn"),
            ) {
              Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("فعال‌سازی اعلان نوار بالای گوشی", style = MaterialTheme.typography.labelMedium)
            }
          }

          Button(
            onClick = {
              val dialIntent = Intent(Intent.ACTION_DIAL, "tel:115".toUri())
              runCatching { context.startActivity(dialIntent) }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = urgentAccent,
              contentColor = if (isDark) DarkBackground else PureWhite,
            ),
            modifier =
              Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("high_fever_call_115_btn"),
          ) {
            Icon(Icons.Rounded.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("تماس با اورژانس ۱۱۵")
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = {
                onDismiss()
                onOpenWhenToSeeDoctor()
              },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("high_fever_doctor_guide_btn"),
            ) {
              Icon(Icons.Rounded.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("راهنمای پزشک", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(
              onClick = onDismiss,
              modifier =
                Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("high_fever_dialog_dismiss_btn"),
            ) {
              Text("متوجه شدم")
            }
          }
        }
      }
    }
  }
}
