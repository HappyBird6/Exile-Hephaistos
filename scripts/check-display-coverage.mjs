import { localeCodes } from "./public-contracts.mjs";
// Completeness is based on active catalog identities, not matching UI key counts.
import fs from "./evidence-files.cjs";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
process.chdir(
  process.env.HEPHAISTOS_TRANSLATION_ROOT ??
    fileURLToPath(new URL("..", import.meta.url)),
);
const templateBytes = fs.readFileSync(
  "frontend/src/shared/i18n/modifierTemplates.json",
);
const runtimeSource = fs.readFileSync(
  "backend/src/main/java/com/poe2craft/shared/item/ModifierTextResources.java",
  "utf8",
);
const runtimeHash = runtimeSource.match(
  /TEMPLATE_SHA256\s*=\s*"([a-f0-9]{64})"/,
);
assert(runtimeHash, "Backend runtime display template SHA-256 pin is missing");
assert.equal(
  createHash("sha256").update(templateBytes).digest("hex"),
  runtimeHash[1],
  "Display templates changed without updating the backend startup SHA-256 pin",
);
const locales = localeCodes;
const addedLocales = ["ru", "th"];
const baseDisplay = JSON.parse(
  fs.readFileSync("frontend/src/shared/i18n/baseDisplay.json", "utf8"),
);
const augmentDisplay = JSON.parse(
  fs.readFileSync("frontend/src/shared/i18n/augmentDisplay.json", "utf8"),
);
const addedSources = JSON.parse(
  fs.readFileSync(
    "docs/evidence/0.5.5/i18n/sources/additional-locales.json",
    "utf8",
  ),
);
const registry = JSON.parse(
  fs.readFileSync(
    "backend/src/main/resources/crafting/registry-v2.json",
    "utf8",
  ),
);
const terms = JSON.parse(
  fs.readFileSync("frontend/src/shared/i18n/gameTerms.json", "utf8"),
);
const mods = JSON.parse(
  fs.readFileSync("frontend/src/shared/i18n/modifierTemplates.json", "utf8"),
);
assert.deepEqual(
  JSON.parse(
    fs.readFileSync(
      "backend/src/main/resources/catalog/augment-crafting.json",
      "utf8",
    ),
  ),
  JSON.parse(
    fs.readFileSync(
      "frontend/src/features/crafting/augmentCrafting.json",
      "utf8",
    ),
  ),
);
assert.deepEqual(
  JSON.parse(
    fs.readFileSync(
      "backend/src/main/resources/catalog/augment-effect-templates.json",
      "utf8",
    ),
  ),
  JSON.parse(
    fs.readFileSync(
      "frontend/src/features/crafting/augmentEffectTemplates.json",
      "utf8",
    ),
  ),
);
const results = { locales: {}, catalogDefinitions: 0, compoundBindings: 0 };
const defs = {};
for (const dir of fs.readdirSync("backend/src/main/resources/catalog")) {
  const root = "backend/src/main/resources/catalog/" + dir;
  if (!fs.statSync(root).isDirectory()) continue;
  for (const f of fs
    .readdirSync(root)
    .filter((f) => f === "catalog.json" || f.endsWith(".catalog.json"))) {
    const c = JSON.parse(fs.readFileSync(root + "/" + f, "utf8"));
    if (c.base)
      for (const l of locales)
        assert(
          Object.values(terms[l]).some((t) => t.itemKey === c.base.id),
          `${l}:base:${c.base.id}`,
        );
    for (const d of c.modifiers ?? []) defs[d.id] = d;
  }
}
for (const d of JSON.parse(
  fs.readFileSync(
    "backend/src/main/resources/catalog/augment-crafting.json",
    "utf8",
  ),
).modifiers)
  defs[d.id] = d;
