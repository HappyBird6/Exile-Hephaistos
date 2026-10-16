# Automatic crafting methods under the approved material policy

Craft Support now uses `/api/v2/crafting/method-searches/automatic`. Workbench remains an independent manual simulator. The older method-search endpoints remain available for saved records.

## Policy and calculation boundary

The approved [187-entry roster](crafting-currency-policy-approved-20261015.md) is sealed in `automatic-currency-policy-v1.json`: 33 USE, 14 CONDITIONAL, 114 EXCLUDED, 26 DEFERRED. The discovery inventory contains 76 execution variants, including consumed-omen combinations and one abstract quality-40 operation. Permission, current-state applicability, and supported probability models are separate decisions. Abyss laws remain unsupported; jewel crafting is outside this integration.

Exploration permits at most four explicit modifiers per item. A fifth explicit contributes boundary/unknown mass; it is never removed from the denominator. Resource limits pause computation and do not cap total currency usage. Method observations use 100, 300 and 500 **cycles**, with preparation and actual currency/omen consumption reported separately.

Regal and Exalted are temporary-affix preparation operations with a subsequent replacement. If preparation itself completes the whole goal, the policy decision remains `PREPARATION_FIRST_HIT_POLICY_UNDECIDED`. That exclusive mass is UNRESOLVED, not success or failure. Its concrete goal item can be displayed without counting it as a successful strategy exit.

## Shared rules and exact roll cells

`WorkbenchService.automaticKernel` supplies the same immutable, reviewed per-base configuration used by the manual rules, including essence target overrides. Candidate selection includes every eligible modifier-tier and all non-goal candidates. Ordinary pools use their declared weight policy. Fixed-material pools explicitly declare uniform individual-candidate selection. Removal denominators include all eligible instances after omen restrictions.

The scalar kernel preserves the selection mass of unverified joint rolls as unresolved while continuing verified scalar branches. It does not use the manual unverified coupled-roll sampler as an exact law. Scalar roll assumptions and their sources remain attached to execution checkpoints.

The existing `RollPartition` is now used by automatic search. It partitions rolls only where the complete goal AST proves intervals safe. Unknown dependencies and coexisting numeric contributors retain individual relevant values. Existing deterministic starting values receive their own cuts. Modifier IDs, tiers, required levels, implicit values, conditions and quality state are not merged. Tier/family orbit compression is deliberately not applied to the new replacement actions.

For supported automatic add/replacement operations, current scalar roll values do not control eligibility or removal. Goal-equivalent scalar intervals therefore have uniform conditional roll laws and identical subsequent transitions. Interval mass is the sum of its individual uniform rolls; a node exposes the interval in `rolls` and the UI renders its range. The stored item is a valid lower-end witness. Editing opens that concrete witness for the user to choose exact values; the new job recompiles partitions from its own start and unchanged goal. The job-scoped kernel identity includes the partition version and full objective; its starting state is retained in the graph checkpoint. Partition kernels are not shared between jobs.

## Method graph and strategy

Item state/roll-cell identity is separate from method template, execution phase, temporary purpose and consumed omens. Method templates contain preparation and one/two-action cycles. First-hit is evaluated after every primitive. Goal, checkpoint, loss, dead, active and unresolved mass remain disjoint and enclose total mass one.

Discovery uses method depth. Within a depth, achieved conditions and a bounded representative probe affect ordering only; they never remove choices or supply probabilities. Candidate order is persisted with scan/pair cursors. At most two primitive laws are opened concurrently. A method admission limit stops new method creation while allowing admitted methods to make progress. Primitive, state and retained-law budgets remain bounded. Shorter late merges relax existing descendant depths.

Partial horizon observations publish absorbed first-hit mass as lower bounds and place all unprocessed mass in unresolved. They are not stored as completed observations. Currency-use bounds include already spent mass and a conservative horizon upper bound.

Alternative arrows are separate decision choices. Only mutually exclusive exits within one choice are summed. The cyclic `CertifiedOutcomeStrategy` computes outcome-dependent strategy bounds, including loss/recovery continuations. Local recommendations require finished direct methods and a lower bound that dominates other immediate upper bounds, including their unresolved mass. Final highlighting is separate and requires completed supported discovery. A fixed path product is not substituted for the adaptive strategy.

## Versioned API and persistence

POST create accepts `{request: {version: 2, clientRequestId, start, goal, activeOmens: []}, abstractQuality40}`. GET returns a revisioned view. POST `/{id}/control` accepts command ID, expected revision/generation, and ADVANCE, RESUME or CANCEL. GET `/{id}/request` preserves the original AST and abstract-quality context for explicit recalculation. The existing ruleset request/response header is required; owner cookies isolate jobs. Conflicts return 409, unavailable ownership returns 404, malformed input returns 422.

Each request advances a bounded slice. Cancellation changes generation; duplicate commands are idempotent and stale responses cannot replace newer UI state. Checkpoints include queued discovery, execution phases, conditional laws, costs, horizons and model identities. The model is shared once per graph document. Atomic publication retains the previous document on failure; storage-budget exhaustion returns PAUSED at that last durable cursor. Unknown model versions require explicit recalculation rather than deleting the original goal.

The graph uses bounded in-memory working laws and local JSON checkpoints. It does not yet reuse the legacy SQLite streaming class/transition stores. This is a material scaling limitation, not proof that all supported states have been explored. The implemented template grammar is also not a completeness proof over arbitrary crafting programs.

## Breach quality work

The approved #99 workflow adds a distinct abstract `WORK:QUALITY_40` checkpoint after the verified maximum-quality modifier exists. It does not choose a catalyst, infer catalyst count or invent numeric quality scaling. Before the work, a branch removing that modifier remains unresolved with `QUALITY_CAP_REMOVAL_BEFORE_40`. After the work, keeping or removing it preserves the completion flag; keeping it does not prevent the goal. The flag survives persistence and selected-node editing.

## Verification and remaining scope

See [integration evidence](verification/automatic-policy-integration-20261016/README.md) for exact tested snapshots, bounded real-catalog measurements, browser results and remaining limitations. Earlier [foundation evidence](verification/automatic-method-foundation-20261016/README.md) describes earlier commits only.