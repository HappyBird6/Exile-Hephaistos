# 목표 설정·그래프 표시 검증 및 서버 적용 — 2026-10-29

기준: `ba219aed155b54649ea075fa52391a0abe49911f`, branch `codex/chaos-first-affix`. 작업 시작 시 clean. Backend 계산 및 운영 JAR 변경 없음.

## 변경

1. 목표 편집기와 새 탐색 요청을 RARE로 정규화. 시작 아이템 희귀도는 보존. 과거 시나리오 요청·결과는 원본 그대로 읽고 재편집 시 새 목표 규칙 적용.
2. 목표 희귀도 선택 및 현재 아이템 수치 판정 버튼/전용 UI 요청 제거. 내부 numeric API, 자동 탐색의 판정은 유지.
3. 동일 속성/티어의 연속 정수 수치를 후보 범위로 표시. 정확한 유리수 합산은 같은 방법·시작 상태·결과 종류 안에서만 수행. 원본 상태별 선택은 접힌 조건 목록에 보존.
4. catalog에서 목표 원본 stat 및 pseudo 기여 stat을 확인하여 비목표 속성만 잡옵으로 묶음. 접두/접미·개수·분열 여부와 목표 속성 구별. 원본 상태·간선·배타 관계·후속 조건은 불변. 새 시나리오에는 표시 분류 메타데이터를 저장하고 엄격히 검증.

## 검사

- `npm ci`, lint, typecheck, format:check, production build 통과.
- 전체 Frontend: 121개 파일, 2,369개 테스트 통과.
- 마지막 시나리오 호환성 보완 후 method-search 26개 파일, 188개 테스트 통과(신규 왕복 저장 검사 포함), lint/typecheck/format/build 재통과. 이 마지막 보완 후 전체 suite를 다시 실행한 것은 아님.
- 신규 회귀: 기존 목표 RARE 정규화 및 시작 상태 보존, 정신력 T1 47–50 후보 집계, 49 목표 경계의 성공/중간 상태 각각 1/2 유지, tier law 확률 1/4 네 개 합산, 잡옵 접두/접미·개수 구분, 원본/간선/조건 확률 불변, pseudo 및 미확인 stat 보수 처리, 시나리오 메타데이터 왕복/잘못된 값 거부.
- Backend 소스 변경이 없어 이번에는 Gradle 전체 검사를 재실행하지 않음. 이전 심연 작업의 알려진 baseline 실패는 `docs/abyss-crafting-verification-20261029.md` 참조.
- 실제 격리 브라우저: NORMAL 태양의 목걸이 → 진화·제왕, 원본 32개 결과를 접두 잡옵 11개와 접미 잡옵 21개의 두 표시 묶음으로 보존. 후보는 각 1개, 정신력 T1 +47–50. 최소 49일 때 +49–50 목표 결과 유지. UI 오류 없음.
- 운영 브라우저: 시작 희귀도 일반/마법/희귀 유지, 목표 희귀도 선택 0개, 수치 판정 버튼 0개, 편집기 정상 및 console error 없음.

## 적용과 데이터 보존

- Frontend `http://127.0.0.1:9001`, Backend `http://127.0.0.1:9000`.
- 검증된 dist 자산을 먼저 복사하고 entry 문서를 반영. 현재 JS `index-CgKSuuAY.js`; HTTP로 227개 파일 SHA-256 일치 확인. 기존 자산은 유지하여 열린 탭의 요청도 보존.
- Backend health/readiness/liveness UP. 심연 stateless API 7개 검사 통과: 미공개 여섯째 슬롯, 확률 없는 후보 세 개, 다른 다섯 속성 보존, 6슬롯 재작업, 조건부 가중치 비교, 더미 제거 위험, 에센스 실행.
- 배포 직전 앱 저장 29개 파일 205,710,473 bytes의 tar 백업을 별도 임시 경로에 풀어 해시 검증. 배포 전후 모든 경로/크기/SHA-256 동일.
- 현재 PostgreSQL 논리 백업을 생성하고 격리 DB에 `pg_restore --exit-on-error --no-owner` 복원 성공.
- 백업 위치: `C:\Users\SSAFY\Documents\Codex\2026-10-29\task\graph-ui-predeploy-backup` (app-data.tar/json, postgres.dump, previous-html, runtime-before.json).
- 운영 앱/프런트/PostgreSQL/Redis 및 타 프로젝트 Redis 2개의 container ID, image, 시작 시각, mount, port 동일. `.env` 및 Backend JAR 해시 동일. 승인된 Redis 내부 연결/외부 포트 제거 override 유지. STS 변경 없음.
- Runtime 파일 반영 방식은 이전 배포와 동일. 컨테이너를 재생성할 경우 검증된 dist를 다시 배포해야 함. 원격 push/master 통합/worktree 삭제 없음.

## 한계

- 기존 시나리오에 잡옵 분류 근거가 없으면 원래 속성 표시를 유지한다. 새로 계산·저장한 시나리오는 분류를 보존한다.
- 연속 정수 단일 수치만 범위로 합친다. 복합 수치/불연속 지지집합은 임의의 연속 범위로 바꾸지 않는다.
- npm ci는 기존 잠금 파일의 취약점 6개(중간 2/높음 4)를 보고했다. 의존성을 임의 갱신하지 않았다. Vite의 기존 큰 bundle 경고 유지.

- 임시 QA 컨테이너 4개와 전용 network 정리 완료. 운영 volume 연결 없이 tmpfs와 읽기 전용 bind만 사용했음을 확인했다.
- Library 기존 문서 identity 유지, v6 갱신 완료. 40,980 bytes 및 원본 SHA-256 421e1d7988b724a2931ad83fc495c23546085ed659d81d19190ccd3a2356d34a 일치. Windows 로컬 xattr 적용은 os.setxattr 미지원으로 실패했으나, 공식 helper의 임시 Linux materialize 및 metadata 검증과 byte 비교는 성공했다. 접근 거절은 없었다.