for (const [baseKey, base] of Object.entries(
  JSON.parse(
    fs.readFileSync(
      "backend/src/main/resources/catalog/top-bases.json",
      "utf8",
    ),
  ),
)) {
  for (const l of locales) {
    assert.equal(
      terms[l][base.slug]?.itemKey,
      base.id,
      `${l}:top-base:${base.id}`,
    );
    if (addedLocales.includes(l)) {
      const source = addedSources.baseSources[baseKey][l];
      const display = baseDisplay[baseKey][l];
      assert.equal(
        source.baseId,
        base.id,
        `${l}:source base identity:${base.id}`,
      );
      assert.equal(
        display.requirements,
        source.requirements,
        `${l}:source requirements:${base.id}`,
      );
      assert.equal(
        Boolean(display.requirements),
        Boolean(base.requirements.en),
        `${l}:requirement presence:${base.id}`,
      );
    } else if (
      [
        "Metadata/Items/Amulets/FourAmulet1",
        "Metadata/Items/Amulets/FourAmulet2",
        "Metadata/Items/Belts/FourBelt2",
      ].includes(base.id)
    ) {
      assert.equal(
        base.requiredLevel,
        0,
        `${l}:source has no character requirement:${base.id}`,
      );
      assert.equal(
        base.requirements[l],
        "",
        `${l}:no invented requirement:${base.id}`,
      );
      const sourceLocale = {
        en: "us",
        ko: "kr",
        "zh-CN": "cn",
        "zh-TW": "tw",
        ja: "jp",
        es: "sp",
      }[l];
      const captured = JSON.parse(
        fs.readFileSync(
          `evidence:${base.id === "Metadata/Items/Belts/FourBelt2" ? "belts-source" : "amulets-source"}/${base.slug}.${sourceLocale}.json`,
          "utf8",
        ),
      );
      const source =
        base.id === "Metadata/Items/Belts/FourBelt2"
          ? captured.variants[0]
          : captured;
      assert.equal(source.fields.Type, base.id);
      assert.equal(
        source.requirements,
        base.requirements[l],
        `${l}:exact source absence:${base.id}`,
      );
    } else if (base.id === "Metadata/Items/Quivers/FourQuiver1") {
      assert.equal(base.family, "quivers");
      assert.equal(
        base.requiredLevel,
        0,
        `${l}:no character requirement:${base.id}`,
      );
      assert.equal(
        base.requirements[l],
        "",
        `${l}:no invented requirement:${base.id}`,
      );
      const sourceLocale = {
        en: "us",
        ko: "kr",
        "zh-CN": "cn",
        "zh-TW": "tw",
        ja: "jp",
        es: "sp",
      }[l];
      const captured = JSON.parse(
        fs.readFileSync(
          `evidence:quivers-source/${base.slug}.${sourceLocale}.json`,
          "utf8",
        ),
      ).variants[0];
      assert.equal(captured.fields.Type, base.id);
      assert.equal(
        captured.requirements,
        "",
        `${l}:exact source absence:${base.id}`,
      );
    } else if (
      base.requiredLevel === 0 &&
      base.sourceUrl &&
      fs.existsSync(
        `evidence:base-audit/${base.sourceUrl.split("/").at(-1)}.${{ en: "us", ko: "kr", "zh-CN": "cn", "zh-TW": "tw", ja: "jp", es: "sp" }[l]}.json`,
      )
    ) {
      const source = JSON.parse(
        fs.readFileSync(
          `evidence:base-audit/${base.sourceUrl.split("/").at(-1)}.${{ en: "us", ko: "kr", "zh-CN": "cn", "zh-TW": "tw", ja: "jp", es: "sp" }[l]}.json`,
          "utf8",
        ),
      ).variants.find((v) => v.fields.Type === base.id);
      assert(source, `${l}:exact source base identity:${base.id}`);
      assert.equal(
        source.requirements,
        "",
        `${l}:source has no character requirement:${base.id}`,
      );
      assert.equal(
        base.requirements[l],
        "",
        `${l}:no invented requirement:${base.id}`,
      );
    } else
      assert(base.requirements[l], `${l}:top-base-requirements:${base.id}`);
  }
}
const equipmentCurrencyPath =
  "backend/src/main/resources/catalog/equipment-currencies.json";
const equipmentCurrencies = JSON.parse(
  fs.readFileSync(equipmentCurrencyPath, "utf8"),
);
assert.deepEqual(
  equipmentCurrencies,
  JSON.parse(
    fs.readFileSync(
      "frontend/src/features/crafting/equipmentCurrencies.json",
      "utf8",
    ),
  ),
  "Equipment currency registry mirror differs",
);
for (const definition of equipmentCurrencies.definitions)
  defs[definition.id] = definition;
