# Workbench integration — 2026-11-02

Base: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
Branch: `codex/workbench-integration-20261102`.

Source commits (independent completed worktrees):
- A `67c4c0bad22dba92a6032904025e8c8de2ac0d4c` → `304bc04`
- B `34324941939d70d2bbfb038b0f22a551510f4f2e` → `45f0393`
- C `c9b284aa81bdf769c4322b260f472efb196a2feb` → `b113976`
- D `cc8a29d058af0a18ba08f3c474ff1246059ed7d9` → `194ee93`

## Integration checklist

The 18 checks below cover the grouped requirements supplied by the parent thread.
They track implementation and validation separately; browser automation remains deferred.

| # | Requirement | Connection | Validation |
|---|---|---|---|
| 1 | Existing outer stash tabs | CraftingPage | Independent-tabs DOM test passes |
| 2 | Right internal stash tabs | WorkbenchStash | Component + DOM tests pass |
| 3 | Bottom two rows of five favorites | CraftingPage + WorkbenchStash | Ten slots, registration DOM test passes |
| 4 | Four columns, internal scroll | workbenchStash.css | Component tests; browser deferred |
| 5 | Greater/Perfect ordinary essence selection | isSelectableMaterial, outer filter | DOM test passes |
| 6 | Bone currency applies unrevealed slot | applyBone + recordAbyss | Final regression pending |
| 7 | Explicit reveal then direct option selection | AbyssPanel | Final regression pending |
| 8 | Remove free start/add-slot buttons | AbyssPanel | B tests; final regression pending |
| 9 | Exclude three Gnawed currencies from new selection | policy → both selectors/favorites | Final regression pending |
| 10 | Actual rune image for hidden slots | ItemCard + abyssCard | Final regression pending |
| 11 | Left overlay base selector | WorkbenchBasePicker | DOM test passes |
| 12 | Classification and base tooltip | base-picker | Component tests pass |
| 13 | Compact item-level input | WorkbenchBasePicker | DOM test passes |
| 14 | Session card list | SessionLibrary | Component tests pass |
| 15 | Session favorite and title | metadata → repository | DOM save test passes |
| 16 | Session deletion and Undo | next-history restoration | Exact-frame restore DOM test passes |
| 17 | One-decimal display, full stored/export precision | numeric-display + ItemCard | Final regression pending |
| 18 | Divine implicit protection and currency-based catalysts | WorkbenchSimulator + existing applyCurrency | Final regression pending |

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
Existing local server and actual saved data have not yet been changed by integration.
Fire Quiver source image returned HTTP 403; its documented fallback remains in place.

## Validation

Intermediate frontend connection tests: 31/31 pass.
First isolated backend subset after seal correction: successful (final counts to be recorded).
Final full frontend/backend/integration/codegen/build results: pending.
