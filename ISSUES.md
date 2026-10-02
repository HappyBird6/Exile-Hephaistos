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

## WB-007 — DONE — Coupled roll evidence binding

- Scope: Workbench API response validation and assumption display; no new game/base support.
- Found: a model label alone did not identify the sampled shared ratio or bind its target modifier. Two independently chosen in-range coordinates could pass generic range validation despite the declared coupled model.
- Conservative overnight choice (user authority 2026-10-02): require structured ratioTick and exactly one target ID in the coupled assumption candidates field; its n=10001 counts ratio ticks, not the single bound modifier ID or distinct rounded tuples. Reject missing, duplicate, wrong-ID, out-of-range/noninteger tick and any computed value mismatch; preserve the input and do not record a craft on rejection. Other assumption types retain their previous field meanings.
- Implementation: [API record](backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java), [model evidence](backend/src/main/java/com/poe2craft/crafting/domain/CoupledStatRollModel.java), [exact BigInt client verification](frontend/src/features/crafting/workbenchApi.ts), [malformed response tests](frontend/src/features/crafting/CoupledRollContract.test.ts). HALF_UP is recomputed exactly including negative ties; no floating-point approximation.
- User impact: valid conjecture responses show Sampled ratio tick/10000; unsupported multi-stat responses fail safely. Existing Solar single-stat routes and old localStorage films remain valid; legacy assumptions may omit ratioTick or use null.
- Alternative/tomorrow review: a separate modifierId field could replace target-ID binding in candidates when a new base API schema is finalised. Sampling resolution/distribution remain WB-001 conjectures. No active catalog opts in before complete source validation.
- Rollback: revert this isolated local checkpoint, or disable coupled opt-in and retain single-stat support; no DB migration/history schema/reset required. Do not relax validation while claiming the coupled model remains verified.
- Done when: Docker BE/FE full regression, invalid response/no-history tests and actual browser model display/forged-tick rejection pass. Synthetic test-only stats are clearly marked and never written to game catalogs.

## Overnight decision policy (2026-10-02)

User authorised continued work overnight: record decisions as issues, prefer independent work, and temporarily choose a reversible conservative recommendation when blocked on a product/implementation decision. Record assumption/scope/impact/location/alternatives/tomorrow review/rollback in the relevant stable issue. This authorises modelling choices, not fabricated stat identities/sources, permanent deletion, DB resets, new authentication/permissions/security/terms changes or remote push/merge/deploy. Current recommendation for WB-002/WB-006 remains preserve complete pools and leave gloves inactive until six actual details are sourced. User-visible conjectures stay distinct from verified game facts.
### WB-007 verification (2026-10-02)

Docker BE full spotless/check/integration/generateJooq/bootJar passed 183 (177 unit + six integration), zero failures/errors. Docker FE lint/typecheck/format/full 102 tests/build passed. Latest immutable QA JAR is poe2craft-9ed0d92fbe65.jar. General actual Chromium Workbench passed 30 checks, zero errors; structured model contract browser results are recorded separately in coupled-evidence-ui-browser-results.json. One QA-only locator matched both wrapper and article and was corrected to the item article before rerunning; product code did not change. Full dedicated Essence browser suites were not rerun, while their FE/BE regressions were included. Additional source lookup: ten new ReducedRequirements/LocalReducedRequirements public candidate URLs all returned empty payloads; no detail adopted or same failed lookup loop. Six missing exact glove details remain WB-002. Evidence stays under codex/qa-20261002, including coupled-evidence-backend-artifacts/test-results.

## WB-008 — DONE — Craft assumption evidence disappears after reload

