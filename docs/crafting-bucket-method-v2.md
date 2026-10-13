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

- **Production search scale:** v8 carries disjoint weighted tier/roll cells through the
  production cursor, method runner and concrete-outcome pages. Family/tier-cell
  combinations are still separate, so large catalogs can pause before all direct
  methods or deeper layers finish. Keep `PAUSED`/partial until the actual remaining
  frontier is exhausted; roll compression alone is not full diamond completion.
  The ilvl-82 Solar inventory alone contains 209 positive-weight explicit tier
  definitions across 12 prefix and 18 suffix families (before state-dependent
  eligibility). Excluding the Spirit and gem-level families still leaves
  `C(11,3)*C(17,3)=112,200` possible family-set combinations before tier and roll
  variants; this combinatorial count is not a reachable-pool audit. ItemState
  already canonicalizes modifier order; ordering does not multiply item states.
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

Docker Java 21 full `check generateJooq bootJar` passed (661 unit/contract tests and seven disposable
DB integration tests). Docker Node 24 lint, typecheck, formatting, 2,147 frontend
tests and production build passed. The final v8 frontend run passed 2,152 tests plus lint,
typecheck, formatting and production build. The preserved v1 source oracle remains 650/21107.
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

## Exact reduction investigation at d6e4e51

The unchanged 36,000-operation Solar workload at `909f7d6` was profiled by nested
shape keys while preserving base, ilvl, rarity, implicit values, conditions,
socket/catalyst state and fracture flags. It contained 10,052 full item states:
one empty state, 1,544 one-explicit states and 8,507 two-explicit states. Dropping
only explicit rolls yields 275 modifier/tier shapes (largest: 1,420 full states).
Replacing modifier IDs with their complete conflict-family sets yields 88 shapes
(largest: 3,540). Keeping only affix counts yields six shapes (largest: 4,937).
These projections are **diagnostic, not valid equivalence classes**. Removing tier
or family information can alter eligibility, removal restrictions and transitions;
removing goal-dependent numeric values can alter AST evaluation after later loss.

Phase and incoming distributions are not in the persistent ItemState key. There
were seven expanded/discovered method sources, 574 method instances, 513 live
runners and 183 saved observation points containing 108,969 support entries;
the observation exit/active laws had 24 distinct contents. Identical stored laws
can potentially share immutable storage; distinct incoming laws must stay distinct.
The run paused after 29,620ms, with first response/method/goal bucket at
606/1,238/1,238ms, and sampled heap 187,555,552 bytes. Local recommendation and
whole exploration were not reached. This is the same budget, not a larger run.

`FiniteQuotientProof` is a closed-finite-graph proof oracle. It refines an initial
partition of full observation judgements until every member has the same labelled
action availability, exact mass into every result block and action costs. Missing
support, partial mass and non-closed graphs are rejected. It retains the original
transition laws and can push a supplied entry law through one selected action,
then condition on a result block; different incoming methods are never averaged.
This deliberately does not reduce production exploration before enumeration.
The tiny ordinary-currency catalog closes at six full states and refines to four
proved classes. Independent sparse finite-horizon Bellman calculations compare
every action value for 12 horizons, and first-hit propagation agrees at
100/300/500 Exalted/Annulment and Chaos cycles. Numeric observations, changed
future laws and different action costs prevent unsafe merging. Opposite 1/4–3/4
incoming roll laws produce distinct full goal arrivals despite sharing a block.
On the real Solar catalog, all 16 basic actions were enumerated for each of the
same-tier Countess Spirit rolls 47, 48, 49 and 50. Their projected transition laws
agree exactly when only that interval is collapsed; Exalted goal arrivals remain
distinct full items per source. This is a local witness for one interval, **not**
a global Solar equivalence proof or production integration.

Production needs lazy roll-domain variables attached to modifier identities,
shared eligible pools, and a conditional-law expression per entry. A variable may
be replaced by a goal interval only with proof that all relevant AST judgements
and future action branches are constant on that interval. Correlated variables
must retain their joint law. Concrete roll enumeration is needed when inspecting
full outcomes or when a rule/goal cannot be evaluated exactly on the representation;
it should not be mandatory for every unrelated roll before graph exploration.
Enumeration is a fallback only for a known finite law; it cannot repair an
unverified joint-roll model or missing candidates, which must remain unresolved.

