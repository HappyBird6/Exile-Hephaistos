import fs from 'node:fs'
import {execFileSync} from 'node:child_process'
const qa='E:/WORK/Exile-Hephaistos/codex/boots-qa-20261004'
const mode=process.argv[2]
if(mode==='backend'||mode==='backend-final') {
 for(const path of ['src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java','src/main/java/com/poe2craft/crafting/application/WorkbenchService.java','src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java','src/main/java/com/poe2craft/crafting/domain/QualityLimitRules.java','src/main/java/com/poe2craft/item/infrastructure/ItemCatalogLoader.java','src/main/java/com/poe2craft/item/ReviewedBoots.java','src/test/java/com/poe2craft/crafting/ReviewedBootsTest.java'])fs.copyFileSync(`${qa}/backend-check/${path}`,`backend/${path}`)
 if(mode==='backend-final')process.exit(0)
 fs.copyFileSync('backend/src/main/resources/crafting/registry-v2.json',`${qa}/backend-check/src/main/resources/crafting/registry-v2.json`)
 fs.writeFileSync(`${qa}/backend-check-2.sh`,'#!/bin/sh\nset -eu\ncd /qa/backend-check\nsh gradlew --no-daemon check generateJooq bootJar > /qa/backend-check-2.log 2>&1\ncp build/libs/poe2craft.jar /qa/poe2craft.jar\n')
} else if(mode==='frontend') {
 for(const path of execFileSync('git',['diff','--name-only'],{encoding:'utf8'}).trim().split('\n').filter(p=>p.startsWith('frontend/src/')))fs.copyFileSync(`${qa}/frontend-check/${path.slice('frontend/'.length)}`,path)
} else throw new Error('Select backend or frontend')
