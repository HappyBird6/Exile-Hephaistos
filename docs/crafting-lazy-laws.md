# Certified class-law storage

This document describes the in-progress replacement for eager saved probability
maps. It does not claim whole Solar exploration, general cyclic strategy solving
or frontend integration is complete.

## Probability representation

A saved observation retains every calculation-class ID and its original integer
dyadic endpoints. Sorted blocks of at most 256 rows are compressed losslessly.
Equal encoded blocks share one payload after full byte equality is checked;
hash equality alone is insufficient. The store is scoped to a job, its class
index, catalog/rule versions and precision. Blocks cannot be reused across an
unrelated class namespace.

The immutable observation also stores its aggregate mass. Summary requests use
that aggregate rather than reading every class row. Direct row lookup searches
block ranges, and offset pagination decodes only intersecting blocks. At most
four decoded blocks are cached. Iterators retain only their row position and
return detached entries, so an iterator suspended by cooperative scheduling
does not keep an evicted decoded block alive.

This is lazy **row-object materialization**, with shared compressed storage. It
does not yet eliminate the first computation of each class outcome. It is not a
symbolic family/tier transition solver. It also does not enumerate or store all
concrete item combinations: the original conditional method operator retains
the actual input distribution, template, classifier, horizon and output event.
Selected outcomes are reconstructed by exact conditional replay. Representative
items are never substituted for those entry distributions.

No low-probability row is removed. The compressed endpoints are not rounded
again. Unknown model mass, unfinished exploration and arithmetic interval width
remain different quantities. Existing first-hit, action eligibility, collision,
consumed-omen and actual-item replay rules are unchanged by this storage layer.

## Cooperative capture and publication

An observation lease prevents a method runner from advancing while its current
boundary is stored. This avoids duplicating its row maps for a snapshot. Key
collection, four-pass integer radix sorting and page encoding proceed in chunks
of at most 256 rows. Only one snapshot capture owns sort workspace at a time;
other method calculations continue through the existing round-robin scheduler.
The two integer sort arrays use eight bytes per source row, plus a 1,024-byte
histogram, and are released when the law is captured.

Each snapshot retains completed laws only. A partial capture cannot return a
snapshot or enter the complete-observation map. Completed page payloads may
already exist in the job's store while the containing observation is incomplete;
that is complete sub-result reuse, not a complete method result.

Publication links at most 256 output class IDs per step. The observation is made
visible after its output links are processed. Absorbed 100/300/500 observations
continue to share their law descriptors. Equivalent initial actions keep their
own source, currency use and exact conditional operator despite shared marginal
payloads. These horizons are per-method observations, not an overall currency
use limit.

## Accounting and lifetime

Primitive kernels use a separate, recomputable cache. Under resident pressure,
completed kernels are evicted before declaring the working calculation paused.
Outstanding delivery handles retain their exact result. The optional cold
cache serializes class IDs, unavailable status, unresolved mass and the full
integer numerator/denominator of every outcome. Inflation reconstructs an exact
kernel and checks its mass; it does not convert it to rounded probabilities.
The hot limit remains 2,000 kernels, and the resident allowance is unchanged.
Cold descriptors count toward that allowance; payload bytes and their limit are
reported separately. Cold eviction permits recomputation and never deletes an
outcome from a method distribution. Partial kernels are not cached as complete.
This changes representation and reclamation, not supported actions or goals.
The change passed nine focused tests and Docker Java 21
`check generateJooq bootJar`: 717 unit/contract tests and seven integration tests,
zero failures/errors (`kernel-cache-targeted`, `kernel-cache-full`). The tests
compare restored kernels with independent exact calculations and exercise
delivery after eviction, zero cache allowance and an insufficient byte allowance.
The same-input root measurement with a 16 MiB cold cache stopped after 32,694 ms
at the unchanged one-million resident-entry guard: 18/42 root methods were done,
with 886,494 active pending/boundary/exit rows. All recomputable cache entries had
already been reclaimed. It calculated 87,956 kernels and restored 104,955 cold
kernels, but different relative method progress accumulated too many concurrent
working laws. This is a performance/resource regression, not a completed root.
Evidence: `kernel-cache-root`. The cold cache is therefore disabled in the default
engine; its exact codec remains tested but is not a claimed production speedup.
Working-cache reclamation remains enabled. Scheduling/working-law storage must
address the concurrent working set before enabling the cold cache again.