The implementation boundary identified at that checkpoint was replacing the primitive cursor's one-roll
output with a modifier-tier plus exact roll-domain law, then making method runners
carry those laws through append/remove/condition operations. The existing paged
full-item API requires a lazy expansion cursor for those expressions. The finite
proof module and local Solar check are references for that work, not a drop-in
substitute: applying a post-hoc quotient to the current partial graph would repeat
the original full enumeration bottleneck and cannot prove future equivalence.

Eventual success and finite method observations are different objectives. The
100/300/500 values describe each arrow; solving an unlimited sequence of such
arrows may make many strategies all have value one. The current returned proper
policy is a success-probability witness, not proof of a unique fastest/cheapest
strategy. No currency prices, total attempt cap or secondary preference is defined.
The user subsequently chose to preserve exact success-probability ties and not
add a cost objective. A scalar expected currency cost would additionally need an
exchange-rate/weight policy; per-currency expected counts alone only give a partial
ordering.

## Production deferred roll representation (v7)

`DeferredItem` stores an immutable representative plus disjoint uniform integer
roll bands per modifier. Each cell is a product law under the existing declared
single-stat roll model. It does not average entries or posit a joint-roll model.
The primitive cursor emits each safe interval once, multiplying the elementary
roll probability by its integer cardinality. Retained variables survive additions;
removal marginalizes only the removed variable, and replacement creates a fresh
roll law. Method runners and outcome-responsive strategy calculations now use
these cells. Distinct method distributions remain separate.

The initial production gate is the reviewed Solar basic-currency model without
active omens, special-state transitions or catalyst quality. Other bases and
unproved numeric dependencies retain exact individual rolls. All candidate tiers
and non-goal candidates remain in the denominator. For direct explicit targets,
an interval may span values only when all contributing modifiers share a conflict
family, so at most one can contribute; every active entry's numeric threshold is
converted to an integer boundary. COUNT/AND/NOT/IF still evaluate their complete
AST on every result. Pseudo and weighted dependencies keep their relevant rolls
individual. Unrelated rolls may share intervals because the supported action
rules do not inspect their values. Every starting deterministic explicit value is
also split into a singleton, preventing overlap with regenerated uniform cells.

The full-item detail endpoint uses BigInteger offsets and Cartesian-product random
access to emit at most 48 concrete items per page. It divides cell mass by exact
cardinality, without expanding the full product in memory. Tokens remain scoped
to job revision, method and observation. Source cards now show ranges in all six
languages; detail cards show actual values. Snapshot metadata separately reports
roll representation, partition and strategy versions.

The small production-search differential test exhausts both eager and deferred
graphs, then compares every method at 100/300/500: each exclusive exit, active
law, initial/cycle consumption and strategy bounds. The oracle integrates each
specified entry cell's law, not alternative actions. Real Solar checks restore
the complete primitive law, split Spirit 47 from 48–50 at a target of 48, compare
all 16 basic actions, and check existing Life preservation, loss and reacquisition.
The production page test restores all real Exalted outcomes exactly once, checks
mass against eager enumeration, repeats cursors and rejects a wrong observation
scope. A separate cell test exercises random access over `2^126` and `2^127` outcomes.

Strategy v5 preserves equal attained success probabilities. Tied arrows each have
a proper stationary-policy witness obtained by replacing that source's action in
the witnessed policy; an equal-valued self-loop that never reaches success is
excluded. Different alternatives' witnesses must not be combined arbitrarily into
a new cyclic policy. No currency-cost preference is added. The UI separately labels
unlimited-repeat eventual success and per-method 100/300/500 observations. For
example, immediate certain success and a retry with probability 1/2 per cycle tie
at eventual one, but their finite observations are `1` and `1-2^-N`, respectively.

An initial v7 36,000-operation run retained 916 cells representing modifier/tier
shapes instead of 10,052 concrete states, and 32,454 observation support entries
instead of 108,969. Sampled heap was 149,080,488 bytes. It remained partial and first
visible result regressed to 25,836ms. Follow-up changes restore kernel reuse after
removing a roll variable, prioritize shallow single-method kernels while retaining
FIFO service for longer work, and let immutable snapshot reads bypass the worker
monitor. Performance/completion claims require the follow-up measurement below.

Control requests now share a fair job lock with computation slices; immutable GET
snapshots remain lock-free. The client refreshes and retries only revision
conflicts (at most three refreshes), retains the identical command on a transport
retry, and never overrides a changed generation. Other conflicts remain visible.
This fixes the cancellation race found by the production performance probe; it
does not turn a paused or cancelled exploration into a completed result.

