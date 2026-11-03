# Workbench correction integration — 2026-11-03

Baseline: `4fb6b89d0f2319444742aac336f9b1a1832b515c`.
The existing localhost:9001 server remains on this baseline until integration passes.
New remote push is not authorized. Browser automation remains deferred.

## Independent ownership

| Owner | Request numbers | Exclusive files / boundary |
|---|---|---|
| A | 1, 2, 3, 7, 8, 9 | `stash/**`, `materials.ts`, `MaterialStash.test.tsx`; visibility module, navigation/favorites component contracts |
| B | 4, 5, 6, 18 | `AbyssPanel.tsx`, `desecrationCurrency.ts`, `abyssResponse.ts`, `abyss-workbench/**`, bone artwork; backend `AbyssCraftingState`, `DesecrationCurrency`, `AbyssCraftingPlan`, `AbyssProbabilityComparison` and dedicated tests |
| C | 12, 13, 16, 17 | Shared workbench API/state shape and quality modules; new derived-defence/sanctification helpers; backend item model, `WorkbenchSimulator`, `WorkbenchCurrency`, `WorkbenchOmen`, `CraftingEngine`, `WorkbenchService`, quality rules and relevant tests |
| D | 11, 14, 15, 19–25 | `ItemCard.tsx`, `item-card.css`, `itemCardData.ts`, `session-library/**`, `workbenchHistory.ts`, new slot-image/control components and tests |
| Integration | 10, all page connections | `CraftingPage.tsx`, `crafting.css`, `app/App.tsx`, global translations, `WorkbenchController`, registry/seal/mirror files and cross-feature tests |

Workers provide reviewed patches for integration-owned files and never write one another's worktrees.
Item state, numeric precision and compatibility changes have one owner (C).
B wraps or calls the existing plain simulator while preserving slot-aware Abyss state.
A exposes an omen activation callback; the integration uses the existing activation handler.
Sanctification engine/consumption metadata belongs to C; B preserves `activeOmens` IDs.
D renders the SANCTIFIED state and receives derived numeric properties from C through integration.

## Worker execution

The user requested gpt-6.1-sol / high / standard tier. Creation tools accept model and reasoning
but expose no service-tier field. Model and High were requested; standard tier remains unverified.
Existing durable-thread resume returned `invalid turn/start params: AbsolutePathBuf deserialized
without a base path`. The previous worker remained idle, so no duplicate turn was started.
Separate local project worktrees were created from the explicitly requested baseline instead.

- A: `01a19f10-baca-7f62-ba60-407670caa08b`, local worktree `2721`.
- B: `01a19f11-e072-7c60-876e-2a51b8fd9807`, local worktree `fdcc`.
- C: `01a19f12-02b7-76d2-a552-5b57ed48f231`, local worktree `df9a`.
- D: `01a19f12-cad1-7b90-831c-6d31761445ca`, local worktree `0ebd`.

## Meaning and verification

Prefix/suffix outcome randomness is independent from unrevealed modifier selection.
Reveal remains explicit user selection; no unpublished probabilities are fabricated.
Legacy/excluded resources are blocked for new selection, never deleted from historical records.
Defences use `(base + flat) * (1 + increased / 100) * (1 + quality / 100)` after matching
actual source stat locality/tags; stored precision is preserved and display uses one decimal.
Sanctification requires independent per-value factors in 78-122%; any simulator distribution,
rounding and target policy must be labelled as policy rather than claimed game facts.

Each item remains pending until both its worker implementation and common-page/API wiring
are verified. Targeted tests precede one final frontend whole run and affected backend tests.
Known prior whole-backend failures, denied DB/codegen validation and deferred browser QA
remain separate from new regressions. No host socket is mounted inside containers.
Before final server replacement, retain rollback JAR/assets and data backups; reuse container IDs,
DB/Redis volumes and settings; compare all original saved-file hashes after startup.

