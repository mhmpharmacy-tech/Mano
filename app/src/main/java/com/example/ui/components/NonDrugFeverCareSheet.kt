package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSky
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

/**
 * Complete Non-Drug Fever Comfort, Common Myths & Red-Flag Care Card.
 * Accessible both from the <4h prior-dose conflict block and from the syringe result screen.
 */
@Composable
fun NonDrugFeverCareScreen(
  onBackToCountdown: () -> Unit,
  onTalkToPharmacist: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isDark = LocalManoDarkTheme.current
  val spacing = ManoTheme.spacing
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)

  BackHandler(onBack = onBackToCountdown)

  fun dialEmergency115() {
    runCatching {
      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:115")))
    }
  }

  Column(
    modifier = modifier.fillMaxSize().testTag("non_drug_fever_care_screen"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("non_drug_care_list"),
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
      ) {
        // Card Header & Reassurance
        item {
          Surface(
            color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.92f, darkAlpha = 0.22f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth().testTag("non_drug_header_card"),
          ) {
            Column(
              modifier = Modifier.padding(spacing.cardPadding),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Text(
                  text = "تا زمان نوبت بعدی دارو، این کارها به آرام شدن کودک کمک می‌کند",
                  style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = onBackToCountdown,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("non_drug_close_btn"),
                ) {
                  Icon(Icons.Rounded.Close, contentDescription = "بستن کارت مراقبت غیردارویی")
                }
              }
              Text(
                text = "هدف اصلی، راحت‌تر شدن حال کوچولوی شماست—نیازی نیست دما بلافاصله به ۳۷ درجه برسد.",
                style = ManoTheme.typeScale.clinicalBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Section 1: Safe Non-Drug Care
        item {
          SectionCard(modifier = Modifier.testTag("non_drug_safe_section")) {
            Text(
              text = "کارهای مؤثر و بی‌خطر در خانه",
              style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
            )

            SafeCareActionTile(
              icon = Icons.Rounded.WaterDrop,
              accent = MaterialTheme.colorScheme.primary,
              titleFa = "پاشویه با آب ولرم (نه سرد!)",
              bodyFa = "یک دستمال نخی را با آب ولرم (نزدیک به دمای بدن) خیس کنید و به آرامی روی پیشانی، زیر بغل، کشاله ران و شکم بکشید. اگر کودک شروع به لرزیدن کرد، فوراً پاشویه را متوقف کنید.",
            )

            SafeCareActionTile(
              icon = Icons.Rounded.LocalDrink,
              accent = SuccessEmerald,
              titleFa = "جرعه‌جرعه مایعات بدهید",
              bodyFa = "تب باعث از دست رفتن آب بدن می‌شود. اگر کودک شیرخوار است، دفعات شیردهی را بیشتر کنید. برای کودکان بزرگ‌تر، آب، محلول ORS یا مایعات ولرم را کم‌کم و در دفعات زیاد بدهید.",
            )

            SafeCareActionTile(
              icon = Icons.Rounded.Checkroom,
              accent = MaterialTheme.colorScheme.primary,
              titleFa = "لباس سبک و اتاق خنک",
              bodyFa = "یک لایه لباس نخی و آزاد کافی است. دمای اتاق را متعادل نگه دارید و برای «عرق کردن» کودک، روی او پتوی ضخیم نیندازید.",
            )
          }
        }

        // Section 2: Common Myths to Avoid
        item {
          Surface(
            color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.18f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.2.dp, cautionAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().testTag("non_drug_myths_section"),
          ) {
            Column(
              modifier = Modifier.padding(spacing.cardPadding),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AcUnit,
                  contentDescription = null,
                  tint = cautionAccent,
                  modifier = Modifier.size(22.dp),
                )
                Column {
                  Text(
                    text = "لطفاً از این کارها خودداری کنید",
                    style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "هشدار ایمنی",
                    style = ManoTheme.typeScale.microCaption.copy(fontWeight = FontWeight.Bold),
                    color = cautionAccent,
                  )
                }
              }

              HorizontalDivider(color = cautionAccent.copy(alpha = 0.25f))

              Text(
                text = "❌ پاشویه با آب یخ، الکل یا سرکه: الکل از طریق پوست جذب می‌شود و خطرناک است؛ آب سرد هم با ایجاد لرز باعث بالاتر رفتن تب می‌شود.",
                style = ManoTheme.typeScale.clinicalBody,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Text(
                text = "❌ پتو پیچیدن کودک: حتی اگر دست و پای کودک سرد به نظر می‌رسد، پوشاندن لباس زیاد مانع خروج گرما از بدن می‌شود.",
                style = ManoTheme.typeScale.clinicalBody,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        // Section 3: Red-Flag Signs
        item {
          Surface(
            color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.22f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, dangerAccent.copy(alpha = 0.60f)),
            modifier = Modifier.fillMaxWidth().testTag("non_drug_red_flags_section"),
          ) {
            Column(
              modifier = Modifier.padding(spacing.cardPadding),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ReportProblem,
                  contentDescription = null,
                  tint = dangerAccent,
                  modifier = Modifier.size(24.dp),
                )
                Text(
                  text = "چه زمانی منتظر نوبت بعدی نمانیم؟",
                  style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
                  color = dangerAccent,
                )
              }

              Text(
                text = "در صورت مشاهده هرکدام از این علائم، فوراً به پزشک یا اورژانس مراجعه کنید:",
                style = ManoTheme.typeScale.clinicalBody.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
              )

              val redFlags =
                listOf(
                  "بی‌حالی شدید، خواب‌آلودگی غیرعادی یا سخت بیدار شدن کودک",
                  "تنفس تند، سخت یا تو رفتن قفسه سینه هنگام نفس کشیدن",
                  "تشنج (حرکات غیرارادی دست و پا یا خیره شدن چشم‌ها)",
                  "لکه‌های قرمز یا بنفش روی پوست که با فشار انگشت محو نمی‌شوند",
                  "خشکی دهان، گریه بدون اشک یا خیس نکردن پوشک بیش از ۶ تا ۸ ساعت",
                  "گریه مداوم و آرام‌نشدنی یا سفتی گردن",
                )
              redFlags.forEach { flag ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Box(
                    modifier =
                      Modifier
                        .padding(top = 7.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dangerAccent)
                  )
                  Text(
                    text = flag,
                    style = ManoTheme.typeScale.clinicalBody,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
      }
    }

    // Bottom Action Bar
    Surface(
      color = if (isDark) MaterialTheme.colorScheme.surface else PureWhite,
      tonalElevation = 4.dp,
      shadowElevation = 6.dp,
      border = BorderStroke(0.75.dp, manoBorderColor()),
      modifier = Modifier.fillMaxWidth().testTag("non_drug_bottom_action_bar"),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 840.dp)
            .padding(horizontal = spacing.screenHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = ::dialEmergency115,
            colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = PureWhite),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("non_drug_emergency_115_btn"),
          ) {
            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("تماس با اورژانس (۱۱۵)", style = ManoTheme.typeScale.actionLabel)
          }

          OutlinedButton(
            onClick = onTalkToPharmacist,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("non_drug_pharmacist_btn"),
          ) {
            Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("گفت‌وگو با داروساز مانو", style = ManoTheme.typeScale.actionLabel)
          }
        }

        TextButton(
          onClick = onBackToCountdown,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("non_drug_back_countdown_btn"),
        ) {
          Text(
            text = "بازگشت به شمارشگر نوبت بعدی دارو",
            style = ManoTheme.typeScale.actionLabel,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun SafeCareActionTile(
  icon: ImageVector,
  accent: Color,
  titleFa: String,
  bodyFa: String,
) {
  Surface(
    color = manoCardSurface(),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, manoBorderColor()),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f)),
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = titleFa,
          style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = bodyFa,
          style = ManoTheme.typeScale.clinicalBodyCompact,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
