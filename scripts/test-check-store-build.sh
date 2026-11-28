#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK="$HERE/check-store-build.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAILED=0

workflow() {
  mkdir -p "$WORK/$1/.github/workflows"
  printf '%s\n' "$2" > "$WORK/$1/.github/workflows/play.yml"
}

built() {
  local dir="$WORK/$1/app/build/generated/source/buildConfig/release/com/frameout/app"
  mkdir -p "$dir"
  printf 'public final class BuildConfig {\n  public static final boolean PLAY_BILLING = %s;\n  public static final String REVENUECAT_API_KEY = "%s";\n}\n' "$2" "$3" > "$dir/BuildConfig.java"
}

expect() {
  local name="$1" want="$2"
  shift 2
  local code=0
  bash "$CHECK" "$@" >/dev/null 2>&1 || code=$?
  if [ "$code" -ne "$want" ]; then
    echo "FAIL: $name expected exit $want got $code"
    FAILED=1
  else
    echo "ok: $name"
  fi
}

UPLOAD='      - uses: r0adkll/upload-google-play@v1.1.5'

workflow store_ok "      - run: gradle :app:bundleRelease -PstoreBuild=true -PadsEnabled=true --no-daemon
$UPLOAD"
expect "store build with upload passes" 0 --root "$WORK/store_ok"

workflow no_flag "      - run: gradle :app:bundleRelease --no-daemon
$UPLOAD"
expect "upload without storeBuild fails" 1 --root "$WORK/no_flag"

workflow free_flag "      - run: gradle :app:bundleRelease -PfreeBuild=true --no-daemon
$UPLOAD"
expect "upload with freeBuild fails" 1 --root "$WORK/free_flag"

workflow unsigned "      - run: gradle :app:bundleRelease -PstoreBuild=true -PallowUnsignedRelease=true
$UPLOAD"
expect "upload with unsigned flag fails" 1 --root "$WORK/unsigned"

workflow no_build "$UPLOAD"
expect "upload with no gradle build fails" 1 --root "$WORK/no_build"

workflow continued "      - run: |
          gradle :app:bundleRelease \\
            -PstoreBuild=true --no-daemon
$UPLOAD"
expect "continued command passes" 0 --root "$WORK/continued"

workflow both "      - run: gradle :app:assembleRelease -PstoreBuild=true -PfreeBuild=true"
expect "both flags together fail" 1 --root "$WORK/both"

workflow free_only "      - run: gradle :app:assembleRelease -PfreeBuild=true"
expect "free build without upload passes" 0 --root "$WORK/free_only"

mkdir -p "$WORK/empty"
expect "no workflows fails" 1 --root "$WORK/empty"

built built_ok true goog_abc123
expect "built store config passes" 0 --built --root "$WORK/built_ok"

built built_free false goog_abc123
expect "built free config fails" 1 --built --root "$WORK/built_free"

built built_key true test_abc123
expect "built config with test key fails" 1 --built --root "$WORK/built_key"

expect "missing built config fails" 1 --built --root "$WORK/empty"

expect "bad argument is rejected" 2 --nope

exit "$FAILED"
