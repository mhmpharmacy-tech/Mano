# MANO Kids character family

Six local specialist navigation cards: Drug, Growth, Skin, Nutrition, Care, Connect. Core urgent guidance and the fever timeline remain above the new section. Existing medical content and routing are unchanged; Care opens the existing fever/symptom screen, Connect opens the existing pharmacist screen. Mascots are educational guides, not medication approvals or risk assessments.

Native Android vector artwork is included. To enable the separate premium 3D concept sheet crops, add these six WebP files under app/src/main/assets/mascots/: drug.webp, growth.webp, skin.webp, nutrition.webp, care.webp, connect.webp. The raster art is not part of this commit. No network fetching or required external image dependency.

Review checks before merge: Android assembleDebug, unit tests, tap all six shortcuts, inspect RTL, dark mode, 320dp width, large fonts and TalkBack; ensure urgent warning remains above mascot section. Validate optional artwork both present and absent. Do not ship without human UI review.

## Adaptive layout and review
One column at narrow widths or large text, two columns on ordinary phones, three on wide screens at normal font scale. Cards keep text uncropped and tap targets independent of artwork. Optional local bitmap decode runs on Dispatchers.IO with immediate native vector fallback. No external loading.
Robolectric test source covers five width/font-scale cases and all six click callbacks in RTL. Execution is pending because this environment has no Android SDK or Gradle installation; the repository also lacks gradlew, the wrapper JAR, and the configured debug.keystore. Do not change AI Studio signing configuration to bypass this.
## Import original artwork
From the repository root on the feature branch, apply the separately supplied binary patch:
```sh
git apply --check /path/to/MANO_3D_assets.patch
git apply /path/to/MANO_3D_assets.patch
git add app/src/main/assets/mascots/
git commit -m "Bundle original MANO 3D mascot artwork"
```
The binary patch has been locally applied and byte-compared with the six original assets. It is not in this commit.
