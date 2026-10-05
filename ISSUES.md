> 최신 완료 집계 (2026-10-04): registry220 = active170(implemented170 = default163 + opt-in legacy7, pending0) + deferred50(보존 구현8 + 미구현42). Catalyst26는 검증된 제한 base만 IMPLEMENTED로 정리했다. 전체 구현178은 deferred8을 포함하므로 현재 사용 가능 수가 아니다. 아래의 이전 집계는 checkpoint 이력이다. [정의·base 제한·검증](docs/workbench-catalyst-registry-2026-10-04.md).

# Project issues

## WB-046 - PARTIAL IMPLEMENTATION / OPEN EVIDENCE - Sapphire basic currencies and Liquid

- 기존 검증된 Cast Speed suffix의 Annulment/Divine을 구현한다. Refined13과 합쳐 Sapphire에서 positive15이며 전체 기본 crafting이나 Liquid 완료가 아니다. [기능 경계와 이어갈 작업](docs/workbench-sapphire-existing-currency-2026-10-04.md).
- JL-01 잔여: current-game Magic/Rare side slot 근거, 일반58행의 eligible pool·stat mapping 검증. `DropChance=1`은 실제 weight로 가져오지 않는다. 확인된 후보에서 source weight가 없으면 사용자 허용 disclosed1/N fallback을 적용한다. 기존 suffix stable ID·film을 유지한다.
- JL-02 잔여: Rare Sapphire ordinary Basic Liquid10의 Crafted 표식 직렬화, 기존 Crafted 교체/제거, family 충돌, side-full과 fractured 분기. 현재 Crafted source 읽기 실패와 modifier hover HTTP403을 확인했으며 이를 규칙 확인으로 취급하지 않는다. 권장 reversible model과 source-confirmed rules를 구분해야 한다.
- 후속 순서: Sapphire 기본 제작 잔여 → Basic Liquid → 다른 Basic Jewel bases → Potent3 → Ancient14. Essence-on-Jewel, Vaal/Hinekora/Desecration 등 기존 제외 범위는 유지한다.

> 최신 사용자 지시 (2026-10-04): 기폭제 서비스 제외 취소. 현재144 = 기본111 + legacy7 + 기폭제 미구현26, 보류76 = 기존65 + 신규11, registry220/구현체126. 기존 기폭제 메뉴·typed 품질 입력/API·film·모델·테스트 보존. 실제 적용과 UX는 사용자 피드백 수집 후 조정. Catalysing 및 Necromancy2·훼손 신규 개발은 계속 보류. 이번5는 구현 checkpoint이며 최종 FE/API/browser 검증 미완료. [최신 범위](docs/workbench-service-scope-2026-10-04.md).


## WB-039 — DONE USER REVIEW — 최종 사용자 동선과 지원 경계

- 검토 코드 HEAD `bbe608d1f673667269c26ab473bcb5194b38f103`, branch `workbench/20261002`. 최신 QA immutable JAR와 HTML bundle 일치 확인. 핵심 UX browser30 PASS/page errors0; Shift/빈곳 취소/Omen 우클릭 활성·충돌/Alt/좌우 stash/새 film/과거 제작 원본 미래/저장 실패·손상 보존 확인. 390px screenshot 자체 리뷰. 추가 표시 경계와 실행 실패는 [최종 증거](docs/evidence/workbench-final-user-review-2026-10-04.json)에 분리 기록.
- 실제 product 결함을 확인하지 않아 executable code/catalog/lockfile/migration은 변경하지 않음. 오래된 README 시작 설명과 pending44 이력을 최신 완료 집계로 오독하지 않도록 [최종 사용자 보고서](docs/workbench-status-2026-10-04.md)를 README 상단에 연결함.
- 추천/적용: 기본111·opt-in legacy2·미완료42·보류65와 보존 Alloy8을 분리 보고하고 legacy/current obtainability, typed quality foundation/per-use action, source weights/game odds 경계를 유지. 신규 scope나 조사 반복 없이 근거 확보 항목부터 진행. rollback은 문서 링크/향후 계획 변경이며 film/DB 삭제가 필요하지 않음.
- 사용자 판단: 현재 미답 제품 결정 없음. WB-001/WB-008 추천은 이미 승인됨. 외부 게임 사실은 개발 조사 조건이며 사용자에게 추측 승인을 요구하지 않음. 향후 Vaal/보류65 해제·새베이스·서버 저장·원격 반영을 요청할 때만 scope 판단 필요.
- 검증 비용: 변경 없는 BE/FE 전체 및 기존 API/browser matrix/cache 재시작은 반복하지 않음. 새 boundary probe는 첫 CSS locator timeout과 두 번째 재료 좌클릭/우클릭 계약 오류 timeout 후, 실제 품질 문구 locator도 교정하여 마지막 재실행12 PASS/page errors0. 총2회 복구이며 product 코드나 기존 기대값은 변경하지 않음. 원본 checkout/.env/DB volume 보존, 임시 browser 정상 종료/--rm, 원격 push/merge/deploy 없음.

Single issue list from 2026-10-02, explicitly requested by the user. Record new bugs, uncertain rules and verification tasks here; audit documents retain supporting evidence, not competing issue lists. IDs are stable. OPEN = unresolved; ASSUMED = authorised conjecture requiring game verification; BLOCKED = missing required data; DONE requires linked verification. Vaal Orb/Hinekora are user scope exclusions, not bugs.

## WB-001 — ASSUMED — Coupled numeric rolls

- Scope: source-proven multi-stat Workbench modifiers; Stocky Mitts, Crude Bow and Attuned Wand Workbench explicitly opt into the conjecture, while default Solar does not.
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

- Scope: current-base quality: Stocky Armourer/Infuser, Solar13 ordinary Catalysts/Infuser/Catalysing Exaltation. Refined variants require Jewels. Liquid27 deferred (WB-022).
- Impact: per-use increment and any rarity/ilvl dependence, type replacement amounts, stat precision/rounding, tag applicability and selection-weight transform/order remain unverified; no quality currency activation. Source-reviewed maximum20/40 is independently implemented (quality-limit-v1), not applied quality or computed defense/jewellery magnitudes. [Source table, minimum activation boundary and next route](docs/workbench-quality-review-2026-10-03.md).
- Evidence: [existing rule audit](docs/workbench-rule-audit-2026-10-02.md), [per-item triage](docs/workbench-remaining-triage-2026-10-02.md).
- Done when: current source confirms increments/state transitions and every affected stat/tag/weight rule; verify quality history and ordinary crafting/omen interactions without mixing incomplete weights.

## WB-005 — OPEN — Other Solar special-state rules

- Current scope: Homogenising, Chance and identification. Sanctification, Mirror and the specifically listed desecration Omens are deferred by the latest user instruction (WB-025); Liquid instilling was already deferred (WB-022). Glove-specific special Essence/Alloy targets require separate complete source/result validation; copying Solar targets is unsupported.
- Impact: current ordinary Solar state does not represent all required state/results; availability/type-selection/roll/replacement evidence remains incomplete. Registration does not imply implementation or obtainability.
- Evidence: [per-item reasons and sources](docs/workbench-remaining-triage-2026-10-02.md). Current development deferrals follow WB-025; dated source classification is historical and catalog presence does not establish current obtainability.
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

## WB-021 — DEFERRED BY USER — Solar Sovereign exact target and resistance-magnitude boundary

