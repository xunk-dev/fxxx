import hashlib
import struct
import sys

PT_LOAD = 1
PF_X = 0x1
SECTION_NAME = ".frameout_text_ref"


def read_ident(data):
    if data[:4] != b"\x7fELF":
        raise SystemExit("not an ELF file")
    ei_class = data[4]
    ei_data = data[5]
    if ei_class not in (1, 2):
        raise SystemExit("unsupported ELF class")
    if ei_data != 1:
        raise SystemExit("only little-endian ELF is supported")
    return ei_class


def parse_ehdr(data, is64):
    # Layout after the 16-byte e_ident, identical field order for ELF32/ELF64:
    # e_type, e_machine, e_version, e_entry, e_phoff, e_shoff, e_flags,
    # e_ehsize, e_phentsize, e_phnum, e_shentsize, e_shnum, e_shstrndx
    addr_fmt = "Q" if is64 else "I"
    fmt = "<HHI" + addr_fmt * 3 + "IHHHHHH"
    names = ["e_type", "e_machine", "e_version", "e_entry", "e_phoff", "e_shoff", "e_flags",
             "e_ehsize", "e_phentsize", "e_phnum", "e_shentsize", "e_shnum", "e_shstrndx"]
    fields = struct.unpack_from(fmt, data, 16)
    eh = dict(zip(names, fields))
    return eh


def find_text_segment(data, is64, eh):
    matches = []
    for i in range(eh["e_phnum"]):
        base = eh["e_phoff"] + i * eh["e_phentsize"]
        if is64:
            p_type, p_flags, p_offset, _va, _pa, p_filesz, _memsz, _align = struct.unpack_from("<IIQQQQQQ", data, base)
        else:
            p_type, p_offset, _va, _pa, p_filesz, _memsz, p_flags, _align = struct.unpack_from("<IIIIIIII", data, base)
        if p_type == PT_LOAD and (p_flags & PF_X):
            matches.append((p_offset, p_filesz))
    if len(matches) != 1:
        raise SystemExit("expected exactly one executable PT_LOAD segment, found %d" % len(matches))
    return matches[0]


def find_ref_section(data, is64, eh):
    shoff, shentsize, shnum, shstrndx = eh["e_shoff"], eh["e_shentsize"], eh["e_shnum"], eh["e_shstrndx"]
    strtab_base = shoff + shstrndx * shentsize
    if is64:
        _n, _t, _f, _a, strtab_off, _s = struct.unpack_from("<IIQQQQ", data, strtab_base)[:6]
    else:
        _n, _t, _f, _a, strtab_off, _s = struct.unpack_from("<IIIIII", data, strtab_base)[:6]
    found = None
    for i in range(shnum):
        base = shoff + i * shentsize
        if is64:
            name_off, _type, _flags, _addr, sh_offset, sh_size = struct.unpack_from("<IIQQQQ", data, base)[:6]
        else:
            name_off, _type, _flags, _addr, sh_offset, sh_size = struct.unpack_from("<IIIIII", data, base)[:6]
        end = data.index(b"\x00", strtab_off + name_off)
        name = data[strtab_off + name_off:end].decode("ascii", "replace")
        if name == SECTION_NAME:
            found = (sh_offset, sh_size)
    if found is None:
        raise SystemExit("section %s not found; was it compiled in and not GC'd?" % SECTION_NAME)
    if found[1] != 40:
        raise SystemExit("section %s has unexpected size %d (expected 40)" % (SECTION_NAME, found[1]))
    return found


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: inject-text-hash.py <elf-path>")
    path = sys.argv[1]
    with open(path, "rb") as f:
        data = bytearray(f.read())
    ei_class = read_ident(data)
    is64 = ei_class == 2
    eh = parse_ehdr(data, is64)
    text_off, text_len = find_text_segment(data, is64, eh)
    digest = hashlib.sha256(bytes(data[text_off:text_off + text_len])).digest()
    payload = digest + struct.pack("<Q", text_len)
    ref_off, _ref_len = find_ref_section(data, is64, eh)
    data[ref_off:ref_off + 40] = payload
    with open(path, "r+b") as f:
        f.seek(ref_off)
        f.write(payload)
    print("OK: injected .text hash %s len=%d (segment offset=0x%x) into %s @0x%x" % (
        digest.hex(), text_len, text_off, SECTION_NAME, ref_off))


if __name__ == "__main__":
    main()
