#!/bin/bash
set -uo pipefail
out=/task-cache/bucket-method-verification/class-index-r2
mkdir -p "$out"
exec >"$out/run.log" 2>&1
cd /source/backend
bash gradlew spotlessApply --no-daemon || { echo 1 >"$out/exit.txt"; exit 1; }
mkdir -p /codex/source
cp -a /source/backend /codex/source/backend
cp -a /source/contracts /codex/source/contracts
cd /codex/source/backend
bash gradlew test --tests '*CertifiedJobRestartTest' --tests '*TemporaryWorkingLawsTest' --tests '*TemporaryClassIndexTest' --tests '*SqliteMethodGraphStoreTest' --tests '*CertifiedMethodRunnerTest' --no-daemon
result=$?
if [ "$result" -eq 0 ]; then
  cp -a build/test-results "$out/targeted"
  bash gradlew check generateJooq bootJar --no-daemon
  result=$?
fi
find src -type f -print0 | sort -z | xargs -0 sha256sum >"$out/source.sha256"
cp -a build/test-results "$out/"
cp -a build/reports "$out/"
echo "$result" >"$out/exit.txt"
exit "$result"






