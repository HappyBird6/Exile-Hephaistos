# Catalyst registry 완료 정합성

기준 commit `e0e0c09c68283581237da9abb3e6f2ae83eed0b8` 이후의 metadata 정리다. 제작 규칙·확률·base eligibility를 확장하지 않는다. 기존 Ancient API/browser 증거와 실패 이력은 그대로 보존한다.

ordinary Catalyst 13종의 `supportedBases`는 Solar Amulet(`solar`)·Iron Ring(`ring`)뿐이다. Refined Catalyst 13종은 Basic Ruby/Emerald/Sapphire/Diamond뿐이다. Time-Lost Jewel은 source에 `jewel_catalyst`가 없으므로 두 계열 모두 거부한다. 다른 reviewed item class도 지원하지 않는다. 종류별 action을 실제 `WorkbenchCurrency`와 연결하고 `IMPLEMENTED`로 정리했다. original source URL·조회 시각·hash는 유지하고 이전 pending 상태·reason·action은 `registrationHistory`에 보존했다. 반복·type 변경의 `max(existing,currentCap)` 및 cap-loss 품질 보존은 기존에 공개한 provisional 모델이며 game proof로 격상하지 않는다.

## 정확한 집계 정의

| 집계 | 수 | 의미 |
|---|---:|---|
| registered | 220 | 고유 registry material ID 전체 |
| active | 170 | 현재 서비스 범위 ID |
| implemented active | 170 | 선언한 base 중 최소 하나에 검증된 positive 실행 경로가 있는 active ID |
| default implemented | 163 | 위 170에서 opt-in legacy 7개를 제외 |
| opt-in legacy implemented | 7 | 기존 legacy opt-in 경계 유지 |
| active pending | 0 | active이면서 미구현인 ID |
| deferred | 50 | 현재 서비스에서 제외된 ID, active와 서로 겹치지 않음 |
| implemented deferred | 8 | 구현을 보존하지만 현재 범위에서 제외된 기존 ID |
| unimplemented deferred | 42 | 제외 상태의 미구현 ID |
| implemented overall | 178 | active170 + deferred8; 현재 사용 가능 수를 뜻하지 않음 |

`IMPLEMENTED`는 모든 base에 적용 가능하거나 game odds가 입증됐다는 뜻이 아니다. Liquid26·Jewel8개 base의 기본 화폐18종은 이전 완료 감사대로 유지한다. Diamond는 source에 존재하는 Liquid만 지원한다. Vaal/Hinekora/Desecration/Essence-on-Jewel 및 unrelated deferred 경계는 그대로다.

`CatalystRegistryTest`는 26개 action의 positive/negative 실제 실행과 선언한 base 집합의 일치, 이전 상태 보존, active/implemented/pending/deferred 집계 일치를 검사한다. 기존 quality·Jewel 회귀 검사와 함께 Backend aggregate를 실행한다. 기존 API positive/negative 1,004개와 Basic API 10,029개는 재실행하지 않고 동일 규칙의 증거로 참조한다. 최신 정리 증거는 [registry validation](evidence/workbench-catalyst-registry-validation-2026-10-04.json)에 기록한다. 이전 Ancient validation의 144/26 및 metadata caveat는 당시 snapshot 이력이며 최신 상태가 아니다.
