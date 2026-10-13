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

## Shared eligibility and action-equivalence proof (v11)

Tier/family compression now derives its eligibility signature from the shared
`AdditionRules.pool` result. Comparing required item level with currency cutoffs
alone missed the rule that retains a generation family's highest eligible tier
even below that cutoff. A regression uses two families with equal ordinary total
weight but different highest-tier weights: Greater Exalted must retain the high
tiers with probabilities 1/2 and 1/6, and neither lower tier. The compressed law
is restored to every concrete outcome and compared with the original kernel.
Multi-family generation groups conservatively keep the roll-only representation
because their tiered-currency grouping is not supported by the shared rule.

`BasicTransitionClasses` separates an action-equivalence key from an incoming
item law. Keys retain nonexplicit context, provenance, family conflicts, goal
regions, fracture status and all goal observations. A completed kernel describes
class probabilities; its replay requires the actual input item and preserves
that item's values. It never substitutes the witness for an incoming distribution.
A complete 24-state graph checks every basic action against the concrete kernel
and an independent finite probabilistic-bisimulation oracle. Real Solar single
modifier members additionally check every basic action and the Spirit threshold.
This is a verified primitive foundation, not yet the production method-transport
integration or a declaration that the full search is complete.

A diagnostic of the preceding class key estimated 112,236 possible Solar
classes (22 family profiles). In 60,004ms it discovered 30,276 classes and expanded
7,186, with 23,090 pending; 49,777 kernels produced 1,855,829 projected edges.
Sampled heap peak was 165,483,656 bytes. Its 60-second driver guard stopped the
diagnostic with `complete=false`. It did not measure completed methods, local
recommendations or eventual optimal strategy, and it preceded the shared-pool
eligibility correction. Production integration must preserve conditional entry
laws separately; replacing full deferred states with these witnesses would be
incorrect. Merely increasing the existing enumeration budget is not the next
implementation step.

V11 passed Docker `check generateJooq bootJar`: 672 unit/contract tests and
7 integration tests. Frontend source and dependencies remain unchanged from
the previously verified build and six-locale Chromium component check.

## Conditional mixtures inside method execution (v12)

The production method service now compacts mutually exclusive outcomes inside
one method at cycle boundaries. It combines one modifier family at a time only
when the entire remaining item and its conditional laws are equal. The family,
fracture flag and compiled numeric goal region must also agree. Other-family
correlations and alternative methods remain separate. Overlapping numeric
intervals use an exact piecewise rational density, not an independent-roll
approximation. Anonymous family orbits retain their existing joint law.

Each generated mixture records its exact coefficients over the original finite
cells. Method observations are expanded back into those cells before renewal
proofs and graph insertion. Posterior weights therefore remain entry-conditional
probabilities rather than creating a potentially unbounded series of new graph
node identities. Renewal shortcuts also reject decreasing per-cell cumulative
exits: different representations alone cannot prove stationary renewal.

Compaction scans yield through the method cursor and consume normal calculation
work slices. A partial compaction cannot publish a completed cycle. Primitive
first-hit checks and currency accounting precede the compaction. Tests restore
overlapping weighted laws exactly, compare all following basic actions, retain
correlated inputs, check first-hit/currency use under one-input slices, and
compare every completed tiny-catalog method at 100/300/500 cycles with the full
concrete oracle. That graph closes with the original six cells rather than
posterior mixture nodes. Docker `spotlessApply check generateJooq bootJar` passed
with 677 unit/contract tests and 7 integration tests. Frontend sources are
unchanged. This change does not claim whole-search completion.

Mixed inputs reuse the original-cell primitive kernels by linear composition.
Dependencies are scheduled before the composition; a partial dependency or
compaction remains pending. If all components share one complete kernel, the
same immutable object is reused. Otherwise weighted outputs are accumulated and
compacted in bounded work slices. Original cells cannot later become generated
posterior laws, so basis references stay flat and acyclic.

The isolated v11 comparison at 132,000 operations took 16,034ms: 610 graph cells,
305/819 completed methods, 33,798 observation support entries and a sampled heap
peak of 220,069,960 bytes (VmHWM 448,144 KiB). The first v12 attempt, which built
fresh primitive kernels for mixed inputs, took 17,001ms with 293/812 methods done
and 254,564,224 bytes sampled heap. This was not a performance improvement.
After linear kernel reuse, a diagnostic observed 15,533ms, 610 graph cells,
280/798 methods done, 31,689 support entries and 283,444,304 bytes sampled heap
(VmHWM 575,448 KiB). It merged 35,318 internal cells across 1,993 distributions
and retained 751 mixture laws. That diagnostic shared its JVM with the new
mixture tests, so startup/heap figures are not an isolated comparison. None of
these runs completed a first local recommendation or the full Solar frontier;
all had 18/42 root methods ready. Work per operation also changes. The current
mechanism proves conditional-law transport but has not yet demonstrated the
required whole-search performance. Anonymous orbit category mixtures, which this
one-axis tier compactor leaves unchanged, remain a candidate for further exact
reduction rather than another raw enumeration budget increase.

