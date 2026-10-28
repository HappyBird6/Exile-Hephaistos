# 목표 필터 Catalog 409 복구 (2026-10-19)

## 원인과 수정

저장 목표의 catalogVersion이 현재 Catalog와 다르면 목표 검증 API가 CATALOG_VERSION_MISMATCH(409)를 반환했다. 편집기는 기존 목표를 복원했지만 호환성을 확인하여 버전을 갱신하는 경로가 없었다.

이제 현재 Catalog를 받은 뒤 옵션 ID·단위·지원 여부·그룹 연산을 확인하고, 목표 사본을 서버에서 검증한다. 검증이 성공한 경우에만 편집기의 catalogVersion을 갱신한다. 목표 옵션·수치·그룹 연산·범위·희귀도·비활성 행과 그 외 저장 데이터는 변경하지 않는다. 늦게 도착한 검증 응답은 그 사이 편집한 목표에 적용하지 않는다.

현재 버전의 사용 옵션 정의(kind/unit/sourceStatIds/contributions)는 별도 로컬 저장 키에 기록한다. 이후 변경 시 같은 ID라도 원본 stat 또는 합산 계수가 달라졌다면 차단한다. 이전 정의가 없는 기존 목표는 source와 layer를 명시하는 v1 직접 옵션 ID 및 단위가 일치하는 경우에 한해 서버 재검증한다. 이전 정의가 없는 PSEUDO 옵션은 자동으로 의미를 추측하지 않는다. 이 경우 또는 옵션 삭제·단위/정의 변경·미지원 연산이면 원본 목표를 유지하고 6개 언어로 항목과 경로를 안내한다.

Catalog 검증 충돌과 ruleset 변경에 한해 Catalog를 다시 읽는다. 자동 탐색 revision 충돌은 이 처리 대상이 아니다. 자동 반복 요청은 하지 않으며, 변경된 목표/새 Catalog에 대해 다시 검증한다. 사용자가 선택한 AND/OR 표현, 메모 및 저장 탐색을 삭제하는 코드는 추가하지 않았다.

## 검증

새 회귀 테스트 15개와 기존 목표 편집기 16개가 통과했다. 이전 버전, 현재 버전, 삭제된 옵션, 단위·정의·연산 변경, 비활성 행, PSEUDO 정의 변경, 실행 중 Catalog 변경, 재마운트, 서버 거절 및 반복 방지, 늦은 응답, revision409 구분, 항목별 UI 안내, 로컬 저장 복원을 포함한다.

실제 localhost Catalog의 Solar Amulet 생명력 목표(120~140, RARE, itemLevel 82~100)를 사용해 호환 검사를 수행하고 목표 사본을 실제 API에 제출했다. 200/valid:true이며 버전 외 목표가 동일함을 확인했다. 테스트는 합성 입력만 사용했고 사용자 저장 작업을 재개하거나 변경하지 않았다.

전체 프런트 검증 및 실행 반영 결과는 아래 완료 기록에 추가한다. Backend 소스·화폐 확률·탐색 엔진 변경은 없다.

## 한계와 사용자 행동

현재 연결된 사용자 브라우저가 없어 실제 사용자 화면/요청은 직접 검증하지 못했다. React/jsdom 검사와 실제 API 검증을 구분한다. 반영 후 localhost:8081을 새로고침하면 복원된 호환 목표가 자동 검증된다. 호환 불가 항목은 안내를 보고 사용자가 수정해야 하며 임의 삭제하거나 목표를 완화하지 않는다. 기존 직접 옵션의 의미는 v1 source/layer 식별 계약을 따른다. 계약을 어기고 동일 source ID 자체의 의미를 바꾸는 외부 변경은 기존 데이터만으로 판별할 수 없다.

## 최종 검증 기록

최종 소스 기준 npm ci, lint, typecheck, format:check, 전체 테스트 2,201개(실패 0, 보류 0), production build가 통과했다. 전체 검증 종료 코드는 0이다. Windows 직접 실행과 Debian 실행은 기존 native binding 환경이 맞지 않아 테스트 시작 전 실패했으며, 잠금 파일을 유지한 Alpine Node 24 환경에서 npm ci 후 최종 검증했다. 기존 dependency audit high 1건과 큰 번들 경고는 남아 있으며 이번 수정으로 의존성을 변경하지 않았다.

검증 증거: `E:/CODEX/codex/2026-10-19/task-2/catalog-recovery-tests.json`, `catalog-recovery-validation.log`. 자체 리뷰 완료. Backend 소스가 바뀌지 않아 Backend 전체 검사를 반복하지 않았고 실제 API 연결은 별도 검증했다.

## localhost 반영 확인

프런트 실행 소스는 `568aa74fcd7a79a4f383c2e3bacbb8f19ed5c52a`, 이미지는 `exile-hephaistos-frontend:catalog-recovery-568aa74`이다. 검증 완료된 dist로 이미지를 만들고 frontend만 교체했다. Backend 실행 소스 `0afab1a`와 app/PostgreSQL/Redis 컨테이너 ID는 그대로다. 원격 push/merge/deploy는 하지 않았다.

반영 후 `/` 200, backend readiness UP, 실제 목표 필터 검증 200/valid:true를 확인했다. 제공 bundle `index-Byi3n3vI.js`의 SHA256 `da00481aaaea821a1ceacbcdc4c5ec1a6677f0b8847ccfa1ba2dadd29b821e29`는 검증 빌드와 일치한다. `.env` 해시와 기존 저장 작업 12개 파일 해시 모두 동일하다. 실행 검증 증거는 `E:/CODEX/codex/2026-10-19/task-2/catalog-recovery-runtime-result.json`이다.

사용자는 localhost:8081을 새로고침하면 된다. 이전 bundle이 유지되면 Ctrl+F5로 갱신한다. localStorage를 지울 필요는 없다. 실제 브라우저 화면은 연결 수단이 없어 미검증이며, 합성 API와 React/jsdom 검증으로 확인한 범위를 보고한다.
