# Bucket method calculation v2

## Contract

This calculation contract is separate from the deployed v1 path-search API. V1's
`attempts` still counts primitive currency uses. V2 observations count completed
**method cycles after initial preparation**; 100 cycles of Exalted/Annulment are
not 100 currency uses. Standard observations are 100, 300 and 500 cycles. These
are observations of each arrow, not a currency limit on the overall strategy.

`ExactMethodRunner` has an initial action sequence and a nonempty repeated cycle.
Each phase selects its action from the full result state. The classifier runs on
the starting state and immediately after every primitive action. First success
absorbs before any later removal or other currency is consumed. Initial and cycle
consumption are separate maps of expected actual uses per currency.

Every boundary observation partitions unit mass into GOAL, CHECKPOINT, LOSS,
DEAD, UNRESOLVED and active full-state distributions. A missing or unverified
kernel contributes unresolved mass. An unavailable action contributes dead mass
without charging currency. Display omission is not unresolved calculation.
Preparation can itself absorb, and its first observation has cycle count zero.

`BasicCurrencyTransitions.primitiveCursor` streams one elementary modifier/roll
outcome at a time, with a retained removal-branch/candidate/roll position. It uses
the existing eligibility rules and preflights all candidates before publication.
Its scope is the declared reviewed single-stat basic model without omens. In particular,
it does not turn an unsupported joint roll or omen into a zero-probability result.

The caller retains a runner while paused and calls `step()` between time-budget
checks. A step processes one state/phase kernel; kernel generation itself must
be separately bounded/streamed before connecting this to production catalog
enumeration. No partial calculation is a complete cached result.

## State and strategy

The generic state key must be immutable and retain full ItemState, remaining
omens and any transition-relevant context. It is not the screen's A/B/C bucket.
Equality checks actual contents through map key equality; hash equality alone
does not merge states. Phase lives in execution identity. Alternative templates
have separate runners and must never have their probabilities added together.

`OutcomeStrategy` computes reachability bounds over a finite discovered graph.
At each exact outcome state it maximizes over the next available methods. Thus
the A and B branches of a diamond may choose different methods. Parallel methods
between the same state pair retain separate method IDs. Bellman updates sum only
mutually exclusive outcomes within one method, then maximize across alternatives.

Unknown outcomes and states with undiscovered actions keep an upper bound of one.
Closed non-goal components that cannot reach an unknown boundary have value zero.
External cycles use iterative bounds; a work limit or a stable-looking decimal
does not prove exact eventual success. A recommendation can be certified when
its lower bound dominates every alternative upper bound and the source action
enumeration is complete. Certification of an action is distinct from exactness
of its probability or completion of the whole exploration.

## Integration boundary

The reviewed direct numeric goal definitions now include
`projectile_skill_gem_level_+` in source level units (`flat`). The existing
`hephaistos:v1:explicit:base_spirit_from_equipment` goal excludes the implicit
Spirit roll. Tests cover explicit 46 versus 47, projectile 2 versus 3, the empty
Rare/A/B/C goal states, and loss/reacquisition with the same compiled full goal.
This establishes goal evaluation, not completion of the diamond method search.
The goal-definition rule version and resource seal change with this mapping;
the production example is regenerated against that catalog version.

`MethodSearchCalculation` now connects the primitive cursor and compiled complete
goal evaluation. It discovers applicable single-action methods, two-action cycles,
and preparation-plus-one-action cycles from the shared action list. These are a
versioned finite template grammar, not all conceivable crafting programs. A
single-action method checkpoints at a rarity/explicit-slot-count change. A paired
method keeps that change internal until a goal/checkpoint/loss or cycle boundary.
This lets an Exalted stage finish without enumerating every possible six-affix
item; Exalted/Annulment still tests first hit before removal. New exact states are
discovered lazily. Identical full return states permit the proven renewal shortcut;
other methods continue the Markov calculation.

Calculation v4 also recognizes a full-state invariant retry distribution. For
adjacent boundaries of the same immutable cycle kernel, it requires exact
`T*u=q*u` equality, then scales every exit/state and currency-use increment by
the geometric sum. Preparation costs and already absorbed mass remain unchanged.
Equal goal-display buckets are not a proof. Independent closed-form checks and
500-cycle Markov enumeration agree at 100/300/500, including initial success,
loss and expected currency use. Pending methods initialize their runner only when
scheduled; completed methods release the mutable runner after saving observations.

