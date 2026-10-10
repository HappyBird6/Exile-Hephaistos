const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const Ajv = require('ajv');
const root = path.resolve(__dirname, '../../..');
const read = p => JSON.parse(fs.readFileSync(path.join(root, p), 'utf8'));
const schema = read('../contracts/crafting-paths-v1/schema.json');
const ajv = new Ajv({allErrors: true, schemaId: 'auto'});
ajv.addSchema(schema);
let count = 0;
function validate(definition, value) {
  const check = ajv.getSchema(schema.$id + '#/definitions/' + definition);
  assert(check(value), JSON.stringify(check.errors)); count++;
}
validate('jobSnapshot', read('build/path-search/solar-snapshot.json'));
for (const page of read('build/path-search/solar-pages.json')) validate('graphPage', page);
for (const example of read('build/path-search/fixture-roundtrips.json')) validate(example.definition, example.value);
const fraction = p => [BigInt(p.numerator), BigInt(p.denominator)];
const add = (a, b) => [a[0] * b[1] + b[0] * a[1], a[1] * b[1]];
const equal = (a, b) => a[0] * b[1] === b[0] * a[1];
const snapshot = read('build/path-search/solar-snapshot.json');
for (const r of snapshot.recommendations) for (const p of r.points) {
  assert(equal([p.lower, p.active, p.dead, p.unresolved].map(fraction).reduce(add), [1n, 1n]));
  assert(equal(add(fraction(p.lower), fraction(p.unresolved)), fraction(p.upper)));
}
const executions = new Map(), edges = new Map();
for (const page of read('build/path-search/solar-pages.json')) {
  for (const e of page.executions) executions.set(e.id, e);
  for (const e of page.edges) { assert(executions.has(e.from) && executions.has(e.to)); edges.set(e.id, e); }
  for (const x of page.expansions) {
    const outgoing = [...edges.values()].filter(e => e.from === x.executionId).map(e => fraction(e.probability)).reduce(add, [0n, 1n]);
    assert(equal(add(outgoing, fraction(x.unresolved)), x.status === 'UNAVAILABLE' ? [0n, 1n] : [1n, 1n]));
  }
}
console.log(`Backend wire schema: ${count} runtime/round-trip payloads validated`);
