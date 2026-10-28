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

수집 원문과 해시는 [evidence](../evidence/), 실행 당시 기록은 [verification](../verification/), 출처 라이선스는 [third-party](../third-party/)에 있다. PathOfBuilding 참고 자료의 [MIT 고지](../third-party/PathOfBuilding-LICENSE.txt)를 보존한다. 과거 출처 설명 전체는 [원문 스냅샷](../archive/2026-10/spec-snapshots-49190b5/README.md)에서 읽는다.

## 기존 계약과 현재 연결

| 문서군 | 현재 위치와 해석 |
|---|---|
| [first-hit](../basic-currency-first-hit.md), [전이](../basic-currency-transitions.md), [UI](../basic-currency-first-hit-ui.md) | BasicPathController/BasicPathService에 연결된 제한된 계산 경로. 주 자동 탐색 전체와 같은 계약이 아니다. |
| [Support](../support-transition-design.md), [목표 v1](../support-goal-filter-v1.md), [검증](../support-goal-verification.md) | 구형 Support 계약·검증. 현재 목표는 goals 문서를 먼저 읽는다. |
| [path v1](../crafting-path-search-v1.md), [backend](../crafting-path-search-backend-v1.md), [integration](../crafting-path-search-integration-v1.md) | 구형 경로 탐색 설계와 연결 기록. 현행 자동 탐색 완료 범위로 읽지 않는다. |
| [bucket](../crafting-bucket-method-v2.md), [certified](../crafting-certified-map-ui.md), [lazy](../crafting-lazy-laws.md) | 구형 엔진의 설계·실험·미완료 경계가 섞여 있다. CertifiedMethodSearchEngine의 TemporaryWorkingLaws 등 실제 연결은 존재하나 모든 목표·순환의 계산 완료를 보장하지 않는다. |
| [store](../crafting-local-method-store.md), [strategy job](../crafting-strategy-job.md) | SupportConfiguration의 구형 methodSearchService는 LocalCertifiedJobRepository와 SqliteMethodGraphStore에 연결된다. 초기 문서의 미연결 설명은 당시 상태다. 현대 자동 작업의 JSON 저장과 구분한다. |
| [Solar workload](../crafting-solar-workload.md) | 당시 부하 실험. 현행 일반 성능 보장치가 아니다. |

이 계약 원문과 미완료 이슈는 그대로 보존한다. 문서 통합으로 알고리즘을 재설계하거나 이슈를 종결하지 않는다.


## 구현 근거

[SupportConfiguration.java](../../backend/src/main/java/com/poe2craft/bootstrap/SupportConfiguration.java) · [CraftingConfiguration.java](../../backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java) · [LocalCertifiedJobRepository.java](../../backend/src/main/java/com/poe2craft/crafting/infrastructure/LocalCertifiedJobRepository.java)

[문서 지도](../README.md)
