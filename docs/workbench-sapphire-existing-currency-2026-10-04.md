# Sapphire 기존 옵션 제거·재굴림 경계

기준 SHA: `f454ad62b1ef5d7d70124182251dbac82bf4f59f`. 이 변경은 JL-01의 기존 옵션 Annulment/Divine 부분만 구현한다. Sapphire 전체 기본 제작 및 Basic Liquid 완료를 의미하지 않는다.

## 실제 지원

- Magic/Rare Sapphire의 기존 `sapphire:suffix:of-enchanting` 옵션에 `Orb_of_Annulment` → `ANNULMENT`, `Divine_Orb` → `DIVINE`를 연결한다. 기존 stable ID와 source 원문 범위 2–4를 유지한다.
- Annulment는 옵션을 제거하며 rarity와 typed quality를 유지한다. 빈 아이템에서 반복 사용은 변경·Omen 소모 없이 거절한다.
- Divine은 Catalyst 표시값 대신 원본 2–4 범위를 다시 굴린다. 3개의 정수 후보에 대한 균등 분포는 기존 명시적 simulator 모델이며 게임의 roll weight를 주장하지 않는다. 결과가 원래 값과 같아도 유효하다.
- 기존 matching Omen 제한과 무관한 Omen 보존, film 직렬화, 응답 검증을 그대로 사용한다. Prefix-only Annulment와 implicit-only Blessed Divine은 이 suffix-only 상태에서 거절한다.
- Refined13은 기존대로 지원한다. Normal/Unique/special state, Essence, ordinary Catalyst, generation, Liquid, Fracturing 및 socket 작업을 새로 허용하지 않는다.

`sapphire-existing-reroll-removal-v1`은 실행 규칙의 변경을 표시한다. catalog snapshot과 기존 film의 modifier ID/roll은 바뀌지 않는다. 기존 editor의 suffix 1개 제한은 실제 Jewel 최대 slot 수로 설명하지 않는다.

## 근거 및 남은 공백

[Currency source](https://poe2db.tw/us/Currency)의 현재 item 대상 Annulment 제거와 Divine numeric modifier 재굴림 규칙을 사용한다. Sapphire의 실제 item rarity 및 기존 검증된 suffix 범위를 함께 확인하고, 새로운 affix 생성·slot 확대를 수반하지 않는 상태에만 적용한다. Artificer's Orb의 명시적 weapon/armour 대상과 구분하며 tooltip에 Equipment 단어가 없다는 이유만으로 첫 tab 전체를 허용하지 않는다.

[Sapphire](https://poe2db.tw/us/Sapphire)는 rarity `magic, rare, unique`, base tag `jewel, intjewel`를 제공한다. 보존된 raw snapshot의 일반 section은 58행이다. `DropChance=1`을 spawn weight로 해석하지 않는다. 최신 source는 modifier weight를 게임 파일에서 얻을 수 없다고 명시한다. 일반 후보와 Liquid/corrupted/desecrated 후보는 다른 section이다.

[기존 inventory](evidence/jewel-liquid-next-inventory-2026-10-04.json)와 [다음 계획](workbench-jewel-liquid-next-2026-10-04.md)을 이어 사용한다. Basic13과 Ancient14는 별개이며 [Liquid Emotions](https://poe2db.tw/us/Liquid_Emotions)의 현재 tooltip은 Rare Basic/Time-Lost Jewel을 구분한다. 최신 `Crafted` 페이지는 읽기 실패, modifier hover는 HTTP403을 반환했다. 이것이 modifier 자체 미지원의 증거는 아니지만 crafted uniqueness/충돌/제거 예외를 확인했다는 주장도 할 수 없다.

권장 다음 작업:

1. JL-01 잔여: 현재 게임 또는 명시적 current-game source로 Magic1P/1S·Rare2P/2S를 확인하고 일반 58행의 base eligibility/stat 범위·family를 검증한다. 검증된 전체 후보에 weight가 없으면 item별 disclosed1/N fallback을 사용한다. 기존 source에 알려진 편향이 있으면 보존한다. Normal rarity와 Transmutation/Alchemy를 임의로 허용하지 않는다.
2. JL-02: Rare Sapphire의 ordinary Basic Liquid10. Crafted 표식은 serialized state에서 유지해야 하며 기존 Crafted 제거/교체·동일 family 충돌·side-full 분기를 각각 확인한다. 증거가 없는 경우 권장 reversible model은 명시적으로 독립한 policy로 설명하고, source-confirmed rule과 구분한다. 아직 그 model은 구현하지 않았다.
3. 다른 Basic Jewel bases → Potent3 → Ancient14 순서로 진행한다. Essence-on-Jewel과 기존 deferred scopes는 제외한다.

quality40에서 cap20으로 감소할 때 값/type/original roll을 유지하는 사용자 확인 규칙과 Catalyst reuse의 `max(existing,currentCap)` simulator policy는 변경하지 않는다.

## 검증·자체 리뷰

격리 Docker 복사본에서 Backend `check generateJooq bootJar`(formatter 포함), unit374 및 integration6을 통과했다. Frontend `npm ci`, lint, typecheck, format:check, unit370, build를 통과했다. 실제 API97·browser37, page errors0. 검사 복사본과 현재 source는 Backend216/Frontend107 파일 모두 byte 동일하며 실행 JAR hash도 검사 JAR과 일치한다.

Alt/Shift, Whittling local orange preview, film undo/redo/reload, invalid input, 기존 suffix 제거와 품질 보존, six-language scope 안내를 확인했다. 영어 desktop과 모든 비영어390px·주황색 후보 screenshot을 직접 검토했다. browser harness의 첫 preview 실패는 Dextral-only 상태에서 Whittling 전용 강조를 기대한 오류였고, 실패 증거를 남긴 뒤 기존 규칙에 맞춰 수정했다. product 동작이나 기대 결과를 약화하지 않았다.

Compose quiet 검사 통과. 격리 서비스만 정상 종료했고 live18080/18081·기존 DB/Redis·사용자 browser/storage·원본 output을 보존했다. 이전 API2108/browser594는 이번 실행 결과로 재사용하지 않았다. [실제 검증 증거](evidence/workbench-sapphire-existing-validation-2026-10-04.json). 자체 리뷰 결과 기존 suffix 경계 내 차단 결함은 없으며 전체 요청은 아직 미완료다.
