# Manual workbench input-state completion

This supersedes the implementation gaps listed at fcbb7d1. Browser automation is deferred at the user's request; it is not a completion gate. No push, deployment, or production restart is included.

## Implemented boundaries

- ItemObservations stores the copied socket count, exact installed-Augment effect lines (including their attached metadata), and catalog-recognized granted skills with explicit levels. Absence of a level/count remains unknown. Numeric levels are observations, not an inferred skill progression table.
- Observed sockets are separate from augmentSockets and Artificer execution capacity. Import does not widen the existing currency rules or reinterpret an observed maximum as an ordinary creation maximum. Counts survive ordinary crafting and Abyss slot operations.
- Augment effect lines are never put into explicit modifier slots or turned into guessed Augment identities/socket assignments. The complete item and its effects can be saved/restored. While identities and crafting implications remain unresolved, ordinary actions return applied=false with the identical state; Abyss mutation rejects the request without mutating the stored item. The UI gives a six-language hold notice. This is observation preservation, not support for using or replacing Augments.
- Shrine Sceptre resolves by exact catalog skill identity (Purity of Fire/Ice/Lightning), including a positive explicitly supplied level. Missing, conflicting and unknown skill evidence cannot select a variant. Frontend and backend use the same rule. Other catalog skill names also accept explicit levels; legacy Mana Drain and Skeletal Warrior names come from the bundled base-proof files.
- All 13 existing catalyst categories accept known English category labels; zero quality, nonmatching modifiers, and explicitly unscalable modifiers retain original values. Existing exact catalog/tier/stat matching still must yield one candidate. Positive affected values are not inverted through the explicitly provisional simulator rounding policy. The refusal states that original unscaled rolls, tier and verified rounding are needed. There is no newly verified positive inverse rule to enable.
- ItemObservations is optional, omitted from Spring JSON when absent, and defaults to unknown for legacy strict automatic request readers. Automatic requests with observations are explicitly rejected so their separate copy paths cannot lose manual state. Existing lower-base film identities remain intact.
- Original pasted documents remain film provenance and never masquerade as current recalculated damage/defence. The exact before-crafting text is visible in six languages. Known observations are immutable through supported currency transitions; response validation rejects dropped/altered observations.

## Evidence boundaries

No new socket capacity, skill progression, Augment probability, or catalyst rounding rule was introduced. Existing catalog sourceProperties/skillLines and attuned-wand/base-proof.json and rattling-sceptre/base-proof.json establish names only. Current catalyst descriptions at https://poe2db.tw/us/Catalysts corroborate category names but are community-extracted data, not proof of game rounding. Existing CatalystQuality.Type defines category semantics. Previously inspected current PoB socket limits were deliberately not copied into versioned crafting policy.

## Input limits and next information

- Effect-only Augment input may not establish unique names/counts or socket assignment. Preserve the source, then request the installed names/counts; verified version-aligned effect/transition data is also needed before enabling craft transitions for that configuration. No dummy mapping is used.
- Catalyst-affected displayed rolls without verified inverse semantics remain pending. A uniquely matched displayed number is not proof it equals the original roll.
- A skill with no copied level keeps level=null. Unknown skills/invalid levels or contradictory same-name-base evidence remain unmapped; the previous film is not overwritten.
- Original derived totals are retained, not recomputed after crafting. Full combat-stat recalculation is not part of this state-preservation work.

## User's manual check (run the new local code first)

1. Paste an actual supported, uncorrupted equipment item with Item Level, Quality and its copied Sockets line. Craft Transmutation → Augmentation → Regal, reload, and compare the saved socket/quality observations and original text.
2. Paste Shrine Sceptre with Grants Skill: Level 17 Purity of Fire, then repeat for Ice/Lightning. Verify the correct base and preserved level. Omit the skill and verify an information request without overwriting the old item.
3. Paste a rune effect line. Verify the item is saved, the effect is retained, and crafting is on hold rather than changing the item. Reload and verify the same state.
4. Import catalyst quality 0% and an unaffected positive-quality example. Check raw rolls after save/reload. Import a positive affected roll and verify the original remains available with an explanation of the required input.
5. Navigate backward in a film, branch a supported craft, cancel/repeat an operation, and reload. Check original provenance, fractures and desecrated slots; retain an existing lower-base film unchanged.

Follow-up currency scope is recorded in ISSUES.md WB-052: audit existing Abyss support first, then add only missing desecration currency/omen support. No currency scope was expanded in this change.


## Verification

- Backend final affected/compatibility run: 81 tests, 76 passed, 5 previously failing identities (BaseRegistryParityTest; two LegacyAutomaticResultReaderTest cases; PathSearchContractTest; PathSearchHttpTest). Integration 8/8 passed; spotlessCheck and bootJar passed. Evidence: task-root state-import-final-backend.log and state-import-final-backend-results.
- Final checked suites merged into the preceding full-run inventory: 980 unit tests, the same 40 failing identities, no new failing identity. This is not a fresh green full backend check. Evidence: state-import-final-validation-summary.json.
- Four final failure messages (including unchanged PathSearchHttpTest) match the earlier full-run messages exactly after removing the absent observations field. The registry seeded digest includes raw ItemState JSON, so its hash changes with a new nullable field; it is not claimed to have an identical signature or to verify every one of its 125 bases at runtime after its first assertion fails. Code review found legacy/null-observation branches unchanged; the separate original seeded Solar behavior digest passes with explicit null assertions and its original expected hash. Evidence: state-import-final-failure-comparison.json.
- Focused frontend: four files / 22 tests passed. First bounded full single-worker run: 127 files / 2407 tests passed in 282.27 seconds, before the final hold-notice clarification. No timeout or assertion was relaxed. The final notice source is being checked separately by the same bounded full-suite command.
- Final frontend lint, typecheck, prettier check, six-language build and production bundle passed. Existing large-bundle warning remains. Generated build files are not committed.
- Browser automation remains deferred by explicit user instruction. Code/API tests are complete independently of browser execution. Production services and user data were not touched.

- The final notice run found one real UI assertion failure: the new static hint duplicated the existing live region's role=status. Removed that redundant role from static hints, retained the existing announcement mechanism and every test assertion. Focused recheck: ItemTextImport + WorkbenchExtension, 22/22 passed. Evidence: state-import-final-ui-frontend.json and state-import-accessibility-recheck.log. The final full rerun is recorded below when complete.

- Final source after the accessibility correction: **127 files / 2407 tests passed, zero failures**, in the 15-minute bounded single-worker run. Final lint/typecheck/format/build also passed. Evidence: state-import-release-frontend.json, state-import-release-frontend.log, state-import-release-checks.log. The failed intermediate notice run is retained, not hidden. No tests were skipped and no expected output or time limit was weakened.
- Temporary backend containers were automatically removed. The user's two pre-existing mini-redis services, existing DB/volumes/settings and real saved records were preserved. No push, release, deployment or production restart.
