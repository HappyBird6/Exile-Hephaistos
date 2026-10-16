# Approved automatic crafting policy implementation

## Contract foundation (2026-10-16)

The approved roster is preserved in [the source snapshot](crafting-currency-policy-approved-20261015.md). The runtime document is `backend/src/main/resources/crafting/automatic-currency-policy-v1.json`, version `approved-187-2026-10-15-v1`. Its fixed 187 numbers cover 160 Workbench currencies, 18 omens and 9 supplemental materials: 33 USE, 14 CONDITIONAL, 114 EXCLUDED, 26 DEFERRED. Policy permission is independent of game eligibility and numeric-model support.

`AutomaticCurrencyPolicy` checks the temporary-removal purpose, paired Regal/Exalted omen and approved item families. It retains both existing registry spellings `helmet` and `helmets`. The four-explicit exploration boundary is an item-state boundary, not an action or currency-use limit.

`AutomaticCraftingAction` keeps currency, consumed omens and purpose separate from item identity. Its candidate inventory represents all 47 approved/conditional materials, including explicitly unsupported Abyss crafting. `WorkbenchSimulator.automaticPlan` reads the same reviewed eligible branches as the manual simulator without changing manual application. It retains all eligible removal branches and fixed modifier outcomes. `AutomaticCraftingKernel` enumerates scalar rolls with exact rational mass, keeps ordinary eligible modifier-tier weights, and declares uniform fixed-material selection separately from ordinary spawn weights. Unsupported joint rolls remain unresolved, even if the manual sampler has an unverified coupled-roll model.

Every primitive result conserves outcome + boundary + unresolved mass = 1. A fifth explicit receives boundary mass without renormalizing allowed outcomes. Replacement at four explicits remains possible. An outcome budget leaves unresolved mass; it is not a failed outcome or a complete search.

## Verification of this foundation

Docker Java 21, two CPUs and 3 GiB container limit:

- Focused policy, kernel, ruleset-seal, class-storage, graph-storage and restart tests passed.
- `check generateJooq bootJar`: 763 unit/contract tests and 7 integration tests passed, none skipped.
- The initial full run failed on 25 pinned ruleset-identity checks and one expected resource-count check after adding the sealed policy document. The source fixture identity and resource count were updated; item values, goal AST and probability expectations were unchanged. The complete rerun passed.
- The kernel oracle checks weighted non-goal candidates, remaining mass under an outcome budget, uniform removal, prefix-only Crystallisation removal, fifth-explicit boundary mass, and four-explicit replacement. Candidate registration covers every approved material; it does not claim every material/base combination has a verified numeric law.

The policy is included in the resource seal, so the reviewed bundle identity changes. Its policy version remains distinct from `automatic-scalar-kernel-v1` and the existing game-rule and numeric-assumption versions.

## Integration still in progress

This foundation is not yet the production certified graph's action provider. That graph still uses `WorkbenchCurrency` without a consumed-omen execution identity. Merely replacing its 16-action list would omit supported material families and misrepresent completion. The following work remains:

- Connect compound execution identity, method phases and material consumption to discovery, conditional transport, cache keys and durable cursor restore.
- Apply the four-explicit boundary to graph expansion and include its unresolved mass in strategy bounds.
- Preserve partial/unsupported discovery instead of treating the inventory as fully searched.
- Integrate the Breach quality-40 abstract checkpoint, warning and post-checkpoint objective handling; no individual catalyst is authorized.
- Verify Life/Chaos resistance and Solar diamond examples under the newly approved action policy; earlier examples must not override new exclusions.
- Production graph integration must retain the existing API generation, cancellation and durable-storage behavior. The policy modal and selected-node editor are locally verified; see [UI verification](verification/automatic-policy-ui-20261016/README.md).

Regal/Exalted cannot be standalone random goal-acquisition templates. Preparation templates require a subsequent replacement. Success immediately after temporary preparation is awaiting the user's clarification; it must not be finalized as either success or failure in the meantime. The in-progress execution layer retains this case as unresolved and can represent either eventual choice explicitly. Preparation alone is not published as a completed intermediate method checkpoint.

No claim is made that production exploration already enforces this policy, that Abyss/jewel crafting is implemented, or that a complete policy search/performance run has passed. No remote push, PR, master integration or deployment is part of this local verification stage.

## Method execution and discovery increment

`AutomaticMethodExecution` carries currency, consumed omens, temporary-preparation purpose and phase independently of full item identity. It reuses certified interval Markov transport, reports initial and cycle material consumption separately, and retains 100/300/500-cycle observations. Its JSON cursor includes the canonical objective identity, policy/catalog/rule/definition/target configuration, precision and pending preparation decision; model or objective changes require explicit recalculation.

Primitive enumeration exhaustion pauses before removing pending mass or charging currency. Increasing the resource budget retries that primitive from its original state; no probability is converted to failure or silently cached as complete. This retry currently recomputes the bounded primitive enumeration rather than resuming an internal scalar-roll cursor. Its materialized kernel and checkpoint rows still require integration with the existing streaming stores before large production searches.

`AutomaticMethodGraph` is an unconnected domain implementation for full-state discovery. It considers all approved candidates and ordered one/two-action method variants, keeps same-node-pair alternatives, queues method transport cooperatively, and passes mutually exclusive outcome laws to the existing cyclic decision-strategy solver. Node/method/retained-state limits pause discovery. Unresolved goals, unsupported laws and the explicit boundary prevent a complete result.

This increment does not yet provide a production API, graph-level durable restore, compressed eligible-state indexing, local recommendation publication, Breach quality work, or the requested real-catalog performance evidence. Its method inventory is the implemented one/two-action template grammar, not a proof of completeness over arbitrary programs. The unconnected layer must not replace the production engine until those integration contracts are implemented and verified.
## Next integration seams

1. Share the immutable, reviewed per-base simulator configuration currently registered by `WorkbenchService` (including fixed/replacement essence targets). Constructing a default simulator for every base loses that registration. Craft Support remains an independent service while sharing these game rules.
2. Extend `BasicTransitionClasses` / `BasicClassIndex` / `IndexedKernelStore` with compound action identities and eligible replacement laws. Preserve the existing deferred conditional operators and streaming working-law stores; do not substitute eager concrete-state enumeration for their production memory behavior.
3. Adapt `CertifiedMethodSearchEngine` discovery and `CertifiedSearchJobs` durable cursors together. Persist execution purpose, consumed omens and the original goal AST under a new explicit version; retain old-version replay or explicit recalculation.
4. Map the new method distributions to the existing local/final recommendation protocol, keeping alternatives separate. Resume after budget exhaustion must retain pending action and newly arriving mass. Add API stale-generation, cancellation, restart and reconnect checks for the new version.
5. Implement the Breach quality-40 checkpoint independently of individual catalysts, then verify the new-policy Life/Chaos and Solar examples. Record first response, first local recommendation, full supported discovery and peak memory separately; unresolved model scope is not complete support.