The follow-up isolated Docker probe returned the first snapshot in 94ms and the
first visible method observation in 246ms. A goal bucket first appeared at
14,409ms; both A-to-C and B-to-C entry buckets were present. At the 60s driver
stop, cancellation completed and metrics were captured at 61,345ms: 23,855
operations, 632 disjoint calculation cells representing 28,647 concrete items,
619 cells with deferred rolls, 626 unexpanded cells, 616 method instances, 102
completed methods and 514 live runners. There were 204 distinct kernels for
1,036 source/action requests (832 shared lookups), 11,880 retained kernel
outcomes, 6,187 folded execution inputs and 34,185 observation support entries.
Sampled process heap peaked at 138,158,960 bytes; Linux process VmHWM was
360,692 KiB (includes JVM and fixtures, not a job-only allocation measure).
First local recommendation and full completion were not reached. This was a
bounded latency probe cancelled by its driver, not a completed production search.
Comparisons with the earlier 36,000-operation run are not equal-work throughput
comparisons. The retained modifier/tier frontier remains the next scale boundary;
roll-cell compression alone does not prove that exhaustive Solar exploration fits.

### Extended profile and exact arithmetic follow-up

At `d3333d8`, an isolated JFR-enabled 180s probe reached 67,983 operations after
five resumes. It retained 1,025 cells representing 81,321 concrete items, with
1,018 cells still unexpanded; 141 of 658 methods had completed. The driver
cancelled successfully at 182,314ms. First snapshot/method/goal times were
102/265/14,801ms; first-local and full completion were unreached. Sampled heap
peaked at 224,367,936 bytes and process VmHWM was 491,248 KiB. These are process
measurements with the profiler enabled. Both diamond continuation arms remained
available. The 13-page graph inspection happened after the captured elapsed time.

16,135 of 16,427 sampled worker stacks contained `MutableBigInteger.binaryGCD`
(98.2%, inclusive sampled stacks, not a wall-time percentage). A thread dump also
showed graph-page exit aggregation in Fraction.add/gcd after calculation stopped.
This gives a second bottleneck beyond the state frontier: repeated exact fraction
normalization and repeated aggregation of immutable observations.

The follow-up arithmetic uses equal-denominator addition or an LCM denominator
and exact zero/one identities; it never rounds probabilities. A direct rational
oracle checks 250 deterministic pairs with large shared denominators. Job-local
arrow presentation reuse is keyed by method identity, status/reason and identical
immutable observation objects, so revised points invalidate it and old revision
pages remain valid. This is presentation reuse, not a completed calculation cache;
partial method status is preserved. No entry laws or alternative action masses
are pooled by this cache.

With those exact-arithmetic/presentation changes, the same JFR-enabled probe
reached 120,000 operations in 39,428ms, then stayed `PAUSED / COMPUTE_BUDGET`:
the measurement driver stopped resuming once its sampled process-heap guard of
300,000,000 bytes was exceeded. That driver guard is not a production currency
limit and is not the service's retained-outcome memory limit. First snapshot,
method and goal times were 90/245/4,181ms. It retained 1,584 cells representing
105,846 concrete items, 1,576 unexpanded cells, 212 completed methods of 735,
523 live runners, 59,929 kernel outcomes and 67,632 observation support entries.
Sampled heap peaked at 326,854,224 bytes; VmHWM was 533,968 KiB. Both continuation
arms remained visible. First local recommendation and full completion were still
unreached. Of 3,637 worker samples, 3,517 contained binaryGCD, so normalization
remains a CPU hotspot even though substantially more work completed sooner.
The runs have different stop conditions and are not a fixed-work benchmark.

The remaining acceptance blocker is explicit: the production graph still indexes
modifier/tier combinations separately, and the outstanding alternatives/frontier
have not been exhausted for the requested real Solar example. The next change
must separate a proven transition-equivalence key from each incoming full-item
conditional law, including tier/family correlations and removal/replacement.
Raising the budget, dropping expensive alternative methods, or calling the four
visible goal-status buckets a completed graph would not satisfy this contract.

### Disjoint weighted tier cells (v8)

The next production representation groups modifier/roll intervals only when their
family set, affix type, tags, supported-currency minimum-level eligibility vector
and every supported direct goal's numeric region agree. It is restricted to the
same reviewed Solar no-omen/no-quality scope. Initial exact modifier values form
separate singleton cells. Weighted/pseudo or unproved numeric dependencies retain
the earlier exact representation. No assumption of equal family probability is
introduced: each interval retains candidate weight times interval-width/tier-width,
then is normalized within its disjoint cell. Other eligible families remain in the
primitive denominator.

