package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun specialistColumnCount(widthDp: Float, fontScale: Float): Int = when {
  widthDp < 320f || fontScale >= 1.5f -> 1
  widthDp >= 650f && fontScale <= 1.2f -> 3
  else -> 2
}

private data class Specialist(
  val id: String,
  val title: String,
  val description: String,
  val drawable: Int,
  val accent: Color,
  val onClick: () -> Unit,
)

/** Education and navigation only; mascots do not diagnose or approve medication. */
@Composable
fun ManoCharacterFamily(
  onMedicines: () -> Unit,
  onGrowth: () -> Unit,
  onSkin: () -> Unit,
  onNutrition: () -> Unit,
  onCare: () -> Unit,
  onConnect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val specialists = listOf(
    Specialist("drug", "مانو دارو", "راهنمای مصرف ایمن", R.drawable.mano_specialist_drug, Color(0xFF187F77), onMedicines),
    Specialist("growth", "مانو رشد", "رشد و تکامل کودک", R.drawable.mano_specialist_growth, Color(0xFF357AAB), onGrowth),
    Specialist("skin", "مانو پوست", "مراقبت از پوست کودک", R.drawable.mano_specialist_skin, Color(0xFFCA7465), onSkin),
    Specialist("nutrition", "مانو تغذیه", "راهنمای تغذیه کودک", R.drawable.mano_specialist_nutrition, Color(0xFF608843), onNutrition),
    Specialist("care", "مانو مراقبت", "تب و راهنمای علائم", R.drawable.mano_specialist_care, Color(0xFFC47C35), onCare),
    Specialist("connect", "مانو همراه", "پرسش از داروساز", R.drawable.mano_specialist_connect, Color(0xFF7968A6), onConnect),
  )
  Column(
    modifier = modifier.fillMaxWidth().testTag("mano_character_family"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text("با خانواده مانو آشنا شوید", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("راهنمای آموزشی بخش‌های سلامت کودک", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val columns = specialistColumnCount(maxWidth.value, LocalDensity.current.fontScale)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        specialists.chunked(columns).forEach { group ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            group.forEach { specialist ->
              key(specialist.id) {
          Card(
            onClick = specialist.onClick,
            modifier = Modifier.weight(1f).testTag("mano_specialist_${specialist.id}"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
          ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
              Box(
                modifier = Modifier.fillMaxWidth().height(128.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(specialist.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
              ) {
                SpecialistArtwork(specialist)
              }
              Text(specialist.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(specialist.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SpecialistArtwork(specialist: Specialist) {
  val context = LocalContext.current
  // Optional 3D raster artwork is bundled locally, never downloaded at runtime.
  // A native vector is always present so missing artwork cannot block navigation.
  val image by produceState<ImageBitmap?>(null, context, specialist.id) {
    value = withContext(Dispatchers.IO) {
      try {
        context.assets.open("mascots/${specialist.id}.webp").use {
          BitmapFactory.decodeStream(it)?.asImageBitmap()
        }
      } catch (_: IOException) {
        null
      }
    }
  }
  val loaded = image
  val artworkModifier = Modifier.fillMaxWidth().height(128.dp)
  if (loaded != null) {
    Image(bitmap = loaded, contentDescription = null, modifier = artworkModifier, contentScale = ContentScale.Fit)
  } else {
    Image(painter = painterResource(specialist.drawable), contentDescription = null, modifier = artworkModifier, contentScale = ContentScale.Fit)
  }
}
