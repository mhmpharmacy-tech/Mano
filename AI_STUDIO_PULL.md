# MANO Kid — importing the web app into AI Studio

## Entrypoints

- `/` is the React application: `index.html` → `src/main.tsx` → `src/App.tsx`.
- `/preview/` preserves the previous approved Care Home design as a separate, in-memory review demo. Its assets remain `src/app.js` and `src/app.css`.
- `server.ts` is the Express/Vite server; `server.mjs` is no longer an entrypoint.
- `app/` contains separate Kotlin sources. Importing the web app does not build or validate Android.

Both HTML entrypoints are built into `dist/`. The main React app has an internal-preview notice and persists local drafts in this browser; the legacy demo clears its in-memory drafts on reload. Use synthetic data only.

## Run and verify

```bash
npm ci
npm run check
npm run build
npm run dev
```

Open `http://localhost:3000` for React and `/preview/` for the retained design. For a production-mode local smoke test, use `NODE_ENV=production npm start` after building. Run `npx playwright install --with-deps chromium` followed by `npm run test:browser` to test the built app in Chromium.

## Pull into AI Studio

1. Verify the linked repository is `mhmpharmacy-tech/Mano` and select the intended branch.
2. Pull the reviewed changes or import the branch into a separate preview workspace.
3. Confirm the start command is `npm run dev` and the root entry loads `src/main.tsx`.
4. Open the dose calculator with a fresh browser profile: age and weight must be blank, and confirming a label must not reveal a dose until both are valid.
5. Confirm `/preview/` still loads the retained design and the default API reports `CLINICAL_SERVICE_DISABLED`.

The GitHub workflow validates code, the built web app and browser flows. An actual AI Studio import or workspace pull still requires verification in that workspace; passing CI does not attest deployment.

## AI access and release status

Setting `GEMINI_API_KEY` alone does not activate AI endpoints. They require the explicit clinical release flag and a server-only bearer token as documented in README.md. Do not add the token to browser code or build environment variables. Browser AI integration remains unavailable pending authenticated user sessions.

This is an internal educational preview, not a release-cleared patient-facing service. Existing clinical signoff, privacy, Firebase/App Check and real-device release gates still apply.
