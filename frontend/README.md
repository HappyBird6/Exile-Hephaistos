# Frontend

React·TypeScript strict·Vite 기반 제작 작업대다. `/`는 Workbench·최대 6옵션 Craft Support, `/admin`은 관리 도구가 없는 최소 페이지다. 표시 언어는 English, 한국어, 简体中文, 繁體中文, 日本語, Español이며 기본 언어는 한국어다. 영어 아이템 복사 텍스트 파서는 UI 번역과 별개다.

## 구조

| 위치                                  | 책임                                          |
| ------------------------------------- | --------------------------------------------- |
| `src/app`                             | 앱 진입점·Provider·페이지 분기                |
| `src/features/crafting`               | 수동 Workbench, 아이템·재료·film, Support     |
| `src/features/crafting/goal-filter`   | 시작 아이템·목표 입력, catalog 복구·수치 판정 |
| `src/features/crafting/method-search` | 자동 계산·그래프·제어, 시나리오 저장·복원     |
| `src/shared/i18n`                     | 6개 언어 표시·언어 선택 저장                  |

TanStack Query는 서버 응답을, Zustand는 편집 입력을 관리한다. 취소와 요청 identity/revision으로 늦은 응답을 폐기하고 원문·기존 결과를 보존한다. Workbench film·Support 입력·시나리오의 저장 및 버전 경계를 임의로 합치지 않는다.

## 실행과 검사

Node/npm 버전은 [package.json](package.json)의 engines와 packageManager를 따른다.

```sh
npm ci
npm run dev
npm run lint
npm run typecheck
npm run format:check
npm run test -- --run
npm run build
```

Vite는 `http://localhost:5173`에서 실행하고 `/api`를 `localhost:8080`으로 proxy한다. Docker에서는 nginx가 정적 파일과 API proxy를 제공한다. `VITE_*`는 브라우저 공개 설정이므로 비밀값을 넣지 않는다.

[문서 안내](../docs/README.md), [프로젝트 개요](../docs/current/overview.md), [지원 범위](../docs/current/support.md)에서 현재 계약을 확인한다. [그래프 UI](../docs/current/graph-ui.md), [파서·매핑](../docs/current/text-parsing.md), [자동 계산·저장](../docs/current/automatic-planning.md)에는 세부 근거가 있다. 공유 API fixture와 contract는 저장소의 `contracts/`를 따른다.

기능별 공통 화면 연결과 상태·기록 보존 계약은 [기능 연결과 유지보수](../docs/development.md)에 모았다. 모듈 안의 과거 INTEGRATION/VERIFICATION 안내를 다시 적용하지 않는다.
