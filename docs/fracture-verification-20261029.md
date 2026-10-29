# 분열 시작 속성 검증 — 2026-10-29

기준 HEAD: 1d388a52cd9033bb464ecc402f5d6274e4b90049, codex/chaos-first-affix. 작업 시작 상태는 clean이었다.

## 변경

- 시작 속성의 수치와 삭제 사이에 6개 언어의 분열 ON/OFF 버튼을 추가했다. Rare 명시 속성 1개만 허용하며 다른 분열을 자동 해제하지 않는다. 분열이 켜진 상태에서 Magic/Normal 변경은 비활성화하고 기존 값을 유지한다.
- 카드·복사 텍스트·저장/복원·수치·티어 편집에 분열 표시를 보존했다. SolarTextMapper가 분열 마커를 거부하던 결함을 수정했다. 기존 Solar 단일 stat/정확한 catalog 매핑 제한은 유지한다.
- 서버가 정렬한 옵션 순서와 편집기 입력 순서가 다르면 동일 아이템의 시나리오 저장이 실패하던 결함을 수정했다. 순서만 정규화해 비교하며 분열·수치 변경은 여전히 거부한다.
- 계산 규칙을 추정하거나 자동 화폐 정책을 확대하지 않았다. 분열과 일반 잡옵은 구분하고 기존 슬롯·배타 그룹·제거 제외 동작을 검증했다.

## 검증 결과

- npm ci, lint, typecheck, format:check, build 통과. 최종 전체 프런트 121파일/2,376테스트 통과.
- 최초 병렬 실행은 기존 stateProjection UI 테스트가 5초 timeout으로 1개 실패했다. 기대값·timeout을 바꾸지 않고 maxWorkers=2로 전체 재실행해 통과했다.
- 관련 Backend 83테스트 통과. 새 AutomaticTierLawTest는 분열 초기 수치가 압축되지 않고 Exalted 및 기반 Chaos의 분포 압축 전후 전체 확률 법칙과 질량 1이 같음을 검증했다. 현재 자동 정책의 Chaos/Annulment 거부도 명시적으로 검사한다. Chaos 기반 계산 비교는 기존 HistoricalAutomaticPolicy 테스트 fixture에서만 수행하며 제품 정책을 변경하지 않는다.
- Java 21 격리 전체 실행: spotlessApply check generateJooq bootJar. Unit 947개 중 29개 실패, 통합 8개 통과. 실패 이름 집합은 이전 abyss-full-backend.log의 29개와 정확히 동일(추가/해소 없음). 전체 check 성공으로 보고하지 않는다. formatter/ArchUnit/jOOQ/JAR 빌드는 통과했다.
- 실제 QA API: Chaos/Annulment/Hysteria 15회 분열 보존, 미공개 더미와 일반 속성 2개의 제거 후보가 각 1/3이며 분열은 제외, 분열 텍스트 매핑 통과.
- 실제 QA 브라우저: 생명력 분열 ON 후 두 번째 마나 분열 비활성, T1→T2→117 수치 변경, 재로드 후 ON/117 유지, 분열 잡옵과 일반 잡옵 구분, Spirit≥47 자동 탐색 완료, 시나리오 코드 생성 활성 및 alert/console 오류 없음.
- 최종 프런트 빌드: index-DHWRG_eQ.js / index-CnkO38EM.css. npm audit 기존 6건 및 큰 bundle 경고는 별도 변경하지 않았다.

## 심연 기존 배포 조사와 최신 요구

현재 배포 1d388a5의 9001 UI에서 Rare Solar Amulet ilvl82, life19/mana14, 목표 life≥19/mana≥14/strength≥5를 직접 재현했다. 시작 노드를 선택하면 그래프 옆 '선택한 아이템' 카드 아래, '시작 아이템으로 사용' 위에 '심연 제작 경로'가 표시된다. 세 전략 모두 확인: 한쪽 채우기, 5.45%(~5.60%) 고가중치 차단, 제거 위험 보존 0→33.33%.

이 구현은 구체 Rare 상태에 붙은 별도 수동 패널이다. 분포 노드나 과거 snapshot에서 숨겨지며 자동 최적화의 확률 간선으로 통합된 것이 아니다. 사용자가 이를 일반 제작 흐름에 통합하라고 최신 지시했으므로 별도 통합 작업을 진행한다. 후보 추첨 확률·전체 기대비용은 추정하지 않는다.

## 배포

분열 변경의 배포와 보존 검증 결과는 적용 완료 후 기록한다. 심연 일반 흐름 통합은 이 분열 검증과 구분한다.


분열 단계 적용 완료: commit 959228d. 9000/9001 health/readiness/liveness UP, 프런트 HTTP 227파일과 빌드 원본의 SHA256 일치. app-data 35파일 222,654,969 bytes를 백업·복원 검증했으며 배포 전후 동일하다. PostgreSQL logical dump를 격리 QA DB에 복원 검증했다. 운영 컨테이너 6개의 ID·이미지·마운트·포트 및 .env를 보존했고 승인된 앱 재시작/JAR 교체만 수행했다. JAR와 프런트는 기존 컨테이너 런타임 파일에 적용했으므로 컨테이너 재생성 시 이 산출물을 다시 적용해야 한다.

운영 qa_fracture_api 쓰기 호출을 포함한 명령은 자동 승인 검토가 거절하여 실행되지 않았다. 재시도하지 않았고, 격리 QA의 쓰기 동작 검사와 운영의 읽기 전용 health/catalog/static hash/data 보존 검사를 구분했다.
