# Play Integrity server (Cloud Functions)

Functions: `issueIntegrityNonce`, `verifyIntegrityToken`. Client: `app/.../security/IntegrityVerifier.kt`.

## Deploy (once)
1. Firebase console: create the Firestore database (Native mode) and enable **App Check** with the **Play Integrity** provider for the Android app.
2. Google Cloud console (project `frame-out`): enable **Play Integrity API**. Play Console > App integrity: link the same Cloud project.
3. Firestore > Indexes > TTL: add a policy on field `expiresAt` for collection groups `integrityNonces` and `integrityRate`.
4. `cp .env.example .env`, fill `CERT_SHA256`.
5. `npm install && firebase deploy --only functions,firestore:rules` (Blaze plan is required for Cloud Functions).
6. Check in `gradle.properties` that `integrityCloudProject` is the Cloud project number linked in Play Console.

## Behaviour
- Store build only (`-PstoreBuild=true`). Debug builds are not App Check enrolled and simply fail open.
- Any network/server error = no penalty. Two consecutive `isGenuine=false` = Pro becomes session-only.
- Watch the function logs for `integrity rejected` and the `failed` array to see which check fails.
