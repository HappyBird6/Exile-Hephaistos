# Rawhide Belt source preparation — 2026-10-03

This records source preparation checkpoint548ae261, before Workbench integration. Current runtime support is documented in [Belt integration](workbench-rawhide-belt-2026-10-03.md). No service dispatch, registry status, current-scope count, browser selector or runtime was changed. The six existing base catalogs and registry/scope/blocker ledgers are byte-preserved. At that preparation checkpoint, inventory remained 116 implemented overall and 108/155 within scope, with 47 pending and 65 excluded.

## Complete ordinary pool

The current [Belts table](https://poe2db.tw/us/Belts) contains 135 positive-weight ordinary rows. All 135 have exactly one public detail match for Name, effect, generation, family and modifier level, plus an eligible first matching ordered Spawn Tag for the belt base. The pinned PoB2 commit bb52d6b368307457eb9c54bb13f1829993d390b1 provides candidate Code locators only. Of 165 candidate codes, 119 cached public details were reused and 46 newly fetched; numerical definitions remain PoE2DB evidence.

The ordinary pool contains 53 prefixes (published weight 49,600) and 82 suffixes (70,700), total 120,300. Seven definitions have multiple stats. All 135 main-table DropChance weights differ from the applicable detail Spawn Tag weight. These sources are not mixed: complete main weights define the POE2DB_AS_PUBLISHED model; ordered detail weights determine eligibility only. Actual game probability and the origin of the difference are NOT_ESTABLISHED. Numeric source-unit and shared-ratio sampling remain UNVERIFIED assumptions when integrated.

[Rawhide Belt](https://poe2db.tw/us/Rawhide_Belt), Metadata/Items/Belts/FourBelt1, has DropLevel 1 and tag belt. It is the lowest class-index reviewed base. Linen Belt also has DropLevel 1 but a different recovery implicit and is not a second supported base. Rawhide's source card shows variable 20–30% increased Life Recovery from Flasks and an aggregate 1–3 Charm-slot range. These are source facts, not proven rolled values, item-level-specific distributions or computed effects. The raw socket string is not an Artificer/Charm operation rule. No quality maximum was published in the reviewed base fields; a cap of 20 must not be inherited from other classes.

## Individually matched Perfect result

| Material | Source code / family | Result | Modifier level / character level |
|---|---|---|---|
| [Perfect Insulation](https://poe2db.tw/us/Perfect_Essence_of_Insulation) | EssenceFireRecoupLife1 / FireDamageTakenRecoupedAsLife | Suffix, (26–30)% of Fire Damage taken Recouped as Life | 72 / 57 |

The exact underlying stat is `fire_damage_taken_goes_to_life_over_4_seconds_%`, integer bounds 26–30. This special definition is stored separately with weight zero, outside all normal rolls. There are 136 prepared definitions in total; no implementation status is inferred from the catalog's existence.

The source card requires Rare, removes a random modifier and adds the guaranteed result. Planned integration reuses the existing uniform eligible unlocked removal model (1/N), guaranteed addition (1), matching Sinistral/Dextral Crystallisation, atomic pair conflict handling, surviving-family/slot refusal and unrelated Omen/fracture preservation. The source modifier level 72 is a conservative simulator gate; character requirement 57 does not replace it or prove lower-level game eligibility. This step has not exercised that runtime path.

## Integration boundary and remaining evidence gaps

A narrow integration can expose explicit affix crafting and Perfect Insulation without inventing implicit or Charm rolls. It must label those base facts as unmodeled, leave quality limit unknown, reject Divine server-side and in response validation, and omit Blessed/Artificer support for this base. A full Divine operation cannot be declared supported while variable numeric implicits and Charm-slot behavior remain unmodeled. This is a model limitation, not a claim that the real game disallows Divine.

The historical [official 0.2.0f patch](https://www.pathofexile.com/forum/view-thread/3762929) describes level-dependent Charm-slot implicit changes, removal of additional-slot affixes and retained legacy items. It is historical context, not proof of current 2026 slot probabilities or Divine behavior. Current level-specific slot outcomes, their probabilities, implicit roll/re-roll relationships and a quality cap remain unresolved. Insanity keeps its existing Vaal dependency; no new deferral is introduced. Ring and Helmet remain later candidates.

Next integration work is the shared loader, seventh Workbench dispatch, one Perfect action, explicit Divine refusal, unknown-quality response contract, base selector/restored-film mapping and exact source-unit verification. Preserve the existing film schema, six old catalogs and 65 deferrals. Then run sequential Docker backend checks, actual API fixture capture, full frontend checks and browser repeat/cancel/history/reload/Alt/narrow/error cases before changing implementation counts.

## Executed checks

Docker source review verified 135 unique public matches, integer bounds and complete positive weights. Docker preparation validation verified ordinary/special separation, exact special code/stat/range/levels, all prepared JSON hashes, four freshly fetched source HTML hashes and 64 existing catalog-file hashes plus unchanged registry/scope/blocker ledgers. The first validation attempt could not resolve the Windows worktree Git pointer inside Linux; it made no source changes. The successful rerun uses a host-read baseline SHA manifest. Source/browser containers closed normally; no DB or volume was changed.

Backend, frontend, API and interactive browser tests were **not run for this source-only step**. Previous checkpoint test results are not presented as Belt validation. Machine-readable results: [source preparation evidence](evidence/workbench-belt-source-preparation-2026-10-03.json). Raw public evidence is bundled under `backend/src/main/resources/catalog/rawhide-belt/`; locator/fetch/review/build/validation scripts and source HTML remain under `codex/qa-20261003/belt-*`.
