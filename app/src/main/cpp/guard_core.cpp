#include "guard_core.h"
#include "guard_impl.h"

namespace guard {

void sha256(const std::uint8_t* data, std::size_t len, std::uint8_t out[32]) {
    std::uint32_t h[8] = {0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19};
    std::size_t i = 0;
    for (; i + 64 <= len; i += 64) compress(h, data + i);
    std::uint8_t last[128] = {0};
    const std::size_t rest = len - i;
    if (rest) std::memcpy(last, data + i, rest);
    last[rest] = 0x80;
    const std::size_t total = rest < 56 ? 64 : 128;
    const std::uint64_t bits = static_cast<std::uint64_t>(len) * 8;
    for (int k = 0; k < 8; ++k) last[total - 1 - k] = static_cast<std::uint8_t>(bits >> (8 * k));
    compress(h, last);
    if (total == 128) compress(h, last + 64);
    for (int k = 0; k < 8; ++k) {
        out[k * 4] = static_cast<std::uint8_t>(h[k] >> 24);
        out[k * 4 + 1] = static_cast<std::uint8_t>(h[k] >> 16);
        out[k * 4 + 2] = static_cast<std::uint8_t>(h[k] >> 8);
        out[k * 4 + 3] = static_cast<std::uint8_t>(h[k]);
    }
}

bool parseHex32(const char* hex, std::uint8_t out[32]) {
    if (!hex) return false;
    auto nib = [](char c) -> int {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    };
    if (std::strlen(hex) != 64) return false;
    for (int i = 0; i < 32; ++i) {
        const int hi = nib(hex[i * 2]), lo = nib(hex[i * 2 + 1]);
        if (hi < 0 || lo < 0) return false;
        out[i] = static_cast<std::uint8_t>(hi * 16 + lo);
    }
    return true;
}

void xorBytes(const std::uint8_t a[32], const std::uint8_t b[32], std::uint8_t out[32]) {
    for (int i = 0; i < 32; ++i) out[i] = static_cast<std::uint8_t>(a[i] ^ b[i]);
}

void toHex32(const std::uint8_t in[32], char out[65]) {
    static const char kDigits[] = "0123456789abcdef";
    for (int i = 0; i < 32; ++i) {
        out[i * 2] = kDigits[in[i] >> 4];
        out[i * 2 + 1] = kDigits[in[i] & 0x0F];
    }
    out[64] = '\0';
}

void wipeBytes(std::uint8_t* buf, std::size_t len) {
    volatile std::uint8_t* p = buf;
    for (std::size_t i = 0; i < len; ++i) p[i] = 0;
}

bool unmaskFingerprintHex(const char* maskedHex, const std::uint8_t mask[32], char hexOut[65]) {
    if (!maskedHex || !*maskedHex) {
        hexOut[0] = '\0';
        return true;
    }
    std::uint8_t maskedBytes[32];
    if (!parseHex32(maskedHex, maskedBytes)) return false;
    std::uint8_t realBytes[32];
    xorBytes(maskedBytes, mask, realBytes);
    toHex32(realBytes, hexOut);
    wipeBytes(maskedBytes, 32);
    wipeBytes(realBytes, 32);
    return true;
}

extern "C" {
__attribute__((section(".frameout_text_ref"), used, aligned(16)))
unsigned char g_frameoutTextRef[40] = {0};
}

bool locateOwnTextSegment(std::uintptr_t selfAddr, std::uintptr_t& outStart, std::uintptr_t& outEnd) {
    const long fd = rawOpen("/proc/self/maps");
    if (fd < 0) return false;
    std::string buf;
    char chunk[4096];
    for (;;) {
        long n;
#if defined(__aarch64__) || defined(__x86_64__)
        n = syscall(SYS_read, fd, chunk, sizeof(chunk));
#else
        n = read(static_cast<int>(fd), chunk, sizeof(chunk));
#endif
        if (n < 0) {
            syscall(SYS_close, fd);
            return false;
        }
        if (n == 0) break;
        buf.append(chunk, static_cast<std::size_t>(n));
        if (buf.size() > 8u * 1024 * 1024) {
            syscall(SYS_close, fd);
            return false;
        }
    }
    syscall(SYS_close, fd);

    bool found = false;
    std::size_t pos = 0;
    while (pos < buf.size()) {
        const std::size_t eol = buf.find('\n', pos);
        const std::size_t lineLen = (eol == std::string::npos ? buf.size() : eol) - pos;
        std::uintptr_t s = 0, e = 0;
        bool execMapping = false;
        if (parseMapsLine(buf.data() + pos, lineLen, s, e, execMapping) && execMapping) {
            if (selfAddr >= s && selfAddr < e) {
                if (found) return false;
                outStart = s;
                outEnd = e;
                found = true;
            }
        }
        pos = (eol == std::string::npos ? buf.size() : eol + 1);
    }
    return found;
}

TextIntegrity verifyTextIntegrity() {
#if defined(__aarch64__) || defined(__x86_64__) || defined(__arm__)
    const std::uintptr_t selfAddr = reinterpret_cast<std::uintptr_t>(&verifyTextIntegrity);
    std::uintptr_t start = 0, end = 0;
    if (!locateOwnTextSegment(selfAddr, start, end)) return kTextUnsupported;
    if (end <= start) return kTextUnsupported;
    std::uint64_t storedLen = 0;
    for (int i = 0; i < 8; ++i) storedLen |= static_cast<std::uint64_t>(g_frameoutTextRef[32 + i]) << (8 * i);
    if (storedLen == 0) return kTextUnsupported;
    if (storedLen > static_cast<std::uint64_t>(end - start)) return kTextMismatch;
    const std::size_t textLen = static_cast<std::size_t>(storedLen);
    std::uint8_t computed[32];
    sha256(reinterpret_cast<const std::uint8_t*>(start), textLen, computed);
    std::uint8_t diff = 0;
    for (int i = 0; i < 32; ++i) diff |= static_cast<std::uint8_t>(computed[i] ^ g_frameoutTextRef[i]);
    wipeBytes(computed, 32);
    return diff == 0 ? kTextOk : kTextMismatch;
#else
    return kTextUnsupported;
#endif
}

TracerState checkTracerPid() {
    const long fd = rawOpen("/proc/self/status");
    if (fd < 0) return kTracerUnknown;

    char buf[4096];
    std::size_t total = 0;
    for (;;) {
        if (total >= sizeof(buf) - 1) break;
#if defined(__aarch64__) || defined(__x86_64__)
        const long n = syscall(SYS_read, fd, buf + total, sizeof(buf) - 1 - total);
#else
        const long n = read(static_cast<int>(fd), buf + total, sizeof(buf) - 1 - total);
#endif
        if (n < 0) { syscall(SYS_close, fd); return kTracerUnknown; }
        if (n == 0) break;
        total += static_cast<std::size_t>(n);
    }
    syscall(SYS_close, fd);
    buf[total] = '\0';

    constexpr char marker[] = "TracerPid:";
    constexpr std::size_t markerLen = sizeof(marker) - 1;
    for (std::size_t i = 0; i + markerLen <= total; ++i) {
        if (std::memcmp(buf + i, marker, markerLen) != 0) continue;
        std::size_t j = i + markerLen;
        while (j < total && (buf[j] == ' ' || buf[j] == '\t')) ++j;
        bool any = false;
        int pid = 0;
        while (j < total && buf[j] >= '0' && buf[j] <= '9') {
            pid = pid * 10 + (buf[j] - '0');
            ++j;
            any = true;
            if (pid > 0) break; // any nonzero digit is enough to decide "attached"
        }
        if (!any) return kTracerUnknown;
        return pid != 0 ? kTracerAttached : kTracerNone;
    }
    return kTracerUnknown;
}

RootState checkRootIndicators() {
    for (std::size_t i = 0; i < kRootPathCount; ++i) {
        if (pathExists(kRootPaths[i])) return kRootDetected;
    }
    return kRootClean;
}

FridaPortState checkFridaDefaultPort() {
    const int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) return kFridaPortUnknown;

