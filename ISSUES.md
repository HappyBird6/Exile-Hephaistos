# Project issues

Single issue list from 2026-10-02, explicitly requested by the user. Record new bugs, uncertain rules and verification tasks here; audit documents retain supporting evidence, not competing issue lists. IDs are stable. OPEN = unresolved; ASSUMED = authorised conjecture requiring game verification; BLOCKED = missing required data; DONE requires linked verification. Vaal Orb/Hinekora are user scope exclusions, not bugs.

## WB-001 — ASSUMED — Coupled numeric rolls

- Scope: source-proven multi-stat Workbench modifiers; Stocky Mitts Workbench now explicitly opts into the conjecture, while default Solar does not.
- User approval 2026-10-02: “독립적이지는 않고 비율에 맞게 나올거야. 소수점은 반올림하고 부정확한 판단이라고 이슈리스트에 추가해”. Shared ratio and rounding are the user's unverified conjecture, not an official rule.
- Model: one tick sampled uniformly from 0..10000 inclusive, ratio=tick/10000; each stat=min+(max-min)*ratio; round integer HALF_UP (negative -1.5 -> -2, positive 1.5 -> 2). Resolution and tick distribution are explicit implementation assumptions. Preserve each source endpoint; fixed stats remain fixed. Exact decimal arithmetic avoids floating point/overflow changes. Rounded tuples may repeat and are not uniform; never use an independent Cartesian product.
- Impact: permits opt-in modelling only for identified multi-stat ranges; API assumption ID user-coupled-ratio-half-up-v1 and UI distinguish conjecture. Existing 1/N agreement applies to established eligible outcome sets; the model tick domain is separately assumed, not a verified game set. Modifier selection weights are separate. Default Solar never opts in. Stocky activation uses the declared WB-003/WB-006 model boundary; stored films remain version 1.
- Code/test: [model](backend/src/main/java/com/poe2craft/crafting/domain/CoupledStatRollModel.java), [simulator opt-in](backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java), [boundary tests](backend/src/test/java/com/poe2craft/crafting/CoupledStatRollModelTest.java), [assumption UI](frontend/src/features/crafting/CraftingPage.tsx).
- Evidence: [stat examples and 34 two-variable / 8 one-variable distinction](docs/workbench-stocky-mitts-preparation-2026-10-02.md). PoE2DB endpoints prove marginal stat bounds, not joint outcomes.
- Done when: current PoE2 primary evidence or reproducible measured observations establish correlation, eligible tuples, resolution/distribution and rounding; replace/confirm the conjecture and regression-test observed boundary cases.

## WB-002 — DONE — Six glove modifier details

- Scope: Stocky Mitts full 182-row ordinary catalog. All six formerly missing exact details (Encased and Worthy/Apt/Talented/Skilled/Proficient) recovered on 2026-10-03.
- Impact: full sourced catalog now bundled and activated in Workbench under the explicit WB-003/WB-006 model boundary. No invented IDs, removed rows or renormalised weights.
- Evidence: [complete 182-row recovery](docs/workbench-stocky-mitts-catalog-2026-10-03.md), [verified activation](docs/workbench-stocky-mitts-runtime-2026-10-03.md). Previous 176/182 coverage is retained historical evidence, superseded by the complete snapshot.
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

- Scope: Homogenising, Sanctification, desecration/reveal, instilling, Chance, Mirror, identification and other special-state means on the current Solar/Stocky Workbench. Glove-specific special Essence/Alloy targets require separate complete source/result validation; copying Solar targets is unsupported.
- Impact: current ordinary Solar state does not represent all required state/results; availability/type-selection/roll/replacement evidence remains incomplete. Registration does not imply implementation or obtainability.
- Evidence: [per-item reasons and sources](docs/workbench-remaining-triage-2026-10-02.md). Four encounter omens are outside equipment scope; Vaal/Hinekora alone are user exclusions.
- Done when: each item has current effect/applicability/state/result evidence and complete eligible pools, then independently implement and verify. Split this issue into stable child IDs when an item begins implementation.

## WB-006 — DONE — Glove activation regression gate

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

