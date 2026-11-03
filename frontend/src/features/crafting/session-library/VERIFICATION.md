# 담당 C 검증 기록

기준 커밋: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
브랜치: `workbench/base-session-c`.
작업 폴더: `<local-user>\Documents\Codex\2026-11-02\task-5\base-session`.

## 구현/자체 리뷰

- 공통 화면/공통 registry/공통 번역/서버는 수정하지 않았다. 기존 파일은 소유권이 배정된 workbenchHistory.ts와 테스트만 변경했다.
- 베이스/세션 UI는 부모 상태 제어형이며 두 INTEGRATION.md에 호출 코드, 메뉴 anchor/shelf ref, 제작 중 작업 가드, 기폭제 설정 제거, 삭제 후 draft와 저장을 함께 갱신하는 순서를 명시했다.
- frames, 원문(sourceDocument), rulesetIdentity, craft evidence와 미지의 추가 필드는 metadata 변경/삭제 복구 과정에서 보존한다. version 1 storage key와 load 경계는 그대로이다.
- 테스트의 삭제/저장은 jsdom 합성 기록에서만 수행했다. 실제 브라우저/localStorage/사용자 세션은 열거나 지우지 않았다.

## 실행 결과

- `npm ci --prefer-offline --no-audit --no-fund`: 통과, 잠금 파일 변경 없음. 제한 실행 환경의 npm DNS가 실패하여 일반 실행 환경에서 동일 명령으로 설치했다.
- `npm run lint`: 통과.
- `npm run typecheck`: 통과.
- `npm run format:check`: 전체 프런트엔드 통과.
- `npm run build`: 통과. 기존 큰 JS 청크 경고만 발생했다. 새 컴포넌트의 공통 화면 연결은 통합 담당 소유이므로 이 브랜치의 배포 엔트리에 아직 들어가지 않는다.
- `git diff --check`: 통과.
- 담당 C 단위/컴포넌트/이미지 검증: 20개 통과. legacy 기록, 사용자 제목/별, 원문/프레임 보존, 명시적 단건 삭제, 인접 선택, 마지막 삭제, Undo와 중간 편집/새 세션 보존, 번호 유지, 카테고리·소분류, hover 정보, 레벨 검증, 포커스 복귀, 로컬 WEBP 해시를 확인했다.
- 기존 6개 회귀 suite의 재검증까지 포함한 79개 실행에서는 77개 통과, 2개 실패였다. 하나는 새 이미지 검증 테스트의 Vite import.meta URL 해석 문제로 테스트 파일 경로를 cwd 기준으로 바로잡았고, 하나는 수정하지 않은 stateProjection의 5초 제한이었다.
- 최종 `npm run test -- --run --maxWorkers=1 src/features/crafting/workbenchHistory.test.ts src/features/crafting/base-picker src/features/crafting/session-library src/features/crafting/method-search/stateProjection.test.tsx`: **5개 파일 / 35개 테스트 통과**. 위 두 실패 모두 해소됐다. timeout/기대값/프로덕션 그래프 코드는 변경하지 않았다.
- 최초 전체 `npm run test -- --run`: **131개 파일, 2,419개 통과 / 8개 실패**. 기존 비동기 UI 6개 suite에서 기본 병렬 실행 중 대기/5초 제한에 걸렸다. DesecrationCurrency, CraftingExplorer, WorkbenchExtension, goalFilter, MethodSearch는 worker 2개 재실행에서 통과했고, stateProjection은 위 최종 단일 worker 실행에서 통과했다. 전체 기본 병렬 실행을 전부 통과했다고 주장하지 않는다.
- 제한 실행 환경의 첫 관련 Vitest 시도는 worker 임시 변환 파일 ENOENT로 테스트 로딩 전에 실패했다. 후속 검증은 일반 실행 환경에서 수행했다.

## 이미지/잔여 연결

- 134개 기존/현재 베이스의 이미지 출처를 대응시켰고, 고유 WEBP 127개(약 1.43MB)를 전용 디렉터리에 캐시했다. WEBP magic과 SHA-256을 단위 테스트로 검증했다. Solar Amulet은 기존 자산을 재사용한다.
- 최초 구현 시 Fire Quiver(`Metadata/Items/Quivers/FourQuiver2`)의 `https://cdn.poe2db.tw/image/Art/2DItems/Quivers/Basetypes/QuiverFire.webp` 다운로드는 HTTP 403으로 거부되어 재시도/우회하지 않았다. 아래 사용자 제공 이미지 후속 반영으로 대체 표시를 해소했다.
- 브라우저 검증은 요청에 따라 수행하지 않았다. 백엔드/API는 변경하지 않았고 서버를 시작하거나 원격 push/공개 배포하지 않았다.
- 남은 공통 화면 연결은 두 INTEGRATION.md에 있다. 특히 삭제 후 기존 restoreFilm의 이전 history closure를 호출하여 삭제 세션이 되살아나는 문제를 피하도록 명시했다.

## 사용자 제공 불 화살통 이미지 후속 반영 — 2026-11-02

- 사용자 요청으로 Downloads의 `QuiverFire.jpg`를 확인하고, 제공된 그림과 같은 이미지임을 시각적으로 확인했다. 실제 파일 시그니처는 WEBP이므로 원본 바이트를 변경하지 않고 `base-picker/images/c8809e1438f9f231be94.webp`로 복사했다. Downloads 원본은 수정하지 않았다.
- 원본과 복사본 SHA-256: `c8809e1438f9f231be94c8d622f7f8c424298a25a9b27906b39bd9d50dfb97bf`, 19,006 bytes. 재인코딩·배경 제거 등 이미지 편집을 하지 않았다.
- Fire Quiver identity의 전용 manifest에 로컬 WEBP를 연결했다. 기존 WEBP 시그니처·전체 해시 검증을 유지했으며 모든 선택 가능 베이스가 로컬 이미지로 해석되도록 기존 placeholder 기대값을 변경했다. 최초 JPG 시그니처 검사 실패로 확장자와 실제 형식의 차이를 발견했고, 테스트를 완화하지 않고 저장 확장자를 바로잡았다.
- 이전 원격 HTTP 403 기록은 보존하며 해당 원격 다운로드는 재시도하지 않았다. 베이스 선택과 세션 카드는 공용 BaseImage를 통해 같은 로컬 이미지를 사용한다. 추가 사용자 이미지 대기는 해소됐다.
- 후속 검증: 담당 C의 4개 파일/20개 테스트 모두 통과, lint·타입 검사 포함 build·변경 파일 Prettier·git diff --check 통과. 원본과 로컬 복사본의 SHA-256 일치를 확인했다. 브라우저 검증과 서버 변경은 수행하지 않았다.
