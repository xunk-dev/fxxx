import hashlib, io, os, random, struct, subprocess, sys, zipfile

BIN = sys.argv[1]


def u32(n):
    return struct.pack("<I", n)


def lp(b):
    return u32(len(b)) + b


def build(certs, ids=(0x7109871A,), no_block=False, corrupt=False, comment=b""):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        z.writestr("a.txt", "hello")
    z = buf.getvalue()
    eocd = z.rfind(b"PK\x05\x06")
    cd = struct.unpack("<I", z[eocd + 16:eocd + 20])[0]
    signers = lp(lp(lp(lp(b"dg") + lp(b"".join(lp(c) for c in certs)))))
    pairs = b"".join(struct.pack("<Q", 4 + len(signers)) + u32(i) + signers for i in ids)
    size = len(pairs) + 24
    block = struct.pack("<Q", size) + pairs + struct.pack("<Q", size) + b"APK Sig Block 42"
    if corrupt:
        block = block[:-5] + b"XXXXX"
    if no_block:
        block = b""
    e = bytearray(z[eocd:])
    e[16:20] = u32(cd + len(block))
    if comment:
        e[20:22] = struct.pack("<H", len(comment))
        e += comment
    return z[:cd] + block + z[cd:eocd] + bytes(e)


def run(data, expected):
    open("x.apk", "wb").write(data)
    return int(subprocess.check_output([BIN, "verify", "x.apk", expected]).decode())


cert_a, cert_b = os.urandom(300), os.urandom(280)
ha, hb = hashlib.sha256(cert_a).hexdigest(), hashlib.sha256(cert_b).hexdigest()
cases = [
    (build([cert_a]), ha, 0), (build([cert_a], ids=(0xF05368C0,)), ha, 0), (build([cert_a], ids=(0x1B93AD61,)), ha, 0),
    (build([cert_a]), hb, 1), (build([cert_a], comment=b"hello"), ha, 0), (build([cert_a, cert_b]), ha, 1),
    (build([cert_a], no_block=True), ha, 2), (build([cert_a], corrupt=True), ha, 2), (build([cert_a]), "", 3),
]
failed = [i for i, (d, e, want) in enumerate(cases) if run(d, e) != want]
for n in [0, 1, 55, 56, 64, 129, 100000]:
    data = os.urandom(n)
    open("v.bin", "wb").write(data)
    if subprocess.check_output([BIN, "sha", "v.bin"]).decode().strip() != hashlib.sha256(data).hexdigest():
        failed.append("sha%d" % n)

def run_fp(data, expected):
    open("x.apk", "wb").write(data)
    code, fp = subprocess.check_output([BIN, "fp", "x.apk", expected]).decode().split()
    return int(code), fp


ZERO = "0" * 64
fp_cases = [
    (build([cert_a]), ha, 0, ha), (build([cert_a], ids=(0xF05368C0,)), ha, 0, ha),
    (build([cert_a], ids=(0x1B93AD61,)), ha, 0, ha), (build([cert_a], ids=(0x7109871A, 0xF05368C0)), ha, 0, ha),
    (build([cert_a, cert_a]), ha, 0, ha), (build([cert_a], comment=b"hello"), ha, 0, ha),
    (build([cert_b]), hb, 0, hb), (build([cert_a]), hb, 1, ZERO), (build([cert_a, cert_b]), ha, 1, ZERO),
    (build([cert_a], no_block=True), ha, 2, ZERO), (build([cert_a], corrupt=True), ha, 2, ZERO),
    (build([cert_a]), "", 3, ZERO), (build([cert_a]), "zz" * 32, 2, ZERO), (build([cert_a]), ha[:-2], 2, ZERO),
]
for i, (d, e, want_code, want_fp) in enumerate(fp_cases):
    if run_fp(d, e) != (want_code, want_fp):
        failed.append("fp%d" % i)
open("x.apk", "wb").write(b"")
if subprocess.check_output([BIN, "fp", "x.apk", ha]).decode().split() != ["2", ZERO]:
    failed.append("fp-empty-file")
if subprocess.check_output([BIN, "fp", "missing.apk", ha]).decode().split() != ["2", ZERO]:
    failed.append("fp-missing-file")
good = build([cert_a])
random.seed(1)
for _ in range(200):
    b = bytearray(good)
    for _ in range(3):
        b[random.randrange(len(b))] = random.randrange(256)
    open("x.apk", "wb").write(b)
    if subprocess.run([BIN, "verify", "x.apk", ha], capture_output=True).returncode != 0:
        failed.append("fuzz")
    code, fp = run_fp(bytes(b), ha)
    if (code == 0 and fp != ha) or (code != 0 and fp != ZERO):
        failed.append("fuzz-fp")
MASK = "a32ec5c422cb5e13e20939e9fdedb61a0773aeb5511f6cf37eb5b58e2664e86f"


def xor_hex(a, b):
    return "".join("%02x" % (int(a[i:i + 2], 16) ^ int(b[i:i + 2], 16)) for i in range(0, 64, 2))


def run_unmask(masked_hex, mask_hex):
    out = subprocess.check_output([BIN, "unmask", masked_hex, mask_hex]).decode().strip().split(" ", 1)
    ok = int(out[0])
    return ok, (out[1] if len(out) > 1 else "")


real_a, real_b = os.urandom(32).hex(), os.urandom(32).hex()
unmask_cases = [
    (xor_hex(real_a, MASK), MASK, 1, real_a),
    (xor_hex(real_b, MASK), MASK, 1, real_b),
    ("", MASK, 1, ""),
    ("zz" * 32, MASK, 0, ""),
    (real_a[:-2], MASK, 0, ""),
]
for i, (masked, mask_hex, want_ok, want_hex) in enumerate(unmask_cases):
    if run_unmask(masked, mask_hex) != (want_ok, want_hex):
        failed.append("unmask%d" % i)
random.seed(2)
for _ in range(50):
    real = os.urandom(32).hex()
    masked = xor_hex(real, MASK)
    if run_unmask(masked, MASK) != (1, real):
        failed.append("unmask-fuzz")

def run_textcheck():
    out = subprocess.check_output([BIN, "textcheck"]).decode().strip().split()
    return int(out[0]), int(out[1]), int(out[2])


t_before, t_located, t_after = run_textcheck()
if t_before != 0:
    failed.append("textcheck-before-not-ok(%d)" % t_before)
if t_located != 1:
    failed.append("textcheck-segment-not-located")
if t_after != 1:
    failed.append("textcheck-after-corruption-not-flagged(%d)" % t_after)

print("FAILED:", failed) if failed else print("OK: native guard host tests passed")
sys.exit(1 if failed else 0)
