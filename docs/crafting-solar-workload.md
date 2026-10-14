# Solar workload diagnosis, 2026-10-14

Input: the existing `DeferredProductionTest` empty rare Solar Amulet and projectile
+3 / explicit Spirit >=47 goal, unchanged full AST. Docker Java 21, one engine,
1 GiB test heap, 64-bit certified arithmetic, one-million resident-entry allowance.
The baseline is edbb424 plus opt-in diagnostic counters. No server was started.

Two runs stop at 33,554,432 scheduler operations (a diagnostic guard, not currency
uses or a completion claim). JFR samples CPU execution every 20 ms. Counters record
distinct source/action kernel keys in optional bitsets; they do not merge outcomes
or influence scheduling. Their final payload was about 58 KiB.

| Measurement | Baseline | Preindexed generation groups |
| --- | ---: | ---: |
| elapsed milliseconds | 130,851 | 121,570 |
| registered methods | 50 | 50 |
| completed / unfinished methods | 36 / 14 | 36 / 14 |
| root completed / total methods | 28 / 42 | 28 / 42 |
| public states | 73,343 | 73,343 |
| primitive kernel starts | 686,682 | 694,655 |
| distinct source/action keys | 246,640 | 245,418 |
| repeated kernel starts | 440,042 | 449,237 |

The runs have the same input and operation guard, but hash/iteration order and
cache timing vary across JVMs: the guard is not identical logical work. The 7.1%
elapsed reduction is a single-run observation, not a proven general speedup.
The independent complete-map oracle remains the correctness check.

In the baseline 33,504,192 steps (99.85%) were transition work; freeze waiting was
11,038 (0.033%). Ready queue identity count equalled its length. Kernel recomputation
was 64.1% of starts, not newly discovered source/action pairs. JFR's top project
frame was `AdditionRules.minimumLevelPool` or its filtering lambda in 313 of 1,683
execution samples (18.6%). The optimization indexes immutable generation-group
identities once and uses a small integer array for maxima over the **actual eligible
pool**. It does not preselect a catalog-wide highest tier, alter fallback rules,
or assume tag/omen eligibility. Afterward group lookup was 67 of 1,351 samples
(5.0%); other costs remain, including exact fractions, validation and primitive
preflight. Sample shares are diagnostic, not allocation or total CPU accounting.

## Known remaining work

After the optimized run, a separate read-only inventory applied the existing
discovery grammar to every one of the 73,343 known public states. It completed in
26,585 ms. One was already a goal; the other states admit **3,430,805 candidate
methods in total**, including the registered 50. This counts the same initial,
single-action and two-action templates used by discovery, with actual currency
preflight and `mayApplyAfter` rules. It does not expand their probability laws.
There were also 1,173,456 primitive discovery checks still unperformed by the
engine. Future methods can discover additional states: this is a known-work lower
bound, not the total size of the eventual graph.

The graph is not unfolding infinite history: state and method keys are deduplicated,
the catalog/roll partition and slots are finite, and each individual method has
100/300/500-cycle observations. However, a finite graph can still be impractically
large. Nor do the measurements prove every future phase will be free of defects.

There is a concrete capacity obstruction independent of wall time. Each completed
method retains three checkpoint-summary descriptors (at least one entry each)
and three exit-summary maps with five exit kinds plus ACTIVE (seven accounting
entries each). Thus even these known candidates require at least
`3,430,805 * 3 * (1 + 7) = 82,339,320` resident summary accounting entries, before
law pages, classes, method objects or working distributions. The current API max
is 1,000,000. Immutable-law disk backing does not externalize these summaries.
Simply extending the run cannot complete this input under the present contract.

## Approved structural direction

Recommended: preserve all candidate identities, conditional laws and residual mass,
but externalize method metadata/summaries and evaluate shared transition operators
lazily. Enumeration and probability readiness must remain separate; uncomputed
results stay partial, and final strategy requires its own sound convergence proof.
This changes storage/evaluation architecture, not the goal or permitted methods.

Alternative: persist the complete eager result graph in an offline batch store.
It still requires externalized metadata, restartable cursors and a measured storage
budget; these runs do not justify an ETA or disk-space estimate for completion.
Neither option is implemented by the small group-index optimization.

The user subsequently approved the lazy/shared-operator direction with bounded
memory caches and local external storage. The first implementation unit is the
[versioned local method store](crafting-local-method-store.md). Its storage tests
do not replace the required same-input end-to-end completion measurement.

Verification: Docker `check generateJooq bootJar` passed with 729 unit/contract and
seven integration tests, zero failures/errors. The complete-map oracle ran with
diagnostics enabled; a forced kernel eviction verifies unique versus repeat counts.
Frontend is unchanged. Raw host artifacts are under
`codex/bucket-method-verification/workload-before` and `workload-group-index`.

## Durable job integration measurement, 2026-10-14

The actual `MethodSearchService` and local job repository were measured with an
empty rare level-82 Solar Amulet, projectile skill level +3 and explicit Spirit
at least 47. Implicit Spirit is excluded. Each run closes the original service
and starts a separate Java process, then compares every published method's
100/300/500 observations and currency costs exactly. These are warm, single-run
measurements with a 1 GiB Java heap, not a completion or throughput estimate.

