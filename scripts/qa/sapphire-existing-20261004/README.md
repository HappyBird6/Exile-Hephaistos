# Sapphire existing-modifier currency QA

Owned isolated copy/profile: `E:/WORK/Exile-Hephaistos/codex/sapphire-existing-20261004`. Source is mounted read-only at `/source`; evidence at `/evidence`. Use the existing project Docker images, with only the isolated checked JAR and frontend dist mounted into the owned runtime at localhost18780/18781. Preserve localhost18080/18081, user browser/storage, original build outputs and existing DB volumes.

Run Backend mandatory checks, then Frontend mandatory checks, then API and headless browser sequentially in the exclusive heavy QA slot. Browser uses `/qa/node_modules/playwright` in `exile-workbench-browser:20261002` and a fresh context.

`api.cjs` distinguishes positive15 (Refined13 plus Annulment/Divine with the existing suffix) from registered rejected actions. It covers both rarities, empty repeat refusal, matching Omen restrictions, quality preservation, source-range roll disclosure and malformed/unknown Crafted state rejection. Crafted is not supported by this bundle.

`browser.cjs` covers starting roll input rejection, Divine/Shift repeat, Annulment, Alt interaction, undo/redo/reload and six-language scope/display screenshots at1440/390. Inspect screenshots after execution; assertion success alone is not visual review.

This is bounded existing-suffix support, not a full Jewel generation or Liquid matrix. Full Sapphire slot/pool and Basic Liquid rules remain in WB-046. Historical QA evidence is retained separately.
