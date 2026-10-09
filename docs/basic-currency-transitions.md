# 기본 화폐 단일 전이 기반

이 묶음은 제작 경로 순위·제작 예산·복구 정책을 결정하지 않는다. Workbench와 기존 API를 유지하고, 후속 탐색기가 사용할 단일 화폐의 유한 분포와 lossless 상태를 제공한다. 현재 API/UI에는 연결하지 않는다.

## 범위

기본 화폐는 Workbench 첫 `Currency` 탭을 뜻한다. 이번 전이 action은 `TRANSMUTATION`, `AUGMENTATION`, `REGAL`, `EXALTED` 각각의 일반/`GREATER_`/`PERFECT_` 12종, `ANNULMENT`, `CHAOS`/`GREATER_CHAOS`/`PERFECT_CHAOS`의 총 16종이다. Greater/Perfect의 minimum modifier level 및 family별 최고 eligible modifier 예외는 기존 `AdditionRules`를 그대로 따른다.

`ALCHEMY`, `DIVINE`, `FRACTURING`, `ARTIFICER`는 기존 Workbench 구현을 유지하지만 이 전이 모델에는 아직 포함하지 않는다. `Vaal_Orb`, `Hinekoras_Lock`, `Mirror_of_Kalandra`는 보류 상태를 유지한다. Essence/Alloy/Omen/Catalysts/Liquid는 포함하지 않는다. Omen 입력은 명시적인 빈 set이어야 한다.

현재 생성 전이는 Solar Amulet와 `POE2DB_AS_PUBLISHED` catalog에 한정한다. 서비스 전체의 134-base 지원을 줄이지 않는다. 작은 synthetic catalog는 별도 snapshot/source를 갖는 테스트 fixture이며 production catalog를 변경하거나 줄이지 않는다.

## 공유 규칙과 확률 해석

Workbench의 no-omen 기본 화폐 계획을 샘플 적용과 유한 전이가 함께 사용한다. availability, rarity upgrade, family/affix/level pool, fractured 제거 보호와 삭제 후 replacement pool을 다시 구현하지 않는다. Chaos는 제거 가능한 explicit 1개를 삭제한 뒤 남은 상태의 pool에서 1개를 생성한다. 같은 modifier와 원래 roll이 다시 나오면 self-loop이고, 서로 다른 제거/생성 분기가 같은 완전 상태에 도달하면 확률을 합한다. 카오스는 전체 옵션 리롤이 아니다.

`COMPLETE`는 선언된 simulator 모델의 분포를 모두 열거했다는 뜻이며 검증된 게임 확률을 뜻하지 않는다. 후보 선택은 기존 PoE2DB weight/eligible total, 제거는 기존 `uniform-removal-v1`의 후보 instance별 `1/N`, 생성 수치는 기존 `uniform-integer-roll-v1`의 단일-stat source min..max 정수 균등 가정이다. Source URL, catalog source digest, Workbench rule/ledger, 새 전이 model version, ruleset identity를 결과에 보존한다. 실제 removal/roll weight는 검증되지 않았다.

새로 생성되는 후보 중 multi-stat/joint roll이 있으면 전체 전이가 `UNSUPPORTED`이며 일부 후보만 남겨 분모를 바꾸지 않는다. 기존 multi-stat instance는 제거하지 않는 한 값과 상관관계를 그대로 보존하지만, 이 사실은 해당 stat의 숫자 목표 평가/확률 지원을 의미하지 않는다. quality는 원래 source roll과 함께 보존하며 기존 사용자 cap-loss 정책을 따른다. 표시값 투영·반올림과 numeric goal의 미지원 경계는 그대로다.

## 상태·identity·한도 계약

상태는 전체 `ItemState`와 명시적인 provenance를 함께 보존한다. snapshot/base/item level/rarity/implicit/explicit/source-unit values/fractured/conditions/known-or-unknown socket/quality를 삭제하거나 평균화하지 않는다. canonical key는 길이 구분 binary encoding으로 계산한다. item modifier/map/set 순서는 의미상 canonical하게 처리하고, catalog modifier·stat 배열과 compatible snapshot 배열의 원래 순서는 fingerprint에 보존한다. ruleset/model/catalog 내용이 다르면 같은 item이라도 cache를 공유하지 않는다. identity는 인증 토큰이나 과거 시즌 호환 선언이 아니다.

정확히 같은 full state만 합친다. family 수·목표 합계·현재 통과 여부가 같다는 이유로 다른 roll을 합치지 않는다. 더 작은 bucket은 모든 허용 action의 그룹별 전이 확률과 목표 판정이 같다는 증명 및 oracle 회귀가 있어야 후속 단계에서 도입할 수 있다.

`UNAVAILABLE`은 유효한 상태에서 화폐가 적용 불가능하다는 뜻이다. `UNSUPPORTED`는 base/action/특수 상태/Omen/roll 분포가 모델 밖이라는 뜻이다. 두 경우 모두 결과 상태와 source provenance를 보존하고 분포를 만들지 않는다. null 입력·빈 identity·잘못된 concrete state·다른 provenance는 입력 오류다.

단일 전이에도 elementary outcome 한도를 명시한다. 한도까지 열거한 확률과 정확한 나머지 질량을 반환하고 `PARTIAL`로 표시한다. 결과를 재정규화하거나 잘라낸 질량을 실패로 취급하지 않는다. `COMPLETE`에서는 결과 확률 합이 정확히 1이고 `PARTIAL`에서는 결과 합+unresolved가 정확히 1이다. bounded cache는 완전 결과만 보관하고 partial은 저장하지 않는다. 거대한 전체 그래프 사전계산이나 checkpoint 저장은 구현하지 않는다.

## 검증과 다음 묶음

작은 synthetic fixture의 독립 brute-force elementary cases와 fraction oracle로 삭제/교체/self-loop/동일 결과 합산/순서 민감성/분열 보호/한도 질량을 검증한다. 실제 Solar 사례는 별도 테스트로 16개 action·기존 생성 규칙·원래 roll과 provenance 보존을 검증한다. Workbench refactor 이전 seed 결과 digest를 고정해 결과·events·assumptions·random draw 순서를 대조한다. cache는 ruleset·catalog·숫자 값 구분과 bounded eviction을 검증한다.

2026-10-09 단일 전이 probe: Java 21.0.12.1, `-Xmx512m`, Docker Desktop에서 Solar level82 Normal + `TRANSMUTATION`의 elementary/unique 결과는 각각 3,378개였다. 새 JVM에서 catalog 초기화 이후 첫 계산은 281.187ms, 동일 complete 결과의 cache hit 1,000회 평균은 5.909μs였다. 한도를 1개로 낮춘 요청은 cached complete 결과를 그대로 반환하지 않고 `PARTIAL`, unresolved `21082/21107`을 반환했다. 당시 같은 호스트에서 전체 회귀 검사도 실행 중이었다. 이 단일 측정은 서비스 지연·메모리 사용량·수억 상태 규모의 성능 보장이 아니며 큰 탐색의 성능은 후속 측정이 필요하다.

다음 묶음은 연금술·신성·분열·숙련공의 분포 및 검증된 추가 base/roll 모델이다. 유한 경로 탐색·3~5개 순위·복구 점프·제작 예산·checkpoint·UI는 사용자 정책 결정과 별도 구현을 요구한다. 기존 `ExactNumericDistribution`은 proven finite joint tuple 검증용 oracle이며 실제 게임 joint 분포를 자동 제공하지 않는다.
