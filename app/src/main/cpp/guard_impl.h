#pragma once
#include "guard_core.h"
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <fcntl.h>
#include <netinet/in.h>
#include <poll.h>
#include <sys/socket.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <string>
#include <vector>

namespace guard {
namespace {

constexpr std::uint32_t kK[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};

inline std::uint32_t rotr(std::uint32_t x, unsigned n) { return (x >> n) | (x << (32 - n)); }

void compress(std::uint32_t h[8], const std::uint8_t block[64]) {
    std::uint32_t w[64];
    for (int i = 0; i < 16; ++i) {
        w[i] = (std::uint32_t(block[i * 4]) << 24) | (std::uint32_t(block[i * 4 + 1]) << 16) |
               (std::uint32_t(block[i * 4 + 2]) << 8) | std::uint32_t(block[i * 4 + 3]);
    }
    for (int i = 16; i < 64; ++i) {
        const std::uint32_t s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >> 3);
        const std::uint32_t s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >> 10);
        w[i] = w[i - 16] + s0 + w[i - 7] + s1;
    }
    std::uint32_t a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
    for (int i = 0; i < 64; ++i) {
        const std::uint32_t t1 = hh + (rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)) + ((e & f) ^ (~e & g)) + kK[i] + w[i];
        const std::uint32_t t2 = (rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)) + ((a & b) ^ (a & c) ^ (b & c));
        hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2;
    }
    h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh;
}

class Reader {
public:
    Reader(const std::uint8_t* p, std::size_t n) : p_(p), n_(n) {}
    bool u32(std::uint32_t& v) {
        if (n_ - pos_ < 4) return false;
        v = std::uint32_t(p_[pos_]) | (std::uint32_t(p_[pos_ + 1]) << 8) | (std::uint32_t(p_[pos_ + 2]) << 16) | (std::uint32_t(p_[pos_ + 3]) << 24);
        pos_ += 4;
        return true;
    }
    bool chunk(Reader& out) {
        std::uint32_t len = 0;
        if (!u32(len) || len > n_ - pos_) return false;
        out = Reader(p_ + pos_, len);
        pos_ += len;
        return true;
    }
    bool take(std::size_t len, Reader& out) {
        if (len > n_ - pos_) return false;
        out = Reader(p_ + pos_, len);
        pos_ += len;
        return true;
    }
    bool done() const { return pos_ == n_; }
    std::size_t remaining() const { return n_ - pos_; }
    const std::uint8_t* data() const { return p_; }
    std::size_t size() const { return n_; }
private:
    const std::uint8_t* p_;
    std::size_t n_;
    std::size_t pos_ = 0;
};

long rawOpen(const char* path) {
#if defined(__aarch64__) || defined(__x86_64__)
    return syscall(SYS_openat, AT_FDCWD, path, O_RDONLY | O_CLOEXEC, 0);
#else
    return open(path, O_RDONLY | O_CLOEXEC);
#endif
}

bool readAt(int fd, std::uint64_t off, std::uint8_t* buf, std::size_t len) {
    std::size_t done = 0;
    while (done < len) {
#if defined(__aarch64__) || defined(__x86_64__)
        if (syscall(SYS_lseek, fd, static_cast<long>(off + done), SEEK_SET) < 0) return false;
        const long n = syscall(SYS_read, fd, buf + done, len - done);
#else
        const long n = pread(fd, buf + done, len - done, static_cast<off_t>(off + done));
#endif
        if (n <= 0) return false;
        done += static_cast<std::size_t>(n);
    }
    return true;
}

constexpr std::uint32_t kSigIds[3] = {0x7109871au, 0xf05368c0u, 0x1b93ad61u};
constexpr std::size_t kMaxBlock = 32u * 1024 * 1024;
constexpr std::size_t kMaxTail = 65557;

