# 데이터 도구의 독립 검사 입력

`catalog-import`는 importer의 숫자·언어 binder와 원본 순서 보존을 독립적으로 비교하는 입력이다. 여기서 baseline은 회귀 검사의 기준 데이터이며 일별 수집·배포 로그가 아니다.

실행 중인 앱 데이터와 구별한다. 기준 값을 현재 구현 출력으로 무조건 재생성하지 않는다. `scripts/tests/reviewed-catalog-importer.test.mjs`와 `scripts/tests/evidence-files.test.mjs`로 원본 옵션 대응과 기능별 파일 조회를 검사한다.
