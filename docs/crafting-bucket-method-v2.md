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

The v2 domain components do not yet replace `PathSearchCalculation`, which is
still the v1 six-policy/single-explicit renewal implementation. Do not advertise
v2 completion, numeric Solar diamond support, generalized omen handling, or a
new API repetition unit until the adapter, job lifecycle and UI are connected.
The production adapter must supply reviewed kernels and full goal AST evaluation,
not the unverified coupled roll model. Synthetic oracle tests prove arithmetic
and state handling, not actual game probabilities.

Pending integration: expose the existing shared family/tier index; connect the primitive cursor;
applicability-driven method discovery;
method-depth BFS; per-arrival distributions; local versus final recommendation;
v2 job/generation/revision responses and six-language map consumption. Catalog
completeness, missing-weight fallback and roll assumptions must be independently
reported and versioned when those components are connected.