## Integration checkpoint: failed-use feedback

Request 10 implemented on the integration branch: passive currency/scope explanations removed.
Necessary currency controls and import failure details remain separate. Only a failed currency
application opens the feedback panel; successful/selection announcements remain in the live
region. Failure state is bound to the current revision and cleared on selection/history changes.
New failure/success history-preservation tests plus existing Workbench regression: **14/14 pass**.
Typecheck and lint pass. This is targeted verification, not final whole integration verification.

## A/D local integration checkpoint

A source commits `ad282129` and `f7b3652e` merged as `61b878aa`; D `29c1c0f4` merged as
`5dc15070`. Exclusive file sets were reviewed; their working trees were complete before merging.
Common-page patches were reconstructed against the frozen baseline and merged with the
integration changes. Conflicts in selection failure clearing and moved item placement were
resolved by retaining both access policy and revision-bound failure clearing, plus exact-ID art.
No historical JSON or live service data was edited.

Tabs, tooltip checkbox and 2x5 favorites are now inside the canvas. History/Favorite controls
are below the base row; disclosure is in the footer. Stale header/always-present feedback
expectations were updated to the new requests, without dropping meaningful assertions.
Initial related run: 56 tests / 52 pass / 4 stale UI failures. Corrected three-file run: 11/11 pass.
Additional page wiring/favorite-filter preservation run: 6/6 pass. Typecheck/lint pass.
The two added page tests check the real component connection and unchanged persisted Films.
Final whole-suite validation is pending B/C integration.

The user resolved the quality increment explicitly: **+1 percentage point per use for every
rarity**. C follow-up `40ec6084` implements `user-selected-equipment-quality-plus-one-v1`;
no 5/2/1 game rule is inferred. Both quality currencies are ACTIVE in the manual registry,
ordinary cap20, unknown-quality/type/condition refusal and pending-omen preservation retained.
They remain excluded from the frozen automatic candidate policy using its existing EXTRA IDs.


## B/C connection and compatibility

B `afbbacc6` and C `d9294b77`, `b49b0e40`, `40ec6084` are merged locally. Common patches
were reviewed before application. C history validation is applied once (D supplied the same
connection), and B's item copies retain the sanctification metadata. The right stash excludes
Gnawed bones; omitted bone side uses the constrained random-side adapter. A hidden-slot
item uses `/abyss/currency`, preserving its opaque slots through value-only changes and additions.
Reveal is a separate explicit user action followed by eligible-option selection.

Cranium and existing rune assets are connected. The user postponed the remaining image work;
seven missing bone images keep the existing fallback. No further attachment requests or blocked
image URL retries are part of this release.

The card uses exact source integers for catalyst eligibility and an exact rational composition
for sanctification/catalyst/reviewed Jewel effect. Only final display rounds to one decimal.
BigInt ratios are never persisted. Defence fields use the matched local base/flat/increased/quality
inputs, not observed header totals; imported source lines remain unchanged in stored observations.
Unknown current quality is not silently inferred.

New manual definitions previously invalidated historical automatic model equality. Read-only
compatibility now allows exactly these two added manual currencies and one omen plus the known
manual version suffix, requiring equality of every old definition/order, ledger, overrides and
numeric policy. It never restores an old execution cursor under a changed model. Basic transition
provenance excludes the manual-only suffix; supported automatic actions are unchanged. Actual
v8-v13 fixture original bytes and read-only results, current-schema old-model records, malformed
references/probability mass rejection and modern cancellation/resume are regression tested.
Null sanctification is normalized at the existing strict automatic HTTP boundaries; non-null
manual sanctification is refused there. Domain records remain free of Jackson annotations.

Stale API action counts increase by exactly two and full manual versions include the new policy.
Frozen catalog/golden oracles are retained: the definitions test appends only the three intentional
new definitions to its old fixture; registry parity restores only the three explicitly changed
metadata fragments for comparison against the unchanged original digest. Basic seeded parity
normalizes only the added null record field, preserving its old digest and random-draw checks.

