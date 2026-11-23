# MANO Kid — Google AI Studio Build Care Home

**Purpose:** Make the approved, restrained premium mobile Care Home appear when this GitHub repository is imported/pulled into a correctly linked **Google AI Studio Build web app**.

## Where the website actually lives

- Root \`index.html\`: approved Persian RTL dashboard design, unchanged original MANO face and round luminous eyes (embedded from the approved reference prototype).
- \`src/app.css\`: premium green/cream visual tokens and accessible responsive interior pages.
- \`src/app.js\`: functional, client-side navigation; clinical safety warnings; profile, growth, vaccine-history and treatment-plan **temporary drafts**.
- \`server.mjs\`: dependency-free restricted preview server, port 3000 by default.
- \`metadata.json\`: existing Google AI Studio app metadata remains in place.
- \`app/\`: separate Android Jetpack Compose project; a web Pull does **not** convert Android Kotlin into web UI.
- \`public/care/\`: a separate Firebase card feature; nothing here deploys to Firebase.

## Run / verify

\`\`\`bash
npm ci
npm run check
npm run build
npm run dev
\`\`\`

The app should appear at \`http://localhost:3000\`. Build output is \`dist/\`. No third-party packages, live network APIs or API keys are required.

## How to pull into Google AI Studio

1. Back up the existing AI Studio project or GitHub branch first.
2. Open **Build** and the existing project, then check **Settings → GitHub** to verify the **same repo** \`mhmpharmacy-tech/Mano\` and branch \`main\` are linked.
3. After this web PR is merged to \`main\`, select **Pull changes** in the GitHub tab. If an existing workspace cannot rebind to the repo, use **+ → Import from GitHub** as a separate preview instead of overwriting the original project.
4. Confirm that the loaded entrypoint is the **root \`index.html\`** and server command is \`npm run dev\`, not \`app/src/main/java/.../HomeScreen.kt\`.
5. Check the five bottom tabs and the primary actions; original robot eyes must not change.
6. If UI Studio still shows a prior app despite a successful GitHub Pull, that workspace is not running the newly imported root entrypoint. Check its attached repository and its preview logs; do not assume pulling Android files changes React.

## Definition of done and limitations

- [x] Final premium Care Home layout + original MANO visual identity, no human child portrait, no sleep icon, one small icon per feature.
- [x] Interactive symptom wizard with conservative urgent stops; no diagnostic clearance.
- [x] Child alias/age, growth measurement and vaccine history **in memory only**, reset on refresh.
- [x] Pharmacist question and medication plan **draft only**; no real submission, dose suggestions, clinical approvals or reminder schedules.
- [x] Internal build, smoke and Chromium responsive tests (360 and 412 px) in CI.
- [ ] **Actual Google AI Studio workspace Pull verification**, requiring the user's connected UI.
- [ ] Back-end integration with authenticated Firebase and validated real medical content; deliberately not attempted in this design sync.
- [ ] Human review of clinical content, privacy/security and device tests before any public release.

**Data safety:** No real patient details should be entered in this demo. It neither sends data nor persists protected health information. Do not deploy as a patient-facing clinical product. The Android project remains a separate integration track.
