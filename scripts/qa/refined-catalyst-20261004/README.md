# Refined catalyst / cap-change QA

Synthetic fixtures only. The runner requires an isolated application on localhost ports18680/18681, a Playwright browser image with `/qa/node_modules/playwright`, the source mounted read-only at `/source`, and an owned writable `/evidence` directory. Do not run build/test output in the live checkout or reuse a user browser profile.

Run `refined-api.cjs` first, then `refined-browser.cjs`; the latter reads the first script's fixture/results JSON. These cover all13 refined actions, ordinary/refined base restrictions, Magic/Rare starting editing, exact original rolls, actual Cast Speed match/NO_MATCH, max/repeat/replacement, explicit cap clamp ledger, Omen candidate odds, before/after film states, non-autorefill, six languages and1440/390px screenshots. Existing Omen/catalyst/i18n regression harnesses are run separately; counts must not be added twice after a retry.

Docker QA location for the completed run: `E:\WORK\Exile-Hephaistos\codex\refined-catalyst-20261004`. Its Compose project uses tmpfs PostgreSQL and no persistent user data. The service hosts in these scripts intentionally address `host.docker.internal` inside Docker. See the repository validation evidence for the actual results, source hashes and preserved failure history.
