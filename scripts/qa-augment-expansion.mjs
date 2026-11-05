import assert from "node:assert/strict";
import fs from "node:fs";
import crypto from "node:crypto";

// Public synthetic catalog items only. Never import a user's clipboard or saved films.
const origin = process.env.SOCKET_QA_ORIGIN ?? "http://127.0.0.1:19510";
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
      a.selectionEnabled ? 200 : 422,
    );
    if (!a.selectionEnabled) continue;
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
    { state, slotIndex: 0, augmentId: "Perfect_Desert_Rune" },
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
    { state, slotIndex, augmentId: "Perfect_Desert_Rune" },
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
    augmentId: "Perfect_Desert_Rune",
  },
  initial.rulesetIdentity,
  422,
);
await post(
  "augments/install",
  {
    state: { ...state, unexpected: "must not vanish" },
    slotIndex: 0,
    augmentId: "Perfect_Desert_Rune",
  },
  initial.rulesetIdentity,
  422,
);
// All limited ordinary Augments reject duplicate installation in a second slot.
let limitedChecks = 0;
let legacyLimitedChecks = 0;
for (const a of catalog.filter((entry) => entry.selectionEnabled && entry.limit)) {
  const cls = Object.keys(a.effects).find((cls) => roots.has(cls));
  assert(cls, a.id);
  const example = roots.get(cls);
  const first = await post("augments/install", { state: { ...example.state, augmentSockets: 2 }, slotIndex: 0, augmentId: a.id }, example.initial.rulesetIdentity);
  await post("augments/install", { state: first.state, slotIndex: 1, augmentId: a.id }, example.initial.rulesetIdentity, 422);
  const replaced = await post("augments/install", { state: first.state, slotIndex: 0, augmentId: a.id }, example.initial.rulesetIdentity);
  assert.deepEqual(replaced.state, first.state);
  if (!a.legacyLimitExempt)
    await post("augments/install", { state: { ...first.state, installedAugments: [...first.state.installedAugments, { slotIndex: 1, augmentId: a.id }] }, slotIndex: 0, augmentId: "Perfect_Desert_Rune" }, example.initial.rulesetIdentity, 422);
  else {
    const old = { ...first.state, installedAugments: [...first.state.installedAugments, { slotIndex: 1, augmentId: a.id }] };
    const ordinary = await post("apply", { state: old, action: "TRANSMUTATION", activeOmens: [] }, example.initial.rulesetIdentity);
    assert.deepEqual(ordinary.state.installedAugments, old.installedAugments);
    await post("augments/install", { state: old, slotIndex: 0, augmentId: "Perfect_Desert_Rune" }, example.initial.rulesetIdentity);
    legacyLimitedChecks++;
  }
  limitedChecks++;
}
for (const [firstId, secondId] of [["Tecrods_Gaze", "Kurgals_Gaze"], ["Legacy_of_Candlemaker", "Legacy_of_Deathblow"]]) {
  const first = await post("augments/install", { state: { ...root, augmentSockets: 2 }, slotIndex: 0, augmentId: firstId }, initial.rulesetIdentity);
  await post("augments/install", { state: first.state, slotIndex: 1, augmentId: secondId }, initial.rulesetIdentity, 422);
  await post("augments/install", { state: first.state, slotIndex: 0, augmentId: secondId }, initial.rulesetIdentity);
}
const legacy = { ...root, augmentSockets: 2, installedAugments: [{ slotIndex: 0, augmentId: "Desert_Rune" }] };
const preserved = await post("augments/install", { state: legacy, slotIndex: 1, augmentId: "Snake_Idol" }, initial.rulesetIdentity);
assert.equal(preserved.state.installedAugments[0].augmentId, "Desert_Rune");
for (const augmentId of ["Cadigans_Epiphany", "Serles_Triumph", "Fox_Idol", "Owl_Idol", "Masterwork_Rune", "DNT-Unused_Rune_of_Chance"])
  await post("augments/install", { state: { ...root, augmentSockets: 2 }, slotIndex: 0, augmentId }, initial.rulesetIdentity, 422);
// Verify the HTTP path too: URL-encoded source IDs must not produce broken icons.
const icons = JSON.parse(fs.readFileSync("frontend/public/assets/augments/sources.json", "utf8"));
let assetChecks = 0;
for (const entry of catalog) {
  const response = await fetch((process.env.SOCKET_QA_UI ?? "http://127.0.0.1:19511") + entry.image);
  assert.equal(response.status, 200, entry.image);
  assert.equal(crypto.createHash("sha256").update(Buffer.from(await response.arrayBuffer())).digest("hex"), icons[entry.id].sha256, entry.id);
  assetChecks++;
}
const report = {
  assetChecks,
  legacyLimitedChecks,
  limitedChecks,
  registeredBases:
    Object.keys(bases).length + Object.keys(policies.legacy).length,
  supportedBases: supported,
  augments: catalog.length,
  combinations,
  calls,
  passed: true,
};
fs.mkdirSync("docs/verification/augment-expansion-20261105", { recursive: true });
fs.writeFileSync(
  "docs/verification/augment-expansion-20261105/api.json",
  JSON.stringify(report, null, 2) + "\n",
);
console.log(report);
