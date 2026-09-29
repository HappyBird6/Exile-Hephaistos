# Frontend

React·TypeScript·Vite 기반 영어 제작 작업대다. `/`는 아이템 입력·재료 선택 화면, `/admin`은 인증 없이 접근하는 최소 관리 페이지다. 현재 관리 도구는 없다.

TanStack Query는 아이템 분석 응답, Zustand는 편집 중 입력을 관리한다. 입력 취소와 늦은 응답 폐기를 유지한다. 번역·언어 선택 구조는 사용하지 않는다.

```sh
npm ci
npm run dev
npm run lint
npm run typecheck
npm run format:check
npm run test -- --run
npm run build
```

Vite는 `http://localhost:5173`에서 실행하고 `/api`를 `localhost:8080`으로 proxy한다. Docker에서는 nginx가 정적 파일과 API proxy를 제공한다.
UI·자산 출처는 [crafting-ui.md](../docs/crafting-ui.md), 아이템 분석 계약은 [item-text-parsing.md](../docs/item-text-parsing.md)를 참고한다.
`VITE_*`는 브라우저 공개 설정이므로 비밀값을 넣지 않는다.