### Final integration handoff (2026-10-02)

Current code HEAD 20851fd added no backend change after the full BE 183 run at 1e5aa54. FE 106 and Chromium 56 remain the current full relevant results. A focused 21-check cross-feature Chromium run additionally passed Alchemy/Fracturing/Hysteria+Crystallisation/Runic Alloy/Blessed and Shift Divine with persisted frame evidence, locks, past crafting, archived futures, reload, Alt and narrow viewport, zero page errors. No failure justified another code change. [Concise overnight handoff](docs/overnight-summary-2026-10-02.md) records baseline, active scope, unverified assumptions, remaining blockers, tomorrow priorities and resume commands. Only four intended QA test-server services remain running; anonymous browser/Node checks self-removed and the successful BE check container is stopped. Original DB/volumes and remote state remain untouched.


### User review completed (2026-10-03)

User selected the recommendation for both reviewed choices (1. 추천대로, 2. 추천대로) and requested continued work. WB-008 retains full optional per-step crafting evidence. WB-001 retains 10001 equally sampled ratio ticks and HALF_UP with explicit unverified-model labelling. This completes the user preference review, not verification of the game's numeric rules: WB-001 remains ASSUMED and WB-008 remains DONE. Missing source details, precision, Catalyst and special-state evidence remain developer investigation tasks; they do not require the user to invent rules or reapprove prior scope exclusions. Current model/speed settings are retained.

### WB-002 complete source recovery (2026-10-03)

All six missing details now match current PoE2DB primary rows through exact public Codes located in pinned public PoB2 data. Full 182-row pool is bundled without pruning: 116 fresh details and 66 labelled retained exact matches, 83/99 prefix/suffix, published weights 63700/84500. [Complete evidence and inactive catalog boundary](docs/workbench-stocky-mitts-catalog-2026-10-03.md). Earlier 176/182 and failed URL observations above are historical. WB-003 precision and WB-006 runtime activation remain unresolved; no unsupported base was exposed.

### Complete glove catalog validation (2026-10-03)

Project Docker spotlessApply/check/integration/generateJooq/bootJar passed: 186 tests (180 unit, six integration), zero failure/error/skipped. Three new catalog tests cover full counts/weights/source rows, signed recovered bounds, checksum rejection and default Solar preservation. XML evidence: codex/qa-20261003/stocky-backend-artifacts/test-results; totals: stocky-backend-validation.json. Runtime activation and FE/browser were not changed or rerun. Prior FE 106 / Chromium 56 plus 21 cross-feature checks remain prior evidence. Original DB/volumes and remote state preserved.

### WB-003 reversible numeric policy and WB-006 activation work (2026-10-03)

Source boundaries and source-unit IDs are verified for all 182 glove rows. Neither the current public PoE2DB endpoint nor the pinned public PoB2 exporter establishes actual game interior increments/distribution. The decimal-rendered leech ranges establish displayed endpoints, not whether the game samples source-unit integers or larger steps. WB-003 remains OPEN for game verification.

Under the user's instruction to proceed with reversible conservative recommendations, Stocky-only single-stat sampling uses assumed-source-integer-roll-v1: uniform raw source-unit integers inclusive of captured bounds. Candidate counts are model counts, not established game outcome counts. All 42 multi-stat definitions opt into the already approved 10001 shared ticks/HALF_UP conjecture, including fixed coordinates; no Cartesian product or pool pruning. Source units are displayed when source text cannot be substituted exactly. Published modifier-selection weights remain independent. Base Armour 15 is a labelled base property; no computed Armour, quality/socket transformation or pasted glove mapping. Impact: numeric values and their modeled probabilities can differ from the real game. Alternative: disable glove simulation pending measured/primary interior-domain evidence. Rollback: remove Stocky service dispatch/selector; existing films stay preserved and never silently migrate into Solar. Review next: replace models and ledger version when the actual domain/rounding is established.

