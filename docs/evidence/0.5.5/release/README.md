# 현재 구조의 검사 결과

`checks/backend.json`은 관련 loader·metadata·architecture·DB 통합·jOOQ·JAR 검사를, `checks/frontend.json`은 정적 검사·빌드·전체 Frontend와 독립 도구 검사를 구분해 기록한다.

Frontend 기존 실패 9개는 변경 전 commit에서도 재현됐다. 전체 앱 검사가 모두 통과한 것으로 표시하지 않는다. 이 결과는 저장소 구조와 현재 소스의 검사이며 실제 게임 0.5.5 규칙·운영 서버 배포·브라우저 검증을 뜻하지 않는다.
