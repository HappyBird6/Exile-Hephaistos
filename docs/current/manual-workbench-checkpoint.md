# Manual workbench checkpoint — 2026-11-02

Base revision: `abfaffb504ab791e20882afda245bfc523a5128b` (remote master verified).

This is a catalog inventory and implementation checkpoint, **not a completed release scope**.
The registry contains 117 reviewed top entries and 17 legacy identities (134 total).
Evidence: `backend/src/main/resources/catalog/top-bases.json`, `base-policies.json`,
individual `catalog/*/catalog.json` and `docs/evidence/top-base-inventory-2026-10-04/`.

## Selection rules and unresolved exclusions

Keep distinct implicit modifiers, granted skills, weapon behaviors and attribute archetypes.
Do not collapse entries by required level or display name. `Endgame` is present in the
source metadata identity for the entries marked below, not inferred from their name.
The bundled registry has no explicit upgrade/predecessor relation connecting the legacy
Stocky Mitts, Crude Bow, Rusted Cuirass or Rusted Greathelm to one specific endgame entry.
Those four are pending lineage review, rather than silently removed or upgraded.
The 17 legacy identities remain readable and usable for old history. No saved data migration
or deletion is performed. No claim is made that this inventory covers every current game base.

Shrine Sceptre has three distinct registered identities (fire, ice, lightning). Name-only
clipboard resolution refuses these ambiguous entries. Existing base selection remains valid.

## Clipboard checkpoint boundaries

Exact unique registered names and matching classes dispatch to their own immutable catalog.
Single-stat, source-unit verified values and fractured markers retain the existing strict checks.
Unknown bases/classes, ambiguous tiers/names, scaled units, multi-stat text, extra properties,
corruption and desecration/unrevealed markers still block the complete conversion; the original
clipboard remains available. None of those states is flattened into an ordinary item.
The frontend loads the matched catalog before validating and storing the imported state.
Implicit-free bases are now accepted. No automatic search currency restriction is changed.

Remaining release work: verify upgrade lineage and finalize the selector; model lossless
property/multi-stat and revealed/unrevealed desecration import; test all manual actions and
history/abort/failure paths in an isolated browser session. This checkpoint does not certify these.

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