The new `/api/v2/crafting/method-searches` endpoints expose create/read,
revision-pinned graph pages, exact conditional outcome pages, cancel and resume.
They require version 2 and the current ruleset header. `cycles` means method
repetitions; expected initial and cycle currency uses are separate. The old v1
API retains its original primitive-use semantics. Job state lives in process
memory for 20 minutes, with four retained revisions. Restart/expiry requires an
explicit recalculation. Request/command IDs are idempotent, owner cookies scope
access, and generation tokens suppress cancelled workers. A run budget pauses
work; it is never a crafting-currency limit.

`GET /api/v2/crafting/method-searches/provenance?baseItemId=...` resolves the
selected registered catalog. Craft Support keys this request by selected base
and ruleset and waits for it before starting. Each job publishes its validated
input model provenance, not a default Solar model. Non-Solar basic searches use
`reviewed-basic-transition-v2`; existing Solar and v1 saved identities are not
silently relabeled. Explicit-uniform selection is a declared model, not a claim
that game weights were verified.

The Craft Support results now consume v2. Input editing and all six languages
remain intact. Presentation buckets aggregate goal-entry statuses only; each
method still names its exact source item, and its paged conditional outcomes retain
full item/roll identities. Alternatives are separate arrows. Local recommendation
requires all source methods and dominance over unresolved upper bounds. Final
strategy recommendations are emitted per exact state after enumeration; cycles
retain bounds unless proven exact. Neither omitted pages nor unresolved outcomes
become failures. The shared synthetic diamond fixture tests separate A/B decisions
and immediate/final recommendation reversal; it is not game-probability evidence.

## Open issues and recommended next work

- **Production search scale:** no transition-equivalence reduction of full roll
  states is implemented. Large catalogs can pause before all direct methods or
  deeper layers finish. Keep `PAUSED`/partial; add a proved weighted-bisimulation
  representation with entry-conditioned distributions before claiming full
  production-diamond exploration or a completion latency.
  The ilvl-82 Solar inventory alone contains 209 positive-weight explicit tier
  definitions across 12 prefix and 18 suffix families (before state-dependent
  eligibility). Excluding the Spirit and gem-level families still leaves
  `C(11,3)*C(17,3)=112,200` possible family-set combinations before tier, roll or
  ordering variants; this combinatorial count is not a reachable-pool audit.
  The recommended next representation is a family/tier transition index plus
  goal-dependent bisimulation classes, with an entry-specific conditional-law DAG
  that can reconstruct full item outcomes. A representative roll or pooled law
  across alternative incoming methods is not acceptable. Shared primitive caches
  and the new phase folding do not substitute for this representation.
- **Rules coverage:** v2 now selects the registered starting base's catalog and
  reviewed basic-currency model. Complete eligible pools must have single-stat
  integer rolls; joint rolls and active omens remain explicitly unresolved.
  Catalog registration is not a promise of complete probability support. The v1
  primitive model/provenance remains Solar-only. Whittling must remain a Chaos
  omen, never a deletion orb.
- **Uniform fallback:** v2 accepts both existing explicit-uniform catalog policy
  names and selects individual eligible modifier-tier candidates with 1/n,
  recording candidate IDs/count, reason and source. Non-goal candidates remain in
  the denominator and per-tier rolls are separate. Zero-weight special definitions
  retain the shared rules' non-generation eligibility. Future nullable/missing
  weight ingestion is still separate work; none was found in the reviewed audit.
- **Cyclic final strategies:** fully known graphs up to 32 states now have an
  exact policy-evaluation cursor. It removes policy classes with no path to a goal,
  solves the remaining rational linear equations one row/pivot per call, improves
  actions strictly, and certifies only an attained policy value satisfying all
  Bellman inequalities. Strategy v4 also proves almost-sure reachability on larger
  fully enumerated, resolved finite graphs, with a support-edge cursor and a proper
  stationary policy witness. It repeatedly removes states that cannot reach a goal
  using actions staying inside the remaining set. Its witness has a positive edge
  toward a smaller reverse-BFS rank at every non-goal state, excluding closed
  non-goal recurrent classes. This does not solve every probability strictly
  between zero and one on large graphs. Such values and unresolved graphs retain
  bounds; making the remaining Bellman/graph initialization work resumable stays open.
