# Workbench v2 support inventory

Active rules: `solar-workbench-affix-v2`. Solar Amulet only. Registration is not implementation or current obtainability. Inventory: 220 entries; completeness not asserted. Implemented: 17 currencies + 8 omens; unsupported: 195. See [rules, ledger, and Support preparation](workbench-simulator.md).

## Implemented currencies and omens

| Name / ID | Category | Rule scope | Evidence |
|---|---|---|---|
| Orb of Transmutation (`Orb_of_Transmutation`) | CURRENCY | TRANSMUTATION; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Orb of Augmentation (`Orb_of_Augmentation`) | CURRENCY | AUGMENTATION; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Orb of Annulment (`Orb_of_Annulment`) | CURRENCY | ANNULMENT; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Regal Orb (`Regal_Orb`) | CURRENCY | REGAL; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Exalted Orb (`Exalted_Orb`) | CURRENCY | EXALTED; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Chaos Orb (`Chaos_Orb`) | CURRENCY | CHAOS; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Divine Orb (`Divine_Orb`) | CURRENCY | DIVINE; minimum modifier level 0 | https://poe2db.tw/us/Currency |
| Greater Orb of Transmutation (`Greater_Orb_of_Transmutation`) | CURRENCY | GREATER_TRANSMUTATION; minimum modifier level 44 | https://poe2db.tw/us/Currency |
| Perfect Orb of Transmutation (`Perfect_Orb_of_Transmutation`) | CURRENCY | PERFECT_TRANSMUTATION; minimum modifier level 70 | https://poe2db.tw/us/Currency |
| Greater Orb of Augmentation (`Greater_Orb_of_Augmentation`) | CURRENCY | GREATER_AUGMENTATION; minimum modifier level 44 | https://poe2db.tw/us/Currency |
| Perfect Orb of Augmentation (`Perfect_Orb_of_Augmentation`) | CURRENCY | PERFECT_AUGMENTATION; minimum modifier level 70 | https://poe2db.tw/us/Currency |
| Greater Regal Orb (`Greater_Regal_Orb`) | CURRENCY | GREATER_REGAL; minimum modifier level 35 | https://poe2db.tw/us/Currency |
| Perfect Regal Orb (`Perfect_Regal_Orb`) | CURRENCY | PERFECT_REGAL; minimum modifier level 50 | https://poe2db.tw/us/Currency |
| Greater Exalted Orb (`Greater_Exalted_Orb`) | CURRENCY | GREATER_EXALTED; minimum modifier level 35 | https://poe2db.tw/us/Currency |
| Perfect Exalted Orb (`Perfect_Exalted_Orb`) | CURRENCY | PERFECT_EXALTED; minimum modifier level 50 | https://poe2db.tw/us/Currency |
| Greater Chaos Orb (`Greater_Chaos_Orb`) | CURRENCY | GREATER_CHAOS; minimum modifier level 35 | https://poe2db.tw/us/Currency |
| Perfect Chaos Orb (`Perfect_Chaos_Orb`) | CURRENCY | PERFECT_CHAOS; minimum modifier level 50 | https://poe2db.tw/us/Currency |
| Omen of Whittling (`Omen_of_Whittling`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of Sinistral Erasure (`Omen_of_Sinistral_Erasure`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of Dextral Erasure (`Omen_of_Dextral_Erasure`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of Sinistral Exaltation (`Omen_of_Sinistral_Exaltation`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of Dextral Exaltation (`Omen_of_Dextral_Exaltation`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of Sinistral Annulment (`Omen_of_Sinistral_Annulment`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of Dextral Annulment (`Omen_of_Dextral_Annulment`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |
| Omen of the Blessed (`Omen_of_the_Blessed`) | OMEN | Single matching omen with ordinary currency; same-operation combinations and Greater/Perfect interactions are pending. | https://poe2db.tw/us/Omen |

Omen effects: Sinistral/Dextral Exaltation adds prefixes/suffixes; Sinistral/Dextral Annulment removes prefixes/suffixes; Sinistral/Dextral Erasure restricts Chaos removal to prefixes/suffixes; Whittling restricts removal to the lowest modifier level (uniform on ties); Blessed restricts Divine to implicit values. Matching omens are consumed only on success; unrelated ones remain.

## Unsupported entries

These entries are blocked for actual crafting. Inventory evidence does not establish implemented effects. Verification pending differs from data/network failure. Legacy reintroduction is not verified.

| Name / ID | Reason | Availability | Inventory source |
|---|---|---|---|
| Orb of Alchemy (`Orb_of_Alchemy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Orb_of_Alchemy |
| Vaal Orb (`Vaal_Orb`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Vaal_Orb |
| Fracturing Orb (`Fracturing_Orb`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Fracturing_Orb |
| Hinekora's Lock (`Hinekoras_Lock`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Hinekoras_Lock |
| Lesser Essence of the Body (`Lesser_Essence_of_the_Body`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_the_Body |
| Lesser Essence of the Mind (`Lesser_Essence_of_the_Mind`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_the_Mind |
| Lesser Essence of Enhancement (`Lesser_Essence_of_Enhancement`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Enhancement |
| Lesser Essence of Abrasion (`Lesser_Essence_of_Abrasion`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Abrasion |
| Lesser Essence of Flames (`Lesser_Essence_of_Flames`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Flames |
| Lesser Essence of Ice (`Lesser_Essence_of_Ice`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Ice |
| Lesser Essence of Electricity (`Lesser_Essence_of_Electricity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Electricity |
| Lesser Essence of Ruin (`Lesser_Essence_of_Ruin`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Ruin |
| Lesser Essence of Battle (`Lesser_Essence_of_Battle`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Battle |
| Lesser Essence of Sorcery (`Lesser_Essence_of_Sorcery`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Sorcery |
| Lesser Essence of Haste (`Lesser_Essence_of_Haste`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Haste |
| Lesser Essence of the Infinite (`Lesser_Essence_of_the_Infinite`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_the_Infinite |
| Essence of the Body (`Essence_of_the_Body`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_the_Body |
| Essence of the Mind (`Essence_of_the_Mind`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_the_Mind |
| Essence of Enhancement (`Essence_of_Enhancement`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Enhancement |
| Essence of Abrasion (`Essence_of_Abrasion`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Abrasion |
| Essence of Flames (`Essence_of_Flames`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Flames |
| Essence of Ice (`Essence_of_Ice`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Ice |
| Essence of Electricity (`Essence_of_Electricity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Electricity |
| Essence of Ruin (`Essence_of_Ruin`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Ruin |
| Essence of Battle (`Essence_of_Battle`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Battle |
| Essence of Sorcery (`Essence_of_Sorcery`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Sorcery |
| Essence of Haste (`Essence_of_Haste`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Haste |
| Essence of the Infinite (`Essence_of_the_Infinite`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_the_Infinite |
| Greater Essence of the Body (`Greater_Essence_of_the_Body`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_the_Body |
| Greater Essence of the Mind (`Greater_Essence_of_the_Mind`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_the_Mind |
| Greater Essence of Enhancement (`Greater_Essence_of_Enhancement`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Enhancement |
| Greater Essence of Abrasion (`Greater_Essence_of_Abrasion`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Abrasion |
| Greater Essence of Flames (`Greater_Essence_of_Flames`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Flames |
| Greater Essence of Ice (`Greater_Essence_of_Ice`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Ice |
| Greater Essence of Electricity (`Greater_Essence_of_Electricity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Electricity |
| Greater Essence of Ruin (`Greater_Essence_of_Ruin`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Ruin |
| Greater Essence of Battle (`Greater_Essence_of_Battle`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Battle |
| Greater Essence of Sorcery (`Greater_Essence_of_Sorcery`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Sorcery |
| Greater Essence of Haste (`Greater_Essence_of_Haste`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Haste |
| Greater Essence of the Infinite (`Greater_Essence_of_the_Infinite`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_the_Infinite |
| Perfect Essence of the Body (`Perfect_Essence_of_the_Body`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_the_Body |
| Perfect Essence of the Mind (`Perfect_Essence_of_the_Mind`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_the_Mind |
| Perfect Essence of Enhancement (`Perfect_Essence_of_Enhancement`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Enhancement |
| Perfect Essence of Abrasion (`Perfect_Essence_of_Abrasion`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Abrasion |
| Perfect Essence of Flames (`Perfect_Essence_of_Flames`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Flames |
| Perfect Essence of Ice (`Perfect_Essence_of_Ice`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Ice |
| Perfect Essence of Electricity (`Perfect_Essence_of_Electricity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Electricity |
| Perfect Essence of Ruin (`Perfect_Essence_of_Ruin`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Ruin |
| Perfect Essence of Battle (`Perfect_Essence_of_Battle`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Battle |
| Perfect Essence of Sorcery (`Perfect_Essence_of_Sorcery`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Sorcery |
| Perfect Essence of Haste (`Perfect_Essence_of_Haste`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Haste |
| Perfect Essence of the Infinite (`Perfect_Essence_of_the_Infinite`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_the_Infinite |
| Lesser Essence of Seeking (`Lesser_Essence_of_Seeking`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Seeking |
| Essence of Seeking (`Essence_of_Seeking`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Seeking |
| Greater Essence of Seeking (`Greater_Essence_of_Seeking`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Seeking |
| Perfect Essence of Seeking (`Perfect_Essence_of_Seeking`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Seeking |
| Essence of Hysteria (`Essence_of_Hysteria`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Hysteria |
| Essence of Delirium (`Essence_of_Delirium`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Delirium |
| Essence of Horror (`Essence_of_Horror`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Horror |
| Essence of Insanity (`Essence_of_Insanity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Insanity |
| Essence of the Abyss (`Essence_of_the_Abyss`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_the_Abyss |
| Essence of the Breach (`Essence_of_the_Breach`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_the_Breach |
| Lesser Essence of Insulation (`Lesser_Essence_of_Insulation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Insulation |
| Essence of Insulation (`Essence_of_Insulation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Insulation |
| Greater Essence of Insulation (`Greater_Essence_of_Insulation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Insulation |
| Perfect Essence of Insulation (`Perfect_Essence_of_Insulation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Insulation |
| Lesser Essence of Thawing (`Lesser_Essence_of_Thawing`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Thawing |
| Essence of Thawing (`Essence_of_Thawing`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Thawing |
| Greater Essence of Thawing (`Greater_Essence_of_Thawing`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Thawing |
| Perfect Essence of Thawing (`Perfect_Essence_of_Thawing`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Thawing |
| Lesser Essence of Grounding (`Lesser_Essence_of_Grounding`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Grounding |
| Essence of Grounding (`Essence_of_Grounding`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Grounding |
| Greater Essence of Grounding (`Greater_Essence_of_Grounding`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Grounding |
| Perfect Essence of Grounding (`Perfect_Essence_of_Grounding`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Grounding |
| Lesser Essence of Alacrity (`Lesser_Essence_of_Alacrity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Alacrity |
| Essence of Alacrity (`Essence_of_Alacrity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Alacrity |
| Greater Essence of Alacrity (`Greater_Essence_of_Alacrity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Alacrity |
| Perfect Essence of Alacrity (`Perfect_Essence_of_Alacrity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Alacrity |
| Lesser Essence of Opulence (`Lesser_Essence_of_Opulence`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Opulence |
| Essence of Opulence (`Essence_of_Opulence`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Opulence |
| Greater Essence of Opulence (`Greater_Essence_of_Opulence`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Opulence |
| Perfect Essence of Opulence (`Perfect_Essence_of_Opulence`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Opulence |
| Lesser Essence of Command (`Lesser_Essence_of_Command`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Lesser_Essence_of_Command |
| Essence of Command (`Essence_of_Command`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Essence_of_Command |
| Greater Essence of Command (`Greater_Essence_of_Command`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Greater_Essence_of_Command |
| Perfect Essence of Command (`Perfect_Essence_of_Command`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Perfect_Essence_of_Command |
| Runic Alloy (`Runic_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Runic_Alloy |
| Adaptive Alloy (`Adaptive_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Adaptive_Alloy |
| Protective Alloy (`Protective_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Protective_Alloy |
| Expansive Alloy (`Expansive_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Expansive_Alloy |
| Swift Alloy (`Swift_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Swift_Alloy |
| Cyclonic Alloy (`Cyclonic_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Cyclonic_Alloy |
| Prismatic Alloy (`Prismatic_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Prismatic_Alloy |
| Mystic Alloy (`Mystic_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Mystic_Alloy |
| Sovereign Alloy (`Sovereign_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Sovereign_Alloy |
| Celestial Alloy (`Celestial_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Celestial_Alloy |
| Transcendent Alloy (`Transcendent_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Transcendent_Alloy |
| The Runebinder's Alloy (`The_Runebinders_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/The_Runebinders_Alloy |
| The Runefather's Alloy (`The_Runefathers_Alloy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/The_Runefathers_Alloy |
| Omen of Dextral Crystallisation (`Omen_of_Dextral_Crystallisation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Dextral_Crystallisation |
| Omen of Sinistral Crystallisation (`Omen_of_Sinistral_Crystallisation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Sinistral_Crystallisation |
| Omen of Sinistral Alchemy (`Omen_of_Sinistral_Alchemy`) | GGG 0.3.0 states this omen can no longer be obtained. Subsequent reintroduction is not verified; legacy rule is not enabled. | NO_LONGER_OBTAINABLE_IN_0_3_0_PATCH | https://poe2db.tw/us/Omen_of_Sinistral_Alchemy |
| Omen of Dextral Alchemy (`Omen_of_Dextral_Alchemy`) | GGG 0.3.0 states this omen can no longer be obtained. Subsequent reintroduction is not verified; legacy rule is not enabled. | NO_LONGER_OBTAINABLE_IN_0_3_0_PATCH | https://poe2db.tw/us/Omen_of_Dextral_Alchemy |
| Omen of Sinistral Coronation (`Omen_of_Sinistral_Coronation`) | GGG 0.3.0 states this omen can no longer be obtained. Subsequent reintroduction is not verified; legacy rule is not enabled. | NO_LONGER_OBTAINABLE_IN_0_3_0_PATCH | https://poe2db.tw/us/Omen_of_Sinistral_Coronation |
| Omen of Dextral Coronation (`Omen_of_Dextral_Coronation`) | GGG 0.3.0 states this omen can no longer be obtained. Subsequent reintroduction is not verified; legacy rule is not enabled. | NO_LONGER_OBTAINABLE_IN_0_3_0_PATCH | https://poe2db.tw/us/Omen_of_Dextral_Coronation |
| Omen of Corruption (`Omen_of_Corruption`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Corruption |
| Omen of Greater Exaltation (`Omen_of_Greater_Exaltation`) | Effect text registered; candidate tag weighting, type-selection distribution, multi-add edge cases or interactions require verification. | Not asserted | https://poe2db.tw/us/Omen_of_Greater_Exaltation |
| Omen of Greater Annulment (`Omen_of_Greater_Annulment`) | GGG 0.3.0 states this omen can no longer be obtained. Subsequent reintroduction is not verified; legacy rule is not enabled. | NO_LONGER_OBTAINABLE_IN_0_3_0_PATCH | https://poe2db.tw/us/Omen_of_Greater_Annulment |
| Omen of Answered Prayers (`Omen_of_Answered_Prayers`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Answered_Prayers |
| Omen of Secret Compartments (`Omen_of_Secret_Compartments`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Secret_Compartments |
| Omen of the Hunt (`Omen_of_the_Hunt`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_the_Hunt |
| Omen of Reinforcements (`Omen_of_Reinforcements`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Reinforcements |
| Omen of Homogenising Exaltation (`Omen_of_Homogenising_Exaltation`) | Effect text registered; candidate tag weighting, type-selection distribution, multi-add edge cases or interactions require verification. | Not asserted | https://poe2db.tw/us/Omen_of_Homogenising_Exaltation |
| Omen of Homogenising Coronation (`Omen_of_Homogenising_Coronation`) | Effect text registered; candidate tag weighting, type-selection distribution, multi-add edge cases or interactions require verification. | Not asserted | https://poe2db.tw/us/Omen_of_Homogenising_Coronation |
| Omen of Sanctification (`Omen_of_Sanctification`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Sanctification |
| Omen of Catalysing Exaltation (`Omen_of_Catalysing_Exaltation`) | Effect text registered; candidate tag weighting, type-selection distribution, multi-add edge cases or interactions require verification. | Not asserted | https://poe2db.tw/us/Omen_of_Catalysing_Exaltation |
| Omen of the Sovereign (`Omen_of_the_Sovereign`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_the_Sovereign |
| Omen of the Liege (`Omen_of_the_Liege`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_the_Liege |
| Omen of the Blackblooded (`Omen_of_the_Blackblooded`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_the_Blackblooded |
| Omen of Putrefaction (`Omen_of_Putrefaction`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Putrefaction |
| Omen of Light (`Omen_of_Light`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Light |
| Omen of Sinistral Necromancy (`Omen_of_Sinistral_Necromancy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Sinistral_Necromancy |
| Omen of Dextral Necromancy (`Omen_of_Dextral_Necromancy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Omen_of_Dextral_Necromancy |
| Flesh Catalyst (`Flesh_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Flesh_Catalyst |
| Neural Catalyst (`Neural_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Neural_Catalyst |
| Carapace Catalyst (`Carapace_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Carapace_Catalyst |
| Uul-Netol's Catalyst (`Uul-Netols_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Uul-Netols_Catalyst |
| Xoph's Catalyst (`Xophs_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Xophs_Catalyst |
| Tul's Catalyst (`Tuls_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Tuls_Catalyst |
| Esh's Catalyst (`Eshs_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Eshs_Catalyst |
| Chayula's Catalyst (`Chayulas_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Chayulas_Catalyst |
| Reaver Catalyst (`Reaver_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Reaver_Catalyst |
| Sibilant Catalyst (`Sibilant_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Sibilant_Catalyst |
| Skittering Catalyst (`Skittering_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Skittering_Catalyst |
| Adaptive Catalyst (`Adaptive_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Adaptive_Catalyst |
| Necrotic Catalyst (`Necrotic_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Necrotic_Catalyst |
| Refined Flesh Catalyst (`Refined_Flesh_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Flesh_Catalyst |
| Refined Neural Catalyst (`Refined_Neural_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Neural_Catalyst |
| Refined Carapace Catalyst (`Refined_Carapace_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Carapace_Catalyst |
| Refined Uul-Netol's Catalyst (`Refined_Uul-Netols_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Uul-Netols_Catalyst |
| Refined Xoph's Catalyst (`Refined_Xophs_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Xophs_Catalyst |
| Refined Tul's Catalyst (`Refined_Tuls_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Tuls_Catalyst |
| Refined Esh's Catalyst (`Refined_Eshs_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Eshs_Catalyst |
| Refined Chayula's Catalyst (`Refined_Chayulas_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Chayulas_Catalyst |
| Refined Reaver Catalyst (`Refined_Reaver_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Reaver_Catalyst |
| Refined Sibilant Catalyst (`Refined_Sibilant_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Sibilant_Catalyst |
| Refined Skittering Catalyst (`Refined_Skittering_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Skittering_Catalyst |
| Refined Adaptive Catalyst (`Refined_Adaptive_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Adaptive_Catalyst |
| Refined Necrotic Catalyst (`Refined_Necrotic_Catalyst`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Refined_Necrotic_Catalyst |
| Diluted Liquid Ire (`Diluted_Liquid_Ire`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Diluted_Liquid_Ire |
| Diluted Liquid Guilt (`Diluted_Liquid_Guilt`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Diluted_Liquid_Guilt |
| Diluted Liquid Greed (`Diluted_Liquid_Greed`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Diluted_Liquid_Greed |
| Liquid Paranoia (`Liquid_Paranoia`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Liquid_Paranoia |
| Liquid Envy (`Liquid_Envy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Liquid_Envy |
| Liquid Disgust (`Liquid_Disgust`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Liquid_Disgust |
| Liquid Despair (`Liquid_Despair`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Liquid_Despair |
| Concentrated Liquid Fear (`Concentrated_Liquid_Fear`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Concentrated_Liquid_Fear |
| Concentrated Liquid Suffering (`Concentrated_Liquid_Suffering`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Concentrated_Liquid_Suffering |
| Concentrated Liquid Isolation (`Concentrated_Liquid_Isolation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Concentrated_Liquid_Isolation |
| Ancient Diluted Liquid Ire (`Ancient_Diluted_Liquid_Ire`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Diluted_Liquid_Ire |
| Ancient Diluted Liquid Guilt (`Ancient_Diluted_Liquid_Guilt`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Diluted_Liquid_Guilt |
| Ancient Diluted Liquid Greed (`Ancient_Diluted_Liquid_Greed`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Diluted_Liquid_Greed |
| Ancient Liquid Paranoia (`Ancient_Liquid_Paranoia`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Liquid_Paranoia |
| Ancient Liquid Envy (`Ancient_Liquid_Envy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Liquid_Envy |
| Ancient Liquid Disgust (`Ancient_Liquid_Disgust`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Liquid_Disgust |
| Ancient Liquid Despair (`Ancient_Liquid_Despair`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Liquid_Despair |
| Ancient Concentrated Liquid Fear (`Ancient_Concentrated_Liquid_Fear`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Concentrated_Liquid_Fear |
| Ancient Concentrated Liquid Suffering (`Ancient_Concentrated_Liquid_Suffering`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Concentrated_Liquid_Suffering |
| Ancient Concentrated Liquid Isolation (`Ancient_Concentrated_Liquid_Isolation`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Concentrated_Liquid_Isolation |
| Potent Liquid Melancholy (`Potent_Liquid_Melancholy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Potent_Liquid_Melancholy |
| Potent Liquid Ferocity (`Potent_Liquid_Ferocity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Potent_Liquid_Ferocity |
| Potent Liquid Contempt (`Potent_Liquid_Contempt`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Potent_Liquid_Contempt |
| Ancient Potent Liquid Melancholy (`Ancient_Potent_Liquid_Melancholy`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Potent_Liquid_Melancholy |
| Ancient Potent Liquid Ferocity (`Ancient_Potent_Liquid_Ferocity`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Potent_Liquid_Ferocity |
| Ancient Potent Liquid Contempt (`Ancient_Potent_Liquid_Contempt`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Ancient_Potent_Liquid_Contempt |
| Liquid Verisium (`Liquid_Verisium`) | Registered from display inventory; effect, eligibility and probability rules must be verified before application. | Not asserted | https://poe2db.tw/us/Liquid_Verisium |
| Blacksmith's Whetstone (`Blacksmiths_Whetstone`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Arcanist's Etcher (`Arcanists_Etcher`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Armourer's Scrap (`Armourers_Scrap`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Scroll of Wisdom (`Scroll_of_Wisdom`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Orb of Chance (`Orb_of_Chance`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Mirror of Kalandra (`Mirror_of_Kalandra`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Artificer's Orb (`Artificers_Orb`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Architect's Orb (`Architects_Orb`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Core Destabiliser (`Core_Destabiliser`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Vaal Cultivation Orb (`Vaal_Cultivation_Orb`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Yaomac's Orb of Sacrifice (`Yaomacs_Orb_of_Sacrifice`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Kopec's Orb of Sacrifice (`Kopecs_Orb_of_Sacrifice`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Kamasa's Orb of Sacrifice (`Kamasas_Orb_of_Sacrifice`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Yugul's Orb of Sacrifice (`Yuguls_Orb_of_Sacrifice`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Vaal Armourer's Infuser (`Vaal_Armourers_Infuser`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Vaal Blacksmith's Infuser (`Vaal_Blacksmiths_Infuser`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Vaal Arcanist's Infuser (`Vaal_Arcanists_Infuser`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Vaal Catalysing Infuser (`Vaal_Catalysing_Infuser`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |
| Orb of Extraction (`Orb_of_Extraction`) | Public equipment-crafting index candidate; canonical identifier, availability, base applicability and effect require verification. | Not asserted | https://poe2db.tw/us/Stackable_Currency |

## Uniform fallback ledger

Published Solar modifier weights govern modifier selection. No uniform substitution for unknown effects/pools. Partial known/unknown weights remain pending.

```json
{
  "id": "uniform-removal-v1",
  "candidateUnit": "eligible explicit modifier instance",
  "eligibility": "Validated Solar Amulet, ordinary Magic/Rare, no special conditions. All explicit instances; Chaos additionally requires Rare and replacement eligibility for every removal branch.",
  "n": "Current eligible explicit instance count",
  "probability": "1/N",
  "sourceUrls": [
    "https://poe2db.tw/us/Orb_of_Annulment",
    "https://poe2db.tw/us/Chaos_Orb"
  ],
  "reason": "Random removal stated; no published per-instance removal weights.",
  "scope": "MODELING_ASSUMPTION"
}
```

```json
{
  "id": "uniform-integer-roll-v1",
  "candidateUnit": "integer value in a single source stat range",
  "eligibility": "Selected published-weight modifier with exactly one stat; all integers from min through max inclusive in source units. Joint-stat domains are rejected.",
  "n": "max - min + 1",
  "probability": "1/N",
  "sourceUrls": [
    "https://poe2db.tw/us/Amulets#ModifiersCalc"
  ],
  "reason": "Source ranges define the modeled candidates; no numeric roll weights. Display units may differ from source units.",
  "scope": "MODELING_ASSUMPTION"
}
```

