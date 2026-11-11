import fs from "node:fs";
import path from "node:path";
import { root } from "./public-contracts.mjs";
/** Collectors produce review inputs outside the repository, never overwrite runtime dictionaries. */
export function proposalDirectory(argv) {
  const flag = argv.indexOf("--output-dir");
  if (flag < 0 || !argv[flag + 1])
    throw Error(
      "Supply --output-dir <owned directory outside the repository>; review the proposal before importing it.",
    );
  const directory = path.resolve(argv[flag + 1]);
  let ancestor = directory;
  while (!fs.existsSync(ancestor)) ancestor = path.dirname(ancestor);
  const real = fs.realpathSync(ancestor),
    repository = fs.realpathSync(root);
  if (real === repository || real.startsWith(repository + path.sep))
    throw Error(
      "Source proposals must be outside the repository; runtime files are never collection outputs.",
    );
  fs.mkdirSync(directory, { recursive: true });
  return fs.realpathSync(directory);
}
