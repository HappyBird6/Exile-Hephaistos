# Automatic currency policy UI verification

Verified locally on 2026-10-16 using the project Node 24 Docker environment.

- ESLint, TypeScript, Prettier, all 2,168 Vitest tests, and production build passed.
- Chromium exercised the actual policy modal and method-map components in all six locales. All 187 entries rendered, 390px mobile content fit, Escape restored trigger focus, and selecting a node invoked the editor callback without browser errors.
- The browser harness supplied the existing certified diamond response as a fixture. This verifies UI interaction, not production integration or probabilities of the new currency policy engine.
- Selecting a node clones its concrete representative state into the existing editor, clears the previous search, retains the goal, and requires a new search generation. Users are prompted to check exact modifier values before restarting.
- The production build reports its existing large JavaScript chunk warning (4.59 MB uncompressed); no new performance claim is made.

The modal exposes the approved roster, conditions, and deferred mechanics. Policy permission is separate from implemented probability support. Production graph integration remains outstanding.
