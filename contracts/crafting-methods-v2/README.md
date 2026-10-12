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
