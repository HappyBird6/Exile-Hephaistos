# 다음 묶음: Jewel 기본 화폐와 Liquid

진행 경계: JL-01의 기존 Sapphire suffix Annulment/Divine 부분은 [제거·재굴림 변경](workbench-sapphire-existing-currency-2026-10-04.md)으로 구현했다. 일반 pool/실제 slot 확대와 Liquid는 아직 미완료다. 아래 순서는 남은 작업의 순서이며 전체 JL-01 완료를 뜻하지 않는다.

이번 채팅은 cap 정정만 구현한다. 다음 fresh chat은 Jewel 기본 제작과 Liquid를 구현한다. 사용자가 Jewel도 첫 Currency tab의 기본 규칙을 적용받는다고 확인했고 Liquid를 다시 요청했으므로 이전 Liquid 제외·catalyst-only Jewel guard는 다음 묶음에서 갱신한다. Essence는 Jewel에 적용하지 않는다. “장비라고 명시되어 있지 않은 애들”은 검증할 가설이다. tooltip 단어 부재를 eligibility로 사용하지 않는다. 서버 갱신 요청은 없다.

첫 tab은 `currencies.ts`의22개다. [전체 stable ID와 source 인벤토리](evidence/jewel-liquid-next-inventory-2026-10-04.json)에 순서대로 기록했다. 일반/Greater/Perfect Transmutation, Augmentation, Regal, Exalted, Chaos15종과 Alchemy, Annulment, Divine3종이 우선 검토 대상18개다. Artificer's Orb는 socket/equipment 대상 경계 확인이 필요하다. Fracturing Orb는 기존 구현과 Jewel 적용·최소 affix 조건을 별도 확인한다. Vaal Orb와 Hinekora's Lock은 첫 tab에 있지만 이전 deferred 범위와 충돌한다. 새 요청 없이 확률/예측/오염 구현으로 확대하지 않는다. Chance·Extraction·Desecration은 이22개에 없다.