Workbench-only service dispatch, base selector, zero-implicit handling and catalog-aware film restore are being validated. Solar Support/Explorer do not receive the glove catalog. Four sourced essence prefixes are Lesser/regular/Greater Enhancement and Greater Battle; all other glove essences/special means remain unsupported. Effects are source-backed; numeric roll sampling is explicitly assumed. Initial validation exposed a leftover StateBucket implicit=1 restriction, Solar action count expansion, and a new FE mock tuple typing error; these were corrected without dropping coverage or changing expected game behavior.

### WB-009 — DONE — Narrow viewport tooltip intercepts material selection (2026-10-03)

Actual Chromium 420px pointer validation found that selecting an offscreen currency after horizontal stash scrolling reached the main background instead of the button, clearing selection before any API request. Minimal captured event proof: codex/qa-20261003/stocky-narrow-error-probe.json (zero requests, Alchemy aria-pressed false, main click). The fixed tooltip was clamped to the full viewport width at the trigger's vertical coordinate and could overlap its trigger. This is a Workbench usability regression, not a game-rule uncertainty. Correction in MaterialTooltip.tsx places the tooltip beside the trigger when space permits, otherwise above/below it with scrollable constrained height. Links and keyboard descriptions are preserved. Validation is pending actual narrow pointer selection/error/retry plus FE regression; prior Solar 30/special 22 and BE 189 results remain previous-to-this-FE-fix evidence.

### WB-006/WB-009 verified activation checkpoint (2026-10-03)

[Runtime scope, rule/model boundaries and rollback](docs/workbench-stocky-mitts-runtime-2026-10-03.md). WB-006 is DONE for this modelled Workbench-only scope; game numeric verification remains WB-001 ASSUMED / WB-003 OPEN. WB-009 is DONE: identical 420px pointer probe changed from selection false/request zero to selected Alchemy/request one/successful craft, then the complete glove browser suite passed.

Docker full BE 189 (183 unit/six integration), zero failure/error/skipped, spotless/check/generateJooq/bootJar passed. Final Docker FE lint/typecheck/format/110 tests/20 files/build passed including post-unmount placement and wrong-base response rejection. Actual final Chromium: Stocky 60, Solar general 30, Solar special cross-feature 25; all zero page errors. Cross-feature check count includes bounded setup attempts; it is this run's count, not a fixed suite size. Glove checks include no invented implicit, ordinary crafting, Shift/cancel, fracture, no-target Blessed, all four guaranteed Essences, low-level/unsupported gates, source/model evidence, reload, past-step new film, original future preservation, cross-base archive/reload, Alt, narrow layout, actual network failure preservation/retry. Dedicated earlier migration/invalid-evidence suites were not rerun; FE regressions and actual archive/reload were. BE was not rerun after FE-only tooltip correction. QA evidence: codex/qa-20261003/stocky-runtime-backend-validation.json, stocky-runtime-backend-artifacts/test-results, stocky-frontend-tooltip-check.log, stocky-workbench-browser-results.json, workbench-browser-results.json, overnight-cross-feature-browser-results.json, stocky-workbench-narrow.png, stocky-narrow-error-before-tooltip-fix.json and stocky-narrow-error-probe.json.

Actual registry remains 220 registered/incomplete inventory; implemented union 64 = 19 currency +11 omen +33 essence +1 alloy. Solar supports prior 60; Stocky supports 32 (19 currency, nine relevant omen, four essence). This is implementation scope with explicit numeric models, not all-game coverage or obtainability. No new Support/Explorer feature or login/DB schema/storage platform. Original DB/volumes preserved; no remote push/merge/deploy.

Final self-review corrected the Stocky preset text header to Gloves/Stocky Mitts instead of Solar. Full FE 110 and actual Stocky 60 passed after this Stocky-only text correction; the immediately preceding Solar general 30 and special cross-feature 25 were not repeated after that text-only correction (Solar and tooltip behavior unchanged). Final FE log: codex/qa-20261003/stocky-frontend-final-check.log. Original checkout remains clean master e937ddccf7493ea0d114139142ff36224c3a672c.

## WB-010 — BLOCKED — Stocky Infinite Essence eligible outcomes

