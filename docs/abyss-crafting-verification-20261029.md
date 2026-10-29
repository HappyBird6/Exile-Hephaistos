# 심연 제작 구현 및 검증 — 2026-10-29

기준: `codex/chaos-first-affix`, HEAD `241302d006e67a813153e17c6cf348cffbe3fc86`. 기존 미커밋 변경 없이 시작했다. 원격 push, master 통합, worktree 삭제는 수행하지 않았다.

## 구현

- 미공개 훼손도 실제 접두/접미 슬롯이며 제거 대상이다. 가짜 catalog modifier ID를 만들지 않는다.
- 드러내기는 화폐가 아닌 관측 후보 3개 입력 및 사용자 선택이다. 후보 확률·재작업 징조 비용·이전 후보 복귀 여부는 계산하지 않는다.
- 6옵션에서도 같은 훼손 슬롯을 재작업하고 나머지 슬롯을 보존한다. 미공개/선택 완료 훼손 속성을 녹색으로 구분한다.
- 다음 Exalted 후보 풀에서 실제 레벨·슬롯·충돌군·weight로 기본 확률과 최고 가중치 비목표 차단 조건부 확률을 비교한다. modifier 선택 확률이며 목표 수치 전체나 드러내기 확률이 아니다.
- 자동 탐색의 현재 구체적 노드에 한쪽 채우기·고가중치 차단·제거 위험 분산을 연결했다. 기존 장비별 화폐/징조 정책을 지킨다. 분포 노드를 구체적 아이템으로 추정하지 않으며 수동 선택 경로를 자동 성공 확률에 합산하지 않는다.
- 선택한 경로의 미공개 슬롯·다음 화폐·징조를 수동 패널로 전달한다. 서로 다른 징조 경로의 캐시와 저장 키를 분리한다. 기존 저장 job/film은 변환하지 않는다.

## 실행 검증

- Frontend: npm ci, lint, typecheck, format:check, build 통과. 전체 120개 파일·2,366개 테스트 통과. 이후 카드 설명/경로 캐시 분리 수정은 관련 37개 테스트와 lint/typecheck/build 재검증 통과. 기존 번들 크기 경고는 유지된다.
- Backend: Java 21 격리 Linux 복사본에서 `--continue check generateJooq bootJar` 완주. 단위 944개 중 29개 실패, 오류 0. 통합 8개 통과. Java 포맷, ArchUnit, jOOQ 생성과 JAR 빌드 통과. 전체 check 성공으로 표시하지 않는다.
- 전체 검사 실패 29개는 모두 기준 HEAD의 해당 테스트에서 같은 실패를 재현했다. 테스트 skip, 기대값 완화, 계산 예산 변경은 하지 않았다. GoalCatalogParityTest 1, CertifiedJobRestartTest 3, CertifiedMethodProductionTest 1, CertifiedSearchJobsTest 5, IndexedMethodSearchServiceTest 2, MethodSearchLifecycleTest 4, MethodSearchServiceTest 1, MethodTransitionTest 4, PathSearchHttpTest 1, PathSearchLifecycleTest 2, PathSearchServiceTest 2, AutomaticPreparationRulesTest 1, AutomaticRefinementsTest 1, AutomaticTierRenewalTest 1이다.
- 격리 HTTP 7건: 미공개 6번째 슬롯, 3후보/확률 없음, 선택/재작업 시 기존 5개 보존, 조건부 가중치, 더미 제거 1/2, 에센스 실행 통과.
- 실제 브라우저: 생명력 19·마나 14 접두와 힘 5 목표로 세 전략 표시. 최고 가중치 차단 5.45%(~5.60%), 제거 시 유효 옵션 보존 0%→33.33%. 한쪽 채우기에서 기존 두 접두·녹색 미공개 접두 유지 및 Exalted의 접미 추가 확인. 콘솔 오류 없음. 최종 정책 필터 후 확인은 아래 후속 기록을 따른다.

## Library와 운영 경계

- 규칙은 동일 Library ID `libfile_bfc527a44a54819198cb7e30dbeb270b`의 v5로 저장했다. 크기 39,341 bytes, SHA-256 `9f19a479dea7b215cbc28543472f5cd321b0b2ef14ff2b9467af802dcc5baefc`. 공식 전송 도우미로 Linux에서 내려받아 로컬 원본과 cmp 및 SHA-256 일치를 확인했다.
- Windows 원본의 버전 xattr 기록은 공식 도우미의 `os.setxattr` 미지원으로 실패했다. 우회하지 않았으며 Library 저장과 원본 바이트 검증은 성공했다.
- 운영 app-data의 읽기 전용 백업을 복원하여 25개 파일, 185,887,212 bytes 전체 해시를 검증했다. 실제 재배포 직전에는 최신 백업을 다시 받아야 한다.
- 기존 Exile 서비스는 작업 전부터 중지되어 있었다. 앱 내부 Redis 주소는 `redis:6379`다. Exile Redis의 호스트 127.0.0.1:6379 공개가 다른 프로젝트와 충돌한다. 해당 호스트 공개만 제거하고 기존 /data 볼륨을 그대로 연결하는 별도 Compose 설정은 config --quiet 검사 통과했다. 실제 변경은 사용자 승인 대기이며 수행하지 않았다.
- STS 8080, 다른 프로젝트 Redis, 운영 DB·볼륨·환경 파일·사용자 저장 자료는 변경하지 않았다. 격리 QA는 19000/19001과 임시 PostgreSQL/Redis를 사용했다. 운영 9000/9001 적용 완료로 표시하지 않는다.

## 최종 정책 보완 검증

자체 검토에서 자동 후보 목록의 장비별 조건을 추천 단계에서도 명시적으로 검사하도록 보완했다. 목걸이에 자동 금지된 발작 에센스와 룬합금·카오스가 심연 추천에 나오지 않는 검사를 추가했다. 세 전략이 모두 남는 것도 확인했다. 최종 Backend 관련 82개 테스트, Java 포맷, JAR 빌드가 실패·오류 없이 통과했다. 이 보완 뒤 전체 944개 단위 검사를 다시 반복한 것은 아니다.

최종 실행 JAR SHA-256: `c9909934c76713726453df681c45dcd1404234f8662fa4cab0a7926872c71c78`. 이 JAR는 격리 QA에서만 실행했으며 운영 배포가 완료된 것은 아니다.

최종 JAR의 실제 브라우저에서도 세 전략과 5.45%(~5.60%)를 확인했다. 목걸이 자동 추천에서 발작 에센스가 제외되고, 한쪽 채우기에서 마나 14·생명력 19·녹색 미공개 접두가 유지됐다. 콘솔 오류는 없었다.
