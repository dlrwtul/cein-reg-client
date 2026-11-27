#!/usr/bin/env bash
# Compare two registration-client JARs: resources byte-for-byte, classes by
# decompiling both sides with CFR and diffing the Java output.
#
# usage: tools/compare-jars.sh <reference.jar> <candidate.jar> [workdir]
set -euo pipefail

REF=$(realpath "$1")
CAND=$(realpath "$2")
WORK=${3:-$(mktemp -d)}
CFR_VERSION=0.152
CFR="$WORK/cfr-$CFR_VERSION.jar"

mkdir -p "$WORK/ref" "$WORK/cand"
unzip -q -o "$REF" -d "$WORK/ref"
unzip -q -o "$CAND" -d "$WORK/cand"
[ -f "$CFR" ] || curl -sSfL -o "$CFR" \
  "https://repo1.maven.org/maven2/org/benf/cfr/$CFR_VERSION/cfr-$CFR_VERSION.jar"

IGNORE='^(META-INF/MANIFEST.MF|META-INF/maven/.*|git.properties)$'
(cd "$WORK/ref" && find . -type f | sed 's|^\./||' | sort) > "$WORK/ref.lst"
(cd "$WORK/cand" && find . -type f | sed 's|^\./||' | sort) > "$WORK/cand.lst"

echo "== Only in reference"; comm -23 "$WORK/ref.lst" "$WORK/cand.lst" | grep -Ev "$IGNORE" || true
echo "== Only in candidate"; comm -13 "$WORK/ref.lst" "$WORK/cand.lst" | grep -Ev "$IGNORE" || true

echo "== Resources that differ"
comm -12 "$WORK/ref.lst" "$WORK/cand.lst" | grep -v '\.class$' | grep -Ev "$IGNORE" | while read -r f; do
  cmp -s "$WORK/ref/$f" "$WORK/cand/$f" || echo "  $f"
done

echo "== Classes whose decompiled source differs"
mkdir -p "$WORK/dec/ref" "$WORK/dec/cand"
comm -12 "$WORK/ref.lst" "$WORK/cand.lst" | grep '\.class$' | grep -v '\$' | while read -r f; do
  java -jar "$CFR" "$WORK/ref/$f" --extraclasspath "$WORK/ref" --outputdir "$WORK/dec/ref" --silent true >/dev/null 2>&1 || true
  java -jar "$CFR" "$WORK/cand/$f" --extraclasspath "$WORK/cand" --outputdir "$WORK/dec/cand" --silent true >/dev/null 2>&1 || true
done
diff -ru -I 'Decompiled with CFR' "$WORK/dec/ref" "$WORK/dec/cand" > "$WORK/classes.diff" || true
grep -E '^\+\+\+ ' "$WORK/classes.diff" | sed "s|^+++ $WORK/dec/cand/|  |; s|\t.*||" || true
echo
echo "Full decompiled diff: $WORK/classes.diff"