bool locateBlock(int fd, std::vector<std::uint8_t>& pairs) {
    const long end = syscall(SYS_lseek, fd, 0L, SEEK_END);
    if (end < 22) return false;
    const std::size_t size = static_cast<std::size_t>(end);
    const std::size_t tailLen = size < kMaxTail ? size : kMaxTail;
    std::vector<std::uint8_t> tail(tailLen);
    if (!readAt(fd, size - tailLen, tail.data(), tailLen)) return false;
    std::size_t cd = 0;
    bool found = false;
    for (std::size_t i = tailLen - 22 + 1; i-- > 0;) {
        if (tail[i] != 0x50 || tail[i + 1] != 0x4b || tail[i + 2] != 5 || tail[i + 3] != 6) continue;
        const std::size_t comment = tail[i + 20] | (std::size_t(tail[i + 21]) << 8);
        if (i + 22 + comment != tailLen) continue;
        cd = std::uint32_t(tail[i + 16]) | (std::uint32_t(tail[i + 17]) << 8) | (std::uint32_t(tail[i + 18]) << 16) | (std::uint32_t(tail[i + 19]) << 24);
        found = true;
        break;
    }
    if (!found || cd < 32 || cd > size) return false;
    std::uint8_t footer[24];
    if (!readAt(fd, cd - 24, footer, 24) || std::memcmp(footer + 8, "APK Sig Block 42", 16) != 0) return false;
    std::uint64_t blockSize = 0;
    for (int i = 7; i >= 0; --i) blockSize = (blockSize << 8) | footer[i];
    if (blockSize < 24 || blockSize > kMaxBlock || blockSize > cd) return false;
    pairs.resize(static_cast<std::size_t>(blockSize) - 24);
    return readAt(fd, cd - blockSize, pairs.data(), pairs.size());
}

bool signerCerts(Reader value, const std::uint8_t expected[32], int& total, int& matching, std::uint8_t seen[32]) {
    Reader signers(nullptr, 0);
    if (!value.chunk(signers)) return false;
    while (!signers.done()) {
        Reader signer(nullptr, 0), signedData(nullptr, 0), digests(nullptr, 0), certs(nullptr, 0);
        if (!signers.chunk(signer) || !signer.chunk(signedData) || !signedData.chunk(digests) || !signedData.chunk(certs)) return false;
        while (!certs.done()) {
            Reader cert(nullptr, 0);
            if (!certs.chunk(cert)) return false;
            std::uint8_t digest[32];
            sha256(cert.data(), cert.size(), digest);
            if (total == 0) std::memcpy(seen, digest, 32);
            ++total;
            std::uint8_t diff = 0;
            for (int i = 0; i < 32; ++i) diff |= static_cast<std::uint8_t>(digest[i] ^ expected[i]);
            if (diff == 0) ++matching;
        }
    }
    return true;
}

bool parseMapsLine(const char* line, std::size_t len, std::uintptr_t& start, std::uintptr_t& end, bool& execMapping) {
    std::size_t i = 0;
    auto parseHexPtr = [&](std::uintptr_t& out) -> bool {
        std::uintptr_t v = 0;
        bool any = false;
        while (i < len) {
            const char c = line[i];
            int d;
            if (c >= '0' && c <= '9') d = c - '0';
            else if (c >= 'a' && c <= 'f') d = c - 'a' + 10;
            else if (c >= 'A' && c <= 'F') d = c - 'A' + 10;
            else break;
            v = (v << 4) | static_cast<std::uintptr_t>(d);
            ++i;
            any = true;
        }
        if (!any) return false;
        out = v;
        return true;
    };
    if (!parseHexPtr(start)) return false;
    if (i >= len || line[i] != '-') return false;
    ++i;
    if (!parseHexPtr(end)) return false;
    if (i >= len || line[i] != ' ') return false;
    ++i;
    execMapping = (i + 2 < len) && line[i + 2] == 'x';
    return end > start;
}

constexpr const char* kRootPaths[] = {
    "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sbin/su",
    "/vendor/bin/su", "/su/bin/su", "/data/local/su", "/data/local/bin/su",
    "/data/local/xbin/su", "/system/bin/.ext/.su", "/system/usr/we-need-root/su",
    "/data/adb/magisk", "/data/adb/su", "/data/adb/ksu", "/sbin/.magisk",
    "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
};
constexpr std::size_t kRootPathCount = sizeof(kRootPaths) / sizeof(kRootPaths[0]);

bool pathExists(const char* path) {
#if defined(__aarch64__) || defined(__x86_64__)
    return syscall(SYS_faccessat, AT_FDCWD, path, F_OK, 0) == 0;
#else
    return access(path, F_OK) == 0;
#endif
}

}

}
