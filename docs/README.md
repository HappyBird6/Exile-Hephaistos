# 문서 지도

이 지도는 `dbd76a7`의 문서를 기준으로 2026-10-28에 분류했다. 문서 위치만 정리했으며, 과거 보고서의 정책·검증 결과·미완료 상태를 현재 결과로 바꾸지 않았다.

## 먼저 읽을 문서

| 목적 | 문서 | 읽을 때 주의할 점 |
|---|---|---|
| 프로젝트 실행·검증 | [프로젝트 README](../README.md) | 실행 절차와 날짜별 개발 이력이 함께 있다. 과거 지원 숫자를 현재 집계로 읽지 않는다. |
| 개발 운영 | [개발 지침](../AGENTS.md), [Worker 운영](solo-workflow/MULTI_SESSION_WORKFLOW.md), [Git 통합](solo-workflow/GIT_INTEGRATION.md) | 운영 지침의 기존 위치와 권한 경계를 유지한다. |
| 기술 개요·상태 트리 hover/focus | [기술 명세](TECHNICAL_SPEC.md#상태-트리-후속-경로-강조) | 앞부분에 과거 장비 확장 이력이 누적되어 있다. |
| 자동 탐색·6옵션 | [자동 정책 구현](crafting-automatic-policy-implementation.md), [6옵션 검증·한계](automatic-six-affix-2026-10-28.md) | 최대 6개는 탐색 상한이다. 장비·희귀도별 접두/접미 한도와 기존 계산 예산은 별도로 적용한다. |
| 현재 지원 범위 확인 | [지원 범위](supported-mechanics.md), [Workbench 모델](workbench-simulator.md) | 여러 시점의 집계가 섞여 있어 각 항목의 날짜·후속 문서를 함께 확인한다. |
| 아이템 상태·파싱 | [ItemState](item-state.md), [영어 텍스트 파서](item-text-parsing.md), [파싱 OpenAPI](openapi-item.yaml) | 표시용 파싱과 catalog 검증된 제작 상태를 구분한다. |
| 데이터 변경·출처 | [Catalog 유지보수](catalog-data-maintenance.md), [출처 안내](data-sources.md), [초기 UI·자산 기록](crafting-ui.md) | 초기 UI/출처 안내에는 현행 기능과 다른 과거 설명이 남아 있다. |
| 미해결 작업 | [이슈 목록](../ISSUES.md) | archive 이동은 이슈 해결·종결을 뜻하지 않는다. |

## 승인 자료와 유지한 정책

- [187개 재료 승인 원문](crafting-currency-policy-approved-20261015.md)은 **2026-10-15 당시 승인 자료**다. 원문의 4옵션 경계·구현 보류 표현은 현행 상한을 뜻하지 않는다. 현재 6옵션 범위는 위 자동 정책 구현과 6옵션 보고서를 따른다. 승인 원문의 내용은 보존했다.
- [품질 상한 감소 시 기존 품질 보존](workbench-quality-preserve-2026-10-04.md)은 사용자 확인으로 정정된 정책이므로 날짜만 보고 archive로 옮기지 않았다.

## 구형 계약·실험·진행 중 설계

다음 문서는 구형 결과 호환이나 설계 근거를 담는다. 현행 자동 탐색 전체의 완료 선언이 아니며, 실행 코드와의 대응 검토 전까지 기존 경로를 유지한다.

| 문서 | 내용·성격 |
|---|---|
| [기본 화폐 단일 전이](basic-currency-transitions.md) | 유한 분포·후보·확률 질량의 기반 계약 |
| [first-hit 계산](basic-currency-first-hit.md) | 명시적 정책의 최초 도달·조건부 복구 계약 |
| [first-hit UI](basic-currency-first-hit-ui.md) | 실험 화면 및 검증 범위 |
| [초기 Support 전이](support-transition-design.md) | Solar 유한 추가 순서 모델과 당시 조사 |
| [목표 필터 v1](support-goal-filter-v1.md) | 공통 AST·그룹 연산 계약 |
| [목표 판정 검증](support-goal-verification.md) | 기존 판정과 추천 계산의 일치성 기록 |
| [경로 검색 v1](crafting-path-search-v1.md) | 당시 구현 전 공통 API 계약 |
| [v1 Backend 인계](crafting-path-search-backend-v1.md) | 구형 구현과 계약 연결 기록 |
| [v1 통합 검증](crafting-path-search-integration-v1.md) | 구형 프런트·백엔드 결합 검증 |
| [bucket 계산 v2](crafting-bucket-method-v2.md) | 계약과 긴 버전별 구현·성능 이력 |
| [certified 지도 UI](crafting-certified-map-ui.md) | certified 결과의 표시 계약 |
| [lazy 확률 법칙](crafting-lazy-laws.md) | 압축·지연 로딩·조건부 재생 및 미완료 경계 |
| [로컬 방법 저장](crafting-local-method-store.md) | SQLite 저장 기반; 엔진 전체 연결 완료와 구분 |
| [전략 계산 작업](crafting-strategy-job.md) | 제한된 그래프의 전략 계산·중단/재개 |
| [Solar 부하 측정](crafting-solar-workload.md) | 대형 목표의 당시 계산·메모리 한계 |
| [초기 Workbench v2 목록](workbench-support-v2.md) | 당시 Solar 지원표; 현행 전체 지원표가 아님 |

## 미완료 계획·근거 조사

- [다음 장비 후보](workbench-next-equipment-plan-2026-10-05.md): 당시 후보와 후속 구현을 대조해야 한다.
- [주얼·Liquid 계획](workbench-jewel-liquid-next-2026-10-04.md): 계획과 완료 정정 이력을 함께 보존한다.
- [감정 주문서 근거](workbench-wisdom-evidence-2026-10-03.md): 숨겨진 옵션 입력·보존 계약의 미해결 근거다. 문서 정리로 완료 처리하지 않는다.

## 출처·검증·라이선스 보관소

| 위치 | 저장 내용 | 이번 정리의 경계 |
|---|---|---|
| [evidence](evidence/) | 수집 JSON/HTML, 출처 URL·시각·해시, QA 결과·화면·프로브 | 5,231개 파일의 기존 경로와 바이트를 보존했다. source/runtime 이름만으로 자료 성격을 단정하지 않는다. |
| [verification](verification/) | 기능 통합·브라우저 검증과 대상 소스 해시 | 24개 파일 보존. 당시 실행의 증거이며 현재 전체 검사 결과가 아니다. |
| [benchmarks](benchmarks/) | 대형 계산·저장·복구 재현 프로브 | 8개 파일 보존. 환경별 경로를 가진 수동 실험 코드다. |
| [third-party](third-party/) | [PathOfBuilding MIT 고지](third-party/PathOfBuilding-LICENSE.txt) | 라이선스 원문 보존 |

## 과거 보고서 색인

아래 92개 보고서는 `archive/2026-10/`으로 이동했다. 파일명·날짜·본문 의미를 유지하고 경로 참조만 갱신했다. 보고서의 과거 승인·중단·배포 상태는 해당 시점의 기록이며 새로운 실행 지시가 아니다.

### Workbench 구현·QA (64개)

- [workbench-amulets-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-amulets-bundle-2026-10-04.md)
- [workbench-ancient-liquid-2026-10-04.md](archive/2026-10/workbench/workbench-ancient-liquid-2026-10-04.md)
- [workbench-armour-source-preflight-2026-10-04.md](archive/2026-10/workbench/workbench-armour-source-preflight-2026-10-04.md)
- [workbench-attuned-wand-2026-10-03.md](archive/2026-10/workbench/workbench-attuned-wand-2026-10-03.md)
- [workbench-basic-jewel-potent-2026-10-04.md](archive/2026-10/workbench/workbench-basic-jewel-potent-2026-10-04.md)
- [workbench-belts-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-belts-bundle-2026-10-05.md)
- [workbench-body-helmet-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-body-helmet-bundle-2026-10-04.md)
- [workbench-boots-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-boots-bundle-2026-10-04.md)
- [workbench-bow-perfect-2026-10-03.md](archive/2026-10/workbench/workbench-bow-perfect-2026-10-03.md)
- [workbench-bows-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-bows-bundle-2026-10-04.md)
- [workbench-catalyst-quality-2026-10-04.md](archive/2026-10/workbench/workbench-catalyst-quality-2026-10-04.md)
- [workbench-catalyst-registry-2026-10-04.md](archive/2026-10/workbench/workbench-catalyst-registry-2026-10-04.md)
- [workbench-catalyst-ux-2026-10-04.md](archive/2026-10/workbench/workbench-catalyst-ux-2026-10-04.md)
- [workbench-continuation-integration-2026-10-03.md](archive/2026-10/workbench/workbench-continuation-integration-2026-10-03.md)
- [workbench-crossbows-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-crossbows-bundle-2026-10-05.md)
- [workbench-crude-bow-2026-10-03.md](archive/2026-10/workbench/workbench-crude-bow-2026-10-03.md)
- [workbench-development-deferrals-2026-10-03.md](archive/2026-10/workbench/workbench-development-deferrals-2026-10-03.md)
- [workbench-evasion-armour-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-evasion-armour-bundle-2026-10-04.md)
- [workbench-gloves-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-gloves-bundle-2026-10-04.md)
- [workbench-hallowed-sceptre-2026-10-04.md](archive/2026-10/workbench/workbench-hallowed-sceptre-2026-10-04.md)
- [workbench-homogenising-legacy-2026-10-03.md](archive/2026-10/workbench/workbench-homogenising-legacy-2026-10-03.md)
- [workbench-iron-ring-2026-10-03.md](archive/2026-10/workbench/workbench-iron-ring-2026-10-03.md)
- [workbench-maces-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-maces-bundle-2026-10-05.md)
- [workbench-offhands-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-offhands-bundle-2026-10-05.md)
- [workbench-omen-composition-2026-10-04.md](archive/2026-10/workbench/workbench-omen-composition-2026-10-04.md)
- [workbench-quality-review-2026-10-03.md](archive/2026-10/workbench/workbench-quality-review-2026-10-03.md)
- [workbench-quality-socket-boundary-2026-10-03.md](archive/2026-10/workbench/workbench-quality-socket-boundary-2026-10-03.md)
- [workbench-quarterstaves-spears-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-quarterstaves-spears-bundle-2026-10-05.md)
- [workbench-quivers-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-quivers-bundle-2026-10-05.md)
- [workbench-rattling-sceptre-2026-10-03.md](archive/2026-10/workbench/workbench-rattling-sceptre-2026-10-03.md)
- [workbench-rawhide-belt-2026-10-03.md](archive/2026-10/workbench/workbench-rawhide-belt-2026-10-03.md)
- [workbench-rawhide-belt-source-preparation-2026-10-03.md](archive/2026-10/workbench/workbench-rawhide-belt-source-preparation-2026-10-03.md)
- [workbench-refined-catalyst-2026-10-04.md](archive/2026-10/workbench/workbench-refined-catalyst-2026-10-04.md)
- [workbench-remaining-gaps-2026-10-03.md](archive/2026-10/workbench/workbench-remaining-gaps-2026-10-03.md)
- [workbench-remaining-materials-2026-10-03.md](archive/2026-10/workbench/workbench-remaining-materials-2026-10-03.md)
- [workbench-remaining-triage-2026-10-02.md](archive/2026-10/workbench/workbench-remaining-triage-2026-10-02.md)
- [workbench-remaining-triage-2026-10-03.md](archive/2026-10/workbench/workbench-remaining-triage-2026-10-03.md)
- [workbench-rings-bundle-2026-10-04.md](archive/2026-10/workbench/workbench-rings-bundle-2026-10-04.md)
- [workbench-rule-audit-2026-10-02.md](archive/2026-10/workbench/workbench-rule-audit-2026-10-02.md)
- [workbench-rusted-cuirass-2026-10-03.md](archive/2026-10/workbench/workbench-rusted-cuirass-2026-10-03.md)
- [workbench-rusted-greathelm-2026-10-03.md](archive/2026-10/workbench/workbench-rusted-greathelm-2026-10-03.md)
- [workbench-sapphire-existing-currency-2026-10-04.md](archive/2026-10/workbench/workbench-sapphire-existing-currency-2026-10-04.md)
- [workbench-sapphire-generation-liquid-2026-10-04.md](archive/2026-10/workbench/workbench-sapphire-generation-liquid-2026-10-04.md)
- [workbench-sceptres-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-sceptres-bundle-2026-10-05.md)
- [workbench-scope-update-2026-10-03.md](archive/2026-10/workbench/workbench-scope-update-2026-10-03.md)
- [workbench-service-scope-2026-10-04.md](archive/2026-10/workbench/workbench-service-scope-2026-10-04.md)
- [workbench-status-2026-10-04.md](archive/2026-10/workbench/workbench-status-2026-10-04.md)
- [workbench-staves-talismans-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-staves-talismans-bundle-2026-10-05.md)
- [workbench-stocky-abyss-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-abyss-2026-10-03.md)
- [workbench-stocky-artificer-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-artificer-2026-10-03.md)
- [workbench-stocky-basic-essences-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-basic-essences-2026-10-03.md)
- [workbench-stocky-horror-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-horror-2026-10-03.md)
- [workbench-stocky-hysteria-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-hysteria-2026-10-03.md)
- [workbench-stocky-mitts-catalog-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-mitts-catalog-2026-10-03.md)
- [workbench-stocky-mitts-preparation-2026-10-02.md](archive/2026-10/workbench/workbench-stocky-mitts-preparation-2026-10-02.md)
- [workbench-stocky-mitts-runtime-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-mitts-runtime-2026-10-03.md)
- [workbench-stocky-perfect-glove-essences-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-perfect-glove-essences-2026-10-03.md)
- [workbench-stocky-prismatic-alloy-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-prismatic-alloy-2026-10-03.md)
- [workbench-stocky-reviewed-alloys-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-reviewed-alloys-2026-10-03.md)
- [workbench-stocky-scalar-alloys-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-scalar-alloys-2026-10-03.md)
- [workbench-stocky-special-evidence-2026-10-03.md](archive/2026-10/workbench/workbench-stocky-special-evidence-2026-10-03.md)
- [workbench-top-bases-2026-10-04.md](archive/2026-10/workbench/workbench-top-bases-2026-10-04.md)
- [workbench-ui-feedback-2026-10-04.md](archive/2026-10/workbench/workbench-ui-feedback-2026-10-04.md)
- [workbench-wands-bundle-2026-10-05.md](archive/2026-10/workbench/workbench-wands-bundle-2026-10-05.md)

### 자동 탐색·목표·UI 검증 (15개)

- [automatic-admission-review-2026-10-19.md](archive/2026-10/automatic-planning/automatic-admission-review-2026-10-19.md)
- [automatic-preparation-grade-2026-10-21.md](archive/2026-10/automatic-planning/automatic-preparation-grade-2026-10-21.md)
- [automatic-refinements-2026-10-21.md](archive/2026-10/automatic-planning/automatic-refinements-2026-10-21.md)
- [automatic-refinements-verification-2026-10-21.md](archive/2026-10/automatic-planning/automatic-refinements-verification-2026-10-21.md)
- [automatic-tier-distribution-2026-10-22.md](archive/2026-10/automatic-planning/automatic-tier-distribution-2026-10-22.md)
- [crafting-ux-review-2026-10-19.md](archive/2026-10/automatic-planning/crafting-ux-review-2026-10-19.md)
- [empty-rare-chaos-capacity-review-2026-10-19.md](archive/2026-10/automatic-planning/empty-rare-chaos-capacity-review-2026-10-19.md)
- [goal-catalog-recovery-2026-10-19.md](archive/2026-10/automatic-planning/goal-catalog-recovery-2026-10-19.md)
- [goal-directed-planner-2026-10-19.md](archive/2026-10/automatic-planning/goal-directed-planner-2026-10-19.md)
- [goal-equivalence-review-2026-10-19.md](archive/2026-10/automatic-planning/goal-equivalence-review-2026-10-19.md)
- [goal-planner-independent-review-2026-10-19.md](archive/2026-10/automatic-planning/goal-planner-independent-review-2026-10-19.md)
- [goal-ui-outcomes-review-2026-10-19.md](archive/2026-10/automatic-planning/goal-ui-outcomes-review-2026-10-19.md)
- [reviewed-rate-goals-2026-10-23.md](archive/2026-10/automatic-planning/reviewed-rate-goals-2026-10-23.md)
- [scenario-ui-review-2026-10-19.md](archive/2026-10/automatic-planning/scenario-ui-review-2026-10-19.md)
- [unsupported-attributes-2026-10-23.md](archive/2026-10/automatic-planning/unsupported-attributes-2026-10-23.md)

### Catalog·registry 변경 (3개)

- [base-registry-refactor-2026-10-05.md](archive/2026-10/catalog/base-registry-refactor-2026-10-05.md)
- [catalog-maintenance-checkpoint-2026-10-09.md](archive/2026-10/catalog/catalog-maintenance-checkpoint-2026-10-09.md)
- [catalog-recovery-2026-10-21.md](archive/2026-10/catalog/catalog-recovery-2026-10-21.md)

### 다국어 구현·통합 (3개)

- [i18n-2026-10-04.md](archive/2026-10/localization/i18n-2026-10-04.md)
- [i18n-omen-integration-2026-10-04.md](archive/2026-10/localization/i18n-omen-integration-2026-10-04.md)
- [i18n-remaining-2026-10-04.md](archive/2026-10/localization/i18n-remaining-2026-10-04.md)

### master 통합 기록 (1개)

- [master-integration-2026-10-23.md](archive/2026-10/integration/master-integration-2026-10-23.md)

### 중단·인계 체크포인트 (6개)

- [crafting-bucket-method-handoff-20261012.md](archive/2026-10/checkpoints/crafting-bucket-method-handoff-20261012.md)
- [crafting-lazy-laws-checkpoint-2026-10-13.md](archive/2026-10/checkpoints/crafting-lazy-laws-checkpoint-2026-10-13.md)
- [crafting-storage-resume-20261014.md](archive/2026-10/checkpoints/crafting-storage-resume-20261014.md)
- [overnight-summary-2026-10-02.md](archive/2026-10/checkpoints/overnight-summary-2026-10-02.md)
- [STOP_CHECKPOINT-2026-10-01.md](archive/2026-10/checkpoints/STOP_CHECKPOINT-2026-10-01.md)
- [STOP_CHECKPOINT-2026-10-02.md](archive/2026-10/checkpoints/STOP_CHECKPOINT-2026-10-02.md)

## 다음 정리 단계

현재 개요·지원 범위·Workbench 설명의 반복 이력을 분리하고 주제별 대표 문서로 통합하는 것은 2차 작업이다. 구형 계약·미완료 설계의 코드 대응, 증거 자료의 역참조, 완전 중복 자료가 각 검증 이력에서 맡는 역할을 확인한 뒤 판단한다. 이번 단계에는 파일 삭제·증거 단일화·정책 변경·이슈 종결을 포함하지 않았다.
