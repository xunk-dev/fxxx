
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const crypto = require("crypto");
const { google } = require("googleapis");

admin.initializeApp();
const db = admin.firestore();
const log = functions.logger;

function b64url(buf) {
    return buf.toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

const PACKAGE_NAME = process.env.PACKAGE_NAME || "com.frameout.app";
const REQUIRE_STRONG = process.env.REQUIRE_STRONG === "true";
const REQUIRE_LICENSED = process.env.REQUIRE_LICENSED === "true";
const CERT_DIGESTS = (process.env.CERT_SHA256 || "")
    .split(",")
    .map((s) => s.toLowerCase().replace(/[^0-9a-f]/g, ""))
    .filter((hex) => hex.length === 64)
    .map((hex) => b64url(Buffer.from(hex, "hex")));

const NONCE_TTL_MS = 2 * 60 * 1000;
const MAX_TOKEN_AGE_MS = 5 * 60 * 1000;
const MAX_CLOCK_SKEW_MS = 60 * 1000;
const MAX_TOKEN_LENGTH = 16 * 1024;
const NONCE_RE = /^[A-Za-z0-9_-]{43}$/;
const RATE_WINDOW_MS = 60 * 1000;
const RATE_MAX_ISSUE = 20;
const RATE_MAX_VERIFY = 30;

const callable = functions.runWith({
    enforceAppCheck: true,
    maxInstances: 10,
    timeoutSeconds: 15,
    memory: "256MB",
});
const auth = new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/playintegrity"] });
const playintegrity = google.playintegrity({ version: "v1", auth });

const isFreshTimestamp = (ts) => {
    if (!Number.isFinite(ts)) return false;
    const age = Date.now() - ts;
    return age <= MAX_TOKEN_AGE_MS && age >= -MAX_CLOCK_SKEW_MS;
};

function requireApp(context) {
    if (!context.app || typeof context.app.appId !== "string") {
        throw new functions.https.HttpsError("failed-precondition", "App Check required");
    }
    return context.app.appId;
}

async function underRateLimit(context, bucket, max) {
    const ip = (context.rawRequest && context.rawRequest.ip) || "unknown";
    const windowId = Math.floor(Date.now() / RATE_WINDOW_MS);
    const key = crypto.createHash("sha256").update(bucket + "|" + ip).digest("hex").slice(0, 40);
    const ref = db.collection("integrityRate").doc(key + "_" + windowId);
    return db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const n = snap.exists ? snap.get("n") : 0;
        if (n >= max) return false;
        tx.set(ref, {
            n: n + 1,
            expiresAt: admin.firestore.Timestamp.fromMillis((windowId + 2) * RATE_WINDOW_MS),
        });
        return true;
    });
}

exports.issueIntegrityNonce = callable.https.onCall(async (data, context) => {
    const appId = requireApp(context);
    if (!(await underRateLimit(context, "issue", RATE_MAX_ISSUE))) {
        throw new functions.https.HttpsError("resource-exhausted", "too many requests");
    }
    const nonce = b64url(crypto.randomBytes(32));
    await db.collection("integrityNonces").doc(nonce).set({
        appId,
        expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + NONCE_TTL_MS),
    });
    return { nonce };
});

exports.verifyIntegrityToken = callable.https.onCall(async (data, context) => {
    const appId = requireApp(context);
    const token = data && data.token;
    const nonce = data && data.nonce;
    if (typeof token !== "string" || typeof nonce !== "string" || token.length === 0 || token.length > MAX_TOKEN_LENGTH) {
        throw new functions.https.HttpsError("invalid-argument", "token and nonce are required");
    }
    if (!NONCE_RE.test(nonce)) return { isGenuine: false };
    if (CERT_DIGESTS.length === 0) {
        log.error("CERT_SHA256 is not configured");
        throw new functions.https.HttpsError("failed-precondition", "server not configured");
    }
    if (!(await underRateLimit(context, "verify", RATE_MAX_VERIFY))) {
        throw new functions.https.HttpsError("resource-exhausted", "too many requests");
    }

    const ref = db.collection("integrityNonces").doc(nonce);
    const fresh = await db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        if (!snap.exists) return false;
        tx.delete(ref);
        return snap.get("appId") === appId && snap.get("expiresAt").toMillis() >= Date.now();
    });
    if (!fresh) return { isGenuine: false };

    let res;
    try {
        res = await playintegrity.v1.decodeIntegrityToken({
            packageName: PACKAGE_NAME,
            requestBody: { integrityToken: token },
        });
    } catch (err) {
        const status = err && (err.code || (err.response && err.response.status));
        if (status === 400) return { isGenuine: false };
        log.error("decodeIntegrityToken failed", { status });
        throw new functions.https.HttpsError("unavailable", "integrity service unavailable, retry");
    }

    const p = (res.data && res.data.tokenPayloadExternal) || {};
    const request = p.requestDetails || {};
    const app = p.appIntegrity || {};
    const device = p.deviceIntegrity || {};
    const account = p.accountDetails || {};

    const checks = {
        requestPackage: request.requestPackageName === PACKAGE_NAME,
        nonce: request.nonce === nonce,
        fresh: isFreshTimestamp(Number(request.timestampMillis)),
        recognized: app.appRecognitionVerdict === "PLAY_RECOGNIZED",
        appPackage: app.packageName === PACKAGE_NAME,
        cert: (app.certificateSha256Digest || []).some((d) => CERT_DIGESTS.includes(d)),
        device: (device.deviceRecognitionVerdict || []).includes(
            REQUIRE_STRONG ? "MEETS_STRONG_INTEGRITY" : "MEETS_DEVICE_INTEGRITY"
        ),
    };
    if (REQUIRE_LICENSED) checks.licensed = account.appLicensingVerdict === "LICENSED";

    const failed = Object.keys(checks).filter((k) => !checks[k]);
    if (failed.length) log.warn("integrity rejected", { failed });

    return { isGenuine: failed.length === 0 };
});
