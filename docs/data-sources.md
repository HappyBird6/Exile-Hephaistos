# 데이터 출처

production seed와 검증된 게임 catalog는 아직 없습니다. Python 원본 수집 코드는
[data-pipeline/README.md](../data-pipeline/README.md)에 설명합니다. 자동 실행하지 않습니다.

- poe2db: 지정된 공개 목록의 raw 수집기만 제공합니다. 정규화·게임 데이터 검증은 후속 작업입니다.
- poe.ninja: 공식 economy API 계약을 확인한 뒤 adapter를 구현합니다.
- GGG: 문서화된 PoE2 지원 API와 scope가 확인된 기능만 구현합니다.
- synthetic: 테스트 전용이며 실제 game ID를 사용하지 않습니다.

TODO(domain): 원본 URL/hash, patch/snapshot, 검토 기록 확보 / 해당 소스 공식 정책 / catalog 및 가격 / 현재 미지원.

## poe2db 사전 조사 — 2026-09-22

- [robots.txt](https://poe2db.tw/robots.txt): 조사 당시 `User-agent: *`, `Allow: /`.
  실행 시 다시 읽고 제한이 바뀌면 따릅니다. 이것만으로 재배포 권한을 인정하지 않습니다.
- [General disclaimer](https://poe2db.tw/us/General_disclaimer): Wiki content의 CC BY-NC-SA 3.0
  고지와 GGG 게임 자산 권리 고지가 함께 있습니다. 게임 데이터 전체에 Wiki 라이선스를 확대 적용하지 않습니다.
- 수집 범위: 로컬 조사용 [Currency](https://poe2db.tw/us/Currency),
  [Amulets](https://poe2db.tw/us/Amulets) HTML 및 robots·고지 증거. 배포용 데이터 포함·이미지 수집은 하지 않습니다.
- 최초 조사에서 위 네 페이지의 로컬 원본 저장이 끝난 뒤 사용자가 코드만 작성하도록 변경했습니다.
  그 이후 live 수집은 하지 않았으며, 코드 검증에는 synthetic fixture만 사용합니다.

TODO(domain): 데이터 재배포 조건·실제 패치·목록 완전성·속성 및 weight 파싱 계약 검증 필요 /
poe2db 고지 및 원본 검토 / catalog 공개·제작 계산 / 현재 raw 전용, publish와 계산 미지원.
