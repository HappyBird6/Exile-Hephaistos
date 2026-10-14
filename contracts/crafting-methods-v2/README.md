# Crafting method contract v2

Wire records are defined in `MethodSearchProtocol.java`. The v2 route is
`/api/v2/crafting/method-searches`; v1 remains unchanged.

- Create: `version: 2`, an idempotent `clientRequestId`, full `start`, the original
  `goal` AST, and explicit `activeOmens`.
- Cancel/resume: `version: 2`, `commandId`, `expectedRevision`, `operation`, and
  nullable `retainedOutcomeLimit`. IDs cannot be reused with different content.
- Graph/conditional distribution reads pin `revision` and an opaque scoped cursor.
  Expired revisions return 410 rather than mixing old and new pages.
- Every observation uses **100/300/500 method cycles**. `initialUses` and
  `cycleUses` are expected currency counts, not probabilities and not cycle counts.
- An arrow's exits plus unresolved mass sum to one. Different arrows are choices;
  their probabilities must never be added. Exact full-state source IDs and the
  distribution endpoint preserve entry-conditioned states behind display buckets.
- Final strategies are indexed by exact `stateId` and observation count. They may
  have different subsequent choices for the A and B outcomes. Bounds are not
  reported as exact probabilities. Partial enumeration never reports completion.

`synthetic-diamond.json` is a shared graph/presentation fixture, not a complete
production snapshot schema or a claim about game probabilities. Its item cards
are placeholders; its state labels are symbolic oracle states. It deliberately
contains an immediate 1/2 direct method and a final-success-1 branching strategy.
The Java independent policy enumeration and frontend graph/DOM tests consume it.
Production Solar modifier eligibility and numeric goals are tested separately in
`SolarCheckpointGoalTest`; source weights are not replaced by this fixture.

See `docs/crafting-bucket-method-v2.md` for implementation boundaries and open
acceptance issues. In particular, primitive coverage is not all game currencies,
and a finite template grammar does not establish global optimality over arbitrary
crafting programs.


## Certified display map

`certified-diamond.json` carries the same synthetic decision problem through
`CertifiedMapProtocol` (`certified-bucket-map-v1`). Bounds have exact rational
lower/upper endpoints and an arithmetic width. Production values may be intervals;
all exits and unresolved mass must jointly enclose one, rather than treating lower
bounds as a normalized distribution. Currency-use bounds may exceed one.

The `/certified/{id}/view` read returns a header and first map page under one job
lock, with the same revision. `/map?revision=...&offset=...` retains every alternative
method in pages of at most twelve. `/map-outcomes` supplies class representatives,
roll/tier/orbit metadata and their probability bounds. These are class marginals;
full entry-conditioned item distributions remain available through the certified
entry binding/expansion endpoints and are not replaced by display grouping.

The display response omits the large audit ledger; the existing certified snapshot
endpoint retains it. A grouped node never merges the calculation state identities.
The shared fixture includes a final recommendation for presentation/oracle checks.
Production final strategy scheduling now runs for completed maps within the finite
adapter preparation budget; larger maps pause explicitly. Raw certified wire v7
reports strategy version, status, bounds and uncertainty causes separately from
enumeration. See `docs/crafting-strategy-job.md` for the exact boundary.
