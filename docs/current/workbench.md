# Workbench

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 역할

Workbench는 검증된 아이템 상태에 선택한 화폐를 적용하는 시뮬레이터다. 단순 화폐 설명 카드에 머물지 않으며, 실제 게임 계정이나 인벤토리를 변경하지 않는다. 자동 탐색의 전체 전략 계산과 단일 적용을 구분한다.

`/api/v1/crafting/workbench` 아래 `/actions`는 자격 있는 동작, `/apply`는 적용, `/quality-display`는 품질 표시, `/map-text`는 텍스트 매핑 경로다. 후보 생성은 검토된 베이스 카탈로그, 희귀도와 슬롯, family 충돌, 아이템 레벨 및 화폐별 조건을 사용한다.

## 상태·확률·보존

registered base 수는 [지원 범위](support.md), 상태 검증은 [아이템 상태](item-state.md)를 따른다. 등록된 화폐가 모든 베이스에서 사용 가능하다는 뜻은 아니다. 선택/roll의 가정과 출처를 구분하고, 증거 없는 효과나 분포를 임의로 추가하지 않는다.

품질 상한 변경의 기본은 기존 값 보존이며, 베이스별 품질 및 Catalyst 제한은 별도로 적용된다. [품질 정책](../workbench-quality-preserve-2026-10-04.md)과 [catalog 유지보수](../catalog-data-maintenance.md)를 참고한다. 과거 film·검증 보고서는 당시 조건의 증거로 보존하며 모든 과거 저장 형식이 현재 재실행 가능하다고 주장하지 않는다.

## 확인되지 않은 기능

[감정 주문서·숨겨진 상태](../workbench-wisdom-evidence-2026-10-03.md)는 reveal과 보존·적격성에 필요한 근거가 미해결이다. 다른 베이스·제작 수단은 현재 catalog·화폐 정책과 [미해결 이슈](../../ISSUES.md)를 확인한다.


## 직전 화폐의 옵션 표시

현재 기보 위치에서 직전에 성공한 화폐 적용으로 추가되거나 값·분열 상태·표시 효과가 바뀐 옵션 앞에 빨간 원을 표시한다. 같은 값이 다시 뽑혔더라도 검증된 재굴림 event가 있으면 표시한다. 반복 화폐는 저장된 한 단계의 시작·최종 상태와 마지막 성공 witness를 사용하며 중간 적용을 추정하지 않는다. 제거된 옵션은 현재 카드에 표시하지 않는다.

표시는 기보에서 계산하는 파생값이다. 실패·화폐 선택만으로 기존 표시를 지우지 않으며, 단계 이동·세션 복원에 맞춰 갱신한다. 미공개 훼손 슬롯을 수동으로 드러낸 경우에는 슬롯 연결을 유지한다. 옵션 문구·수치와 저장 계약은 바꾸지 않는다.

## 기능 연결과 유지보수

베이스·입력 관측·창고·기록·훼손·주얼 overflow·품질/축성·반복 Chaos의 현재 연결과 보존 조건은 [통합 문서](../development.md)를 따른다. 초기 계획·담당자별 patch 안내·작업별 검증 이력은 해당 문서의 Git 조회 방법으로 확인한다.

## 구현 근거

[WorkbenchService.java](../../backend/src/main/java/com/poe2craft/crafting/application/WorkbenchService.java) · [WorkbenchController.java](../../backend/src/main/java/com/poe2craft/crafting/presentation/WorkbenchController.java)

[문서 지도](../README.md)

## 소켓과 Augment

장비별 추가 1개를 허용하는 Workbench 정책과 79종 일반 Rune/Soul Core의 장착·표시·미지원 경계는 [소켓과 Augment](socket-augments.md)를 따른다.


## 선택한 베이스의 보관함 · 2026-11-05

기본 Solar Amulet 그림만 있는 첫 화면은 실제 선택 완료로 취급하지 않는다. 선택·새 제작·복원으로 구조화된 아이템/기록이 생기거나, 가져온 텍스트의 등록 베이스가 확인되면 그 베이스의 지원 부위로 보관함을 필터한다. 새 제작은 같은 베이스를 유지하고, 활성 아이템이 없는 초기 상태에서는 전체 기본 목록을 보여준다.

`initial.baseMaterialActions`는 실행 가능 목록이 아닌 **베이스·검토 catalog 자격**이다. Backend `WorkbenchSimulator`가 실제 실행과 같은 Essence/Alloy 대상 override와 catalog 포함 여부, Catalyst의 ordinary/refined 베이스 정책, Jewel Liquid의 검토 출처를 재사용한다. 희귀도·레벨·현재 옵션·소켓 개수·활성 징조·Crafted 존재 여부 등 일시적인 실행 조건은 포함하지 않는다. 실제 적용 시의 기존 검증은 그대로 수행한다.

Frontend는 이 메타데이터를 현재 베이스와 일치하는 응답에서만 사용한다. Augment는 기존 등록 소켓 지원 상한과 해당 itemClass의 검토된 효과를 사용하며 빈 소켓이 없거나 아직 0개여도 표시한다. Perfect-only/Tempered/Soul Core 선택 정책은 먼저 유지한다. Omen과 일반 Currency는 부위 필터 대상이 아니다. 지원하지 않는/검토되지 않은 재료는 선택된 부위 목록에 나타나지 않으며 게임 전체 지원 범위로 추정하지 않는다.

검색과 Essence 특수 재료 첫 행·4행 스크롤은 유지한다. 필터로 숨은 재료도 기존 즐겨찾기·장착·저장 기록에서 삭제하지 않는다. 알려진 베이스의 자격 메타데이터를 받기 전에는 목록을 임의로 줄이지 않는다.
