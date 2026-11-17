# MANO Kids character family

Six local specialist navigation cards: Drug, Growth, Skin, Nutrition, Care, Connect. Core urgent guidance and the fever timeline remain above the new section. Existing medical content and routing are unchanged; Care opens the existing fever/symptom screen, Connect opens the existing pharmacist screen. Mascots are educational guides, not medication approvals or risk assessments.

Native Android vector artwork is included. To enable the separate premium 3D concept sheet crops, add these six WebP files under app/src/main/assets/mascots/: drug.webp, growth.webp, skin.webp, nutrition.webp, care.webp, connect.webp. The raster art is not part of this commit. No network fetching or required external image dependency.

Review checks before merge: Android assembleDebug, unit tests, tap all six shortcuts, inspect RTL, dark mode, 320dp width, large fonts and TalkBack; ensure urgent warning remains above mascot section. Validate optional artwork both present and absent. Do not ship without human UI review.
