// Run from repository root. Executes the existing frontend formatter, not a handwritten oracle.
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { createHash } from "node:crypto";
import { stripTypeScriptTypes } from "node:module";

const path =
  "frontend/src/features/crafting/numeric-display/attributeNumbers.ts";
const source = readFileSync(path, "utf8");
const executable = stripTypeScriptTypes(
  source
    .replace(/^import .*\r?\n/gm, "")
    .replaceAll("export function ", "function "),
);
const { formatAttributeNumber, formatAttributeText } = new Function(
  "getLocale",
  executable + "\nreturn { formatAttributeNumber, formatAttributeText }",
)(() => "en");
const locales = ["en", "ko", "zh-CN", "zh-TW", "ja", "es", "ru", "th"];
const values = [
  0,
  -0,
  0.0499,
  -0.0499,
  0.05,
  -0.05,
  1.25,
  -1.25,
  791 / 60,
  999.95,
  1234,
  1234.55,
  12345,
  12345.65,
  1000000,
  9007199254740991,
];
const numberCases = locales.flatMap((locale) =>
  values.map((value) => ({
    locale,
    value,
    expected: formatAttributeNumber(value, locale),
  })),
);
const texts = [
  "+12.345% increased",
  "(-1.25—2.35) seconds",
  "0.0499 to -0.0499",
  "0.05 to -0.05",
  "999.999%",
  "+12 / -9 / 1,234",
  "1.2 ms and 1.000%",
  "9007199254740993.55%",
  "stat_id_1.234",
  "1234.55 and 12345.55",
];
const textCases = locales.flatMap((output) =>
  texts.map((text) => ({
    source: "en",
    output,
    text,
    expected: formatAttributeText(text, "en", output),
  })),
);
for (const text of ["1.234,567% / 1.234", "-0,0499 / -0,05", "+12,345"]) {
  textCases.push({
    source: "es",
    output: "es",
    text,
    expected: formatAttributeText(text, "es"),
  });
}
for (const output of ["ru", "en"]) {
  const text = "1\u00a0234,567% / -0,05";
  textCases.push({
    source: "ru",
    output,
    text,
    expected: formatAttributeText(text, "ru", output),
  });
}
const target = "backend/src/test/resources/chaosdisplay/numbers-golden.json";
mkdirSync("backend/src/test/resources/chaosdisplay", { recursive: true });
writeFileSync(
  target,
  JSON.stringify(
    {
      sourcePath: path,
      sourceSha256: createHash("sha256").update(source).digest("hex"),
      nodeVersion: process.version,
      numberCases,
      textCases,
    },
    null,
    2,
  ) + "\n",
);
