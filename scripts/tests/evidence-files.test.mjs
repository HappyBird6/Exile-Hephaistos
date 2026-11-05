import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import evidence from "../evidence-files.cjs";

test("one tool collection reads localized pages and option details from different features", () => {
  const page = "evidence:maces-source/One_Hand_Maces.us.json";
  const detail = "evidence:maces-source/details/One_Hand_Maces/normal-0.json";
  assert.match(
    evidence.resolveEvidencePath(page),
    /0\.5\.5\/i18n\/sources\/en\//,
  );
  assert.match(
    evidence.resolveEvidencePath(detail),
    /0\.5\.5\/catalog\/modifiers\//,
  );
  const rows = JSON.parse(evidence.readFileSync(page, "utf8")).data.normal;
  const option = JSON.parse(evidence.readFileSync(detail, "utf8"));
  assert.equal(option.row.Name, rows[0].Name);
  assert.equal(option.row.DropChance, rows[0].DropChance);
  assert(
    evidence
      .readdirSync("evidence:maces-source/details")
      .includes("One_Hand_Maces"),
  );
  assert(
    evidence
      .readdirSync("evidence:maces-source", { withFileTypes: true })
      .some((d) => d.name === "details" && d.isDirectory()),
  );
  assert.equal(
    evidence.existsSync("evidence:missing-input/absent.json"),
    false,
  );
});

test("copying indexed tool inputs keeps physical feature paths and leaves the original index unchanged", () => {
  const sourceIndex = path.resolve("docs/evidence/0.5.5/file-index.json");
  const original = fs.readFileSync(sourceIndex);
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "evidence-inputs-"));
  try {
    evidence.cpSync(
      "evidence:base-registry-refactor",
      root + "/evidence:base-registry-refactor",
      { recursive: true },
    );
    const input = "evidence:base-registry-refactor/importer-baseline.json";
    assert.deepEqual(
      evidence.readFileSync(root + "/" + input),
      evidence.readFileSync(input),
    );
    assert(
      fs.existsSync(
        path.join(root, "docs/evidence/0.5.5/tooling/catalog-import"),
      ),
    );
    assert.equal(
      fs.existsSync(path.join(root, "evidence:base-registry-refactor")),
      false,
    );
    assert.deepEqual(fs.readFileSync(sourceIndex), original);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test("removing a logical collection cannot recursively delete a shared feature directory", () => {
  assert.throws(
    () => evidence.rmSync("evidence:maces-source", { recursive: true }),
    /individual indexed files/,
  );
  assert(evidence.existsSync("evidence:maces-source/One_Hand_Maces.us.json"));
});

test("every indexed input exists below the version root with a date-free feature path", () => {
  const root = path.resolve("docs/evidence/0.5.5");
  const index = JSON.parse(
    fs.readFileSync(path.join(root, "file-index.json"), "utf8"),
  );
  assert.equal(index.version, "0.5.5");
  const features = new Set([
    "i18n",
    "catalog",
    "crafting",
    "search",
    "workbench",
    "assets",
    "tooling",
    "release",
  ]);
  for (const [id, file] of Object.entries(index.files)) {
    const full = path.resolve(root, file);
    assert(!path.relative(root, full).startsWith(".."), id);
    assert(features.has(file.split("/")[0]), id);
    assert(!/20\d{2}[-_]?\d{2}[-_]?\d{2}/.test(file), id);
    assert(fs.statSync(full).isFile(), id);
    assert.equal(evidence.resolveEvidencePath("evidence:" + id), full);
  }
});

test("new validation output goes to its feature and never edits the source index", () => {
  const original = fs.readFileSync("docs/evidence/0.5.5/file-index.json");
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "evidence-check-"));
  const output = root + "/evidence:general/workbench-history-validation.json";
  try {
    evidence.writeFileSync(output, '{"passed":true}\n');
    assert.match(
      evidence.resolveEvidencePath(output),
      /0\.5\.5\/workbench\/history\/checks\//,
    );
    assert.deepEqual(JSON.parse(evidence.readFileSync(output, "utf8")), {
      passed: true,
    });
    assert(evidence.existsSync(output));
    assert.deepEqual(
      fs.readFileSync("docs/evidence/0.5.5/file-index.json"),
      original,
    );
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
