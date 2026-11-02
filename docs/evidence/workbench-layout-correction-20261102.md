# Workbench layout correction checkpoint — 2026-11-02

Status: A integrated and checked; C correction commit pending. Server replacement deferred.

## Source and preserved release

- Branch: `codex/workbench-integration-20261102`.
- Current running release: `8524b6b781651379a6cff9108525282a0691df25`.
- Preserved release reference: `codex/workbench-release-8524b6b`.
- A source commit: `d4e99cfd9b8733347d741a22305fed385fd4515a`.
- Integration cherry-pick: `9a53901` (no conflict).
- Other worker worktrees were read only. C catalog/registry/base-picker files were not modified.

## Corrected user requirement

The existing category selector alone controls the content displayed on the right.
There is no additional right-side tab selector. The fixed currency shelf remains in place.
Favorites form two rows of five below the whole crafting surface, outside both the right
content panel and the upper stash board. This replaces the earlier dual-selector/right-favorites interpretation.

## Reviewed expectation changes

The handoff `stash/TEST_EXPECTATIONS.patch` was read and checked before applying it.

- The right Currency panel has 30 entries: the existing 21, eight selectable bones and
  Artificer's Orb. The three Gnawed IDs remain excluded from new selection.
- The connection test now checks that the right panel has no tablist, the existing category
  selector changes that panel, and ten favorites are in the separate bottom region.
- App-level tab count is nine: three workspace tabs plus six category tabs, replacing 15.
- No assertions were removed or skipped; crafting/omen selection and favorite behavior remain tested.

## Checks

- Focused single-worker integration run: 59/59 pass, covering stash components, layout,
  MaterialStash, WorkbenchIntegration, DesecrationCurrency, Homogenising and WorkbenchExtension.
- App single-worker run: 6/6 pass.
- Lint, typecheck, format check and production frontend build pass.
- Responsive DOM/CSS checks cover 1280/800/375px contracts; they are not browser pixel verification.
- No backend change in this checkpoint. Full combined regression awaits C's completed correction.
- Existing 9001 server assets still match the previous release hashes and backend health is UP.
  No service was restarted or replaced, and no DB/configuration/saved-record mutation was performed.

## Next integration boundary

C is based on the same 8524b6b release. Preserve A's single `activeTab`, right-content-only
WorkbenchStash and whole-surface WorkbenchFavorites connection when integrating any shared
CraftingPage/crafting.css edits. Await the supplied C SHA, then run the final combined checks
before updating the existing local server. Browser automation remains deferred; no remote push
or public deployment is authorized.
