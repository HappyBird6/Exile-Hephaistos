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
Its scope remains the declared single-stat Solar model without omens. In particular,
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

The new `/api/v2/crafting/method-searches` endpoints expose create/read,
revision-pinned graph pages, exact conditional outcome pages, cancel and resume.
They require version 2 and the current ruleset header. `cycles` means method
repetitions; expected initial and cycle currency uses are separate. The old v1
API retains its original primitive-use semantics. Job state lives in process
memory for 20 minutes, with four retained revisions. Restart/expiry requires an
explicit recalculation. Request/command IDs are idempotent, owner cookies scope
access, and generation tokens suppress cancelled workers. A run budget pauses
work; it is never a crafting-currency limit.

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
- **Rules coverage:** the primitive model remains Solar, single-stat integer rolls,
  supported basic currencies and no active omens. Other bases, joint rolls and
  omens remain explicitly unresolved. Extend reviewed rule kernels before widening
  the advertised scope. Whittling must remain a Chaos omen, never a deletion orb.
- **Uniform fallback:** the current primitive adapter still accepts only the
  published-weight policy. The 30 explicit-uniform catalogs and future missing
  weights need a separately tested eligible-modifier-tier selector. No missing
  weight was found in the reviewed 132-catalog audit.
- **Cyclic final strategies:** Bellman bounds are sound but do not always converge
  to an exact rational in finite iterations. Add SCC/policy solution proofs and
  bounded solver cursors; never call a stable decimal an exact optimum.
- **Memory budgets:** retained primitive outcomes are bounded; runner distributions,
  method objects and revision indexes also need a total-memory budget. Resume can
  request a larger retained-outcome limit through the API. Eviction must preserve
  pending cursors and must not promote partial kernels into complete cache entries.
- **Verification remaining:** production first-local/whole-search timing and peak
  memory, real-browser interaction, and broader lifecycle fault injection remain
  distinct acceptance checks. Passing synthetic arithmetic tests does not establish
  those results.

## Verification evidence (2026-10-12)

Docker Java 21 full `check generateJooq bootJar` passed (627 unit/contract tests and seven disposable
DB integration tests). Docker Node 24 lint, typecheck, formatting, 2,146 frontend
tests and production build passed. The preserved v1 source oracle remains 650/21107.
The actual Solar pool test enumerates Exalted outcomes for root, Projectile +3,
explicit Spirit 47 and existing Life 50; both diamond arms/merges and Life+Chaos
are reachable, and the complete primitive distributions conserve mass.

An isolated external JUnit probe used the production catalog with an empty Rare
Solar item and AND(Projectile >=3, explicit Spirit >=47). One 12,000-operation run
measured first response **329ms**, first method visible on the first graph page
**1,011ms**, and sampled process-heap maximum **72,939,176 bytes**. It paused with
`COMPUTE_BUDGET`, 9,952 discovered method instances and three display buckets.
First local recommendation and whole-search completion were **not reached**.
The heap figure is sampled process usage, not a proved peak or isolated job size.
These measurements do not certify production completion performance.

Real-browser review was attempted with the computer-use browser tool, but its
launcher failed before opening a page: required Windows platform directory missing
(HRESULT 0x80070003). The temporary review page/server were removed. DOM tests do
verify all six synthetic methods, eight outcome connections, separate immediate
and final recommendations, and 100/300/500 observations; they do not replace a
real-browser layout/interaction check.