- Scope: Workbench linear localStorage films, especially WB-001 modeled numeric rolls.
- Found: current Frame saves only state/action; the Last craft assumptions panel uses in-memory mutation data. Reload keeps actual values/history but loses the specific sampled ratio and assumption evidence needed for tomorrow's review.
- Impact: future modeled steps cannot be audited from the persisted film; this is independent of missing glove source details.
- Location: [Frame/recordCraft](frontend/src/features/crafting/workbenchHistory.ts), [assumption panel](frontend/src/features/crafting/CraftingPage.tsx).
- Recommended reversible overnight choice: add optional per-frame craft provenance (rule/ledger/model evidence), keep existing version-1 films compatible and preserve capacity/failure behavior. Validate stored evidence before display; no data overwrite/migration reset or branching tree. Alternative: document ephemeral evidence only, or wait for future per-user DB (login remains out of scope).
- Tomorrow review/rollback: assess extra storage and desired amount of retained evidence; omit the optional provenance writer/reader to return to legacy films without deleting them.
- Done when: verified evidence persists/restores after reload and past-step navigation; corrupted/oversized evidence never makes false claims or overwrites saved data; old films, branching-by-new-session semantics and quota failures regressions pass in Docker FE and actual browser.
### WB-008 implementation decision (2026-10-02)

The reversible recommendation is implemented as optional Frame.evidence, containing original snapshot/action/rule/ledger/events/assumptions/consumed omens. Version 1, storage key, film/session semantics and capacity limit remain unchanged. New valid API results are cloned into their own frame; legacy frames are not backfilled. Compatible catalog identity upgrades preserve original evidence provenance. Render only evidence agreeing with the current catalog/item and coupled arithmetic; malformed optional evidence warns and is hidden without discarding valid frames or rewriting loaded bytes. This is browser-local recorded evidence, not authenticated game proof. No signature/authentication/server storage platform was added. Returning to a prior root suppresses later-step assumptions; each crafted step displays its own evidence. Rollback can stop writing/displaying the optional field without deleting existing films. Extra capacity usage still uses the existing safe quota/limit behavior.

The new render path exposed existing React purity/ref lint violations. Film IDs remain generated only by event actions through a module helper; pending UI state is now keyed to base revision and rendering reads state, while the existing request ref still prevents duplicate actions and aborts stale requests. Existing late-result/base replacement tests cover this correction. Docker full FE lint/typecheck/format/106 tests/build passed before browser validation. No backend rules changed, so BE 183 remains the previous checkpoint's successful verification, not a new BE run.
### WB-008 verification and remaining Workbench review (2026-10-02)

Latest Docker FE lint/typecheck/format/full 106 tests/build passed. Actual latest QA Chromium checks passed: modeled evidence reload/navigation 9, invalid evidence/legacy films 6, general Workbench 30, explicitly compatible snapshot migration/future preservation 11 (56 total, zero page errors). New tests preserve recorded evidence clones, original source identity across compatible upgrades, old stored bytes and valid frames despite invalid evidence. Existing quota/size, duplicate request, late result/base replacement, archive/fork and narrow screen regressions passed. Backend source/rules were unchanged; BE full 183 from 1e5aa54 was not rerun. Evidence: codex/qa-20261002/history-evidence-validation.json and history-evidence-*-browser-results.json; workbench-browser-results.json. Only the FE QA image was updated in this stage; no DB/auth/platform/remote changes.

No additional fully sourced ready Solar material or reproducible Workbench UX regression was found after these checks. Remaining work is accurately data/decision gated (WB-001 assumed model, WB-002 six exact details, WB-003 precision/display, WB-004 catalysts, WB-005 special-state rules, WB-006 full glove activation). Do not repeat failed glove lookups or fill overnight time with unrelated edits. Preserve this checkpoint for the parent/user review; project completion is not claimed.

Tomorrow's concise decision list: review WB-001 common-ratio conjecture, tick resolution/distribution and rounding; review WB-007 target-ID binding versus a future separate modifierId field; review WB-008 optional provenance amount/storage tradeoff. Conservative recommendations remain source-unit fallbacks for unverified display conversion and no glove activation/weight renormalisation until missing details are sourced. Vaal/Hinekora remain scope exclusions, not outstanding bugs. LocalStorage films remain version 1 and no login/per-user DB implementation was introduced.