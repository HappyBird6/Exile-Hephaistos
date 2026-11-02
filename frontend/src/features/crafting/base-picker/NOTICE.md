Base art is copyright Grinding Gear Games. PoE2DB supplies the referenced source pages and image hosting. Source attribution records are not a new license or a claim of ownership.

`baseImageSources.json` associates exact item identities with the existing evidence file or fetched PoE2DB page, its SHA-256, and the cached image hash. `images/` contains the corresponding source images and the user-provided Fire Quiver image. Solar Amulet reuses the existing attributed local asset.

Fire Quiver's remote source image returned HTTP 403 and was not retried. On 2026-11-02 the user supplied `QuiverFire.jpg` from their Downloads folder. Its content is WEBP despite the filename; the original bytes are preserved under the matching `.webp` extension and verified by SHA-256. Both the base picker and session cards now use that asset. The remote denial record remains for maintenance tools; no runtime remote request is made.

Maintenance tools are manual, never run by the application or build. `collectBaseImageSources.mjs` reads committed evidence and preserves cached records; `--fetch-missing` explicitly permits fetching source pages. `cacheBaseImages.mjs` fetches only uncached, permitted source URLs and checks WEBP magic before recording bytes and hashes.
