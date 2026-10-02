# Project issues

Single issue list from 2026-10-02, explicitly requested by the user. Record new bugs, uncertain rules and verification tasks here; audit documents retain supporting evidence, not competing issue lists. IDs are stable. OPEN = unresolved; ASSUMED = authorised conjecture requiring game verification; BLOCKED = missing required data; DONE requires linked verification. Vaal Orb/Hinekora are user scope exclusions, not bugs.

## WB-001 — ASSUMED — Coupled numeric rolls

- Scope: source-proven multi-stat Workbench modifiers; glove runtime remains inactive.
- User approval 2026-10-02: “독립적이지는 않고 비율에 맞게 나올거야. 소수점은 반올림하고 부정확한 판단이라고 이슈리스트에 추가해”. Shared ratio and rounding are the user's unverified conjecture, not an official rule.
- Model: one tick sampled uniformly from 0..10000 inclusive, ratio=tick/10000; each stat=min+(max-min)*ratio; round integer HALF_UP (negative -1.5 -> -2, positive 1.5 -> 2). Resolution and tick distribution are explicit implementation assumptions. Preserve each source endpoint; fixed stats remain fixed. Exact decimal arithmetic avoids floating point/overflow changes. Rounded tuples may repeat and are not uniform; never use an independent Cartesian product.
- Impact: permits opt-in modelling only for identified multi-stat ranges; API assumption ID user-coupled-ratio-half-up-v1 and UI distinguish conjecture. Existing 1/N agreement applies to established eligible outcome sets; the model tick domain is separately assumed, not a verified game set. Modifier selection weights are separate. Default Solar never opts in; no new base activation or stored-history schema change.
- Code/test: [model](backend/src/main/java/com/poe2craft/crafting/domain/CoupledStatRollModel.java), [simulator opt-in](backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java), [boundary tests](backend/src/test/java/com/poe2craft/crafting/CoupledStatRollModelTest.java), [assumption UI](frontend/src/features/crafting/CraftingPage.tsx).
- Evidence: [stat examples and 34 two-variable / 8 one-variable distinction](docs/workbench-stocky-mitts-preparation-2026-10-02.md). PoE2DB endpoints prove marginal stat bounds, not joint outcomes.
- Done when: current PoE2 primary evidence or reproducible measured observations establish correlation, eligible tuples, resolution/distribution and rounding; replace/confirm the conjecture and regression-test observed boundary cases.

## WB-002 — BLOCKED — Six glove modifier details

- Scope: Stocky Mitts full 182-row ordinary catalog. Missing exact internal stat details: Encased and five LocalAttributeRequirements tiers (Worthy/Apt/Talented/Skilled/Proficient).
- Impact: keep new glove runtime/catalog selection inactive; do not invent IDs, remove rows or renormalise weights.
- Evidence: [coverage and alternate-source audit](docs/workbench-stocky-mitts-preparation-2026-10-02.md); codex/qa-20261002/stocky-mitts-normal-coverage.json (176/182 union; 110 fresh /82 retained comparison overlap).
- Done when: all six exact source identities/ranges/locality/applicability match captured rows and full checksum/count/weight validation passes.

## WB-003 — OPEN — Source precision and display units

- Scope: new glove source numeric domains and existing source-unit fallback display; especially Leech permyriad and regeneration conversion.
- Impact: Leech scale /100 to percent is proven by endpoints, but step 1 vs step 10 and game display precision/rounding remain unverified. A displayed 6-6.9% range alone does not prove 91 vs ten permitted values. Keep explicit source units where display conversion is unproven; no silent exact game-unit claims.
- Evidence: [precision examples](docs/workbench-stocky-mitts-preparation-2026-10-02.md), [mapper](backend/src/main/java/com/poe2craft/item/SolarTextMapper.java), [UI mapping](frontend/src/features/crafting/workbenchApi.ts).
- Done when: source step/scale/display rounding are documented per stat, conversion and Alt ranges tested, and assumptions or verified status stated accurately.

## WB-004 — OPEN — Catalyst amount, scaling and weighting

- Scope: jewellery quality, catalyst types/refined variants and Catalysing Exaltation.
- Impact: rarity-dependent increment, type replacement/cap, stat precision/rounding, tag applicability and selection-weight transform/order are unresolved; no catalyst activation.
- Evidence: [existing rule audit](docs/workbench-rule-audit-2026-10-02.md), [per-item triage](docs/workbench-remaining-triage-2026-10-02.md).
- Done when: current source confirms increments/state transitions and every affected stat/tag/weight rule; verify quality history and ordinary crafting/omen interactions without mixing incomplete weights.

## WB-005 — OPEN — Other Solar special-state rules

- Scope: Homogenising, Sanctification, desecration/reveal, instilling, Chance, Mirror, identification and other special-state means.
- Impact: current ordinary Solar state does not represent all required state/results; availability/type-selection/roll/replacement evidence remains incomplete. Registration does not imply implementation or obtainability.
- Evidence: [per-item reasons and sources](docs/workbench-remaining-triage-2026-10-02.md). Four encounter omens are outside equipment scope; Vaal/Hinekora alone are user exclusions.
- Done when: each item has current effect/applicability/state/result evidence and complete eligible pools, then independently implement and verify. Split this issue into stable child IDs when an item begins implementation.

## WB-006 — BLOCKED — Glove activation regression gate

- Scope: new Workbench-only base selection/ordinary crafting/basic Enhancement and Greater Battle; no Support/Explorer feature expansion.
- Impact: depends on WB-002 plus accurate per-stat verified/assumed classification. Base Armour is a property, not implicit. Final local Armour rounding, quality/socket effects need separate proof before showing computed totals.
- Evidence: [staged plan](docs/workbench-stocky-mitts-preparation-2026-10-02.md).
- Done when: full catalog integrity, action dispatch and slot/level/family rules pass; Docker BE/FE full checks, actual browser repeat/cancel/error/narrow layout and localStorage compatibility verified; UI/API expose numeric assumptions separately from published selection weights.

## Latest verification (2026-10-02)

Docker backend spotless/check/integration/generateJooq/bootJar passed 183 tests (177 unit, six integration), zero failures/errors. Coupled model tests cover shared midpoint, source endpoints, fixed stats, negative/positive half ties, extreme long ranges, non-Cartesian/nonuniform rounded tuples and guarded opt-in/default Solar. An initial test lambda compilation error was corrected before the successful full run. Docker FE lint/typecheck/format/full 100 tests/build passed; the final ratio-range wording refinement additionally passed typecheck/format and nine related UI tests and image build. Updated QA immutable JAR poe2craft-fcc46c2752e0.jar passed actual Chromium general Workbench 30 checks; the latest FE model-label/localStorage contract fixture passed three checks, zero page errors. Fixture injection verifies UI handling only, not active glove API behavior. Dedicated Abyss/Runic/Breach browser suites were not rerun in this model-only stage; their prior evidence remains prior. No incomplete glove runtime, new stored-history schema, original DB mutation or remote push/merge/deploy. Evidence: codex/qa-20261002/coupled-model-validation.json, coupled-backend-artifacts/test-results, coupled-model-ui-browser-results.json and workbench-browser-results.json.
