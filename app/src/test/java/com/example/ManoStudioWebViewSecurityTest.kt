package com.example

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.isTrustedThreeStudioAssetUri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * All WebView resources must originate from the six local, packaged studio files.
 * AndroidBridge must not be exposed while a remote or unexpected document loads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManoStudioWebViewSecurityTest {

  private val origin = "https://appassets.androidplatform.net/assets/three/"

  @Test
  fun onlyExplicitlyBundledStudioResourcesAreTrusted() {
    listOf(
      "index.html",
      "scene.js",
      "three.module.min.js",
      "three.core.min.js",
      "OrbitControls.js",
      "RoomEnvironment.js",
    ).forEach { name ->
      assertTrue("Bundled studio file must be trusted: $name", isTrustedThreeStudioAssetUri(Uri.parse(origin + name)))
    }
  }

  @Test
  fun blocksRemoteHostsInsecureSchemesAndUnexpectedLocalFiles() {
    listOf(
      "https://evil.example/assets/three/index.html",
      "https://appassets.androidplatform.net.evil.example/assets/three/index.html",
      "https://attacker@appassets.androidplatform.net/assets/three/index.html",
      "http://appassets.androidplatform.net/assets/three/index.html",
      "https://appassets.androidplatform.net:443/assets/three/index.html",
      "https://appassets.androidplatform.net/assets/other/index.html",
      "https://appassets.androidplatform.net/assets/three/private.html",
      "https://appassets.androidplatform.net/assets/three/../secret.js",
      "https://appassets.androidplatform.net/assets/three/%2e%2e/secret.js",
      "https://appassets.androidplatform.net/assets/three/scene.js?code=remote",
      "https://appassets.androidplatform.net/assets/three/index.html#foreign",
      "javascript:alert(1)",
      "file:///android_asset/three/index.html",
      "data:text/html,<script>alert(1)</script>",
    ).forEach { url ->
      assertFalse("Unexpected URL must be rejected: $url", isTrustedThreeStudioAssetUri(Uri.parse(url)))
    }
  }
  @Test
  fun packagedStudioDocumentForbidsNetworkScriptsFramesAndRequests() {
    val html =
      ApplicationProvider.getApplicationContext<Application>()
        .assets.open("three/index.html").bufferedReader().use { it.readText() }
    assertTrue(html.contains("Content-Security-Policy"))
    assertTrue(html.contains("default-src 'none'"))
    assertTrue(html.contains("script-src 'self'"))
    assertTrue(html.contains("connect-src 'none'"))
    assertTrue(html.contains("frame-src 'none'"))
    assertTrue(html.contains("form-action 'none'"))
    assertFalse(html.contains("<iframe"))
    assertFalse(html.contains("https://cdn."))
  }

}
