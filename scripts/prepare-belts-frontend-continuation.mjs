import fs from 'node:fs'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
fs.writeFileSync(`${q}/frontend-check-5.sh`, `#!/bin/sh
set -eu
export HEPHAISTOS_TRANSLATION_ROOT=/source
cd /qa/frontend-check
npx prettier --write src/features/crafting/ReviewedBeltsContract.test.ts > /qa/frontend-check-5.log 2>&1
npm run format:check >> /qa/frontend-check-5.log 2>&1
npm run test -- --run >> /qa/frontend-check-5.log 2>&1
npm run build >> /qa/frontend-check-5.log 2>&1
`, { flag: 'wx' })
