# Workbench stash mockup alignment — 2026-11-03

Baseline: `8091b627` (served History/feedback follow-up).
Branch: `codex/stash-mockup-20261103`.

The user supplied an annotated 844×681 screenshot and clarified that the right
A margin must equal the left A margin, B2 must equal B1, and the right C gap
must equal the left C gap. The red rectangle extends the right stash down to
the last favorites row. Browser automation remains deferred by the user.

## Geometry

Coordinates refer to the existing 935×550 upper plane, before its existing
75% page zoom. The lower canvas extension remains unchanged.

| Constraint | Result |
| --- | --- |
| A: equal outer horizontal margins | 29 on each side |
| B: equal top offsets | 40 for the fixed currencies and right stash |
| C: equal group gaps | 46 between x283 and x329, and between x603 and x649 |
| Red rectangle bottom | y744, equal to favorites y570 + height174 |
| Right stash bounds | x649, y40, width257, height704 |

The old stash was constrained to the upper plane with fixed 12px/14px vertical
insets, ending above the favorites. Its right margin was also 12 instead of 29.
The panel now uses proportional coordinates for all four bounds and includes
its border/padding in those bounds. The resource grid fills the available
height while keeping its existing internal overflow and four columns.

No currency coordinates, item placement, colors, borders, typography, assets,
category behavior, favorites, History, game rules, backend or save formats changed.

## Validation

- Added a regression check using the actual CSS coordinates and catalog positions
  for A/B/C and the favorites bottom. The existing overlap check now reads the
  production stash bounds rather than an independent hardcoded rectangle.
- Related UI checks: 5 files / 28 tests passed.
- Typecheck, lint, format check and production build passed.
- The first sandbox test invocation could not read generated transform temp files;
  the same tests passed on the host. The initial offline dependency install lacked
  cached packages; the normal locked `npm ci` succeeded. No test was skipped.
- Self-review compared the diff with the screenshot and the user's A/B/C clarification.
- Whole frontend: 2,562 tests passed, zero failures or pending tests.

## Served result and preservation

- Updated localhost:9001 assets, then its entry document, after verifying that the
  previous server entry still matched the captured baseline. No service restart.
- HTTP-downloaded HTML, JS and CSS match the production build hashes.
- Entry assets: `index-DmXT9z5g.js` and `index-CJM6pnmD.css`.
- Backend health is UP. All 54 saved-file hashes are unchanged. Backend,
  PostgreSQL and Redis container identities, images, start times and mounts match
  their captured baseline. No data schema or service configuration change.
- Private local proof: `../runtime-proof/frontend-full.json` and
  `../runtime-proof/runtime-verification.json` relative to this worktree root.
- The initial publish attempt was rejected by automatic approval review for
  unclear authorization. After rereading the user's original instruction to
  serve UI changes immediately, the same operation was retried once and allowed.
- Browser visual verification and backend/DB tests were not run for this CSS-only
  change. A separate History/background follow-up has not been included here.

No temporary Docker container or image was created for this change. No remote push.
