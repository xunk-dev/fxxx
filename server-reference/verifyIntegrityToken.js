// Firebase Cloud Functions (v1) - Play Integrity verification, server side.
// The client (PlayIntegrityGuard.kt) is only a courier: every decision happens here.
//
// One-time setup:
//   firebase init functions && cd functions && npm install firebase-admin firebase-functions googleapis
//   Enable "Play Integrity API" on the Cloud project linked to the app in Play Console.
//   Firebase App Check must be enabled in the app (Play Integrity provider): both functions refuse
//   calls without a valid App Check token, so strangers cannot burn your quota.
//   The function runs with its own service account (Application Default Credentials):
//   no key file, no JSON key in the repo.
//   functions:config / env: PACKAGE_NAME=com.frameout.app and CERT_SHA256=<64 hex of your Play signing cert>
//   firebase deploy --only functions
//
// Flow: app -> issueIntegrityNonce -> Play Integrity token(nonce) -> verifyIntegrityToken -> { isGenuine }
// The nonce is random, single use and expires, so a captured token cannot be replayed.

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const { google } = require("googleapis");

admin.initializeApp();
const db = admin.firestore();

const PACKAGE_NAME = process.env.PACKAGE_NAME || "com.frameout.app";
const CERT_SHA256_HEX = (process.env.CERT_SHA256 || "").toLowerCase().replace(/[^0-9a-f]/g, "");
const NONCE_TTL_MS = 2 * 60 * 1000;
const MAX_TOKEN_AGE_MS = 5 * 60 * 1000;
const callable = functions.runWith({ enforceAppCheck: true });

const b64url = (buf) => buf.toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");

exports.issueIntegrityNonce = callable.https.onCall(async () => {
    const nonce = b64url(crypto.randomBytes(32));
    await db.collection("integrityNonces").doc(nonce).set({ expiresAt: Date.now() + NONCE_TTL_MS });
    return { nonce };
});

exports.verifyIntegrityToken = callable.https.onCall(async (data) => {
    const token = data && data.token;
    const nonce = data && data.nonce;
    if (typeof token !== "string" || typeof nonce !== "string") {
        throw new functions.https.HttpsError("invalid-argument", "token and nonce are required");
    }
    if (CERT_SHA256_HEX.length !== 64) {
        throw new functions.https.HttpsError("failed-precondition", "CERT_SHA256 is not configured");
    }

    // Single use: the nonce must exist, be fresh, and is deleted atomically.
    const ref = db.collection("integrityNonces").doc(nonce);
    const fresh = await db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        if (!snap.exists) return false;
        tx.delete(ref);
        return snap.data().expiresAt >= Date.now();
    });
    if (!fresh) return { isGenuine: false };

    const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/playintegrity"] });
    const playintegrity = google.playintegrity({ version: "v1", auth });
    const res = await playintegrity.v1.decodeIntegrityToken({
        packageName: PACKAGE_NAME,
        requestBody: { integrityToken: token },
    });

    const p = res.data.tokenPayloadExternal || {};
    const request = p.requestDetails || {};
    const app = p.appIntegrity || {};
    const device = p.deviceIntegrity || {};
    const expectedDigest = b64url(Buffer.from(CERT_SHA256_HEX, "hex"));

    const isGenuine =
        request.requestPackageName === PACKAGE_NAME &&
        request.nonce === nonce &&
        Date.now() - Number(request.timestampMillis || 0) <= MAX_TOKEN_AGE_MS &&
        app.appRecognitionVerdict === "PLAY_RECOGNIZED" &&
        app.packageName === PACKAGE_NAME &&
        (app.certificateSha256Digest || []).includes(expectedDigest) &&
        (device.deviceRecognitionVerdict || []).includes("MEETS_DEVICE_INTEGRITY");

    // Never return the raw verdict to the app: only the final decision.
    return { isGenuine };
});
