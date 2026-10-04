const fs = require('fs');
const cp = require('child_process');
const crypto = require('crypto');
const root = process.cwd();
const qa = 'E:/WORK/Exile-Hephaistos/codex/sapphire-generation-20261004';
const changed = cp.execFileSync('git', ['diff', '--name-only'], {encoding:'utf8'}).trim().split('\n');
for (const p of changed.filter(p=>p.startsWith('frontend/src/'))) {
  const checked = qa+'/frontend-check/'+p.slice('frontend/'.length);
  if (fs.existsSync(checked)) fs.copyFileSync(checked,root+'/'+p);
}
for (const p of ['src/features/crafting/SapphireGenerationContract.test.ts','src/shared/test/sapphire-generation-responses.json']) fs.copyFileSync(qa+'/frontend-check/'+p,root+'/frontend/'+p);
const log=fs.readFileSync(qa+'/frontend-contract-check.log','utf8');
if (!/464 passed/.test(log)||!/built in/.test(log)) throw Error('Final frontend checks incomplete');
const files=[];
function walk(path){for(const d of fs.readdirSync(root+'/'+path,{withFileTypes:true})){const p=path+'/'+d.name;if(d.isDirectory())walk(p);else files.push(p)}}
walk('backend/src');walk('frontend/src');
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
for(const p of files){const checked=qa+'/'+p.replace('backend/','backend-check/').replace('frontend/','frontend-check/');if(!fs.existsSync(checked)||hash(root+'/'+p)!==hash(checked))throw Error('Checked source mismatch '+p)}
const evidence={baseHead:'3208ff40d583d72ab7c77eb1baea595d644fb9a9',snapshotId:'poe2db-sapphire-basic-liquid-20261004-2eaf12518879',ordinary:{count:58,prefix:23,suffix:35,weightPolicy:'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS'},positiveBasicCurrencies:18,positiveBasicLiquids:10,backend:{unit:378,integration:6,command:'spotlessApply check generateJooq bootJar'},frontend:{tests:464,npmCi:true,lint:true,typecheck:true,formatCheck:true,build:true},api:JSON.parse(fs.readFileSync(qa+'/api-results.json')).checks,browser:JSON.parse(fs.readFileSync(qa+'/browser-results.json')).passed,priorQualityAndOmenRegression:JSON.parse(fs.readFileSync(qa+'/omen-api-results.json')).passed,sourceFilesVerified:files.length,jarSha256:hash(qa+'/backend-check/build/libs/poe2craft.jar'),evidenceDirectory:qa,screenshots:['en','ko','zh-CN','zh-TW','ja','es'].flatMap(l=>[l+'-1440.png',l+'-390.png']),isolatedProject:'exile-sapphire-generation-20261004',livePortsPreserved:[18080,18081],recoveryHistory:['Node22 EBADENGINE: corrected to required Node24','Previous one-modifier localization assertion: replaced by exact 68-source-definition assertion','QA Whittling trigger: corrected Annulment input to existing Chaos trigger','QA Liquid label lookup: added existing product registry fallback','Early backend sync attempt interrupted; final full backend checks passed'],remaining:['Ruby/Emerald/Diamond eligible pools','Potent Melancholy/Ferocity/Contempt with insertion cap versus preserved overflow','Ancient13 Time-Lost mechanics'],review:'Self review; no independent review claim',deployment:'No push, merge, deploy or live update'};
fs.mkdirSync(root+'/docs/evidence',{recursive:true});fs.writeFileSync(root+'/docs/evidence/workbench-sapphire-generation-liquid-validation-2026-10-04.json',JSON.stringify(evidence,null,2)+'\n');
console.log(JSON.stringify({verifiedSourceFiles:files.length,frontendTests:464}));
