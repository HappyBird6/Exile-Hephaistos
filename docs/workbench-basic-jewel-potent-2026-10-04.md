# Basic Jewel bases와 Potent Liquid

기준 SHA `c691a2ee536f56e544f5383ed5df88b2395000e2`. 기존 Sapphire bundle을 확장한다. 라이브 18080/18081, 사용자 browser/storage와 DB volume은 변경하지 않는다.

## 출처와 적용 범위

[Basic Jewel](https://poe2db.tw/us/Basic_Jewel)은 Ruby/Emerald/Sapphire/Diamond를 명시한다. Refined 적용은 기존 [Catalyst 근거](workbench-refined-catalyst-2026-10-04.md)와 Jewel Catalyst의 `jewel_catalyst` 구분을 재사용하며 special/Time-Lost Jewel로 일반화하지 않는다.

[Ruby](https://poe2db.tw/us/Ruby), [Emerald](https://poe2db.tw/us/Emerald), [Sapphire](https://poe2db.tw/us/Sapphire), [Diamond](https://poe2db.tw/us/Diamond)의 개별 `new ModsView` JSON만 사용한다. 혼합 Jewel 193행은 사용하지 않는다. `ItemClassesID=42`, `ModDomainsID=11`과 base item IDs, normal spawn tags를 확인했다. normal 후보는 각각 50/74/58/160개다. `corrupted`, `desecrated`는 제외하며 `liquid`는 별도 Crafted 후보다. 원본 행·family·side·level·tag·display bounds·source URL과 SHA256을 base별 catalog에 보존한다.

Normal은 기존 사용자 승인 simulator 시작 상태다. 게임 base의 enabled rarity가 Normal을 포함한다고 주장하지 않는다. Magic 1P/1S, Rare 2P/2S와 기본 화폐18종, refined catalyst13종을 네 Basic Jewel에 제공한다. Essence-on-Jewel, Vaal, Hinekora, Desecration, socket/fracturing 특수 상태는 계속 제외한다. Support/Explorer는 Solar 범위 그대로다.

게임 weight는 얻을 수 없으므로 필터된 적격 ordinary 후보에 명시적인 1/N simulator 모델을 사용한다. `DropChance=1`은 검증된 game weight가 아니다. Higher-tier currency의 family/side 최고 level 예외는 유지한다. 기존 `sapphire-uniform-candidates-v1` ledger ID는 호환성을 위해 유지하되 candidate 집합과 source URL은 실제 base를 기록한다.

## Base별 Liquid

[Liquid Emotions](https://poe2db.tw/us/Liquid_Emotions)와 각 base의 liquid 행을 대조했다. Ruby/Emerald/Sapphire는 Basic Liquid13종(일반10 + Potent3)을 지원한다. Diamond는 Concentrated Liquid Isolation, Potent Liquid Ferocity, Potent Liquid Contempt만 명시되므로 이3종만 지원한다. Diamond Melancholy나 다른 일반 Liquid의 결과를 다른 base에서 유추하지 않는다.

[GGG 0.5 notes](https://www.pathofexile.com/forum/view-thread/3932540)의 Crafted 한 개 제한을 적용한다. Crafted는 별도 무료 슬롯이 아니라 ordinary explicit 슬롯 하나를 점유한다. 현재 Crafted가 있으면 Liquid는 unchanged/no spend다. 없으면 **법적으로 삽입 가능한 결과가 하나 이상인 제거 후보 중 균등 제거 → 제거 후 유효한 source outcome 중 균등 1/N 선택**한다. 제거·결과 candidate 집합과 각 확률을 ledger에 기록한다. Game failure/retry나 실제 outcome weight의 검증을 의미하지 않는다. 유효 제거/결과가 없으면 unchanged/no spend, 관련 없는 Omen도 소모하지 않는다. Ordinary Annulment/Chaos는 기존 정책대로 Crafted를 제거할 수 있다.

### Potent Liquid Ferocity

[개별 출처](https://poe2db.tw/us/Potent_Liquid_Ferocity): Prefix는 Suffix Effect 40–60%, Suffix는 Prefix Effect 40–60%다. 서로 다른 source Code를 사용하고 동일 family conflict를 유지한다. Divine은 원래 Ferocity roll을 재추첨할 수 있지만 다른 affix의 원본값은 변경하지 않는다.

표시 projection은 **original × (100 + matching quality)/100 × (100 + opposite-side effect)/100**에서 기존 central HALF_UP rounding을 한 번 적용한다. Quality와 Ferocity의 결합 순서·정밀도는 primary game 증거가 없어 **잠정 simulator 표시 가정**이다. 가정은 UI scope, modifier detail, film ledger에 표시한다. Derived 값은 catalog range 검증·generation weight·film 원본 roll로 되먹이지 않는다. Ferocity/Contempt와 조건부 presence marker는 unscalable이다. 조건부 효과나 전투 합계를 계산하지 않는다.

### Potent Liquid Contempt

[개별 출처](https://poe2db.tw/us/Potent_Liquid_Contempt): Prefix는 +1 Suffix allowed, Suffix는 +1 Prefix allowed다. Rare의 해당 side cap은3, 반대쪽2이고 total5다. Modifier 자체는 보통 슬롯을 차지한다. 제거 후 원래 excess affix를 삭제하거나 보정하지 않는다. 확장 modifier가 없는 기존 Rare는 side 최대3·total4까지 보존할 수 있지만 **새 삽입은 현재 side2·total4 제한**을 따른다. Contempt가 존재하면 현재 실제 확장 side를 정확히 검증한다.

Cap-loss 보존은 사용자 지정 정책이며 [Mobalytics crafting guide](https://mobalytics.gg/poe-2/profile/paintmaster/guides/recoup-chronomancer-gear-crafting-guide)를 secondary 근거로 둔다. Primary proof는 확보하지 못했다. 기존 overflow film의 reload/undo/redo/Divine/Annulment는 유효하지만, total4 상태의 새 Exalted 삽입은 거부한다. Chaos는 현재 원자적 정책대로 모든 제거 branch의 replacement pool을 요구하므로, cap 제거 후 합법적 추가가 없는 branch가 있으면 전체 작업을 거부한다.

### Potent Liquid Melancholy

[개별 출처](https://poe2db.tw/us/Potent_Liquid_Melancholy): Ruby는 Emerald+Sapphire socketed 조건의 Debilitate on Hit, Emerald는 Ruby+Sapphire 조건의 Blind on Hit, Sapphire는 Ruby+Emerald 조건의 Elemental Exposure on Hit다. 외부 passive-tree 상태를 만들지 않고 정확한 조건 문구만 표시한다. `display_condition_present=1`은 내부 presence marker이며 game stat ID나 전투 수치가 아니다. Diamond 결과는 출처에 없어 미지원이다.

## 호환성과 언어

기존 Sapphire source IDs/roll bounds와 이전 snapshot identities를 유지한다. Films는 source IDs, canonical values, rule/ledger version, candidate 확률·가정을 저장한다. Quality40 cap40→20 보존과 catalyst repeat/type change `max(existing,currentCap)` 잠정 정책은 바꾸지 않는다. Alt/Shift, local orange Omen과 six-language UI를 유지한다.

각 locale 행은 family·side·level·tag·bounds로 결합한다. 동일 signature의 복수 ordinary 행은 base별 source 순서를 추가 결합 키로 사용하고 복수 개수가 같음을 검증한다. Spanish에는 Liquid table이 없어 일치하는 ordinary translation을 사용하며 Emerald Life on Kill은 동일 family/side/bounds의 Diamond ordinary 행을 사용한다. Spanish Potent/Maximum Chaos Resistance는 English source semantics에 근거한 수동 번역이다. 출처에서 그대로 제공된 Spanish Liquid 번역으로 주장하지 않는다. Source display bindings는 내부 IDs를 사용하며 게임 stat IDs를 발명하지 않는다.

검증 명령·실패 이력·실제 수와 screenshot 경로는 `docs/evidence/workbench-basic-jewel-potent-validation-2026-10-04.json`에 기록한다. 다음 범위는 Ancient13/Time-Lost의 별도 base·radius·Small/Notable semantics다. Remote push/merge/deploy/release는 수행하지 않는다.
