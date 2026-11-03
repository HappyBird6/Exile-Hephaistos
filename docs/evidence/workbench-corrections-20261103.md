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
- D: create request accepted; actual thread/worktree setup still pending at this checkpoint.

## Meaning and verification

Prefix/suffix outcome randomness is independent from unrevealed modifier selection.
Reveal remains explicit user selection; no unpublished probabilities are fabricated.
Legacy/excluded resources are blocked for new selection, never deleted from historical records.
Defences use `(base + flat) * (1 + increased / 100) * (1 + quality / 100)` after matching
actual source stat locality/tags; stored precision is preserved and display uses one decimal.
Sanctification requires independent per-value factors in 78–122%; any simulator distribution,
rounding and target policy must be labelled as policy rather than claimed game facts.

Each item remains pending until both its worker implementation and common-page/API wiring
are verified. Targeted tests precede one final frontend whole run and affected backend tests.
Known prior whole-backend failures, denied DB/codegen validation and deferred browser QA
remain separate from new regressions. No host socket is mounted inside containers.
Before final server replacement, retain rollback JAR/assets and data backups; reuse container IDs,
DB/Redis volumes and settings; compare all original saved-file hashes after startup.