The follow-up with the cold cache disabled also paused at the same resident guard
after 52,303 ms, with 18/42 methods complete and 876,393 pending/boundary/exit rows.
It calculated 246,891 kernels, used no cold payload, and reclaimed every completed
kernel. Evidence: `reclaimed-hot-root`. Thus the earlier single 702-second root
completion is not a reproducible resource guarantee. Relative iteration and
method progress can accumulate a larger concurrent working set even without
cold caching. Do not attribute the entire regression to compression or claim that
disabling it fixes the working-set limitation. No distribution was dropped and
both resource stops remained explicitly incomplete.

The resident-entry limit counts active calculation rows, class/kernel records,
page descriptors and currently decoded rows. Packed payload bytes have a separate
provisional 32 MiB guard. Storage metrics expose these quantities separately,
including temporary freeze workspace. The old saved-row count and the new
resident-entry count are not interchangeable memory measurements; comparisons
must also report actual JVM heap and packed bytes.

`storedClassRows` counts rows in unique physical pages, including completed pages
belonging to an unfinished capture. It does not count every logical occurrence
across all methods and horizons. A storage or working-memory pause is partial
work. It is never enumeration completion, probability zero, or a currency cap.
The packed-byte policy currently has no per-request increase control; a job
stopped by that policy cannot progress merely by repeating the same resume call.
If real workloads reach it, choose a bounded backing store or an explicit policy
change rather than silently discarding outcomes or expanding the limit.

Cancellation preserves the store for resume. Expired-job eviction and service
shutdown invalidate its readers and release packed payloads. Historical graph
revisions within a live job retain immutable descriptors. Closed-law lookup,
paging, iteration and aggregate reads reject access. No file spill or database
storage is introduced by this implementation.

## Observable revisions and command replay

The v3 certified wire contract retains the most recent 64 mutation responses for
idempotent replay, rather than limiting a job to 64 mutations. A replay still in
that window returns its original response. An evicted original request carries
an old expected revision and receives `REVISION_CONFLICT`; it cannot perform the
action again. Clients must use a new command ID for a new action and refresh the
snapshot after a conflict. Generation, cancellation and request ownership checks
are unchanged. The fixed expiration period is a separate outstanding limitation.

The engine tracks changes to observable states, depths, methods, completed
observations and direct completion. Scheduler work that changes none of those
does not force another full graph snapshot. Status/generation changes still
publish, reusing immutable graph lists when possible. Operation/storage counters
describe the published snapshot, not a separate live telemetry stream. Four
graph revisions remain available; this does not remove pagination or stale
revision checks. These changes passed six focused tests and Docker Java 21
`check generateJooq bootJar`: 718 unit/contract tests and seven integration tests,
zero failures/errors (`publication-targeted`, `publication-full`). Coverage includes
80 resumptions, retained duplicate replay, eviction followed by stale-revision
rejection, and unchanged graph-version samples checked against complete views.

## Checkpoint-specific local comparisons

The v4 wire model adds checkpoint summaries keyed by the complete goal/entry
status pattern. They aggregate only mutually exclusive resolved goal, checkpoint
and loss exits within one method observation. They never add alternative methods
or incoming execution laws. The full class distribution and conditional entry
operator remain available independently of this display summary.

Summaries are accumulated cooperatively while publication already walks the law,
cached only after the entire law has been read, and included in resident accounting.
The protocol compares methods separately for each observed target pattern; unknown
or unsupported targets are not treated as numeric failure. Unresolved mass still
widens the possible ranking. The existing whole-goal local comparison remains
explicitly identifiable by a null target. Neither is the final adaptive strategy.

Authenticated activity now renews the 20-minute idle lease, and running slices
renew it while doing work. Abandoned paused jobs still expire. A request rechecks
expiration after obtaining the job lock before renewal. Unchanged root-local
summaries are reused with unchanged graph lists. These checkpoint/lifetime changes
passed seven focused tests and Docker Java 21 `check generateJooq bootJar`: 719
unit/contract tests and seven integration tests, zero failures/errors
(`checkpoint-targeted`, `checkpoint-full`). Every checkpoint aggregate is checked
against the independent exact engine's resolved exit laws. The tests also retain
an active job for 76 simulated minutes and reject it after 21 idle minutes.
The service-publication timing probe remains pending at this checkpoint.

## Verification record

On 2026-10-14 the original WIP commit
`8712cfd1c9131df8eaa4ad37e6f70a3e1a3bea8a` passed Docker Java 21
`check generateJooq bootJar`: 713 unit/contract tests and seven integration tests,
zero failures/errors. That result predates the cooperative capture and direct
paging changes described above. Their test and measurement results must be
recorded separately before promotion from the WIP branch.

