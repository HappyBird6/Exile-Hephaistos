# Compact workbench and session notation integration — 2026-11-03

Baseline e82e246de18df1688e0eaba8a858b57a3c5b60ff; integration branch codex/workbench-compact-notation-20261103.
Initial worktree clean. Previous server/code/data remain until final verified artifact replacement.
Remote push not requested; browser automation deferred; no image collection; no Docker socket mount.

## Independent worktree owners

- A: Exile-Hephaistos | 화면·캔버스 배치 A — 01a19fc7-8c86-7e33-a67e-e76787436b26, worktree8b6b. Requests1/2/3/5/6/10. Own stash/**, item-card.css, compact-viewport/**; shared CSS/JSX changes supplied as patches only.
- B: Exile-Hephaistos | 품질 최대 적용 B — 01a19fc7-bb8a-7bb1-b853-54ae12919c9f, worktree1210. Request4. Own QualityCurrencyRules, WorkbenchSimulator, qualityCurrency helper and direct tests. Registry/seal/version compatibility remain root-owned.
- C: Exile-Hephaistos | 세션 화폐 기보 C — 01a19fc7-d161-7a81-8bc4-60683d7eb269, worktreeadd9. Requests8/9. Own session-notation/** including pure extraction, six locale copy, presentation and tests.
- Root: CraftingPage.tsx, crafting.css, workbenchHistory.ts and cross-feature tests; failure feedback relocation7, component wiring, registry/seal and historical read-only compatibility, final integration/regression/server.

All three were created with requested gpt-6.1-sol/high. No additional subworkers. Dedicated worktrees share the explicit baseline, with no edits in one another's trees.

## Contracts

Favorites become a direct child of stash-canvas__content; workbench-favorites-region is removed. The upper currency and favorite slots share dimensions. No global transform/zoom; responsive layout controls must retain readable fonts and prevent overflow.

Failure-only feedback becomes a direct bench-lower child after the item card. SessionNotation({film,cursor,resources}) follows it. The notation shows verified currency steps only through the current cursor and reuses data-material-tooltip delegation. No failed/cancelled request or reveal/observe operation becomes a currency use.

New recordAbyss frames explicitly retain consumedOmens:[] when none were consumed. Old missing fields are preserved unchanged and signify incomplete historical information; no zero or omen is guessed.

Quality uses user-selected-equipment-quality-maximum-v2. The ordinary equipment scope has cap20; cap40 belongs to the existing maximum-quality Amulet/catalyst path and is not generalized. At/above cap refuses without downgrading. Automatic candidate policy remains unchanged; previous +1 manual-model records must remain immutable read-only observations under a known-version adapter.

## Verification status

Implementation/integration in progress. A B-worker Docker image query was denied by automatic approval review; that query has been stopped and is not delegated to a different tool or worker. Exact command/reason is being retained, and permitted no-socket versus host access scope is being checked before final Java/server actions. Independent implementation continues.
