// Compatibility entrypoint for existing import scripts; the shared registry owns all mirrors.
import { synchronize, mirrors } from './sync-workbench-data.mjs'
import { fileURLToPath } from 'node:url'
synchronize(fileURLToPath(new URL('..', import.meta.url)), !process.argv.includes('--check'), mirrors.slice(0, 3))
