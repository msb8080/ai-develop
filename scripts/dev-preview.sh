#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
java_home="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
maven_settings="/Users/shuaibomin/tools/maven/hexin-settings.xml"
log_dir="$repo_dir/.logs"

if [[ ! -r "$maven_settings" ]]; then
  echo "Required Maven settings is not readable: $maven_settings" >&2
  exit 2
fi

mkdir -p "$log_dir"
cd "$repo_dir"
docker compose up -d postgres

./scripts/with-xtoken.sh env JAVA_HOME="$java_home" \
  ./backend/mvnw -f backend/pom.xml -s "$maven_settings" spring-boot:run \
  >"$log_dir/backend.log" 2>&1 &
backend_pid=$!

cleanup() {
  kill "$backend_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

for _ in {1..45}; do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "Backend ready: http://localhost:8080"
    exec npm --prefix frontend run dev -- --host 127.0.0.1
  fi
  sleep 1
done

echo "Backend did not become ready; see $log_dir/backend.log" >&2
exit 1