A subsequent isolated v12 run confirmed the limitation: 132,000 operations in
15,949ms, 610 cells, 280/798 methods done, 31,689 observation support entries,
and 285,466,680 bytes sampled heap (VmHWM 537,008 KiB). First snapshot/method/goal
were 142/335/1,953ms. Internal compaction merged 36,455 cells and retained 751
generated tier laws. The first local recommendation and full completion remained
unreached. The verified v12 source was backed up at
`ad467e9c67548ba7d244c6e2a3cc64676f860b1d` before further changes.

## Joint category mixtures and derived-kernel lifetime (v13)

Anonymous family orbits can now carry an exact weighted mixture of category-count
laws at fixed occupancy. Every component retains the original injective family
assignment and weighted tier/roll law. Components are flattened, normalized, and
checked against the complete profile, not just a profile hash. Different occupied
slot counts remain separate. Compaction combines one axis only when the entire
remaining conditional item is equal; correlated diagonal inputs therefore stay
separate, while an independent rectangle can compact without losing its joint law.
Graph observations and primitive dependencies restore the original finite cells.
Direct primitive evaluation of a mixed orbit is rejected rather than treating its
representative category as the whole distribution.

The larger independent fixture has 54 concrete states and 2,849 methods. Its first
full run passed 681 of 682 tests but stopped the deferred comparison at 100,001
retained kernel outcomes, with 31,368 distinct kernels and only 24 graph cells.
The failure exposed posterior-law cache retention, not a probability discrepancy.
The retained-outcome limit remains 100,000. Derived kernels now use a bounded
reuse cache; deliveries are pinned until every waiting method obtains the complete
kernel. Eviction removes only reconstructible derived results and their metadata;
original finite-cell kernels and method observations remain available. In-flight
runners own immutable kernel references and are unaffected by cache eviction.

Internal complete-cycle snapshots avoid repeatedly summing all accumulated exits.
Public observations, saved 100/300/500-cycle results and renewal shortcuts still
validate total mass. Renewal checks compare each active component by exact integer
cross multiplication and stop on the first mismatch, before cumulative checks.
They do not construct and reduce a whole temporary scaled distribution merely
to reject nonstationarity. Exact fraction addition can retain an
unreduced rational internally; multiplication and every public value operation
canonicalize it. The canonical numerator/denominator JSON contract, equality and
hashing are unchanged. Atomic immutable representation snapshots support concurrent
reads. An independent Java arithmetic driver verified 168,500 comparisons and
64,000 concurrent comparisons before integration. Docker
`spotlessApply check generateJooq bootJar` then passed 683 unit/contract tests and
7 integration tests in 7m18s for the full check/build. The 54-state, 2,849-method
comparison completed with derived-kernel eviction enabled, below the unchanged
100,000-outcome guard, and matched every full conditional 100/300/500-cycle law,
currency count and strategy bound. Two intermediate verification runs were
intentionally stopped for the subsequent cache/comparison fixes and are not counted
as passes. The isolated production measurement remains separate.

The isolated v13 Solar run used the same 132,000-operation driver guard and paused
for `COMPUTE_BUDGET` in 8,111ms. First snapshot/method/goal were 160/323/925ms.
It retained 609 graph cells (600 unexpanded), 272/784 completed methods, 30,744
observation support entries and 44,715 cached kernel outcomes. Internal compaction
merged 43,625 cells, including 20,398 orbit cells, and retained 579 generated tier
laws. Sampled heap peak was 283,476,352 bytes; process VmHWM was 533,372 KiB.
Only 18/42 root methods were ready at both 100 and 500 cycles, with no first local
recommendation or full completion. Compared with isolated v12's 15,949ms, this is
a lower runtime for the fixed driver guard, not proof of a completed-work speedup:
work per operation and the completed-method count differ. Total-memory control and
the remaining production frontier still require further work.

A longer v13 diagnostic used a 1GiB test heap, a 500,000 retained-outcome allowance
after the first resume, and an external 650MB sampled-heap guard. It stopped after
408,000 operations/33 resumes in 34,938ms at 656,608,688 sampled bytes (VmHWM
902,364 KiB). It had 656 cells, 317/833 completed methods and 163,924 retained kernel
outcomes, but still only 18/42 root methods ready and no local/complete milestone.
Those are diagnostic limits, not product defaults or currency-use limits. Raising
the computation allowance alone did not solve the outstanding frontier.

## Lifecycle boundary follow-up

A resume request below a previously enlarged retained-outcome allowance now returns
`422 INVALID_BUDGET` without changing generation, revision or job state. The new
regression reproduced the prior uncategorized `IllegalArgumentException` before
the fix. Additional service tests execute an old queued generation after cancellation
and resumption, replay a command after later revisions, reject reused/stale commands,
retain an available old page while rejecting cross-revision cursors, expire old
revision pages, and replace an expired job using the same client request ID without
accepting the old worker's publication. Docker ran all six lifecycle/service tests,
`spotlessCheck` and `bootJar` successfully. Calculation code is unchanged from the
683+7-test v13 full verification.

