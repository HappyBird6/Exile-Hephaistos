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

All three worker commits are merged locally: A9e37d2b0, Bed7ea96f, C82a10576. Shared patches were reviewed: the common CSS removals and imports/coordinates were applied manually because root had already relocated favorites/feedback. No duplicate wrapper or extra favorite height remains. The card/font/desktop width and narrow-layout contracts are in A's INTEGRATION.md.

Connected frontend checks:45 pass, final geometry/page connection37 pass. Final frontend: **155 files /2559 tests all pass**, one worker; lint/typecheck/format/build pass. Notation pointer/focus tooltip is retained even while a currency is selected, using a scoped mode of the same existing MaterialTooltip instance. Outside notation, selected-currency tooltip suppression is retained.

Final backend affected/domain/API/historical preflight: **163 tests /0 failures** including ArchUnit, Spotless and bootJar. Previous +1-model records remain read-only with exact definition/ledger/numerical-policy equality, immutable bytes, and rejected execution under the new manual policy. All805 compiled production source/resource files and612 JAR resources match the integration checkout. Three migrations are byte-identical to the previous server release. The final whole backend run is still in progress and its12 baseline failure identities are compared separately.

The B-worker query `docker image inspect eclipse-temurin:21-jdk-jammy --format '{{.Id}}'` was denied by automatic approval review before execution. Exact reason: the reviewer treated the Docker socket prohibition as also excluding host-daemon inspect. This query was not retried or delegated. Backend validation instead completed with the independently permitted local Temurin21.0.13+9 Windows JDK, using the official Adoptium archive checksum `a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9`. No Docker or database was used by the Java tests. DB integration/codegen are unexecuted under the existing restriction. The earlier explicit user approval for host server read/artifact replacement/container start or restart remains distinct from the prohibited container socket mount; the final server operations are still pending review/execution.
