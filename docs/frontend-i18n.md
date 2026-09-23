# Frontend 언어와 아이템 텍스트 입력

2026-09-23: 사용자 요청에 따라 UI는 영어·한국어를 제공한다. 새 dependency 없이 React context와 타입 검사되는 locale resource를 사용한다.

## 언어 확장

- `frontend/src/shared/i18n/locales/ko.ts`가 메시지 key 계약이며 `en.ts`는 같은 key와 화폐 label을 모두 제공해야 TypeScript 검사를 통과한다.
- 새 locale 파일에 같은 메시지·화폐 key를 구현하고 `messages.ts`의 `locales`에 언어 코드, 자국어 label, resource를 등록한다. selector와 Locale 타입은 registry에서 확장된다. 다른 컴포넌트에 언어 분기를 추가하지 않는다.
- `{name}`, `{count}` 등 placeholder로 문장을 구성한다. 동적인 알림·검증 오류는 번역된 문자열 대신 key를 보관하여 언어 전환 즉시 다시 번역한다. 새 locale을 추가하면 placeholder 동등성 테스트도 해당 locale을 포함한다.
- 초기 언어는 저장한 지원 언어 → 브라우저의 지원 언어 → 영어 순서다. `exile-hephaistos.locale`에 언어 코드만 저장하며 저장소 접근 실패 시에도 화면은 동작한다. `document.documentElement.lang`을 갱신한다.
- 입력 아이템의 이름·속성·요구사항·상태·미해석 행과 관리자가 입력한 대상 이름은 원문 데이터다. UI 언어 전환 시 번역하거나 변경하지 않는다. 브랜드 장식의 영문 표기는 유지한다.

## 아이템 정보 표시

제작 작업대의 **아이템 텍스트 → 아이템 분석**은 `POST /api/v1/items/parse`를 호출한다. 영어·한국어 텍스트는 backend가 감지하며 UI 언어와 무관하다. UTF-8 16 KiB 초과와 빈 입력은 호출 전에 거부하고, 서버 검증은 독립적으로 유지한다.

이름·표시 베이스·종류·희귀도·레벨과 원문 속성·요구사항·표식 옵션·상태를 보여준다. 알 수 없는 값은 확인 불가로 표시하고 미해석 행, 경고 코드, 전체 원문을 보존한다. 결과는 catalog 검증 전 초안이며 확률 계산용 ItemState로 취급하지 않는다. 텍스트는 React escaping으로 표시한다.

Zustand는 입력만, TanStack Query mutation은 서버 결과를 소유한다. 입력 수정·베이스 전환·새 분석·unmount 시 진행 중 요청을 취소한다. 요청 revision을 대조하고 응답 원문이 제출한 원문과 일치하는지 검증하므로 이전 결과가 최신 입력을 덮어쓰지 않는다. 오류 시 입력은 유지하며 재시도할 수 있다. 결과나 아이템 원문을 localStorage에 저장하지 않는다.

`frontend/src/features/crafting/itemTextApi.ts`는 `docs/openapi-item.yaml`의 공통 TypeScript DTO와 runtime validation을 함께 관리한다. 이번 변경은 기존 admin adapter와 같은 수동 대응 방식이며 API 변경 시 두 계약을 함께 검토해야 한다.

TODO(frontend): 기술 명세의 OpenAPI DTO 자동 생성 전환 / 저장소 공통 generator 선정 / item·admin DTO 동기화 / 현재 수동 공통 DTO와 runtime validation, 계약 리뷰로 대응.

## 검증

`frontend/`에서 `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run test -- --run`, `npm run build`를 실행한다. 테스트는 synthetic API 응답으로 입력 제한·정보 표시·원문 보존·오류/재시도·늦은 응답·취소·영한 전환·저장소 장애와 기존 관리자 흐름을 검증한다. 실제 게임 규칙이나 catalog 정확성 검증을 의미하지 않는다.