## Bounded primitive reuse (v14)

Completed original and mixed primitive kernels now share an access-ordered cache.
Eviction starts at three quarters of the configured retained-outcome allowance;
partial kernels are never cached as complete or discarded. Every waiting method
pins a completed delivery until it obtains the immutable kernel, including a cache
alias discovered after a miss. A mixed kernel pins all original-cell dependencies
until its complete linear composition is available. Eviction removes all aliases,
identity indexes and scheduling metadata for that kernel. Later demand reconstructs
it from the same versioned rules. Shared immutable outcome lists are conservatively
counted for each cached reference, so evicting an original does not hide storage
still retained by a derived alias. This remains a kernel-cache budget, not a total
process-memory guarantee; in-flight runners, graph results and revisions are separate.

Immutable fraction hashes and orbit representatives/hashes are memoized without
changing equality, hash values, canonical fraction JSON or conditional item laws.
A tiny full search with forced primitive eviction matches every completed method
and state. A separate 32-outcome-cache test reconstructs mixed dependencies and
matches every full concrete 100/300/500-cycle law and strategy bound. These targeted
tests and arithmetic/mixture checks passed. Docker
`spotlessApply check generateJooq bootJar` passed 689 unit/contract tests and
7 integration tests; the full check/build took 6m59s. Production completion has
not yet been demonstrated.

The isolated 132,000-operation v14 measurement took 7,258ms, with first
snapshot/method/goal at 124/266/836ms. It had 609 cells, 272/784 completed methods,
30,744 observation support entries, sampled heap peak 272,629,760 bytes and VmHWM
490,908 KiB. The retained-outcome counter now conservatively counts shared cached
references, so it is not directly comparable with v13's unique-list ownership count.

A longer v14 diagnostic kept the product's 100,000 kernel-outcome allowance, with
the same 1GiB test heap and external 650MB sampled-heap guard used for the extended
v13 run. It stopped at 504,000 operations/41 resumes in 37,676ms: 656 cells,
312/833 completed methods, 34,251 observation support entries and 74,978 retained
kernel references. It evicted/rebuilt 5,719 primitive and 1,806 derived kernels.
Sampled heap peak was 663,320,384 bytes (VmHWM 913,000 KiB), with 378,888,448 bytes
still used after an explicit diagnostic GC. The measured search time excludes
that GC. Root readiness stayed at 18/42 for 100 and 500 cycles; no first local or
full completion was reached. This confirms bounded kernel reuse, not a solution
to the total live-distribution/frontier cost. More operations did not mean more
completed methods than the differently budgeted v13 diagnostic.

## Incremental terminal laws (v15)

Method execution now restores each newly absorbed result into its original finite
cells once and adds that increment to its exit accumulator. Existing accumulated
goal/checkpoint/loss/dead/unresolved laws are not recompressed and expanded again
at every later cycle. Active laws retain their conditional compaction. Restoring
a multi-axis law uses a Cartesian-position cursor that emits at most the requested
number of original-cell combinations per slice; it does not allocate the complete
product before yielding. An already-original law takes a direct immutable fast path.
The application also slices active-law restoration before evaluating a completed
cycle. A partially restored terminal law cannot expose a completed boundary.

First-hit decisions precede normalization, so normalization never executes another
primitive or changes consumed currencies. An already terminal root still consumes
zero cycles. Public result mass checks remain mandatory. Targeted tests cover
one-output slices of a correlated/independent orbit fixture, terminal preparation,
first-hit before a destructive next action, unchanged cumulative exits, and the
small complete concrete/deferred method comparisons. Docker
`spotlessApply check generateJooq bootJar` passed 692 unit/contract tests and
7 integration tests; the full check/build took 5m38s.

The isolated 132,000-operation v15 run took 7,270ms, with first snapshot/method/goal
at 116/269/686ms, 609 cells, 277/798 completed methods and 31,242 observation support
entries. Sampled heap peak was 270,805,104 bytes (VmHWM 488,612 KiB). Internal
compaction processed 1,219 distributions and retained 403 generated tier laws,
versus v14's 1,997 distributions and 579 laws at the same operation guard.

The extended diagnostic used the same default 100,000 kernel-outcome allowance,
1GiB test heap and external 650MB sampled-heap guard as extended v14. It stopped
at 504,000 operations in 37,119ms: 656 cells, 310/833 completed methods, 34,026
observation support entries and 74,983 cached kernel references. Peak sampled heap
was 652,172,672 bytes, post-GC heap 367,419,952 bytes, and VmHWM 892,328 KiB. It
processed 3,147 compactions and retained 1,039 generated tier laws. Both runs still
had only 18/42 root methods ready and no first local recommendation or full
completion. Reducing cumulative normalization work did not resolve combinatorial
state/method expansion; larger fixed work allowances are not the proposed fix.

## Indexed transition-class kernels