Cells are job-fixed and disjoint, not a different overlapping mixture for each
currency. This is essential: overlapping ordinary/greater-currency tier mixtures
would otherwise duplicate concrete items in a result and erase history-dependent
correlations. With this partition, a retained cell's law is unchanged by any
supported rule; a fresh addition independently draws a cell's proven conditional
law, and a removal marginalizes that component. Correlations between cell choices
remain in the full item-cell distribution. Distinct incoming methods still have
separate distributions.

Production cursors aggregate only whole eligible cells and assert that no member
crosses an actual eligible-pool boundary. Canonical state identity includes the
weighted tier law, and the primitive cache includes retained tier laws. Concrete
pages use nonuniform per-item probabilities and arbitrary-size random access;
source cards list disjoint localized ranges instead of inventing values in gaps.
Full weighted enumeration checks pass for this unit; the full production frontier is still incomplete.

The weighted tiny search exhausts both representations and compares every method
at 100/300/500, each exclusive exit, active distribution, initial/cycle cost and
strategy bounds. Solar tests compare both roll-only and weighted-tier source cells
under all 16 actions against full weighted enumeration. The actual detail API
restores every root Exalted outcome once with its original probability. A separate
check proves an initial fixed Life 50 unrelated to the requested goal stays a
singleton disjoint from regenerated weighted cells.

The v8 isolated JFR probe reached 120,000 operations in 41,634ms: 1,037 cells
represented 149,026 concrete items, versus v7's 1,584 cells and 105,846 concrete
items at the same operation count. Representation changes the work inside a step,
so this is not an equal-work timing comparison. There were 588 weighted-tier
cells, 1,029 unexpanded cells, 270 completed methods of 784 and 514 live runners.
Kernel outcomes numbered 59,908 and observation support entries 58,398. First
snapshot/method/goal times were 111/253/3,048ms. Sampled heap peaked at 309,165,048
bytes and VmHWM at 505,032 KiB. The driver stopped resuming at its 300MB sampled
heap guard with `PAUSED / COMPUTE_BUDGET`; first-local and whole exploration were
still unreached. Both A-to-C and B-to-C continuation buckets remained present.

## Joint exchangeable-family cells (v9)

The production partition now also represents a joint distribution over anonymous
families whose complete basic-currency eligibility-weight profiles are identical.
The profile includes affix side and weights in every actual currency eligibility
category (the current thresholds are 0, 35, 44, 50 and 70). Goal-relevant families,
all initially observed families, overlapping conflict-family sets and multi-stat
families are excluded. The existing no-quality/no-omen support gate remains.
Unequal profiles, including the Solar cold-resistance family, stay separate.

An orbit records occupied counts per eligibility category. Its conditional law is
uniform over injective family assignments, followed by each family's weighted
modifier-tier and roll law. Selection is without replacement: two occupied slots
cannot independently choose the same family. Different category-count cells are
disjoint. Outer state probabilities preserve correlations between cells, while
incoming methods retain their own conditional distributions.

This reduction follows by induction from the empty anonymous-family population:
identical profile totals make each unoccupied family equally likely, and uniform
eligible removal marginalizes the same joint law. Primitive branches are merged
before a complete kernel is exposed to a method runner. A single projected raw
branch must not be interpreted as its own concrete conditional distribution.
All supported actions and goal evaluation are invariant under these permutations;
unsupported actions must not reuse this proof.

Concrete distribution cursors restore weighted item probabilities from the joint
law. Display cards show whole family combinations, rather than falsely presenting
independent choices. Hashes include the law identity and equality still checks
actual profile contents. The partition rejects a fixed observed modifier that a
caller incorrectly supplies as an anonymous family.

Verification of v9 passed the independent complete tiny-search comparison for all
methods and all three observations, including full conditional exit laws, active
laws, currency use and strategy bounds. Its four-family concrete oracle has 24
states and 1,477 method instances; the test work guard was increased from 500,000
to 2,000,000 steps after the former stopped with 108 methods still running. No
production budget or expected probability was changed. The real Solar primitive
oracle also checks all 16 actions from a two-family, mixed-category joint cell.