At commit `0bd0006`, the 8,192-operation run returned its first response in 379 ms,
paused for compute budget after 1,374 ms, and recovered in 1,789 ms. It retained
26 visible states and 50 methods; job files occupied 3,119,692 bytes.

The 1,000,000-operation configuration instead paused for resident memory budget
at 817,847 operations. Its first response took 397 ms, run took 30,787 ms and fresh
process recovery took 36,993 ms. It retained 70 visible states and 50 methods,
with 131,009,103 bytes of job files. Neither run reached a root local
recommendation or whole-search completion. Polling observed the root snapshot;
these numbers do not measure all child-node recommendations.

For the larger run, Linux process peak RSS was 588,588 KiB during execution and
401,440 KiB during recovery. Heap pool peak sums were 258,784,928 and 249,889,672
bytes respectively; pool peaks need not occur simultaneously. The file total
includes SQLite high-water/free pages and job checkpoints; the snapshot's law
page `diskBytes` field is not this total. The checkpoint contained 8,889 CLASS
rows and 89,641 runner ROW records, despite having only 70 visible states.

This exposed redundant numerical checkpoint rewriting during generation changes.
The v2 control format keeps immutable numerical rows and updates control metadata
against the same execution reference. It does not externalize the live engine
inventory or remove the large-graph strategy preparation bound. Recommended next
work remains paged method inventory, bounded mutable law storage and incremental
strategy construction; increasing caps would only postpone the same obstruction.

Raw measurements and the probe source are retained under
`codex/bucket-method-verification/durable-solar-probe-r1` and
`durable-solar-probe-production`. The initial small probe failed to locate its
standalone Java class; the classpath was fixed before these passing measurements.

With v2 control records, the same 1,000,000-operation configuration returned its
first response in 416 ms and paused for memory budget at 845,116 operations after
34,501 ms, again with 70 visible states and 50 methods. Fresh-process recovery
took 3,936 ms, with every published method
observation and currency cost equal before and after recovery. Job files occupied
67,209,815 bytes. Execution/recovery peak RSS was 548,580/380,728 KiB; heap pool
peak sums were 248,762,744/247,877,800 bytes. Root local recommendation and global
completion were still not reached. Reports and the exact probe source are under
`codex/bucket-method-verification/durable-solar-probe-control`.

The observed recovery reduction from about 37.0 seconds to 3.9 seconds is
consistent with eliminating the full numerical rewrite. Execution itself was
slower in this pair of runs. JVM iteration order and cache timing can change the
prefix, and each configuration was measured once: neither a general speedup nor
a complete-search performance claim follows from this comparison.

## Active working-law retention

A further same-input run at `7923a14` stopped at 895,363 operations with 100,060
resident accounting entries. There were 31,367 pending executions and 37,582
cycle-boundary states across 24 active runners: about 69% of the entry allowance.
The class registry held 10,089 classes and addition profiles held 17,122 rows.
Only 70 states and 50 methods were visible; 18 of the root's 42 methods were
complete. Thus externalizing only the visible method list would not remove this
particular interruption. Raw retention/workload data and the probe are under
`codex/bucket-method-verification/durable-solar-retention-before`.

The next implementation connects pending and boundary laws to ordered SQLite
scratch rows with a shared bounded value cache. It preserves the insertion order,
state/phase identity, exact interval endpoints and all remaining probability.
The same durable job cursor continues to define restart recovery. Logical disk
rows are distinguished from resident cache entries; physical heap/RSS and disk
measurements remain necessary alongside the entry allowance.

At `8d4e717`, the same 100,000-entry allowance reached the full 1,000,000-operation
server slice and paused for `COMPUTE_BUDGET`, rather than memory. It retained
31,499 pending and 59,840 boundary rows on disk, with 128 cached values and 48 law
descriptors. The working file occupied 4,419,584 bytes. There were 11,404 classes,
70 visible states and 50 methods; the root local recommendation was still pending.
First response was 406 ms, run time 58,133 ms, and fresh-process recovery 5,107 ms.
All published observations and currency costs matched across recovery.

Execution peak RSS was 408,300 KiB and heap pool peak sum 174,223,816 bytes;
recovery peak RSS was 397,604 KiB. Durable job files occupied 84,683,351 bytes,
separately from the scratch file that is deleted on service closure. This removes
the measured active-map interruption in this slice, but does not establish whole
search completion or a runtime speedup: SQL lookup/update cost increased runtime.
Artifacts and source are under `codex/bucket-method-verification/durable-solar-working-laws`.

A separate continuation probe fixed source at `8d4e717` and kept the same memory
allowance. It recorded completed server slices at 1,000,000, 2,000,000 and
3,000,000 operations (54.3, 195.2 and 540.5 seconds from creation), each paused
for compute budget. There was still no root local recommendation, and the graph
remained at 70 visible states and 50 methods. This is real progress beyond the
previous memory interruption, not repeated failure at the same memory boundary.
However, sampling showed substantial time in full checkpoint row appends; storage
cost had become another bottleneck. The longer probe was intentionally stopped
after recording these slices, with committed job files retained. It is not a
passing full-search or large-checkpoint recovery test. Artifacts are under
`codex/bucket-method-verification/durable-solar-continuation`.

Work stopped at the user's 2026-10-14 safe-shutdown request. Subsequent class-index
and checkpoint-capture changes belong to a separate WIP branch and are not covered
by the complete validation reported for `8d4e717`.