The class-kernel cursor now groups generated modifier/roll regions within each
removal branch before constructing full item witnesses. Candidate-band mass uses
the primitive cursor's validated pool, effective weight policy, roll range and
removal denominator. Witness construction uses the shared basic-currency finisher.
This does not infer eligibility from family counts or replace individual-candidate
weights with family weights. Equivalent candidates' already-computed masses are
added only within the same mutually exclusive action outcome law.

The original projection cursor remains an independent comparison path. Tests compare
all supported basic actions on every state of the small exhaustive fixture and on
production Solar goal-threshold variants. Indexed reads do not advance the primitive
cursor. Replay still requires the actual incoming full item and preserves its rolls;
the class witness is not a conditional distribution or a replacement for that item.

This is a foundation for separating class probability calculation from conditional
item transport. The production method calculation does not yet execute methods on
these class kernels, so this change alone does not establish production completion
or resolve the recorded distribution/frontier growth.

Docker `spotlessApply check generateJooq bootJar` passed 693 unit/contract tests
and 7 integration tests. The full check/build took 6m01s.

Two isolated 60-second diagnostics used the same current rules and source, a blank
Rare Solar82 root, and Projectile+3 plus explicit Spirit47 goal observations. The
original projection discovered 34,280 classes and expanded 8,177, leaving 26,103
pending; it calculated 56,672 kernels/2,136,547 class edges with a sampled heap
peak of 137,659,448 bytes. The indexed cursor discovered 75,079 and expanded
25,546, leaving 49,533 pending; it calculated 176,022 kernels/6,739,970 edges,
materialized 7,139,988 witnesses and reached a sampled heap peak of 229,495,800
bytes. Both stopped at the time guard with `complete=false`. This compares work
within a time window, not equal completed work or peak memory for equal frontiers.
The 112,236 combinatorial class bound and 22 family profiles are unchanged.

## Interned class identities

`BasicClassIndex` assigns stable job-local integer identities to complete class
keys. A projected kernel stores integer destinations, while the index keeps one
key and witness per class. It checks structural key equality through the map and
through replay validation; a matching hash is insufficient. Its cursor projects
candidate bands first, then interns one result per step. No partial kernel is
available from `result()`. The witness serves class calculation only. Replaying
an actual incoming item still uses that item's full state and validates its class.

The small closed-graph test compares every integer kernel with the original
full-state projection for all supported actions, including unavailable actions.
It verifies stable identities, exact masses and distinct entry-roll replay.

A separate diagnostic ran the repeated Exalted/Chaos method directly on class
keys, with first-hit goal/checkpoint classification, a 2,000-kernel cache and a
450MB sampled-heap guard. It reached three completed cycles in 7,558ms, discovered
14,058 classes and computed 9,934 kernels/617,618 outcomes before stopping at a
sampled heap peak of 479,701,792 bytes. Replacing kernel destinations with integer
identities reached four completed cycles in 30,610ms, 48,874 classes and 32,850
kernels/1,867,794 outcomes before stopping at 455,866,160 sampled bytes. These are
different amounts of completed work; neither reached a 100-cycle observation.
The diagnostic guard is not a currency limit or a supported completion boundary.

Remaining integration issue: storing exact full conditional laws at every method
boundary still couples display/transport size to probability calculation. The
recommended next representation is a conditional method operator retaining the
actual entry law, template/phases, first-hit classifier, observation cycle and
selected exit/class. It must reconstruct or page the actual item law when needed,
never substitute the class witness or average alternative methods. Class-only
method arithmetic also remains expensive; the integer representation is not yet
a completed production search engine, and no numerical probability contract has
been weakened or silently replaced by floating-point estimates.

Docker `spotlessApply check generateJooq bootJar` passed the complete 693
unit/contract tests and 7 integration tests in 5m49s for the class-index change.

## Bounded phase-kernel retention (v16)

The method runner previously collected every distinct kernel in a primitive phase
before consuming their outcomes. Those references remained live even after a
bounded application cache evicted them. It now consumes a prefix when 32 distinct
kernels have accumulated. Remaining inputs retain their phase and exact mass;
new next-phase inputs stay behind them and coalesce normally. Equal kernels still
share work within each prefix. This is a live-reference bound, not a restriction
on states, actions, cycles or currency use.

A regression with 96 distinct kernels proves that at most 32 are retained and
checks the complete first-hit exit law and preparation/cycle consumption against
closed-form fractions. It includes success before the second action, so batching
cannot charge that action on already successful paths. The targeted runner suite
passed. The same short class-method diagnostic reached four cycles, 48,874 classes
and 28,005 kernels/1,476,197 outcomes in 33,134ms before its sampled-heap guard
stopped it at 467,602,416 bytes. This still does not prove lower total live memory
or completion; sampled allocated heap includes garbage awaiting collection.

Docker `spotlessApply check generateJooq bootJar` passed all 694 unit/contract
tests and 7 integration tests for v16. The full check/build took 5m26s.