- **Memory budgets:** retained primitive outcomes are bounded; runner distributions,
  method objects and revision indexes also need a total-memory budget. Resume can
  request a larger retained-outcome limit through the API. Eviction must preserve
  pending cursors and must not promote partial kernels into complete cache entries.
- **Verification remaining:** production first-local/whole-search timing and peak
  memory, real-browser interaction, and broader lifecycle fault injection remain
  distinct acceptance checks. Passing synthetic arithmetic tests does not establish
  those results.

## Verification evidence (2026-10-12)

Docker Java 21 full `check generateJooq bootJar` passed (646 unit/contract tests and seven disposable
DB integration tests). Docker Node 24 lint, typecheck, formatting, 2,147 frontend
tests and production build passed. The preserved v1 source oracle remains 650/21107.
The actual Solar pool test enumerates Exalted outcomes for root, Projectile +3,
explicit Spirit 47 and existing Life 50; both diamond arms/merges and Life+Chaos
are reachable, and the complete primitive distributions conserve mass.
The ordinary-catalog audit exercises all 30 declared uniform catalogs against
the 16 basic actions without base/weight-policy rejection. This is rule-entry
coverage, not proof of every roll kernel. The real Sapphire addition test checks
all 58 eligible individual candidates at 1/58 after summing their distinct rolls.
Service/HTTP tests cover base-specific provenance and mixed-base job responses;
frontend tests cover the selected-base route and ruleset/base-change gating.

An isolated external JUnit probe used the production catalog with an empty Rare
Solar item and AND(Projectile >=3, explicit Spirit >=47). One 12,000-operation run
measured first response **329ms**, first method visible on the first graph page
**1,011ms**, and sampled process-heap maximum **72,939,176 bytes**. It paused with
`COMPUTE_BUDGET`, 9,952 discovered method instances and three display buckets.
First local recommendation and whole-search completion were **not reached**.
The heap figure is sampled process usage, not a proved peak or isolated job size.
These measurements do not certify production completion performance.

The v3 calculation/runner update slices primitive outcome classification into
bounded batches, parks methods waiting for an unfinished kernel, and rotates
equal-priority work with global queue tickets. One in four kernel/method turns
uses FIFO, while the other turns prioritize achieved checkpoints. State discovery
still orders the frontier by method depth. Shared rarity/slot rules prune only
structurally impossible second actions; exhaustive concrete-branch tests guard
that pruning. Completed earlier observations are immutable even when a method
absorbs at a later cycle, and currency cost is charged once across slice retries.

The same external production probe, with two explicit resumes (36,000 operations),
measured first response **259ms**, first visible method **400ms**, first complete-goal
display bucket **1,040ms**, and sampled process-heap maximum **265,753,592 bytes**.
It found four display buckets and 60,557 method instances. Revision-pinned graph
inspection found positive 100-cycle goal exits from both A-only and B-only source
buckets within nine pages, establishing both production continuation arms in the
v2 result. This does not prove every alternative has finished. The job remained
`PAUSED / COMPUTE_BUDGET`; first local recommendation and whole-search completion
were not reached. The longer run and changed schedule make its heap/method counts
incomparable to the earlier single-run baseline as a pure speedup measurement.
A separate finite tiny-catalog test reaches actual enumeration completion, checks
every method's 100/300/500 observations, and certifies the outcome-responsive
strategy; it is not evidence of full production-catalog completion.

