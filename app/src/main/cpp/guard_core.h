#pragma once
#include <cstddef>
#include <cstdint>

namespace guard {

enum Result : int { kOk = 0, kMismatch = 1, kError = 2, kUnchecked = 3 };

void sha256(const std::uint8_t* data, std::size_t len, std::uint8_t out[32]);
bool parseHex32(const char* hex, std::uint8_t out[32]);
Result verifyApk(const char* apkPath, const char* expectedHex, std::uint8_t* fingerprint = nullptr);

void xorBytes(const std::uint8_t a[32], const std::uint8_t b[32], std::uint8_t out[32]);
void toHex32(const std::uint8_t in[32], char out[65]);
void wipeBytes(std::uint8_t* buf, std::size_t len);
bool unmaskFingerprintHex(const char* maskedHex, const std::uint8_t mask[32], char hexOut[65]);

enum TextIntegrity : int { kTextOk = 0, kTextMismatch = 1, kTextUnsupported = 2 };

extern "C" unsigned char g_frameoutTextRef[40];

bool locateOwnTextSegment(std::uintptr_t selfAddr, std::uintptr_t& outStart, std::uintptr_t& outEnd);
TextIntegrity verifyTextIntegrity();

enum TracerState : int { kTracerNone = 0, kTracerAttached = 1, kTracerUnknown = 2 };

TracerState checkTracerPid();

enum RootState : int { kRootClean = 0, kRootDetected = 1 };

RootState checkRootIndicators();

enum FridaPortState : int { kFridaPortClosed = 0, kFridaPortOpen = 1, kFridaPortUnknown = 2 };

FridaPortState checkFridaDefaultPort();

}