With the same fixed 512MiB JVM heap but without the early sampled-allocation guard,
the class-method diagnostic ran for 180,000ms and completed five cycles. It had
108,999 classes, 90,337 calculated kernels and 4,279,654 outcomes; the last complete
boundary had 24,884 active classes and 13,193 exit classes. Peak sampled allocation
was 531,628,032 bytes, but post-diagnostic-GC use was 116,095,128 bytes. It remained
partial at the time guard. This distinguishes allocation pressure from retained
heap; it does not establish a completed 100-cycle method observation.

The production v16 132,000-operation diagnostic took 7,001ms, with first
snapshot/method/goal at 128/280/727ms. It had 609 cells (600 unexpanded), 274/798
completed methods and 30,885 observation support entries. Sampled heap peak was
275,456,608 bytes and VmHWM 529,516 KiB. Root readiness remained 18/42 at 100 and
500 cycles, with no first local recommendation or full completion. The phase
reference bound is verified, but this production measurement does not demonstrate
a total-memory improvement or resolve the outstanding frontier problem.

## Cooperative class method marginals

`IndexedMethodRunner` now advances class-kernel construction and method execution
cooperatively. A missing kernel suspends before committing currency consumption or
phase progress. Only completed kernels enter its bounded LRU cache. Eviction drops
derived work, not states or probability mass; the fixed class index preserves
identities and exact incoming-item replay.

An independent forward enumeration of the closed concrete fixture compares every
exit class, active mass and currency cost at 100/300/500 cycles for retry and
loss-capable methods. An initial cache-limit-two test did not actually evict: its
first method needed only two class kernels. The guard correctly failed even though
all probability comparisons passed. Reducing the cache allowance to one forces
repeated eviction/recalculation; the same probability assertions then passed.
Docker `spotlessApply check generateJooq bootJar` passed 694 unit/contract tests
and 7 integration tests in 5m37s.

This runner returns class marginals, not full conditional item distributions. It
is not yet wired into the user API. The separate conditional method-operator
implementation and its full-state transport checks remain the next integration
step; a class witness must not be published as that distribution.

The longer cooperative Solar diagnostic completed the repeated Exalted/Chaos
method's class distribution at cycle 7, with no active mass. It took 326,202ms
and 63,196,775 cursor/execution steps under a fixed 512MiB JVM heap. It registered
108,999 classes and calculated 153,171 kernels, keeping 2,000 cached and evicting
151,171. The absorbed law has 73,318 exit classes. Peak sampled allocation was
536,228,880 bytes; post-diagnostic-GC use was 282,371,112 bytes. The complete
absorbed law can fill later 100/300/500 observations without further currency use.
This is one method, not the 42-method root comparison, first local recommendation,
whole graph or final strategy completion. During this diagnostic a separate
Java compilation check ran briefly; the elapsed time is not an isolated benchmark.

## Conditional method operators

`ConditionalMethodLaw` retains a normalized actual entry law and a chain of method
definitions, observation horizons and selected exit/classes. The class runner
creates an operator only for a positive, completed marginal and the correct
incoming class/job. Each probability is relative to that specified parent law;
alternative methods are not combined. Keeping an operator does not enumerate its
possible complete items or substitute a class witness for their distribution.

The reference full-state transport advances input preparation, each primitive
outcome, active-exit selection and normalization incrementally. It checks first hit
after every primitive, distinguishes unavailable and unresolved outcomes, verifies
total mass and verifies the selected mass against the independently calculated
class marginal before exposing a normalized result. Operator chains are traversed
iteratively. A saved operator is unchanged when its originating runner advances.
This full-state expansion still materializes the selected law before `result()`;
production pagination/resource handling and the UI/API connection are not yet
implemented and must not publish a witness in place of the law.

Tests compare full conditional distributions with independent concrete enumeration
at 100/300/500 cycles. They also check composed methods, a two-roll incoming law
versus a singleton with the same class probability, preparation at cycle zero,
already-successful starts, unavailable actions, zero-probability selections and
wrong-class/wrong-job inputs. A draft sliced-mass implementation failed compilation
by referencing a nonexistent fraction subtraction method; tracking the completed
phase's accumulated mass removed the unnecessary operation. No probability
assertion was relaxed. Targeted checks then passed, followed by Docker
`spotlessApply check generateJooq bootJar`: 694 unit/contract tests and 7 integration
tests, with the full check/build taking 5m29s.

## Probability-representation-independent API views

The progressive service now consumes immutable state-ID-based engine views. The
existing deferred-cell implementation is wrapped without changing the v2 wire
contract or probability calculations. Display representatives and optional actual
distribution cells are separate fields; a missing actual cell is rejected with
`CONDITIONAL_DISTRIBUTION_REQUIRED`, never replaced with the display witness.
Observations retain their identity across unchanged snapshots, and strategy edges
combine only exclusive outcomes within one method choice.

A complete small graph test compares every state, method mass, currency cost and
five strategy sweeps at 100/300/500 cycles with the original calculation. Lifecycle,
service and local-recommendation checks also pass. Docker
`spotlessApply check generateJooq bootJar` passed 695 unit/contract and 7 integration
tests. The production 132,000-operation diagnostic remained partial: 609 states,
798 methods, 274 completed methods, root readiness 18/42 at 100 and 500 cycles.
Elapsed time was 6,844 ms; first response/method/goal bucket appeared at
129/281/710 ms. Sampled heap peak was 281,545,640 bytes and process peak RSS was
505,660 KiB. Neither first local recommendation nor whole-search completion was
reached. This adapter does not yet enable the class engine in production.

