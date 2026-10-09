# Distinct implicit Amulet 7종 · 2026-10-04

기준 HEAD `233044a6e233fa3250de2483d860e63407b79308`, branch `workbench/top-bases-20261004`. 기존54개에 7개를 추가하여 Workbench는 **61개 base**다. Jewelry는 strict tier가 아니라 distinct implicit sidegrade다. Solar catalog·implicit·film과 모든 기존54개 identity를 유지한다. Support/Explorer는 Solar-only이며 active170/deferred50을 확장하지 않는다.

| Base | Type suffix | Character level | Implicit | Source family |
|---|---|---:|---|---|
| [Stellar Amulet](https://poe2db.tw/us/Stellar_Amulet) | FourAmulet8 | 25 | +(5—7) to all Attributes | AllAttributes |
| [Amber Amulet](https://poe2db.tw/us/Amber_Amulet) | FourAmulet3 | 8 | +(10—15) to Strength | Strength |
| [Bloodstone Amulet](https://poe2db.tw/us/Bloodstone_Amulet) | FourAmulet7 | 18 | +(30—40) to maximum Life | IncreasedLife |
| [Lunar Amulet](https://poe2db.tw/us/Lunar_Amulet) | FourAmulet6 | 14 | +(20—30) to maximum Energy Shield | IncreasedEnergyShield |
| [Azure Amulet](https://poe2db.tw/us/Azure_Amulet) | FourAmulet2 | 0 | (20—30)% increased Mana Regeneration Rate | ManaRegeneration |
| [Crimson Amulet](https://poe2db.tw/us/Crimson_Amulet) | FourAmulet1 | 0 | (2—4) Life Regeneration per second | LifeRegeneration |
| [Pearlescent Amulet](https://poe2db.tw/us/Pearlescent_Amulet) | FourAmulet11 | 8 | +(7—10)% to all Elemental Resistances | AllResistances |

Type prefix는 `Metadata/Items/Amulets/`, class는 `Amulets`, source base tag는 `amulet`, attribute requirement는 없다. Character requirement와 crafting itemLevel은 별개다. Crimson은 canonical 분당120–240을 유지하고 화면에서는 divisor60으로 초당2–4를 표시한다. Catalyst는 canonical 정수에 기존 central rounding을 한 번 적용하고 표시 단위로 변환한다. Lunar source `energy shield` Craft Tag는 기존 canonical `energyshield`로만 정규화한다. 원본 source에는 원래 표기를 보존한다.

[Source 검증](evidence/amulets-source-bundle-2026-10-04/verification.json)은 full ordinary209개의 stats/ranges와 ordered Spawn Tags eligibility, 6locale ordinary template 전체를 확인했다. Class DropChance와 detail Spawn Tags의 weight는 별도 domain이며 게시된 class weight를 기존 policy로 사용한다. 이 bundle의 ordinary weight는 모두 게시되어 있다. 알려진 weight가 없는 후보는 사용자 승인에 따라 **실제 source로 eligibility를 확인한 후보만1/N**이며 eligibility를 추측하지 않는다. 게임의 실제 확률을 독립 검증했다는 뜻은 아니다.

각 catalog는 ordinary209 + source-valid special8 + 해당 base implicit1이다. 일반 Essence24 action과 Perfect Infinite3/Enhancement1, Hysteria1, Abyss prefix/suffix2, Breach1의 exact source Code/Level/Family와 기존 full definitions를 재사용한다. Infinite의 표는 Strength/Dexterity/Intelligence를 묶어 설명하지만 결과는 각각의 source modifier다. Abyss의 Mark 약식 표시는 기존 full detail을 유지한다. Import 시 six-language ordinary template source 대조와 implicit placeholder/range 대조를 수행한다. Runtime은 source를 fetch하지 않는다.

일반 Catalyst13은 정확한 class identity와 Craft Tags에 적용된다. 기본 cap20, source Breach의 +20 maximum quality prefix가 있을 때만40이다. cap 손실 뒤 기존40은 보존하고 type switch/repeat에도 기존 overflow policy를 적용한다.41 이상은 거부한다. Source range·weight·original rolls·film은 scaling하지 않는다. 일반 currency, Fracturing, Alchemy, source-valid Essence/Omen을 적용하며 crossclass Essence, Artificer, refined Catalyst, deferred Alloy는 atomic refusal을 유지한다. Runic Alloy의 기존 Solar 준비 정의와 deferred entry는 유지하며 새 active 경로를 만들지 않는다.

이번 bundle에서 제외한 Amulet base는 Jade/Lapis(attribute sidegrade), Gold(rarity), Veridical Chain·Runemastered variants(Runic Ward 등), Lament/Portent/Absent(granted skill 및 affix capacity), Corona(socket-transfer), Dusk/Gloam/Penumbra/Tenebrous/Twisted/Distorted(capacity)다. 이는 새 mechanic를 deferred 목록에 추가한 것이 아니라 이번7종에 포함하지 않은 roster의 명시적 기록이다. [전체 saved roster](evidence/top-base-inventory-2026-10-04/Amulets.json)를 유지한다.

## 다음 exact source roster

다음 표는 이번 runtime에 추가하지 않은 source acquisition 결과다. Wand/Sceptre skill은 표시용 source family이며 combat 효과·level scaling·implicit 의미를 추측하지 않는다. 동일 family에서도 highest requirement가 strict power tier임을 보장하지 않는다. 모든 variant를 독립 identity로 유지해야 한다.

| Class | Base | Exact Type suffix | Source card |
|---|---|---|---|
| Belts | [Golden Obi](https://poe2db.tw/us/Golden_Obi) | BeltDemigods1 | BeltsRequires:  Level 12Has (1—3) Charm Slots(20—30)% increased Rarity of Items found |
| Belts | [Rawhide Belt](https://poe2db.tw/us/Rawhide_Belt) | FourBelt1 | BeltsHas (1—3) Charm Slots(20—30)% increased Life Recovery from Flasks |
| Belts | [Utility Belt](https://poe2db.tw/us/Utility_Belt) | FourBelt10 | BeltsRequires:  Level 55Has (1—3) Charm Slots20% of Flask Recovery applied Instantly |
| Belts | [Fine Belt](https://poe2db.tw/us/Fine_Belt) | FourBelt11 | BeltsRequires:  Level 62Has (1—3) Charm SlotsFlasks gain 0.17 charges per Second |
| Belts | [Linen Belt](https://poe2db.tw/us/Linen_Belt) | FourBelt2 | BeltsHas (1—3) Charm Slots(20—30)% increased Mana Recovery from Flasks |
| Belts | [Wide Belt](https://poe2db.tw/us/Wide_Belt) | FourBelt3 | BeltsRequires:  Level 14Has (1—3) Charm Slots(20—30)% increased Flask Charges gained |
| Belts | [Long Belt](https://poe2db.tw/us/Long_Belt) | FourBelt4 | BeltsRequires:  Level 20Has (1—3) Charm Slots(15—20)% increased Charm Effect Duration |
| Belts | [Plate Belt](https://poe2db.tw/us/Plate_Belt) | FourBelt5 | BeltsRequires:  Level 25Has (1—3) Charm Slots+(140—180) to Armour |
| Belts | [Ornate Belt](https://poe2db.tw/us/Ornate_Belt) | FourBelt6 | BeltsRequires:  Level 31Has (1—3) Charm Slots(10—15)% reduced Charm Charges used |
| Belts | [Mail Belt](https://poe2db.tw/us/Mail_Belt) | FourBelt7 | BeltsRequires:  Level 40Has (1—3) Charm Slots(10—15)% reduced Flask Charges used |
| Belts | [Double Belt](https://poe2db.tw/us/Double_Belt) | FourBelt8 | BeltsRequires:  Level 44Has (1—3) Charm Slots(20—30)% increased Charm Charges gained |
| Belts | [Heavy Belt](https://poe2db.tw/us/Heavy_Belt) | FourBelt9 | BeltsRequires:  Level 50Has (1—3) Charm Slots(20—30)% increased Stun Threshold |
| Belts | [Runemastered Heavy Belt](https://poe2db.tw/us/Runemastered_Heavy_Belt) | FourBelt9VerisiumUnique1 | BeltsRequires:  Level 65Has (1—3) Charm Slots(20—30)% increased Stun Threshold(15—25)% Life Recovery from Flasks also applies to Runic Ward |
| Belts | [Runemastered Heavy Belt](https://poe2db.tw/us/Runemastered_Heavy_Belt) | FourBelt9VerisiumUnique2 | BeltsRequires:  Level 65Has (1—3) Charm Slots(20—30)% increased Stun Threshold(20—40)% increased Runic Ward Regeneration Rate |
| Belts | [Runemastered Heavy Belt](https://poe2db.tw/us/Runemastered_Heavy_Belt) | FourBelt9VerisiumUnique3 | BeltsRequires:  Level 65Has (1—3) Charm Slots(20—30)% increased Stun ThresholdRunic Ward recovery can Overflow maximum Runic Ward |
| Belts | [Runemastered Heavy Belt](https://poe2db.tw/us/Runemastered_Heavy_Belt) | FourBelt9VerisiumUnique4 | BeltsRequires:  Level 65Has (1—3) Charm Slots(20—30)% increased Stun ThresholdFlasks gain (0.5—1) charges per Second |
| Belts | [Stalking Belt](https://poe2db.tw/us/Stalking_Belt) | FourBeltB1 | BeltsRequires:  Level 50Has (1—3) Charm SlotsHas 1 Charm SlotsThis item gains bonuses from Socketed Items as though it was Boots It is our steps that stir. |
| Belts | [Invoking Belt](https://poe2db.tw/us/Invoking_Belt) | FourBeltB2 | BeltsRequires:  Level 32Has (1—3) Charm SlotsHas 1 Charm Slots(8—12)% increased Cast Speed It is our hands that join. |
| Belts | [Sinew Belt](https://poe2db.tw/us/Sinew_Belt) | FourBeltB3 | BeltsRequires:  Level 32Has (1—3) Charm SlotsHas 1 Charm Slots+(15—20) to Strength It is our flesh that binds. |
| Belts | [Forking Belt](https://poe2db.tw/us/Forking_Belt) | FourBeltB4 | BeltsRequires:  Level 32Has (1—3) Charm SlotsHas 1 Charm SlotsAdds 1 to (20—30) Lightning damage to Attacks It is our mind that splits. |
| Sceptres | [Rattling Sceptre](https://poe2db.tw/us/Rattling_Sceptre) | FourSceptre1 | SceptresSpirit: 100 Grants Skill: Skeletal Warrior |
| Sceptres | [Wrath Sceptre](https://poe2db.tw/us/Wrath_Sceptre) | FourSceptre10 | SceptresSpirit: 100Requires:  Level 49, 87 Int Grants Skill: Fulmination |
| Sceptres | [Aromatic Sceptre](https://poe2db.tw/us/Aromatic_Sceptre) | FourSceptre11 | SceptresSpirit: 100Requires:  Level 52, 92 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Pious Sceptre](https://poe2db.tw/us/Pious_Sceptre) | FourSceptre12 | SceptresSpirit: 100Requires:  Level 58, 102 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Hallowed Sceptre](https://poe2db.tw/us/Hallowed_Sceptre) | FourSceptre13 | SceptresSpirit: 100Requires:  Level 65, 114 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Stoic Sceptre](https://poe2db.tw/us/Stoic_Sceptre) | FourSceptre2 | SceptresSpirit: 100Requires:  Level 6, 12 Int Grants Skill: Discipline |
| Sceptres | [Lupine Sceptre](https://poe2db.tw/us/Lupine_Sceptre) | FourSceptre3 | SceptresSpirit: 100Requires:  Level 12, 24 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Omen Sceptre](https://poe2db.tw/us/Omen_Sceptre) | FourSceptre4 | SceptresSpirit: 100Requires:  Level 16, 12 Str, 25 Int Grants Skill: Malice |
| Sceptres | [Ochre Sceptre](https://poe2db.tw/us/Ochre_Sceptre) | FourSceptre5 | SceptresSpirit: 100Requires:  Level 21, 40 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Shrine Sceptre](https://poe2db.tw/us/Shrine_Sceptre) | FourSceptre6a | SceptresSpirit: 100Requires:  Level 26, 17 Str, 38 Int Grants Skill: Purity of Fire |
| Sceptres | [Shrine Sceptre](https://poe2db.tw/us/Shrine_Sceptre) | FourSceptre6b | SceptresSpirit: 100Requires:  Level 26, 17 Str, 38 Int Grants Skill: Purity of Ice |
| Sceptres | [Shrine Sceptre](https://poe2db.tw/us/Shrine_Sceptre) | FourSceptre6c | SceptresSpirit: 100Requires:  Level 26, 17 Str, 38 Int Grants Skill: Purity of Lightning |
| Sceptres | [Devouring Sceptre](https://poe2db.tw/us/Devouring_Sceptre) | FourSceptre7 | SceptresSpirit: 100Requires:  Level 33, 60 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Clasped Sceptre](https://poe2db.tw/us/Clasped_Sceptre) | FourSceptre8 | SceptresSpirit: 100Requires:  Level 36, 65 Int Grants Skill: Heart of Ice |
| Sceptres | [Devotional Sceptre](https://poe2db.tw/us/Devotional_Sceptre) | FourSceptre9 | SceptresSpirit: 100Requires:  Level 45, 26 Str, 63 Int Grants Skill: Skeletal Warrior |
| Sceptres | [Shrine Sceptre](https://poe2db.tw/us/Shrine_Sceptre) | FourSceptreUnique1 | SceptresSpirit: 100Requires:  Level 26, 17 Str, 38 Int Grants Skill: Impurity |
| Wands | [Withered Wand](https://poe2db.tw/us/Withered_Wand) | FourWand1 | Wands Grants Skill: Chaos Bolt |
| Wands | [Torture Wand](https://poe2db.tw/us/Torture_Wand) | FourWand10 | WandsRequires:  Level 49, 87 Int Grants Skill: Chaos Bolt |
| Wands | [Critical Wand](https://poe2db.tw/us/Critical_Wand) | FourWand11 | WandsRequires:  Level 52, 92 Int Grants Skill: Chaos Bolt |
| Wands | [Primordial Wand](https://poe2db.tw/us/Primordial_Wand) | FourWand12 | WandsRequires:  Level 56, 99 Int Grants Skill: Wither |
| Wands | [Dueling Wand](https://poe2db.tw/us/Dueling_Wand) | FourWand13 | WandsRequires:  Level 65, 114 Int Grants Skill: Spellslinger |
| Wands | [Bone Wand](https://poe2db.tw/us/Bone_Wand) | FourWand2 | WandsRequires:  Level 2 Grants Skill: Bone Blast |
| Wands | [Attuned Wand](https://poe2db.tw/us/Attuned_Wand) | FourWand3 | WandsRequires:  Level 2 Grants Skill: Mana Drain |
| Wands | [Siphoning Wand](https://poe2db.tw/us/Siphoning_Wand) | FourWand4 | WandsRequires:  Level 11, 23 Int Grants Skill: Power Siphon |
| Wands | [Volatile Wand](https://poe2db.tw/us/Volatile_Wand) | FourWand5 | WandsRequires:  Level 16, 31 Int Grants Skill: Volatile Dead |
| Wands | [Galvanic Wand](https://poe2db.tw/us/Galvanic_Wand) | FourWand6 | WandsRequires:  Level 25, 46 Int Grants Skill: Galvanic Field |
| Wands | [Acrid Wand](https://poe2db.tw/us/Acrid_Wand) | FourWand7 | WandsRequires:  Level 33, 60 Int Grants Skill: Decompose |
| Wands | [Offering Wand](https://poe2db.tw/us/Offering_Wand) | FourWand8 | WandsRequires:  Level 38, 68 Int Grants Skill: Exsanguinate |
| Wands | [Frigid Wand](https://poe2db.tw/us/Frigid_Wand) | FourWand9 | WandsRequires:  Level 45, 80 Int Grants Skill: Chaos Bolt |
| Wands | [Twisted Wand](https://poe2db.tw/us/Twisted_Wand) | FourWandUnique1 | WandsRequires:  Level 65, 114 Int Grants Skill: Coiling Bolts |
| Wands | [Runic Fork](https://poe2db.tw/us/Runic_Fork) | FourWandUnique2 | WandsRequires:  Level 65, 114 Int |
| Wands | [Runemastered Runic Fork](https://poe2db.tw/us/Runemastered_Runic_Fork) | FourWandUnique2VerisiumUnique1 | WandsRequires:  Level 65, 114 Int(30—50)% chance for Spell Skills to fire 2 additional Projectiles |
| Wands | [Runemastered Runic Fork](https://poe2db.tw/us/Runemastered_Runic_Fork) | FourWandUnique2VerisiumUnique2 | WandsRequires:  Level 65, 114 Int(30—50)% increased Mana Regeneration Rate |
| Wands | [Runemastered Runic Fork](https://poe2db.tw/us/Runemastered_Runic_Fork) | FourWandUnique2VerisiumUnique3 | WandsRequires:  Level 65, 114 Int+300 to maximum Runic Ward |

Wand의 distinct granted skill 후보는 Bone Blast(Bone), Mana Drain(기존Attuned), Power Siphon(Siphoning), Volatile Dead(Volatile), Galvanic Field(Galvanic), Decompose(Acrid), Exsanguinate(Offering), Chaos Bolt(Critical: 요구level52; Withered/Frigid/Torture sidegrade 보존), Wither(Primordial), Spellslinger(Dueling), Coiling Bolts(Twisted)다. Runic Fork는 별도 granted skill이 없는 source identity `FourWandUnique2`이며 Runemastered3종은 서로 다른 implicit를 가진다. 이들은 ordinary family 선택과 섞지 않는다.

Sceptre 후보는 Skeletal Warrior(Hallowed: 요구level65, 기존Rattling 보존), Discipline(Stoic), Malice(Omen), Purity of Fire/Ice/Lightning(Shrine `FourSceptre6a/6b/6c`), Heart of Ice(Clasped), Fulmination(Wrath)다. Shrine Impurity는 별도 `FourSceptreUnique1`이므로 Purity alias가 아니다. 각 source Spirit100·요구사항과 skill identity를 다음 bundle에서 검증해야 한다.

Belt의 distinct implicit 후보는 Linen(Mana Flask), Wide(Flask Charges gained), Long(Charm Duration), Plate(Armour), Ornate(Charm Charges used), Mail(Flask Charges used), Double(Charm Charges gained), Heavy(Stun Threshold), Utility(instant recovery), Fine(charges/second), Golden Obi(rarity·`BeltDemigods1` provenance 추가 검토), Invoking(Cast Speed), Sinew(Strength), Forking(compound Lightning damage)다. Rawhide는 기존 유지한다. 일반 Charm Slot1–3과 Breach base의 별도1slot, Stalking(socket-transfer), Runemastered Heavy4종(Runic Ward/Flask) 의미는 source 그대로 분리하고 다음 작업에서 지원 경계를 검토해야 한다.

현재 roster는 Body7/Helmet7/Gloves7/Boots6/Bow6/Basic·Time-Lost Jewel8/Amulet8/Ring9/Belt1/Wand1/Sceptre1이다. 다른 weapon/offhand/equipment class는 아직 full source pool·대표 base runtime 검증을 하지 않았다. 특히 Crossbow, melee weapon classes, Shields/Foci/Quivers 등의 class-specific properties·requirements·implicit·skill·socket constraints는 기존 class의 catalog로 대체할 수 없다. 이번 추가는 broader class coverage 완료를 의미하지 않는다.

## 검증·격리·완료

최종 [runtime 검증 기록](evidence/amulets-runtime-bundle-2026-10-04/verification.json): Backend unit438/integration6(실패·skip0), `check generateJooq bootJar`와 formatter 통과. Frontend `npm ci`, lint/typecheck/format/coverage/unit1809/build 통과. 기존 Vite large chunk 경고만 유지된다. API1248 + material-path490 = **1738 assertions**, 실제 currency133/Omen126 positive paths를 포함한다. Browser **1551 assertions**와 console error0, importer replay14/source14 검증을 통과했다. 검증 copy와 최종 source522파일은 byte-identical이다.

[Screenshot manifest](evidence/amulets-runtime-bundle-2026-10-04/screenshots.json)은 원본326PNG와 contact sheet48개(총374)의 경로·SHA256을 기록한다. 48개 contact sheets를 모두 visual review했다. 새7종의 six-language implicit/quality/mobile1440·390, Catalyst repeat/overflow, crossclass rejection, orange preview, 기존54films를 확인했다. Amulet ordinary209/special8에는 multi-stat definition이 없으므로 compound roll은 실제 Kinetic Ring two-stat source modifier로 six-language Alt ranges와 원본 roll/film 보존을 별도 검증했다. Amulet에 가짜 compound를 추가하지 않았다.

실패 이력은 runtime 기록과 QA attempt별 원본에 보존한다. Backend 첫 신규 fixture의 deferred Solar Runic 포함 오류, Frontend coverage의 Azure/Crimson 빈 requirement 가정, 실제 UI class singular fallback, Browser compound/Essence capacity fixture 오류를 각각 고쳤다. 기존 test 기대값을 약화하거나 skip하지 않았다. 실제 UI 수정 후 Frontend 전체 검증을 재실행했다.

[QA release](evidence/amulets-runtime-bundle-2026-10-04/qa-release.json): 전용4services/network만 정상 종료했고 exclusive slot을 반환했다. Live UI/API/DB/Redis의 원래 container IDs와 실행 상태를 확인했으며 UI/API start time도 그대로다. 기존 DB volumes/user browser storage/original repo와 다른 services는 변경하지 않았다. Local branch commit만 수행하며 master 반영·remote push·deploy는 다음 Git 통합 절차에 남긴다.

검증 결과는 완료 후 별도 runtime evidence에 기록한다. QA copy는 `codex/amulets-qa-20261004`, Compose project는 `exile-amulets-20261004`, localhost API/UI19980/19981이다. Heavy checks는 exclusive slot에서 순차 실행한다. 기존 UI18081(`18a2a3d`)/API18080(`7d27d6c`), live containers·DB volumes·사용자 browser storage·원본repo를 보존한다. Remote push/merge/deploy는 수행하지 않는다. 모든 실패 output과 이전 QA output은 보존한다. Worker 세션 이름 변경 도구가 없어 자동 이름 설정은 수행하지 못했다.
