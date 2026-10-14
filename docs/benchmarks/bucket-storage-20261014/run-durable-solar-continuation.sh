#!/bin/bash
set -uo pipefail
out=/task-cache/bucket-method-verification/durable-solar-continuation
mkdir -p "$out" /codex/source
exec >"$out/run.log" 2>&1
tar xf /task-cache/working-laws-8d4e717.tar -C /codex/source

cd /codex/source/backend
bash gradlew -I /task-cache/bucket-method-review/durable-solar-probe/probe.gradle test --tests '*DurableSolarContinuationProbe' --no-daemon
result=$?
cp -a build/test-results "$out/"
echo "$result" >"$out/exit.txt"
exit "$result"



