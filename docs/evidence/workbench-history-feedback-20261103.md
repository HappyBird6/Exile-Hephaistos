# History, feedback and tooltip follow-up — 2026-11-03

## Required order observed

1. Restored original design at local commit `69e5e07612d20e18914f6131019121df7f8486e4`.
2. Served that exact frontend first on localhost:9001; HTTP/index/JS/CSS hashes, backend health UP, unchanged JAR and 54 unchanged user-file hashes confirmed in the private `server-design-restoration-20261103-backup/stage1-verification.json`. No service restart was required.
3. Only after that confirmation, began the new follow-up on `codex/workbench-history-feedback-20261103`.

## Changes

- History is an ordered CSS grid with two columns and row flow: 1 2 / 3 4 / 5 6. Source/keyboard order remains chronological. Current frame highlight and `aria-current` remain; current-step explanation is removed.
- Heading is exactly `History` in all supported locales. Removed the general currency-application description, retaining honest missing-record limitations and the empty state.
- A permanent 96px feedback region precedes History. Only actual failure messages create alerts; overflow scrolls inside the region. Adding/clearing a message cannot change the allocated height.
- The tooltip checkbox is a direct child of the canvas content, outside workbench-stash. It still controls material tooltips, including History.
- Existing delegated material tooltips now accept mouseover as well as pointerover/focusin. The old listener handled only pointerover/focusin; a mouseover-only event did not open the tooltip. Integration tests exercise the image child, while another currency is selected, keyboard focus, Escape, and checkbox disable/re-enable. This is code/DOM verification, not a claim that the user's exact browser failure was visually reproduced.
- Consumed omens stay with the actual successful currency application. No changes to Film records, cursor/branch rules, save formats, numerical rules, backend or existing image assets.

## Checks

Targeted checks: 7 frontend files / 58 tests pass. Final whole-frontend and static/build results are recorded in the companion validation JSON. Prior design-restoration targeted checks: 5 files / 17 tests pass, lint/typecheck/format/build pass.

Backend tests, DB integration/codegen and browser automation were not run for this frontend-only correction. Browser verification is deferred by the user; the existing prohibition on container Docker socket exposure remains unchanged. No remote push is authorized for either correction.

Self-review: checked changes against each follow-up request, preserved the served design checkpoint, verified ordered icons, failure/clear placement and tooltip access without changing user records. Final runtime asset and file-preservation proof is stored privately; no user payloads or settings are committed.