## Direct user check after release

1. Change a base and check its slot image, computed defences and compact controls.
2. Check tabs/tooltips in the canvas, 4-column Essence/special row, and lower 2x5 favorites.
3. Use armour/weapon quality repeatedly; verify +1 each use and refusal at20 without history changes.
4. Apply a bone without selecting a side, add another modifier, reveal explicitly and choose an option.
5. Right-click Sanctification, use Divine on Rare, check the bottom condition and subsequent refusal.
6. Reload saved history; verify original rolls/factors, favorites/title/delete/Undo and Favorite-only view.

No browser automation is performed. Unresolved installed-Augment observations still require
resolution before slot-aware crafting; preserved source data is not guessed into modeled effects.
Final counts, preserved-file comparison and runtime hashes are recorded in the release evidence.


## Final frontend and affected-backend verification

Final frontend: **151 files / 2534 tests, all pass**, single worker. Lint, typecheck, Prettier
and production build pass. The first sandbox run started no tests because of temporary IPC
ENOENT; the native run exposed exactly three stale expectations (removed legacy opt-in UI and
two newly enabled quality identities). Those assertions were updated to the explicit requests;
the final full run above has no failures. No production behavior was relaxed for tests.

Affected Java/API/domain + historical compatibility preflight: **157 tests / 0 failures**;
Spotless and bootJar pass in Java21 without a Docker socket mount. Every one of the **805**
compiled production source/resource files is byte-identical to the integration checkout.
Final whole Java: **1036 tests / 1024 pass / 12 failures**, exactly the 12 effective baseline
failure identities; **0 new failures**. The historical 40-failure checkpoint predates the prior
release and is not the current baseline. Two retained failure messages changed: frozen base
fingerprints reflect intentional manual enum/initial metadata additions, and the raw-mapper
wire assertion includes the new nullable field. Production null omission and actual historical
original-document/strict HTTP regressions pass. No probability or result oracle was weakened.
See `workbench-corrections-20261103-validation.json` for identities and difference context.

DB integration and generateJooq are NOT EXECUTED: the earlier denied Docker socket connection
remains forbidden. No alternate socket path or security setting was used. This does not turn
unit/API tests into a database integration pass. Browser automation remains user-deferred.


## Local release completed

Runtime code revision: `7e0bc96c7db030e7d55d00b0a0c9699aab9202d2`.
Server: **http://localhost:9001**, health **UP**. Subsequent commit only records verification;
production sources and the served artifact are unchanged. JAR SHA256:
`99640ca543dd07ebadd77db02923feb10f03f799b34d731fc8ccced8926d8d3d`.

All 612 JAR resources match the checkout. The served index/JS/CSS, both quality currency icons,
secured Cranium and existing Fire Quiver image match the final distribution bytes. Read-only
health and fresh stateless API checks passed: all three rarities +1pp, 19->20 and cap refusal,
wrong classes, retained pending omen, ordinary Divine implicit preservation, default bone side,
unrevealed slot plus Exalted, explicit direct selection, Sanctification with hidden slot, retained
original rolls/factors, and refused subsequent edit. The registry serves all three new ACTIVE rules.

**54 before / 54 after / 0 changed or missing/added** user files. Container IDs, mount volumes,
ports and configuration fingerprints are unchanged. DB/Redis volumes were not recreated or
initialized. Prior JAR/static files/user files and a successful PostgreSQL custom-format dump are
retained privately for rollback; backup folders are Git-ignored. No persisted-user QA write,
browser action, remote push, public deployment or unrelated service change occurred.

The checklist maps all25 requests: 24 implemented and verified in code/local serving; request5
remaining images is user-deferred, with secured assets retained. Visual browser QA is deferred.
The full backend suite still has the explicitly listed12 baseline failures; it is not reported green.
