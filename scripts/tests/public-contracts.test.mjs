import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { root } from "../public-contracts.mjs";
import { proposalDirectory } from "../source-proposals.mjs";
test("one policy change propagates to adapter, proxy, schema and documentation; stale artifacts fail", () => {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), "public-contract-"));
  try {
    const files = [
      "backend/src/main/resources/contracts/locales.json",
      "backend/src/main/resources/contracts/chaos-repeat.json",
      "backend/src/main/resources/crafting/workbench-definitions-v1.json",
      "backend/src/main/resources/crafting/automatic-currency-policy-v1.json",
      "backend/src/main/resources/crafting/goalfilter/definitions-v1.json",
      "backend/src/main/resources/catalog/top-bases.json",
      "backend/src/main/resources/catalog/base-policies.json",
      "backend/src/main/resources/catalog/augments.json",
      "contracts/workbench-definitions-v1.schema.json",
      "contracts/goal-definitions-v1.schema.json",
      "docs/current/support.md",
      "frontend/src/shared/contracts/public.ts",
      "infra/docker/chaos-repeat-limits.conf",
    ];
    for (const file of files) {
      fs.mkdirSync(path.dirname(path.join(temporary, file)), {
        recursive: true,
      });
      fs.copyFileSync(root + file, path.join(temporary, file));
    }
    const run = (flag) =>
      spawnSync(
        process.execPath,
        [root + "scripts/sync-public-contracts.mjs", flag],
        {
          env: { ...process.env, HEPHAISTOS_CONTRACT_ROOT: temporary },
          encoding: "utf8",
        },
      );
    assert.equal(run("--check").status, 0);
    const file = path.join(temporary, files[1]),
      value = JSON.parse(fs.readFileSync(file));
    value.defaultUses = 700;
    value.maximumBodyBytes = 32000;
    fs.writeFileSync(file, JSON.stringify(value));
    const definitionsPath = path.join(temporary, files[2]);
    const definitions = JSON.parse(fs.readFileSync(definitionsPath));
    definitions.currencies.push({
      ...definitions.currencies[0],
      id: "TEST_NEW_CURRENCY",
    });
    fs.writeFileSync(definitionsPath, JSON.stringify(definitions));
    assert.notEqual(run("--check").status, 0);
    assert.equal(run("--write").status, 0);
    assert.equal(run("--check").status, 0);
    const schema = JSON.parse(
      fs.readFileSync(
        path.join(temporary, "contracts/workbench-definitions-v1.schema.json"),
      ),
    );
    assert.equal(
      schema.properties.currencies.maxItems,
      definitions.currencies.length,
    );
    assert(
      schema.properties.currencies.items.properties.id.enum.includes(
        "TEST_NEW_CURRENCY",
      ),
    );
    assert.match(
      fs.readFileSync(
        temporary + "/frontend/src/shared/contracts/public.ts",
        "utf8",
      ),
      /"defaultUses":700/,
    );
    assert.match(
      fs.readFileSync(
        temporary + "/infra/docker/chaos-repeat-limits.conf",
        "utf8",
      ),
      /32000/,
    );
    assert.match(
      fs.readFileSync(temporary + "/docs/current/support.md", "utf8"),
      /기본 700/,
    );
  } finally {
    fs.rmSync(temporary, { recursive: true, force: true });
  }
});
test("collection output cannot target runtime files or a symlink into the repository", () => {
  assert.throws(() => proposalDirectory([]), /output-dir/);
  assert.throws(
    () =>
      proposalDirectory(["--output-dir", root + "frontend/src/shared/i18n"]),
    /outside/,
  );
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "proposal-"));
  try {
    fs.symlinkSync(root, dir + "/link");
    assert.throws(
      () => proposalDirectory(["--output-dir", dir + "/link"]),
      /outside/,
    );
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