## Shared indexed kernels and retained observations

`IndexedKernelStore` scopes completed primitive kernels to one class index/job.
Interleaved cooperative method runners share an unfinished request; only complete
kernels enter the LRU. Both kernel count and total cached outcome rows are bounded.
A delivery handle retains its complete law across cache eviction, including a
single kernel larger than the entire outcome allowance. These cache allowances
bound retained derived data, not currency use or exploration completeness. Active
requests and in-flight method phases remain separate working memory.

`IndexedMethodRunner.Snapshot` retains a completed observation and its conditional
input operator without retaining the mutable runner. A later request can bind the
saved observation to a verified actual incoming law. Absorbed observations can be
extended to later horizons without additional currency use; an active observation
cannot use that shortcut. This is a prerequisite for the class-engine API adapter,
not its activation. Tests retain an earlier observation across runner advancement
and compare its reconstructed conditional law with direct concrete enumeration.

Remaining integration decisions: expose incoming-law identities and bounded
asynchronous detail work explicitly; never serve class witnesses as distributions.
A full exact-fraction real-data method was measured separately at about five
minutes. Dense all-source/all-method distributions remain a scalability limit.
Certified enclosing probability intervals have been proposed to the user, but
are not implemented or assumed approved. Exact fractions remain the contract.

Validation: the shared-store version passed Docker `check generateJooq bootJar`
with 695 unit/contract tests and 7 integration tests (5m41s). After adding the
retained observation API, final-source targeted probability, engine-view, service
and lifecycle tests plus `spotlessCheck bootJar` passed (42s). The full test run
preceded that last small observation change; it is not reported as a final-source
full-suite rerun. No probability assertion was weakened.

## Explicit indexed API connection

The existing `POST /api/v2/crafting/method-searches` continues to use the deferred
engine and its unchanged v2 contracts. The additive `POST .../indexed` uses the
same validated create payload and returns `{search, rootEntry}`. It enables the
exact class engine explicitly; ordinary requests are not switched silently.
A client request ID cannot be reused between the two engines. Active omens are
rejected on the indexed route because the class equivalence proof excludes them;
the existing route remains available. No indexed global strategy is computed or
recommended (`strategyVersion: NOT_REQUESTED`, empty `strategies`). On this route,
`COMPLETED` means every supported state/method expansion is enumerated, not that a
final strategy has been solved. Budget exhaustion stays `PAUSED`.

Indexed methods discover the shared rule engine's applicable single, two-action
and preparation/cycle templates. Their full observation maps remain exact
fractions. Only method-boundary classes enter the public graph; primitive classes
stay inside the job-local index. The class witness is display-only. Shared kernel
assumptions are collected from every requested primitive pool, including internal
method states. The current indexed memory allowance pauses when the registered
class count exceeds the retained allowance; the separately bounded kernel LRU
may discard derived work without dropping probability mass.

`GET .../{id}/class-outcomes?revision=...&methodId=...&cycles=...` returns at most
48 explicit `(kind, stateId, probability)` outcomes and a revision-scoped cursor.
These probabilities are class marginals for a specified source class.
`POST .../{id}/entries` binds one positive known outcome to a named parent entry:
`{version:2,requestId,revision,parentEntryId,methodId,cycles,kind,targetStateId}`.
The returned opaque entry ID retains the original conditional operator chain.
Repeated identical bindings reuse the entry; changed bindings with the same ID
are rejected. Wrong source classes/jobs and unresolved outcomes cannot manufacture
a concrete law. The ordinary distribution endpoint rejects indexed classes with
`CONDITIONAL_DISTRIBUTION_REQUIRED` instead of returning a witness.

`POST .../{id}/entries/{entryId}/advance` takes
`{version:2,expectedGeneration,maxSteps,retainedStateLimit}`. Work is bounded by
4096 steps and a 20 ms cooperative slice. The monotonically increased state
allowance applies across this job's entry expansions, not separately per entry;
at most 64 non-root entries are retained. Exceeding it pauses transport without
renormalizing or publishing a partial law. A generation mismatch or cancelled job
rejects advancement. Resuming the search preserves the saved operators and their
transport cursors. `GET .../{id}/entries/{entryId}?offset=...` reads at most 48
concrete rows only after complete transport; each page probability is normalized
within the selected entry, and `conditionalProbability` is relative to its named
parent. Pending work is distinct from unresolved probability. Root/entry access is
owner-scoped and expires with its job. No new server or global recommendation is
part of this integration.

Indexed cancellation also covers completed enumeration, because concrete entry
transport may still be pending. Resume preserves that transport even when no graph
expansion remains. The indexed create wrapper reads its search snapshot and root
entry under the same job lock, so a concurrent cancellation cannot pair different
generations in one response.

