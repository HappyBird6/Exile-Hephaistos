# 공통 훼손 전이 검증 — 2026-10-30

## 요구와 구현

사용자의 ‘일반 아이템 상태’는 Normal 희귀도가 아니라 기존 공통 상태 노드를 의미한다. 이전 v15의 `NodeView.abyss` 후처리 부착은 이 요구를 충족하지 못했다. v16은 `ItemState.desecrated`를 상태 정체성과 슬롯 계산에 포함하고, `choices → Method → ready → Node`의 같은 실행 경로에서 DESECRATE 작업을 생성한다. 신규 탐색의 별도 abyss 결과는 비어 있다. 기존 저장 결과는 역사적 버전으로 읽는다.

훼손 작업은 적용 가능한 Rare 상태에서 접두/접미 슬롯을 확정적으로 추가한다. 이 전이의 100%는 슬롯 생성만 의미하며 드러내기 후보의 확률이 아니다. 후속 엑잘/허용된 제거·교체 작업도 같은 상태 모델을 사용한다. 분열은 제거 대상에서 제외하고 슬롯과 배타 그룹을 유지한다. 미공개 슬롯은 제거 대상이며 확정 후보 선택 후 해당 modifier의 배타 그룹을 적용한다.

관측 UI는 실제 후보 세 개 입력과 선택에만 사용한다. 사용자 훼손 클릭 없이 자동 탐색에 훼손 결과와 기존 화폐 후속 작업이 생성된다. 후보 선택 후 같은 일반 탐색으로 이어간다. 훼손 상태를 단순 선택하는 것만으로 수동 이력을 만들지 않는다.

`p%(~N%)`는 후속 속성 선택의 기준 확률과 최고 가중치 비목표 속성 차단 조건부 비교다. 후보 추첨 확률이나 전체 목표 성공률/기대 비용이 아니다. 미확정 관측이 남으면 전체 완료/도달 불가로 단정하지 않는다.

## 검증 결과

- Frontend 전체 124 files / 2,392 tests 통과 (`--maxWorkers=2`). lint, typecheck, format:check, build 통과. 병렬 부하에서 발생한 두 타이밍 실패는 assertion/timeout 변경 없이 전체 재실행으로 통과했다.
- Backend 관련 12 classes / 92 tests 통과, failures/errors/skips 0. 마지막 두 클래스 재검사는 3m48s, bootJar 성공. 최종 Spotless 적용/검사 성공.
- Docker 격리 integrationTest 8개 및 generateJooq 통과.
- 전체 Backend check/test 실행은 오래된 중간 코드와 Windows bind IO 부담으로 중단했다. 현재 버전 전체 Backend suite 통과를 주장하지 않는다. 과거 baseline 실패 29개를 이번 전체 결과로 대체하지 않는다.
- CommonDesecrationGraphTest: 동일 choices의 엑잘/훼손, 공통 큐 후속 작업, 확률 질량, 분열 보존, 6슬롯 관측/선택 및 저장 복원. 소멸 제거 검증은 명시적인 격리 테스트 정책을 사용하며 운영의 소멸 제외 정책은 유지한다.
- 격리 API 19000: 일반 및 분열 입력 각각 root 훼손 Method 2개, 공통 훼손 상태 4개, 엑잘 후속 Method 2개. 저장 GET 복원 동일. 별도 abyss 부착 없음. 미확정 모델은 PARTIAL.
- 격리 실제 UI 19001: Solar Amulet level82, 분열 생명력117 + 마나180, 목표 생명력1/마나1/힘5. 같은 시작 노드에서 엑잘+우측 징조와 훼손 P/S가 연결되고 미공개 결과에서 엑잘이 이어짐을 확인. 실제 후보 정신력47~50/방어도27~32/에너지보호막52~61 입력 후 정신력47 선택, 후속 일반 탐색에서 분열과 훼손 표시 보존 확인. 최신 UI에서 공통 상태 시나리오 코드 복사 성공.
- 배포 후보 JAR와 실제 격리 QA JAR의 모든 BOOT-INF/classes 파일 바이트 동일. Frontend bundle `index-D_QsvWbE.js`.

## 명시적 제한

- 미지의 드러내기 추첨 법칙을 통과하는 전체 최적화/확정 기대 비용은 제공하지 않는다. 관측 전에는 조건부 상태로 남는다.
- 빈 Rare의 광범위한 strength-percent 시나리오는 공통 훼손 분기로 상태 수가 늘어 기존 128 states/256 methods 상한에서 부분 결과다. 목표 노드가 발견되어도 탐색 완료로 표시하지 않는다. 상한이나 테스트 허용 시간을 늘려 숨기지 않았다.
- 기존 Workbench 구체 편집 API는 비어 있지 않은 훼손 metadata를 거절한다. 자동 탐색/관측 흐름에서 보존하며 손실을 초래하는 편집 전송은 비활성화한다.
- 시나리오 공유는 현재 자동 탐색 segment를 포함한다. 관측 수동 이력 전체를 원격 공유하는 기능은 추가하지 않았다.
- 운영 쓰기 QA는 이전 자동 승인 거절에 따라 재시도하지 않는다. 운영 검증은 GET health/catalog/UI와 파일 해시로 한정한다.

## 규칙 문서

`docs/current/user-crafting-rules.md`와 기존 Library `libfile_bfc527a44a54819198cb7e30dbeb270b` v10을 갱신했다. 공식 helper 업로드 및 다시 materialize한 파일을 원본과 바이트 비교했다. 53,267 bytes, SHA-256 `1bc0d0221d3f909e3bc93ac78c4699e673649356ad4b4ea7325ad15f01aa4cf6`.

## 배포

현재 검증된 변경을 로컬 commit 후, 운영 앱을 정지한 일관된 시점에 app-data archive/추출 해시 검증과 PostgreSQL dump/격리 복원 검증을 수행한다. 기존 컨테이너와 볼륨을 유지한 채 JAR/frontend를 교체하고 health/정적 파일 해시/저장 데이터 전후 해시를 검증한다. 배포 완료 기록은 아래에 추가한다.

### 배포 완료

- 구현 commit `3d7f3f6b93ec8027dbace656e209547824dee3b6`을 기존 http://localhost:9000 / http://localhost:9001 에 반영했다.
- health/readiness/liveness UP, 프록시 catalog GET 및 실제 브라우저 초기 UI 로딩 확인. 운영 POST 검증은 수행하지 않았다.
- JAR SHA-256 `c823441a6588801a66921a58244dcdb378d0fc49ed5ef1011fdef3afd761f33c`, frontend 227개 파일 HTTP 바이트 해시 일치.
- app-data 43 files / 252,480,537 bytes: archive 추출 복원과 배포 전후 전체 해시 동일. PostgreSQL dump는 격리 QA DB로 복원 성공.
- 백업: `C:\Users\SSAFY\Documents\Codex\2026-10-29\task\common-predeploy-backup-1030`. rollback JAR/HTML, 데이터 archive/manifest, DB dump 보존.
- 기존 컨테이너 ID/image/mount/port, .env 동일. 운영 app만 재시작했다. 다른 프로젝트 Redis 두 개는 작업 중 사용자가 시작한 현재 실행 상태와 StartedAt을 보존했다. STS는 조작하지 않았다.
