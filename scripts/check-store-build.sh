#!/usr/bin/env bash
set -euo pipefail
MODE="static"
ROOT="."
while [ "$#" -gt 0 ]; do
  case "$1" in
    --built) MODE="built" ;;
    --root) ROOT="${2:?--root needs a directory}"; shift ;;
    *) echo "usage: check-store-build.sh [--built] [--root DIR]" >&2; exit 2 ;;
  esac
  shift
done
python3 - "$MODE" "$ROOT" <<'PY'
import re
import sys
from pathlib import Path

mode, root = sys.argv[1], Path(sys.argv[2])
release = re.compile(r"(assemble|bundle|package)Release")
upload = re.compile(r"upload-google-play|androidpublisher|fastlane\s+supply")
errors = []


def gradle_release_commands(text):
    joined = re.sub(r"\\\s*\n", " ", text)
    lines = [line.strip() for line in joined.splitlines() if not line.strip().startswith("#")]
    return [line for line in lines if re.search(r"\bgradlew?\b", line) and release.search(line)]


def check_workflow(path):
    text = path.read_text(encoding="utf-8")
    commands = gradle_release_commands(text)
    for command in commands:
        if "storeBuild=true" in command and "freeBuild=true" in command:
            errors.append(f"{path.name}: storeBuild and freeBuild together: {command}")
    if not upload.search(text):
        return
    if not commands:
        errors.append(f"{path.name}: uploads to Google Play but has no gradle release build command")
    for command in commands:
        if "-PstoreBuild=true" not in command:
            errors.append(f"{path.name}: Play upload built without -PstoreBuild=true: {command}")
        for banned in ("freeBuild", "allowUnsignedRelease", "PLAY_BILLING=false", "storeBuild=false"):
            if banned in command:
                errors.append(f"{path.name}: Play upload build uses {banned}: {command}")


def check_workflows():
    folder = root / ".github" / "workflows"
    files = sorted(folder.glob("*.y*ml")) if folder.is_dir() else []
    if not files:
        errors.append(f"no workflows found under {folder}")
    for path in files:
        check_workflow(path)


def check_built():
    base = root / "app" / "build" / "generated" / "source" / "buildConfig" / "release"
    found = sorted(base.rglob("BuildConfig.java")) if base.is_dir() else []
    if not found:
        errors.append(f"release BuildConfig.java not found under {base}")
    for path in found:
        text = path.read_text(encoding="utf-8")
        if not re.search(r"boolean\s+PLAY_BILLING\s*=\s*true\s*;", text):
            errors.append(f"{path}: PLAY_BILLING is not true, this build would unlock every feature")
        if not re.search(r'String\s+REVENUECAT_API_KEY\s*=\s*"goog_', text):
            errors.append(f"{path}: REVENUECAT_API_KEY does not start with goog_")


if mode == "built":
    check_built()
else:
    check_workflows()
for error in errors:
    print("FAIL:", error)
if errors:
    raise SystemExit(1)
print("OK: store build guard (" + mode + ")")
PY
