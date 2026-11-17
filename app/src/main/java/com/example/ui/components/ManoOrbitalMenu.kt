package com.example.ui.components

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.domain.model.ReviewStatus
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkOnPrimary
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.LightMint
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.TealGlow
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoElevatedSurface
import com.example.ui.theme.manoSemanticAccent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

enum class GalacticRing(val labelFa: String) {
  INNER("مدار اصلی"),
  OUTER("مدار دوم"),
}

/**
 * Hero centerpiece showcasing MANO inside a Dual-Ring Galactic Orbit encircling MANO's head,
 * featuring continuous counter-rotating motion, fluid horizontal finger scrubbing with velocity
 * inertia, magnetic spring snapping onto the zenith crown, and full Day/Night theme adaptation.
 */
@Composable
fun ManoOrbitalHeroStage(
  topics: List<RadialTopicItem>,
  onSelectTopic: (RadialTopicItem) -> Unit,
  modifier: Modifier = Modifier,
  outerTopics: List<RadialTopicItem> = emptyList(),
  hasPlayedIntro: Boolean = false,
  onIntroPlayed: () -> Unit = {},
  savedRing: GalacticRing? = null,
  savedTopicId: String? = null,
  onPersistSelection: (GalacticRing, String) -> Unit = { _, _ -> },
) {
  val (innerList, outerList) =
    remember(topics, outerTopics) {
      when {
        outerTopics.isNotEmpty() -> topics to outerTopics
        topics.size > 6 -> {
          val splitAt = (topics.size + 1) / 2
          topics.take(splitAt) to topics.drop(splitAt)
        }
        else -> topics to emptyList()
      }
    }

  if (innerList.isEmpty()) return
  val hasDualRings = outerList.isNotEmpty()

  val isDark = LocalManoDarkTheme.current
  val heroShape = RoundedCornerShape(28.dp)
  val primaryGlow = MaterialTheme.colorScheme.primary
  val goldAccent = manoSemanticAccent(AccentGold, DarkGold)
  val skyAccent = manoSemanticAccent(DeepTeal, DarkSky)

  val innerCount = innerList.size
  val outerCount = outerList.size.coerceAtLeast(1)

  val scope = rememberCoroutineScope()
  val animatorsEnabled = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ValueAnimator.areAnimatorsEnabled() else true
  }
  val shouldRunIntro = remember { !hasPlayedIntro && animatorsEnabled }

  val mascotAlpha = remember { Animatable(if (shouldRunIntro) 0f else 1f) }
  val mascotScale = remember { Animatable(if (shouldRunIntro) 0.68f else 1f) }
  val mascotOffsetYDp = remember { Animatable(if (shouldRunIntro) 26f else 0f) }
  val orbitTrackAlpha = remember { Animatable(if (shouldRunIntro) 0f else 1f) }
  val orbitExpand = remember { Animatable(if (shouldRunIntro) 0.24f else 1f) }

  val tapBounceScale = remember { Animatable(1f) }
  val tapBounceOffsetYDp = remember { Animatable(0f) }
  val tapHaloPulse = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    if (shouldRunIntro) {
      onIntroPlayed()
      launch { mascotAlpha.animateTo(1f, tween(durationMillis = 190, easing = FastOutSlowInEasing)) }
      launch {
        mascotOffsetYDp.animateTo(
          targetValue = 0f,
          animationSpec =
            spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessLow,
            ),
        )
      }
      launch {
        mascotScale.animateTo(
          targetValue = 1f,
          animationSpec = spring(dampingRatio = 0.56f, stiffness = 240f),
        )
      }
      launch {
        orbitTrackAlpha.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 280, delayMillis = 140, easing = FastOutSlowInEasing),
        )
      }
      launch {
        orbitExpand.animateTo(
          targetValue = 1f,
          animationSpec = spring(dampingRatio = 0.66f, stiffness = 200f),
        )
      }
    } else if (!hasPlayedIntro) {
      onIntroPlayed()
    }
  }

  fun resolveSavedTarget(ringHint: GalacticRing?, topicId: String?): Pair<GalacticRing, Int>? {
    if (topicId.isNullOrBlank()) return null
    if (ringHint == GalacticRing.OUTER && hasDualRings) {
      val idx = outerList.indexOfFirst { it.id == topicId }
      if (idx >= 0) return GalacticRing.OUTER to idx
    }
    if (ringHint == GalacticRing.INNER) {
      val idx = innerList.indexOfFirst { it.id == topicId }
      if (idx >= 0) return GalacticRing.INNER to idx
    }
    val innerFallback = innerList.indexOfFirst { it.id == topicId }
    if (innerFallback >= 0) return GalacticRing.INNER to innerFallback
    if (hasDualRings) {
      val outerFallback = outerList.indexOfFirst { it.id == topicId }
      if (outerFallback >= 0) return GalacticRing.OUTER to outerFallback
    }
    return null
  }

  val initialSavedTarget = remember { resolveSavedTarget(savedRing, savedTopicId) }
  var userInteracted by remember { mutableStateOf(false) }
  var hasAppliedSavedRestore by remember { mutableStateOf(initialSavedTarget != null) }

  var isAutoRotating by remember { mutableStateOf(initialSavedTarget == null) }
  var activeRing by remember { mutableStateOf(initialSavedTarget?.first ?: GalacticRing.INNER) }

  // Continuous step progress for both galactic rings; Animatable gives fluid drag + spring magnetic snap
  val innerPhase =
    remember {
      Animatable(if (initialSavedTarget?.first == GalacticRing.INNER) initialSavedTarget.second.toFloat() else 0f)
    }
  val outerPhase =
    remember {
      Animatable(if (initialSavedTarget?.first == GalacticRing.OUTER) initialSavedTarget.second.toFloat() else 0f)
    }

  LaunchedEffect(savedRing, savedTopicId) {
    if (!userInteracted && !hasAppliedSavedRestore) {
      val target = resolveSavedTarget(savedRing, savedTopicId)
      if (target != null) {
        hasAppliedSavedRestore = true
        val (targetRing, targetIdx) = target
        activeRing = targetRing
        isAutoRotating = false
        if (targetRing == GalacticRing.INNER || !hasDualRings) {
          innerPhase.snapTo(targetIdx.toFloat())
        } else {
          outerPhase.snapTo(targetIdx.toFloat())
        }
      }
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "galacticOrbitSpin")
  val innerAutoDrift by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = innerCount.toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 24_000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "innerAutoDrift",
    )

  val outerAutoDrift by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = -outerCount.toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 32_000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "outerAutoDrift",
    )

  val breatheScale by
    infiniteTransition.animateFloat(
      initialValue = 0.99f,
      targetValue = 1.028f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "manoBreatheScale",
    )

  val breatheFloatDp by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = -3.5f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "manoBreatheFloat",
    )

  val activeAutoMultiplier = if (isAutoRotating && animatorsEnabled) 1f else 0f
  val innerContinuous = innerPhase.value + innerAutoDrift * activeAutoMultiplier
  val outerContinuous = outerPhase.value + outerAutoDrift * activeAutoMultiplier

  val innerCrownStep = innerContinuous.roundToInt()
  val outerCrownStep = outerContinuous.roundToInt()
  val innerActiveIndex = ((innerCrownStep % innerCount) + innerCount) % innerCount
  val outerActiveIndex = ((outerCrownStep % outerCount) + outerCount) % outerCount

  val activeRingList = if (activeRing == GalacticRing.INNER || !hasDualRings) innerList else outerList
  val activeIndex = if (activeRing == GalacticRing.INNER || !hasDualRings) innerActiveIndex else outerActiveIndex
  val activeTopic = activeRingList[activeIndex]
  val activeAccent = activeTopic.accentRole.resolveAccentColor()

  val animatedAccent by animateColorAsState(
    targetValue = activeAccent,
    animationSpec = tween(durationMillis = 260),
    label = "activeGalacticAccent",
  )

  val snapSpring = remember {
    spring<Float>(dampingRatio = 0.72f, stiffness = 260f)
  }

  fun freezeAutoRotationToCurrentPhase() {
    val frozenInner = innerContinuous
    val frozenOuter = outerContinuous
    isAutoRotating = false
    scope.launch {
      innerPhase.snapTo(frozenInner)
      if (hasDualRings) outerPhase.snapTo(frozenOuter)
    }
  }

  fun magneticSnapActiveRing(extraSteps: Float = 0f, targetRing: GalacticRing = activeRing) {
    val currentInner = innerContinuous
    val currentOuter = outerContinuous
    isAutoRotating = false
    val resolvedRing = if (targetRing == GalacticRing.OUTER && hasDualRings) GalacticRing.OUTER else GalacticRing.INNER
    val snappedTopicId =
      if (resolvedRing == GalacticRing.INNER) {
        val snapTargetInt = (currentInner + extraSteps).roundToInt()
        val wrappedIdx = ((snapTargetInt % innerCount) + innerCount) % innerCount
        innerList[wrappedIdx].id
      } else {
        val snapTargetInt = (currentOuter + extraSteps).roundToInt()
        val wrappedIdx = ((snapTargetInt % outerCount) + outerCount) % outerCount
        outerList[wrappedIdx].id
      }
    onPersistSelection(resolvedRing, snappedTopicId)
    scope.launch {
      if (resolvedRing == GalacticRing.INNER) {
        val snapTarget = (currentInner + extraSteps).roundToInt().toFloat()
        launch { innerPhase.snapTo(currentInner); innerPhase.animateTo(snapTarget, snapSpring) }
        if (hasDualRings) {
          val companionSnap = currentOuter.roundToInt().toFloat()
          launch { outerPhase.snapTo(currentOuter); outerPhase.animateTo(companionSnap, snapSpring) }
        }
      } else {
        val snapTarget = (currentOuter + extraSteps).roundToInt().toFloat()
        launch { outerPhase.snapTo(currentOuter); outerPhase.animateTo(snapTarget, snapSpring) }
        val companionSnap = currentInner.roundToInt().toFloat()
        launch { innerPhase.snapTo(currentInner); innerPhase.animateTo(companionSnap, snapSpring) }
      }
    }
  }

  fun stepActiveRingBy(deltaSteps: Int) {
    userInteracted = true
    val resolvedRing = if (activeRing == GalacticRing.OUTER && hasDualRings) GalacticRing.OUTER else GalacticRing.INNER
    val ringList = if (resolvedRing == GalacticRing.INNER) innerList else outerList
    val ringCount = ringList.size
    val base = if (resolvedRing == GalacticRing.INNER) innerCrownStep else outerCrownStep
    val targetStep = base + deltaSteps
    val target = targetStep.toFloat()
    val wrappedIdx = ((targetStep % ringCount) + ringCount) % ringCount
    onPersistSelection(resolvedRing, ringList[wrappedIdx].id)
    val currentInner = innerContinuous
    val currentOuter = outerContinuous
    isAutoRotating = false
    scope.launch {
      if (resolvedRing == GalacticRing.INNER) {
        innerPhase.snapTo(currentInner)
        innerPhase.animateTo(target, snapSpring)
      } else {
        outerPhase.snapTo(currentOuter)
        outerPhase.animateTo(target, snapSpring)
      }
    }
  }

  fun nearestWrappedDelta(index: Int, continuousStep: Float, itemCount: Int): Float {
    val raw = index - continuousStep
    val half = itemCount / 2f
    var wrapped = ((raw % itemCount) + itemCount) % itemCount
    if (wrapped > half) wrapped -= itemCount
    return wrapped
  }

  fun focusSatellite(ring: GalacticRing, index: Int, item: RadialTopicItem) {
    userInteracted = true
    onPersistSelection(ring, item.id)
    val ringCount = if (ring == GalacticRing.INNER) innerCount else outerCount
    val currentCont = if (ring == GalacticRing.INNER) innerContinuous else outerContinuous
    val delta = nearestWrappedDelta(index, currentCont, ringCount)
    val alreadyCrownActive = activeRing == ring && abs(delta) < 0.36f
    if (alreadyCrownActive) {
      onSelectTopic(item)
      return
    }
    activeRing = ring
    val currentInner = innerContinuous
    val currentOuter = outerContinuous
    isAutoRotating = false
    scope.launch {
      if (ring == GalacticRing.INNER) {
        innerPhase.snapTo(currentInner)
        innerPhase.animateTo(currentInner + delta, snapSpring)
      } else {
        outerPhase.snapTo(currentOuter)
        outerPhase.animateTo(currentOuter + delta, snapSpring)
      }
    }
  }

  val cardColor = manoCardSurface()
  val elevatedSurface = manoElevatedSurface()
  val borderColor = manoBorderColor()
  val cardBorder = if (isDark) animatedAccent.copy(alpha = 0.36f) else borderColor
  val onAccentText = if (isDark) DarkOnPrimary else PureWhite

  Card(
    shape = heroShape,
    border = BorderStroke(1.dp, cardBorder),
    colors = CardDefaults.cardColors(containerColor = cardColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 12.dp,
          shape = heroShape,
          ambientColor = if (isDark) animatedAccent.copy(alpha = 0.22f) else ShadowSlate.copy(alpha = 0.14f),
          spotColor = if (isDark) animatedAccent.copy(alpha = 0.18f) else ShadowSlate.copy(alpha = 0.12f),
        )
        .testTag("home_hero_card"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(cardColor)
          .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top Trust & Welcome Header + Galactic Spin Toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SourceBadge(reviewStatus = ReviewStatus.APPROVED)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = if (isDark) primaryGlow.copy(alpha = 0.18f) else LightMint.copy(alpha = 0.88f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, primaryGlow.copy(alpha = 0.30f)),
          ) {
            Text(
              text = stringResource(R.string.welcome_badge),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Surface(
            onClick = {
              userInteracted = true
              if (isAutoRotating) {
                magneticSnapActiveRing()
              } else {
                val baseInner = innerPhase.value - innerAutoDrift
                val baseOuter = outerPhase.value - outerAutoDrift
                scope.launch {
                  innerPhase.snapTo(baseInner)
                  if (hasDualRings) outerPhase.snapTo(baseOuter)
                }
                isAutoRotating = true
              }
            },
            color = elevatedSurface,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.42f)),
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = if (isAutoRotating) "توقف چرخش مدار" else "شروع چرخش مدار" }
                .testTag("orbital_toggle_spin"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector = if (isAutoRotating) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = if (isAutoRotating) "توقف" else "چرخش",
                style = MaterialTheme.typography.labelSmall,
                color = animatedAccent,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      Text(
        text = stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Dual-Ring Galactic Orbital Stage Encircling MANO's Head
      BoxWithConstraints(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(if (hasDualRings) 280.dp else 244.dp)
            .pointerInput(innerCount, outerCount, hasDualRings) {
              val dragPxPerStep = 104f
              val velocityTracker = VelocityTracker()
              detectHorizontalDragGestures(
                onDragStart = {
                  userInteracted = true
                  velocityTracker.resetTracking()
                  freezeAutoRotationToCurrentPhase()
                },
                onDragEnd = {
                  val vx = velocityTracker.calculateVelocity().x
                  val flingSteps = ((-vx / dragPxPerStep) * 0.16f).coerceIn(-2.2f, 2.2f)
                  magneticSnapActiveRing(extraSteps = flingSteps)
                },
                onDragCancel = {
                  magneticSnapActiveRing()
                },
              ) { change, dragAmount ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                val deltaStep = -dragAmount / dragPxPerStep
                scope.launch {
                  if (activeRing == GalacticRing.INNER || !hasDualRings) {
                    innerPhase.snapTo(innerPhase.value + deltaStep)
                    if (hasDualRings) {
                      outerPhase.snapTo(outerPhase.value + deltaStep * 0.35f)
                    }
                  } else {
                    outerPhase.snapTo(outerPhase.value + deltaStep)
                    innerPhase.snapTo(innerPhase.value + deltaStep * 0.35f)
                  }
                }
              }
            }
            .testTag("home_orbital_stage"),
      ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val centerX = widthPx / 2f
        val orbitCenterY = heightPx * 0.54f

        val safePadX = with(density) { 46.dp.toPx() }
        val safeTopPad = with(density) { 48.dp.toPx() }
        val safeBottomPad = with(density) { 44.dp.toPx() }

        val maxRadiusX = (widthPx / 2f - safePadX).coerceAtLeast(with(density) { 80.dp.toPx() })
        val maxRadiusY = min(orbitCenterY - safeTopPad, heightPx - orbitCenterY - safeBottomPad)
          .coerceAtLeast(with(density) { 64.dp.toPx() })

        val radiusXOuter = min(maxRadiusX, with(density) { 164.dp.toPx() })
        val radiusYOuter = min(maxRadiusY, with(density) { 102.dp.toPx() })
        val radiusXInner =
          if (hasDualRings) radiusXOuter * 0.67f
          else min(maxRadiusX, with(density) { 118.dp.toPx() })
        val radiusYInner =
          if (hasDualRings) radiusYOuter * 0.67f
          else min(maxRadiusY, with(density) { 74.dp.toPx() })

        // Galactic Dual-Ring Tracks + Crown Zenith Spotlight Glow + Star Dust
        Canvas(modifier = Modifier.fillMaxSize()) {
          val orbitCenter = Offset(centerX, orbitCenterY)
          val trackAlpha = orbitTrackAlpha.value
          val exp = orbitExpand.value

          // Nebula radial aura behind MANO & galactic wheel
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    animatedAccent.copy(alpha = (if (isDark) 0.16f else 0.10f) * mascotAlpha.value),
                    skyAccent.copy(alpha = (if (isDark) 0.08f else 0.04f) * mascotAlpha.value),
                    Color.Transparent,
                  ),
                center = orbitCenter,
                radius = radiusXOuter * 1.12f * exp.coerceAtLeast(0.45f),
              )
          )

          // Soft ambient backdrop inside galactic oval
          drawOval(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    (if (isDark) DarkSurfaceHigh else AquaSurface).copy(alpha = (if (isDark) 0.28f else 0.22f) * trackAlpha),
                    Color.Transparent,
                  ),
                center = orbitCenter,
                radius = radiusXOuter.coerceAtLeast(1f),
              ),
            topLeft = Offset(centerX - radiusXOuter * exp, orbitCenterY - radiusYOuter * exp),
            size = Size(radiusXOuter * 2f * exp, radiusYOuter * 2f * exp),
          )

          if (hasDualRings) {
            val outerStrokeColor =
              (if (activeRing == GalacticRing.OUTER) animatedAccent else skyAccent)
                .copy(alpha = (if (activeRing == GalacticRing.OUTER) 0.45f else if (isDark) 0.24f else 0.16f) * trackAlpha)
            drawOval(
              color = outerStrokeColor,
              topLeft = Offset(centerX - radiusXOuter * exp, orbitCenterY - radiusYOuter * exp),
              size = Size(radiusXOuter * 2f * exp, radiusYOuter * 2f * exp),
              style =
                Stroke(
                  width = if (activeRing == GalacticRing.OUTER) 4f else 2.5f,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                ),
            )

            // Subtle galactic starlight particles speckled around the outer orbit
            val sparkleAngles = listOf(-145.0, -35.0, 25.0, 155.0)
            sparkleAngles.forEachIndexed { i, baseDeg ->
              val rad = (baseDeg + outerContinuous * 16.0) * (PI / 180.0)
              val sx = centerX + (radiusXOuter * exp) * cos(rad).toFloat()
              val sy = orbitCenterY + (radiusYOuter * exp) * sin(rad).toFloat()
              drawCircle(
                color = (if (i % 2 == 0) goldAccent else primaryGlow).copy(alpha = 0.55f * trackAlpha),
                radius = if (i % 2 == 0) 4.2f else 3f,
                center = Offset(sx, sy),
              )
            }
          }

          val innerStrokeColor =
            (if (activeRing == GalacticRing.INNER || !hasDualRings) animatedAccent else primaryGlow)
              .copy(alpha = (if (activeRing == GalacticRing.INNER || !hasDualRings) 0.48f else if (isDark) 0.24f else 0.18f) * trackAlpha)
          drawOval(
            color = innerStrokeColor,
            topLeft = Offset(centerX - radiusXInner * exp, orbitCenterY - radiusYInner * exp),
            size = Size(radiusXInner * 2f * exp, radiusYInner * 2f * exp),
            style =
              Stroke(
                width = if (activeRing == GalacticRing.INNER || !hasDualRings) 4f else 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
              ),
          )

          // Top Zenith Crown Spotlight Glow marking the locked top focus point (-90 deg)
          val crownRadiusY =
            if (activeRing == GalacticRing.INNER || !hasDualRings) radiusYInner else radiusYOuter
          val crownCenter = Offset(centerX, orbitCenterY - crownRadiusY * exp)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    goldAccent.copy(alpha = (if (isDark) 0.34f else 0.24f) * trackAlpha),
                    animatedAccent.copy(alpha = (if (isDark) 0.18f else 0.10f) * trackAlpha),
                    Color.Transparent,
                  ),
                center = crownCenter,
                radius = with(density) { 46.dp.toPx() },
              ),
            radius = with(density) { 46.dp.toPx() },
            center = crownCenter,
          )

          if (tapHaloPulse.value > 0.01f) {
            val pulseProgress = 1f - tapHaloPulse.value
            drawCircle(
              color = animatedAccent.copy(alpha = 0.32f * tapHaloPulse.value),
              radius = with(density) { (56.dp + 38.dp * pulseProgress).toPx() },
              center = Offset(centerX, orbitCenterY + with(density) { 10.dp.toPx() }),
              style = Stroke(width = 4.5f),
            )
          }
        }

        // Prominent Centerpiece MANO Mascot (Spring Entrance + Idle Breathing + Interactive Tap Bounce)
        val mascotSize = if (hasDualRings) 98.dp else 108.dp
        val activeBreatheScale = if (animatorsEnabled) 1f + (breatheScale - 1f) * mascotAlpha.value else 1f
        val activeBreatheFloatDp = if (animatorsEnabled) breatheFloatDp * mascotAlpha.value else 0f
        val combinedScale = mascotScale.value * activeBreatheScale * tapBounceScale.value
        val combinedTranslateYPx =
          with(density) { (mascotOffsetYDp.value + activeBreatheFloatDp + tapBounceOffsetYDp.value).dp.toPx() }

        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .align(Alignment.Center)
              .offset(y = if (hasDualRings) 18.dp else 16.dp)
              .size(mascotSize + 10.dp)
              .zIndex(10f)
              .graphicsLayer {
                alpha = mascotAlpha.value
                scaleX = combinedScale
                scaleY = combinedScale
                translationY = combinedTranslateYPx
              }
              .clip(CircleShape)
              .clickable {
                scope.launch {
                  tapHaloPulse.snapTo(1f)
                  launch { tapHaloPulse.animateTo(0f, tween(durationMillis = 520, easing = FastOutSlowInEasing)) }
                  launch {
                    tapBounceScale.snapTo(0.90f)
                    tapBounceScale.animateTo(1.13f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
                    tapBounceScale.animateTo(
                      targetValue = 1f,
                      animationSpec = spring(dampingRatio = 0.44f, stiffness = 340f),
                    )
                  }
                  launch {
                    tapBounceOffsetYDp.animateTo(-10f, tween(durationMillis = 105, easing = FastOutSlowInEasing))
                    tapBounceOffsetYDp.animateTo(
                      targetValue = 0f,
                      animationSpec = spring(dampingRatio = 0.48f, stiffness = 300f),
                    )
                  }
                }
              }
              .minimumInteractiveComponentSize()
              .semantics { contentDescription = "شخصیت مانو، برای پرش شاد ضربه بزنید" }
              .testTag("mano_mascot_touch"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(mascotSize + 10.dp)
                .shadow(
                  elevation = 12.dp,
                  shape = CircleShape,
                  ambientColor = animatedAccent.copy(alpha = 0.28f),
                  spotColor = goldAccent.copy(alpha = 0.28f),
                )
                .clip(CircleShape)
                .background(Color(0xFFF0F8F7)),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_mano_hero_clean),
              contentDescription = "شخصیت رسمی مانو، ربات سفید با چهره سبز و برگ سبز روی سر",
              contentScale = ContentScale.Crop,
              alignment = Alignment.Center,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            // Soft perimeter glow + glowing metallic ring on top so MANO fits edge-to-edge inside the circle
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .background(
                    Brush.radialGradient(
                      colors =
                        listOf(
                          Color.Transparent,
                          Color.Transparent,
                          animatedAccent.copy(alpha = if (isDark) 0.26f else 0.14f),
                        )
                    )
                  )
                  .border(
                    width = 2.6.dp,
                    brush =
                      Brush.linearGradient(
                        listOf(
                          animatedAccent.copy(alpha = 0.88f),
                          goldAccent.copy(alpha = 0.88f),
                          TealGlow.copy(alpha = 0.82f),
                        )
                      ),
                    shape = CircleShape,
                  )
            )
          }
        }

        // Deterministic LTR Trig Plane for Both Galactic Rings
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Box(modifier = Modifier.fillMaxSize()) {
            // Outer Galactic Ring Satellites (6 specialized tools)
            if (hasDualRings) {
              outerList.forEachIndexed { index, item ->
                val delta = nearestWrappedDelta(index, outerContinuous, outerCount)
                val absDelta = abs(delta)
                val thetaRad = (-90f + delta * (360f / outerCount)) * (PI / 180.0)
                val satX = centerX + (radiusXOuter * orbitExpand.value) * cos(thetaRad).toFloat()
                val satY = orbitCenterY + (radiusYOuter * orbitExpand.value) * sin(thetaRad).toFloat()

                val isRingFocused = activeRing == GalacticRing.OUTER
                val isCrownLocked = isRingFocused && absDelta < 0.38f
                val proximity = (1f - (absDelta / (outerCount / 2f))).coerceIn(0f, 1f)
                val revealScale = 0.44f + 0.56f * orbitExpand.value.coerceIn(0f, 1.15f)
                val baseScale =
                  when {
                    isCrownLocked -> 1.18f
                    isRingFocused -> 0.70f + 0.30f * proximity
                    else -> 0.64f + 0.16f * proximity
                  }
                val itemAlpha =
                  when {
                    isCrownLocked -> 1f
                    isRingFocused -> (0.58f + 0.42f * proximity) * orbitTrackAlpha.value
                    else -> (0.44f + 0.34f * proximity) * orbitTrackAlpha.value
                  }
                val showLabel =
                  isCrownLocked ||
                    (isRingFocused && absDelta <= 1.25f) ||
                    (!isRingFocused && absDelta < 0.32f && sin(thetaRad) < 0f)

                GalacticSatelliteNode(
                  item = item,
                  satX = satX,
                  satY = satY,
                  nodeBoxDp = 74.dp,
                  medallionSize = if (isCrownLocked) 50.dp else 40.dp,
                  iconSize = if (isCrownLocked) 28.dp else 22.dp,
                  scaleFactor = baseScale * revealScale,
                  alpha = itemAlpha,
                  zIndex = if (isCrownLocked) 32f else if (isRingFocused) 16f + proximity * 4f else 4f + proximity * 2f,
                  isCrownLocked = isCrownLocked,
                  showLabel = showLabel,
                  goldAccent = goldAccent,
                  isDark = isDark,
                  onClick = { focusSatellite(GalacticRing.OUTER, index, item) },
                )
              }
            }

            // Inner Galactic Ring Satellites (6 primary core actions around MANO's head)
            innerList.forEachIndexed { index, item ->
              val delta = nearestWrappedDelta(index, innerContinuous, innerCount)
              val absDelta = abs(delta)
              val thetaRad = (-90f + delta * (360f / innerCount)) * (PI / 180.0)
              val satX = centerX + (radiusXInner * orbitExpand.value) * cos(thetaRad).toFloat()
              val satY = orbitCenterY + (radiusYInner * orbitExpand.value) * sin(thetaRad).toFloat()

              val isRingFocused = activeRing == GalacticRing.INNER || !hasDualRings
              val isCrownLocked = isRingFocused && absDelta < 0.38f
              val proximity = (1f - (absDelta / (innerCount / 2f))).coerceIn(0f, 1f)
              val revealScale = 0.44f + 0.56f * orbitExpand.value.coerceIn(0f, 1.15f)
              val baseScale =
                when {
                  isCrownLocked -> 1.22f
                  isRingFocused -> 0.72f + 0.30f * proximity
                  else -> 0.66f + 0.16f * proximity
                }
              val itemAlpha =
                when {
                  isCrownLocked -> 1f
                  isRingFocused -> (0.62f + 0.38f * proximity) * orbitTrackAlpha.value
                  else -> (0.48f + 0.32f * proximity) * orbitTrackAlpha.value
                }
              val showLabel = isCrownLocked || (isRingFocused && absDelta <= 1.25f)

              GalacticSatelliteNode(
                item = item,
                satX = satX,
                satY = satY,
                nodeBoxDp = 78.dp,
                medallionSize = if (isCrownLocked) 52.dp else 44.dp,
                iconSize = if (isCrownLocked) 30.dp else 24.dp,
                scaleFactor = baseScale * revealScale,
                alpha = itemAlpha,
                zIndex = if (isCrownLocked) 32f else if (isRingFocused) 20f + proximity * 5f else 6f + proximity * 3f,
                isCrownLocked = isCrownLocked,
                showLabel = showLabel,
                goldAccent = goldAccent,
                isDark = isDark,
                onClick = { focusSatellite(GalacticRing.INNER, index, item) },
              )
            }
          }
        }
      }

      // Galactic Ring Switcher Pills (Inner vs Outer Orbit)
      if (hasDualRings) {
        Row(
          modifier = Modifier.fillMaxWidth().testTag("orbital_ring_switcher"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf(
            Triple(GalacticRing.INNER, "${GalacticRing.INNER.labelFa} (${innerList.size})", "orbital_ring_inner"),
            Triple(GalacticRing.OUTER, "${GalacticRing.OUTER.labelFa} (${outerList.size})", "orbital_ring_outer"),
          ).forEach { (ring, label, tag) ->
            val selected = activeRing == ring
            Surface(
              onClick = {
                userInteracted = true
                if (activeRing != ring) {
                  activeRing = ring
                  if (!isAutoRotating) {
                    magneticSnapActiveRing(targetRing = ring)
                  } else {
                    val ringList = if (ring == GalacticRing.INNER) innerList else outerList
                    val ringIdx = if (ring == GalacticRing.INNER) innerActiveIndex else outerActiveIndex
                    onPersistSelection(ring, ringList[ringIdx].id)
                  }
                }
              },
              color = if (selected) animatedAccent else elevatedSurface,
              shape = RoundedCornerShape(50),
              border = BorderStroke(1.dp, if (selected) animatedAccent else borderColor),
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag(tag),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  tint = if (selected) onAccentText else animatedAccent,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (selected) onAccentText else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      }

      // Active Crown Focus Preview Bar + Accessible Magnetic Step Controls
      Surface(
        color = elevatedSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isDark) animatedAccent.copy(alpha = 0.42f) else borderColor),
        modifier =
          Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = ShadowSlate.copy(alpha = 0.10f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
            onClick = { stepActiveRingBy(-1) },
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = "چرخش مدار به بخش قبلی" }
                .testTag("orbital_rotate_prev"),
          ) {
            Surface(
              color = animatedAccent.copy(alpha = 0.14f),
              shape = CircleShape,
              modifier = Modifier.size(36.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = animatedAccent)
              }
            }
          }

          AnimatedContent(
            targetState = activeTopic to activeRing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "orbitalFocusTopic",
            modifier = Modifier.weight(1f).testTag("orbital_active_topic"),
          ) { (focused, ring) ->
            val focusedAccent = focused.accentRole.resolveAccentColor()
            val focusedButtonText = if (isDark) DarkOnPrimary else PureWhite
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Box(
                modifier =
                  Modifier
                    .width(3.5.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(focusedAccent)
              )
              ManoIconMedallion(
                icon = focused.icon,
                accentColor = focusedAccent,
                iconKind = focused.iconKind,
                containerSize = 42.dp,
                iconSize = 26.dp,
                cornerRadius = 13.dp,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = focused.titleFa,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (hasDualRings) "${ring.labelFa} • ${focused.subtitleFa}" else focused.subtitleFa,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Button(
                onClick = {
                  userInteracted = true
                  onPersistSelection(ring, focused.id)
                  onSelectTopic(focused)
                },
                colors = ButtonDefaults.buttonColors(containerColor = focusedAccent, contentColor = focusedButtonText),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("orbital_focus_cta"),
              ) {
                Text("ورود", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            }
          }

          IconButton(
            onClick = { stepActiveRingBy(1) },
            modifier =
              Modifier
                .minimumInteractiveComponentSize()
                .semantics { contentDescription = "چرخش مدار به بخش بعدی" }
                .testTag("orbital_rotate_next"),
          ) {
            Surface(
              color = animatedAccent.copy(alpha = 0.14f),
              shape = CircleShape,
              modifier = Modifier.size(36.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = animatedAccent)
              }
            }
          }
        }
      }

      Text(
        text = stringResource(R.string.welcome_body),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun GalacticSatelliteNode(
  item: RadialTopicItem,
  satX: Float,
  satY: Float,
  nodeBoxDp: Dp,
  medallionSize: Dp,
  iconSize: Dp,
  scaleFactor: Float,
  alpha: Float,
  zIndex: Float,
  isCrownLocked: Boolean,
  showLabel: Boolean,
  goldAccent: Color,
  isDark: Boolean,
  onClick: () -> Unit,
) {
  val density = LocalDensity.current
  val halfNodePx = with(density) { (nodeBoxDp / 2).toPx() }
  val itemAccent = item.accentRole.resolveAccentColor()
  val crownTextColor = if (isDark) DarkOnPrimary else PureWhite
  val pillSurface = if (isDark) DarkSurfaceHigh.copy(alpha = 0.94f) else PureWhite.copy(alpha = 0.96f)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp),
    modifier =
      Modifier
        .absoluteOffset {
          IntOffset(
            x = (satX - halfNodePx).roundToInt(),
            y = (satY - halfNodePx).roundToInt(),
          )
        }
        .width(nodeBoxDp)
        .zIndex(zIndex)
        .scale(scaleFactor)
        .alpha(alpha)
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize()
        .testTag("orbital_item_${item.id}"),
  ) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.then(
            if (isCrownLocked) {
              Modifier
                .shadow(
                  elevation = 8.dp,
                  shape = RoundedCornerShape(20.dp),
                  ambientColor = goldAccent.copy(alpha = 0.35f),
                  spotColor = itemAccent.copy(alpha = 0.35f),
                )
                .border(
                  width = 2.2.dp,
                  brush = Brush.linearGradient(listOf(goldAccent, itemAccent)),
                  shape = RoundedCornerShape(20.dp),
                )
                .padding(3.dp)
            } else {
              Modifier
            }
          ),
      ) {
        ManoIconMedallion(
          icon = item.icon,
          accentColor = itemAccent,
          iconKind = item.iconKind,
          containerSize = medallionSize,
          iconSize = iconSize,
          cornerRadius = if (isCrownLocked) 16.dp else 14.dp,
        )
      }
      if (showLabel) {
        Surface(
          color = if (isCrownLocked) itemAccent else pillSurface,
          shape = RoundedCornerShape(50),
          border = BorderStroke(if (isCrownLocked) 1.dp else 0.8.dp, itemAccent.copy(alpha = if (isCrownLocked) 0.9f else 0.45f)),
        ) {
          Text(
            text = item.titleFa,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCrownLocked) crownTextColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}
}
