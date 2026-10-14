# Storage work: safe shutdown, 2026-10-14

The user requested that today's work stop and be pushed safely. Implementation,
measurements and tests were stopped. Do not resume from this note without a new
user instruction. No master merge, deployment, force push or machine shutdown
was performed.

## Remote preservation

- Validated working branch: `crafting/bucket-method-search-v2`,
  `ceea41875dfe8d16473b3ea8d5f719ad17abd05e`.
- Last fully validated source change: `8d4e717f7b3547b54fbf6db8c1a3e8a13b0a0535`.
  `ceea418` adds measured results and the shutdown boundary only.
- Unfinished source and this note: `wip/bucket-method-storage-20261014`.
  Its exact final SHA is included in the shutdown report.
- The active checkout is left on the WIP branch. The original nested checkout was
  not reset or changed. Large measurement databases, caches and local settings
  are excluded from Git.

## What improved, and what did not

The baseline retention probe found 31,367 pending executions and 37,582
cycle-boundary rows in 24 runners: about 69% of the 100,000-entry allowance.
Only 70 public states and 50 methods were visible. The old in-memory maps were
therefore a concrete bottleneck, rather than a hypothesis based on UI node count.

`8d4e717` moves these two ordered maps into job-owned SQLite scratch storage with
a 128-value Java cache and 1 MiB SQLite page cache. It preserves all rows, insertion
order, interval endpoints and state/phase identity. The durable job checkpoint
remains separate. Docker `check generateJooq bootJar` passed 753 unit/contract and
seven integration tests; the frozen source manifest matched with zero differences.

With the same Solar input and 100,000-entry allowance, the changed code reached
1,000,000 operations and paused for compute budget, rather than memory. It held
91,339 working rows in a 4,419,584-byte scratch file. Its published observations
and currency costs matched after a fresh JVM recovery. First response was 406 ms,
run time 58,133 ms and recovery 5,107 ms. Execution peak RSS was 408,300 KiB.

A longer baseline continuation recorded 2,000,000 and 3,000,000 operations with
the same memory allowance, still pausing for compute budget. This is progress
beyond the former memory interruption. It is not completion: root local
recommendation remained absent, and visible inventory remained 70 states and
50 methods. Runtime increased, and thread samples showed substantial time in
full checkpoint row appends. The continuation was intentionally stopped after
those recorded slices; it is not a passing end-to-end or large-recovery test.

## WIP source changes

1. `BasicClassIndex.Storage` and `TemporaryClassIndex` externalize full class
   witnesses with a 256-entry decoded cache. Hashes only select candidates; full
   reconstructed keys must compare equal. ID order and full witness contents are
   retained. A forced-collision/eviction test compares against the memory index.
2. The engine bounds its goal-progress cache to 512 entries and removes an
   unnecessary unbounded `class:<id>` string cache. Resident accounting includes
   public state/method inventory and these caches.
3. `MethodCursorStore.captureExecution` and its SQLite override stream a complete
   synchronous checkpoint in one FULL-synchronous transaction, using one prepared
   insert statement. Publication still uses the existing revision-checked atomic
   reference/control update. The old incremental append API is unchanged.
4. A new capture test checks one commit, rollback after an injected Error,
   preservation of the previously published cursor and complete reopening/page
   reads. Existing actual-job restart tests run through the new capture path.

These changes are not fully validated. `class-index-r1` failed compilation due
to a missing import. After fixing it, `class-index-r2` passed compilation and all
17 focused tests, including collision/eviction, cursor capture and job restart.
The subsequent full `check generateJooq bootJar` was interrupted at the user's
shutdown request. Its source manifest matched the WIP source with zero differences.
No completed full-check result or Solar performance result exists for this WIP.

## Resume sequence, only after user authorization

1. Confirm machine, checkout, branch, Git status and remote SHAs. Preserve existing
   changes. Read the checkout instructions and this note; the prior user-approved
   exception to the two-repair limit remains task-specific evidence, not a reason
   to restart work without authorization.
2. Complete Docker verification of the WIP: focused tests, then
   `check generateJooq bootJar`. Retain failures and verify the tested source hash.
3. Run the prepared large-checkpoint recovery probe only against a safe copy.
   It was prepared but never executed. Durable jobs have a 20-minute idle lease;
   the historical checkpoint may have expired by the next session. Reproduce a
   fresh baseline or use an explicitly documented test clock fixture; do not
   silently weaken production expiry behavior or claim an expired job resumed.
4. Measure the same Solar input and unchanged entry allowance with class storage
   and atomic capture. Separate first response, first root local recommendation,
   total completion, heap/RSS, scratch disk and durable checkpoint disk. Record
   partial search honestly. Compare all published observations/costs after restart.
5. Continue the still-unimplemented paged method/state inventory, summaries and
   large-graph final strategy integration. The engine still retains public maps
   and published revision lists. Strategy preparation still has the documented
   128-state/1,024-method/8,192-transition-row bounds. Do not merely raise caps,
   discard alternatives or call a partial graph complete.

## Reproduction materials and cleanup

Small probe sources and existing runner scripts are preserved under
[`benchmarks/bucket-storage-20261014`](benchmarks/bucket-storage-20261014).
They are historical diagnostic tools, not tests that run automatically. Scripts
expect the workspace mounted as `/source` and the task's `codex` directory as
`/task-cache`. Restore the probe files to
`/task-cache/bucket-method-review/durable-solar-probe` before using their Gradle
init script. The continuation script expects a source archive made with
`git archive --format=tar --output=<codex>/working-laws-8d4e717.tar 8d4e717`.
Use fresh output directories; do not overwrite earlier databases or reports.
The continuation probe's 16-slice guard is a diagnostic work guard, not a global
currency limit. It was interrupted before reaching that guard.

Local evidence remains under `codex/bucket-method-verification/`:

- `working-laws-r1`: completed full validation of `8d4e717`.
- `durable-solar-retention-before`: baseline retention measurement.
- `durable-solar-working-laws`: completed 1,000,000-operation/recovery probe.
- `durable-solar-continuation`: recorded 3,000,000-operation continuation;
  `INTERRUPTED.txt` identifies its deliberate stop. Large checkpoint files retained.
- `class-index-r1`: failed compile, corrected import.
- `class-index-r2`: 17 passing focused tests, frozen source manifest and
  `INTERRUPTED.txt`; full validation is incomplete.

The owned continuation and validation containers were stopped. Earlier owned
containers used `--rm` and exited. Existing Redis services and all unrelated
services, DB data, volumes and user files were preserved. No application server
was launched. There was no connection or approval blocker at shutdown; remaining
work is implementation/verification work stopped at the user's request.