Frontend revalidation used the unchanged source. The first default-parallel run
spawned 92 workers and failed 14 of 2,152 tests with timeouts or asynchronous UI
readiness errors. With only `--maxWorkers=2` changed, all 2,152 tests passed; test
assertions and timeouts were unchanged. Lint, typecheck, formatting, all six-locale
coverage and production build passed. The existing large-bundle build warning
remains. This is component/contract verification, not a live browser/server test.

Final API assembly validation: Docker `spotlessApply check generateJooq bootJar`
passed 697 unit/contract tests and 7 integration tests in 6m04s. This run includes
the retained observation changes, indexed API, completed-enumeration cancellation
and coherent create wrapper. The real low-level Solar test uses the complete
current catalog with item-level eligibility, not fabricated weights: recombining
all returned conditional laws equals the raw Exalted kernel exactly, and there
are more concrete outcomes than classes. The same test verifies that incomplete
exploration remains paused and emits no global strategy.

The user subsequently approved certified arithmetic intervals on 2026-10-13.
That supersedes the earlier pending decision; this API checkpoint still uses
exact fractions. Interval arithmetic, strict display/ranking certification and
real Solar performance measurements form the following implementation unit.

## Certified arithmetic foundation

The exact fraction oracle is retained. `CertifiedInterval` represents a
nonnegative quantity as `[L/2^b, U/2^b]`, using `BigInteger` throughout. Converting
an exact `N/D` uses `floor(N*2^b/D)` and `ceil(N*2^b/D)`. Addition sums endpoint
integers exactly. Multiplication uses `floor(L1*L2/2^b)` and
`ceil(U1*U2/2^b)`. Every rounding operation is outward; neither floating-point
estimates nor statistical confidence intervals are used. Intersecting with [0,1]
is allowed only for quantities already known independently to be probabilities.
Expected currency counts are not clipped to [0,1].

The domain precision policy accepts 16–256 fractional bits and 0–12 percent
places; the initial default is 64 bits and two percent places. Different binary
precisions cannot be mixed in arithmetic. `confirmedPercent` returns a single
rounded number only when both endpoints produce exactly the same HALF_UP decimal
percentage using integer/decimal arithmetic. An interval straddling a rounding
tie remains unconfirmed even when very narrow. This foundation is not yet wired
to the UI or a bounded-search request contract.

`CertifiedMethodRunner` propagates enclosing Markov laws, checks first hit after
every primitive, and separates preparation usage, cycle usage, active mass and
all five exit kinds. Every completed boundary must enclose total mass one.
Unresolved model mass remains an explicit exit, not an arithmetic error term.
`IndexedCertifiedMethodRunner` shares the unchanged exact primitive kernel cache;
method arithmetic is scoped to its precision policy. Exact kernel support is not
dropped when a positive probability rounds down to a zero lower bound.

Local ranking distinguishes `PENDING_ENUMERATION`, `PENDING_MODEL` and
`PENDING_ARITHMETIC`. A strict winner needs its lower bound above every other
potential upper bound; exact proven ties may also be returned. Missing direct
observations cannot be treated as zero. Recovery remains a separately classified
loss/checkpoint transition; this arithmetic layer does not add it to success or
substitute fixed-path products for a strategy.

Targeted Docker tests passed for integer enclosure, 500-cycle first-hit and cost
laws, preparation, unavailable actions, explicit unresolved mass, rounding ties,
precision mismatch and ranking ambiguity. Five precisions (16/32/64/128/256 bits)
were compared with exact fraction arithmetic. A real level-one Solar catalog
case derives its one-cycle Cold Resistance >=7 chance from the full eligible
Exalted pool, then checks Exalted/Annulment 100/300/500 observations and currency
counts against the exact renewal law at 16 and 64 bits. The return-to-source
condition is checked, not assumed. These are numerical containment checks under
the declared catalog/rule/roll model, not verification of game-data completeness.

## Certified graph and conditional transport

The certified graph engine preserves the same discovered single-action, paired
cycle and preparation/cycle alternatives as the exact indexed engine. A complete
small-map comparison checks every 100/300/500 exit and active probability against
the exact engine, including method depth, state joins and all alternatives.
Priority-based frontier discovery avoids rescanning every known class on each
primitive step; adding an edge only propagates depth changes when needed.

Certified observations retain conditional operators, not representative-item
substitutes. Requested concrete transport still uses exact fractions, verifies
that the actual selected event mass lies inside its recorded enclosure, and
normalizes by that exact selected mass. Bounds are never used as normalization
constants. Chained bounded operators are checked against independent concrete
laws; the original exact API remains compatible. The graph/transport targeted
Docker run passed eight tests. The bounded graph is not yet exposed through an
API or the UI; final adaptive recommendations remain outstanding.

