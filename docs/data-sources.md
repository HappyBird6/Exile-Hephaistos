# 데이터 출처

검증된 게임 catalog와 production seed는 아직 없다. 외부 데이터를 수집하는 실행 코드와 API는 제공하지 않는다.

현재 사용하는 표시 자산은 [제작 화면 자산 기록](crafting-ui.md), `frontend/public/assets/currency/sources.json`, `frontend/public/assets/materials/sources.json`, `frontend/public/assets/materials/tooltip-sources.json`에 기록한다. 게임 아트의 권리는 원저작권자에게 있으며 출처 기록이 재배포 허가를 의미하지 않는다.

아이템 파서의 참조 출처와 라이선스는 [아이템 파싱 명세](item-text-parsing.md)와 `third-party/PathOfBuilding-LICENSE.txt`에 보존한다.

과거에 저장한 로컬 원본은 이번 코드 정리로 삭제하지 않는다. `data-pipeline/captures/`의 Git 제외 규칙도 원본 보호를 위해 유지한다. 해당 경로가 기록에 등장하더라도 실행 가능한 수집 기능을 뜻하지 않는다.

테스트용 synthetic 데이터는 실제 게임 규칙의 근거가 아니다. 실제 제작 계산을 추가하려면 먼저 대상 patch·규칙·weight의 출처와 검증 기록이 필요하다.
