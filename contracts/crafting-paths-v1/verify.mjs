import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { createRequire } from "node:module";

// Use the existing frontend dependency tree; no install, server, DB or build is performed.
const require = createRequire(
  new URL("../../frontend/package.json", import.meta.url),
);
const Ajv = require("ajv");
const here = new URL("./", import.meta.url);
const resources = new URL("../../backend/src/main/resources/", here);
const read = (base, name) =>
  JSON.parse(readFileSync(new URL(name, base), "utf8"));
const schema = read(here, "schema.json");
const fixtures = read(here, "synthetic-fixtures.json");
const solar = read(here, "solar-source-fixture.json");
const protocol = read(here, "protocol-cases.json");
assert.equal(
  protocol.verification,
  "EXPECTED_CONSUMER_TESTS_NOT_RUNTIME_EXECUTED",
);
assert.equal(
  new Set(protocol.cases.map((c) => c.id)).size,
  protocol.cases.length,
);
assert(
  protocol.cases.every(
    (c) => c.given && c.operation && (c.expectedHttp || c.expected),
  ),
);
const ajv = new Ajv({
  allErrors: true,
  strictKeywords: true,
  schemaId: "auto",
});
assert(ajv.validateSchema(schema), JSON.stringify(ajv.errors));
ajv.addSchema(schema);
const validators = new Map();
const validate = (name, value) => {
  if (!validators.has(name)) {
    validators.set(
      name,
      ajv.compile({ $ref: schema.$id + "#/definitions/" + name }),
    );
  }
  const check = validators.get(name);
  assert(check(value), name + ": " + JSON.stringify(check.errors));
};
const gcd = (a, b) => {
  while (b) [a, b] = [b, a % b];
  return a;
};
const fraction = (value) => {
  const n = BigInt(value.numerator),
    d = BigInt(value.denominator);
  assert(d > 0n && n >= 0n && n <= d, "Probability outside [0,1]");
  assert.equal(gcd(n, d), 1n, "Fractions must be reduced");
  return [n, d];
};
const eq = (a, b) => a[0] * b[1] === b[0] * a[1];
const cmp = (a, b) => a[0] * b[1] - b[0] * a[1];
const add = (a, b) => [a[0] * b[1] + b[0] * a[1], a[1] * b[1]];
const sum = (values) => values.reduce(add, [0n, 1n]);
function point(p) {
  const [low, high, active, dead, unresolved] = [
    "lower",
    "upper",
    "active",
    "dead",
    "unresolved",
  ].map((k) => fraction(p[k]));
  assert(
    eq(sum([low, active, dead, unresolved]), [1n, 1n]),
    "Mass must sum to one",
  );
  assert(eq(add(low, unresolved), high), "upper must equal lower + unresolved");
  assert.equal(
    p.status,
    unresolved[0] === 0n ? "COMPLETE" : low[0] === 0n ? "UNKNOWN" : "PARTIAL",
  );
}
function graph(g, previous = null) {
  const nodes = new Map(previous?.nodes ?? []);
  const executions = new Map(previous?.executions ?? []);
  const edges = new Map(previous?.edges ?? []);
  for (const node of g.nodes) {
    if (nodes.has(node.id)) assert.deepEqual(nodes.get(node.id), node);
    nodes.set(node.id, node);
  }
  for (const execution of g.executions) {
    assert(nodes.has(execution.stateId), "Dangling state reference");
    if (executions.has(execution.id))
      assert.deepEqual(executions.get(execution.id), execution);
    executions.set(execution.id, execution);
  }
  for (const edge of g.edges) {
    assert(
      executions.has(edge.from) && executions.has(edge.to),
      "Dangling execution reference",
    );
    assert.equal(
      executions.get(edge.from).policyId,
      executions.get(edge.to).policyId,
    );
    fraction(edge.probability);
    if (edges.has(edge.id)) assert.deepEqual(edges.get(edge.id), edge);
    edges.set(edge.id, edge);
  }
  for (const expansion of g.expansions) {
    assert(executions.has(expansion.executionId));
    const outgoing = [...edges.values()].filter(
      (e) => e.from === expansion.executionId,
    );
    const unresolved = fraction(expansion.unresolved);
    if (expansion.status === "UNAVAILABLE") {
      assert.equal(outgoing.length, 0);
      assert.equal(unresolved[0], 0n);
    } else {
      assert(
        eq(add(sum(outgoing.map((e) => fraction(e.probability))), unresolved), [
          1n,
          1n,
        ]),
      );
      if (expansion.status === "COMPLETE") assert.equal(unresolved[0], 0n);
      if (expansion.status === "UNSUPPORTED") assert.equal(outgoing.length, 0);
    }
  }
  return { nodes, executions, edges };
}
function snapshot(job) {
  assert.equal(job.graph.jobId, job.jobId);
  assert.equal(job.graph.revision, job.revision);
  const policies = new Map(job.recommendations.map((r) => [r.policy.id, r]));
  assert.equal(policies.size, job.recommendations.length);
  assert(job.candidateScope.generated >= policies.size);
  if (job.candidateScope.enumerationComplete)
    assert.equal(job.candidateScope.generated, job.candidateScope.total);
  if (job.status === "UNSUPPORTED") {
    assert.equal(job.capabilities.search, "UNSUPPORTED");
    assert.equal(
      job.recommendations.length,
      0,
      "Unsupported input cannot have success estimates",
    );
    assert.equal(job.rankings.length, 0);
  }
  if (job.resumable) assert(["PAUSED", "CANCELLED"].includes(job.status));
  for (const r of job.recommendations) {
    assert(
      r.policy.actions.every((action) =>
        job.capabilities.actions.includes(action),
      ),
    );
    let previous = null;
    for (const p of r.points) {
      point(p);
      if (previous) {
        assert(BigInt(p.attempts) > BigInt(previous.attempts));
        assert(
          cmp(fraction(p.lower), fraction(previous.lower)) >= 0n,
          "CDF lower must be monotone",
        );
      }
      previous = p;
    }
    if (r.eventual.status === "PROVEN") {
      assert(r.eventual.proofVersion);
      const eventual = fraction(r.eventual.probability);
      for (const p of r.points) assert(cmp(eventual, fraction(p.lower)) >= 0n);
    } else {
      assert.equal(r.eventual.probability, null);
      assert.equal(r.eventual.proofVersion, null);
    }
  }
  for (const ranking of job.rankings) {
    const entries = ranking.entries.map((e) => {
      assert(policies.has(e.policyId));
      const p = policies
        .get(e.policyId)
        .points.find((p) => p.attempts === ranking.attempts);
      assert(p);
      return { ...e, point: p };
    });
    assert.equal(new Set(entries.map((e) => e.policyId)).size, entries.length);
    if (ranking.status === "CERTIFIED_WITHIN_CANDIDATES") {
      assert(
        job.candidateScope.enumerationComplete,
        "Unsearched candidates cannot be certified",
      );
      assert.equal(
        job.candidateScope.total,
        policies.size,
        "All candidates must be evaluated for v1 certification",
      );
      assert(
        [...policies.values()].every(
          (r) =>
            r.points.find((p) => p.attempts === ranking.attempts)?.status ===
            "COMPLETE",
        ),
      );
    }
    for (let i = 0; i < entries.length; i++) {
      if (i === 0) assert.equal(entries[i].rank, 1);
      else {
        const order = cmp(
          fraction(entries[i - 1].point.lower),
          fraction(entries[i].point.lower),
        );
        assert(order >= 0n);
        assert.equal(
          entries[i].rank,
          order === 0n ? entries[i - 1].rank : i + 1,
        );
      }
    }
  }
  const retained = graph(job.graph);
  for (const edge of retained.edges.values()) {
    const source = retained.executions.get(edge.from);
    const policy = policies.get(source.policyId)?.policy;
    assert(policy, "Graph execution must reference an evaluated policy");
    assert.equal(edge.action, policy.actions[source.phase]);
    assert.notEqual(
      retained.nodes.get(source.stateId).goalStatus,
      "MATCH",
      "First hit is absorbing",
    );
  }
}
function semantic(definition, value) {
  if (definition === "jobSnapshot") snapshot(value);
  if (definition === "createRequest") {
    assert(value.observations.every((n) => BigInt(n) <= 9223372036854775807n));
    assert.equal(value.start.item.baseItemId, value.goal.general.baseItemId);
  }
}
let positive = 0,
  negative = 0;