Complete primitive laws now share cache entries when retained item fields,
candidate definitions, ordered removal branches, action and provenance are equal.
Removed inputs may differ, but source item/method identities and conditional
distributions remain separate. Unknown/unavailable kernels do not share identities.
The 36,000-operation probe requested 1,596 state/action kernels and reused 884,
leaving 712 distinct kernels and 13,234 retained outcomes. It still discovered
60,557 methods and paused; sampled heap was 224,279,816 bytes, first response 350ms,
first method 1,038ms and first goal bucket 1,267ms. Both continuation arms remained
visible. This isolates method proliferation as a remaining issue and does not
establish a latency improvement. Exact cyclic tests independently solve
`A=B/2+1/4, B=A/2+1/2` (A=2/3, B=5/6), compare an alternative 3/5 action, reject
closed self-loop witnesses, and retain unknown bounds when support is incomplete.

With lazy runner allocation and invariant-law detection, the same 36,000-operation
probe retained 5,633 active runners for 60,557 discovered methods (119 complete).
First response was 267ms, first method 425ms, first goal bucket 993ms, and sampled
heap maximum 191,889,408 bytes. Both diamond continuation arms were retained.
First local recommendation and whole exploration remained unreached. This exposes
the next bottleneck: creating new method work substantially outpaces completing it.

Calculation v5 applies frontier backpressure at 512 unfinished methods (one
finite action-template batch may overshoot). All undiscovered states/actions stay
in the frontier and prevent enumeration completion. This is a scheduling window,
not a method-depth, currency-use or support-scope cutoff. The same 36,000-operation
production probe discovered 574 methods (61 complete, 513 live runners), used
170 distinct kernels for 1,731 source/action requests, and retained 18,990 kernel
outcomes. First response was 278ms, first method 516ms and first goal bucket 621ms;
both A/B continuation arms were found within six pages. Sampled heap maximum was
201,371,856 bytes, so this run does not establish a memory reduction over v4.
It remained paused without a local recommendation or global completion. The
working-set change reduces premature method creation and redirects work into
kernels; it does not solve the full combinatorial state space.
A tiny-catalog differential test runs a window of two and an unrestricted
scheduler to actual completion, comparing every method, all 100/300/500 exact
conditional distributions, every full state and final shortest method depth.
They are identical; the test also proves that deferred frontier work was retained.

Real-browser review was attempted with the computer-use browser tool, but its
launcher failed before opening a page: required Windows platform directory missing
(HRESULT 0x80070003). The temporary review page/server were removed. DOM tests do
verify all six synthetic methods, eight outcome connections, separate immediate
and final recommendations, and 100/300/500 observations; they do not replace a
real-browser layout/interaction check.

Calculation v6 / runner v4 additionally groups inputs within one method and one
primitive phase only when the action and cached immutable full transition law
are identical. This sums mutually exclusive arrivals of that method; it never
averages distinct methods' entry distributions. Unknown and unavailable exits
retain the original source state, and currency use is charged per incoming mass.
Differential tests compare shared kernels against independently allocated kernels,
including distinct entry laws, unknown mass, action choices and unavailable inputs.
Goal projection now computes the recursive dependency closure of active goal
entries; the original AST and all required numeric values remain unchanged.
Tests compare compiled and ordinary evaluation across every supported AST group
kind, pseudo dependencies, loss and merged modifiers.

Before this change, the same 36,000-operation Solar probe measured 10,052 states,
10,046 unexpanded states, 574 methods and 108,969 observation support entries in
30,492ms; sampled process heap reached 197,551,256 bytes. After the change those
state/method/observation counts were unchanged, with 4,760 execution inputs grouped,
24,854ms elapsed and 219,915,960 bytes sampled heap. First response/method/goal-bucket
were 28/328/328ms. Both A/B continuation arms remained present. These single runs
are diagnostic, not a reliable speedup or memory improvement claim. Neither reached
first local recommendation or whole-search completion. This is execution-law
reuse, **not** a quotient of persistent item states or a completed production search.
Persistent state reduction still needs a goal/action/transition equivalence proof
and a representation retaining each incoming method's conditional full-item law.

Strategy v4 is checked against independent exhaustive deterministic-policy
enumeration for 200 seeded small MDPs. A separate transitive-closure oracle checks
the returned combined policy has no reachable non-goal closed class. Larger tests
cover a 100-state recovery cycle with stalling alternatives, risky exits propagating
backward through 100 states, and incomplete/unknown graphs. Only fully enumerated,
resolved graphs enter this proof; the production Solar job above remains partial.
