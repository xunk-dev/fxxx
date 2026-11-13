#pragma once
#include <cstdint>

typedef int jint;
typedef unsigned char jboolean;
typedef signed char jbyte;
typedef void* jobject;
typedef jobject jclass;
typedef jobject jstring;
typedef jobject jbyteArray;
#define JNI_OK 0
#define JNI_ERR (-1)
#define JNI_VERSION_1_6 0x00010006
#define JNIEXPORT
#define JNICALL

struct JNINativeMethod {
    char* name;
    char* signature;
    void* fnPtr;
};

class JNIEnv_ {
public:
    const char* GetStringUTFChars(jstring, jboolean*) { return "/tmp/stub.apk"; }
    void ReleaseStringUTFChars(jstring, const char*) {}
    jclass FindClass(const char*) { return reinterpret_cast<jclass>(0x1); }
    jbyteArray NewByteArray(jint) { return reinterpret_cast<jbyteArray>(0x2); }
    void SetByteArrayRegion(jbyteArray, jint, jint, const jbyte*) {}
    jint RegisterNatives(jclass, const JNINativeMethod*, jint) { return JNI_OK; }
};
typedef JNIEnv_ JNIEnv;

class JavaVM_ {
public:
    jint GetEnv(void**, jint) { return JNI_ERR; }
};
typedef JavaVM_ JavaVM;
