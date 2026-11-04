# Workbench Chaos 반복 연결

CraftingPage는 기존 중앙 아이템 사용 버튼에서만 반복을 시작한다. ON은 모드
편집이며 실행하지 않는다. `CHAOS`, `GREATER_CHAOS`, `PERFECT_CHAOS`만 정식
`applyChaosRepeat` adapter로 요청하고 다른 화폐의 단일 적용 경로는 유지한다.

- 기본 최대 횟수는 100, 입력 상한은 1000이다. 잘못된 입력은 원문을 유지하고
  요청을 보내지 않는다. 패턴을 Page에서 trim하거나 JavaScript regex로 실행하지
  않는다. 기본 regex의 옵션별 find, case insensitive, 복합 옵션 묶음과 최소
  첫 1회 적용은 서버 계약이다.
- 요청 당시 requestId·before·base revision/source·Film/cursor·locale·징조·catalog를
  고정한다. 응답은 현재 ref뿐 아니라 Query cache·Zustand·locale의 현재 값과도
  비교하므로 React 알림 전의 변경이나 unmount 뒤 결과를 반영하지 않는다.
- 검증된 completed/partial receipt만 `recordChaosRepeat`로 최종 Frame 하나를
  기록한다. CONCRETE와 ABYSS를 구분하며 확인된 정확한 N·징조 소모·남은 징조를
  반영한다. 중간 Frame, 누락 witness의 event 또는 추정 횟수는 만들지 않는다.
- MATCH/LIMIT는 정상 상태 안내, 양의 INAPPLICABLE/ERROR는 최종 상태를 반영한
  중단 안내다. 0회·요청 거부·네트워크 불확실·손상 응답은 기존 기보와 징조를
  보존하며 각각 다른 안내를 쓴다. 자동 재시도와 단일 적용 fallback은 없다.
- 실제 요청 중에는 Controls spinner를 표시한다. 취소 UI는 없다. 화폐·징조·
  Base/Edit·언어·세션·기보 이동·Abyss 변경을 disabled와 동기 owner guard로 막는다.
  Abyss의 자체 변경도 동기 busy 통지로 Page 요청을 막는다. 출처 링크와 실제
  held/selected 재료 설명의 우선순위, 독립 Base 미리보기는 유지한다.
- Controls는 기존 canvas의 가운데 grid 행에 배치한다. 기존 창고 좌표·containment·
  내부 스크롤·75% 배율·200px 피드백 영역의 CSS는 변경하지 않는다.

통합 검사는 jsdom에서 실제 adapter·전용 validator·receipt·history writer를
연결하고 HTTP 응답만 검토된 합성 fixture로 제공한다. 실제 backend HTTP 실행,
브라우저 QA, 서버·Docker·DB·Redis와 사용자 기록 조작은 이 검사에 포함되지 않는다.
