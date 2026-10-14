# Local method graph storage

`MethodGraphStore` defines a durable catalog for the lazy method evaluation work.
`SqliteMethodGraphStore` implements it with a job-owned local SQLite file and the
pinned SQLite JDBC 3.53.4.0 runtime library. It needs no database server or paid
service. This is a storage foundation; the current certified search engine still
uses its existing eager method maps. It does not yet solve the Solar completion
limit documented in [the workload measurement](crafting-solar-workload.md).

## Identity and consistency

The file records separate data, rule, roll, template, goal, arithmetic and season
versions, plus the complete original input. Reopening with a different identity
fails explicitly. Original unknown goal fields are not removed by this store.
Source IDs are checked against complete canonical content. Method IDs are checked
against source and complete program content. Operator digests only locate possible
matches: full canonical content equality is required, so a digest collision does
not merge operators.

A revision-checked transaction commits source discovery, method definitions,
observation statuses and opaque resume checkpoints together. A failed transaction
does not advance the revision or checkpoint. Unchecked `Error` failures also
roll back before restoring autocommit; a rollback failure closes the connection
instead of risking a partial commit. Immutable operators are interned
separately; an interrupted later publication can leave an unreferenced operator,
but cannot publish a reference to an uncommitted operator.

An absent observation is unrequested. `REQUESTED`, `PARTIAL`, `READY` and
`UNSUPPORTED` are distinct. Only 100/300/500 cycle observations are accepted.
Requests cannot downgrade existing work; terminal observations are immutable.
Source discovery completion is independent of observation readiness. The store
does not infer probability zero from absence, partial work or unsupported work.
Probability/mass validation belongs to the calculation codec, not this opaque
storage boundary.

## Bounded access and recovery

Method pages contain at most 256 rows and use a `(depth, ordinal)` index and keyset
cursor. A page first seeks `depth = cursor.depth AND ordinal > cursor.ordinal`;
only if necessary does it fill from later depths. The measured SQLite query plan
for a single row-value range sought only `depth`, repeatedly scanning preceding
ordinals at the same depth. A regression test checks that the actual current-depth
query seeks both keys. All alternatives remain in the catalog. A shorter discovered source depth
updates the indexed method depth transactionally. Pages pin a revision; stale
continuations reject instead of silently skipping or duplicating reordered rows.
There is no Java map mirroring the stored rows.

The SQLite page cache has an explicit KiB allowance, memory mapping is disabled,
temporary SQLite data is file-backed, and WAL commits use `synchronous=FULL`.
The page-cache allowance is **not** a bound on total JVM/native memory. Metrics
separate row counts, terminal ready counts, database/WAL/shared-memory file bytes,
logical read/write calls and configured cache allowance. Read/write counters reset
when the store is reopened; they are not physical device I/O counters.

Closing keeps the durable file. Callers must allocate a job-owned path and manage
its retention. The constructor must not receive an unrelated application database.
Process-exit recovery of committed records is tested; complete running-engine
recovery additionally requires serializing class identities, operator execution
cursors and job ownership. Those integrations are not implied by this component.

## Shared lazy expression layer

`StoredMethodExpressions` persists an expression DAG with action, sequence,
checkpoint-policy, first-hit program and requested observation nodes. A 500-cycle
request refers to the same program as a 100-cycle request; it does not duplicate
500 action lists or calculate the other horizons. Registering a method and
requesting an observation perform no primitive transition work. Programs may be
shared across distinct source rows, but the source identities are never merged.
Policies share only by their complete canonical content, not their display label.
The enclosing store identity supplies data/rule/roll/season versions; the request
also pins arithmetic precision and rejects changes to an existing observation.

Evaluation decodes the compact program and drives the existing certified Markov
runner cooperatively to the requested boundary, checking first hit after each
primitive action. Early complete absorption can fill that requested boundary
without executing remaining currency actions. The primitive kernel and classifier
must be supplied for the matching full source and versioned policy. There is no
assumption of independent rolls or identical retries in this adapter.

Decoded and interned node LRU caches each have a configurable maximum of 256
entries. Content longer than 8,192 characters bypasses both caches without being
rejected or omitted. SQL retains the complete content. This bounds cached payload
and entry counts, not a complete process RSS budget.

This layer currently supports the constant-action initial/cycle programs used by
the existing basic-method discovery grammar. It does not claim arbitrary adaptive
selectors within one stored program. Outcome-adaptive graph strategy remains a
separate solver. A completed in-memory evaluation does not automatically publish
`READY`: the caller must commit the validated summary and matching operator.

Neither this layer nor the storage port is connected to production job scheduling
yet. Evaluation cursor serialization, conditional-entry operator persistence,
externalized graph paging and large-graph strategy are still required. In
particular, this unit does not claim same-Solar speedup or whole-search completion.

### Integration gates

The remaining migration must replace the in-memory method catalog, not mirror it
into SQLite while retaining every old `Method` object. The current eager engine
remains the small-graph oracle during that migration.

1. Persist stable full class identities/witnesses and discovery cursors. Reopening
   must rebuild the same class numbering under an identical version identity;
   never reconstruct an incoming item law from a display bucket representative.
2. Separate candidate discovery and reachable result support from requested
   numeric horizons. A completed 100-cycle calculation alone is not proof that
   every 300/500-cycle result state has been discovered. Periodic chains and
   initial phases must be represented explicitly.
3. Replace whole-list revision snapshots with store-backed graph pages and bounded
   active evaluation leases. Preserve per-method alternatives and stale-generation
   protection. A shared expression is not a shared source probability law.