for (const example of fixtures.examples) {
  if (example.expectedInvalid === "schema") {
    assert.throws(
      () => validate(example.definition, example.value),
      undefined,
      example.id,
    );
    negative++;
  } else {
    validate(example.definition, example.value);
    if (example.expectedInvalid === "semantic") {
      assert.throws(
        () => semantic(example.definition, example.value),
        undefined,
        example.id,
      );
      negative++;
    } else {
      semantic(example.definition, example.value);
      positive++;
    }
  }
}
const example = (id) => fixtures.examples.find((e) => e.id === id).value;
// Independent binary enumeration, including repeated visits and first-hit absorption.
for (let depth = 0; depth <= 12; depth++) {
  let successes = 0n;
  for (let path = 0; path < 2 ** depth; path++) {
    let hit = false;
    for (let step = 0; step < depth; step++)
      hit ||= Boolean(path & (1 << step));
    if (hit) successes++;
  }
  assert.equal(successes, (1n << BigInt(depth)) - 1n);
}
for (const p of fixtures.oracle.observations) {
  point(p);
  const d = 1n << BigInt(p.attempts);
  assert(eq(fraction(p.lower), [d - 1n, d]));
}
const phases = graph(example("same-state-different-phase"));
assert.equal(
  phases.executions.get("x0").stateId,
  phases.executions.get("x2").stateId,
);
assert.notEqual(
  phases.executions.get("x0").phase,
  phases.executions.get("x2").phase,
);
const paged = graph(example("graph-page2"), graph(example("graph-page1")));
assert.deepEqual(paged, phases);
assert.deepEqual(
  graph(example("graph-page2"), paged),
  paged,
  "Repeated page must be idempotent",
);
const parent = example("recovery-parent"),
  recovery = example("conditional-recovery");
