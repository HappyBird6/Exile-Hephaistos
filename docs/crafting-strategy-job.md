# Bounded final-strategy job integration

After enumeration, certified jobs now sequence the 100/300/500-cycle adaptive
decision models through the existing worker, command generation and compute lease.
Each worker step either prepares one finite model or performs one refinement sweep.
Only one horizon's solver is retained; completed rows remain immutable. Cancellation
invalidates queued generations without discarding computed rows; resumption continues
the same cursor. Publication revisions track strategy progress independently of
graph changes. Local recommendations remain the direct-method calculation.

The raw wire is `certified-method-wire-v7-strategy-job`. It includes the separate
solver version and per-state/horizon lower and upper rational bounds, policy method
IDs, and reachable-open/unresolved, transition-arithmetic and fixed-point-pending
flags. Strategy interval width is not mislabeled as arithmetic error. The existing
display header projects these rows without changing the start/goal form or locales.

A horizon is certified only when every state's interval endpoints round to the
requested percent precision and its action is proved (or it is goal/proven zero).
This can finish with nonzero interval width; it does not claim an exact rational
answer. Stable unresolved or arithmetic/iteration bounds pause with
STRATEGY_UNRESOLVED or STRATEGY_PRECISION and retain all bounds. They do not become
failure probability zero. These fixed-model pauses are not offered as resumable
in the display; recalculation remains available. Compute-budget and user cancellation
pauses still resume normally.

## Explicit implementation boundary

The existing solver's graph construction and complete sweeps are synchronous.
This integration therefore gates preparation at **128 states, 1,024 methods and
8,192 outcome-row references per horizon**. Counts are checked before copying any
full outcome maps. A larger completed enumeration pauses with
STRATEGY_PREPARATION_BUDGET; it is not declared a completed support search. These
are implementation/work allowances, not currency-use caps, probability truncation,
or a restricted set of six strategies. No state, method or residual mass is removed.
The guard bounds a work step's input; it is not a hard wall-clock deadline.
The existing Storage resident-entry counter remains the enumeration engine's
counter. The strategy adapter has the separate bounded allowance above, plus at
most three result rows per state; it is not included in that engine counter.

This unit connects the verified general-cycle solver only for small maps. It does
not solve the Solar capacity problem documented in
[the workload diagnosis](crafting-solar-workload.md), nor does it claim production
support for arbitrarily large cyclic graphs. Large-map operator storage and fully
cooperative preparation remain separate architectural work.

Verification passed in Docker Java 21: ten focused tests, followed by
`check generateJooq bootJar` with 732 unit/contract and seven integration tests,
zero failures/errors. The first focused run found an old test expecting the former
NOT_REQUESTED result for an already achieved goal; it now verifies the real final
rows and retained cancellation/entry behavior. Frontend source and display schema
are unchanged in this unit; no application server was started. Host reports are
under `codex/bucket-method-verification/strategy-job-r1`.

Added cases cover the independent fully enumerated small
item graph through the job API with one-operation leases, cancellation of a queued
strategy generation, resumed final rows, a local/final diamond reversal, non-dyadic
cyclic reachability containing 1/3 at every publication, and explicit unknown mass.
Existing general-cycle and policy-enumeration oracle tests remain in the full suite.
