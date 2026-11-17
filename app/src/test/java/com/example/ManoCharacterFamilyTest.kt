package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.ManoCharacterFamily
import com.example.ui.components.specialistColumnCount
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManoCharacterFamilyTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun columnsAdaptToWidthsAndLargeText() {
    assertEquals(1, specialistColumnCount(288f, 1f))
    assertEquals(2, specialistColumnCount(360f, 1f))
    assertEquals(3, specialistColumnCount(720f, 1f))
    assertEquals(1, specialistColumnCount(720f, 1.5f))
    assertEquals(2, specialistColumnCount(720f, 1.3f))
  }

  @Test
  fun sixSpecialistsDispatchExactlyTheirOwnCallbacks() {
    val clicked = mutableListOf<String>()
    compose.setContent {
      MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          Column(Modifier.verticalScroll(rememberScrollState())) {
            ManoCharacterFamily(
              onMedicines = { clicked.add("drug") },
              onGrowth = { clicked.add("growth") },
              onSkin = { clicked.add("skin") },
              onNutrition = { clicked.add("nutrition") },
              onCare = { clicked.add("care") },
              onConnect = { clicked.add("connect") },
            )
          }
        }
      }
    }
    val expected = listOf("drug", "growth", "skin", "nutrition", "care", "connect")
    expected.forEach { id ->
      compose.onNodeWithTag("mano_specialist_$id").performScrollTo().performClick()
    }
    compose.runOnIdle { assertEquals(expected, clicked) }
  }
}