The isolated v9 probe reached 132,000 operations in 27,946ms, with 794 cells
representing 185,009 concrete items (345 orbit cells, 348 weighted-tier cells).
It retained 51,719 kernel outcomes and 40,713 observation support entries. Of 784
methods, 260 completed and 524 still ran; 786 states remained unexpanded. First
snapshot/method/goal times were 132/284/7,024ms. Sampled process heap peaked at
233,481,976 bytes and VmHWM at 460,344 KiB. The driver exhausted its ten resumes
with `PAUSED / COMPUTE_BUDGET`, below the 300MB driver guard. Both continuation
buckets reached the goal. First-local recommendation and full completion remain
unreached; no global completion or optimal full-catalog strategy is claimed.
The changed step composition and operation count preclude a fixed-work speedup
claim against v8. Frontend lint/type/format, all 2,152 tests and production build
passed (the existing large bundle warning remains).

Docker backend check, integration tests, jOOQ generation and bootJar also passed: 664 unit/contract tests and 7 integration tests, including fixed observed-family exclusion. Browser verification remains blocked by the previously recorded launcher failure; no application server was started.

## 2026-10-13 resumption: WIP verified

The user explicitly resumed work on 2026-10-13. The prior-day stop instruction
is historical. The WIP at e9f5e258a04d2bf8535ad5186ed80d5e528a0852 passed Docker
`check generateJooq bootJar`, including 667 unit/contract tests and 7 integration
tests. The checked work branch was fast-forwarded to this verified source; the
WIP backup branch remains intact. The earlier handoff records the checks that
were available at the time of the stop, not this new verification.

The rational arithmetic and observation-specific local recommendation changes
are now fully backend-verified. Frontend sources/dependencies are unchanged from
the previous 2,152-test full frontend check. Production whole-search completion
and real browser verification are still open; this verification does not imply
that the full Solar frontier has been exhausted.

## Reachable-action partition and consumed-law cache (v10)

Rare starts remain rare under every supported basic action. The partition now
uses that shared-rule invariant: its eligibility vector includes all seven
reachable Exalted/Chaos/Annulment variants (thresholds 0, 35 and 50), rather than
also splitting at the unreachable magic-action thresholds 44 and 70. Other
starts keep the conservative full action set. Method discovery still inspects
all 16 supported actions; unavailable actions remain unavailable. An adapter
rejects a caller that attempts an available out-of-scope action with a partition
compiled for a different reachable rarity scope.

This coarsens only proven equivalent cells. Family conflicts, eligible candidate
weights, goal numeric regions, initial fixed values and conditional tier/roll
laws remain intact. Full concrete restoration and all 16 action comparisons
pass, as does the independent complete tiny method-search oracle. A regression
also verifies that a rare partition cannot be reused for an available Greater
Augmentation on a magic item.

Primitive cache identity now retains an orbit law only for removal branches in
which at least one of its variables survives. Once the entire variable is
consumed, it cannot affect the fresh outcome law. Partially retained laws remain
conservatively separate. Tests compare both the shared identity and full result
law of last-modifier Chaos/Annulment, and preserve different addition identities.

The isolated v10 Solar probe used the same 132,000 operations: 610 cells,
179,706 represented concrete items, 601 unexpanded states, 305/819 methods done,
514 live runners, 50,738 kernel outcomes and 33,798 observation support entries.
Elapsed time was 16,202ms; first snapshot/method/goal were 117/269/1,388ms.
Sampled heap peak was 221,747,376 bytes, VmHWM 458,756 KiB. The root had 42
methods, 18 with all observations; the remaining 24 predominantly add modifiers
repeatedly. The job paused at the diagnostic resume budget. First-local and
whole-search completion remain unreached. The preceding v9 resumed measurement
was 17,258ms, 794 cells and 40,713 observation support entries at 132,000
operations. Different representations change work per operation, so this is
not a fixed-work timing comparison.

V10 passed Docker `check generateJooq bootJar`: 669 unit/contract tests and 7
integration tests. No frontend source changed in this unit. A separate real
Chromium run loaded the actual MethodSearch component from a static file using
the shared diamond contract: all six locales rendered six method cards/eight
arrows, 100/300/500 radio changes worked, no page errors occurred, and document
width stayed within 1440px desktop and 390px mobile viewports. Screenshots and
results are under `codex/bucket-browser` outside Git. This is a component browser
check with recorded contract responses, not a live-server end-to-end check.
The native browser connector still fails at startup with HRESULT 0x80070003.
No application server was started.
