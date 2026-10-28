# 프로젝트 개요

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


Exile-Hephaistos는 PoE2 복사 텍스트를 해석하고, 검토된 카탈로그의 아이템 상태에 화폐를 적용하며, 목표까지의 제작 방법을 탐색하는 로컬 작업대다. 실제 게임 인벤토리·재료를 소비하는 도구가 아니다.

## 읽는 순서

- [지원 범위](support.md): 장비·언어·정책과 확인 범위.
- [아이템 상태](item-state.md), [텍스트 파싱](text-parsing.md): 표시용 입력을 검증된 상태로 바꾸는 경계.
- [Workbench](workbench.md): 단일 화폐 적용과 후보 자격.
- [자동 탐색](automatic-planning.md), [목표](goals.md), [그래프 UI](graph-ui.md): 계산·저장·화면을 각각 설명한다.
- [실행·개발](../../README.md), [데이터 유지보수](../catalog-data-maintenance.md), [문서 전체 지도](../README.md).

## 출처와 확률

카탈로그와 출처 스냅샷은 특정 수집 시점의 자료다. 게시 가중치와 명시된 균등 선택/roll 가정을 사용하며, 계산된 확률을 실제 게임의 검증된 확률로 확대 해석하지 않는다. 출처가 없는 효과·결합 분포를 임의 추정하지 않는다. 미선정 베이스와 미검증 게임 업데이트까지 포괄하지 않는다.

수집 원문과 해시는 [evidence](../evidence/), 실행 당시 기록은 [verification](../verification/), 출처 라이선스는 [third-party](../third-party/)에 있다. PathOfBuilding 참고 자료의 [MIT 고지](../third-party/PathOfBuilding-LICENSE.txt)를 보존한다. 표시 자산의 원저작권은 각 권리자에게 있으며 출처 기록이 재배포 허가를 의미하지 않는다. 파서의 고정 참조 소스와 checksum은 [텍스트 파싱](text-parsing.md#참조-소스와-라이선스)에 기록한다.

## 기존 계약과 현재 연결

| 문서군 | 현재 위치와 해석 |
|---|---|
| [first-hit](../basic-currency-first-hit.md), [전이](../basic-currency-transitions.md), [UI](../basic-currency-first-hit-ui.md) | BasicPathController/BasicPathService에 연결된 제한된 계산 경로. 주 자동 탐색 전체와 같은 계약이 아니다. |
| [Support](../support-transition-design.md), [목표 v1](../support-goal-filter-v1.md) | 구형 Support 계약·검증. 현재 목표는 goals 문서를 먼저 읽는다. |
| [path v1](../crafting-path-search-v1.md) | 구형 경로 탐색 설계와 연결 기록. 현행 자동 탐색 완료 범위로 읽지 않는다. |
| [bucket](../crafting-bucket-method-v2.md), [certified](../crafting-certified-map-ui.md), [lazy](../crafting-lazy-laws.md) | 구형 엔진의 설계·실험·미완료 경계가 섞여 있다. CertifiedMethodSearchEngine의 TemporaryWorkingLaws 등 실제 연결은 존재하나 모든 목표·순환의 계산 완료를 보장하지 않는다. |
| [store](../crafting-local-method-store.md), [strategy job](../crafting-strategy-job.md) | SupportConfiguration의 구형 methodSearchService는 LocalCertifiedJobRepository와 SqliteMethodGraphStore에 연결된다. 초기 문서의 미연결 설명은 당시 상태다. 현대 자동 작업의 JSON 저장과 구분한다. |

사용 중인 API·저장 계약과 미완료 이슈를 유지한다. 문서 통합으로 알고리즘을 재설계하거나 이슈를 종결하지 않는다.


## 코드와 API 구성

| 위치 | 책임 |
|---|---|
| `item` / `item.testparser` | 계산용 상태·catalog·검증과 표시용 영어 텍스트 분석 |
| `crafting.presentation` / `crafting.application` | HTTP 입력·응답, Workbench·Support·계산 job 조정 |
| `crafting.domain` / `crafting.infrastructure` | 제작 규칙·목표·확률 계산, 봉인 리소스와 저장소 연결 |
| `frontend/src/features/crafting/goal-filter` | 시작 아이템·목표 입력과 catalog 복구 |
| `frontend/src/features/crafting/method-search` | 자동 계산 제어·그래프·시나리오 |

표시 파서는 `POST /api/v1/items/parse`, 수동 simulator는 `/api/v1/crafting/workbench`, 목표 판정은 `/api/v1/crafting/support/goal-filters`, 현행 자동 계산은 `/api/v2/crafting/method-searches/automatic`을 사용한다. 구형 family Support·버킷 탐색·method API는 각 계약의 별도 경로를 유지한다.

제작 POST는 `X-Crafting-Ruleset`으로 입력의 규칙 출처를 명시하며 목표의 `catalogVersion`도 별도로 검사한다. 사용자 로그인·관리자 인증은 없고 method job 소유권은 `crafting_method_client` cookie로 구분한다. 소유권 cookie를 공개 운영용 사용자 인증으로 해석하지 않는다.

Workbench film은 브라우저에, 자동 job은 로컬 JSON에 저장한다. 기존 Support addition pool은 PostgreSQL cache, 구형 certified 계산은 SQLite 기반 저장을 사용한다. Redis 연결 기반과 readiness는 제작 계산의 완결을 보장하지 않는다. [Frontend 구조와 검사](../../frontend/README.md), [자동 저장·재개](automatic-planning.md)를 함께 확인한다.

## 구현 근거

[SupportConfiguration.java](../../backend/src/main/java/com/poe2craft/bootstrap/SupportConfiguration.java) · [CraftingConfiguration.java](../../backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java) · [LocalCertifiedJobRepository.java](../../backend/src/main/java/com/poe2craft/crafting/infrastructure/LocalCertifiedJobRepository.java)

[문서 지도](../README.md)
