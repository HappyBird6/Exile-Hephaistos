## 2026-11-02 layout correction in integration (not yet served)

A's correction uses one existing category selector for the right content and places favorites
below the whole crafting surface. Focused 59/59 plus App 6/6 and lint/type/format/build pass.
The existing server remains at 8524b6b; C's correction commit is awaited before replacement.
See [the correction checkpoint](../evidence/workbench-layout-correction-20261102.md).

## 2026-11-02 integrated workbench

A/B/C/D and the supplied Fire Quiver image are integrated on `codex/workbench-integration-20261102`.
The final frontend has 2,465 passing tests. Backend impact tests 75/75 and DB integration 8/8 pass;
40 pre-existing backend failures remain, with no unresolved new failure. Local server: http://localhost:9001.
See [integration and 18 checks](../evidence/workbench-integration-20261102.md) and
[machine-readable validation](../evidence/workbench-integration-validation-20261102.json).
Historical evidence below is preserved.

> WB-052 최신 작업: [화폐 11종·징조 제한·직접 옵션 선택 및 저장/복원](../evidence/manual-reveal-20261102.md). 후보 3개 입력을 직접 선택으로 대체했습니다. 확률·무작위 결과를 만들지 않으며 Mark의 고등급 교체·검증되지 않은 조합·범위 밖 경로석은 별도 한계로 남습니다. 자동 브라우저·배포·push는 수행하지 않았습니다.

> 최신 구현·검증 및 직접 테스트 절차: [입력 상태 보존 완료 기록](../evidence/manual-workbench-input-state-20261102.md). 아래 목록은 134종 기록 호환 / 130종 신규 선택 범위의 근거입니다. 브라우저 자동 검증은 사용자 요청으로 미뤘으며, 훼손 화폐의 최신 상태는 위 WB-052 기록을 참조합니다.

# Manual workbench checkpoint — 2026-11-02

Base revision: `abfaffb504ab791e20882afda245bfc523a5128b` (remote master verified).

This is a catalog inventory and implementation checkpoint, **not a completed release scope**.
The registry contains 117 reviewed top entries and 17 legacy identities (134 total).
Evidence: `backend/src/main/resources/catalog/top-bases.json`, `base-policies.json`,
individual `catalog/*/catalog.json` and `docs/evidence/top-base-inventory-2026-10-04/`.

## Selection rules and release selector

Keep distinct implicit modifiers, granted skills, weapon behaviors and attribute archetypes.
Do not collapse entries by required level or display name. `Endgame` is present in the
source metadata identity for the entries marked below, not inferred from their name.
The user confirmed on 2026-11-02 that ethnicity tags do not define separate families:
STR armour-only helmets are one family, so Imperial replaces Rusted in new selection.
The selector has **130 entries**; the compatible catalog keeps **134 entries**.
The machine-readable list is [manual-workbench-roster-20261102.json](../evidence/manual-workbench-roster-20261102.json).

| Excluded from new selection | Replacement | Same intrinsic behavior; reviewed base stat |
| --- | --- | --- |
| Stocky Mitts (`stocky`) | Massive Mitts (`massive`) | STR armour gloves, Armour 15 → 187 |
| Crude Bow (`bow`) | Warmonger Bow (`warmonger`) | Bow, physical 6–9 → 56–84; same 5% crit / 1.2 APS |
| Rusted Cuirass (`body`) | Soldier Cuirass (`soldier`) | STR armour body, Armour 45 → 570; same movement penalty |
| Rusted Greathelm (`helmet`) | Imperial Greathelm (`imperial`) | STR armour helmet, Armour 29 → 374 |

Evidence: archived class inventories under `docs/evidence/top-base-inventory-2026-10-04/`,
`docs/evidence/armour-source-bundle-2026-10-04/reviewed-roster.json`, and
`scripts/review-armour-roster.mjs`. Cross-checked against the corresponding `body.lua`,
`helmet.lua`, `gloves.lua`, and `bow.lua` files in PathOfBuildingCommunity/PathOfBuilding-PoE2
`dev/src/Data/Bases` on 2026-11-02. This corroborates base properties, not every live game rule.
No other entries are grouped without matching intrinsic evidence. The table below remains the
full compatibility inventory; all entries except these four are in the release selector.

The 17 legacy identities remain readable and usable for old history. No saved data migration
or deletion is performed. No claim is made that this inventory covers every current game base.

