# Workbench design restoration — 2026-11-03

Baseline comparison: e82e246de18df1688e0eaba8a858b57a3c5b60ff → 6bcefa7de1528428ff35699e9f1b9964503d3512. This correction is local only; no remote push is authorized.

## Actual unintended changes and restoration

- A new `compactCurrencyPositions` changed the original three tier columns / five currency-family rows into seven columns / three rows. Removed that display-only override; CraftingPage again uses unchanged `currencies.ts` coordinates.
- The central item moved from (371,240), 193×274 to (489,340), 154×174 on the 935×550 upper plane. Removed the override; original position, size, frame and spacing apply again.
- Shrinking only the page width while retaining full-size controls triggered new container breakpoints at 1114px / 739px, moved the card below the stash, and replaced the shelf with a five-column grid. Removed those new reflows. Scale the original workbench composition with CSS zoom 0.75; retain the original viewport breakpoint at 1000px. Other workspace views are not zoomed, avoiding unrelated graph interaction changes.
- Removed incidental nav padding, item-input shape/spacing, and sidebar grid/spacing overrides. No new colors, backgrounds or border shapes are introduced. The diff from e82 did not change the underlying rarity palette; it is preserved.

## Requested behavior preserved

- Side whitespace, 75% workbench scale; the original 340px sidebar becomes 374px before zoom (relative 10% enlargement). Compensating text sizes remain readable.
- Favorites remain direct children of the canvas content, two rows of five, each 74×74 on the original coordinate scale. Their lower extension starts at y570, outside the unchanged upper shelf/item/stash; there is no separate favorites-region wrapper. This extra canvas height is necessary to preserve the original shelf and avoid overlap.
- Existing category tabs, stash, quality-to-maximum behavior, sessions, feedback in bench-lower, notation and delegated material tooltips remain connected.
- The selected currency cursor is portaled to body, so viewport client coordinates do not inherit the workbench zoom. Base preview, material tooltips and shelf overlay already use body portals.
- Narrow viewports use available width without a forced desktop minimum or reordered currency grid.

## Verification and scope

- Related frontend tests: 5 files / 17 tests pass after correcting the new cursor test to use the existing right-click selection interaction (ordinary keyboard-style click deliberately clears pointer state). The first attempted test asserted a cursor after that deliberate clear; no production failure was concealed.
- Type/lint/format/build results are recorded at the local checkpoint; final asset and user-file preservation verification is recorded in the private server backup.
- No backend source, rules, model, storage schema or user record changes. Backend tests/codegen are not rerun for this frontend-only correction. Browser automation remains deferred by user instruction; DOM/geometry checks are not visual browser QA.
- Scale semantics reference: https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/zoom (zoom affects layout, unlike transform scaling). No browser reset keyword is used, preserving user zoom accessibility.

Self review: compared the concrete baseline diff, coordinate rectangles and production changes. Additional History requests are intentionally excluded until this checkpoint is served and health verified.