- Scope: Lesser/ordinary/Greater Essence of the Infinite on the existing strength-glove base; Solar's verified three-target mapping is unchanged.
- Primary source: current PoE2DB Gloves_str lists Strength/Dexterity/Intelligence Codes for each tier. Strength normal spawn tags include str_armour; Dexterity includes gloves; Intelligence includes helmet/int_armour and lacks an applicable Stocky tag.
- Unknown: whether the Essence can force otherwise ineligible Intelligence or filters the result set. Ordinary spawn tags alone are insufficient proof of Essence filtering or forcing. Do not prune a candidate, invent an extra definition or apply 1/2 or 1/3 probabilities.
- Impact: all three glove Infinite actions stay safely unsupported; independent fixed basic Essences proceed.
- Evidence: [basic Essence scope and exact source-backed targets](docs/workbench-stocky-basic-essences-2026-10-03.md); raw candidate rows retained at codex/qa-20261003/stocky-basic-essence-candidates.json.
- Done when: current primary evidence or reproducible game observation establishes the complete eligible outcome set on strength gloves and exact target identity/ranges. Only then use sourced weights or explicitly agreed uniform 1/N if weights alone are missing.
## Latest fixed-Essence verification (2026-10-03, v18)

Stocky basic Essences increase 4 -> 25; full normal pool and explicit numeric model boundary are preserved. Docker BE 190 (184 unit/six integration), FE 111, actual new-Essence browser 86, Stocky regression 63, retained-v17 compatibility 4, latest Solar 30/cross-feature 21 all pass with zero failures/page errors. Registry union remains 64; Stocky support is 53 records/44 actions. [Primary evidence, exact effect/level/slot/target table, execution evidence and remaining triage](docs/workbench-stocky-basic-essences-2026-10-03.md). WB-001/WB-003 remain unverified models, WB-004/WB-005 remain open, WB-010 blocks only the unresolved Infinite outcome set. No remote or original database mutation.

Next bounded candidate: glove Hysteria's current CriticalMultiplier4 matches the already complete of Fury ordinary target; source proof is collected, but v18 has not enabled its replacement override. Current glove perfect_essence has 13 rows/12 materials, including dedicated targets and distinct Alloys. [Fresh item applicability and next-target audit](docs/workbench-stocky-basic-essences-2026-10-03.md).

## WB-011 — DONE — Base-specific Stocky Hysteria replacement

- Scope: Rare Stocky Mitts Hysteria CriticalMultiplier4/of Fury at catalog level 45+, plus individual Crystallisation removal restrictions. Solar life-recoup target is preserved.
- Implementation: validated explicit per-base replacement override; all eligible removal branches must accept the fixed suffix. No invalid branch pruning. Matching omen consumed only on success; conflicts, missing eligible side, insufficient level, rarity, family and slot cases preserve input/omens. Fractured instances and every unaffected roll are preserved.
- Probability boundary: fixed target probability 1; removal 1/N remains explicit uniform-removal assumption. WB-003's five-integer source numeric model remains unverified. No ordinary pool or source weights changed.
- Evidence and completion checks: [current primary sources, detailed scope, Docker/backend/client/browser verification and next bounded candidate](docs/workbench-stocky-hysteria-2026-10-03.md).
- Current unique-material count remains 64; Stocky 56 support records/45 actions (25 basic Essences + Hysteria, 11 omens, 19 currencies). No cross-base sum used as implementation count.
Latest Hysteria verification: Docker BE 195 (189 unit/six integration), FE 114/21 files, actual Hysteria browser 51, Stocky regression 62/basic Essence 86, retained v17/v18 compatibility 4 each, Solar 30/cross-feature 22; all zero failures/page errors. Source collection for all 13 glove special-result rows is complete; next activation candidate is Abyss's two source-proven zero-spawn dedicated targets. [Execution evidence, unsupported scope and next step](docs/workbench-stocky-hysteria-2026-10-03.md).

## WB-012 — DONE — Stocky Abyss special pool and additive snapshot

