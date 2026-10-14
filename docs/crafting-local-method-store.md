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

## Verification

Docker Java 21 focused tests cover all four scenarios:

- Committed checkpoint recovery after a child JVM exits with `Runtime.halt`,
  without closing the connection.
- Reopen, exact version/input invalidation, absent 300/500 observations and the
  requested-to-partial-to-ready transition.
- Deliberate digest collisions, conflicting full source/program identities,
  rollback, stale revisions and terminal-result immutability.
- 16,384 method alternatives read in bounded 37-row depth-ordered pages with a
  64 KiB SQLite cache allowance, including depth changes and stale page rejection.

These are correctness/storage tests, not a full Solar completion benchmark or a
measurement of peak application memory. Docker `check generateJooq bootJar`
passed: 736 unit/contract tests and seven integration tests, zero failures/errors.
Reports are retained locally under `codex/bucket-method-verification/local-store`.
Frontend behavior is unchanged by this foundation.