- Current primary Sovereign item page explicitly gives Amulets/Jewellery +20..30% Explicit Resistance Modifier magnitudes, different from the source-proven Local Ward glove Code.
- 2026-10-03 primary Amulet target/detail now verified: AlloyEffectOfResistanceMods1; Prefix, EnchantmentHeistArmour, Level65/effective52, zero spawn, IsAlloy/Removes; stat heist enchantment resistance mod effect +%, 20..30, Local/Unscalable Value. Exact target discovery is complete. Solar remains unsupported while magnitude rules, eligible resistance stats, numeric grain and low-ilvl boundary are unresolved. [Captured source and remaining route](docs/workbench-remaining-triage-2026-10-03.md).
- Resolve by capturing the current Amulet target/detail and eligible conditions, then defining whether a bounded magnitude-affix assignment can be represented independently or needs modifier-scaling state. Actual resistance magnitudes must not be labeled computed until the scaling rules/provenance and roll/lock behavior are verified. Existing WB-004/005 remain separate.
- [Primary page and current unsupported boundary](docs/workbench-stocky-reviewed-alloys-2026-10-03.md#classification-and-primary-evidence).
## WB-022 — DEFERRED BY USER — Liquid crafting materials

- 2026-10-03 user explicitly deferred all 27 LIQUID_EMOTION registry identities. Retain every registry/source/catalog record; no Liquid implementation or current completion target.
- 13 Basic-Jewel and 13 Time-Lost-Jewel current item-card operations, plus Liquid Verisium encounter. Historical v25 classification remains dated evidence; user scope supersedes it without rewriting source facts.
- Registry remains220 / implemented74. Non-Liquid inventory193; unimplemented non-Liquid119. These inventory counts are not a current-two-base completion percentage. Basic currency exclusion in the requested Korean-name output is output-only, not implementation deferral.
- Current quality work continues separately. [Updated scope and priorities](docs/workbench-scope-update-2026-10-03.md).

## WB-023 - PARTIAL - Ordinary Stocky Artificer implemented; exceptional/resource state pending

- Current full primary card confirms one Augment Socket and eligible equipment classes; Stocky is Armour, Solar is outside those classes. The Stocky `socket_info=1:5:100` tuple does not establish a use cap or minimum level without its semantics. Exceptional dropped sockets and Socket-bound augments require explicit state preservation.
- Ordinary Stocky0→1 is now implemented through reviewed secondary cap/class evidence. Fresh states know zero; legacy unknown stays unknown and Artificer-refused while ordinary crafting remains available. Exceptional/socketed Augment states and Extraction remain unsupported. [Delivered path, source boundaries and checks](docs/workbench-stocky-artificer-2026-10-03.md).

## WB-024 - DONE - Unsupported item properties were silently erased by crafting

- Actual prior Docker API reproduced200/applied while discarding supplied quality/socket state. Apply/actions now reject unsupported root/modifier fields with422 `UNSUPPORTED_ITEM_PROPERTIES` before conversion.
- Frontend rejects unsupported requests/responses/restored frames without rewriting saved archives. Existing ordinary crafting and legacy derived `modifierIds` remain supported. No newly implemented material is counted; quality and socket engine work stays open. [Repair and verification](docs/workbench-quality-socket-boundary-2026-10-03.md).
## WB-025 - DEFERRED BY USER - Vaal, Mirror, Core, specified Omens and all Alloys

- Future development only: exact mapping65 effective exclusions (64 direct, one Vaal-operation dependency), including retained Liquid27/Hinekora, all Alloy13, Vaal-related11 including Architects Orb, Mirror/Core and10 specified Omens. Existing8 implemented Alloys/code/UI are preserved. No deletion or unnecessary disablement.
- Omen of Putrefaction and Omen of Corruption are distinct names/IDs; the latter is separately dependent on deferred Vaal Orb. Crystallisation for current Essences remains supported. WB-021 Solar Sovereign and additional Alloy work are deferred; WB-005 Vaal-related outcomes and Sanctification are future work. The existing nine-family route is overridden by these deferrals.
- Registry220/overall implemented75 remains separate from current development inventory155/implemented67/unimplemented88. The new count75 includes bounded Stocky Artificer. No complete-game or two-base completion claim. [Full Korean/ID mapping and counts](docs/workbench-development-deferrals-2026-10-03.md).
## WB-026 - DONE - Crude Bow bounded Workbench and basic Essences

- Scope: user-authorised limited equipment base expansion, compared against Wand/body armour and existing rule/state candidates. One complete140-row Bow catalog enables21 basic Essence paths,20 new registry implementations. No combat totals, full-base crawl, socket resource engine or Perfect Bow result claim.
- Evidence: [selection, primary sources, local/global Spawn Tag disambiguation and item/character level distinction](docs/workbench-crude-bow-2026-10-03.md). Same65 current-development exclusions and eight implemented deferred Alloys retained.
- Regression: shared ordinary crafting, exact guaranteed targets, level/family refusals, Fracture retention and old Solar/Stocky catalogs; final Bow browser120, Artificer browser26, API121+42, full BE289/FE192 passed; details in the linked document. Numeric precision/correlation remains explicitly ASSUMED/OPEN (WB-001/WB-003).

## WB-027 - DONE - JSON stat key order rejected valid crafts

- Trigger: actual Lesser Essence of Ice on Crude Bow. Server event values and final modifier values contained identical two-stat data with different JSON property order. FE's JSON.stringify equality rejected the valid response and preserved the old Magic film.
- Fix: compare exact stat-key sets and values independently of serialization order, also for preserved/replacement/Greater Exaltation/Fractured instances. Forged extra keys or changed values still refuse. No archived film rewriting.
- Evidence: actual QA response fixture, positive reorder/Fracture refusal and negative forged-data contracts in BowWorkbenchContract.test.ts; final Bow browser120/Artificer browser26 and full FE192 passed on index-ChEgnTIJ.js; no page errors.

## WB-028 - DONE / OPEN EVIDENCE - Bow Perfect Essence assignment

- DONE: six source-matched Bow special results, zero ordinary spawn weights, generic Rare replacement/Crystallisation path and three-base history compatibility. [Primary evidence, conditions and executed checks](docs/workbench-bow-perfect-2026-10-03.md). Same65 exclusions and retained Alloys preserved.
- OPEN evidence: actual Essence applicability below source mod level72; character requirement57 is distinct. Simulator conservatively blocks below72. Source-unit integer increments/distribution remain explicitly UNVERIFIED (WB-003); fixed Battle +2 has no numeric lottery. Onslaught trigger/duration, resulting damage and skill-level calculations remain outside equipment-affix assignment. No invented engine rules or combat totals.

## WB-029 - DONE / OPEN SOURCE BOUNDARY - bounded Attuned Wand

- DONE: complete185-row ordinary pool,11 individually verified Essence paths (eight new identities; Seeking three already on Bow), two zero-spawn Perfect results, shared four-base dispatch, immutable Mana Drain base fact displayed without simulating its skill. [Primary evidence and verification](docs/workbench-attuned-wand-2026-10-03.md).
- Source distinction: all185 detail Spawn Tag numbers differ from the main table weight fields. The existing user-selected POE2DB_AS_PUBLISHED policy consistently uses the complete main table values; detail ordered Spawn Tags establish eligibility only. No missing weights or mixed weight units. These source-published weights are not official game probability measurements, which remain unverified.
- OPEN evidence: source-mod-level gates are conservative simulation boundaries, not confirmed lower-item-level game Essence requirements (WB-028). Source-unit precision/distribution and coupled ratios remain UNVERIFIED (WB-001/WB-003). Innate Mana Drain level/progression, combat, Wand applied quality, sockets/resource state and pasted Wand mapping are not simulated. Excluded65 and existing implemented Alloys remain preserved.

## WB-030 - DONE AUDIT / OPEN TARGETS - remaining material and probability boundaries

- Remaining Essence12 newly verified against individual cards and four current class tables: existing-base result0, additional-target12. Delirium has an unverified Notable pool; Insanity additionally depends on deferred Vaal/enchantment rules. No material is marked implemented and exclusions65 remain intact. [Classification, per-material blockers and source evidence](docs/workbench-remaining-materials-2026-10-03.md).
- WB-029 further audit:185 exact Codes/levels and unique eligible tags agree;12 table/detail numeric ratios exclude a single scale factor. Public client confirms DropChance weighted use but not server derivation. Cache/extraction provenance and actual game odds remain unresolved. Complete table DropChance is a source model only; ordered detail Spawn Tags determine eligibility.
- UI correction: guaranteed Essence/Alloy ADD events no longer masquerade as natural weighted draws. Natural draws explicitly name source-model probability and the selected base table/field distinction. Numeric/fixed-result ledgers, low-level gates, four-base films and JSON contracts are preserved.

## WB-031 - DONE / OPEN EVIDENCE - minimal Rusted Cuirass Perfect batch

- DONE: compared Body144/171 source-code lookups with Sceptre150/346; chose lowest class-index Rusted Cuirass. Complete144 ordinary definitions plus three zero-spawn Perfect Body/Ruin/Seeking results reuse the existing replacement engine and fifth-base film dispatch. [Base/effect proof, support boundaries and validation](docs/workbench-rusted-cuirass-2026-10-03.md).
- Scope: current104/155 implemented,51 pending; overall112/220, effective exclusions65 preserved. Delirium is still unimplemented and now primarily RULE_DATA_GAP because its Body target exists but Notable outcomes remain unverified. Principal pending totals23 other-target,21 rule/data,2 engine state,5 reintroduction.
- OPEN evidence: actual lower-level Essence gates, source numeric precision/distribution, and table/detail weight derivation/game odds. DropChance is the complete source model, ordered detail Spawn Tags only eligibility. Armour/combat/movement, applied quality/Scrap, sockets and pasted Body mapping remain unsupported. Insanity/Vaal deferred dependencies unchanged.

## WB-032 - DONE / OPEN EVIDENCE - Iron Ring and fixed implicit

- DONE: complete203 ordinary Ring rows uniquely match391 public Code candidates (314 cached77 new); fixed implicit endpoints1/4 and one Perfect Mind result are preserved. Normal Divine supported; Blessed has no variable target. Source-reviewed quality cap is absent and stays null/unknown. [Source, supported conditions and executed checks](docs/workbench-iron-ring-2026-10-03.md), [validation](docs/evidence/workbench-ring-validation-2026-10-03.json).
- Browser found a real display defect: multi-stat fixed implicit used internal stat IDs instead of the source phrase. Recommended/applied reversible fix: return the sourced text only when all multi-stat endpoints are fixed and exactly match the values. Forged endpoints retain fallback; applied/initial contracts independently reject them. Revert the bounded rolledText branch if a future source format disproves it; no state or history schema migration is involved.
- First BE validation found implicit tier1 violated the existing IMPLICIT tier0 contract; source-independent representation corrected and full mandatory check rerun. No old tests weakened.
- OPEN evidence remains WB-001/WB-003 numeric assumptions, WB-028 lower-level game applicability, table/detail weight derivation and actual game odds. No new inference about generic source Flags/Unique GenerationType, Ring quality maximum, sockets or combat.

## WB-033 - DONE INVENTORY / BLOCKED RULES - post-Ring gaps and next validation

- Exact inventory:119/220 implemented overall; current scope111/155 implemented,44 pending. Existing exclusions65 contain57 unimplemented and8 implemented preserved Alloys. [Complete44+57 ID list, evidence/engine/user gates and integration plan](docs/workbench-remaining-gaps-2026-10-03.md), [machine inventory](docs/evidence/workbench-remaining-gaps-2026-10-03.json).
- Recommended next work: verify precise quality rules for existing reviewed equipment before adding more bases; seek complete Delirium/Insanity and Omen outcome evidence independently. Keep unspecified effects/pools disabled; authorisation cannot create game facts. No new base is selected. Undo recommendation by changing only the future plan; current support remains intact.
- User gates:65 existing exclusions remain binding. Insanity remains current pending and inherits the user-deferred Vaal dependency; enabling it would require a later explicit scope change. No unanswered product choice blocks Ring. Future reversible product decisions should record recommendation/application/rollback here; no fresh security permission is assumed.
- Planned, not passed: maintained legacy browser30/cachepersist and the exhaustive9-base material/Omen/storage-failure browser matrix. Existing checks and current Ring validation are separately recorded; old passes are not reused as current results. Sequential Docker only, preserve DB/volumes, normal owned-container cleanup, local commits only, no remote writes.
## WB-034 - DONE - Perfect Mind canonical image recovered

- Visual review found the existing Perfect Mind icon is unavailable. The tracked sources.json already records an unavailable download with historical403; the local file is absent. This is not a new crafting/state failure and current CDN status was not retried in this final checkpoint.
- Recommended/applied boundary: preserve the existing clearly named Image unavailable fallback; do not invent or substitute another material's icon. Next permitted public-source check can retry the canonical reviewed art URL and retain source/hash if successful. Do not bypass access restrictions or infer a required credential. Rollback a later replacement by restoring the previous source manifest/fallback, with no item or film changes.
- Evidence: frontend/public/assets/materials/sources.json, ring-narrow.png visual review under codex/qa-20261003. Crafting and retry remain usable; source/image limitation is distinct from rule support.

- 2026-10-03 continuation: the exact existing canonical CDN URL now returns HTTP200 in project Docker. Downloaded original108x108 WebP visually matches the Mind artwork; SHA-256 `0f7f33951fb9436ad90ab782900156970f4d8e57e4d0af57d0f9a8fc2863e6cd`. Asset and manifest restored without substituting art or changing source/license. Historical403 above remains the prior checkpoint, not current status. The existing Image unavailable fallback stays available for later load failures.
- Applied reversible recommendation: restore the reviewed asset rather than add image-generation or another source. Impact is the material/favorite/tooltip image only; no item state, film schema, rule or support-count change. Rollback by reverting this asset/manifest commit; no storage migration.

## WB-035 - DONE TRIAGE / PARTIAL MATRIX - continuation integration review

- Pending44 primary routes:42 source/effect/outcome evidence gates, two identification/extraction state-design gates, zero confirmed code-only feature omissions. Routes overlap: state gates still need precise use rules; quality/Notable/Insanity also need representation. No new material, base, inferred probability or deferred operation is enabled. [Exact rationale](docs/workbench-remaining-gaps-2026-10-03.md#implementation-triage-after-handoff).
- Legacy browser entrypoint failed its obsolete660px assertion. Existing WB-014 already documents600px to keep the central button fully visible. Use the maintained `stocky-horror-workbench-browser.cjs` checks (local scrolling, targets at least44px, entire central button visible); current30 assertions pass, including quota/corrupt cache preservation. This is a test-entrypoint correction, not a new CSS change or weakened current UX requirement.
- Remaining exhaustive per-material/per-base browser coverage is distinct from the bounded cross-base matrix. [Execution report and limits](docs/workbench-continuation-integration-2026-10-03.md): FE320, browser993/page errors0 (prior617 + completion376), cachepersist passed. Retain existing tests and source evidence. Rollback the triage recommendation by editing future priorities only.
- Completion selection covers default/override basic Essence guarantees, two-stat Bow values, Greater low-level/rarity boundaries, six same-trigger UI conflicts, independent server conflicts, distinct-trigger sequential consumption, double-add capacity/tier refusal, basic versus replacement triggers, old-future/evidence/reload and390px. No new product bug was found; no rule, assertion or history schema was weakened. The bounded requested integration batch is complete, while exhaustive permutations remain outside its stated scope.
- Clarified pending44: all44 retain external fact gaps and missing material behavior;35 also have confirmed missing operational state (including quality-dependent Catalysing Exaltation), nine retain fact/rule-extension gates, zero have complete facts with only code left. The existing UNIDENTIFIED enum/parser marker is preserved and acknowledged; operational reveal is absent. [Exact44-row audit](docs/evidence/workbench-pending44-fact-implementation-audit-2026-10-03.json). No fully evidence-ready next material can be claimed; Wisdom is the narrowest candidate only after authoritative reveal/retention evidence. This fact dependency needs no invented user preference or guessed result. Rollback is documentation-only; support counts do not change.

## WB-037 - DONE FOUNDATION / OPEN ACTIONS - typed catalyst quality

- Final requested boundary review found no new bug and reused existing successful unit/API/browser evidence without rerunning it. Repeat rendering always derives from saved original values; multi-tag matching is boolean/once; cap, negative/decimal/unreviewed mixed cases stay unscaled or rejected. Currency refusal is confined to explicitly typed-quality jewellery states; all nine unsupplied-quality ordinary crafting paths remain verified. No selective allowance is guessed without interaction evidence. [Exact coverage and completion boundary](docs/workbench-catalyst-quality-2026-10-04.md#final-boundary-review-2026-10-04-kst).
- Compatibility defect fixed: restored films previously selected only Solar/Stocky catalogs. Dispatch now selects the existing catalog for all nine bases, preserving original frames and allowing typed Iron films. Revert the dispatch to restore the previous behavior; no archive migration occurs. The new source link uses the existing readable gold/focus styling after visual review.
- Validation: Backend354 tests; Frontend320 existing plus nine corrected new fixtures, lint/type/format/build; runtime API74, browser56 plus five final link/focus checks, page errors0. All1487 nine-base source entries preserve weights/ranges/tags. [Completion evidence](docs/evidence/workbench-catalyst-quality-validation-2026-10-04.json). Browser/tests were sequential and owned temporary containers closed normally.
- Delivered one optional ordinary catalyst type/current amount on reviewed Solar/Iron ItemState, original-roll preservation, bounded positive integer derived magnitudes, inspection API, existing starting-base input and film v1 persistence. Iron cap20 now source-verified; other unknown-cap policy stays intact. No new base/action completion: current111/155, pending44 and exclusions65 unchanged. [Evidence and contract](docs/workbench-catalyst-quality-2026-10-04.md).
- Reversible recommendation applied: connect already present quality to the existing Place base form; retain API/history support for validated Magic/Rare original rolls. This avoids pretending to know per-use currency consumption/reset/increments. Impact is state/validation/display/film compatibility, with no migration or DB mutation. Undo by reverting this implementation; saved typed frames then remain preserved unsupported archives under the old boundary rather than being stripped.
- Reversible numeric boundary: scale only the explicit reviewed whole-number stat unit list, plus Iron's fixed physical implicit. Matching unknown numeric units, negative/mixed stat cases and unscalable caps retain their original values/status; a secondary-source scalar is never represented as official engine precision. Extend only after stat-specific evidence, or remove an approved stat from both display lists while retaining every raw roll.
- Ordinary currencies and Omens on supplied typed quality (including0) refuse unchanged because their interactions are not established. Types/replacement application, ilvl increments, unique exceptions, negative/decimal edge rounding and Catalysing Exaltation's quality-to-weight function remain OPEN. No obsolete PoE1 formula, universal+1 or guessed1/N is used.

## WB-036 - BLOCKED EVIDENCE / INPUT - Wisdom reveal and hidden-state provenance

- PoE2DB current card confirms identification and unidentified target instruction; official Item API documents identified/unidentifiedTier but no hidden-payload or reveal contract. Complete retained fields, special-condition eligibility and same-item payload provenance remain unresolved. [Sources, exact limits and resume gates](docs/workbench-wisdom-evidence-2026-10-03.md). No new material/base or random hidden affix generation; current111/155 and pending44 unchanged.
- Reason: existing concrete ItemState has no operational hidden payload, and unknown pasted modifiers cannot be reconstructed from ordinary currency pools. Effect text alone does not supply the missing game facts/input. Actual Docker nine-base actions/apply refusal probes18 passed; feature unit/API/browser tests were not run because no justified executable path was added.
- Recommended design: Workbench-specific known/unknown visibility envelope with source-bound payload; deterministic reveal preserves all concrete attributes and film branches/reload, refuses unknown/repeated/unverified-condition uses without consumption. Impact would span state/API/UI/film contracts, not Support/Explorer. Apply only after documented source/input gates; no placeholder or visibility demo counted as implemented. Rollback now is documentation-only; later implementation must preserve old validated films and unsupported archives.
# WB-038 - BOUNDED LEGACY SUPPORT / OPEN INTERACTIONS - Homogenising Omens

- 최종 검증 완료: Backend359, FE 정적검사/build 및 고유333개(전체331 + 관련30), runtime API269, 원문 catalog1487, browser83 PASS. 아래 중단 체크포인트는 재개 전 이력이며 완료 상태로 대체한다. 기본111 + legacy2 = scope113/155, pending42, registry121/220; 보류65와 기존 retired5 유지.
- UI 가역 추천 추가: 좁은 화면에서 긴 안내가 재료 목록을 가리는 문제를 실제 screenshot에서 확인했다. 안내 높이110px/내부 스크롤과 기존 출처 링크 스타일을 적용해 내용·검색·목록을 보존한다. rollback은 legacy-omen-info CSS/class만 되돌린다. 범위 제한 정적검사/관련30개/build/layout 추가 검증 비용을 기록한다.

- 제한적 재개: 최신 사용자 지시 “자리비울테니 … 작업 계속 … 이슈는 … 기록 … 추천사항대로 작업”와 후속 위임의 명시 재개 승인에 따라 이번 fixtureFetch 최소 수정, FE 필수 검사 및 미실행 API/browser, 안전한 로컬 commit을 진행한다. 기존2회 정책 자체를 바꾸지 않으며 비용 한도의 이번 복구 예외만 기록한다. 원격금지/DB보존/무거운검사 순차는 유지한다. 반복 복구 실패는 정확한 시도·원인을 보고한다.
- 재개 후 전체 FE333 중331 PASS/2 UI 계약 불일치: 새 legacy opt-in과 기본 목록30이 과거 “Omen checkbox 없음/32개” 기대값에 걸렸다. per-Omen activation checkbox 부재와 legacy visibility checkbox를 구분하며 기본30/opt-in32/다시30 및 두 Legacy 이름을 추가 검증한다. 원래 활성 해제/충돌/즐겨찾기15/원본재료180/카드 보존 검증 유지. product 코드 변경 없이 해당 두 테스트와 새4 테스트30개만 재검증하고 build를 완료한다. 넓은 성공331을 또 실행하지 않는다.

- 검증 체크포인트: Backend359 PASS, Frontend npm ci/lint PASS/typecheck TS2554(new fixtureFetch call) 실패. 작업 전체 수정·재검증2회 한도로 재개 승인 요청. 아직 전체 묶음 완료·commit·clean 아님. 아래113은 staged 구현 집계; 최종 검증된111/155 완료 집계 유지. [실행/미실행 기록](docs/evidence/workbench-homogenising-validation-2026-10-03.json).

- 추천/적용: 공식0.4의 drop 중단·기존 개체 작동 근거에 따라 Homogenising Exaltation/Coronation만 legacy 지원으로 연결. 기본 Omen 목록에서 숨기고 명시적 opt-in과 Legacy 이름으로 현재 재료와 구분. 사용자의 가역적 제품 추천안 적용 승인에 따른 결정이며 다른 legacy5를 활성화하지 않는다.
- 영향: 9베이스 ordinary pool/weight 공유, tag 교집합, tagless 성공 소비, Greater Exaltation 병용의 시전 전 tag 고정, 성공 시 matching trigger만 소비. 기존 원문 catalog/film v1/보류65/typed quality 경계 유지. 현재 개발 구현113/155는 기존111+legacy2; 기본111, legacy2, pending42; 전체121/220. 현재 drop 가능 개수 주장 없음.
- 미확인: 실제 실패 소비, 다른 same-trigger 병용, 강화 화폐, typed quality 상호작용. Workbench는 원자적 거부로 보존하며 UI에 게임 사실과 구별. Necromancy의 ordinary+exclusive 후보와 공개/분포/포화/특수 상태 및 Catalysing의 bias 함수는 문서 조사만, 미구현 유지.
- rollback: 이 묶음의 enum/규칙/UI/registry 변경을 되돌림. saved film/raw values를 삭제·재작성하지 않음. [근거·범위·검증](docs/workbench-homogenising-legacy-2026-10-03.md).

최종 UI 보정 검증: 기존 node:24-alpine에서 lint/typecheck/format:check, 관련3파일30개, build PASS. 최종 build layout10개(390px/1440px)와 screenshot 자체 리뷰 PASS. 추가 node:22-bookworm 선택 오류는 test startup 이전 native binding 실패로 기록하며, lockfile/의존성은 바꾸지 않았다.

# WB-040 — REVISED / CHECKPOINT — 최신 사용자 범위

기폭제26의 보류 취소를 반영했다. 이전 메뉴숨김·입력비활성·quality-display API 차단은 이번 작업 변경만 되돌렸고 모델·테스트·원문·film을 보존한다. active144/pending26/deferred76/registry220, 구현체118/전체126. 신규 보류11은 품질화폐3·특수Essence2·Wisdom/Chance/Extraction3·Catalysing1·Necromancy2다. 기존보류65 유지. 기폭제 실제 적용과 새UX는 피드백 수집 완료 후 조정한다. 훼손 신규 조사/개발은 하지 않는다.

# WB-041 — COMPLETE / GAME EVIDENCE GAPS OPEN — legacy5

최종 검증은 WB-042에서 완료했다: Backend362+6, FE 고유344/정적검사/build,9-base API640, browser95 PASS. 아래 중단 기록은 이전 checkpoint 이력이며 실패·복구 횟수를 보존한다. 현재 범위 구현/검증118, catalyst pending26; 사용자 live server에는 미반영이다. legacy Alchemy 내부 joint odds와 실제 실패소모 근거 gap은 유지한다.

Sinistral/Dextral Alchemy·Coronation과 Greater Annulment 기본 효과는 현재 PoE2DB Omen에 명확하다. 공식0.3 획득중단을 효과삭제와 구분하고 일반 경로를 구현했다. 기존 engine/base/slots/family/ilvl을 공유하며 same-trigger 조합·tiered currency·one-removal GreaterAnnulment만 안전하게 거부한다. fractured Magic은 기존 validator가 거부한다. GreaterAnnulment는 unlocked2개 순차 비복원 제거로 fracture를 보존한다.

Alchemy 최대방향3개 먼저/반대1개 후 conditional published weight draw는 가역적 제품 모델이며 실제 게임 내부순서·joint odds를 확인한 사실이 아니다. legacy-alchemy-order-v1 UNVERIFIED assumption으로 film에 남긴다. 실제 실패소모·제거가능1개·same-trigger 조합·tieredRegal은 미확인 게임사실로 좁게 유지한다. 서비스 실패 자원보존은 승인된 제품선택이다. rollback은 새enum/분기/metadata/UI만 되돌리고 기존film/원문/DB를 보존한다.

검증: scope복귀 전 BE check/generateJooq/bootJar PASS(357unit+6integration), legacy5 4tests PASS. FE 격리복사 npmci/lint/typecheck PASS 뒤 변경link label format 중단; unit/build/runtimeAPI/browser 미실행. 운영명세 작업전체복구2회를 사용해 추가재시도 중단. 사용자 테스트서버/실제localStorage는 변경하지 않았다. [최신 근거·재개](docs/workbench-service-scope-2026-10-04.md).
# WB-042 — COMPLETE / BOUNDED GAME GAPS OPEN — Omen composition and local removal preview (2026-10-04)

- **최종 완료**: 사용자 명시 continuous 지시에 따른 한정 harness 복구1묶음에서 FE fixture22 PASS, 이전 변경 없는322와 합쳐 고유344 PASS. Backend362 unit/ArchUnit +6integration 및 check/generateJooq/bootJar, FE npmci/lint/type/format/build PASS.9-base API640, browser95/page errors0,390px/1440px 후보 카드 visual QA와 film reload PASS. source537개 SHA 일치. 이전2회·최초API/browser 실패 이력은 아래 보존한다. 구현118/검증113 pending 구분은 최종 로컬 검증118로 대체하며 live 반영 아님. 모든 substantive assertions 유지; FE/계약 타입·browser 대기 fixture만 복구했고 제품 코드 추가 변경 없음.
- isolated project18280/18281 정상 stop, 사용자18080/18081·기존4 container·DB/storage 보존, shared heavy slot 반환. 별도 i18n `242954aa0822e572cb999a439d0b7a1abfb5f24a` 통합하지 않음. 다음 catalyst 실제 적용/UX와 이후 i18n 통합검증을 위한 clean checkpoint다.390px document overflow는 없고 orange 후보/안내는 보이나 기존 고정 currency canvas 일부가 panel 내부에 잘리는 모습은 다음 UX 검토 항목으로 기록한다. 신규 responsive 기능 범위 추가 없음.

- 사용자 명시 구현 승인으로 omen bundle과 legacy5 최종 검증을 재개했다. 기준 `0c044759`, `workbench/20261002`, 단독 writer. 이전 bundle의 복구2회 이력은 보존하며 이번 승인 이후의 결함 수정·재검증도 최대2회로 제한한다. 사용자 서버18080/18081과 실제 browser/localStorage는 보존한다.
- 확정 제품 규칙: Sinistral Erasure + Whittling은 unlocked prefix 후보를 먼저 제한한 뒤 그 안에서 `requiredItemLevel` 최솟값을 선택한다. 동률은1/N. 이는 명시 사용자 규칙이며 독립적으로 확인된 실제 게임 순서/확률로 표현하지 않는다. frontend는 현재 catalog/state에서 후보 전체를 계산하고 orange 표시한다. hover별 API 요청 없음.
- audit의 Wiki Omen 예시에 근거해 Sinistral Annulment + Greater Annulment를 병용하고, 기존 방향 의미에 따라 Dextral도 대칭적인 suffix 모델로 지원한다. 해당 side의 unlocked2개 이상 조건, 단계별1/N, 두 matching omen각1소모, unrelated 유지, 거부 시 draw/state/소모 없음. Wiki는 secondary community source다. 1개만 남은 동작으로 일반화하지 않는다.
- 추천·채택한 가역 구현 선택: 단일 directional Coronation/Exaltation, 단일 Erasure/Whittling, 단일 Homogenising의 Greater/Perfect는 기존 효과와 기존 minimum-added-level35/50·최고 eligible type fallback을 합성한다. tag/side eligibility 먼저, level pool 나중. Chaos의 삭제 pool에는 added-level cutoff를 적용하지 않는다. 이는 구현 합성 정책이며 모든 조합을 실제 게임에서 재현했다고 주장하지 않는다. rollback은 명시 allowlist의 해당 omen 제거로 가능하다.
- 추가 추천·채택: Dextral Erasure+Whittling은 suffix→최저level→동률1/N으로 대칭 지원한다. 별도 사실 차이가 없는데 예시가 Sinistral이라는 이유만으로 차단하지 않는다. Greater Exaltation은 Greater/Perfect 각 추가 단계에 기존 minimum-level pool을 적용하고 모든 첫 결과의 두 번째 pool을 선검사한다. Homogenising 병용 시 각 단계에서도 시전 전 tags를 고정하고 tag eligibility 뒤 level pool을 적용한다. `tiered-omen-composition-v1` UNVERIFIED ledger로 게임 odds/실패소모 보증과 구분한다. rollback은 bounded 조합 allowlist 또는 tiered 분기 재차단이며 데이터 변경 없음.
- 유지하는 정확한 미확인 의미: Greater Exaltation + directional Exaltation의 count/side 중첩에서 두 결과 모두 같은 side인지, 한 side slot만 남았을 때 partial action/소모가 있는지는 audit에서 확인하지 못했다. 추천은 두 draw 모두 side 제한하고 완전한2-result branch만 허용하는 모델이지만 이번에는 근거 예시가 있는 제거 조합과 기존 Homogenising 예외만 확장한다. Greater Exaltation1-total-slot와 Greater Annulment1-eligible-target의 partial 동작/소모 역시 미확인이라 거부한다. Homogenising+directional tag/side 중첩도 source가 구체적 순서와 zero-overlap 처리를 확립하지 않아 제외한다. 반대 side pair는 제품 제한 충돌이고 기타 미확인 same-trigger 조합은 별도 근거 부족으로 atomic refusal한다. 모든 조합 일괄 허용 없음.
- **사용자 인게임 검증 보류**: modifier level이 없는 special affix의 Whittling 순서, Fractured와 Whittling 순서/동작. 누락 level을0으로 만들지 않는다. Backend catalog는1..100을 요구하고 부적합 catalog/state를 거부한다. frontend는 누락/0/invalid level이면 후보 표시를 중단한다. 현재 Fractured Whittling 전체 거부 유지. 확인 전 schema/model 완화 없음.
- 제외 범위 유지: Desecration 전체, Catalysing Exaltation, Necromancy, Vaal/Hinekora, Liquid 및 지정 special material. catalyst26은 서비스 범위에 있지만 실제 적용/새UX는 다음 bundle이다. 이번 bundle에서 추가하지 않는다.
- 자체 리뷰에서 finite addition의 과거 tiered+omen refusal이 persistent pool cache에 남을 수 있음을 확인했다. Support의 pool projection namespace를 `solar-family-pool-v2-omen-composition`으로 변경해 새 규칙이 과거 refusal을 재사용하지 않게 한다. DB row/volume 삭제 없이 버전으로 분리하며 기능 확장이 아니라 변경한 AdditionRules의 직접 영향 경계다.
- 근거: `codex/omen-audit-20261004/OMEN_RULE_AUDIT_20261004.md`, `all-32-omens.json`, [Wiki Omen](https://www.poe2wiki.net/wiki/Omen), 기존 [서비스 범위](docs/workbench-service-scope-2026-10-04.md). 구현118과 검증완료113은 최종 검증 완료까지 구분한다. remote push/merge/deploy 없음.

- 검증 checkpoint: Backend `check generateJooq bootJar` PASS(362 unit/ArchUnit +6 Docker integration, failures/errors/skips0). FE npm ci/lint/typecheck/format:check/build PASS. 전체 unit344 중343 PASS/1 FAIL; 기존 MaterialStash의 ordinary Essence74 기대값은 원본19 families×4=76과 달랐다. 정확한76 및 제외2 ID·별도 special4 ID 검사를 추가했다. 마지막 복구 실행은 추가한 special ID의 순서를 잘못 적어21/22 PASS로 중단됐다. 원본 순서 Hysteria/Horror/Abyss/Breach로 정정했으나 **최종22개 재검증 미실행**이다. 제품 범위·검사를 삭제하거나 실패를 skip하지 않았다.
- 복구1은 Docker Desktop Testcontainers host override와 기존 serviceScope formatter/cache version 수정, 복구2는 위 기존 목록 테스트 수정이었다. 총2회 한도 도달 후 unit/API/browser를 다시 실행하지 않았다. 첫 Backend integration의 Ryuk 연결 실패는 공식 `host.docker.internal` override로 해결했다. 기존 container의 전체 환경변수 조회는 auto-review가 secret 노출 가능성으로 거부했고 실행·재시도하지 않았다; 공식 Testcontainers 문서와 새 runner의 명시 설정만 사용했다.
- API 최초 검사18 PASS: Solar ordinary/Greater/Perfect Chaos의 side-first 후보·동률1/N·ledger·두 omen 소모·unrelated 보존·weighted replacement, 대칭 Dextral. 다음 opposing-side atomic의 원시 배열 비교가 실패했다. 기존 `ItemState.canonical`의 정렬과 synthetic 배열 순서 차이가 원인일 가능성을 자체 리뷰에서 확인했지만, 응답 전체가 저장되지 않아 단정하지 않는다. **canonical 계약 대조와 나머지9-base/legacy5 API 검사가 재개 blocker**다. 실패 script/log를 보존했고 재시도 없음.
- Browser 최초 검사8 PASS/page error0: legacy5 기본숨김, Solar orange 후보·hover zero API·실제 Perfect Chaos pair 소모·비확률 composition 안내·film 이전 frame 보존·preview 해제. 다음 film 선택 직후 Previous의 이전 enabled 상태를 읽고 새 disabled 버튼을 기다리다 timeout30s. **선택 film 반영 대기를 추가한 뒤 남은9-base/legacy5/390px/reload 검사 재개 필요**. Screenshot 단계에 도달하지 않아 layout 검증은 미실행이다. Browser 오류를 제품 결함으로 단정하지 않는다.
- 검사 복사본과 최종 source537개 SHA256 일치(Backend213/Frontend324). 테스트 source의 마지막 literal 정정 이후 build/typecheck는 통과했지만 이것이 unit PASS를 대신하지 않는다. 사용자의18080/18081과 기존4 container는 유지됐고 이번18280/18281 project는 정상 stop했다. volumes/DB data 삭제 없음; heavy slot 반환. 구현118·검증113 구분 유지. [결과·실패·미실행 근거](docs/evidence/workbench-omen-composition-validation-2026-10-04.json), [모델 계약과 재개](docs/workbench-omen-composition-2026-10-04.md).
- 후속 한정 재개 준비: 부모가 제시한 사용자 `중단없이 진행` 명시 지시와 운영명세의 사용자 지시 우선 조항에 따라 세 harness repair만 추가 bounded1묶음으로 승인 범위 내 처리한다. 기본2회/모든 실패 이력은 보존하며 횟수를 stage/session으로 초기화하지 않는다. 새 action-time 승인이 반드시 필요하다는 이전 보고는 과도한 해석으로 정정한다. 자동심사 거부·live output/서버 보호는 그대로다. i18n shared slot 사용 중; **heavy 검사 미실행**.
- API source 재진단 정정: Solar synthetic 배열은 이미 canonical 순서였다. initial.state가 StateBucket인 반면 apply.state는 ItemState여서 optional sockets/quality null 필드가 추가되는 계약 차이를 확인했다. optional defaults와 기존 canonical order만 비교 입력에 맞추고 다른 필드/roll/fracture/atomic consumption assertions는 유지한다. Browser는 다른 film active/cursor/DOM 반영 대기, 같은 active 재선택 생략, 각 Previous 후 cursor 감소 대기를 추가했다. request/raw response와 UI/storage failure capture를 새 파일에 보존한다. [commit한 harness](scripts/qa/omen-composition-20261004/README.md)3개 Node syntax PASS; runtime pending. 제품 코드 변경 없음. slot grant 후 최종22와 API/browser를 순차 검증한다.

# I18N-001 — Six-language display localization / isolated Docker validation passed

Korean default; menu English → Korean → zh-CN → zh-TW → Japanese → Spanish. All six central service dictionaries implemented; ID-based PoE2DB terminology covers 144 active service items and nine bases, with one Spanish refined Necrotic name and two Spanish descriptions falling back to English. All 1,485 ordinary affix definitions now have verified templates in six locales (792 templates each). The 37 special/implicit gaps, multi-stat source-unit rolls, source/server text and English-only clipboard parsing remain explicit limitations. Crafting rules, service exclusions, API/film schemas and persisted game IDs are unchanged. Probability disclosure remains visible below the common craft-title on every tab. Required isolated Docker checks pass with 351 tests; six-language browser QA and visual review completed. Remaining translation gaps and the large dictionary bundle are documented follow-ups; integrated omen QA remains parent-owned. See [implementation and integration handoff](docs/i18n-2026-10-04.md) and [source evidence](docs/evidence/i18n-sources-2026-10-04.json). Reversible via the isolated branch's commit range; no DB migration or reset.

Final combined checkpoint: six-language branch integrated once into `workbench/20261002` with current Omen behavior preserved. Final isolated Docker npm/lint/type/format/build and 357 tests PASS; nine-base API640, Omen/locale-preview browser125 plus six-locale browser224, page errors0. Current UI has295 keys per locale (six added integration display keys). Source334 files and verified Backend JAR hashes match. Existing failure histories above remain historical; integration QA has no unresolved blocker. Owned18380/18381 services stopped, heavy slot released, user18080/18081 preserved. Next bundle remains26 catalysts max-quality-on-use/type replacement and collected UX. [Combined evidence](docs/evidence/i18n-omen-integration-validation-2026-10-04.json).

## WB-044 — Catalyst simulator policy and collected Workbench UX (2026-10-04)

후속 사용자 승인 범위(2026-10-04): Sapphire의 최소 source-backed 시작 아이템 편집과 refined13 실제 적용을 추가한다. 기존 "Jewel catalog 확장 제외" 및 "over-cap cap 제거 action 거절"은 이 후속 작업의 명시 지시로 대체한다. 이전 결정·실패 기록은 아래에 보존한다. [후속 범위와 근거](docs/workbench-refined-catalyst-2026-10-04.md).

- 사용자 결정: ordinary/refined catalyst 26종 모두 서비스 선택 대상으로 유지한다. 한 번 적용하면 현재 아이템의 검증된 최대 품질을 설정하고 기존 유형을 교체한다. 실제 게임의 1회 사용 증가량을 구현한 것으로 표시하지 않는다. 원본 affix 값은 보존하고 새 유형의 표시 효과만 한 번 계산한다.
- 권장 결정 적용: 품질 표시 소수는 기존 compound roll의 HALF_UP(정확히 절반이면 0에서 멀어짐) 정책과 맞춘다. Backend/Frontend `QualityRoundingPolicy`에 모아 게임 정밀도 검증 후 교체 가능하게 했다. 기존 source-unit/negative/mixed-stat 미검증 경계는 유지한다. rollback은 품질 정책 함수와 표시 버전 변경으로 한정 가능하다.
- 근거: [PoE2DB Catalysts](https://poe2db.tw/us/Catalysts)는 ordinary Ring/Amulet, refined Jewel 및 단일 유형 교체를 명시한다. 기존 검증 cap 계산을 재사용하며 Solar의 Breach cap40을 보존한다. 지원 catalog에 Jewel이 없어 refined13은 선택과 명시적 적용 거절까지 구현하며, Ring/Amulet에 우회 적용하지 않는다. Jewel catalog 확장은 이번 요청에 추가하지 않는 권장 선택이다.
- 열린 게임 검증: 품질이20을 넘을 때 cap을 제공하는 Breach 옵션을 제거한 뒤의 품질 처리. 해당 옵션을 제거할 수 있는 후보 집합에 한해 명시적으로 거절한다. 낮은 품질 및 cap 옵션을 제거할 수 없는 제작은 기존 지원 규칙대로 수행한다. 자동 clamp/reset을 추측하지 않는다. Catalysing/Necromancy 및 기존 제외 화폐는 미지원 그대로다.
- 권장 UX 적용: stash 위에 Select base/New craft를 노출한다. New craft는 현재 base/level의 새 Normal root를 만들어 이전 film을 보존한다. 과거 frame 제작은 기존 linear film 분리 규칙을 유지한다. 설명은 중앙 item 영역 아래, affix 카드와 분리한다. 여유 높이에만 최대24px을 추가하며 resize 시 다시 계산한다. 회색 Fractured와 숨은 의미 설명, Alt prefix/suffix 복귀를 유지한다. rollback은 해당 UI/CSS 변경으로 가능하며 저장 schema나 DB migration은 없다.
- 검증 진행 및 실패 이력은 [구현/검증 기록](docs/workbench-catalyst-ux-2026-10-04.md)에 기록한다. 이전 checkpoint와 실패 기록은 삭제하지 않는다.

## WB-045 — Sapphire 편집 경계 대기 / cap 삭제 품질 보존 정정

- 모바일 검토용 권장 결정 적용: Sapphire Magic/Rare 시작 아이템에서 옵션 없음 또는 검증된 Cast Speed suffix 하나만 입력한다. 실제 Jewel 전체 slot/pool/특수 Jewel/Unique 제작은 열지 않는다. Normal은 source rarity 목록에 없으므로 거절한다. catalog의0P/1S는 제품 편집 제한이다. 후속 증거와 사용자 결정으로 편집 범위를 확장하거나 Sapphire dispatch를 제거할 수 있다.
- source identity가 확인된 suffix의2–4% 표시 단위만 사용한다. CDN hover403 때문에 game stat ID는 미확인이다. 내부 `display_cast_speed_percent` key를 game stat로 주장하지 않는다. generation weight는0으로 두고 실제 게임 odds를 추측하지 않는다. Sibilant/Skittering 외11종의 NO_MATCH와 빈 아이템의 NO_MATCH는 정당한 결과다.
- 모바일 검토용 권장 정책 적용: Solar quality40의 Breach cap prefix를 accepted Annul/Chaos/replacement로 삭제한 경우 quality20으로 clamp하고 유형·살아남은 원본 roll을 보존한다. cap prefix를 제거 후보에서 제외하지 않는다. Omen filter 후 실제 선택된 후보와1/N odds는 그대로다. cap 재증가 시 자동 refill하지 않고 다음 catalyst 사용만 현재 max까지 설정한다.
- 증거 gap: 실제 인게임 cap modifier 삭제 직후 품질이 clamp/유지/reset되는지, 표시 precision과 special Jewel cap은 미확인이다. `QualityCapChangePolicy.DEFAULT`와 `REJECT_OVERCAP` toggle 및 `unverified-quality-cap-clamp-v1` ledger로 simulator 가정과 source 사실을 분리한다. 사용자 인게임 검증 후 policy/버전을 교체할 수 있고 DB 변경은 없다.
- 기존 UI/6언어/film/exclusions 유지. CraftSupport/StateExplorer, Desecration/Vaal/Hinekora/Liquid/Catalysing/Necromancy는 추가하지 않는다. 지원144는 최소 한 검증 base에서 동작하는 수이며 모든 base×currency 지원 의미가 아니다. 최종 QA 결과는 후속 문서/evidence에서 확인한다.

WB-045 최종 검증: 명시된 사용자 계속 작업 지시에 따라 bounded recovery 완료. 기존 1485 binding은 모두 그대로이며 source-backed Sapphire 1건의 identity/원문/stat 범위/6언어 coverage를 검증한다. Backend370+6, Frontend369 및 필수 검사/build, API2090, browser565, page error0 PASS. 6언어1440/390px·Rare 편집·cap film 화면 검토 완료. 실패 이력과 복구 내역은 [최종 evidence](docs/evidence/workbench-refined-catalyst-validation-2026-10-04.json)에 보존한다. 지원144는 최소 한 검증된 base에서의 positive 지원이며 모든 조합을 뜻하지 않는다. game clamp 증거와 full Jewel crafting pool은 여전히 판단/증거 gap이다. 격리 서비스 종료·heavy QA 해제·사용자18080/18081 보존; 원격 반영 없음.


WB-045 사용자 정정(2026-10-04): maximum40→20 시 stored quality40을 보존한다. 기존 clamp 가정은 superseded. 기본 정책과 API/film 입력 검증을 reachable quality 범위로 분리했고 Catalyst 반복/전환은 max(existing,currentCap) simulator 정책으로 기존40을 보존한다. 실제 Catalyst 전환 효과는 추가 근거 대기다. [정정 명세](docs/workbench-quality-preserve-2026-10-04.md), [다음 Jewel/Liquid 계획](docs/workbench-jewel-liquid-next-2026-10-04.md)을 따른다. Essence Jewel 제외 유지; Liquid27과 첫 Currency tab 기본 제작은 다음 fresh chat에서 재개한다. 이전 모든 조합 지원을 주장하지 않는다.

WB-045 cap 정정 최종 검증: Backend371+6, Frontend369/50files 및 필수 검사/build, API2108, browser594/pageerror0 PASS. cap20의40 보존·13종 Catalyst 반복/전환·잘못된 새 입력·원본29/표시41·film undo/redo/reload·6언어1440/390 검증. 실패3건과 source 동등성은 [정정 validation evidence](docs/evidence/workbench-quality-preserve-validation-2026-10-04.json)에 기록한다. local commit만 수행하고 격리 서비스 정상 종료·heavy QA 해제·사용자18080/18081 보존.


## WB-046 Sapphire 일반 생성 + Basic Liquid10 (2026-10-04)

이전 existing-suffix Annul/Divine만으로 JL-01을 완료하지 않는다. 이번 묶음은 Sapphire normal58(23P/35S), Magic1P/1S·Rare2P/2S, 기본 화폐18종과 Rare Sapphire Basic Liquid10의 실제 positive 경로를 구현한다. Crafted identity·one-per-item cap·side/family 충돌·Divine/Annul/Chaos·film/6언어를 포함한다. [구현 및 출처](docs/workbench-sapphire-generation-liquid-2026-10-04.md).

사용자 승인 가역 정책: 게임 weight 부재는 명시적 후보1/N으로 계산한다. DropChance1을 검증된 game weight로 주장하지 않는다. Liquid는 제거 후 보장 Crafted를 추가할 수 있는 legal 분기에서 균등 선택하며, 게임 무효 분기의 소비/재시도는 미검증이다. 기존 Crafted가 있는 두 번째 Liquid는 무소모 거절한다. Normal Sapphire 시작과 display-unit integer/Catalyst precision은 명시적 simulator 모델이다. 기존 quality40/cap20 보존과 max(existing,currentCap) 정책은 유지한다.

잔여: Ruby/Emerald/Diamond 및 Potent3·Ancient13, Contempt cap-loss overflow 유지/추가 슬롯·Ferocity opposite-side scaling·Melancholy tree 조건·Time-Lost radius와 Small/Notable 범위. Liquid27은 Basic13+Ancient13+unrelated Verisium1이며 raw outcome 수15/14와 currency 수13/13을 구분한다. Essence-on-Jewel·Fracturing/socket 특례·Vaal/Hinekora/Desecration은 계속 제외한다.

### 2026-10-04 remaining display localization

`18a2a3d` UI checkpoint 기반 별도 branch에서 active170/deferred50 이름, Liquid26 이름·설명, 베이스17 이름, modifier2314 bindings/templates1690을 6개 언어로 연결했다. 복합228 및 source-unit/negative103 표시 변환은 stat identity와 source endpoint를 검증했으며 canonical payload·quality·film·game mechanics·English import는 변경하지 않았다. 실제 API JSON 객체 키 순서 차이로 special locale fallback이 발생한 문제도 id/min/max의 엄격한 비교로 수정했다.

Spanish 공식 이름이 없는6항목은 자체 번역 provenance를 표시하고 deferred Verisium 설명은 English source로 명시한다. 신규 active 번역 누락은 pretest coverage guard로 차단한다. 미래 베이스 확대는 최고 티어 대표 기준이며 현재17종을 변경하지 않는다. 출처·예외·검증 이력과 독립 QA 결과는 [remaining localization handoff](docs/i18n-remaining-2026-10-04.md)를 따른다. remote push/merge/live deploy는 수행하지 않는다.

## WB-047 최상위 베이스의 첫 묶음 (2026-10-04)

사용자의 `최상위 베이스들로만`, `베이스 확장 작업 ㄱㄱ`에 따라 기존 17개의 ID/snapshot/film을 보존하고 Soldier Cuirass와 Imperial Greathelm을 별도 Workbench 베이스로 추가한다. Soldier의 최고 기본 Armour와 높은 레벨 implicit sidegrade를 구분한다. source tags를 상세 정의의 ordered Spawn Tags에 대입했고 현재 Body144/Helmet137 ModsView 일반 행·published weights도 기존 풀과 정확히 일치했다. 저티어 base 속성을 복사하지 않는다. source 기본 Armour570/374와 캐릭터 요구 레벨65/80·STR121/115를 six locales로 표시한다. 새 베이스를 old base로 alias하지 않으며 이름/요구치·snapshot source digest·supportedBases를 검토된 manifest로 생성한다.

가역 추천 결정: STR Body의 기본 방어도 대표는 Soldier이며 Warlord의 최대 요구 레벨을 단독 최상위 기준으로 삼지 않는다. 장신구와 innate skill 무기는 distinct implicit/skill 계열을 유지한다. 동급 방어도에 더 낮은 요구치가 있는 Gloves/Boots는 metadata 검토 전에 높은 레벨 이름을 우선하지 않는다. Runeforged/Runemastered·socket-transfer·capacity 변경·Runic Ward·아직 지원하지 않는 weapon/offhand class는 별도 검증 대상으로 남긴다. concrete 후보와 우선순위는 [범위·출처·검증 문서](docs/workbench-top-bases-2026-10-04.md)를 따른다.

이 묶음은 기존 class 규칙의 실제 성공 경로를 검증하는 확장이며 등록만으로 모든 재료 지원을 주장하지 않는다. deferred50·excluded mechanics·Solar Support/Explorer·quality40 보존·rounding·Shift/Alt·film storage는 변경하지 않는다. remote push/merge/deploy는 수행하지 않는다. 다음 묶음은 Massive Mitts 및 나머지 defence archetype부터 진행하는 것을 권장한다.

최종 검증 상태: Backend396/Frontend1749/Browser125/API87 통과, 6locale×2viewport×2base 화면24장 확인. 첫 commit의 `fractured:false` fixture 비교 중단은 부모가 명시적으로 승인한 bounded recovery로 해결했다. 생략/false의 실제 API 동등성과 모든 기존 상태·roll·소모 assertion을 유지했고 runtime registry 및 기존220 entry 보존도 통과했다. Imperial은 source 일반137+special1=138개이며 초기 기록의 special3을 바로잡았다. 실패 로그와 준비 오류·승인 근거는 위 문서에 보존한다. 제품 코드는 recovery에서 변경하지 않았다. 자체 QA service는 종료했고 원격 반영은 수행하지 않았다.

## WB-048 Wand nine skill-family expansion (2026-10-05)

Wand9 skill-family를 추가해 총71 bases를 제공한다. ordered Spawn Tags로 일반 pool을118/123/185개로 구분했고 기존 Attuned modifier ID·family·tier·range·weight를 보존했다. granted skill은 표시용 property이며 affix나 combat 계산으로 전환하지 않는다. Spanish Bone/Offering 요구치2건은 원문 증거를 유지하고 English canonical 값으로 표시한다. Infinite 특수 target은 source가 있으나 기존 Attuned 지원 범위를 유지했다. Twisted/Runic Fork/Runemastered의 실제 ordinary availability는 별도 확인 대상으로 남긴다. [Wand bundle](docs/workbench-wands-bundle-2026-10-05.md)에 출처·정책·정확한 다음 Sceptre7/Belt/class gap을 기록했다.

Backend441+integration6, Frontend1820, API539, Browser719가 통과했고228 screenshot을38 contact sheets로 pixels 검사했다. 기존62 initial 응답·film·번역·registry220/deferred50·Solar-only Support/Explorer·quality40/rounding·Shift/Alt·local orange를 보존했다. 첫 Backend accessor 오류와 첫 Frontend Node22 engine 거부 로그는 유지했다. 조건을 완화하지 않고 수정/Node24로 전체 검증을 통과했다. remote push/merge/deploy는 수행하지 않는다.
