# Augment 전수 대조

2026-11-05 수집. [PoE2DB 원문](https://poe2db.tw/kr/Augment) 313종을 모두 분류했다.

일반 효과 198종 추가(Rune 129·Idol 33·Soul Core 32·Abyssal Eye 4), 기존 79종 보존으로 총 277종이다. 신규 선택은 232종이며 기존 Perfect 정책으로 45종을 숨긴다.

특수 동작 23종은 사용자 지시로 보류한다. Sceptres/Foci 전용 5종은 장착 자격·홈 상한 미검증, DNT-Unused 8종은 원문에 일반 효과가 없다.

표시하는 것은 기본 소켓 효과다. 캐릭터 능력치·전투 결과·Bonded는 계산하지 않는다. 같은 아이템의 중복/공유 그룹 제한만 검사하며 전체 캐릭터 장비 제한은 확인하지 않는다. 기존 제한 누락 상태를 보존하기 위해 원래 79종 중 제한이 확인된 Soul Core 9종은 상태 읽기에서 기존 중복을 허용하지만 새 장착은 제한한다.

6개 언어 원문·원본 효과·장비별 자격·제한은 [audit.json](../evidence/0.5.5/augment-expansion-20261105/audit.json)과 두 실행 카탈로그에 연결한다. 원문 HTML 및 이미지의 SHA-256을 별도 보존한다.

## 추가한 일반 증강물 (198)

| ID | 한국어 원문 이름 | 지원 class 또는 사유 |
|---|---|---|
| `Greater_Rune_of_Leadership` | 통솔의 상위 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Greater_Rune_of_Tithing` | 십일조의 상위 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Greater_Rune_of_Alacrity` | 기민성의 상위 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Greater_Rune_of_Nobility` | 고결성의 상위 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Hedgewitch_Assandras_Rune_of_Wisdom` | 산울마녀 아산드라의 지혜의 룬 | Wands, Staves |
| `Saqawals_Rune_of_the_Sky` | 사카왈의 하늘의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Fenumus_Rune_of_Agony` | 페누무스의 괴로움의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Farruls_Rune_of_Grace` | 페룰의 은총의 룬 | Boots |
| `Farruls_Rune_of_the_Chase` | 페룰의 추격의 룬 | Boots |
| `Craiceanns_Rune_of_Warding` | 크라칸의 수호의 룬 | Body Armours |
| `Saqawals_Rune_of_Memory` | 사카왈의 기억의 룬 | Helmets |
| `Saqawals_Rune_of_Erosion` | 사카왈의 침식의 룬 | Helmets |
| `Farruls_Rune_of_the_Hunt` | 페룰의 사냥의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Craiceanns_Rune_of_Recovery` | 크라칸의 회복의 룬 | Body Armours |
| `Courtesan_Mannans_Rune_of_Cruelty` | 매춘부 마난의 잔혹의 룬 | Gloves |
| `Thane_Grannells_Rune_of_Mastery` | 테인 그라넬의 숙련의 룬 | Gloves |
| `Fenumus_Rune_of_Spinning` | 페누무스의 회전의 룬 | Gloves |
| `Countess_Seskes_Rune_of_Archery` | 세스케 백작 부인의 궁술의 룬 | Bows |
| `Thane_Girts_Rune_of_Wildness` | 테인 거트의 야생의 룬 | Wands, Staves |
| `Fenumus_Rune_of_Draining` | 페누무스의 고갈의 룬 | Gloves |
| `Thane_Myrks_Rune_of_Summer` | 테인 머크의 여름의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Lady_Hestras_Rune_of_Winter` | 헤스트라 부인의 겨울의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Thane_Lelds_Rune_of_Spring` | 테인 렐드의 봄의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `The_Greatwolfs_Rune_of_Claws` | 위대한 늑대의 발톱의 룬 | Gloves |
| `The_Greatwolfs_Rune_of_Willpower` | 위대한 늑대의 의지의 룬 | Body Armours |
| `Idol_of_Sirrius` | 시리우스의 우상 | Gloves |
| `Idol_of_Thruldana` | 스룰다나의 우상 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Idol_of_Grold` | 그롤드의 우상 | Boots |
| `Idol_of_Eeshta` | 이쉬타의 우상 | Helmets |
| `Idol_of_Egrin` | 에그린의 우상 | Helmets |
| `Idol_of_Maxarius` | 막사리우스의 우상 | Body Armours |
| `Idol_of_Ralakesh` | 랄라케쉬의 우상 | Helmets |
| `Idol_of_Greust` | 그루스트의 우상 | Shields, Bucklers |
| `Idol_of_Yeena` | 이이나의 우상 | Boots |
| `Idol_of_Eramir` | 에라미어의 우상 | Body Armours |
| `Idol_of_Oak` | 오크의 우상 | Boots |
| `Idol_of_Alira` | 알리라의 우상 | Helmets |
| `Idol_of_Kraityn` | 크레이틴의 우상 | Gloves |
| `Idol_of_Silk` | 실크의 우상 | Shields, Bucklers |
| `Idol_of_the_Sycophant` | 아첨꾼의 우상 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Idol_of_the_Martyr` | 순교자의 우상 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Idol_of_the_Pharisee` | 바리새인의 우상 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Panther_Idol` | 흑표 우상 | Body Armours |
| `Hawk_Idol` | 매 우상 | Body Armours |
| `Stoat_Idol` | 담비 우상 | Body Armours |
| `Hayoxis_Soul_Core_of_Heatproofing` | 하욕시의 내열의 영혼 핵 | Helmets |
| `Zalatls_Soul_Core_of_Insulation` | 잘라틀의 절연의 영혼 핵 | Boots |
| `Topotantes_Soul_Core_of_Dampening` | 토포탄테의 완화의 영혼 핵 | Gloves |
| `Atmohuas_Soul_Core_of_Retreat` | 앗모후아의 후퇴의 영혼 핵 | Body Armours |
| `Quipolatls_Soul_Core_of_Flow` | 퀴폴라틀의 흐름의 영혼 핵 | Helmets |
| `Tzamotos_Soul_Core_of_Ferocity` | 자모토의 흉포함의 영혼 핵 | Helmets |
| `Uromotis_Soul_Core_of_Attenuation` | 우로모티의 감쇠의 영혼 핵 | Boots |
| `Opilotis_Soul_Core_of_Assault` | 오필로티의 맹공의 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Guatelitzis_Soul_Core_of_Endurance` | 과텔리치의 인내의 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Xopecs_Soul_Core_of_Power` | 조펙의 힘의 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Estazuntis_Soul_Core_of_Convalescence` | 에스타준티의 요양의 영혼 핵 | Boots, Helmets |
| `Tacatis_Soul_Core_of_Affliction` | 타카티의 수난의 영혼 핵 | Helmets |
| `Cholotls_Soul_Core_of_War` | 촐로틀의 전쟁의 영혼 핵 | Bows |
| `Citaqualotls_Soul_Core_of_Foulness` | 시타콰로틀의 부정의 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Xipocados_Soul_Core_of_Dominion` | 지포카도의 지배의 영혼 핵 | Wands, Staves |
| `Jiquanis_Soul_Core_of_Automation` | 지콰니의 자동화 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Malediction` | 지콰니의 악담 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Targeting` | 지콰니의 대상 지정 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Rallying` | 지콰니의 재집결 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Radiance` | 지콰니의 광휘 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Severing` | 지콰니의 단절 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Rippling` | 지콰니의 물결 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Quaking` | 지콰니의 진동 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Munitions` | 지콰니의 군수품 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Snares` | 지콰니의 올가미 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Abundance` | 지콰니의 풍부 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Squalls` | 지콰니의 돌풍 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Jiquanis_Soul_Core_of_Thundering` | 지콰니의 천둥 영혼 핵 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves |
| `Snake_Idol` | 독사 우상 | Gloves |
| `Primate_Idol` | 영장류 우상 | Helmets |
| `Cat_Idol` | 고양이 우상 | Gloves |
| `Wolf_Idol` | 늑대 우상 | Gloves |
| `Stag_Idol` | 수사슴 우상 | Helmets |
| `Boar_Idol` | 멧돼지 우상 | Gloves |
| `Bear_Idol` | 곰 우상 | Helmets |
| `Ox_Idol` | 소 우상 | Shields, Bucklers |
| `Rabbit_Idol` | 토끼 우상 | Body Armours |
| `Amanamus_Gaze` | 아마나무의 응시 | Helmets, Body Armours, Boots |
| `Tecrods_Gaze` | 테크로드의 응시 | Body Armours, Gloves, Boots |
| `Kurgals_Gaze` | 쿠르갈의 응시 | Helmets, Gloves, Boots |
| `Ulamans_Gaze` | 울라만의 응시 | Helmets, Gloves, Body Armours |
| `Guatelitzis_Thesis` | 과텔리치의 명제 | Helmets, Body Armours, Boots |
| `Citaqualotls_Thesis` | 시타콰로틀의 명제 | Body Armours, Gloves, Boots |
| `Jiquanis_Thesis` | 지콰니의 명제 | Helmets, Gloves, Boots |
| `Quipolatls_Thesis` | 퀴폴라틀의 명제 | Helmets, Gloves, Body Armours |
| `Emergent_Vigour` | 발현된 활력 | Helmets, Body Armours, Boots |
| `Emergent_Possibility` | 발현된 가능성 | Body Armours, Gloves, Boots |
| `Emergent_Protection` | 발현된 보호 | Helmets, Gloves, Boots |
| `Emergent_Instinct` | 발현된 본능 | Helmets, Gloves, Body Armours |
| `Carved_Cunning` | 조각된 교활함 | Helmets, Body Armours, Boots |
| `Carved_Majesty` | 조각된 위엄 | Body Armours, Gloves, Boots |
| `Carved_Mischief` | 조각된 장난기 | Helmets, Gloves, Body Armours |
| `Carved_Tenacity` | 조각된 참을성 | Helmets, Gloves, Boots |
| `Warding_Rune_of_Reinforcement` | 보강의 수호하는 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Warding_Rune_of_Protection` | 보호의 수호하는 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Warding_Rune_of_Disintegration` | 분해의 수호하는 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Warding_Rune_of_Desperation` | 필사의 수호하는 룬 | Wands, Staves |
| `Warding_Rune_of_Symbiosis` | 공생의 수호하는 룬 | Boots |
| `Warding_Rune_of_Courage` | 용기의 수호하는 룬 | Helmets |
| `Warding_Rune_of_Stability` | 안정성의 수호하는 룬 | Shields, Bucklers |
| `Warding_Rune_of_Glancing` | 튕김의 수호하는 룬 | Body Armours |
| `Warding_Rune_of_Heart` | 심장의 수호하는 룬 | Body Armours |
| `Warding_Rune_of_Nourishment` | 자양분의 수호하는 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Warding_Rune_of_Annihilation` | 전멸의 수호하는 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Warding_Rune_of_Armature` | 골조의 수호하는 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Warding_Rune_of_Obsession` | 집착의 수호하는 룬 | Wands |
| `Warding_Rune_of_Equinox` | 지점의 수호하는 룬 | Wands, Staves |
| `Warding_Rune_of_Hollowing` | 공동화의 수호하는 룬 | Wands, Staves |
| `Ancient_Rune_of_Splinters` | 파편의 고대 룬 | Bows |
| `Ancient_Rune_of_Dueling` | 결투의 고대 룬 | Bucklers |
| `Ancient_Rune_of_the_Titan` | 거신의 고대 룬 | Two Hand Maces |
| `Ancient_Rune_of_Shattering` | 산산조각의 고대 룬 | One Hand Maces |
| `Ancient_Rune_of_Prowess` | 기량의 고대 룬 | Spears |
| `Ancient_Rune_of_Control` | 통제의 고대 룬 | Quarterstaves |
| `Ancient_Rune_of_Discovery` | 발견의 고대 룬 | Staves |
| `Ancient_Rune_of_Decay` | 부패의 고대 룬 | Wands |
| `Ancient_Rune_of_Animosity` | 적대감의 고대 룬 | Talismans |
| `Ancient_Rune_of_Detonation` | 기폭의 고대 룬 | Crossbows |
| `Ancient_Rune_of_Retaliation` | 보복의 고대 룬 | Shields |
| `Rune_of_Vitality` | 활력의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves, Shields, Bucklers |
| `Rune_of_the_Hunt` | 사냥의 룬 | Boots |
| `Rune_of_Acrobatics` | 곡예의 룬 | Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Wands, Staves, Boots |
| `Rune_of_Culmination` | 고조의 룬 | Gloves, One Hand Maces, Quarterstaves |
| `Rune_of_Renown` | 명성의 룬 | Gloves, Wands, Staves |
| `Rune_of_Accumulation` | 축적의 룬 | Gloves, Crossbows, Bows, Spears |
| `Rune_of_Foundations` | 기초의 룬 | Body Armours |
| `Rune_of_the_Prism` | 분광기의 룬 | Body Armours |
| `Rune_of_the_Blossom` | 만개의 룬 | Body Armours |
| `Rune_of_Consistency` | 일관성의 룬 | Helmets, Wands, Staves |
| `Rune_of_Reach` | 범위의 룬 | Helmets, Wands, Staves |
| `Rune_of_Vital_Flame` | 활력 불길의 룬 | One Hand Maces, Two Hand Maces, Talismans |
| `Rune_of_Confrontation` | 대적의 룬 | Quarterstaves, Spears |
| `Legacy_of_Bramblejack` | 가시나무 갑옷의 유산 | Body Armours |
| `Legacy_of_Blackbraid` | 검은끈의 유산 | Body Armours |
| `Legacy_of_Edyrns_Tusks` | 에디른의 엄니의 유산 | Body Armours |
| `Legacy_of_Kingsguard` | 왕의 보호구의 유산 | Body Armours |
| `Legacy_of_Bristleboar` | 가시털멧돼지의 유산 | Body Armours |
| `Legacy_of_Foxshade` | 여우 그림자의 유산 | Body Armours |
| `Legacy_of_Ashrend` | 부서진 잿더미의 유산 | Body Armours |
| `Legacy_of_Briskwrap` | 활력의 겉옷의 유산 | Body Armours |
| `Legacy_of_The_Unleashed` | 해방된 자의 유산 | Body Armours |
| `Legacy_of_Horns_of_Bynden` | 빈덴의 뿔의 유산 | Helmets |
| `Legacy_of_Wings_of_Caelyn` | 카일린의 날개의 유산 | Helmets |
| `Legacy_of_Ezomyte_Peak` | 에조미어의 면갑의 유산 | Helmets |
| `Legacy_of_Deidbell` | 죽음의 종의 유산 | Helmets |
| `Legacy_of_Elevore` | 원소포식의 유산 | Helmets |
| `Legacy_of_Starkonjas_Head` | 스타콘자의 머리의 유산 | Helmets |
| `Legacy_of_Crown_of_Thorns` | 가시 왕관의 유산 | Helmets |
| `Legacy_of_Greymake` | 회색작품의 유산 | Helmets |
| `Legacy_of_Erians_Cobble` | 에리안의 수선의 유산 | Helmets |
| `Legacy_of_The_Smiling_Knight` | 미소 짓는 기사의 유산 | Helmets |
| `Legacy_of_The_Vile_Knight` | 끔찍한 기사의 유산 | Helmets |
| `Legacy_of_Northpaw` | 북방발의 유산 | Gloves |
| `Legacy_of_Candlemaker` | 양초 제작자의 유산 | Gloves |
| `Legacy_of_Deathblow` | 죽음의 일격의 유산 | Gloves |
| `Legacy_of_Legionstride` | 군단의 보조의 유산 | Boots |
| `Legacy_of_Trampletoe` | 짓발가락의 유산 | Boots |
| `Legacy_of_Briarpatch` | 가시밭의 유산 | Boots |
| `Legacy_of_Bushwhack` | 덤불 매복의 유산 | Boots |
| `Legacy_of_Wanderlust` | 방랑벽의 유산 | Boots |
| `Legacy_of_The_Knight-errant` | 무사 수행 기사의 유산 | Boots |
| `Legacy_of_Oberns_Bastion` | 오베른의 보루의 유산 | Boots |
| `Legacy_of_Dionadair` | 다이오나데어의 유산 | Shields |
| `Legacy_of_Wulfsbane` | 늑대의 천적의 유산 | Shields |
| `Legacy_of_Chernobogs_Pillar` | 체르노보그의 기둥의 유산 | Shields |
| `Legacy_of_Alkem_Eira` | 알켐 에이라의 유산 | Shields |
| `Legacy_of_Oaksworn` | 참나무맹세의 유산 | Shields |
| `Legacy_of_Dunkelhalt` | 둥켈할트의 유산 | Bucklers |
| `Legacy_of_Rondel_de_Ezo` | 론델 데 에소의 유산 | Bucklers |
| `Legacy_of_Brynhands_Mark` | 브린핸드의 징표의 유산 | One Hand Maces |
| `Legacy_of_Trenchtimbre` | 참호목의 유산 | One Hand Maces |
| `Legacy_of_Mj%C3%B6lner` | 묠니르의 유산 | One Hand Maces |
| `Legacy_of_Twisted_Empyrean` | 뒤틀린 천공인의 유산 | Two Hand Maces |
| `Legacy_of_Hoghunt` | 돼지사냥의 유산 | Two Hand Maces |
| `Legacy_of_Hrimnors_Hymn` | 흐림노르의 찬가의 유산 | Two Hand Maces |
| `Legacy_of_Brain_Rattler` | 요동치는 두뇌의 유산 | Two Hand Maces |
| `Legacy_of_Lifesprig` | 생명의 가지의 유산 | Wands |
| `Legacy_of_Duality` | 이중성의 유산 | Quarterstaves |
| `Legacy_of_Tyrannys_Grip` | 폭정의 손아귀의 유산 | Spears |
| `Legacy_of_The_Sentry` | 보초의 유산 | Quarterstaves |
| `Legacy_of_Adonias_Ego` | 아도니아의 자아의 유산 | Wands |
| `Legacy_of_Cursecarver` | 저주난자의 유산 | Wands |
| `Legacy_of_Dusk_Vigil` | 황혼의 불침번의 유산 | Staves |
| `Legacy_of_The_Blood_Thorn` | 피의 가시의 유산 | Quarterstaves |
| `Legacy_of_Quill_Rain` | 화살 비의 유산 | Bows |
| `Legacy_of_Ironbound` | 철결속의 유산 | Bows |
| `Legacy_of_Amor_Mandragora` | 아모르 만드라고라의 유산 | Talismans |
| `Legacy_of_Spiteful_Floret` | 악의에 찬 소화의 유산 | Talismans |
| `Legacy_of_Svalinn` | 스발린의 유산 | Shields |
| `Legacy_of_Keeper_of_the_Arc` | 전호의 보관자의 유산 | Helmets |
| `Legacy_of_Olrovasara` | 올로바사라의 유산 | One Hand Maces |
| `Legacy_of_A_Worthy_Foe` | 호적수의 유산 | Shields |
| `Legacy_of_Facebreaker` | 페이스브레이커의 유산 | Gloves |

## 보존한 기존 증강물 (79)

| ID | 한국어 원문 이름 | 지원 class 또는 사유 |
|---|---|---|
| `Desert_Rune` | 사막 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Glacial_Rune` | 빙하 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Storm_Rune` | 폭풍 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Iron_Rune` | 철 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Body_Rune` | 육신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Mind_Rune` | 정신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Rebirth_Rune` | 부활 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Inspiration_Rune` | 영감 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Stone_Rune` | 돌 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Vision_Rune` | 환영 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Desert_Rune` | 하위 사막 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Glacial_Rune` | 하위 빙하 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Storm_Rune` | 하위 폭풍 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Iron_Rune` | 하위 철 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Body_Rune` | 하위 육신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Mind_Rune` | 하위 정신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Rebirth_Rune` | 하위 부활 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Inspiration_Rune` | 하위 영감 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Stone_Rune` | 하위 돌 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Vision_Rune` | 하위 환영 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Desert_Rune` | 상위 사막 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Glacial_Rune` | 상위 빙하 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Storm_Rune` | 상위 폭풍 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Iron_Rune` | 상위 철 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Body_Rune` | 상위 육신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Mind_Rune` | 상위 정신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Rebirth_Rune` | 상위 부활 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Inspiration_Rune` | 상위 영감 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Stone_Rune` | 상위 돌 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Vision_Rune` | 상위 환영 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Desert_Rune` | 완벽한 사막 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Glacial_Rune` | 완벽한 빙하 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Storm_Rune` | 완벽한 폭풍 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Iron_Rune` | 완벽한 철 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Body_Rune` | 완벽한 육신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Mind_Rune` | 완벽한 정신 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Rebirth_Rune` | 완벽한 부활 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Inspiration_Rune` | 완벽한 영감 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Stone_Rune` | 완벽한 돌 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Vision_Rune` | 완벽한 환영 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Robust_Rune` | 하위 왕성 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Robust_Rune` | 왕성한 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Robust_Rune` | 상위 왕성 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Robust_Rune` | 완벽한 왕성한 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Adept_Rune` | 하위 숙달 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Adept_Rune` | 숙달 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Adept_Rune` | 상위 숙달 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Adept_Rune` | 완벽한 숙달 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Resolve_Rune` | 하위 투지 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Resolve_Rune` | 투지 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Greater_Resolve_Rune` | 상위 투지 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Perfect_Resolve_Rune` | 완벽한 투지 룬 | Gloves, Boots, Helmets, Body Armours, Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans, Shields, Bucklers |
| `Lesser_Tempered_Rune` | 하위 단조 룬 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Tempered_Rune` | 단조 룬 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Greater_Tempered_Rune` | 상위 단조 룬 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Soul_Core_of_Vizoma` | 비조마의 영혼 핵 | Wands, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Staves, Talismans |
| `Soul_Core_of_Tacati` | 타카티의 영혼 핵 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Soul_Core_of_Opiloti` | 오필로티의 영혼 핵 | Helmets, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Jiquani` | 지콰니의 영혼 핵 | Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Zalatl` | 잘라틀의 영혼 핵 | Helmets, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Citaqualotl` | 시타콰로틀의 영혼 핵 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Soul_Core_of_Puhuarte` | 푸후알테의 영혼 핵 | Gloves, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Tzamoto` | 자모토의 영혼 핵 | Helmets, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Xopec` | 조펙의 영혼 핵 | Boots, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Azcapa` | 아즈카파의 영혼 핵 | Gloves, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Topotante` | 토포탄테의 영혼 핵 | Boots, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Quipolatl` | 퀴폴라틀의 영혼 핵 | Boots, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans |
| `Soul_Core_of_Ticaba` | 티카바의 영혼 핵 | Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Soul_Core_of_Atmohua` | 앗모후아의 영혼 핵 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Soul_Core_of_Cholotl` | 촐로틀의 영혼 핵 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Soul_Core_of_Zantipi` | 잔티피의 영혼 핵 | Gloves, Boots, Helmets, Body Armours, Bows, Crossbows, One Hand Maces, Two Hand Maces, Quarterstaves, Spears, Talismans, Shields, Bucklers |
| `Lesser_Ward_Rune` | 하위 수호 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Ward_Rune` | 수호 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Greater_Ward_Rune` | 상위 수호 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Perfect_Ward_Rune` | 완벽한 수호 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Lesser_Charging_Rune` | 하위 충전 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Charging_Rune` | 충전 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Greater_Charging_Rune` | 상위 충전 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |
| `Perfect_Charging_Rune` | 완벽한 충전 룬 | Gloves, Boots, Helmets, Body Armours, Shields, Bucklers |

## 보류한 특수 동작 (23)

| ID | 한국어 원문 이름 | 지원 class 또는 사유 |
|---|---|---|
| `Masterwork_Rune` | 일품 룬 | 장착 Rune 업그레이드 보류. 정확한 등급 전환 대응도 추가 검증 필요. |
| `Atziris_Soul_Core_of_Devotion` | 앗지리의 헌신 영혼 핵 | 타락 시 반드시 변화하는 규칙·Socket-bound 교체 금지 보류. |
| `Atziris_Soul_Core_of_Vitality` | 앗지리의 활력 영혼 핵 | 타락 시 반드시 변화하는 규칙·Socket-bound 교체 금지 보류. |
| `Atziris_Soul_Core_of_Alacrity` | 앗지리의 기민성 영혼 핵 | 타락 시 반드시 변화하는 규칙·Socket-bound 교체 금지 보류. |
| `Atziris_Soul_Core_of_Inoculation` | 앗지리의 접종 영혼 핵 | 타락 시 반드시 변화하는 규칙·Socket-bound 교체 금지 보류. |
| `Fox_Idol` | 여우 우상 | 같이 장착한 Idol의 Bonded 활성화 보류. |
| `Raven-Touched_Shard` | 큰까마귀의 손길에 닿은 파편 | Raven-Touched 특수 효과·Socket-bound 교체 금지 보류. |
| `Passion_of_Aldur` | 알두르의 열정 | 원소 접사 변환·Socket-bound 교체 금지 보류. 동등 Modifier 대응표도 필요. |
| `Breath_of_Aldur` | 알두르의 숨결 | 원소 접사 변환·Socket-bound 교체 금지 보류. 동등 Modifier 대응표도 필요. |
| `Ire_of_Aldur` | 알두르의 노여움 | 원소 접사 변환·Socket-bound 교체 금지 보류. 동등 Modifier 대응표도 필요. |
| `Betrayal_of_Aldur` | 알두르의 배신 | 원소 접사 변환·Socket-bound 교체 금지 보류. 동등 Modifier 대응표도 필요. |
| `Serles_Triumph` | 세를의 승리 | 접미어·전체 접사 한도 변경 보류. |
| `Cadigans_Epiphany` | 캐디건의 통찰 | Augment 홈 파괴·Jewel 홈 생성 보류. |
| `Astrids_Creativity` | 아스트리드의 창의성 | Crafted Modifier 한도 변경 보류. |
| `Uhtreds_Sidereus` | 우트레드의 성좌 | Chronomancy 접사 풀 해금·추첨·Socket-bound 교체 금지 보류. |
| `Kolrs_Hunt` | 콜르의 사냥 | Marksman 접사 풀 해금·추첨·Socket-bound 교체 금지 보류. |
| `Voranas_Carnage` | 보라나의 학살 | Berserking 접사 풀 해금·추첨·Socket-bound 교체 금지 보류. |
| `Thruds_Might` | 스루드의 완력 | Destruction 접사 풀 해금·추첨·Socket-bound 교체 금지 보류. |
| `Medveds_Tending` | 메드베드의 보살핌 | Soul 접사 풀 해금·추첨·Socket-bound 교체 금지 보류. |
| `Katlas_Gloom` | 카틀라의 순흑 | Decay 접사 풀 해금·추첨·Socket-bound 교체 금지 보류. |
| `Aldurs_Legacy` | 알두르의 유산 | Unique 파괴·결과 Rune 생성 보류. Unique별 대응표도 필요. |
| `Legacy_of_Serles_Grit` | 세를의 근성의 유산 | 최대 Quality 40%로 변경하는 동작 보류. |
| `Legacy_of_Runeseekers_Call` | 룬 탐구자의 소명의 유산 | 다른 Rune 효과 증폭 보류. 적용 대상·반올림도 추가 검증 필요. |

## 지원 베이스가 없는 항목 (5)

| ID | 한국어 원문 이름 | 지원 class 또는 사유 |
|---|---|---|
| `Owl_Idol` | 올빼미 우상 | 일반 효과가 Sceptres/Foci에만 있음. 해당 class의 홈 자격·상한 미검증. |
| `Warding_Rune_of_Salvaging` | 분해의 수호하는 룬 | 일반 효과가 Sceptres/Foci에만 있음. 해당 class의 홈 자격·상한 미검증. |
| `Warding_Rune_of_Bodyguards` | 호위병의 수호하는 룬 | 일반 효과가 Sceptres/Foci에만 있음. 해당 class의 홈 자격·상한 미검증. |
| `Ancient_Rune_of_Witchcraft` | 마술의 고대 룬 | 일반 효과가 Sceptres/Foci에만 있음. 해당 class의 홈 자격·상한 미검증. |
| `Ancient_Rune_of_the_Horde` | 떼의 고대 룬 | 일반 효과가 Sceptres/Foci에만 있음. 해당 class의 홈 자격·상한 미검증. |

## 명시적 미사용 항목 (8)

| ID | 한국어 원문 이름 | 지원 class 또는 사유 |
|---|---|---|
| `DNT-Unused_Rune_of_Wild_Ferocity` | [DNT-Unused] Rune of Wild Ferocity | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Partnership` | [DNT-Unused] Rune of Partnership | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Reverberation` | [DNT-Unused] Rune of Reverberation | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Vital_Flame` | [DNT-Unused] Rune of Vital Flame | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Confrontation` | [DNT-Unused] Rune of Confrontation | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Chance` | [DNT-Unused] Rune of Chance | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Confidence` | [DNT-Unused] Rune of Confidence | DNT-Unused 명시, 일반 효과 없음. |
| `DNT-Unused_Rune_of_Duplication` | [DNT-Unused] Rune of Duplication | DNT-Unused 명시, 일반 효과 없음. |
