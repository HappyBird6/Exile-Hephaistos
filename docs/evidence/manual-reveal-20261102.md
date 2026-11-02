# WB-052 직접 선택 및 잔여 훼손 화폐

기준: `b00932bcab27ed5455452a48d5912b7f927cfc3d`. 사용자 최신 결정(2026-11-02): 드러내기는 적용 가능한 옵션 중 직접 선택한다. 후보 3개 관측 입력 의무를 대체하며 게임에서 확정 획득한다는 뜻은 아니다. 이전 체크포인트 문서의 미완료 표보다 이 문서의 상태가 우선한다.

## 구현

- 첫 화폐 탭: 기존 Gnawed/Preserved/Ancient Rib·Jawbone·Collarbone 9종 + Preserved Cranium + Altered Collarbone = 11종. 기존 베이스 전체 134종 기록 호환 / 130종 신규 선택 목록을 그대로 유지한다. [실제 목록과 제외 근거](../current/manual-workbench-checkpoint.md)를 참조한다.
- Cranium: 기존 일반/Time-Lost 주얼 8종. 현재 Crafted 슬롯 상한, 상한 상실 후 기존 overflow, 미공개 슬롯 점유를 함께 처리한다. 새 삽입은 현재 상한을 넘지 못한다. Unique Undying Hate의 반복/파괴 규칙을 일반 주얼에 적용하지 않는다.
- Altered: 장신구 기본/훼손 풀에 해당 클래스의 Otherworldly 후보를 추가한다. `revealPool=ALTERED`를 미공개·선택 상태·film에 보존한다.
- Sinistral/Dextral Necromancy: 강제 접두/접미. Sovereign/Liege/Blackblooded: 무기·장신구에 Ulaman/Amanamu/Kurgal 풀 제한. 각 계열 하나씩 조합 가능, 성공 시만 소비 기록. Altered+Lich 조합은 검증된 상호작용 근거가 없어 원본을 보존하고 거부한다.
- Putrefaction: 접두·접미 개수를 관측 입력으로 받는다. 기존 명시 옵션을 대체하고 여러 미공개 슬롯·Corrupted를 함께 기록한다. 무작위 개수/측면/제거를 만들지 않는다. 분열·Mark 포함 입력 및 다른 징조와의 조합은 검증 근거가 없어 거부한다. 타락 아이템의 드러내기·복원은 허용하되 일반 화폐 적용은 차단한다.
- 수동 작업대만 `manualReveal=true`로 추가 정의를 받는다. 기존 공용 initial/자동 제작/목표 검색 카탈로그 및 일반 추첨 순서를 유지한다.
- 드러내기: `/abyss/eligible`가 슬롯/접두·접미/그룹 충돌/아이템 레벨/Ancient 최소 레벨/화폐·징조 풀을 검사한다. `/abyss/select`는 선택한 옵션 한 개와 롤을 검증한다. API 성공·보존 검증 후에만 공통 아이템/film 상태를 갱신한다. 실제 분포·성공률·평균 비용을 만들지 않는다. 소수 표시값은 정수 저장 단위로 무손실 변환한다.
- 기존 후보 3개 저장 기록과 API는 호환용으로 남긴다. 작업대 UI는 후보 3개 입력을 요구하지 않는다. 기존 기록을 삭제하거나 다시 쓰지 않는다. sealed 데이터 추가로 ruleset identity는 바뀌므로, 이전 identity의 film은 기존 정책대로 읽기 전용으로 보존하며 새 identity로 몰래 재표시하지 않는다. 하위 베이스 카탈로그와 기존 snapshot ID는 유지한다.
- **Echoes는 최신 직접 선택 방식으로 요구가 대체됨**: 재추첨 권리·가짜 소비·임의 후보 생성 없음. 선택 자체에 화폐 비용을 붙이지 않는다. 과거 기록은 보존한다.

## 자료와 적용 범위

[PoB 게임 데이터](https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/bb52d6b368307457eb9c54bb13f1829993d390b1/src/Data/ModVeiled.lua)를 커밋 고정해 사용했다. 원문 SHA-256 `95234097bcb70946ad451fbdb80b93cff3bd4a57abfdf29052305905fd32a632`. 게임 자료에서 추출한 277개 정의와 134개 베이스 매핑을 추가하고, 원문 행 및 필드 대조 결과는 `manual-reveal-source-20261102.json`에 남겼다.

