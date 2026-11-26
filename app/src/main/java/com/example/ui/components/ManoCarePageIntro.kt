package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ManoTheme

/**
 * Shared native CARE page introduction across clinical screens.
 * Uses the canonical white-and-cyan MANO orbital character in a framed medallion.
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
  val colors = ManoTheme.colors
  val dark = colors.isDark
  val surface = if (dark) colors.card else colors.cyan.container
  val ink = colors.ink
  val muted = colors.muted
  val border = colors.cyan.border
  val rimColor = colors.cyan.accent
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
      horizontalArrangement = Arrangement.spacedBy(12.dp),
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
        Box(
          modifier =
            Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(if (dark) colors.elevated else colors.card)
              .border(1.3.dp, rimColor.copy(alpha = if (dark) 0.55f else 0.35f), RoundedCornerShape(18.dp)),
        ) {
          Image(
            painter = painterResource(R.drawable.img_mano_orbit_hero),
            contentDescription = "مانو، شخصیت اصلی برنامه",
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentScale = ContentScale.Fit,
          )
        }
      } else {
        Box(
          modifier = Modifier.size(46.dp).background(accent.copy(alpha = if (dark) 0.24f else 0.14f), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = if (dark) colors.cyan.accent else accent, modifier = Modifier.size(24.dp))
        }
      }
    }
  }
}