Shrine Sceptre has three distinct registered identities (fire, ice, lightning). Name-only
clipboard resolution refuses these ambiguous entries. Existing base selection remains valid.

## Clipboard checkpoint boundaries

Exact unique registered names and matching classes dispatch to their own immutable catalog.
Source-unit values, fixed multi-stat implicits, unambiguous compound ranges and attached
advanced affix/tier metadata can map to catalog definitions. Multiline modifiers sharing one
metadata header stay one affix. Numeric ranges bind only to a unique matching stat range;
stat list order and unit conversion are never guessed. Fractured markers are retained.

Ordinary equipment quality now has an explicit nullable field; absence remains unknown.
Revealed desecrated modifiers and unrevealed prefix/suffix slots retain their identities.
Slot-aware manual Abyss import/validation handles these states; the ordinary endpoint still
refuses to flatten them. Its existing scenario persistence retains observed candidate offers.
Socket parsing is currently limited to the reviewed legacy Stocky socket policy.

Still incomplete: general socket properties, catalyst-adjusted roll reconstruction, ordinary
clipboard derived properties/skill lines, all-base action coverage and actual browser E2E.
Unknown/ambiguous input stays preserved and blocked, and this is **not** counted as lossless
import completion. UI automation was stopped by the computer-use tool's URL policy check;
no retry or alternate browser-control mechanism was used after that block.

## Registered identities

