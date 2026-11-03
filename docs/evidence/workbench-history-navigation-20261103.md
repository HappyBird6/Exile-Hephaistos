# Complete History navigation and full-viewport background — 2026-11-03

Baseline: `8091b627dac4d06c6e93ac30009e91a5045f36e1`. The separate stash mockup `ad3f90ead8e1ec8c1ecf0d8a3d06e2ab9ff3b435` was confirmed as the actual served frontend and merged without conflicts at `30a4c80b`. Its stash A/B/C margins and height are preserved. No remote push is authorized.

## History behavior

The stored Film was not losing frames when browsing backwards: the display extractor explicitly iterated only through the cursor. It now projects every successful currency record in the active Film. All rows, numbers, consumed omens and their order remain visible at cursor 0, intermediate steps and the end. Only the current highlight moves. At the initial item no currency row is current; on a non-currency observation frame the last preceding currency row remains highlighted, matching the previous currency-step interpretation.

Every recorded currency row in the workbench is a native button that calls the existing `browse(frameIndex)`. This uses exactly the previous/next controls' storage, item-display and in-flight-operation guards. Native Enter/Space activation and ordinary pointer clicks therefore share that route. Rows are disabled during a currency request. Icons inside the button do not create nested tab stops; focusing the row offers the currency tooltip, and hovering an omen or currency still selects its individual tooltip.

No Film writer, model or storage format changed. Applying new currency while viewing an earlier frame retains the existing policy: start a new Film from that state; the old Film, including its entire future, remains unchanged in the session library. The new Film numbers its own applications from 1. Browsing an existing Film does not create a branch or truncate it. DOM/storage checks cover back/forward, direct jumps, keyboard-style activation, reload and original-session preservation on a real new branch.

## Source image and clipping findings

- Available local source: `frontend/public/assets/currency/stash-background.webp`, WebP VP8X, **936×936**, 24,518 bytes, SHA256 `cb03ca4a3e0fcab15d8d296d1a2c20064784f2dfb4a804cd7b06ebc22be787ee`.
- Inspected the actual image pixels with the local image viewer. The available file contains both its upper and lower decorative frame. No image editing or invented reconstruction was performed; there is no earlier uncropped master available to establish whether anything predating this file was removed.
- The effective image is on `.stash-viewport`; `.stash-canvas` later sets its own background to none. Existing `center/cover` crops the square source when the viewport is rectangular. Grid `align-items:start` also left the viewport shorter than a taller right-hand card/History column.
- Use explicit `background-size:100% 100%`, `background-repeat:no-repeat` and centered positioning, plus viewport `align-self:stretch`. Both gradient and image fill the entire left grid cell, including its longer blank lower area; the entire source is displayed at any aspect ratio as the user requested. No original image bytes, stash coordinates, colors, card sizes or 75% scale changed.
- A first computed-style test exposed shorthand/longhand handling returning auto sizes. The production rule was rewritten entirely with explicit background properties; the same strict size/repeat/stretch assertions then passed. The assertion was not weakened.

## Verification and preservation

Related History/storage/integration/mockup checks passed; final counts and static/build results are in the companion validation JSON. Final frontend verification uses a single worker. Browser automation remains explicitly deferred. Backend, DB integration and codegen are not run for these frontend-only changes; no Docker socket exposure is used.

Before serving, captured the actual ad3f90ea entry/assets, unchanged backend JAR, running service identities/start times/mounts and all 54 user-file hashes in the private `server-history-navigation-20261103-backup`. Publish must check the live entry still matches that baseline before replacing it. Final HTTP asset, API/health and preservation evidence is kept in that private backup; no user payloads or settings are committed.

Self-review: reviewed the complete diff, mockup merge, accessible navigation, current-row rules, immutable-session branch behavior and effective CSS clipping chain. No temporary Docker container or image was created, so there is no task-owned Docker resource to remove. Other workers' containers, caches, files and worktrees are outside this cleanup scope.

## Explicit shared-section style correction

The later user request identifies `frontend/src/app/styles.css`'s bare `section` rule. Removed only `margin-top:48px` and `border-radius:12px` from that rule. Its padding, border and background remain. Other selectors' deliberate 12px radii are unchanged. This removes a global margin that also affected positioned stash sections and History; the approved mockup's explicit proportional geometry remains authoritative. The two declaration removals happened during the final full run; they do not alter the tested JS/DOM behavior, and the final static checks/build include them. No additional implementation-mirroring test is added for this reversible two-line CSS removal.
