const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const cp = require('child_process');
const root = 'E:/WORK/Exile-Hephaistos/codex/basic-jewel-potent-20261004';
const sha = p => crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const modified = cp.execFileSync('git', ['diff', '--name-only'], {encoding:'utf8'}).trim().split('\n');
const untracked = cp.execFileSync('git', ['ls-files', '--others', '--exclude-standard'], {encoding:'utf8'}).trim().split('\n');
const own = [...new Set([...modified,...untracked])].filter(p=>p.startsWith('frontend/src/'));
for (const p of own) fs.copyFileSync(root+'/frontend-check/'+p.slice('frontend/'.length),p);
for(const p of ['src/features/crafting/BasicJewelPotentContract.test.ts','src/shared/test/basic-jewel-potent-responses.json'])fs.copyFileSync(root+'/frontend-check/'+p,'frontend/'+p);
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(dir+'/'+e.name):[dir+'/'+e.name])}
const compared=[];
for(const area of ['backend','frontend'])for(const file of walk(area+'/src')){
 const relative=file.slice(area.length+1),copy=root+'/'+area+'-check/'+relative;
 if(!fs.existsSync(copy)||sha(file)!==sha(copy))throw Error('Checked source differs: '+file);
 compared.push(file);
}
const tests={};
for(const dir of fs.readdirSync(root+'/backend-check/build/test-results')){
 tests[dir]=fs.readdirSync(root+'/backend-check/build/test-results/'+dir).filter(f=>f.endsWith('.xml')).reduce((n,f)=>n+Number(fs.readFileSync(root+'/backend-check/build/test-results/'+dir+'/'+f,'utf8').match(/<testsuite[^>]* tests="(\d+)"/)?.[1]||0),0);
}
const api=JSON.parse(fs.readFileSync(root+'/api-results.json'));
const browser=JSON.parse(fs.readFileSync(root+'/browser-results.json'));
const frontend=fs.readFileSync(root+'/frontend-final.log','utf8').replace(/\x1b\[[0-9;]*m/g,'');
const owner=JSON.parse(fs.readFileSync(root+'/heavy-qa-owner.json'));
const evidence={baseline:'c691a2ee536f56e544f5383ed5df88b2395000e2',date:'2026-10-04',scope:'Ruby/Emerald/Diamond Basic Jewel and Potent Liquid3; Sapphire compatibility',ordinaryCandidates:{ruby:50,emerald:74,sapphire:58,diamond:160},craftedCandidates:{ruby:15,emerald:15,sapphire:15,diamond:5},backend:{command:'spotlessApply check generateJooq bootJar',passed:true,tests},frontend:{npmCi:true,lint:true,typecheck:true,formatCheck:true,tests:Number(frontend.match(/Tests\s+(\d+) passed/)?.[1]),build:frontend.includes('built in')},api:{checks:api.checks,captures:api.captures.length},browser:{checks:browser.passed,pageErrors:browser.errors,screenshots:fs.readdirSync(root).filter(f=>f.endsWith('.png'))},checkedSourceFiles:compared.length,identicalCheckedSources:true,isolatedProject:'exile-basic-jewel-potent-20261004',ports:[18880,18881],livePortsUntouched:[18080,18081],qaOwner:owner,logsDirectory:root,failureHistory:[{log:'backend-check-1.log',failure:'Missing BasicJewel import',resolution:'Added import; reran full checks'},{log:'backend-check-2.log',failure:'ArchUnit application service depended on catalog infrastructure',resolution:'Moved catalog loading to bootstrap; reran full checks'}],provisional:['1/N after eligibility; actual weights unavailable','Ferocity matching quality multiplies from original values, one central rounding','Contempt cap-loss overflow preserves existing affixes; secondary evidence only','Spanish absent Liquid templates use documented semantic translation'],remaining:'Ancient Liquid13; no remote push/merge/deploy/release',selfReview:'Source identity, category/tags, atomic no-spend, family/slot restrictions, derived values, six languages and history verified'};
fs.mkdirSync('docs/evidence',{recursive:true});fs.writeFileSync('docs/evidence/workbench-basic-jewel-potent-validation-2026-10-04.json',JSON.stringify(evidence,null,2)+'\n');
evidence.visuallyInspected=['contact-ruby.png','contact-emerald.png','contact-diamond.png','contact-sapphire.png','ruby-ko-390.png','emerald-es-1440.png','diamond-zh-CN-390.png','sapphire-ja-1440.png','sapphire-overflow.png','omen-candidates.png'];
evidence.failureHistory.push({log:'browser-failure-1.json',failure:'Harness selected one-affix Ferocity sample but expected four Alt summaries',resolution:'Selected actual four-affix response; repeated full browser checks without weakening assertion'});
evidence.failureHistory.push({log:'browser-failure-2.json',failure:'Same sparse sample selection in final Omen preview harness',resolution:'Selected actual four-affix before state; repeated full browser checks without weakening assertion'});
evidence.jarSha256=sha(root+'/backend-check/build/libs/poe2craft.jar');
evidence.buildWarning='Vite existing large-chunk warning; build passed without changing warning thresholds';
fs.writeFileSync('docs/evidence/workbench-basic-jewel-potent-validation-2026-10-04.json',JSON.stringify(evidence,null,2)+'\n');
console.log(JSON.stringify({tests,frontend:evidence.frontend,api:evidence.api,browser:browser.passed,compared:compared.length}));
