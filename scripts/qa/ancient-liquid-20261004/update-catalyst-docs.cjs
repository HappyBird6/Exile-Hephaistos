const fs=require('fs');
const path=require('node:path').posix;
const note='> 최신 완료 집계 (2026-10-04): registry220 = active170(implemented170 = default163 + opt-in legacy7, pending0) + deferred50(보존 구현8 + 미구현42). Catalyst26는 검증된 제한 base만 IMPLEMENTED로 정리했다. 전체 구현178은 deferred8을 포함하므로 현재 사용 가능 수가 아니다. 아래의 이전 집계는 checkpoint 이력이다. [정의·base 제한·검증](PATH).\n\n';
for(const p of ['README.md','ISSUES.md','docs/archive/2026-10/workbench/workbench-status-2026-10-04.md','docs/archive/2026-10/workbench/workbench-service-scope-2026-10-04.md','docs/archive/2026-10/workbench/workbench-ancient-liquid-2026-10-04.md','docs/supported-mechanics.md']){
 const old=fs.readFileSync(p,'utf8');if(old.includes('최신 완료 집계 (2026-10-04): registry220 = active170'))continue;
 fs.writeFileSync(p,note.replace('PATH',path.relative(path.dirname(p),'docs/archive/2026-10/workbench/workbench-catalyst-registry-2026-10-04.md'))+old);
}
