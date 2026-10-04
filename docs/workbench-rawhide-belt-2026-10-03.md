# Rawhide Belt Workbench — 2026-10-03

Rawhide Belt is now a usable seventh Workbench base. It supports 18 ordinary currency paths and one Perfect Insulation path through the existing engine. Registry220 has117 implemented identities; the current development inventory155 has109 implemented and46 pending. The65 exclusions and eight already-implemented deferred Alloys remain unchanged. Preparation checkpoint548ae261 did not count this support; this integration checkpoint does. Registered inventory is not the complete game inventory.

## Source, catalog and probability boundary

The prepared [Rawhide Belt source evidence](workbench-rawhide-belt-source-preparation-2026-10-03.md) uniquely matches all135 positive-weight ordinary [Belt rows](https://poe2db.tw/us/Belts). There are53 prefixes with published weight49600 and82 suffixes with70700, total120300. One zero-weight special suffix makes136 definitions,53 prefixes/83 suffixes. Seven multi-stat ordinary definitions reuse the authorised UNVERIFIED shared-ratio tick/HALF_UP model; scalar source-unit sampling also remains UNVERIFIED. Special definitions never enter normal draws.

All135 applicable detail Spawn Tag weights differ from the main table. Complete main DropChance values define the POE2DB_AS_PUBLISHED model, while first matching ordered detail tags prove eligibility only. Actual game odds and the source of the difference remain NOT_ESTABLISHED. No mixed weight pool or invented distribution was introduced. Raw source hashes and exact effect/stat/family/generation/level details remain unchanged from preparation.

[Perfect Insulation](https://poe2db.tw/us/Perfect_Essence_of_Insulation) uses Code EssenceFireRecoupLife1, family FireDamageTakenRecoupedAsLife, suffix, `(26–30)% of Fire Damage taken Recouped as Life`. Its underlying source-unit stat is `fire_damage_taken_goes_to_life_over_4_seconds_%`. Source modifier level72 is a conservative simulator gate; character requirement57 is separate. Actual lower-level game applicability remains unverified.

The Rare operation uniformly removes one eligible unlocked modifier (model1/N), then adds exactly the guaranteed suffix (1). Sinistral/Dextral Crystallisation restrict the removal side, conflict atomically as a pair, and consume only the matching Omen on success. Fractured modifiers and unrelated Omens survive. A surviving family or slot conflict on any removal branch refuses the whole operation; invalid branches are not silently pruned.

## Explicit projection and data preservation

Only [Rawhide Belt](https://poe2db.tw/us/Rawhide_Belt), Metadata/Items/Belts/FourBelt1, is supported; this does not establish all Belt bases. Variable flask-recovery implicit and Charm slots are recorded as UNKNOWN_UNMODELED in registry base metadata and displayed as unknown on the item card. The source20–30% recovery range is a base fact, not a rolled value. The source aggregate1–3 Charm range does not establish a level-specific outcome or probability and is not displayed as an actual count. Numeric implicit projection is empty, with the explicit-only boundary stated in the UI and registry.

Divine is excluded from supported actions and explicitly refuses direct API calls, preserving the item, events and active Omens. The frontend also rejects a forged applied Divine response for this base. Blessed, Artificer, quality application, sockets/Charm operations, pasted Belt mapping and combat are unsupported. There is no inferred quality cap20: backend returns a null limit and the UI omits the maximum-quality property. Null limits are accepted only for this Belt projection; the six previous bases retain their cap checks. No claim is made that these unsupported operations are forbidden in the real game.

Existing strict request/state shape validation refuses unknown item properties and incompatible socket data before conversion or crafting. It does not silently drop provided Charm, flask, quality or other future fields. No item or history schema migration was added. The new selector/restored-film dispatch uses the same session-local linear films and preserves an original film's future when crafting from its past. No login or DB transition was introduced.

There are29 implemented Belt support records:18 currencies,10 Omens and one Essence. Divine and Blessed have no Belt support tag. All29 changed registry records are bounded to this addition;191 unrelated records and65 excluded records are unchanged. Existing six-base catalog files, snapshot identities and model weighting are byte-preserved. Craft Support/Explorer remain on their Solar catalog.

## Executed validation

Sequential project Docker checks passed:

- Backend331 unit/architecture cases plus6 integration cases, total337; zero failures/errors/skips, including catalog integrity, exact Perfect source, level/family/rarity refusal, uniform removal, both Crystallisation sides/pair conflict, fracture preservation, weighted normal draws, dispatch and atomic Divine refusal. Final registry resources were included in the final check/JAR.
- Frontend282 tests across39 files, plus lint, typecheck, format check and production build. New Belt tests consume actual API responses and reject missing/extra/out-of-range values, forged source or ledger, below-level/wrong-base responses, an invented quality cap and applied Divine.
- Actual API43 assertions, including existing six base counts/snapshots, complete published-weight probability, unknown-field refusal, Divine with/without unrelated Blessed,19 supported material actions and live117 registry count.
- Actual Chromium77 assertions, zero page errors: sourced Perfect effect, Shift repeat/refusal/cancel, crafting from a past Rare state into a new film with the original future preserved, byte-exact reload, unknown base facts/quality boundary, Divine refusal, Alt/keyup,390px source-level refusal, no document overflow, visible focused gold source link,503 preservation/retry, six previous-base operations and all-seven-base film reload. Perfect/desktop/narrow screenshots were visually reviewed. Browser and temporary validation containers closed normally.

The first frontend attempt stopped at a test fixture typing error. A later full run found the new initial-response fixture had been normalized for crafting and lacked the derived bucket field; it was replaced by a fresh, unmodified initial API response. The final full suite passed without loosening initial-state verification. No product behavior was changed after the successful frontend/API/browser checks.

Legacy browser30/cachepersist and exhaustive browser checks of every older material were not rerun. Testcontainers used their own disposable test databases; existing project/QA DB volumes were preserved. Only QA app/frontend services were replaced. No push, merge or deployment was performed. [Machine-readable executed evidence](evidence/workbench-belt-validation-2026-10-03.json); scripts, source HTML, API/browser results, screenshots and compatibility proof are under codex/qa-20261003/belt-*.

## Remaining work

Current46 principal barriers are other target17, rule/data22, engine state2 and reintroduction5. Insanity's ordinary Belt catalog gap is resolved, but eligible enchantment outcomes/corruption interactions and the deferred Vaal dependency remain; it stays pending without becoming a new exclusion. Perfect Mind needs a complete Ring catalog, Perfect Thawing a complete Helmet catalog, and Delirium still needs its full eligible Notable outcomes/allocation rules. Variable implicit/Charm probabilities and complete Divine semantics remain unknown. Ring/Helmet are future candidates, not implemented support.
