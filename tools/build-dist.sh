#!/usr/bin/env bash
# Generate the two JARs to drop into the lib/ folder of a registration client
# downloaded from a MOSIP 1.2.0.2 server:
#   - registration-client-1.2.0.2.jar   : built from this repository (CEIN customisation)
#   - registration-services-1.2.0.2.jar : official MOSIP 1.2.0.2 JAR (classes unchanged) with
#       props/mosip-application.properties <- config/environments/<env>/mosip-application.properties
#       spring.properties                  <- keys of config/common/spring.properties.overrides
#
# usage: tools/build-dist.sh <env> [--skip-build]
#   env: a folder of config/environments (e.g. sandbox)
# output: dist/<env>/
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
ENV_NAME=${1:?usage: tools/build-dist.sh <env> [--skip-build]}
SKIP_BUILD=${2:-}
VERSION=1.2.0.2
ENV_DIR="$ROOT/config/environments/$ENV_NAME"
OVERRIDES="$ROOT/config/common/spring.properties.overrides"
OUT="$ROOT/dist/$ENV_NAME"
SERVICES_URL="https://repo1.maven.org/maven2/io/mosip/registration/registration-services/$VERSION/registration-services-$VERSION.jar"

[ -f "$ENV_DIR/mosip-application.properties" ] || { echo "Unknown environment: $ENV_DIR/mosip-application.properties not found" >&2; exit 1; }
command -v jar >/dev/null || { echo "'jar' not found: put a JDK 11 bin folder in PATH" >&2; exit 1; }

# 1. registration-client
if [ "$SKIP_BUILD" != "--skip-build" ]; then
  (cd "$ROOT/registration" && mvn -B -q -f registration-client/pom.xml -DskipTests package)
fi
CLIENT_JAR="$ROOT/registration/registration-client/target/registration-client-$VERSION.jar"
[ -f "$CLIENT_JAR" ] || { echo "$CLIENT_JAR not found (build failed?)" >&2; exit 1; }

# 2. registration-services: official JAR + environment configuration
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
SERVICES_SRC="$HOME/.m2/repository/io/mosip/registration/registration-services/$VERSION/registration-services-$VERSION.jar"
if [ -f "$SERVICES_SRC" ]; then
  cp "$SERVICES_SRC" "$WORK/registration-services-$VERSION.jar"
else
  curl -sSfL -o "$WORK/registration-services-$VERSION.jar" "$SERVICES_URL"
fi

mkdir -p "$WORK/content/props"
cp "$ENV_DIR/mosip-application.properties" "$WORK/content/props/mosip-application.properties"
(cd "$WORK/content" && unzip -q -o "$WORK/registration-services-$VERSION.jar" spring.properties)

# replace (or append) every key of the overrides file in spring.properties
while IFS= read -r line; do
  case "$line" in ''|\#*) continue ;; esac
  key=${line%%=*}
  if grep -q "^${key}=" "$WORK/content/spring.properties"; then
    awk -v k="$key" -v l="$line" 'BEGIN{FS="="} $1==k {print l; next} {print}' \
      "$WORK/content/spring.properties" > "$WORK/content/spring.tmp"
    mv "$WORK/content/spring.tmp" "$WORK/content/spring.properties"
  else
    printf '\n%s\n' "$line" >> "$WORK/content/spring.properties"
  fi
done < "$OVERRIDES"

(cd "$WORK/content" && jar uf "$WORK/registration-services-$VERSION.jar" props/mosip-application.properties spring.properties)

# 3. output
mkdir -p "$OUT"
cp "$CLIENT_JAR" "$OUT/registration-client-$VERSION.jar"
cp "$WORK/registration-services-$VERSION.jar" "$OUT/registration-services-$VERSION.jar"
(cd "$OUT" && sha256sum registration-*.jar > SHA256SUMS)

echo "Environment : $ENV_NAME"
echo "Output      : $OUT"
cat "$OUT/SHA256SUMS"
echo "Copy both JARs into the lib/ folder of the registration client (replace the existing ones), then run run.bat."
