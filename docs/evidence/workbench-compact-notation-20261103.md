# Compact workbench and session notation integration — 2026-11-03

Baseline e82e246de18df1688e0eaba8a858b57a3c5b60ff; integration branch codex/workbench-compact-notation-20261103.
Initial worktree clean. Previous server/code/data remain until final verified artifact replacement.
Latest user instruction supersedes the original no-push boundary: after finishing, non-force push to HappyBird6/Exile-Hephaistos master, verify remote/CI, and clean only owned temporary caches/worktrees after data/commit/runtime-dependency checks. Browser automation deferred; no image collection; no container Docker socket mount. The primary existing integration checkout, shared caches and necessary recovery/data backups are not disposable auxiliary artifacts.

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

Final backend affected/domain/API/historical preflight: **163 tests /0 failures** including ArchUnit, Spotless and bootJar. Previous +1-model records remain read-only with exact definition/ledger/numerical-policy equality, immutable bytes, and rejected execution under the new manual policy. All805 compiled production source/resource files and612 JAR resources match the integration checkout. Three migrations are byte-identical to the previous server release. Final whole backend:1040 tests,1028 pass,12 unchanged baseline failure identities,0 new failures. This is not a green whole-backend result.

The B-worker query `docker image inspect eclipse-temurin:21-jdk-jammy --format '{{.Id}}'` was denied by automatic approval review before execution. Exact reason: the reviewer treated the Docker socket prohibition as also excluding host-daemon inspect. This query was not retried or delegated. Backend validation instead completed with the independently permitted local Temurin21.0.13+9 Windows JDK, using the official Adoptium archive checksum `a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9`. No Docker or database was used by the Java tests. DB integration/codegen are unexecuted under the existing restriction. The earlier explicit user approval for host server read/artifact replacement/container start or restart remains distinct from the prohibited container socket mount; the root host-server read and artifact/start operations were separately approved and completed within that existing grant. The image query remains unexecuted, and no container socket was connected.


## Local release and publish preparation

Server http://localhost:9001 is RUNNING, health UP, runtime code180045150b04ca9dec9b1f806ab73b8dd3b994db. Subsequent final commit records evidence only; production source is identical. JAR SHA25640b66d401f15172789c1fcdc0731e021e68bbaf9c0dbefa17f8f569a6c9b0902. Served index/JS/CSS and retained image hashes match final distribution. CSS includes compact width/shared slot/notation contracts and has no workbench-favorites-region selector.

Stateless API checks pass:0->20 in both equipment types and all three rarities;7/19->20;20/30/40/100 refuse unchanged;wrong types;pending omen preservation;normal Divine implicit preservation;bone hidden slot plus Exalted;direct option selection;Sanctification with hidden slot and subsequent refusal. No persisted-user QA writes. All54 original files retain exact hashes;0 missing/changed/added. Same container IDs/mount volumes/ports/configuration fingerprints, three unchanged migrations. DB/Redis/settings are not recreated or reset.

Origin verified as HappyBird6/Exile-Hephaistos. Fetched master4fb6b89d0f2319444742aac336f9b1a1832b515c is an ancestor of the candidate. Pre-push audit found no forbidden cache/data/settings/binary archive paths or credential patterns among183 changed paths. A final freshness/fast-forward check precedes the authorized non-force push.

Owned auxiliary cleanup follows remote/CI verification. A's1485 non-ignored entries are entirely its documented .verification-compact source copy; no unrelated untracked changes were found. B/C are clean. Worker snapshots/branches and private build caches may be disposed only after all worker commits are reachable remotely and no process/runtime mount refers to them. Primary existing integration checkout, previous unrelated worktrees, shared caches, original data/config and necessary recovery backups are preserved. The runtime uses copied container JAR/static assets, not worker source/cache mounts. Actual cleanup outcome is reported separately after execution; it is not claimed here in advance.
