package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoContentRepository
import com.example.domain.util.ManoDeepLinkParser
import com.example.domain.util.ManoDomainLinks
import com.example.domain.util.PharmacyInvitationPolicy
import com.example.domain.model.PublicPharmacyOption
import com.example.domain.util.VaccinationScheduleEngine
import com.example.ui.navigation.AskPharmacistRoute
import com.example.ui.navigation.FaqRoute
import com.example.ui.navigation.GrowthRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.InfoRoute
import com.example.ui.navigation.MedicineDetailRoute
import com.example.ui.navigation.MedicinesRoute
import com.example.ui.navigation.NutritionRoute
import com.example.ui.navigation.Pediatric3DStudioRoute
import com.example.ui.navigation.SymptomsRoute
import com.example.ui.navigation.TopicDetailRoute
import com.example.ui.navigation.TopicsRoute
import com.example.ui.navigation.VaccinesRoute
import com.example.ui.screens.InfoScreen
import com.example.ui.screens.TopicDetailScreen
import com.example.ui.theme.ManoKidsTheme
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = Application::class)
class ManoDomainLinksTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val repository = ManoContentRepository()
  private val articleSlugs = repository.articles.map { it.slug }.toSet()
  private val medSlugs = repository.medications.map { it.slug }.toSet()
  private val symptomIds = repository.symptomFlows.map { it.id }.toSet()

  @Test
  fun hostAllowlist_acceptsIrAndComAndSubdomains_rejectsUntrustedHosts() {
    assertTrue(ManoDomainLinks.isSupportedHost("manokid.ir"))
    assertTrue(ManoDomainLinks.isSupportedHost("WWW.MANOKID.IR"))
    assertTrue(ManoDomainLinks.isSupportedHost("manokid.com"))
    assertTrue(ManoDomainLinks.isSupportedHost("www.manokid.com:443"))

    assertFalse(ManoDomainLinks.isSupportedHost("manokid.ir.evil.com"))
    assertFalse(ManoDomainLinks.isSupportedHost("google.com"))
    assertFalse(ManoDomainLinks.isSupportedHost(null))
  }

  @Test
  fun pharmacyInvitationLinks_requireSafeIdsAndNeverAuthorizeUnknownHosts() {
    assertEquals(
      "https://manokid.ir/pharmacist?pharmacy=pharm_10",
      ManoDomainLinks.pharmacyInvitationUrl("pharm_10"),
    )
    assertTrue(ManoDomainLinks.isValidPublicPharmacyId("rx-21_abc"))
    for (bad in listOf("", " ", "../rx", "rx?other=1", "pharmacy/xx", "داروخانه", "a".repeat(65))) {
      assertFalse("Invalid id must be rejected: $bad", ManoDomainLinks.isValidPublicPharmacyId(bad))
      assertNull(ManoDomainLinks.pharmacyInvitationUrl(bad))
    }
    assertEquals(
      "https://manokid.ir/pharmacist?pharmacy=pharm_10",
      ManoDomainLinks.pharmacyInvitationUrl("pharm_10", host = "evil.example"),
    )
    val text = ManoDomainLinks.buildPharmacyIntroductionText("pharm_10", "داروخانه آزمایشی")
    assertNotNull(text)
    assertTrue(text!!.contains("داروخانه آزمایشی"))
    assertTrue(text.contains("فور"))
    assertTrue(text.contains("pharmacy=pharm_10"))
    assertNull(ManoDomainLinks.buildPharmacyIntroductionText("bad/id"))
  }

  @Test
  fun pharmacyInvitationPolicy_neverSilentlyRedirectsToAnotherPharmacy() {
    val verifiedList = listOf(
      PublicPharmacyOption("pharm_a", "داروخانه الف", acceptingCases = true),
      PublicPharmacyOption("pharm_b", "داروخانه ب", acceptingCases = false),
      PublicPharmacyOption("pharm_c", "داروخانه ج", acceptingCases = true),
    )
    assertEquals("pharm_a",
      PharmacyInvitationPolicy.resolveAcceptingPharmacyId("pharm_a", verifiedList))
    assertEquals("pharm_c",
      PharmacyInvitationPolicy.resolveAcceptingPharmacyId("pharm_c", verifiedList))
    // A paused shop may never fall back to the first accepting pharmacy.
    assertNull(PharmacyInvitationPolicy.resolveAcceptingPharmacyId("pharm_b", verifiedList))
    assertNull(PharmacyInvitationPolicy.resolveAcceptingPharmacyId("unknown", verifiedList))
    assertNull(PharmacyInvitationPolicy.resolveAcceptingPharmacyId("../pharm_a", verifiedList))
    assertNull(PharmacyInvitationPolicy.resolveAcceptingPharmacyId("pharm_a", emptyList()))
    assertNull(PharmacyInvitationPolicy.resolveAcceptingPharmacyId(null, verifiedList))
  }

  @Test
  fun pharmacyDeepLink_isOnlyUntrustedNavigationHint_notAPharmacistPrivilege() {
    val valid = ManoDeepLinkParser.resolve(
      "https://manokid.ir/pharmacist?pharmacy=pharm_10",
      articleSlugs, medSlugs, symptomIds,
    )
    assertEquals(AskPharmacistRoute(invitedPharmacyId = "pharm_10"), valid?.route)
    assertNull(valid?.fallbackNoticeFa)

    val invalid = ManoDeepLinkParser.resolve(
      "https://manokid.ir/pharmacist?pharmacy=../pharm_10",
      articleSlugs, medSlugs, symptomIds,
    )
    assertEquals(AskPharmacistRoute(), invalid?.route)
    assertNotNull(invalid?.fallbackNoticeFa)

    assertNull(ManoDeepLinkParser.resolve(
      "https://manokid.ir.attacker.example/pharmacist?pharmacy=pharm_10",
      articleSlugs, medSlugs, symptomIds,
    ))
    assertNull(ManoDeepLinkParser.resolve(
      "javascript:pharmacy=pharm_10", articleSlugs, medSlugs, symptomIds,
    ))
  }

  @Test
  fun pharmacyStartKit_showsEducationalSharingWithoutUnverifiedPharmacyInvitation() {
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        com.example.ui.screens.PharmacistStaffPanelContent(
          currentUser = null,
          staffAccess = com.example.domain.model.StaffAccessStatus(),
          inboxCases = emptyList(),
          isLoadingInbox = false,
          inFlightCaseId = null,
          staffErrorFa = null,
          onRefreshStaffInbox = { _, _ -> },
          onAcknowledgeReceived = { _, _, _ -> },
          onSubmitReview = { _, _, _, _, _ -> },
        )
      }
    }
    composeTestRule.onNodeWithTag("pharmacy_start_kit").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pharmacy_generic_guide_share").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pharmacy_invite_copy").assertDoesNotExist()
    composeTestRule.onNodeWithTag("pharmacy_invite_share").assertDoesNotExist()
  }

  @Test
  fun deepLinkParser_resolvesGuidesMedicinesVaccinesAndSubTabs() {
    val firstArticleSlug = articleSlugs.first()
    val firstMedSlug = medSlugs.first()
    val firstSymptomId = symptomIds.first()

    val guideRes =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/topics/$firstArticleSlug",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(TopicDetailRoute(firstArticleSlug), guideRes?.route)
    assertNull(guideRes?.fallbackNoticeFa)

    val comMedRes =
      ManoDeepLinkParser.resolve(
        "https://www.manokid.com/medicines/$firstMedSlug/",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(MedicineDetailRoute(firstMedSlug), comMedRes?.route)

    val timelineRes =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/medicines?tab=timeline",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(MedicinesRoute, timelineRes?.route)
    assertEquals("timeline", timelineRes?.medicineTabKey)

    val vaccinesRes =
      ManoDeepLinkParser.resolve(
        "http://manokid.com/vaccines",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(VaccinesRoute, vaccinesRes?.route)

    val symptomRes =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/symptoms/$firstSymptomId",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(SymptomsRoute(firstSymptomId), symptomRes?.route)

    assertEquals(
      GrowthRoute,
      ManoDeepLinkParser.resolve("https://manokid.ir/growth", articleSlugs, medSlugs, symptomIds)?.route,
    )
    assertEquals(
      NutritionRoute,
      ManoDeepLinkParser.resolve("https://manokid.com/nutrition", articleSlugs, medSlugs, symptomIds)?.route,
    )
    assertEquals(
      Pediatric3DStudioRoute("weighing"),
      ManoDeepLinkParser.resolve("https://manokid.ir/3d?scene=weighing", articleSlugs, medSlugs, symptomIds)?.route,
    )
    assertEquals(
      InfoRoute("domain"),
      ManoDeepLinkParser.resolve("https://manokid.ir/info?section=domain", articleSlugs, medSlugs, symptomIds)?.route,
    )
    assertEquals(
      HomeRoute,
      ManoDeepLinkParser.resolve("https://manokid.ir/", articleSlugs, medSlugs, symptomIds)?.route,
    )
  }

  @Test
  fun deepLinkParser_sanitizesUnknownSlugsAndRejectsForeignDomains() {
    val unknownTopic =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/topics/non-existent-slug",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(TopicsRoute(), unknownTopic?.route)
    assertNotNull(unknownTopic?.fallbackNoticeFa)

    val unknownMed =
      ManoDeepLinkParser.resolve(
        "https://manokid.com/medicines/non-existent-med",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(MedicinesRoute, unknownMed?.route)
    assertNotNull(unknownMed?.fallbackNoticeFa)

    assertNull(
      ManoDeepLinkParser.resolve(
        "https://untrusted.example.com/vaccines",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    )
    assertNull(
      ManoDeepLinkParser.resolve(
        "javascript:alert(1)",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    )
  }

  @Test
  fun shareBuildersAndAssetLinksJson_produceExpectedUrlsAndValidJson() {
    val article = repository.articles.first()
    val med = repository.medications.first()
    val summary = VaccinationScheduleEngine.resolveSchedule()

    assertTrue(ManoDomainLinks.buildTopicShareText(article).contains("https://manokid.ir/topics/${article.slug}"))
    assertTrue(ManoDomainLinks.buildMedicineShareText(med).contains("https://manokid.ir/medicines/${med.slug}"))
    assertTrue(ManoDomainLinks.buildVaccinesShareText(summary, "آوا").contains("https://manokid.ir/vaccines"))

    val jsonStr =
      ManoDomainLinks.buildAssetLinksJson(
        packageName = "com.aistudio.manokids.rnkvpx",
        sha256Fingerprints = listOf("AA:BB:CC:DD"),
      )
    val parsed = JSONArray(jsonStr)
    assertEquals(1, parsed.length())
    val statement = parsed.getJSONObject(0)
    val target = statement.getJSONObject("target")
    assertEquals("com.aistudio.manokids.rnkvpx", target.getString("package_name"))
    val fps = target.getJSONArray("sha256_cert_fingerprints")
    assertEquals("AA:BB:CC:DD", fps.getString(0))
  }

  @Test
  fun infoScreenDomainSection_copiesAssetLinksJsonToClipboard() {
    var feedbackMsg = ""
    lateinit var composeContext: Context

    composeTestRule.setContent {
      composeContext = androidx.compose.ui.platform.LocalContext.current
      ManoKidsTheme(darkTheme = false) {
        InfoScreen(
          initialSection = "domain",
          sources = repository.sources,
          onTestDeepLink = {},
          onShowFeedback = { feedbackMsg = it },
        )
      }
    }

    composeTestRule.onNodeWithTag("info_domain_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("info_list").performScrollToNode(hasTestTag("copy_assetlinks_btn"))
    composeTestRule.onNodeWithTag("copy_assetlinks_btn").performClick()
    composeTestRule.waitForIdle()

    assertTrue("Expected feedback but got: '$feedbackMsg'", feedbackMsg.contains("assetlinks.json"))
    val clipboard = composeContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
    assertTrue("Expected assetlinks JSON in clipboard, got: '$copied'", copied.contains("delegate_permission/common.handle_all_urls"))
    assertTrue(copied.contains("/vaccines*"))
  }

  @Test
  fun topicDetailScreen_copyDomainPill_copiesCanonicalTopicUrl() {
    val article = repository.articles.first()
    var feedbackMsg = ""
    lateinit var composeContext: Context

    composeTestRule.setContent {
      composeContext = androidx.compose.ui.platform.LocalContext.current
      ManoKidsTheme(darkTheme = false) {
        TopicDetailScreen(
          article = article,
          relatedArticles = emptyList(),
          isBookmarked = false,
          onToggleBookmark = {},
          onBack = {},
          onNextStepCta = {},
          onOpenArticle = {},
          onShowFeedback = { feedbackMsg = it },
        )
      }
    }

    composeTestRule.onNodeWithTag("detail_share_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("detail_domain_link_pill").performClick()
    composeTestRule.waitForIdle()

    assertTrue("Expected non-blank feedback", feedbackMsg.isNotBlank())
    val clipboard = composeContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
    assertEquals("https://manokid.ir/topics/${article.slug}", copied)
  }

  @Test
  fun deepLinkParser_handlesCaseInsensitiveSlugsSymptomQueryAndFilterResetFlags() {
    val firstArticleSlug = articleSlugs.first()
    val firstMedSlug = medSlugs.first()
    val firstSymptomId = symptomIds.first()

    val upperTopic =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/Topics/${firstArticleSlug.uppercase()}",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(TopicDetailRoute(firstArticleSlug), upperTopic?.route)
    assertNull(upperTopic?.fallbackNoticeFa)

    val upperCategory =
      ManoDeepLinkParser.resolve(
        "https://manokid.com/topics?category=SYMPTOMS&q=%D8%AA%D8%A8",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(TopicsRoute(categoryId = "symptoms", query = "تب"), upperCategory?.route)
    assertEquals("symptoms", upperCategory?.topicCategoryId)
    assertEquals("تب", upperCategory?.topicQuery)
    assertTrue(upperCategory?.resetTopicFilters == true)

    val unknownTopic =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/topics/non-existent",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertTrue(unknownTopic?.resetTopicFilters == true)

    val faqSearch =
      ManoDeepLinkParser.resolve(
        "https://manokid.com/faq?q=%D8%AA%D8%A8",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(FaqRoute("تب"), faqSearch?.route)
    assertTrue(faqSearch?.resetFaqFilters == true)

    val upperMed =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/medicines/${firstMedSlug.uppercase()}",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(MedicineDetailRoute(firstMedSlug), upperMed?.route)

    val upperSymptomPath =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/symptoms/${firstSymptomId.uppercase()}",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(SymptomsRoute(firstSymptomId), upperSymptomPath?.route)

    val upperSymptomQuery =
      ManoDeepLinkParser.resolve(
        "https://manokid.ir/symptoms?id=${firstSymptomId.uppercase()}",
        articleSlugs,
        medSlugs,
        symptomIds,
      )
    assertEquals(SymptomsRoute(firstSymptomId), upperSymptomQuery?.route)
  }
}
