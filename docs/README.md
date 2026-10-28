# 프로젝트 문서

현행 구현은 아래 대표 문서에서 확인한다. 실행·검사 명령은 [프로젝트 README](../README.md), 개발 운영은 [AGENTS.md](../AGENTS.md)를 따른다.

## 현행 명세

[사용자 제작 규칙](current/user-crafting-rules.md) — 최신 사용자 정책·대체된 지시·구현 불일치·검증 조건의 단일 기준.

| 문서 | 내용 |
|---|---|
| [프로젝트 개요](current/overview.md) | 구조·API·저장소·출처 |
| [지원 범위](current/support.md) | 베이스·언어·자동 정책과 제한 |
| [아이템 상태](current/item-state.md) | 옵션·희귀도·후보 검증 |
| [텍스트 파싱과 매핑](current/text-parsing.md) | 표시용 파서·계산 상태·참조 라이선스 |
| [Workbench](current/workbench.md) | 단일 화폐 적용·품질·미지원 기능 |
| [자동 탐색](current/automatic-planning.md) | 6옵션·확률·계산 한계·저장 호환 |
| [목표 조건](current/goals.md) | 그룹 연산·복합 조건·최소 필요 옵션 |
| [그래프 UI](current/graph-ui.md) | 후속 강조·선택·패닝·표시 자산 |

## 유지보수와 근거

- [Catalog 수정·봉인·규칙 identity](catalog-data-maintenance.md), [파싱 OpenAPI](openapi-item.yaml).
- [187개 재료 승인 정책](crafting-currency-policy-approved-20261015.md), [품질 보존 정책](workbench-quality-preserve-2026-10-04.md).
- [미해결 이슈](../ISSUES.md), [감정·숨겨진 상태의 미해결 근거](workbench-wisdom-evidence-2026-10-03.md).
- [6옵션 검증과 남은 실패](automatic-six-affix-2026-10-28.md). 기록된 결과는 해당 소스·시점의 결과다.
- [원자료](evidence/), [보존된 검증 자료](verification/), [재현 프로브](benchmarks/), [라이선스](third-party/PathOfBuilding-LICENSE.txt).
- [기존 QA 백업·복원 안내](evidence-retention.md), [백업 자료 목록](evidence-archive-manifest.json).

## 사용 중인 별도 API·저장 계약

| 영역 | 문서 |
|---|---|
| 기본 화폐 전이·명시적 정책 | [단일 전이](basic-currency-transitions.md), [first-hit·복구](basic-currency-first-hit.md), [실험 UI](basic-currency-first-hit-ui.md) |
| 기존 family Support·목표 AST | [Support 전이](support-transition-design.md), [목표 필터 v1](support-goal-filter-v1.md), [공유 fixture](../contracts/support-goal-filter-v1/README.md) |
| 경로 검색 v1 | [API 계약](crafting-path-search-v1.md), [공유 schema·fixture](../contracts/crafting-paths-v1/README.md) |
| 기존 method·certified 계산 | [bucket v2](crafting-bucket-method-v2.md), [화면 계약](crafting-certified-map-ui.md), [공유 v2 계약](../contracts/crafting-methods-v2/README.md) |
| 저장·조건부 확률·전략 | [로컬 저장](crafting-local-method-store.md), [lazy laws](crafting-lazy-laws.md), [전략 job](crafting-strategy-job.md) |

현행 자동 탐색과 별도로 유지하는 계약이다. 도입 시점의 측정·설명은 현재 전체 기능의 완료 선언으로 사용하지 않는다.

## 문서 정리 기준

완료 보고서·중단 기록·중복 안내·통합 전 사본과 일회성 문서 작성 스크립트는 작업트리에서 제거했다. 삭제 전 문서는 Git commit `48aff53`에서 조회·복원할 수 있다. 예: `git show 48aff53:docs/README.md`.

현재 실행·유지보수에 쓰는 계약, 미해결 규칙, 승인 정책, 원자료·프로브·라이선스를 유지한다. 과거 보고서 삭제는 이슈 종결이나 게임 확률 검증을 뜻하지 않는다.
