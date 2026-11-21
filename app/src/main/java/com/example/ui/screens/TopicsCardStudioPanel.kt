package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import com.example.ui.components.ManoCircularProgressSize
import com.example.ui.components.ManoTurquoiseLoadingRing
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AlertSeverity
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.DarkSky
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoTintedSurface
import com.example.ui.util.CardShareHelper
import com.example.ui.viewmodel.GeneratedCareCard
import com.example.ui.viewmodel.ImageStudioUiState
import com.example.ui.viewmodel.ManoImageStudioViewModel

@Composable
fun TopicsCardStudioPanel(
  studioViewModel: ManoImageStudioViewModel,
  onOpen3DStudio: () -> Unit,
  onShowFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()
  val pickedReferenceBitmap by studioViewModel.pickedReferenceBitmap.collectAsStateWithLifecycle()

  var promptText by remember { mutableStateOf(studioViewModel.presets.first().promptFa) }
  var editInstructionText by remember { mutableStateOf("") }
  var activeTitleHint by remember { mutableStateOf(studioViewModel.presets.first().titleFa) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded: Bitmap? = CardShareHelper.decodeScaledBitmap(context, uri)
        if (decoded != null) {
          studioViewModel.setPickedReferenceBitmap(decoded)
        } else {
          onShowFeedback("خواندن تصویر انتخاب‌شده ممکن نشد.")
        }
      }
    }

  val isLoading = uiState is ImageStudioUiState.Loading
  val activeCard: GeneratedCareCard? =
    (uiState as? ImageStudioUiState.Success)?.activeCard ?: uiState.gallery.firstOrNull()

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("topics_ai_studio_list"),
    contentPadding = ManoTheme.spacing.screenContentPadding,
    verticalArrangement = Arrangement.spacedBy(ManoTheme.spacing.sectionGap),
  ) {
    item {
      SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("استودیو و گالری کارت آموزشی کودک", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "با هوش مصنوعی کارت‌های تصویری شاد برای روتین خواب، مسواک، شستن دست‌ها یا آموزش کودک بسازید و ویرایش کنید.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item {
      SectionCard {
        Text("پیشنهادهای آماده کودک‌پسند", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(studioViewModel.presets, key = { it.id }) { preset ->
            FilterChip(
              selected = activeTitleHint == preset.titleFa,
              onClick = {
                activeTitleHint = preset.titleFa
                promptText = preset.promptFa
              },
              label = { Text(preset.titleFa) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("preset_chip_${preset.id}"),
            )
          }
        }

        OutlinedTextField(
          value = promptText,
          onValueChange = {
            promptText = it
            activeTitleHint = ""
          },
          label = { Text("توضیح کارت آموزشی یا تغییر دلخواه") },
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("ai_studio_prompt_input"),
        )

        if (pickedReferenceBitmap != null) {
          Surface(
            color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 1f, darkAlpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, manoBorderColor()),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                color = manoCardSurface(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, manoBorderColor()),
                modifier = Modifier.size(58.dp),
              ) {
                Image(
                  bitmap = pickedReferenceBitmap!!.asImageBitmap(),
                  contentDescription = "تصویر مرجع انتخاب‌شده",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              Text(
                text = "تصویر مرجع آماده ویرایش است.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = { studioViewModel.setPickedReferenceBitmap(null) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_picked_photo_btn"),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "حذف تصویر مرجع")
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            enabled = !isLoading,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("pick_reference_photo_btn"),
          ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("انتخاب عکس")
          }
          Button(
            onClick = { studioViewModel.generateCard(promptText, activeTitleHint) },
            enabled = !isLoading,
            modifier = Modifier.weight(1.3f).minimumInteractiveComponentSize().testTag("generate_ai_card_btn"),
          ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (pickedReferenceBitmap != null) "ویرایش عکس با هوش مصنوعی" else "ساخت کارت تصویری")
          }
        }
      }
    }

    when (val state = uiState) {
      is ImageStudioUiState.Loading -> {
        item {
          SectionCard {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp).testTag("ai_studio_loading"),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ManoTurquoiseLoadingRing(
                  size = ManoCircularProgressSize.MEDIUM,
                  contentDescriptionFa = state.statusTextFa,
                )
                Text(
                  text = state.statusTextFa,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
      is ImageStudioUiState.Error -> {
        item {
          SeverityAlert(
            severity = AlertSeverity.CAUTION,
            title = "خطا در سرویس ساخت تصویر",
            body = state.messageFa,
          )
        }
      }
      else -> Unit
    }

    if (activeCard != null) {
      item {
        SectionCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = activeCard.titleFa,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = {
                CardShareHelper.shareCareCard(context, activeCard.bitmap, activeCard.titleFa)
                  .onFailure { onShowFeedback("اشتراک‌گذاری کارت ممکن نشد.") }
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("share_active_card_btn"),
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("اشتراک‌گذاری")
            }
          }

          Surface(
            color = manoCardSurface(),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, manoBorderColor()),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
          ) {
            Image(
              bitmap = activeCard.bitmap.asImageBitmap(),
              contentDescription = activeCard.titleFa,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().testTag("active_generated_card_image"),
            )
          }

          OutlinedTextField(
            value = editInstructionText,
            onValueChange = { editInstructionText = it },
            label = { Text("ویرایش همین کارت (مثلاً: پس‌زمینه آبی ملایم کن)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_active_card_input"),
          )

          Button(
            onClick = { studioViewModel.editActiveCard(editInstructionText) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("edit_active_card_btn"),
          ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("ویرایش همین تصویر")
          }
        }
      }
    }

    if (uiState.gallery.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("گالری کارت‌های ساخته‌شده (${uiState.gallery.size})", style = MaterialTheme.typography.titleSmall)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("ai_studio_gallery_row"),
          ) {
            items(uiState.gallery, key = { it.id }) { card ->
              val isSelected = activeCard?.id == card.id
              Surface(
                color = manoCardSurface(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else manoBorderColor()),
                modifier =
                  Modifier
                    .size(104.dp)
                    .clickable { studioViewModel.selectCardFromGallery(card.id) }
                    .testTag("gallery_card_${card.id}"),
              ) {
                Image(
                  bitmap = card.bitmap.asImageBitmap(),
                  contentDescription = card.titleFa,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
          }
        }
      }
    }

    item {
      SeverityAlert(
        severity = AlertSeverity.INFO,
        title = "مرز ایمنی تصاویر هوش مصنوعی",
        body = "تصاویر ساخته‌شده صرفاً برای آموزش عادت‌های کودکانه (خواب، شستن دست‌ها، آرامش) است و جایگزین توصیه داروساز یا پزشک نیست.",
      )
      Spacer(Modifier.height(12.dp))
    }
  }
}