원문의 weightKey/weightVal은 순서 있는 적격성 태그 판별에만 쓴다. runtime weight는 모두 0이며 일반 제작 추첨에 추가하지 않는다. 기존 일반 옵션은 현재 카탈로그의 양수 weight 적격성만 활용하며 **그 weight를 드러내기 확률로 사용하지 않는다**. [훼손 설명](https://poe2db.tw/us/Desecrated_Modifiers)에 기본 옵션 포함 가능성과 Lich 보장, 방어구 접두 전용 옵션 부재가 명시되어 있다. [Altered 클래스별 옵션](https://poe2db.tw/us/Altered_Collarbone)도 대조했다.

일반 주얼 전용 32개/Time-Lost 전용 12개는 구분한다. 다른 고유 특성의 베이스를 임의 통합하지 않는다. 원본에 tier 번호가 없어 `tier=0`과 `tier_unknown`을 내부 표식으로 사용하며 화면에서 T1로 날조하지 않는다. 신규 옵션 번역은 확인된 영어 원문으로 fallback하고 서비스 UI·화폐명은 기존 6개 언어를 유지한다. 기존 ruleset의 게임 패치/season은 UNVERIFIED를 유지하며 최신 라이브 서버와 완전 일치한다고 주장하지 않는다. additive resource를 sealed manifest에 포함한다.

## 남은 실제 한계

- Vertebrae/경로석: 현재 134종 작업대 베이스 밖. 별도 아이템 영역 확대를 하지 않았다.
- Abyssal Lord Mark: 우선 제거 및 higher-tier 대체 설명은 있지만 정확한 tier 매핑/중복/다른 징조와의 우선순위 자료가 없다. 표식을 단순 제거하고 완료한 것처럼 표시하지 않는다. 정확한 버전별 규칙 자료가 필요하다.
- 분열/Mark+Putrefaction, Altered+Lich, Putrefaction+다른 징조: 확정되지 않은 상호작용을 임의 구현하지 않았다. 사용자 취향 질문으로 게임 규칙을 결정할 사안은 아니다.
- 기본 뼈의 측면/만석 제거 및 Putrefaction 결과 개수는 관측값 입력이다. 확률이 없는 무작위 시뮬레이터와 구분한다.
- 원본 화폐 아이콘 CDN 접근 문제는 텍스트 타일로 처리한다. 신규 옵션의 확인되지 않은 번역은 영어 원문으로 표시한다.

## 직접 확인 순서

1. 첫 화폐 탭에서 아이템 클래스에 맞는 뼈 선택 → 접두/접미 및 만석이면 제거할 옵션 선택 → 아이템 적용.
2. 미공개 슬롯 선택 → 적격 옵션·롤 선택 → 적용. 기존 옵션·분열·품질·소켓 보존 확인.
3. 주얼 8종과 장신구 Altered에서 전용 옵션 확인. Lich 징조 사용 시 해당 풀만 보이는지 확인.
4. Putrefaction만 활성화 → 접두/접미 개수 입력 → 기존 옵션 대체·타락·여러 슬롯 확인 → 각각 드러내기.
5. 저장 후 새로고침/이전 프레임 복원/분기 → 선택 옵션·풀 제한·소비 기록 유지 확인. 잘못된 측면·레벨·분열 제거 및 요청 실패 시 원본/징조 유지 확인.

브라우저 자동 검증은 사용자 지시에 따라 미실행. 운영 재시작·배포·push 미실행. 코드·단위·통합 검사 결과는 최종 검증 JSON에 별도 기록한다.

## 검증 및 자체 리뷰

- 전체 백엔드: 993개, 기존 실패 40개. 신규 실패 0개/해소 0개. 실패 메시지가 바뀐 25개는 현재 v5 ruleset identity의 해시를 정규화하면 이전 메시지와 정확히 같다. 나머지 15개는 원문 메시지도 동일하다. 기존 실패를 통과로 표시하거나 기대값 완화로 숨기지 않았다.
- 전체 검사 이후 저장 풀의 클래스·옵션 제약을 보강하고 최종 소스로 영향 검사 137개, 통합 8개를 재실행해 모두 통과했다. 전체 검사와 마지막 경계 검증의 소스 단계를 구분해 JSON에 기록했다. spotlessCheck/bootJar 통과, 전체 실행의 generateJooq도 완료했다.
- 자체 리뷰에서 공용 초기 카탈로그 혼입을 수동 전용 요청/캐시로 분리했고, Lich 제한 위조 복원과 소수 입력의 조용한 반올림을 차단했다. 기본 옵션과 특수 풀의 포함 규칙, 보호된 옵션, 성공 시 소비, 저장 기록 불변성을 확인했다.
- 임시 컨테이너 종료 확인: 기존 `mini-redis-mini-redis-1`, `mini-redis-official-redis-1`만 남았다. 운영 데이터/볼륨/설정은 변경하지 않았다.
- 프런트 전체·lint·타입·포맷·빌드 최종 결과와 소스 해시는 `manual-reveal-validation-20261102.json` 참조.

최종 고정 소스 프런트 검사: 129개 파일 / 2,419개 모두 통과(단일 워커, 15분 제한). lint/typecheck/format:check/build 통과. 기존 번들 크기 경고만 남는다. 고정 이후 소스 42개 파일의 해시 변화 없음. 모든 상세 수치와 변경 파일 목록은 검증 JSON에 기록했다.
