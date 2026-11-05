import assert from "node:assert/strict";
import fs from "node:fs";

// Public synthetic catalog items only. Never import a user's clipboard or saved films.
const origin = process.env.SOCKET_QA_ORIGIN ?? "http://127.0.0.1:19010";
const catalog = JSON.parse(
  fs.readFileSync("frontend/src/features/crafting/augments.json", "utf8"),
);
const bases = JSON.parse(
  fs.readFileSync("frontend/src/features/crafting/topBases.json", "utf8"),
);
const policies = JSON.parse(
  fs.readFileSync("frontend/src/features/crafting/basePolicies.json", "utf8"),
);
let calls = 0;
async function get(base) {
  const r = await fetch(
    `${origin}/api/v1/crafting/workbench/initial?base=${base}&itemLevel=82`,
    { signal: AbortSignal.timeout(15000) },
  ).catch((e) => {
    throw new Error("Initial request failed: " + base, { cause: e });
  });
  assert.equal(r.status, 200);
  calls++;
  const initial = await r.json();
  return {
    initial,
    state: {
      snapshotId: initial.state.snapshotId,
      baseItemId: initial.state.baseItemId,
      itemLevel: 82,
      rarity: initial.state.rarity,
      implicits: initial.state.implicits,
      explicits: [],
      conditions: [],
      augmentSockets: initial.augmentSockets,
      ...(initial.equipmentQuality == null
        ? {}
        : { equipmentQuality: initial.equipmentQuality }),
    },
  };
}
async function post(path, data, identity, status = 200) {
  const r = await fetch(`${origin}/api/v1/crafting/workbench/${path}`, {
    method: "POST",
    signal: AbortSignal.timeout(15000),
    headers: {
      "Content-Type": "application/json",
      "X-Crafting-Ruleset": identity,
    },
    body: JSON.stringify(data),
  });
  const result = await r.json();
  calls++;
  assert.equal(r.status, status, `${path}: ${JSON.stringify(result)}`);
  if (status === 200)
    assert.equal(r.headers.get("X-Crafting-Ruleset"), identity);
  return result;
}
let supported = 0;
for (const [key, base] of Object.entries({ ...bases, ...policies.legacy })) {
  const { initial, state: root } = await get(key);
  const policy = policies.legacy[key] ?? {
    ...policies.families[base.family],
    ...policies.baseOverrides[key],
  };
  const cap = policy.socketExecutionMaximum;
  assert.equal(initial.augmentSockets, cap == null ? null : 0, key);
  if (cap == null) continue;
  supported++;
  let state = root;
  for (let count = 1; count <= cap; count++) {
    const result = await post(
      "apply",
      { state, action: "ARTIFICER", activeOmens: [] },
      initial.rulesetIdentity,
    );
    assert.equal(result.applied, true, key);
    assert.equal(result.state.augmentSockets, count, key);
    state = result.state;
  }
  const full = await post(
    "apply",
    { state, action: "ARTIFICER", activeOmens: [] },
    initial.rulesetIdentity,
  );
  assert.equal(full.applied, false, key);
  assert.deepEqual(full.state, state);
}
const { initial, state: root } = await get("stocky");
let state = { ...root, augmentSockets: 2 };
let combinations = 0;
const representatives = new Map();
for (const [key, b] of Object.entries(bases)) {
  const policy = {
    ...policies.families[b.family],
    ...policies.baseOverrides[key],
  };
  if (
    policy.socketExecutionMaximum != null &&
    !representatives.has(policy.itemClass)
  )
    representatives.set(policy.itemClass, key);
}
const roots = new Map();
for (const [cls, key] of representatives) roots.set(cls, await get(key));
for (const a of catalog)
  for (const cls of Object.keys(a.effects)) {
    const example = roots.get(cls);
    if (!example) continue;
    const result = await post(
      "augments/install",
      {
        state: { ...example.state, augmentSockets: 1 },
        slotIndex: 0,
        augmentId: a.id,
      },
      example.initial.rulesetIdentity,
    );
    assert.equal(result.rulesetIdentity, example.initial.rulesetIdentity);
    assert.deepEqual(
      result.state.installedAugments,
      [{ slotIndex: 0, augmentId: a.id }],
      a.id + ":" + cls,
    );
    combinations++;
  }
state = (
  await post(
    "augments/install",
    { state, slotIndex: 0, augmentId: "Desert_Rune" },
    initial.rulesetIdentity,
  )
).state;
state = (
  await post(
    "augments/install",
    { state, slotIndex: 1, augmentId: "Perfect_Glacial_Rune" },
    initial.rulesetIdentity,
  )
).state;
const installed = state.installedAugments;
for (const action of [
  "TRANSMUTATION",
  "AUGMENTATION",
  "REGAL",
  "EXALTED",
  "DIVINE",
  "CHAOS",
  "ANNULMENT",
]) {
  const result = await post(
    "apply",
    { state, action, activeOmens: [] },
    initial.rulesetIdentity,
  );
  assert.deepEqual(result.state.installedAugments, installed, action);
  state = result.state;
}
for (const slotIndex of [-1, 2, 0.5, null])
  await post(
    "augments/install",
    { state, slotIndex, augmentId: "Desert_Rune" },
    initial.rulesetIdentity,
    422,
  );
await post(
  "augments/install",
  { state, slotIndex: 0, augmentId: "Invented_Rune" },
  initial.rulesetIdentity,
  422,
);
await post(
  "augments/install",
  {
    state: { ...state, installedAugments: [...installed, installed[0]] },
    slotIndex: 0,
    augmentId: "Desert_Rune",
  },
  initial.rulesetIdentity,
  422,
);
await post(
  "augments/install",
  {
    state: { ...state, unexpected: "must not vanish" },
    slotIndex: 0,
    augmentId: "Desert_Rune",
  },
  initial.rulesetIdentity,
  422,
);
const report = {
  registeredBases:
    Object.keys(bases).length + Object.keys(policies.legacy).length,
  supportedBases: supported,
  augments: catalog.length,
  combinations,
  calls,
  passed: true,
};
fs.mkdirSync("docs/verification/socket-augments-20261105", { recursive: true });
fs.writeFileSync(
  "docs/verification/socket-augments-20261105/api.json",
  JSON.stringify(report, null, 2) + "\n",
);
console.log(report);
