package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.DeviceGuide
import com.example.domain.model.MascotPose
import com.example.domain.model.MilestoneItem
import com.example.domain.model.ReviewStatus
import com.example.ui.components.AgeSelectorRow
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ClinicalBulletList
import com.example.ui.components.ClinicalNote
import com.example.ui.components.InteractiveOralSyringeLabCard
import com.example.ui.components.InteractiveThermometerLabCard
import com.example.ui.components.LtrText
import com.example.ui.components.ManoBannerFrame
import com.example.ui.components.ManoFramedIllustration
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.PharmacistCtaCard
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.components.WhoGrowthChartCard
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkSky
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoTintedSurface

@Composable
fun GrowthScreen(
  selectedAgeBand: AgeBand?,
  milestones: List<MilestoneItem>,
  checkedMilestoneIds: Set<String>,
  onSelectAgeBand: (AgeBand?) -> Unit,
  onToggleMilestone: (String) -> Unit,
  onOpenGrowthArticle: () -> Unit,
  onOpen3DWeighingGuide: () -> Unit = {},
  childProfile: ChildProfileDemo? = null,
  savedHeightCm: String = "",
  savedNutritionPlansCount: Int = 0,
  onOpenNutritionAdvisor: () -> Unit = {},
  vaccineProgressSummaryFa: String = "",
  onOpenVaccinesSchedule: () -> Unit = {},
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("growth_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoFramedIllustration(
              resId = R.drawable.mano_growth_orbit,
              contentDescription = "ربات مهربان مانو کید همراه پایش رشد کودک",
              width = 96.dp,
              aspectRatio = 1.0f,
              accentColor = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("رشد و تکامل کودک", style = MaterialTheme.typography.headlineSmall)
                SourceBadge(reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW)
              }
              Text(
                text = "پایش نقاط عطف حرکتی، گفتاری و اجتماعی به همراه یادآوری ویزیت‌های دوره‌ای رشد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.INFO,
          title = "نقاط عطف جایگزین غربالگری تخصصی نیستند",
          body =
            "چک‌لیست رشد صرفاً برای آگاهی و گفت‌وگوی بهتر والدین با پزشک است و هیچ تشخیص یا برچسب‌زنی تکاملی (مانند ADHD یا اختلال یادگیری) انجام نمی‌دهد.",
          actionLabel = "مطالعه مقاله کامل رشد و تکامل",
          onAction = onOpenGrowthArticle,
        )
      }

      item {
        AgeSelectorRow(selectedAgeBand = selectedAgeBand, onSelect = onSelectAgeBand)
      }

      // Educational monthly WHO table interpolation; review pending.
      item {
        WhoGrowthChartCard(
          childProfile = childProfile,
          savedHeightCm = savedHeightCm,
          selectedAgeBand = selectedAgeBand,
          onOpen3DWeighingGuide = onOpen3DWeighingGuide,
        )
      }

      item {
        NutritionGrowthEntryCard(
          childProfile = childProfile,
          selectedAgeBand = selectedAgeBand,
          savedHeightCm = savedHeightCm,
          checkedMilestonesCount = checkedMilestoneIds.size,
          savedPlansCount = savedNutritionPlansCount,
          onOpenNutritionAdvisor = onOpenNutritionAdvisor,
        )
      }

      item {
        SectionCard(modifier = Modifier.testTag("growth_vaccines_entry_card")) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Rounded.Vaccines, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("برنامه واکسیناسیون و یادآور پیش از موعد", style = MaterialTheme.typography.titleMedium)
              Text(
                text =
                  vaccineProgressSummaryFa.ifBlank {
                    "ثبت تاریخ تولد، پیگیری نوبت‌های واکسن و دریافت اعلان پیش از موعد"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Button(
            onClick = onOpenVaccinesSchedule,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("growth_open_vaccines_btn"),
          ) {
            Icon(Icons.Rounded.Vaccines, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("مدیریت برنامه واکسیناسیون")
          }
        }
      }

      items(milestones, key = { it.id }) { item ->
        val checked = item.id in checkedMilestoneIds
        Surface(
          color = manoCardSurface(),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else manoBorderColor()),
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable { onToggleMilestone(item.id) }
              .minimumInteractiveComponentSize()
              .testTag("milestone_item_${item.id}"),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = checked, onCheckedChange = { onToggleMilestone(item.id) })
              Spacer(Modifier.width(6.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("${item.ageBand.labelFa} • ${item.domainFa}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(item.descriptionFa, style = MaterialTheme.typography.bodyMedium)
              }
            }
            Text(
              text = "نکته پیگیری با پزشک: ${item.watchOutFa}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun SkinScreen(
  onNavigateSymptomDiaper: () -> Unit,
  onOpenDiaperArticle: () -> Unit,
  onNavigateAskPharmacist: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoFramedIllustration(
              resId = R.drawable.mano_pose_caution,
              contentDescription = "ربات مهربان مانو کید همراه مراقبت پوست کودک",
              width = 96.dp,
              aspectRatio = 1.0f,
              accentColor = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("پوست کودک و مراقبت پوشک", style = MaterialTheme.typography.headlineSmall)
                SourceBadge(reviewStatus = ReviewStatus.APPROVED)
              }
              Text(
                text = "آموزش مراقبت پوست خشک، حساس و سوختگی پوشک با تفکیک کامل از تبلیغات فروشگاهی.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        SectionCard {
          Text("اصول مراقبت از سوختگی پای نوزاد", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          ClinicalBulletList(
            items = listOf(
              "تعویض سریع پوشک خیس یا کثیف و دادن زمان کوتاه هواخوری روزانه.",
              "شست‌وشو با آب ولرم و خشک کردن ملایم بدون کشیدن حوله یا دستمال مرطوب عطردار.",
              "لایه محافظ زینک اکساید ساده بدون عطر؛ پرهیز از کرم‌های کورتونی قوی بدون تجویز پزشک.",
            ),
            bulletColor = MaterialTheme.colorScheme.primary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = onOpenDiaperArticle,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("skin_diaper_article_btn"),
            ) {
              Text("مقاله سوختگی پوشک")
            }
            OutlinedButton(
              onClick = onNavigateSymptomDiaper,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Text("بررسی علائم پوشک")
            }
          }
        }
      }

      item {
        SectionCard {
          Text("انتخاب محصولات بهداشتی پوست حساس کودک", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          ClinicalBulletList(
            items = listOf(
              "ترجیح شوینده‌های ملایم بدون اسانس (Fragrance-free) و فاقد الکل.",
              "حمام کوتاه ۵ تا ۱۰ دقیقه‌ای با آب ولرم و مرطوب‌سازی بلافاصله پس از حمام.",
            ),
            bulletColor = MaterialTheme.colorScheme.primary,
          )
        }
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "چه زمانی ضایعه پوستی نیازمند معاینه فوری است؟",
          body = "وجود تب همراه بثورات پوستی، تاول یا ترشح چرکی، قرمزی نقطه‌ای در چین‌ها یا دانه‌های بنفش/قرمزی که با فشار محو نمی‌شوند.",
          showCautionMascot = true,
        )
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.INFO,
          title = "مرز شفاف آموزش و فروشگاه",
          body = "محتوای آموزشی و هشدارهای پوست در مانو کاملاً مستقل از فروشگاه آینده است و مشاهده هیچ راهنمایی منوط به خرید محصول نیست.",
        )
      }

      item {
        PharmacistCtaCard(
          title = "پرسش درباره کرم محافظ یا پوست حساس",
          onClick = onNavigateAskPharmacist,
        )
      }
    }
  }
}

