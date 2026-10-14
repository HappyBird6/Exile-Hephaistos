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

## Remaining integration and performance work

- The original-WIP run calculated 1,705,181 primitive kernels while retaining at
  most 2,000 full kernels. Compression has not established a CPU improvement.
  Any cache redistribution must stay within the working-memory policy and be
  compared against the same exact kernel oracle; it must not simply raise the
  global memory allowance.
- These are bare-engine measurements. API revision publication constructs state
  views, and its latency/memory must be measured separately before calling the
  UI responsive. The fixed job lifetime also needs review against long resumed
  calculations; an expired job is not a completed search.
- Lower-layer scheduling still defers deeper methods until root methods finish.
  The production certified API still reports final strategy NOT_REQUESTED. The
  finite completed-map strategy adapter is an oracle-scale integration and can
  materialize many rows; it is not yet a scalable whole-job strategy scheduler.
