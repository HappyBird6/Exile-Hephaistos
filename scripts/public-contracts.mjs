import fs from "node:fs";
import { fileURLToPath } from "node:url";
export const root = (
  process.env.HEPHAISTOS_CONTRACT_ROOT ??
  fileURLToPath(new URL("../", import.meta.url))
).replace(/\/?$/, "/");
export const readJSON = (path) =>
  JSON.parse(fs.readFileSync(root + path, "utf8"));
export const localeContract = readJSON(
  "backend/src/main/resources/contracts/locales.json",
);
export const localePaths = Object.fromEntries(
  Object.entries(localeContract.locales).map(([code, entry]) => [
    code,
    entry.sourcePath,
  ]),
);
export const localeCodes = Object.keys(localePaths);
export const repeatContract = readJSON(
  "backend/src/main/resources/contracts/chaos-repeat.json",
);