- Scope: exactly two source-proven zero-spawn prefix/suffix Mark definitions on the existing glove base. Normal 182 definitions/weights unchanged; no downstream desecration/reveal.
- Rules: Rare replacement; every eligible removal and both target branches must fit family/slots. No pruning. Individual Crystallisation affects removal only; matching pairs rejected. Fractured/untouched values and unrelated omens preserved; a sole unlocked Mark can replace itself.
- Probability: explicit separate removal 1/N and unpublished two-outcome choice 1/2 assumptions. Fixed marker value has no numeric range. WB-003 remains unverified for other rolls.
- Compatibility: separately checksum-validated special catalog; only the intact normal snapshot is compatible. Restore/view preserves stored bytes; actual craft may upgrade validated state identities only. Original evidence and future frames preserved; unknown snapshots retained untouched.
- Evidence and verification: [source targets, loader integrity, actual runtime/browser/history and rollback boundary](docs/workbench-stocky-abyss-2026-10-03.md).
- Current runtime: 184 definitions = 182 normal + two zero-spawn; unique implementation 64 unchanged; Stocky 57 support records/46 actions.

Remaining special-target source review: [13 rows / 12 material identities, exact stat/family/locality and independently checked page conditions](docs/workbench-stocky-special-evidence-2026-10-03.md). Source capture is not implementation; Horror's fixed glove/boot effect is the next independent candidate. Root ISSUES.md remains the single issue list.
Latest Abyss verification: Docker BE 201 (195 unit/six integration), FE 117/22 files; actual Abyss browser 50, snapshot/history compatibility 10, Stocky general 59/basic Essence 86/Hysteria 37, latest Solar 30/cross-feature 22; zero failures/page errors. Real API additionally reached and Fractured both zero-spawn Mark outcomes and preserved them through Divine/Chaos/Annul without ordinary-pool contamination. [Execution evidence and non-rerun limits](docs/workbench-stocky-abyss-2026-10-03.md).

## WB-013 — DONE — Stocky Horror fixed Local Augment-effect affix

- Exact current primary target: `EssenceLocalRuneAndSoulCoreEffect1`, level-one suffix, `SoulCore`, fixed Local 60%, source-marked Unscalable. Separate checksum-reviewed zero-spawn extension; all 182 normal definitions and weights preserved.
- Rare replacement validates every eligible removal branch, suffix capacity/family and Fractures. Crystallisation restricts removal only, consumes only matching omen, and rejects the matching pair. Fixed value has no numeric/outcome-choice assumption; eligible removal retains explicit 1/N assumption.
- Affix assignment only. Solar Horror unsupported; socketing and Rune/Soul Core effect calculations remain unsupported and are visibly labeled. This does not close WB-004/WB-005.
- Old normal and Abyss snapshot identities remain compatible. Restore/view preserves stored bytes; real crafting upgrades only validated state identity while original evidence, future steps, inactive films and unknown archives survive.
- Current unique scope: 65 implemented of 220 registered materials; Stocky 58 records/47 actions, Solar 60/49. Docker BE 208 (202 unit/six integration), FE 121/23 files; actual Horror 49, snapshot 11, Stocky 68, Solar 30/cross-feature 21, all zero failures/page errors. API probe additionally exercises actual Horror Fracture and Divine/Chaos/Annul lock preservation.
- [Source meaning, probability/simulation limits, execution evidence and checks not rerun](docs/workbench-stocky-horror-2026-10-03.md).

## WB-014 — DONE — 390px central-use button clipped at initial stash position

