package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.domain.util.ManoDomainLinks
import com.example.ui.util.PharmacyQrCard
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManoPharmacyQrCardTest {
  @Test
  fun verifiedPharmacyInvitation_encodesAndDecodesExactHttpsUrlOffline() {
    val url = ManoDomainLinks.pharmacyInvitationUrl("pharm_2026-AB")
    assertNotNull(url)
    val bitmap = PharmacyQrCard.createQrBitmap(url!!, 400)
    assertNotNull(bitmap)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap!!.config)
    assertEquals(400, bitmap.width)
    assertEquals(400, bitmap.height)

    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val luminance = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(luminance))).text
    assertEquals(url, decoded)
    assertTrue(decoded.startsWith("https://manokid.ir/pharmacist?pharmacy="))
    assertFalse(decoded.contains("child", ignoreCase = true))
    assertFalse(decoded.contains("token", ignoreCase = true))
  }

  @Test
  fun invalidOrExternalPharmacyQrData_isRejectedBeforeAnyBitmapCreation() {
    val unsafe = listOf(
      "",
      "https://evil.example/pharmacist?pharmacy=shop",
      "http://manokid.ir/pharmacist?pharmacy=shop",
      "https://manokid.ir/pharmacist?pharmacy=shop&child=child_1",
      "https://manokid.ir/pharmacist?pharmacy=../shop",
      "https://manokid.ir/pharmacist?pharmacy=shop#secret",
      "https://manokid.ir/pharmacist?pharmacy=",
      "javascript:alert(1)",
      "https://manokid.ir/pharmacist?pharmacy=shop%26another",
    )
    unsafe.forEach { url ->
      assertFalse("Must not encode unsafe invitation: $url", PharmacyQrCard.isSafeInvitation(url))
      assertNull(PharmacyQrCard.createQrBitmap(url))
    }
    val url = ManoDomainLinks.pharmacyInvitationUrl("pharm_12")!!
    assertNull(PharmacyQrCard.createQrBitmap(url, sizePx = 120))
    assertNull(PharmacyQrCard.createQrBitmap(url, sizePx = 2500))
  }

  @Test
  fun printCardRejectsNonPharmacyUrlsWithoutShowingAndroidPrintDialog() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    assertTrue(PharmacyQrCard.printStarterCard(context, "https://evil.example", "داروخانه").isFailure)
  }
}
