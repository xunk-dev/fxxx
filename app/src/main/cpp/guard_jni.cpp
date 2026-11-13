#include <jni.h>
#include "guard_core.h"

#ifndef FRAMEOUT_CERT_MASKED_HEX_STR
#define FRAMEOUT_CERT_MASKED_HEX_STR ""
#endif

namespace {

constexpr std::uint8_t kFingerprintMask[32] = {
    0xa3, 0x2e, 0xc5, 0xc4, 0x22, 0xcb, 0x5e, 0x13, 0xe2, 0x09, 0x39, 0xe9, 0xfd, 0xed, 0xb6, 0x1a,
    0x07, 0x73, 0xae, 0xb5, 0x51, 0x1f, 0x6c, 0xf3, 0x7e, 0xb5, 0xb5, 0x8e, 0x26, 0x64, 0xe8, 0x6f};

jint nativeCheck(JNIEnv* env, jclass, jstring apkPath) {
    if (!apkPath) return guard::kError;
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    if (!path) return guard::kError;
    char hex[65];
    const bool unmasked = guard::unmaskFingerprintHex(FRAMEOUT_CERT_MASKED_HEX_STR, kFingerprintMask, hex);
    int result = unmasked ? guard::verifyApk(path, hex) : guard::kError;
    guard::wipeBytes(reinterpret_cast<std::uint8_t*>(hex), sizeof(hex));
    env->ReleaseStringUTFChars(apkPath, path);
    if (result == guard::kOk && guard::verifyTextIntegrity() == guard::kTextMismatch) {
        result = guard::kMismatch;
    }
    return result;
}

jbyteArray nativeFingerprint(JNIEnv* env, jclass, jstring apkPath) {
    if (!apkPath) return nullptr;
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    if (!path) return nullptr;
    char hex[65];
    const bool unmasked = guard::unmaskFingerprintHex(FRAMEOUT_CERT_MASKED_HEX_STR, kFingerprintMask, hex);
    std::uint8_t fingerprint[32];
    int result = unmasked ? guard::verifyApk(path, hex, fingerprint) : guard::kError;
    guard::wipeBytes(reinterpret_cast<std::uint8_t*>(hex), sizeof(hex));
    env->ReleaseStringUTFChars(apkPath, path);
    if (result == guard::kOk && guard::verifyTextIntegrity() == guard::kTextMismatch) {
        result = guard::kMismatch;
    }
    jbyteArray out = nullptr;
    if (result == guard::kOk && (out = env->NewByteArray(32)) != nullptr) {
        env->SetByteArrayRegion(out, 0, 32, reinterpret_cast<const jbyte*>(fingerprint));
    }
    guard::wipeBytes(fingerprint, 32);
    return out;
}

jint nativeTracerCheck(JNIEnv*, jclass) {
    return static_cast<jint>(guard::checkTracerPid());
}

jint nativeRootCheck(JNIEnv*, jclass) {
    return static_cast<jint>(guard::checkRootIndicators());
}

jint nativeFridaPortCheck(JNIEnv*, jclass) {
    return static_cast<jint>(guard::checkFridaDefaultPort());
}

}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK || !env) return JNI_ERR;
    jclass cls = env->FindClass("com/frameout/app/security/NativeGuard");
    if (!cls) return JNI_ERR;
    static const JNINativeMethod methods[] = {
        {const_cast<char*>("check"), const_cast<char*>("(Ljava/lang/String;)I"), reinterpret_cast<void*>(nativeCheck)},
        {const_cast<char*>("signerFingerprint"), const_cast<char*>("(Ljava/lang/String;)[B"), reinterpret_cast<void*>(nativeFingerprint)},
        {const_cast<char*>("tracerCheck"), const_cast<char*>("()I"), reinterpret_cast<void*>(nativeTracerCheck)},
        {const_cast<char*>("rootCheck"), const_cast<char*>("()I"), reinterpret_cast<void*>(nativeRootCheck)},
        {const_cast<char*>("fridaPortCheck"), const_cast<char*>("()I"), reinterpret_cast<void*>(nativeFridaPortCheck)},
    };
    if (env->RegisterNatives(cls, methods, 5) != JNI_OK) return JNI_ERR;
    return JNI_VERSION_1_6;
}
