# Craft Support frontend revision — 2026-10-16

Base commit: `7e64de72bcafce383e49fb798bffce54cbb691d4`. Backend, database, search rules and the unresolved temporary Regal/Exalted first-hit policy are unchanged.

## Changes

- Implicit stats are absent from target picker candidates/categories. Existing implicit goal rows are preserved, explained and block a new search until explicitly removed. Starting-item implicit data is unchanged.
- Method depth runs vertically downwards. The desktop inspector sits to the right; narrow screens stack it below the scrollable graph. Incremental nodes and parallel arrows remain visible.
- The inspector includes the selected candidate route. Clicking an arrow or outcome chooses that particular method; the incoming-method selector distinguishes alternatives at a merge. Initial actions, repeat-cycle order, omens and abstract quality work are listed separately. This is not a record of actual expenditure. The existing API does not provide realized repetition counts or the exact terminal primitive; the UI states that limitation and does not invent such a trace.
- Tab switching previously cleared `searchRequest` in an effect keyed by `active` and unmounted the method component through an `active` render condition. This was not a memory policy. Tabs now retain the component/input and pause scheduling new slices while hidden. A request already in flight may finish.
- Versioned localStorage saves the draft, goal, last workspace tab, job reference, selected node, cycle horizon and selected route (at most 64 method selections). It never stores the full graph, catalog or continuous revisions. Records expire after 24 hours and are capped at 128 KiB each. Corrupt/unsupported records are rejected, quota errors are visible, and expired/missing/ruleset-incompatible jobs require explicit recalculation with the input preserved. Reloaded jobs are fetched and await explicit continuation, preventing an automatic second search.
- Calculation badges distinguish preparation, queued slices, processing an ADVANCE request, pending stop/resume, confirmed stop, restored state, budget pause, unresolved partial results, confirmed completion and errors. Pending commands disable their buttons. Only an active calculation request animates; reduced-motion disables the animation. Labels and icons supplement colors. READY alone is not treated as proof that a calculation is currently executing, and incomplete results are not labeled complete.

## Verification

Final lint, typecheck, format check, all 98 test files / 2,180 tests and production build passed. The checked source manifest matches the working source with zero mismatches. Backend checks were not rerun because no backend source changed.

See `browser.json`, screenshots and `checks.json` in this directory. Actual Windows Edge checks used the existing localhost server with real HTTP responses: implicit candidates excluded; one created job across tab round-trip and reload; selected node/route and 300-cycle horizon restored; alternate incoming method selected; stop/resume confirmed; desktop panel to the right; mobile document without horizontal overflow; reduced-motion animation disabled; no browser page errors. The policy dialog was opened and closed without losing state.

Unit/component regressions cover corrupted/expired/oversized/quota-limited storage, existing implicit goal preservation, same-job restore without automatic creation/advance, expired-job explicit recalculation, alternative route selection, editor optional-field defaults, late cancellation responses, and waiting for server confirmation before claiming a stop.

The browser run found an editor-draft restoration defect: optional item fields had been validated as mandatory wire fields. Validation now supplies their documented defaults without modifying the saved draft, and a regression covers it. Earlier type/format/test-selector failures were corrected before final checks. The existing large JavaScript bundle warning remains.

## Local runtime

The test URL remains `http://localhost:8080/`. Only static frontend build files were updated; old hashed assets were retained for already-open tabs. The existing server, PostgreSQL/Redis and database volumes remain intact. Refresh the page to load the new frontend. No push, PR or external deployment was performed.
