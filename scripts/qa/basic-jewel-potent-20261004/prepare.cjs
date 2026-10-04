const fs=require('fs'),path=require('path');
const root=path.resolve('../basic-jewel-potent-20261004');
for(const component of ['backend','frontend']) {
 const target=root+'/'+component+'-check';if(fs.existsSync(target))throw Error('Preserve existing check output: '+target);
 fs.cpSync(component,target,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['build','.gradle','node_modules','dist','coverage'].includes(s))});
}
const old=fs.readFileSync('../sapphire-generation-20261004/compose.yaml','utf8');
fs.writeFileSync(root+'/compose.yaml',old.replaceAll('exile-sapphire-generation-20261004','exile-basic-jewel-potent-20261004').replaceAll('18780','18880').replaceAll('18781','18881'));
fs.writeFileSync(root+'/backend-check.sh','#!/bin/sh\nset -eu\ncd /evidence/backend-check\nchmod +x gradlew\n./gradlew --no-daemon spotlessApply check generateJooq bootJar > /evidence/backend-check-1.log 2>&1\n');
fs.writeFileSync(root+'/frontend-check.sh','#!/bin/sh\nset -eu\ncd /evidence/frontend-check\nnpm ci > /evidence/frontend-check-1.log 2>&1\nnpx prettier --write src > /evidence/frontend-format-1.log 2>&1\nnpm run lint >> /evidence/frontend-check-1.log 2>&1\nnpm run typecheck >> /evidence/frontend-check-1.log 2>&1\nnpm run format:check >> /evidence/frontend-check-1.log 2>&1\nnpm run test -- --run >> /evidence/frontend-check-1.log 2>&1\nnpm run build >> /evidence/frontend-check-1.log 2>&1\n');
fs.writeFileSync(root+'/heavy-qa-owner.json',JSON.stringify({owner:'workbench/20261002',baseHead:'c691a2ee536f56e544f5383ed5df88b2395000e2',active:true,project:'exile-basic-jewel-potent-20261004',ports:[18880,18881]},null,2));