    const int flags = fcntl(fd, F_GETFL, 0);
    if (flags < 0 || fcntl(fd, F_SETFL, flags | O_NONBLOCK) < 0) {
        close(fd);
        return kFridaPortUnknown;
    }

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(27042);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    const int rc = connect(fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr));
    if (rc == 0) {
        close(fd);
        return kFridaPortOpen;
    }
    if (errno != EINPROGRESS) {
        close(fd);
        return kFridaPortClosed;
    }

    pollfd pfd{fd, POLLOUT, 0};
    const int pr = poll(&pfd, 1, 200); // short budget; this runs off the main thread (Sentinel's IO dispatcher)
    if (pr <= 0) {
        close(fd); // timeout/error => nothing answered => treat as closed, not "open"
        return kFridaPortClosed;
    }

    int soErr = 0;
    socklen_t len = sizeof(soErr);
    getsockopt(fd, SOL_SOCKET, SO_ERROR, &soErr, &len);
    close(fd);
    return soErr == 0 ? kFridaPortOpen : kFridaPortClosed;
}

Result verifyApk(const char* apkPath, const char* expectedHex, std::uint8_t* fingerprint) {
    std::uint8_t expected[32];
    std::uint8_t seen[32] = {0};
    if (fingerprint) std::memset(fingerprint, 0, 32);
    if (!expectedHex || !*expectedHex) return kUnchecked;
    if (!parseHex32(expectedHex, expected) || !apkPath) return kError;
    const long fd = rawOpen(apkPath);
    if (fd < 0) return kError;
    std::vector<std::uint8_t> pairs;
    const bool located = locateBlock(static_cast<int>(fd), pairs);
    syscall(SYS_close, fd);
    if (!located) return kError;
    int total = 0, matching = 0;
    Reader all(pairs.data(), pairs.size());
    while (all.remaining() >= 12) {
        std::uint32_t lenLo = 0, lenHi = 0, id = 0;
        if (!all.u32(lenLo) || !all.u32(lenHi) || lenHi != 0 || lenLo < 4 || !all.u32(id)) return kError;
        Reader value(nullptr, 0);
        if (!all.take(lenLo - 4, value)) return kError;
        for (std::uint32_t known : kSigIds) {
            if (id == known && !signerCerts(value, expected, total, matching, seen)) return kError;
        }
    }
    if (total == 0) return kError;
    if (matching != total) return kMismatch;
    if (fingerprint) std::memcpy(fingerprint, seen, 32);
    return kOk;
}

}
