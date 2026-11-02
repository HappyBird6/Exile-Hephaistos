Base art is copyright Grinding Gear Games. PoE2DB supplies the referenced source pages and image hosting. Source attribution records are not a new license or a claim of ownership.

`baseImageSources.json` associates exact item identities with the existing evidence file or fetched PoE2DB page, its SHA-256, and the cached WEBP hash. `images/` contains only the corresponding source images. Solar Amulet reuses the existing attributed local asset.

Fire Quiver's source image returned HTTP 403 and was not retried. That base uses an accessible placeholder. Supply approved art through the optional `imageForBase` prop or add a verified local asset and update its manifest entry during integration.

Maintenance tools are manual, never run by the application or build. `collectBaseImageSources.mjs` reads committed evidence and preserves cached records; `--fetch-missing` explicitly permits fetching source pages. `cacheBaseImages.mjs` fetches only uncached, permitted source URLs and checks WEBP magic before recording bytes and hashes.