- Found by visually reviewing the actual Horror screenshot. Reduced mobile canvas minimum from 660px to 600px while retaining local horizontal scrolling for off-screen inventory/favorites.
- Actual 390px geometry verifies a completely visible central button, material icons at least 44px and no document overflow; final Horror/Solar/Stocky browser regressions pass. Narrower widths were not newly verified. Desktop coordinates remain intact.
- [Visual evidence and scope](docs/workbench-stocky-horror-2026-10-03.md#wb-014-390px-central-item-visibility).

## WB-015 — DONE — Git newline conversion changed checksum-bound Horror raw proof

- Caught during staged-change review before checkpoint: generic LF normalization would change the reviewed raw proof bytes on another checkout and invalidate its SHA-256.
- Added a narrowly scoped `-text` attribute for the checksum-bound Horror raw resource, preserving the already-tested bytes and snapshot identity. Unfiltered worktree blob and staged Git blob are compared before commit; other source/text normalization is unchanged.

## WB-016 — DONE — Stocky Perfect Grounding/Opulence affix assignment at supported ilvl 72+

- Two primary glove targets: Global suffixes `EssenceLightningRecoupLife1` (Lightning Recoup 26..30) and `EssenceGoldDropped1` (Gold quantity 10..15), distinct families, modifier Level 72/effective requirement 57. Rare replacement follows complete removal-branch/family/slot/Fracture rules and Crystallisation matching/conflict handling.
- Dedicated zero-spawn extension preserves all 182 positive-weight definitions and normal 63,700/84,500 weight totals. Current 187 definitions = 182 normal + five special; compatible snapshots retain normal, Abyss and Horror. Source-byte regression extracts the staged Git tree and validates original Horror/new proof checksum and provenance in Docker.
- Scalar integer domains remain WB-003 UNVERIFIED models; client acceptance requires source/range/stat/count/label evidence. No Recoup timing/recovery or Gold-drop calculation; Solar unsupported. Lower-ilvl eligibility remains WB-017, not a proven game prohibition.
- Actual unique scope 67 of 220 registered, Stocky 60 support records/49 actions, Solar 60/49. Docker BE 215 (209 unit/six integration), FE 129/24 files; actual new-material browser 107, history 11, Stocky 60, previous Horror 49, Solar 30/cross-feature 21, zero failures/page errors. API also reaches/Fractures both targets and checks Divine/Chaos/Annul locks and ordinary pool separation.
- [Primary sources, level distinction, implementation scope, checks and non-rerun limits](docs/workbench-stocky-perfect-glove-essences-2026-10-03.md).

## WB-017 — OPEN — Lower-ilvl Perfect glove Essence eligibility is not established

- Detailed modifier Level 72 and effective/Required Level 57 are distinct published fields. Neither by itself establishes the material's actual minimum eligible item level; do not promote 57 into the item-level rule or call lower levels illegal in the game.
- Current implementation conservatively supports Stocky ilvl 72+ under the existing catalog convention; ilvl 1..71 remains unsupported with visible boundary and safe refusal. This is a bounded implementation scope, not a claimed complete eligibility rule.
- Resolve with current primary/official evidence for forced Essence modifier eligibility at lower item levels. Independently supported higher-level crafting and other material work may continue. See [source/eligibility boundary](docs/workbench-stocky-perfect-glove-essences-2026-10-03.md#source-evidence-and-eligibility-boundary).

## WB-018 — DONE — Stocky Prismatic Alloy through shared Rare replacement

- Source-proven zero-spawn Global ElementalPenetration prefix 9..15, Code AlloyElementalPenetration1; conservative supported ilvl45+, lower levels remain WB-017. Effect assignment only; resistance/damage calculation absent and labeled.
- Alloy classification excludes Crystallisation matching/consumption, including its pair; all eligible branches must fit family/capacity. Ordinary 182 definitions/weights intact; 188 total/six zero-spawn. Four prior snapshots remain compatible.
- Current unique implementation 68/220 registered; Stocky61 support records/50 actions, Solar60/49. Docker BE219/FE133 pass; actual new Alloy54/shared Runic48, old v21/v22 history11 each, zero page errors; actual API lock/pool and staged source-byte checks pass.
- WB-017 also covers Prismatic modifier Level45 versus effective requirement36: low-ilvl forced-Alloy eligibility is not established. No game prohibition inferred.
- [Primary evidence, exact limits, executed/non-rerun checks, observed costs and next data batch](docs/workbench-stocky-prismatic-alloy-2026-10-03.md).

Next independent data batch: source-backed single-effect Expansive/Cyclonic/Mystic Alloys through the same engine, with per-material automated proof/invariant tests and one shared browser regression. Conditional/multiple-family/Local-Ward candidates follow their own boundaries; WB-003/004/005/010/017 remain unresolved. Broad unchanged-screen regressions are not repeated per material.
## WB-019 — DONE — Expansive/Cyclonic/Mystic Alloy data batch

- Three exact current PoE2 glove results share the existing Rare Alloy replacement engine: Global suffix RemnantPickupRadius35..50/level25, DamagingAilmentDuration20..25/level45, IncreasedAttackAreaOfEffect10..15/level45. One checksum-reviewed extension; other equipment/Fists of Stone outcomes excluded.
- Whole removal-branch/family/capacity checks, Fracture/unaffected value preservation and Alloy-specific Crystallisation non-consumption retained. Numeric integers remain WB-003 UNVERIFIED; lower ilvl remains WB-017 (Expansive25 vs20 and Cyclonic/Mystic45 vs36). Computed collection/timing/area absent and labeled.
- Registry unique implemented71/registered220; Stocky64 support records/53 actions/191 definitions, Solar60/49 unchanged. Ordinary182 positive-weight definitions and63,700/84,500 totals unchanged; nine zero-spawn special definitions. Five old snapshot identities retained.
- Docker final BE231/FE145 pass; one combined actual Alloy browser70 and old v23 film11 assertions, zero page errors; actual per-target API locks/pools and staged source checks pass. Earlier full browser matrices/cachepersist were not rerun for this data batch.
- [Exact evidence, count definitions, execution limits and next invariant review](docs/workbench-stocky-scalar-alloys-2026-10-03.md).

Next independent review: Adaptive conditional effect, Swift two-family exclusion and Sovereign Local Ward property boundary. Source capture alone is not implementation. WB-003/004/005/010/017 remain unresolved.
## WB-020 — DONE — Adaptive/Swift/Sovereign bounded Stocky Alloy assignment

- Current primary glove targets: Adaptive conditional Attack Speed10..15 suffix/level25; Swift Cast Speed9..12 suffix/level45 with both IncreasedCastSpeed and IncreasedAttackSpeed families; Sovereign Local Ward24..30 prefix/level25, name Verisium. One source-checked data extension, existing replacement engine unchanged.
- Conditions remain source text, not evaluated state or invented currency prerequisites. No speed or Local Ward total/quality/socket calculation. Every removal branch and each family is checked; ordinary speed/Adaptive/Swift clashes and Fractures refuse safely. Alloy omens unconsumed, including pair. UI labels bounded assignment.
- Numeric model remains WB-003; lower-ilvl25 vs20/45 vs36 remains WB-017. Solar unsupported for these targets; Sovereign's separate Solar effect is WB-021.
- Unique implemented74/registered220; Stocky67 support records/56 actions/194 definitions, Solar60/49 unchanged. Ordinary182 positive weights63,700/84,500 preserved;12 zero-spawn special definitions; six compatible snapshots.
- Docker BE246/FE160 pass; combined actual browser70 and v24 history11, zero page errors. Per-target API locks and real family/condition/Solar refusal probes pass; staged source-byte checks pass. Unchanged individual browser matrices/cachepersist not rerun.
- [Classification, exact source/eligibility/calculation boundaries and execution evidence](docs/workbench-stocky-reviewed-alloys-2026-10-03.md).

## WB-021 — OPEN — Solar Sovereign exact target and resistance-magnitude boundary

- Current primary Sovereign item page explicitly gives Amulets/Jewellery +20..30% Explicit Resistance Modifier magnitudes, different from the source-proven Local Ward glove Code.
- The current source collection identifies the exact strength-glove target, not the exact Solar magnitude modifier's Code/families/stats/level. Do not reuse LocalRunicWardPercent or invent a resistance target/eligible set. Solar remains safely unsupported; confirmed glove work proceeds independently.
- Resolve by capturing the current Amulet target/detail and eligible conditions, then defining whether a bounded magnitude-affix assignment can be represented independently or needs modifier-scaling state. Actual resistance magnitudes must not be labeled computed until the scaling rules/provenance and roll/lock behavior are verified. Existing WB-004/005 remain separate.
- [Primary page and current unsupported boundary](docs/workbench-stocky-reviewed-alloys-2026-10-03.md#classification-and-primary-evidence).