| Key | Name | Source identity | Family/class | Evidence/status |
| --- | --- | --- | --- | --- |
| soldier | Soldier Cuirass | Metadata/Items/Armours/BodyArmours/FourBodyStr3Endgame | body | Endgame |
| imperial | Imperial Greathelm | Metadata/Items/Armours/Helmets/FourHelmetStr7Endgame | helmet | Endgame |
| massive | Massive Mitts | Metadata/Items/Armours/Gloves/FourGlovesStr6Endgame | gloves | Endgame |
| sirenscale | Sirenscale Gloves | Metadata/Items/Armours/Gloves/FourGlovesInt6Endgame | gloves | Endgame |
| adherent | Adherent Cuffs | Metadata/Items/Armours/Gloves/FourGlovesStrInt4Endgame | gloves | Endgame |
| polished | Polished Bracers | Metadata/Items/Armours/Gloves/FourGlovesDex6Endgame | gloves | Endgame |
| blacksteel-gloves | Blacksteel Gauntlets | Metadata/Items/Armours/Gloves/FourGlovesStrDex4Endgame | gloves | Endgame |
| war-wraps | War Wraps | Metadata/Items/Armours/Gloves/FourGlovesDexInt1Endgame | gloves | Endgame |
| freebooter | Freebooter Cap | Metadata/Items/Armours/Helmets/FourHelmetDex7Endgame | helmets | Endgame |
| gladiatorial | Gladiatorial Helm | Metadata/Items/Armours/Helmets/FourHelmetStrDex6Endgame | helmets | Endgame |
| grinning | Grinning Mask | Metadata/Items/Armours/Helmets/FourHelmetDexInt6Endgame | helmets | Endgame |
| slipstrike | Slipstrike Vest | Metadata/Items/Armours/BodyArmours/FourBodyDex6Endgame | body | Endgame |
| death-mail | Death Mail | Metadata/Items/Armours/BodyArmours/FourBodyStrDex6Endgame | body | Endgame |
| sleek | Sleek Jacket | Metadata/Items/Armours/BodyArmours/FourBodyDexInt2Endgame | body | Endgame |
| vile | Vile Robe | Metadata/Items/Armours/BodyArmours/FourBodyInt3Endgame | body | Endgame |
| wolfskin | Wolfskin Mantle | Metadata/Items/Armours/BodyArmours/FourBodyStrInt2Endgame | body | Endgame |
| ancestral | Ancestral Tiara | Metadata/Items/Armours/Helmets/FourHelmetInt8Endgame | helmets | Endgame |
| cryptic | Cryptic Crown | Metadata/Items/Armours/Helmets/FourHelmetStrInt6Endgame | helmets | Endgame |
| tasalian | Tasalian Greaves | Metadata/Items/Armours/Boots/FourBootsStr6Endgame | boots | Endgame |
| drakeskin | Drakeskin Boots | Metadata/Items/Armours/Boots/FourBootsDex6Endgame | boots | Endgame |
| sekhema | Sekhema Sandals | Metadata/Items/Armours/Boots/FourBootsInt6Endgame | boots | Endgame |
| blacksteel-boots | Blacksteel Sabatons | Metadata/Items/Armours/Boots/FourBootsStrDex4Endgame | boots | Endgame |
| faithful | Faithful Leggings | Metadata/Items/Armours/Boots/FourBootsStrInt1Endgame | boots | Endgame |
| daggerfoot | Daggerfoot Shoes | Metadata/Items/Armours/Boots/FourBootsDexInt4Endgame | boots | Endgame |
| warmonger | Warmonger Bow | Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow8Endgame | bows | Endgame |
| guardian | Guardian Bow | Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow3Endgame | bows | Endgame |
| gemini | Gemini Bow | Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow6Endgame | bows | Endgame |
| fanatic | Fanatic Bow | Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow7Endgame | bows | Endgame |
| obliterator | Obliterator Bow | Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow9Endgame | bows | Endgame |
| kinetic | Kinetic Ring | Metadata/Items/Rings/FourRingB4 | rings | distinct identity |
| vitalic | Vitalic Ring | Metadata/Items/Rings/FourRingB2 | rings | distinct identity |
| mnemonic | Mnemonic Ring | Metadata/Items/Rings/FourRingB3 | rings | distinct identity |
| pearl | Pearl Ring | Metadata/Items/Rings/FourRing8 | rings | distinct identity |
| amethyst | Amethyst Ring | Metadata/Items/Rings/FourRing6 | rings | distinct identity |
| prismatic | Prismatic Ring | Metadata/Items/Rings/FourRing9 | rings | distinct identity |
| ruby-ring | Ruby Ring | Metadata/Items/Rings/FourRing3 | rings | distinct identity |
| two-stone-fire-cold | Two-Stone Ring | Metadata/Items/Rings/FourRing13a | rings | distinct identity |
| stellar | Stellar Amulet | Metadata/Items/Amulets/FourAmulet8 | amulets | distinct identity |
| amber | Amber Amulet | Metadata/Items/Amulets/FourAmulet3 | amulets | distinct identity |
| bloodstone | Bloodstone Amulet | Metadata/Items/Amulets/FourAmulet7 | amulets | distinct identity |
| lunar | Lunar Amulet | Metadata/Items/Amulets/FourAmulet6 | amulets | distinct identity |
| azure | Azure Amulet | Metadata/Items/Amulets/FourAmulet2 | amulets | distinct identity |
| crimson | Crimson Amulet | Metadata/Items/Amulets/FourAmulet1 | amulets | distinct identity |
| pearlescent | Pearlescent Amulet | Metadata/Items/Amulets/FourAmulet11 | amulets | distinct identity |
| hallowed | Hallowed Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13 | sceptres | distinct identity |
| bone | Bone Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand2 | wands | distinct identity |
| siphoning | Siphoning Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand4 | wands | distinct identity |
| volatile | Volatile Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand5 | wands | distinct identity |
| galvanic | Galvanic Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand6 | wands | distinct identity |
| acrid | Acrid Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand7 | wands | distinct identity |
| offering | Offering Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand8 | wands | distinct identity |
| critical | Critical Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand11 | wands | distinct identity |
| primordial | Primordial Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand12 | wands | distinct identity |
| dueling | Dueling Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand13 | wands | distinct identity |
| stoic | Stoic Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre2 | sceptres | distinct identity |
| omen | Omen Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre4 | sceptres | distinct identity |
| shrine-fire | Shrine Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre6a | sceptres | distinct identity |
| shrine-ice | Shrine Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre6b | sceptres | distinct identity |
| shrine-lightning | Shrine Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre6c | sceptres | distinct identity |
| clasped | Clasped Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre8 | sceptres | distinct identity |
| wrath | Wrath Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre10 | sceptres | distinct identity |
| linen-belt | Linen Belt | Metadata/Items/Belts/FourBelt2 | belts | distinct identity |
| wide-belt | Wide Belt | Metadata/Items/Belts/FourBelt3 | belts | distinct identity |
| long-belt | Long Belt | Metadata/Items/Belts/FourBelt4 | belts | distinct identity |
| plate-belt | Plate Belt | Metadata/Items/Belts/FourBelt5 | belts | distinct identity |
| ornate-belt | Ornate Belt | Metadata/Items/Belts/FourBelt6 | belts | distinct identity |
| mail-belt | Mail Belt | Metadata/Items/Belts/FourBelt7 | belts | distinct identity |
| double-belt | Double Belt | Metadata/Items/Belts/FourBelt8 | belts | distinct identity |
| heavy-belt | Heavy Belt | Metadata/Items/Belts/FourBelt9 | belts | distinct identity |
| utility-belt | Utility Belt | Metadata/Items/Belts/FourBelt10 | belts | distinct identity |
| fine-belt | Fine Belt | Metadata/Items/Belts/FourBelt11 | belts | distinct identity |
| invoking-belt | Invoking Belt | Metadata/Items/Belts/FourBeltB2 | belts | distinct identity |
| sinew-belt | Sinew Belt | Metadata/Items/Belts/FourBeltB3 | belts | distinct identity |
| forking-belt | Forking Belt | Metadata/Items/Belts/FourBeltB4 | belts | distinct identity |
| siege-crossbow | Siege Crossbow | Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow7Endgame | crossbows | Endgame |
| gemini-crossbow | Gemini Crossbow | Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow5Endgame | crossbows | Endgame |
| elegant-crossbow | Elegant Crossbow | Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow10Endgame | crossbows | Endgame |
| flexed-crossbow | Flexed Crossbow | Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow2Endgame | crossbows | Endgame |
| desolate-crossbow | Desolate Crossbow | Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow8Endgame | crossbows | Endgame |
| engraved-crossbow | Engraved Crossbow | Metadata/Items/Weapons/TwoHandWeapons/Crossbows/FourCrossbow4Endgame | crossbows | Endgame |
| tawhoan-tower-shield | Tawhoan Tower Shield | Metadata/Items/Armours/Shields/FourShieldStr10Endgame | shields | Endgame |
| golden-targe | Golden Targe | Metadata/Items/Armours/Shields/FourShieldStrDex10Endgame | shields | Endgame |
| blacksteel-crest-shield | Blacksteel Crest Shield | Metadata/Items/Armours/Shields/FourShieldStrInt7Endgame | shields | Endgame |
| desert-buckler | Desert Buckler | Metadata/Items/Armours/Shields/FourShieldDex9Endgame | bucklers | Endgame |
| tasalian-focus | Tasalian Focus | Metadata/Items/Armours/Focii/FourFocus10Endgame | foci | Endgame |
| visceral-quiver | Visceral Quiver | Metadata/Items/Quivers/FourQuiver11 | quivers | distinct identity |
| volant-quiver | Volant Quiver | Metadata/Items/Quivers/FourQuiver10 | quivers | distinct identity |
| penetrating-quiver | Penetrating Quiver | Metadata/Items/Quivers/FourQuiver9 | quivers | distinct identity |
| primed-quiver | Primed Quiver | Metadata/Items/Quivers/FourQuiver8 | quivers | distinct identity |
| serrated-quiver | Serrated Quiver | Metadata/Items/Quivers/FourQuiver7 | quivers | distinct identity |
| toxic-quiver | Toxic Quiver | Metadata/Items/Quivers/FourQuiver6 | quivers | distinct identity |
| blunt-quiver | Blunt Quiver | Metadata/Items/Quivers/FourQuiver5 | quivers | distinct identity |
| two-point-quiver | Two-Point Quiver | Metadata/Items/Quivers/FourQuiver4 | quivers | distinct identity |
| sacral-quiver | Sacral Quiver | Metadata/Items/Quivers/FourQuiver3 | quivers | distinct identity |
| fire-quiver | Fire Quiver | Metadata/Items/Quivers/FourQuiver2 | quivers | distinct identity |
| broadhead-quiver | Broadhead Quiver | Metadata/Items/Quivers/FourQuiver1 | quivers | distinct identity |
| fortified-hammer | Fortified Hammer | Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace8Endgame | one-hand-maces | Endgame |
| strife-pick | Strife Pick | Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace5Endgame | one-hand-maces | Endgame |
| akoyan-club | Akoyan Club | Metadata/Items/Weapons/OneHandWeapons/OneHandMaces/FourOneHandMace10Endgame | one-hand-maces | Endgame |
| ruination-maul | Ruination Maul | Metadata/Items/Weapons/TwoHandWeapons/TwoHandMaces/FourTwoHandMace8Endgame | two-hand-maces | Endgame |
| fanatic-greathammer | Fanatic Greathammer | Metadata/Items/Weapons/TwoHandWeapons/TwoHandMaces/FourTwoHandMace5Endgame | two-hand-maces | Endgame |
| tawhoan-greatclub | Tawhoan Greatclub | Metadata/Items/Weapons/TwoHandWeapons/TwoHandMaces/FourTwoHandMace10Endgame | two-hand-maces | Endgame |
| aegis-quarterstaff | Aegis Quarterstaff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff8Endgame | quarterstaves | Endgame |
| bolting-quarterstaff | Bolting Quarterstaff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff4Endgame | quarterstaves | Endgame |
| dreaming-quarterstaff | Dreaming Quarterstaff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourQuarterstaff10Endgame | quarterstaves | Endgame |
| grand-spear | Grand Spear | Metadata/Items/Weapons/OneHandWeapons/OneHandSpears/FourSpear8Endgame | spears | Endgame |
| flying-spear | Flying Spear | Metadata/Items/Weapons/OneHandWeapons/OneHandSpears/FourSpear5Endgame | spears | Endgame |
| akoyan-spear | Akoyan Spear | Metadata/Items/Weapons/OneHandWeapons/OneHandSpears/FourSpear10Endgame | spears | Endgame |
| permafrost-staff | Permafrost Staff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaffUnique1 | staves | distinct identity |
| reflecting-staff | Reflecting Staff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaffUnique3 | staves | distinct identity |
| dark-staff | Dark Staff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaff13 | staves | distinct identity |
| ravenous-staff | Ravenous Staff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaff14 | staves | distinct identity |
| perching-staff | Perching Staff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaffUnique4 | staves | distinct identity |
| sanctified-staff | Sanctified Staff | Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaff12 | staves | distinct identity |
| maji-talisman | Maji Talisman | Metadata/Items/Weapons/TwoHandWeapons/Talismans/Talisman7Endgame | talismans | Endgame |
| fungal-talisman | Fungal Talisman | Metadata/Items/Weapons/TwoHandWeapons/Talismans/Talisman5Endgame | talismans | Endgame |
| jade-talisman | Jade Talisman | Metadata/Items/Weapons/TwoHandWeapons/Talismans/Talisman10Endgame | talismans | Endgame |
| solar | Solar Amulet | Metadata/Items/Amulets/FourAmulet9 | Amulets | legacy compatibility |
| stocky | Stocky Mitts | Metadata/Items/Armours/Gloves/FourGlovesStr1 | Gloves | legacy compatibility |
| bow | Crude Bow | Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1 | Bows | legacy compatibility |
| wand | Attuned Wand | Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3 | Wands | legacy compatibility |
| body | Rusted Cuirass | Metadata/Items/Armours/BodyArmours/FourBodyStr1 | Body Armours | legacy compatibility |
| sceptre | Rattling Sceptre | Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1 | Sceptres | legacy compatibility |
| belt | Rawhide Belt | Metadata/Items/Belts/FourBelt1 | Belts | legacy compatibility |
| helmet | Rusted Greathelm | Metadata/Items/Armours/Helmets/FourHelmetStr1 | Helmets | legacy compatibility |
| ring | Iron Ring | Metadata/Items/Rings/FourRing1 | Rings | legacy compatibility |
| ruby | Ruby | Metadata/Items/Jewels/JewelStr | Jewels | legacy compatibility |
| emerald | Emerald | Metadata/Items/Jewels/JewelDex | Jewels | legacy compatibility |
| sapphire | Sapphire | Metadata/Items/Jewels/JewelInt | Jewels | legacy compatibility |
| diamond | Diamond | Metadata/Items/Jewels/JewelDiamond | Jewels | legacy compatibility |
| time-lost-ruby | Time-Lost Ruby | Metadata/Items/Jewels/JewelRadiusStr | Jewels | legacy compatibility |
| time-lost-emerald | Time-Lost Emerald | Metadata/Items/Jewels/JewelRadiusDex | Jewels | legacy compatibility |
| time-lost-sapphire | Time-Lost Sapphire | Metadata/Items/Jewels/JewelRadiusInt | Jewels | legacy compatibility |
| time-lost-diamond | Time-Lost Diamond | Metadata/Items/Jewels/JewelRadiusDiamond | Jewels | legacy compatibility |