@Composable
fun DevicesScreen(
  guides: List<DeviceGuide>,
  onNavigateSymptoms: () -> Unit,
  childProfile: ChildProfileDemo? = null,
  childBirthDateEpochMs: Long? = null,
  onNavigate3DStudio: () -> Unit = {},
  onOpen3DWithDose: (Float) -> Unit = {},
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("devices_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SectionCard {
          ManoBannerFrame(
            resId = R.drawable.img_devices_kit,
            contentDescription = "نمای استودیویی مانو در کنار نبولایزر، اسپیسر، دماسنج و سرنگ mL کودک",
            aspectRatio = 1.34f,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoPoseAvatar(pose = MascotPose.EXPLAIN, size = 64.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("وسایل مراقبت از کودک و ابزارهای آموزشی", style = MaterialTheme.typography.headlineSmall)
              Text(
                text = "سرنگ مدرج تعاملی (سن و وزن) و مقیاس دماسنج دیجیتال، به همراه راهنمای ایمن نبولایزر، اسپیسر و دستگاه بخور.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          OutlinedButton(
            onClick = onNavigate3DStudio,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("devices_open_3d_btn"),
          ) {
            Text("نمای سه‌بعدی تعاملی میز مراقبت و سرنگ (3D)")
          }
        }
      }

      item {
        InteractiveOralSyringeLabCard(
          childProfile = childProfile,
          childBirthDateEpochMs = childBirthDateEpochMs,
          onOpen3DWithDose = onOpen3DWithDose,
        )
      }

      item {
        InteractiveThermometerLabCard()
      }

      items(guides, key = { it.id }) { guide ->
        SectionCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(guide.titleFa, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            SourceBadge(reviewStatus = guide.review.status)
          }
          Text(guide.subtitleFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Surface(color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 1f, darkAlpha = 0.16f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              text = guide.distinctionNoteFa,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(12.dp),
            )
          }
          ClinicalBulletList(
            items = guide.stepsFa,
            bulletColor = MaterialTheme.colorScheme.primary,
          )
          SeverityAlert(
            severity = AlertSeverity.CAUTION,
            title = "نکات ایمنی مهم",
            body = guide.cautionNotesFa.joinToString("\n\n• ", prefix = "• "),
          )
        }
      }

      item {
        ClinicalNote(
          title = "نیاز به بررسی علائم دارید؟",
          items = listOf("ابزارهای خانگی فقط کمک‌کننده‌اند؛ در تنفس سخت یا تب نوزاد، هدایتگر علائم را شروع کنید."),
          sources = listOf(ManoSourcesAndClinicalSeed.srcAapMedication, ManoSourcesAndClinicalSeed.srcNiceFever),
        )
      }

      item {
        OutlinedButton(
          onClick = onNavigateSymptoms,
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        ) {
          Text("رفتن به هدایتگر علائم کودک")
        }
      }
    }
  }
}