results.catalogDefinitions = Object.keys(defs).length;
const reveal = JSON.parse(
  fs.readFileSync(
    "backend/src/main/resources/catalog/manual-reveal.json",
    "utf8",
  ),
);
for (const definition of reveal.definitions) defs[definition.id] = definition;
results.manualRevealDefinitions = reveal.definitions.length;
for (const [id, d] of Object.entries(defs)) {
  const b = mods.definitions[id];
  assert(b, `Missing active catalog binding: ${id}`);
  assert.equal(b.englishText, d.text, `Stale display source: ${id}`);
  assert.deepEqual(b.stats, d.stats ?? [], `Changed stat identity: ${id}`);
  for (const l of locales) {
    const t = mods.templates[l][b.template];
    assert(t?.template, `Missing active modifier locale: ${l}:${id}`);
    const placeholders = [...t.template.matchAll(/\{v(\d+)\}/g)].map(
      (m) => +m[1],
    );
    assert(
      placeholders.every((i) => i < b.values.length),
      `Unbound display value: ${l}:${id}`,
    );
    assert.deepEqual(
      [...new Set(placeholders)].sort(),
      [
        ...new Set(
          [
            ...mods.templates.en[b.template].template.matchAll(/\{v(\d+)\}/g),
          ].map((m) => +m[1]),
        ),
      ].sort(),
      `Changed placeholders: ${l}:${id}`,
    );
  }
  if (
    b.valueStats &&
    d.stats.length > 1 &&
    d.stats.some((s) => s.min !== s.max)
  )
    results.compoundBindings++;
}
for (const l of locales) {
  for (const [id, term] of Object.entries(terms[l]))
    assert(
      !(term.lines ?? []).some((line) => /^&nbsp;?$/.test(line)),
      `${l}:HTML whitespace artifact:${id}`,
    );
  const active = registry.entries.filter((e) => e.serviceScope !== "DEFERRED");
  const missing = active.filter((e) => !terms[l][e.id]?.name);
  // Deferred identities are explicit documented exceptions; newly active entries fail.
  assert.equal(
    missing.length,
    0,
    `${l}:missing active names:${missing.map((e) => e.id)}`,
  );
  const exceptions = registry.entries.filter(
    (e) => e.serviceScope === "DEFERRED" && !terms[l][e.id]?.name,
  );
  const liquid = registry.entries.filter(
    (e) => e.category === "LIQUID_EMOTION" && e.id !== "Liquid_Verisium",
  );
  assert.equal(liquid.length, 26);
  for (const e of liquid)
    assert(terms[l][e.id]?.lines.length, `${l}:Liquid tooltip:${e.id}`);
  results.locales[l] = {
    names: registry.entries.filter((e) => terms[l][e.id]?.name).length,
    total: registry.entries.length,
    exceptions: exceptions.map((e) => e.id),
    liquidNamesAndDescriptions: liquid.length,
  };
}

for (const entry of JSON.parse(
  fs.readFileSync("frontend/src/features/crafting/augments.json", "utf8"),
)) {
  for (const locale of addedLocales) {
    const display = augmentDisplay[entry.id]?.[locale];
    assert(display?.name, `${locale}:augment name:${entry.id}`);
    assert(
      !display.properties.some((text) =>
        /^(Requires|Limited to|Stack Size):/.test(text),
      ),
      `${locale}:untranslated augment property:${entry.id}`,
    );
    assert.deepEqual(
      Object.keys(display.effects).sort(),
      Object.keys(entry.effects).sort(),
      `${locale}:augment equipment coverage:${entry.id}`,
    );
    for (const [equipment, text] of Object.entries(display.effects)) {
      assert(text, `${locale}:augment effect:${entry.id}:${equipment}`);
      const source = addedSources.augmentEffects.find(
        (source) =>
          source.locale === locale &&
          source.englishText === entry.effects[equipment].en &&
          source.localizedText === text,
      );
      assert(
        source,
        `${locale}:source-correlated augment effect:${entry.id}:${equipment}`,
      );
      assert.deepEqual(
        source.numericSpans,
        source.englishNumericSpans,
        `${locale}:augment numeric spans:${entry.id}:${equipment}`,
      );
    }
  }
}

console.log(JSON.stringify(results, null, 2));