A real blank Rare level-82 Solar Exalted/Chaos method with Projectile +3 and
explicit Spirit >=47 checkpoints completed absorption at cycle seven in 64,258
ms using 64-bit certified arithmetic and a 512 MiB heap. It visited 108,999
classes, calculated 153,171 kernels (2,000 cached), and retained 73,318 exit
classes. Sampled heap peak was 508,559,360 bytes; post-GC heap 137,025,552 bytes.
The earlier exact run of this method took 326,202 ms. These observations are not
an isolated benchmark comparison and cover one method only, not the entire root
method set or the whole graph. Full-search measurements remain required.

## Whole-search certified diagnostic

The first whole-search diagnostic used the real blank Rare level-82 Solar,
Projectile +3 and explicit Spirit >=47, 64 fractional bits, a 1 GiB test JVM,
a 180-second wall-time guard and a two-million retained-entry allowance. It
stopped at the memory allowance after 113,564 ms, with 41,567,948 cooperative
operations. Engine construction/first root view took 5 ms (not HTTP latency).
There were 78,393 calculation classes, 13,763 public state classes and 133
methods. The root had 42 methods, of which 18 were complete. No root direct
completion, first local recommendation or whole-search completion was reached.
Sampled JVM heap peak was 669,866,288 bytes and post-GC heap 360,177,656 bytes.

This is a partial search, not a currency limit or completed support map. The
retained-entry total at pause was 2,000,011; one cooperative operation can cross
the allowance before the next check. Retained entries are an accounting measure,
not a hard byte cap. The precision change alone does not solve concurrent method
law retention or the number of reachable boundary classes. These remain
scalability work. The diagnostic used an allowance larger than the initial
certified API allowance and its current maximum; it is not an API benchmark.

## Certified API contract

The additive `/api/v2/crafting/method-searches/certified` creation route accepts
`{version:1, request:<existing v2 create>, precision:{fractionalBits,percentDecimals}}`.
The exact and deferred routes remain unchanged. Precision is immutable for a job;
changing it requires a new request ID. Request IDs cannot silently switch engine
kind. Certified jobs use the existing single worker, owner scope, generation
checks, four retained revisions, twenty-minute lifetime and bounded capacity.

GET `certified/{id}` returns coherent status. GET `certified/{id}/graph` pages
states and methods independently using revision plus state/method offsets.
GET `certified/{id}/outcomes` pages a method's disjoint class exits at the specified
revision and 100/300/500 cycles. POST `certified/{id}/control` accepts the existing
v2 CANCEL/RESUME mutation. `entries`, `entries/{entryId}` and
`entries/{entryId}/advance` bind, read and cooperatively materialize the actual
conditional item law using the existing indexed bind/advance request contracts.
Expired revision pages are rejected rather than mixed with current results.

Probability wire values carry exact rational endpoint strings, arithmetic width,
an exactness flag and nullable `confirmedPercent`. Expected currency quantities
use the same endpoint representation without a percent label or clipping at one.
Model-unresolved exit mass and incomplete method discovery are separate from
arithmetic width. Local recommendations are certified only when the requested
horizon is available for every discovered direct method and discovery is closed;
overlapping candidates remain pending. The root's local status appears in the
snapshot, and each state page includes its own local results.

The snapshot reports `strategyStatus:NOT_REQUESTED`: completed enumeration does
not claim a completed adaptive strategy. This route is not yet selected by the
production frontend. Its initial retained-entry allowance is 100,000; RESUME can
raise it monotonically to 1,000,000. Conditional transport has a separate shared
entry allowance. Neither allowance caps crafting currency use. API integration
does not remove the whole-search scalability limitation reported above.

### Remaining issues and recommended continuation

- Whole-map retention: keep partial/pause semantics and reduce simultaneous dense
  method-law retention before increasing the production allowance. Slice boundary
  publication as well as primitive transitions; measure API publication cost
  separately from the bare engine diagnostic.
- Certified frontend: add the typed client and shared FE/BE rounding fixture,
  preserve all six locales and existing start/goal controls, and show a single
  percentage only when endpoint rounding agrees. A rounded 100 percent with
  nonzero arithmetic width must not be presented as a certainty proof.
- Adaptive strategy: retain the exact strategy oracle. Add a certified solver
  with explicit cyclic/fixed-point uncertainty and policy verification before
  publishing final recommendations. Method completion or a local winner is not
  final-strategy completion. Recovery remains a conditional next decision.
- Integration evidence: the current HTTP checks use MockMvc; they do not claim
  live-browser E2E. Recheck the final assembled UI with the actual bounded routes
  when frontend integration is complete.

These are implementation work items, not requests for further user permission.

Final assembled backend verification for this certified API unit passed in
Docker: `spotlessApply`, then a frozen-source `check generateJooq bootJar` in
6m02s. Reports contain 705 unit/contract tests and seven integration tests, with
zero failures, errors or skips. This includes cross-engine request-ID isolation,
precision mismatch, real-catalog conditional reconstruction, stale generations,
revision expiry and completed-enumeration entry cancellation/resume. Temporary
verification containers exited; unrelated Redis services were preserved.
Frontend files were unchanged in this unit; the earlier 2,152-test worker-limited
pass and build remain the latest frontend evidence, not validation of a certified
UI connection. No application server, master merge or deployment was performed.
