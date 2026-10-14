#!/bin/bash
set -uo pipefail
out=/task-cache/bucket-method-verification/solar-interrupted-recovery
mkdir -p "$out" /codex/source
exec >"$out/run.log" 2>&1
cp -a /source/backend /codex/source/backend
cp -a /source/contracts /codex/source/contracts
cd /codex/source/backend
bash gradlew -I /task-cache/bucket-method-review/durable-solar-probe/probe.gradle test --tests '*RecoveredSolarCheckpointProbe' --no-daemon
result=$?
cp -a build/test-results "$out/"
echo "$result" >"$out/exit.txt"
exit "$result"



