# Frontend

Node.js 24.18.1 / npm 11.16.0 기준입니다. 의존성 버전은 `package.json`과 `package-lock.json`에 고정합니다.

```sh
npm ci
npm run dev
```

개발 페이지는 http://localhost:5173 이며 `/api` 요청은 `http://localhost:8080`으로 전달됩니다. 현재는 시작 페이지만 제공하며 API 요청, 실제 제작 계산, AI, 가격, 로그인은 구현하지 않았습니다.

```sh
npm run lint
npm run typecheck
npm run test -- --run
npm run build
npm run format:check
```

`src/app`은 앱 조립과 provider, `src/features`는 기능별 UI와 입력 draft, `src/shared`는 공통 코드를 소유합니다. 기능은 구현될 때 해당 디렉터리를 추가합니다. TanStack Query provider가 서버 상태의 진입점이며 Zustand는 후속 입력 draft에 사용하도록 설치되어 있습니다. 서버 응답을 Zustand의 원본 상태로 복제하지 않습니다.

`VITE_*` 변수는 브라우저에 공개됩니다. 비밀정보를 넣지 않습니다. production 정적 호스팅에서는 별도 reverse proxy로 `/api`를 Backend에 연결해야 합니다. Vite 개발 proxy는 빌드 결과에 포함되지 않습니다.

호환성 확인 자료: [Vite 시작하기](https://vite.dev/guide/), [Vitest 시작하기](https://vitest.dev/guide/). 설치 시 npm peer dependency 검사와 아래 검증 명령으로 조합을 확인합니다.
