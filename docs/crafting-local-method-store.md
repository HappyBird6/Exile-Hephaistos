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
does not advance the revision or checkpoint. Immutable operators are interned
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
cursor. All alternatives remain in the catalog. A shorter discovered source depth
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

Docker Java 21 focused tests cover all four scenarios:

- Committed checkpoint recovery after a child JVM exits with `Runtime.halt`,
  without closing the connection.
- Reopen, exact version/input invalidation, absent 300/500 observations and the
  requested-to-partial-to-ready transition.
- Deliberate digest collisions, conflicting full source/program identities,
  rollback, stale revisions and terminal-result immutability.
- 16,384 method alternatives read in bounded 37-row depth-ordered pages with a
  64 KiB SQLite cache allowance, including depth changes and stale page rejection.

The lazy expression tests additionally verify shared definitions and observation
references without transition calls, arithmetic-precision mismatch rejection,
bounded cache reuse, and an independent first-hit/expected-consumption oracle at
100/300/500 cycles. Initial success skips the remaining cycle, and unsupported
mass remains explicit. The in-memory evaluator never publishes READY by itself.

These are correctness/storage tests, not a full Solar completion benchmark or a
measurement of peak application memory. Docker `check generateJooq bootJar`
passed: 740 unit/contract tests and seven integration tests, zero failures/errors.
Reports are retained locally under
`codex/bucket-method-verification/lazy-expressions-final`; the preceding storage
unit reports are under `local-store`. Frontend behavior is unchanged.
