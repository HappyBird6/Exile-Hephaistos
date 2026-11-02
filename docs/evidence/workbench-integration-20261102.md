# Workbench integration — 2026-11-02

Base: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
Branch: `codex/workbench-integration-20261102`.

Source commits (independent completed worktrees):
- A `67c4c0bad22dba92a6032904025e8c8de2ac0d4c` → `304bc04`
- B `34324941939d70d2bbfb038b0f22a551510f4f2e` → `45f0393`
- C `c9b284aa81bdf769c4322b260f472efb196a2feb` → `b113976`
- D `cc8a29d058af0a18ba08f3c474ff1246059ed7d9` → `194ee93`
- C supplied Fire Quiver image `89df9f03a09e49b0a931316c50a360c55f227768` → `1c37cb1`

## Integration checklist

The 18 checks below cover the grouped requirements supplied by the parent thread.
They track implementation and validation separately; browser automation remains deferred.

| # | Requirement | Connection | Verification |
|---|---|---|---|
| 1 | Preserve outer tabs and add independent right internal tabs | CraftingPage + WorkbenchStash | Independent-tab DOM test |
| 2 | Bottom two rows of five favorites | WorkbenchStash | Ten-slot registration DOM test |
| 3 | Four columns with internal scrolling | workbenchStash.css | Component tests and CSS inspection |
| 4 | Greater/Perfect ordinary essence selection | Both selector filters | DOM and selection-policy tests |
| 5 | Bone currency creates an unrevealed slot | applyBone + recordAbyss | Domain/API tests |
| 6 | Explicit reveal followed by direct option selection | AbyssPanel | Interaction/API tests; no invented reveal odds |
| 7 | Remove free start/add-slot buttons | AbyssPanel | B interaction tests |
| 8 | Exclude three Gnawed currencies from new selection | Policy → both shelves/favorites | Selection-policy tests; historical IDs retained |
| 9 | Actual rune image for hidden slots | ItemCard + abyssCard | Supplied-image component and projection connection |
| 10 | Left overlay base selector | WorkbenchBasePicker | DOM and overlay-placement tests |
| 11 | Classification and base tooltip | base-picker | Component tests |
| 12 | Compact item-level input | WorkbenchBasePicker | DOM test |
| 13 | Session card list | SessionLibrary | Component and connection tests |
| 14 | Session favorite and title | Metadata → repository | DOM save and write-failure tests |
| 15 | Session deletion and Undo | Restore from next history | Exact-frame delete/restore DOM test |
| 16 | One-decimal display with full stored/export precision | numeric-display + ItemCard | Numeric and export-preservation tests |
| 17 | Divine preserves implicit modifiers; Blessed retains its existing exception | WorkbenchSimulator + client verification | Five new domain tests and related API regression |
| 18 | Apply catalysts through currency selection | Existing applyCurrency → recordCraft | DOM use/save/reload test and existing policy validation |

## Resource integrity

The repository enforces LF through `.gitattributes`. The committed manual-reveal JSON
has SHA-256 `f1e3af497c5527269607c481bd1947c7741f0c74b8cb6d8f2edff54f96b8cdf9`.
Its previous manifest entry incorrectly sealed a Windows CRLF working-copy hash.
Only that entry differed from the actual resources; it now seals the committed LF bytes.
The loader's exact-byte checks remain enabled. This changes content identity; historical
sessions retain their recorded identity and are never silently relabeled.

## Preservation and release status

Other worktrees were read only. No source worker branch was modified.
No browser automation, remote push, or public deployment.
The validated final artifacts were applied to the existing local app/frontend containers. PostgreSQL and Redis were not restarted. All four container IDs, volume mounts and port settings are unchanged. A logical PostgreSQL dump, previous JAR, previous frontend files and saved-file archive are preserved at `C:\Users\SSAFY\Documents\Codex\2026-11-02\task\server-integration-backup`. All 54 persisted app files have identical pre/post SHA-256 values. No persisted user record was used for QA. The three DB migrations are byte-for-byte unchanged; startup confirmed no migration was necessary.
The original Fire Quiver download returned HTTP 403. The subsequently supplied image was connected by C and is included in the final build (`c8809e1438f9f231be94-C4Iy8Xtg.webp`, 19,006 bytes). No blocked URL was retried by integration.

## Validation

Intermediate frontend connection tests: 31/31 pass.
First isolated backend subset after seal correction: 27/27 pass.
Final backend impact subset: 75/75 pass. Full unit run: 999 tests, 41 failures; 40 are the prior failure identities (25 messages differ only by ruleset hash; 15 are byte-for-byte unchanged). The one additional failure was the obsolete expectation that Divine can act on an item with only fixed numeric explicit values; it was corrected to assert refusal and unchanged state, and passes in the final impact subset. No unresolved new backend failure remains.
DB integration: 8/8 pass in both the full check and an independent temporary-DB run. `generateJooq` and `bootJar` completed. Final `spotlessCheck` passes after normalizing one test file from Windows CRLF to the repository LF policy. Exact-byte resource sealing remains enabled.
Final frozen frontend: **2,465/2,465 pass**, single worker. Lint, typecheck, format check and build pass. The 1,667-file source fingerprint comparison shows only LF normalization of one Java test after freezing; no frontend or runtime semantic change. Detailed machine-readable evidence: [validation](workbench-integration-validation-20261102.json).

## Running server and recovery

- URL: http://localhost:9001 (backend http://localhost:9000).
- Health: UP. JAR SHA-256: `9b633b3fa1efd415c27330d5a28024e94ebc05ac872f7107b884ab7d574af5b3`.
- Served JS/CSS and supplied Fire Quiver image hashes match the frozen build.
- Fresh, stateless API requests verified catalyst application, Divine implicit preservation,
  bone use producing a hidden slot, eligible-option lookup and direct option selection.
  No save endpoint or real saved crafting record was used.
- Rollback artifacts are in the backup directory above. If needed, stop only the existing
  app/frontend containers, copy `previous-app.jar` back to `/app/app.jar` and `html/.`
  back to `/usr/share/nginx/html/`, then start those same containers. Database restoration
  was not needed and was not performed. Preserve current volumes and configuration.
- No automatic browser validation, remote push or public deployment was performed.
- Reveal odds remain unknown and are not fabricated. Existing observed-side/removal/count
  inputs and verified pool constraints remain in effect; unverified game-rule combinations
  continue to reject the action while preserving the item.
