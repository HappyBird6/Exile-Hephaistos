// Display-only localization. Reads existing catalogs but never rewrites them.
import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";

const root = "backend/src/main/resources/catalog";
const cache = process.argv[2];
if (!cache) throw new Error("Pass an owned source-evidence cache directory");
fs.mkdirSync(cache, { recursive: true });
const locales = {
  en: "us",
  ko: "kr",
  "zh-CN": "cn",
  "zh-TW": "tw",
  ja: "jp",
  es: "sp",
};
const pages = {
  "solar-amulet": "Amulets",
  "stocky-mitts": "Gloves_str",
  "crude-bow": "Bows",
  "attuned-wand": "Wands",
  "rusted-cuirass": "Body_Armours_str",
  "rattling-sceptre": "Sceptres",
  "rawhide-belt": "Belts",
  "iron-ring": "Rings",
  "rusted-greathelm": "Helmets_str",
};
const clean = (html) =>
  html
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&nbsp;/g, " ")
    .replace(/&quot;/g, '"')
    .replace(/&#0*39;|&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&#x([a-f\d]+);/gi, (_, n) =>
      String.fromCodePoint(parseInt(n, 16)),
    )
    .trim();
function dataFrom(html) {
  const offset = html.indexOf("new ModsView(");
  if (offset < 0) throw new Error("Modifier view JSON not found");
  const start = html.indexOf("{", offset);
  let depth = 0,
    quoted = false,
    escaped = false;
  for (let i = start; i < html.length; i++) {
    const ch = html[i];
    if (quoted) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === '"') quoted = false;
    } else if (ch === '"') quoted = true;
    else if (ch === "{") depth++;
    else if (ch === "}" && --depth === 0)
      return JSON.parse(html.slice(start, i + 1));
  }
  throw new Error("Unterminated modifier view JSON");
}
function spans(html) {
  // Nested ndash spans need balanced span extraction, not a flat regex.
  const parts = [],
    expression = /<span\b[^>]*>|<\/span>/g;
  let start,
    depth = 0;
  for (const m of html.matchAll(expression)) {
    if (start !== undefined) {
      if (m[0].startsWith("</")) depth--;
      else depth++;
      if (!depth) {
        parts.push({
          start,
          end: m.index + m[0].length,
          value: clean(html.slice(start, m.index + m[0].length)),
        });
        start = undefined;
      }
    } else if (/class=['"]mod-value['"]/.test(m[0])) {
      start = m.index;
      depth = 1;
    }
  }
  return parts;
}
const normal = (text) => text.replace(/[?–—]/g, "-").replace(/\s+/g, "").trim();
function identity(row) {
  return JSON.stringify([
    String(row.ModGenerationTypeID),
    [...row.ModFamilyList].sort(),
    String(row.Level),
    [...(row.fossil_no ?? [])].sort(),
    [...(row.spawn_no ?? [])].sort(),
    spans(row.str).map((span) => normal(span.value)),
  ]);
}
function display(row) {
  const numbers = spans(row.str);
  let html = row.str;
  for (const [index, span] of [...numbers.entries()].reverse())
    html = html.slice(0, span.start) + `{v${index}}` + html.slice(span.end);
  return {
    text: clean(row.str),
    template: clean(html),
    spans: numbers.map((span) => span.value),
    name: clean(row.Name),
  };
}
const definitions = {},
  catalogGroups = {};
const retainedRows = new Map();
for (const base of Object.keys(pages)) {
  const modifiers = [];
  for (const filename of fs
    .readdirSync(path.join(root, base))
    .filter(
      (name) => name === "catalog.json" || name.endsWith(".catalog.json"),
    )) {
    const catalog = JSON.parse(
      fs.readFileSync(path.join(root, base, filename), "utf8"),
    );
    for (const definition of catalog.modifiers ?? []) {
      if (!definitions[definition.id]) modifiers.push(definition);
      definitions[definition.id] = definition;
    }
  }
  catalogGroups[base] = modifiers;
  for (const filename of fs
    .readdirSync(path.join(root, base))
    .filter((name) => name.endsWith(".raw.json"))) {
    const raw = JSON.parse(
      fs.readFileSync(path.join(root, base, filename), "utf8"),
    );
    function retain(record) {
      if (!record || typeof record !== "object") return;
      const hover = record.row?.hover ?? record.sourceRowUrl;
      if (hover)
        for (const reference of [
          record.url,
          record.detailSourceUrl,
          record.sourceUrl,
        ]) {
          if (reference)
            retainedRows.set(reference, { hover, code: record.code ?? null });
        }
      for (const value of Object.values(record))
        if (value && typeof value === "object") {
          if (Array.isArray(value)) value.forEach(retain);
          else retain(value);
        }
    }
    if (Array.isArray(raw)) raw.forEach(retain);
    else retain(raw);
  }
}
const result = {
  definitions: {},
  templates: Object.fromEntries(
    Object.keys(locales).map((locale) => [locale, {}]),
  ),
};
const evidence = {
  retrievedAt: new Date().toISOString(),
  pages: [],
  counts: {},
  gaps: {},
  method:
    "Retained source-code/hover proof or unique family/generation/level identity; unchanged English numeric text corroboration; unique family/generation/level/tag/numeric-span locale signature; never translated display-name matching",
};
const templateIds = new Map();
for (const [base, pageName] of Object.entries(pages)) {
  const rows = {};
  for (const [locale, prefix] of Object.entries(locales)) {
    const url = `https://poe2db.tw/${prefix}/${pageName}`;
    const target = path.join(cache, `${base}-${locale}.html`);
    let html;
    if (fs.existsSync(target)) html = fs.readFileSync(target, "utf8");
    else {
      const response = await fetch(url);
      if (!response.ok) throw new Error(`${response.status}: ${url}`);
      html = await response.text();
      fs.writeFileSync(target, html);
    }
    evidence.pages.push({
      url,
      sha256: createHash("sha256").update(html).digest("hex"),
    });
    const data = dataFrom(html);
    const candidates = Object.values(data)
      .filter(Array.isArray)
      .flat()
      .filter((row) => row?.ModFamilyList && row.str && row.hover);
    rows[locale] = [
      ...new Map(candidates.map((row) => [row.hover, row])).values(),
    ];
  }
  for (const definition of catalogGroups[base]) {
    const retained = retainedRows.get(definition.sourceUrl);
    let english = rows.en.find(
      (row) => row.hover === (retained?.hover ?? definition.sourceUrl),
    );
    if (!english) {
      const candidates = rows.en.filter(
        (row) =>
          JSON.stringify([...row.ModFamilyList].sort()) ===
            JSON.stringify([...definition.familyIds].sort()) &&
          String(row.ModGenerationTypeID) ===
            (definition.affixType === "PREFIX" ? "1" : "2") &&
          Number(row.Level) === definition.requiredItemLevel,
      );
      if (candidates.length === 1) english = candidates[0];
    }
    if (!english || normal(clean(english.str)) !== normal(definition.text))
      continue;
    const signature = identity(english);
    if (rows.en.filter((row) => identity(row) === signature).length !== 1)
      continue;
    const baseline = display(english);
    // Keep original API numeric values and full English text for stale-snapshot guards.
    const binding = {
      stats: definition.stats ?? [],
      englishText: definition.text,
      values: baseline.spans,
      template: undefined,
      sourceCode: retained?.code ?? null,
    };
    const candidateKey = JSON.stringify([
      baseline.template,
      baseline.spans,
      baseline.name,
    ]);
    let templateId = templateIds.get(candidateKey);
    if (!templateId) {
      templateId = `modifier.${createHash("sha256").update(candidateKey).digest("hex").slice(0, 16)}`;
      templateIds.set(candidateKey, templateId);
    }
    binding.template = templateId;
    result.definitions[definition.id] = binding;
    result.templates.en[templateId] = {
      template: baseline.template,
      name: baseline.name,
    };
    for (const locale of Object.keys(locales).filter(
      (locale) => locale !== "en",
    )) {
      const matches = rows[locale].filter((row) => identity(row) === signature);
      if (matches.length !== 1) continue;
      const local = display(matches[0]);
      const existing = result.templates[locale][templateId];
      if (existing && existing.template !== local.template) {
        delete result.templates[locale][templateId];
        continue;
      }
      result.templates[locale][templateId] = {
        template: local.template,
        name: local.name,
      };
    }
  }
  console.log(
    `${base}: ${catalogGroups[base].length} catalog definitions, ${Object.keys(result.definitions).length} verified bindings cumulative`,
  );
}
for (const locale of Object.keys(locales)) {
  const covered = Object.entries(result.definitions)
    .filter(([, binding]) => result.templates[locale][binding.template])
    .map(([id]) => id);
  const ordinary = Object.values(definitions).filter((d) => d.weight > 0);
  evidence.counts[locale] = {
    total: Object.keys(definitions).length,
    verified: covered.length,
    ordinaryTotal: ordinary.length,
    ordinaryVerified: ordinary.filter((d) => covered.includes(d.id)).length,
    templates: Object.keys(result.templates[locale]).length,
  };
  evidence.gaps[locale] = Object.keys(definitions).filter(
    (id) => !covered.includes(id),
  );
}
fs.writeFileSync(
  "frontend/src/shared/i18n/modifierTemplates.json",
  JSON.stringify(result, null, 2) + "\n",
);
fs.writeFileSync(
  "docs/evidence/i18n-modifier-sources-2026-10-04.json",
  JSON.stringify(evidence, null, 2) + "\n",
);
console.log(JSON.stringify(evidence.counts));