[Crafting 원문](https://poe2db.tw/us/Crafting)은 rarity별 기본 연산을 구분한다. 단, 개별 tooltip의 Equipment 제한과 Jewel 예외·Greater/Perfect minimum-level 의미는 개별 원문/게임 자료와 대조해야 한다. 기존 parser의 미검증 표시 값을 crafting catalog로 자동 승격하지 않는다.

[Basic Jewel 원문](https://poe2db.tw/us/Basic_Jewel)은 Ruby/Emerald/Sapphire/Diamond를 구분한다. [Sapphire](https://poe2db.tw/us/Sapphire), [Ruby](https://poe2db.tw/us/Ruby), [Emerald](https://poe2db.tw/us/Emerald), [Diamond](https://poe2db.tw/us/Diamond)의 raw ModsView를 새로 확보했다. normal section 행 수는 각각58/50/74/160이며 **eligible generation pool 개수는 아니다**. 각 페이지는 liquid·corrupted·desecrated section을 따로 갖는다. base tag/domain, family, item level, source stat bounds, 일반 생성 여부를 대조해야 한다. `DropChance=1`을 실제 spawn weight로 자동 해석하지 않는다. 정상 후보의 weight만 없을 때 사용자 허용 정책대로 disclosed1/N을 사용한다. 완전한 후보 집합 자체가 불명확하면 지원하지 않는다.

현재 Sapphire의 Magic/Rare, reviewed suffix 한 개는 editor 안전 경계이며 게임 최대 slot이 아니다. 통상 검토 후보인 Magic1prefix/1suffix·Rare2prefix/2suffix·Normal 생성은 다음 구현 전에 직접 게임 근거로 확정한다. [PoB Item.lua](https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/src/Classes/Item.lua)는 Jewel affixLimit4를 사용하지만 보조 구현 자료이며 이것만으로 최신 게임의 rarity/side/cap 예외를 확정하지 않는다. 현재 PoE2DB raw base에는 slot 수가 없다. 향후 Contempt가 slot 수를 바꾸므로 일반 slot과 effective slot을 구분한다.

[Liquid Emotions 원문](https://poe2db.tw/us/Liquid_Emotions)은27종을 구분한다. non-Ancient13은 Rare Basic Jewel, Ancient14는 Rare Time-Lost Jewel용이다. 기본 동작은 무작위 기존 modifier 제거 후 해당 base에 보장된 Crafted modifier 추가다. Map Delirium과 amulet instilling은 이번 Jewel 요청과 분리한다. 모든 liquid에 같은 Jewel pool을 사용하지 않는다. Diamond는 별도 표기된 행만 허용한다. 보장 추가 효과와 실제 제거 확률을 구분한다.

인벤토리의 `liquidModifierRows`는 각 base에 대한 currency stable ID, source Code, PREFIX/SUFFIX, family, spawn tag, 원문 범위를 보존한다. Sapphire/Ruby/Emerald 각각15행은13개 액체 중 Ferocity/Contempt의 양쪽 행을 포함한다. Diamond5행은 Isolation1행과 Ferocity/Contempt 각각2행이다. 선택 확률·crafted 동시 보유 수·family 충돌·fractured 제거 제외·빈 item·삭제 후 대상 side가 가득 찬 경우는 원문만으로 확정되지 않아 별도 검증해야 한다.

가장 작은 구현 순서:

1. `JL-01`: Sapphire normal generation snapshot과 slot/rarity 근거 확정, 기존 `sapphire:suffix:of-enchanting` ID·원본 roll·6언어 유지. Annulment/Divine부터 기존 선택 suffix에 positive 검증 후 정상 풀 기반18 기본 화폐 적용을 확장한다. tiered 후보 부재는 honest NO_CANDIDATE. Essence·ordinary Catalyst 제외를 유지한다.
2. `JL-02`: Rare Sapphire의 `Diluted_Liquid_Ire`, `Diluted_Liquid_Guilt`, `Diluted_Liquid_Greed`, `Liquid_Paranoia`, `Liquid_Envy`, `Liquid_Disgust`, `Liquid_Despair`, `Concentrated_Liquid_Fear`, `Concentrated_Liquid_Suffering`, `Concentrated_Liquid_Isolation`10종. 각 source Code/stat·family/side를 가져오고 삭제→보장 추가 전이를 원자적으로 검증한다. 후보가 확인되고 weight가 없을 때만 uniform-removal1/N을 명시한다.
3. `JL-03`: Ruby/Emerald 같은10종과 basic crafting을 base별 풀로 확장. Diamond는 Isolation부터 source 행에 한정한다. 새 base의 Refined Catalyst 적용 여부는 별도 tooltip 확인 후 확장한다.
4. `JL-04`: `Potent_Liquid_Melancholy`, `Potent_Liquid_Ferocity`, `Potent_Liquid_Contempt`는 조건부 tree 효과·opposite-affix scaling·추가 slot 연산을 분리한다. 방향 선택 확률, stacking, cap-loss 보존과 slot-loss 후 기존 affix 보존 규칙을 확인하고 독립 테스트한다. 미확인 probability를50/50으로 만들지 않는다.
5. `JL-05`: Ancient14는 Time-Lost base snapshot·radius·Small/Notable scope가 필요하다. 10 standard Ancient + Ancient Potent3 + Ancient Liquid Inspiration1을 별도 묶음으로 구현한다. 현 Basic Sapphire에 적용하지 않는다. Time-Lost 전체를 이 첫 batch에 억지로 포함하지 않는다.

새 endpoint와 UI는 stable material ID→명시적 action mapping, Problem Details, source/probability disclosure, late response 취소·Alt/Shift·film/undo/redo·local orange Omen preview·6언어를 유지한다. 격리 복사본·독점 heavy QA로 BE/FE 필수 검사와 API/browser positive/negative coverage를 순차 수행한다. live18080/18081·개인 browser/storage·DB volume·원본 repo·원격 반영은 건드리지 않는다. 모든27종이 요청되었지만 위 미확인 의미는 지원 완료로 표시하지 않는다.

원문 HTML/raw ModsView와 SHA256는 `codex/quality-preserve-20261004`에 보존했다. `Crafted` 페이지404도 source 실패 기록으로 유지한다. 다음 채팅은 해당 증거와 인벤토리를 재사용하고 최신 source 변경을 비교한다.


현재 진행 갱신: Sapphire JL-01의 ordinary58/실제 슬롯/기본18종과 JL-02 Basic Liquid10을 [새 구현 명세](workbench-sapphire-generation-liquid-2026-10-04.md)에서 다룬다. 기존 Annul/Divine 전용 handoff는 이 부분에서 superseded. Liquid inventory27은 Basic13+Ancient13+unrelated Liquid Verisium1이다. raw15/14는 modifier outcome 행 수이며 currency 수가 아니다. JL-03~05는 잔여다.
