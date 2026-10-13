# Paused WIP: lazy certified class laws

The user requested a safe stop and remote backup before 2026-10-13 17:45 KST.
Do not resume automatically after the stop report. This branch is a backup of an
unfinished implementation, not a completed feature or a release candidate.

## Verified baseline

- Host: DESKTOP-NTUJ7E4.
- Worktree: `C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\codex\bucket-method-search`.
- Normal work branch: `crafting/bucket-method-search-v2`.
- Local and remote verified commit: `cfe78d8525cd5cab65ce85bdadd3462bd1822143`.
- Docker Java 21 `check generateJooq bootJar`: 711 unit/contract tests and seven
  integration tests passed. Formatting passed. Frontend unchanged in that unit.
- Baseline Solar diagnostic: 600,018 ms, root 36/42 methods completed, 108,999
  calculation classes, 73,342 displayed state classes, no first root direct
  completion/local recommendation and no whole-search completion. The six
  remaining Exalted/Chaos method variants were at cycle four/five. Deeper methods
  were deferred by lower-layer scheduling.
- Baseline retention: 1,854,869 entries, including 1,269,094 unique saved-law rows
  and 421,004 active rows. Sampled heap peak 941,096,960 bytes; post-GC heap
  519,606,224 bytes. This used the two-million diagnostic allowance, not the API's
  maximum one-million resident-entry setting.

## Current backup branch

`wip/crafting-lazy-laws-20261013`, based on the verified commit above. The final
backup SHA is recorded in the stop report and in the local handoff record under
the outer `codex/bucket-method-review` directory. A commit cannot contain its own
SHA; use `git rev-parse HEAD` and `git ls-remote` to verify the backup independently.

## Implemented prototype, not acceptance-complete

`CertifiedLawStore` holds immutable class-law page descriptors and complete,
losslessly compressed page payloads. Each page contains up to 256 sorted class
IDs and the original integer lower/upper endpoints. No tail is dropped, no
probability is rounded again, and zero or exactly-one endpoints remain valid.
Content sharing checks the entire encoded byte array after hashing; a matching
hash alone is insufficient. Up to four pages (1,024 rows) are materialized in the
read cache. Point lookup uses page ranges. Each law also retains its aggregate
probability for summaries without scanning all rows.

The concrete item conditional replay operator is still retained in the snapshot.
The store represents class marginals only; a representative item never replaces
the entry distribution. Completed observations and equivalent initial methods
can share page payloads. The full item distribution remains reconstructible by
the existing exact conditional operator.

The engine prototype freezes observations into that store. Wire version changes
to `certified-method-wire-v2-lazy-laws` and exposes storage metrics. Active rows,
materialized rows and descriptors count against the existing resident-entry
guard; compressed bytes have a separate provisional 32 MiB guard. The resident
entry maximum has not been increased. This changes the accounting contract and
must be reviewed and measured before promotion; compressed rows are not zero
cost. `STORAGE_BUDGET` is a partial stop, never completion or a currency cap.
`storedClassRows` counts rows in unique physical pages, not logical occurrences
across all observations. Numeric interval width remains independent of storage
and unfinished enumeration.

Job eviction and service shutdown invalidate the store and release page payloads.
Cancellation preserves it for resume. Old views reject reads after store closure.
No disk spill, server, database change or persistent data deletion was added.

## Known unfinished work and review points

1. This delays row-object materialization and shares storage; it does **not** yet
   avoid calculating every class outcome once. Do not describe it as a complete
   symbolic/factored transition-law implementation.
2. A one-page `Freeze` cursor exists, but the engine still calls the synchronous
   `freeze` wrapper. Wire that cursor into cooperative method scheduling before
   claiming bounded publication latency. The initial sorted-key array and the
   copied runner snapshot also need transient-memory accounting. `prepare`
   currently assumes its input is immutable while the cursor runs.
3. Outcome API pagination still iterates past preceding and trailing outcomes.
   Add direct page-offset iteration and stop after the requested page. Summary
   mass queries already avoid full scans through the stored aggregate.
4. The provisional storage guard is checked after a save and can overshoot by an
   observation. Budget exhaustion/resume, expiry races, old revisions, close
   during queued work and arbitrary paging require dedicated tests. Closed
   summary access and retained iterators need lifecycle review as well.
5. API hard-cap semantics changed: resident objects/descriptors and packed bytes
   are separate. Review this explicit contract and its actual peak heap; never
   compare the new resident-entry count alone with old saved-row accounting.
6. No Solar measurement has been run on this prototype. No root completion,
   latency, memory reduction or whole-search improvement is established yet.
7. Root primitive-kernel recalculation and the six long Exalted/Chaos methods
   may remain the CPU bottleneck after reducing completed-law retention.
8. General cyclic adaptive strategy scheduling and frontend integration remain
   unfinished. The certified API still reports final strategy NOT_REQUESTED.

## Verification and next commands

The initial prototype passed five targeted tests (law-page endpoint restoration,
sharing/lifecycle, complete small graph against exact oracle, and conditional
preparation equivalence). This was before the final cursor/close cleanup.
The final short stop-check result is appended below. Full backend checks,
frontend checks, browser validation and the Solar comparison were deliberately
not started after the stop instruction.

On a future explicitly authorized resumption:

1. Inspect `git status`, the branch, remote SHA and this checkpoint first. Preserve
   the normal verified branch and work on the WIP branch until promotion checks
   pass. Check the real current host/path; do not reuse a different computer.
2. Finish cooperative freezing, direct paging and lifecycle/budget tests above.
3. In the existing Java 21 Docker setup, run `spotlessApply`, then freeze the
   source and run `test --tests '*CertifiedLawStoreTest' --tests
   '*CertifiedMethodSearchEngineTest' --tests '*CertifiedPreparationSharingTest'
   --tests '*CertifiedSearchJobsTest'` plus the new tests.
4. Re-run the same 600-second Solar probe from outer
   `codex/bucket-method-review/certified-graph-probe` (one Gradle container at a
   time). Measure actual heap, stored bytes, materialized rows, transient rows,
   first direct completion, first local recommendation and whole completion
   separately. If incomplete, report partial work rather than inventing a final
   recommendation. Also check the API one-million resident-entry setting.
5. Run `check generateJooq bootJar` before promotion, then commit/push and verify
   the remote SHA. Keep tests/reports/cache under outer `codex`; do not commit
   caches, credentials or local settings. Do not start a server or modify master.

## Final short stop check

Docker Java 21 frozen-source `spotlessCheck` and the four targeted test classes
above passed: eight tests, zero failures and zero errors. Compilation passed.
Results are in outer `codex/bucket-method-verification/lazy-stop-check`.
The `bucket-lazy-stop-check` container used `--rm` and exited successfully.
This is limited verification of the saved WIP, not the full acceptance suite.
