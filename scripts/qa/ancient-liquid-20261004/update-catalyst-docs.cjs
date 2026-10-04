const fs=require('fs');
const note='> 최신 완료 집계 (2026-10-04): registry220 = active170(implemented170 = default163 + opt-in legacy7, pending0) + deferred50(보존 구현8 + 미구현42). Catalyst26는 검증된 제한 base만 IMPLEMENTED로 정리했다. 전체 구현178은 deferred8을 포함하므로 현재 사용 가능 수가 아니다. 아래의 이전 집계는 checkpoint 이력이다. [정의·base 제한·검증](PATHworkbench-catalyst-registry-2026-10-04.md).\n\n';
for(const p of ['README.md','ISSUES.md','docs/workbench-status-2026-10-04.md','docs/workbench-service-scope-2026-10-04.md','docs/workbench-ancient-liquid-2026-10-04.md','docs/supported-mechanics.md']){
 const old=fs.readFileSync(p,'utf8');if(old.includes('최신 완료 집계 (2026-10-04): registry220 = active170'))continue;
 fs.writeFileSync(p,note.replace('PATH',p.startsWith('docs/')?'':'docs/')+old);
}
