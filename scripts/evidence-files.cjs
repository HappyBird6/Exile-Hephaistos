// Physical evidence is organized by feature. Collection keys are stable tool input IDs.
const fs = require("node:fs");
const path = require("node:path");
const repository = path.resolve(__dirname, "..");
const versionRoot = "docs/evidence/0.5.5";
let index;
function catalog() {
  return (index ??= JSON.parse(
    fs.readFileSync(
      path.join(repository, versionRoot, "file-index.json"),
      "utf8",
    ),
  ));
}
function logical(value) {
  if (typeof value !== "string") return null;
  const match = value
    .replaceAll("\\", "/")
    .match(/^(.*?)(?:^|\/)evidence:(.*)$/);
  if (!match) return null;
  let key = match[2]
    .replace(/\/$/, "")
    .replace(/[-_]?20\d{2}[-_]?\d{2}[-_]?\d{2}/g, "");
  if (!key.includes("/") && /\.[a-z]+$/.test(key)) key = "general/" + key;
  return { base: match[1] || process.cwd(), key };
}
function slug(value) {
  return value
    .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}
function resolveEvidencePath(value) {
  const ref = logical(value);
  if (!ref) return value;
  const data = catalog();
  if (data.files[ref.key])
    return path.resolve(ref.base, versionRoot, data.files[ref.key]);
  const [collection, ...parts] = ref.key.split("/");
  let feature = data.collections[collection] || "catalog";
  if (collection === "general") {
    const name = parts.join("-");
    if (/^i18n/.test(name)) feature = "i18n";
    else if (/assets?|images?/.test(name)) feature = "assets";
    else if (/history|notation|film/.test(name)) feature = "workbench/history";
    else if (/layout|stash|viewport/.test(name)) feature = "workbench/layout";
    else if (/feedback/.test(name)) feature = "workbench/feedback";
    else if (/desecration|reveal|necromancy|abyss/.test(name))
      feature = "crafting/abyss";
    else if (/quality|catalyst/.test(name)) feature = "crafting/quality";
    else if (/socket/.test(name)) feature = "crafting/sockets";
    else if (/first-hit/.test(name)) feature = "search/first-hit";
    else if (/automatic|preparation/.test(name)) feature = "search/automatic";
    else if (/release|backend|frontend|integration/.test(name))
      feature = "release";
  }
  if (!parts.length) return path.resolve(ref.base, versionRoot, feature);
  // New tool outputs belong to the feature's checks directory; raw inputs use indexed paths.
  const filename = parts.pop();
  const ext = path.extname(filename);
  return path.resolve(
    ref.base,
    versionRoot,
    feature,
    "checks",
    slug(
      collection + "-" + parts.concat(filename.slice(0, -ext.length)).join("-"),
    ) + ext,
  );
}
function entries(ref, options) {
  const prefix = ref.key + "/";
  const children = new Map();
  for (const key of Object.keys(catalog().files)) {
    if (!key.startsWith(prefix)) continue;
    const tail = key.slice(prefix.length);
    const name = tail.split("/")[0];
    children.set(name, children.get(name) || tail.includes("/"));
  }
  return [...children].map(([name, directory]) =>
    options?.withFileTypes
      ? {
          name,
          isDirectory: () => directory,
          isFile: () => !directory,
          isSymbolicLink: () => false,
        }
      : name,
  );
}
function readFileSync(file, ...args) {
  return fs.readFileSync(resolveEvidencePath(file), ...args);
}
function writeFileSync(file, data, ...args) {
  const target = resolveEvidencePath(file);
  if (logical(file)) fs.mkdirSync(path.dirname(target), { recursive: true });
  const result = fs.writeFileSync(target, data, ...args);
  const ref = logical(file);
  if (ref && !catalog().files[ref.key]) {
    catalog().files[ref.key] = path
      .relative(path.resolve(ref.base, versionRoot), target)
      .replaceAll("\\", "/");
    fs.mkdirSync(path.resolve(ref.base, versionRoot), { recursive: true });
    fs.writeFileSync(
      path.resolve(ref.base, versionRoot, "file-index.json"),
      JSON.stringify(catalog(), null, 2) + "\n",
    );
  }
  return result;
}
function existsSync(file) {
  const ref = logical(file);
  if (
    ref &&
    Object.keys(catalog().files).some((k) => k.startsWith(ref.key + "/"))
  )
    return true;
  if (ref && !catalog().files[ref.key]) return false;
  return fs.existsSync(resolveEvidencePath(file));
}
function readdirSync(file, options) {
  const ref = logical(file);
  return ref ? entries(ref, options) : fs.readdirSync(file, options);
}
function mkdirSync(file, options) {
  const ref = logical(file);
  return fs.mkdirSync(
    resolveEvidencePath(file),
    ref ? { recursive: true } : options,
  );
}
function statSync(file, ...args) {
  const ref = logical(file);
  if (
    ref &&
    Object.keys(catalog().files).some((k) => k.startsWith(ref.key + "/"))
  ) {
    return fs.statSync(path.join(repository, versionRoot));
  }
  return fs.statSync(resolveEvidencePath(file), ...args);
}
function cpSync(source, destination, options) {
  const from = logical(source),
    to = logical(destination);
  if (
    from &&
    to &&
    Object.keys(catalog().files).some((k) => k.startsWith(from.key + "/"))
  ) {
    for (const [key, file] of Object.entries(catalog().files)) {
      if (!key.startsWith(from.key + "/")) continue;
      const sourcePath = path.resolve(from.base, versionRoot, file);
      const targetPath = path.resolve(to.base, versionRoot, file);
      fs.mkdirSync(path.dirname(targetPath), { recursive: true });
      fs.cpSync(sourcePath, targetPath, options);
    }
    fs.writeFileSync(
      path.resolve(to.base, versionRoot, "file-index.json"),
      JSON.stringify(catalog(), null, 2) + "\n",
    );
    return;
  }
  return fs.cpSync(
    resolveEvidencePath(source),
    resolveEvidencePath(destination),
    options,
  );
}
module.exports = { ...fs, resolveEvidencePath };
module.exports.readFileSync = readFileSync;
module.exports.writeFileSync = writeFileSync;
module.exports.existsSync = existsSync;
module.exports.readdirSync = readdirSync;
module.exports.mkdirSync = mkdirSync;
module.exports.statSync = statSync;
module.exports.cpSync = cpSync;
module.exports.copyFileSync = (a, b, ...args) =>
  fs.copyFileSync(resolveEvidencePath(a), resolveEvidencePath(b), ...args);
module.exports.appendFileSync = (a, ...args) =>
  fs.appendFileSync(resolveEvidencePath(a), ...args);
module.exports.lstatSync = (a, ...args) =>
  fs.lstatSync(resolveEvidencePath(a), ...args);
module.exports.rmSync = (file, ...args) => {
  const ref = logical(file);
  if (
    ref &&
    Object.keys(catalog().files).some((key) => key.startsWith(ref.key + "/"))
  ) {
    throw new Error(
      "Remove individual indexed files, not a shared evidence feature directory",
    );
  }
  return fs.rmSync(resolveEvidencePath(file), ...args);
};