4. Serialize actual execution cursors and conditional-entry operators before
   claiming process restart/resume. The present opaque checkpoint port and lazy
   expression decoder do not serialize a running `CertifiedMethodRunner`.
5. Add large-graph adaptive strategy scheduling with certified lower/upper bounds;
   preserve the existing small exact oracle and local/final recommendation
   distinction. Candidate discovery, local certification and global certification
   are separate completion gates.

Repeat the same Solar input and resource allowance only after those integrations.
Record first result, first local recommendation, root direct completion and full
completion independently, along with peak memory, disk bytes, lookup latency,
stored row counts and evaluated versus merely registered observations. A resource
pause remains partial; neither this document nor a store microbenchmark certifies
whole-search completion.

## Verification evidence

Docker Java 21 storage tests cover these scenarios:

- Committed checkpoint recovery after a child JVM exits with `Runtime.halt`,
  without closing the connection.
- Reopen, exact version/input invalidation, absent 300/500 observations and the
  requested-to-partial-to-ready transition.
- Deliberate digest collisions, conflicting full source/program identities,
  rollback, stale revisions and terminal-result immutability.
- 16,384 method alternatives read in bounded 37-row depth-ordered pages with a
  64 KiB SQLite cache allowance, including depth changes and stale page rejection.
- An injected unchecked failure after a partially written batch rolls back rather
  than committing when autocommit is restored.
- The pinned SQLite runtime uses the ordinal range within the current depth;
  the planner does not restart at the beginning of that depth for every page.

The lazy expression tests additionally verify shared definitions and observation
references without transition calls, arithmetic-precision mismatch rejection,
bounded cache reuse, and an independent first-hit/expected-consumption oracle at
100/300/500 cycles. Initial success skips the remaining cycle, and unsupported
mass remains explicit. The in-memory evaluator never publishes READY by itself.

These are correctness/storage tests, not a full Solar completion benchmark or a
measurement of peak application memory. Docker `check generateJooq bootJar`
passed: 742 unit/contract tests and seven integration tests, zero failures/errors.
Reports are retained locally under
`codex/bucket-method-verification/local-store-hardening`; preceding unit reports
are under `lazy-expressions-final` and `local-store`. Frontend behavior is unchanged.

## Solar candidate storage measurement

On DESKTOP-NTUJ7E4, Docker Java 21 with a 1 GiB test JVM heap reproduced the same
empty rare Solar Amulet input (ilvl 82, Projectile +3 and explicit Spirit >=47).
The existing engine performed 33,554,432 scheduler operations in 124,465 ms and
exposed 73,343 states. It was not complete: 37 of 50 registered methods had
completed, and root direct work was 29 of 42. JVM iteration order can change the
work within a fixed operation count; this is not a speed comparison with the
earlier prefix run.

The probe then enumerated the actual basic-method discovery grammar on those
known states and committed every candidate into the local store. No outcome
distributions were evaluated in this storage phase. The retained file was opened
in a fresh test JVM and all method rows were read in 128-row pages, checking strict
`(depth, ordinal)` ordering and exact equality with the stored row count.

| Measurement | Observed value |
| --- | ---: |
| Known source states stored | 73,343 |
| Candidate methods stored/read | 3,430,805 |
| Shared definition nodes | 1,270 |
| Requested/ready numerical observations | 0 / 0 |
| Database + WAL + shared-memory files | 985,721,360 bytes (~940.1 MiB) |
| SQLite page-cache allowance | 1,024 KiB |
| Pages read | 26,804 |
| Full page scan | 21,550 ms |
| Page query p50 / p95 | 635 / 1,336 microseconds |
| Store open | 77,851 microseconds |
| Logical reads during the read probe, including counter queries | 53,620 |
| Java heap used after scan / after explicit GC | 57,591,496 / 18,632,200 bytes |
| Sum of heap-pool peak usage | 72,976,240 bytes |
| Read-probe process peak RSS (`VmHWM`) | 343,620 KiB |

The heap-pool peak sum is not necessarily a simultaneous heap peak. Process RSS
includes native memory; it is distinct from the SQLite cache allowance and JVM
heap. These memory values cover the separate metadata read probe, not probability
evaluation or the earlier eager prefix. The OS file cache was warm from the
preceding write/read attempts; these are not cold-disk or concurrent-service
latencies. One run does not establish a general throughput guarantee.

The first full scan exposed a real query-plan defect: the row-value predicate
`(depth, ordinal) > (?, ?)` produced `SEARCH ... (depth>?)`. That attempt was
stopped after diagnosis, retaining the committed file. Splitting current-depth
and later-depth ranges produces `SEARCH ... (depth=? AND ordinal>?)` for the
current-depth query. The complete 21.55-second scan above uses that correction.
The original combined probe wrote timing output only after its scan, so its exact
storage duration and original logical-write counter were not recovered. No
write-time estimate or before/after speedup ratio is claimed.

Raw artifacts are under `codex/bucket-method-verification/local-catalog-probe`
(`eager-prefix.json`, `known-candidates.sqlite`, `read-result.json`); the successful
read-probe test report is under `local-store-hardening/catalog-read-tests`.
Temporary probe sources remain under `codex/bucket-method-review/local-catalog-probe`.
The inventory was generated with commit `76c1c815b3725f596a98030fafee1f4eacbd1e93`;
the final scan includes the indexed-page and transaction-error corrections.

This proves storage and bounded-page access for the known inventory. It does not
prove whole-search completion: undiscovered methods can introduce more states,
and no stored method has a ready probability observation in this experiment.
First response, first local recommendation, root direct completion and global
completion must still be measured after production integration. The result
explicitly records `knownInventoryComplete=true` and `wholeSearchComplete=false`.