The unchanged original WIP was also measured with the same Solar fixture,
64-bit arithmetic, one-GiB test JVM, two-million diagnostic resident allowance
and 600-second guard. The run ended at 600,017 ms with 194,257,779 operations,
108,999 classes, 73,342 displayed state classes and root 36/42 methods complete.
First root direct completion and whole completion were not reached. It was a
time-guard stop, not a memory/storage pause.

Its 1,269,094 saved class rows occupied 14,244,773 compressed bytes in 5,047 pages;
there were zero additional equal-page reuse hits in this fixture. Thus the
measured benefit here comes from lossless compression, not additional sharing.
Post-GC heap was 294,008,320 bytes versus 519,606,224 in the pre-page baseline
(about 43% lower). Sampled peak heap was still 929,672,472 bytes versus
941,096,960: transient/active calculation pressure remains substantial. The
different resident-entry accounting reported 580,925 entries; that number alone
must not be called a byte-memory reduction. This run is not an API latency test
or proof of completion at the API's one-million setting.

Evidence directories under outer `codex/bucket-method-verification` are
`lazy-resume-full` and `lazy-resume-solar`. The temporary container exited and
was removed. Existing Redis services were preserved.

The cooperative capture, direct paging, detached iterators, aggregate lookup and
expiry-lock changes subsequently passed 18 focused tests and Docker Java 21
`check generateJooq bootJar`: 717 unit/contract tests and seven integration tests,
zero failures/errors. Formatting passed. Evidence is under
`cooperative-laws-targeted` and `cooperative-laws-full`. The updated Solar run is
pending at this checkpoint; this functional result alone does not establish a
performance improvement or whole-search completion. Frontend source is unchanged.

The updated same-condition run then finished at 600,021 ms with 197,881,908
operations, 108,999 classes, 73,342 displayed state classes and root 36/42 methods
complete. First root direct completion and whole completion were still absent.
It retained the same 1,269,094 saved rows in 14,253,341 bytes/5,047 pages, with no
additional equal-page reuse. Post-GC heap was 294,496,272 bytes; sampled peak was
929,846,736 bytes. There was no memory/storage pause. Evidence is in
`cooperative-laws-solar`.

The storage benefit is retained; this run does not establish a CPU speedup.
Different class-interning/iteration order and cache timing changed individual
unfinished-method progress and kernel cache statistics between runs. Both runs
stopped with six root methods unfinished. The next diagnostic uses the actual
one-million resident setting and stops at root direct completion, a resource
pause, or an explicitly reported diagnostic time guard. Root completion alone
must still not be presented as whole enumeration completion.

## Remaining integration and performance work

The one-million-entry root diagnostic on the verified cooperative-page source
finished all 42 root methods after 702,060 ms (702,064 ms including the report).
It performed 224,858,112 scheduler operations and calculated 2,241,653 primitive
kernels. Peak resident entries were 947,367; no memory or packed-storage pause
occurred. It retained 1,709,002 saved class rows in 19,188,122 packed bytes.
Sampled JVM peak was 927,001,296 bytes. Core post-GC heap was 190,597,496 bytes;
this probe did not retain the complete public state-view list, so that value is
not directly comparable to the previous probes' post-GC measurements.
Evidence is `root-api-budget`. The measurement exercises the engine at the API
allowance, not HTTP publication latency. Whole enumeration was still incomplete.

The root's existing local ranking reports ties because it compares only immediate
whole-goal mass. Methods stop at the first intermediate checkpoint, so this does
not rank the A/B directions usefully. Follow-up: expose target/checkpoint-specific
local ranking, keeping intermediate progress distinct from final adaptive success.
Do not silently label all zero-direct-success alternatives as the final strategy.

- The original-WIP run calculated 1,705,181 primitive kernels while retaining at
  most 2,000 full kernels. Compression has not established a CPU improvement.
  Any cache redistribution must stay within the working-memory policy and be
  compared against the same exact kernel oracle; it must not simply raise the
  global memory allowance.
- These are bare-engine measurements. API revision publication constructs state
  views, and its latency/memory must be measured separately before calling the
  UI responsive. The fixed job lifetime also needs review against long resumed
  calculations; an expired job is not a completed search.
  The former 64-command lifetime capacity could not support the measured
  224-million-operation root through 12,000-operation leases. Bounded replay now
  removes that lifetime ceiling, and unchanged graph lists are reused. Actual
  service publication and repeated-resumption costs still require measurement.
- Lower-layer scheduling still defers deeper methods until root methods finish.
  The production certified API still reports final strategy NOT_REQUESTED. The
  finite completed-map strategy adapter is an oracle-scale integration and can
  materialize many rows; it is not yet a scalable whole-job strategy scheduler.
