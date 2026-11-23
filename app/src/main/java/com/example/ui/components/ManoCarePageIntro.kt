package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalManoDarkTheme

/**
 * Shared native CARE page introduction across the real clinical screens.
 * Keeps one small, canonical brand mascot (never AI-reimagined) or one
 * distinct section icon; business/clinical data is deliberately absent.
 */
@Composable
fun ManoCarePageIntro(
  titleFa: String,
  descriptionFa: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
  withMascot: Boolean = true,
) {
  val dark = LocalManoDarkTheme.current
  val surface = if (dark) Color(0xFF243731) else Color(0xFFF1F8F2)
  val ink = if (dark) Color(0xFFF3F9F6) else Color(0xFF103C31)
  val muted = if (dark) Color(0xFFC7D8CF) else Color(0xFF52665E)
  val border = if (dark) Color(0xFF3F6255) else Color(0xFFD6E9DD)
  Surface(
    color = surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, border),
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          titleFa,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = ink,
        )
        Text(
          descriptionFa,
          style = MaterialTheme.typography.bodySmall,
          color = muted,
        )
      }
      if (withMascot) {
        Image(
          painter = painterResource(R.drawable.mano_hero),
          contentDescription = "مانو، شخصیت اصلی برنامه",
          modifier = Modifier.size(68.dp),
          contentScale = ContentScale.Fit,
        )
      } else {
        Box(
          modifier = Modifier.size(46.dp).background(accent.copy(alpha = if (dark) 0.24f else 0.12f), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = if (dark) Color(0xFF86D8B9) else accent, modifier = Modifier.size(24.dp))
        }
      }
    }
  }
}
