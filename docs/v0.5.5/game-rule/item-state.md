# 아이템 상태와 저장 호환

텍스트 파서의 표시 결과는 검증된 제작 상태가 아니다. catalog 매핑 후 snapshot/base, 희귀도, 레벨, implicit/explicit, 수치·품질·분열·훼손·소켓 상태를 검증한다. 원문과 미해석 행은 보존한다.

- 알 수 없는 modifier, 잘못된 layer, 중복 ID, family 충돌, 잘못된 stat 집합과 범위 밖 값을 거절한다.
- implicit는 베이스 정의와 일치해야 한다. NORMAL은 명시 옵션 0개이며 MAGIC/RARE는 해당 베이스 용량을 따른다. 주얼과 증강물 확장을 포함해 모든 상태에 3+3 또는 6개를 고정 적용하지 않는다.
- 복합 modifier의 여러 줄은 하나의 옵션이다. 기존 상태 검증과 새 후보의 requiredItemLevel 검증은 별도다.
- 지원 밖 분열·희귀도·소켓 조합을 허용하지 않는다. 새 상태 필드는 Java 공통 읽기 계약과 단일·반복 제작에서 동일하게 처리한다.

[카탈로그](../game-data/catalog/top-bases.json), [베이스 정책](../game-data/catalog/base-policies.json), 각 pool의 catalog가 수치와 자격의 원본이다.

## 규칙 identity와 기록

[기존 ruleset](../game-data/crafting/ruleset-v1.json)은 실행 리소스의 정확한 bytes와 engine/ledger 의미 버전을 봉인한다. v0.5.5는 이 저장소의 데이터 묶음이며 확인되지 않은 실제 게임 시즌·패치 번호를 뜻하지 않는다.

과거 film·목표·snapshot을 새 규칙으로 자동 재라벨링하거나 삭제하지 않는다. 이어하기는 현재 자격 검증 후 새 체크포인트를 만들고 원본 연결을 보존한다. 과거 단계에서 분기하면 원본의 미래를 남긴다. 저장 실패·revision 충돌은 성공으로 처리하지 않는다.

새 매핑은 준비 단계다. 기존 저장 데이터, API의 ID, ruleset identity는 이번 문서 정리로 전환하지 않는다. 향후 전환 시 서버·브라우저의 매핑 버전과 구버전 기록 호환을 함께 검증한다.
