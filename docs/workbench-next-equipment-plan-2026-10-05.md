# 다음 highest-tier equipment 후보

현재 Workbench 91종은 선택한 최고 tier와 distinct implicit/skill sidegrade를 제공한다. 전체 low-tier catalog를 채우는 계획이 아니다. [Fresh source inventory](evidence/belts-source-bundle-2026-10-05/next-equipment/inventory.json)와 [reviewed candidates](evidence/belts-source-bundle-2026-10-05/next-equipment/reviewed-candidates.json)는 원문 SHA·시각·URL을 보존하며 아직 import하지 않은 bounded 후보를 기록한다.

현재 released equipment 검증 대상으로 유지할 class는 Wands/Sceptres/Bows, Crossbows, One Hand Maces/Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields/Bucklers/Foci/Quivers, Gloves/Boots/Body Armours/Helmets, Rings/Amulets/Belts와 기존 Jewel classes다. [공식 0.5.0 checkpoint](https://www.pathofexile.com/forum/view-thread/3932540)는 현재 Talisman, Crossbow, Staff, Shield 등의 실제 skill/item 변경을 기록한다. [최신 공식 patch index](https://www.pathofexile.com/forum/view-forum/2212)는 2026-09-27의 0.5.5d를 포함한다. 공식 원문의 shell fetch는 HTTP403이었고 공개 web reader로 읽은 URL을 근거로 남긴다. class page/normal rarity flag만으로 특정 base의 드롭 가능성까지 확정하지 않는다.

| 다음 class | Bounded 최고 tier/sidegrade 후보 | import 전 확인할 경계 |
|---|---|---|
| Crossbows | Siege, Gemini, Elegant, Flexed, Desolate, Engraved | reload time·bolt/grenade implicit·complete pool·socket cap |
| One Hand Maces | Fortified Hammer, Strife Pick, Akoyan Club; source가 증명한 distinct implicit sidegrade | local weapon properties와 global implicit, dual wield/combat은 표시 밖 |
| Two Hand Maces | Ruination Maul, Fanatic Greathammer, Tawhoan Greatclub | complete class pool·attack speed·critical/range·implicit |
| Quarterstaves | Aegis, Bolting, Dreaming | distinct implicit와 높은 damage/defence sidegrade |
| Spears | Grand, Flying, Akoyan | implicit/attack property와 requirements |
| Staves | Permafrost, Reflecting, Dark, Ravenous, Perching, Sanctified에서 distinct innate skill의 최고 requirement variant | skill identity·source level·Spirit/reservation은 실제 증거 범위만 |
| Talismans | Maji, Fungal, Jade에서 highest-tier profile; 다른 innate skill은 family별 1종 | shapeshift skill identity·local weapon stats·socket source |
| Shields | Tawhoan Tower Shield, Golden Targe, Blacksteel Crest Shield | Str/StrDex/StrInt defence archetype·Block·movement·Raise Shield |
| Bucklers | Desert Buckler, 최고 tier의 source-backed distinct implicit sidegrade | Parry·block/defence·requirements·socket cap |
| Foci | Tasalian Focus; distinct implicit가 있으면 해당 family 최고 tier 1종 | Energy Shield·caster pool·requirements·socket cap |
| Quivers | Visceral, Volant, Penetrating, Primed 및 남은 distinct implicit마다 대표 1종 | physical/elemental/poison/accuracy sidegrade; Level55와100% pierce 분리 |

권장 다음 bundle은 Crossbow 6종으로 한 class의 complete ordinary pool·Essence/Omen restrictions·6locale·old/new film을 먼저 완료하는 것이다. 다음으로 Shields 3 defence archetypes + 최고 Buckler 1종 + Tasalian Focus를 검증하고, Quiver distinct implicit roster와 melee class를 이어간다. 이 순서는 나머지 class를 취소하거나 생략하는 결정이 아니다.

Claws, Daggers, One/Two Hand Swords, One/Two Hand Axes, Flails, Traps는 [Items index](https://poe2db.tw/us/Items)에 페이지가 있으나 이번 source 수집은 현재 released/craftable availability를 입증하지 못했다. 모두 별도 release-unverified 후보로 남긴다. 긍정적인 출시·ordinary drop provenance가 확인되기 전 import하지 않는다. unique-only, Runeforged/Runemastered, 특별 socket transfer, 추가 affix-cap 변경은 ordinary 최고 tier에 섞지 않는다.

각 bundle은 old IDs·canonical rolls·source locale data를 재사용하고 actual class tags와 ordered spawn rules를 검증한다. selection weight가 알려진 경우 해당 source field를 사용하고, 알려지지 않은 경우 valid candidate 제한 후 명시적인 1/N simulator model을 적용한다. modifier weight와 numeric roll 분포를 game-established fact로 표시하지 않는다. deferred50·excluded mechanics·Solar-only Support/Explorer는 별도 사용자 요청 전 그대로 유지한다.
