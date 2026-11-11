import { localePaths, readJSON } from "./public-contracts.mjs";
import { proposalDirectory } from "./source-proposals.mjs";
const proposal = proposalDirectory(process.argv);
// Bounded display-only source collection. Never used by crafting or ingestion.
import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";

const root = process.cwd();
const output = path.join(proposal, "gameTerms.json");
const evidence = path.join(proposal, "i18n-sources.json");
const catalog = JSON.parse(
  fs.readFileSync(
    "frontend/src/features/crafting/materialTooltips.json",
    "utf8",
  ),
);
// Collection scope follows reviewed runtime identities; no historical exclusion list.
const registry = readJSON(
  "backend/src/main/resources/crafting/registry-v2.json",
);
const allowed = new Set(
  registry.entries.filter((row) => catalog[row.id]).map((row) => row.id),
);
const bases = Object.fromEntries(
  [
    ...Object.values(
      readJSON("backend/src/main/resources/catalog/base-policies.json").legacy,
    ),
    ...Object.values(
      readJSON("backend/src/main/resources/catalog/top-bases.json"),
    ),
  ].map((row) => [row.slug, row.id]),
);
const locales = localePaths;
const clean = (text) =>
  text
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, '"')
    .replace(/&#0*39;|&apos;/g, "'")
    .replace(/&nbsp;/g, " ")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&#x([a-f\d]+);/gi, (_, n) =>
      String.fromCodePoint(parseInt(n, 16)),
    )
    .trim();
const manifest = {
  retrievedAt: new Date().toISOString(),
  localePaths: locales,
  pages: [],
  coverage: {},
  gaps: [],
};
const supplement = process.argv.includes("--supplement");
const dictionaries = supplement
  ? JSON.parse(
      fs.readFileSync(
        path.join(root, "frontend/src/shared/i18n/gameTerms.json"),
        "utf8",
      ),
    )
  : {};
if (supplement && fs.existsSync(evidence)) {
  const previous = JSON.parse(fs.readFileSync(evidence, "utf8"));
  manifest.pages = previous.pages;
  manifest.gaps = previous.gaps;
}
async function page(url) {
  const response = await fetch(url);
  const html = await response.text();
  manifest.pages.push({
    url,
    status: response.status,
    sha256: createHash("sha256").update(html).digest("hex"),
  });
  if (!response.ok) throw new Error(`${response.status}: ${url}`);
  return html;
}
for (const [locale, prefix] of Object.entries(locales)) {
  const entries = supplement ? { ...dictionaries[locale] } : {};
  for (const category of supplement
    ? ["Catalysts"]
    : ["Currency", "Essence", "Omen", "Catalysts"]) {
    const url = `https://poe2db.tw/${prefix}/${category}`;
    try {
      const html = await page(url);
      for (const row of html.split('<div class="col">')) {
        const item = row.match(
          /<div class="flex-grow-1 ms-2"><a[^>]+href="([^"#]+)"[^>]*>(.*?)<\/a>/s,
        );
        if (!item || !allowed.has(item[1])) continue;
        const id = item[1];
        const name = clean(item[2]);
        const itemKey =
          item[2].match(/alt="([^"]+)"/)?.[1] ??
          row.slice(0, item.index).match(/alt="([^"]+)"/)?.[1];
        if (!name || !itemKey) continue;
        const english = dictionaries.en?.[id];
        if (locale !== "en" && (!english || english.itemKey !== itemKey)) {
          manifest.gaps.push({
            locale,
            id,
            reason:
              "Source asset identifier differs or English counterpart missing",
          });
          continue;
        }
        // Keep only display description blocks belonging to this item row.
        const body = row
          .slice(item.index + item[0].length)
          .split('<div class="flex-shrink-0">')[0];
        const lines = [
          ...body.matchAll(
            /<div class="(?:explicitMod|descrText)">(.*?)<\/div>/gs,
          ),
        ]
          .map((m) => clean(m[1]))
          .filter(Boolean);
        entries[id] = {
          name,
          lines,
          itemKey,
          sourceUrl: `https://poe2db.tw/${prefix}/${id}`,
        };
      }
    } catch (error) {
      manifest.gaps.push({ locale, category, reason: error.message });
    }
    console.log(
      `${locale}: ${category} (${Object.keys(entries).length} verified terms)`,
    );
  }
  for (const [slug, expectedMetadata] of supplement
    ? []
    : Object.entries(bases)) {
    const url = `https://poe2db.tw/${prefix}/${slug}`;
    try {
      const html = await page(url);
      const title = clean(html.match(/<title>(.*?) - /s)?.[1] ?? "");
      const metadata = html.match(/Metadata\/Items\/[A-Za-z\d/_]+/)?.[0];
      const english = dictionaries.en?.[slug];
      if (
        !title ||
        metadata !== expectedMetadata ||
        (locale !== "en" && english?.itemKey !== metadata)
      ) {
        manifest.gaps.push({
          locale,
          id: slug,
          reason: "Missing or mismatched metadata identity",
        });
        continue;
      }
      entries[slug] = {
        name: title,
        lines: [],
        itemKey: metadata,
        sourceUrl: url,
      };
    } catch (error) {
      manifest.gaps.push({ locale, id: slug, reason: error.message });
    }
  }
  for (const id of Object.keys(entries))
    if (!allowed.has(id) && !bases[id]) delete entries[id];
  dictionaries[locale] = entries;
  manifest.coverage[locale] = {
    verified: Object.keys(entries).length,
    missingInventory: [...allowed].filter((id) => !entries[id]),
    missingBases: Object.keys(bases).filter((id) => !entries[id]),
  };
}
// English baseline descriptions stay in the existing catalog; no schema or roll text is rewritten.
for (const [id, entry] of Object.entries(dictionaries.en))
  if (catalog[id])
    entry.lines = catalog[id].lines.filter(
      (line) => !line.startsWith("Stack Size:"),
    );
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.mkdirSync(path.dirname(evidence), { recursive: true });
fs.writeFileSync(output, JSON.stringify(dictionaries, null, 2) + "\n");
fs.writeFileSync(evidence, JSON.stringify(manifest, null, 2) + "\n");
console.log(JSON.stringify(manifest.coverage));
