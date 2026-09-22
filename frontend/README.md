# Frontend

Node.js 24.18.1 / npm 11.16.0 기준입니다. 의존성 버전은 `package.json`과 `package-lock.json`에 고정합니다.

```sh
npm ci
npm run dev
```

개발 페이지는 http://localhost:5173 이며 `/api` 요청은 `http://localhost:8080`으로 전달됩니다.
`/`는 제작 기능 준비 화면, `/admin` 또는 `/admin/crawling`은 크롤링 관리자 화면입니다.
일반 사용자 로그인·제작 계산·AI·가격은 구현 전입니다. 관리자 계정과 실행 환경 설정은
[관리자 크롤링 운영 안내](../docs/admin-crawling.md)를 참고하세요.

## 크롤링 관리자

- 서버 환경변수로 설정한 관리자 계정 1개를 사용합니다. 계정 미설정이면 관리 기능이 비활성화됩니다.
- 로그인은 서버 session과 CSRF를 사용하며, 비밀번호·token·대상 목록을 localStorage에 저장하지 않습니다.
- 크롤링 대상의 이름·poe2db 페이지 URL·활성 상태를 편집하고 DB에 저장합니다.
  미저장 입력이 있으면 수집을 시작하지 못하며, 다른 관리 화면의 변경과 충돌하면 입력을 보존합니다.
- 크롤링 버튼은 저장한 활성 대상에 대해 서버의 Python 원본 수집 배치를 요청합니다.
  화면을 열거나 설정을 저장하는 것만으로 수집하지 않습니다.
- 최근 이력은 3초 간격으로 조회하며 대기·실행·원본 수집 완료·실패를 표시합니다.
  원본 HTML을 브라우저에서 렌더링하지 않습니다.
- 데이터 정제·변경 비교·최신화는 후속 단계입니다. 현재 수집 성공은 게임 데이터 검증이나 발행 성공이 아닙니다.

관리 UI/API 계약은 [OpenAPI](../docs/openapi-admin.yaml)에 있습니다.
`src/features/admin-crawling`이 UI·입력 draft·응답 검증을 소유합니다.
API 미응답/미설정 상태를 실제 상태로 표시하며 mock 값을 운영 데이터로 표시하지 않습니다.

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
