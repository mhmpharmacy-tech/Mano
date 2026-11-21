package com.example

import com.example.data.FeverLogCloud
import com.example.domain.model.GrowthMeasurement
import com.example.domain.model.PreviousCare
import com.example.domain.util.TimelineTempPoint
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousCareTest {
  @Test fun selectionUsesSavedDatesAndCurrentAccount() {
    val local = listOf(TimelineTempPoint("guest", 37f, 50_000L, "", "", "", "", false))
    val mine = FeverLogCloud(userId = "mine", tempCelsius = 37.5, createdAt = Timestamp(20L, 0))
    val cloud = listOf(mine.copy(userId = "other", createdAt = Timestamp(200L, 0)), mine,
      mine.copy(createdAt = null), mine.copy(tempCelsius = Double.NaN, createdAt = Timestamp(80L, 0)))
    val growth = GrowthMeasurement(30_000L, 10.0, null)
    assertEquals(PreviousCare.Temperature(50_000L, 37.0), PreviousCare.latest(null, local, cloud, listOf(growth)))
    assertEquals(PreviousCare.Growth(growth), PreviousCare.latest("mine", local, cloud, listOf(growth)))
    assertEquals(PreviousCare.Temperature(20_000L, 37.5), PreviousCare.latest("mine", local, cloud, emptyList()))
    assertNull(PreviousCare.latest("new", local, cloud, emptyList()))
    assertNull(PreviousCare.latest(null, emptyList(), cloud, emptyList()))
  }
}
