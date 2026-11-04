# Chaos repeat controls

The workbench has one mode-toggle button and one regular-expression input.
The button displays 1000 x the selected Chaos image and Use; clicking it only toggles mode.
Existing stash/favorite selection and central item application start a batch when enabled.
Every batch explicitly requests 1000 uses, stops early on a match, and applies at least once.
Other currencies retain their single-use path. No count editor or extra visible explanations.
The toggle exposes aria-pressed and image color. Hints/errors use the shared feedback area.
During a request the existing button shows a calculating indicator and mutations are locked.
The API keeps its omitted-count fallback for old callers; the workbench always supplies 1000.
