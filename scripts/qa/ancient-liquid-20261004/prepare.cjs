const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const root=path.resolve('../ancient-liquid-20261004');
for(const d of fs.readdirSync('..')){const p='../'+d+'/heavy-qa-owner.json';if(fs.existsSync(p))assert(!JSON.parse(fs.readFileSync(p)).active,'Heavy QA already owned: '+p)}
for(const component of ['backend','frontend']) {
 const target=root+'/'+component+'-check';assert(!fs.existsSync(target),'Preserve earlier check copy');
 fs.cpSync(component,target,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['build','.gradle','node_modules','dist','coverage'].includes(s))});
}
fs.cpSync('../basic-jewel-potent-20261004/gradle-cache',root+'/gradle-cache',{recursive:true});
fs.writeFileSync(root+'/compose.yaml',fs.readFileSync('../basic-jewel-potent-20261004/compose.yaml','utf8').replaceAll('exile-basic-jewel-potent-20261004','exile-ancient-liquid-20261004').replaceAll('18880','18980').replaceAll('18881','18981'));
fs.writeFileSync(root+'/backend-check.sh','#!/bin/sh\nset -eu\ncd /evidence/backend-check\nchmod +x gradlew\n./gradlew --no-daemon spotlessApply check generateJooq bootJar > /evidence/backend-check-1.log 2>&1\n');
fs.writeFileSync(root+'/frontend-check.sh','#!/bin/sh\nset -eu\ncd /evidence/frontend-check\nnpm ci > /evidence/frontend-check-1.log 2>&1\nnpx prettier --write src > /evidence/frontend-format-1.log 2>&1\nnpm run lint >> /evidence/frontend-check-1.log 2>&1\nnpm run typecheck >> /evidence/frontend-check-1.log 2>&1\nnpm run format:check >> /evidence/frontend-check-1.log 2>&1\nnpm run test -- --run >> /evidence/frontend-check-1.log 2>&1\nnpm run build >> /evidence/frontend-check-1.log 2>&1\n');
fs.writeFileSync(root+'/heavy-qa-owner.json',JSON.stringify({owner:'workbench/20261002',baseHead:'d36ef9a6a8a5b4371d59c09cf5d295eb1b2f0833',active:true,project:'exile-ancient-liquid-20261004',ports:[18980,18981]},null,2));
