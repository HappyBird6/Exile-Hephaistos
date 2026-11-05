# 번역·표시 자료

- `sources/en`, `ko`, `zh-cn`, `zh-tw`, `ja`, `es`: 해당 언어의 원문 JSON/HTML이다. 아이템 종류별 폴더는 두지 않는다.
- `mappings`: 복합 옵션의 숫자 자리, 원본 단위 변환, 표시 문구의 대응 자료다.
- `checks/display-coverage.json`: 현재 실행한 여섯 언어 누락 검사 결과다.

파일명으로 대상을 찾고 JSON의 URL·원문·ID·내용 해시로 출처를 확인한다. 언어별 원문을 수정해 번역처럼 보이게 만들지 않는다. 실제 표시 정의는 `frontend/src/shared/i18n`에 있으며 원문과의 일치 여부를 검사한다.
