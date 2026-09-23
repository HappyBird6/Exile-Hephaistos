# Frontend locale resources

Runtime UI and item clipboard parsing currently support English only (user decision 2026-09-23). Korean resources have been removed. Old saved `ko` preferences and unsupported browser languages fall back to `en`; the language selector hides itself while only one locale is registered. HTML starts with `lang="en"`.

English keys in `src/shared/i18n/locales/en.ts` define the message and currency-label contract. `messages.ts`, `LocaleProvider` and `useI18n()` remain the small extension point. To add a language later, create a typed `Record<keyof typeof en, string>` resource and corresponding currency labels, register it in `locales`, and test key/placeholder parity. The selector will then appear. Do not hardcode translated strings into components or duplicate server item data in locale state.

Item source text, affix names and values are not automatically translated. Future catalog translation and translated clipboard input require separate verified mappings. The manual TypeScript API DTO follows `docs/openapi-item.yaml` and validates structured modifier fields at runtime; OpenAPI generation is a future tooling task.