assert.equal(recovery.recovery.parentJobId, parent.jobId);
assert.equal(recovery.recovery.parentRevision, parent.revision);
assert(
  parent.graph.executions.some(
    (x) => x.id === recovery.recovery.failureExecutionId,
  ),
);
assert(
  parent.graph.nodes.some((x) => x.id === recovery.recovery.checkpointStateId),
);
assert.equal(parent.recommendations[0].points[0].lower.numerator, "0");
assert.equal(recovery.recovery.includedInMain, false);
let revision = fixtures.lifecycle.initialRevision;
const seen = new Map();
for (const event of fixtures.lifecycle.events) {
  const outcome = seen.has(event.commandId)
    ? "REPLAY"
    : event.expectedRevision !== revision
      ? "REVISION_CONFLICT"
      : "ACCEPTED";
  if (outcome === "ACCEPTED") {
    revision++;
    seen.set(event.commandId, revision);
  }
  assert.equal(outcome, event.result);
  assert.equal(revision, event.revision);
}
// Production source fixture: independently verify source bytes, IDs and the declared-model oracle.
assert.equal(solar.synthetic, false);
assert.equal(
  solar.verification,
  "SOURCE_CONTRACT_ORACLE_ONLY_NOT_PATH_SEARCH_RUNTIME_VERIFIED",
);
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
for (const source of solar.sources)
  assert.equal(
    hash(readFileSync(new URL(source.path, resources))),
    source.sha256,
    source.path,
  );
const catalog = read(resources, "catalog/solar-amulet/catalog.json");
const manifest = read(resources, "crafting/ruleset-v1.json");
const definitions = read(resources, "crafting/goalfilter/definitions-v1.json");
const files =
  "{" +
  Object.entries(manifest.files)
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([k, v]) => k + "=" + v)
    .join(", ") +
  "}";
assert.equal(
  solar.rulesetIdentity,
  manifest.rulesetVersion +
    "-" +
    hash(
      [
        manifest.rulesetVersion,
        manifest.gameSeason,
        manifest.gamePatch,
        manifest.engineRuleVersion,
        manifest.ledgerVersion,
        files,
      ].join("|"),
    ),
);
for (const source of solar.sources.filter(
  (s) => s.path !== "crafting/ruleset-v1.json",
))
  assert.equal(source.sha256, manifest.files[source.path]);
validate("item", solar.startItem);
validate("goal", solar.goalTemplate);
assert.equal(solar.startItem.baseItemId, catalog.base.id);
assert.equal(solar.startItem.snapshotId, catalog.metadata.snapshotId);
for (const instance of [
  ...solar.startItem.implicits,
  ...solar.startItem.explicits,
]) {
  const modifier = catalog.modifiers.find((m) => m.id === instance.modifierId);
  assert(modifier, "Missing source modifier");
  for (const s of modifier.stats)
    assert(instance.values[s.id] >= s.min && instance.values[s.id] <= s.max);
}
const target = "base_cold_damage_resistance_%";
assert(
  definitions.stats.some(
    (s) => s.sourceStatId === target && s.unit === "percent",
  ),
);
const pool = catalog.modifiers.filter(
  (m) =>
    m.layer === "EXPLICIT" &&
    m.weight > 0 &&
    m.requiredItemLevel <= solar.startItem.itemLevel,
);
assert(
  pool.every((m) => m.stats.length === 1),
  "Do not silently discard joint outcomes",
);
const total = pool.reduce((a, m) => a + BigInt(m.weight), 0n);
assert.equal(String(total), solar.modelOracle.eligibleTotalWeight);
assert.equal(pool.length, solar.modelOracle.eligibleModifierCount);
let probability = [0n, 1n];
for (const m of pool) {
  const s = m.stats[0];
  let hitCount = 0n;
  for (let value = s.min; value <= s.max; value++)
    if (s.id === target && value >= 20) hitCount++;
  probability = add(probability, [
    BigInt(m.weight) * hitCount,
    total * BigInt(s.max - s.min + 1),
  ]);
  const g = gcd(...probability);
  probability = probability.map((v) => v / g);
}
assert(eq(probability, fraction(solar.modelOracle.oneStepSuccess)));
assert.equal(solar.modelOracle.existingNumericRenewalSupport, false);
console.log(
  `PASS: ${positive} positive + ${negative} negative schema/semantic fixtures; binary oracle depths 0..12 and 100/300/500; graph/phase/pagination/recovery/revision; pinned Solar source oracle ${probability.join("/")}.`,
);
console.log(
  "No product endpoint, new build, database, server, or production renewal proof was executed.",
);
