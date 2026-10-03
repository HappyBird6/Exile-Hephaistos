# Current development deferrals (2026-10-03, second user scope update)

This supersedes the Liquid-only development denominator. Registry entries, source facts, existing Alloy implementation/UI and archived item films are preserved. Deferral means future development priority, not data deletion or a game-obtainability assertion.

Direct user-deferred identities:64 = Liquid27 + Alloy13 + Hinekora1 + Vaal-related currency11 + Mirror/Core2 + named Omens10. Architects Orb is included in the Vaal-related group because its current effect targets Corrupted equipment/Jewels; the group is not selected merely by an English `Vaal_` prefix. The four sacrifice currencies and four Vaal infusers are explicit members. Omen of Corruption is a separate one-record operation dependency on deferred Vaal Orb; it is not confused with Omen of Putrefaction (부패의 징조).

Effective current-development exclusions65 = direct64 + dependency1. Registered220 = excluded65 + current-development inventory155. Overall implemented109 = current-scope implemented101 + preserved deferred Alloy8. Current-scope unimplemented54 =155−101; this includes other-base requirements and unverified rules/obtainability, so it is not a two-base completion percentage or complete game-inventory denominator. This109 includes the Crude Bow batches and eight new Attuned Wand Essence identities;75/67/88 remains the historical Artificer checkpoint count.

Full implemented categories: Currency20, Essence70, Alloy8, Omen11. The eight retained implemented Alloys are Runic, Prismatic, Expansive, Cyclonic, Mystic, Adaptive, Swift and Sovereign. No existing Alloy action is disabled. Crystallisation omens also serve current Essences and are not deferred merely because some material interactions exist.

Current next work: remaining bounded equipment targets, with complete eligible-result evidence first. Attuned Wand adds11 paths but only eight new identities; [evidence](workbench-attuned-wand-2026-10-03.md). Six reviewed Perfect Bow Essences are delivered through the same shared engine; [evidence and checks](workbench-bow-perfect-2026-10-03.md). Crude Bow basic Essence delivery adds20 unique implementations through the shared engine; no combat calculator. Quality increments and Wisdom/Extraction state boundaries remain unresolved. The same65 exclusions apply. See [selection, evidence and actual blockers](workbench-crude-bow-2026-10-03.md).

## Exact mapping

Korean item names come from individually captured PoE2DB `/kr/{id}` cards, not invented translations. Liquid27 was freshly captured; other names use today's existing193-page manifest. [Machine-readable IDs, names, source/hash provenance and counts](evidence/workbench-current-development-scope-2026-10-03.json).

| Registry ID | Korean name | Development disposition | Existing implementation |
|---|---|---|---|
| Vaal_Orb | 바알 오브 | USER_DEFERRED | Unimplemented |
| Hinekoras_Lock | 히네코라의 머리카락 | USER_DEFERRED | Unimplemented |
| Runic_Alloy | 룬 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Adaptive_Alloy | 적응형 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Protective_Alloy | 보호의 합금 | USER_DEFERRED | Unimplemented |
| Expansive_Alloy | 팽창하는 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Swift_Alloy | 재빠른 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Cyclonic_Alloy | 회오리바람 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Prismatic_Alloy | 분광 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Mystic_Alloy | 신비한 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Sovereign_Alloy | 군왕의 합금 | USER_DEFERRED | IMPLEMENTED, retained |
| Celestial_Alloy | 천공의 합금 | USER_DEFERRED | Unimplemented |
| Transcendent_Alloy | 초월의 합금 | USER_DEFERRED | Unimplemented |
| The_Runebinders_Alloy | 룬 결속사의 합금 | USER_DEFERRED | Unimplemented |
| The_Runefathers_Alloy | 룬 아버지의 합금 | USER_DEFERRED | Unimplemented |
| Omen_of_Corruption | 타락의 징조 | EXCLUDED_OPERATION_DEPENDENCY | Unimplemented |
| Omen_of_Answered_Prayers | 응답받은 기도의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_Secret_Compartments | 비밀 공간의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_the_Hunt | 사냥의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_Reinforcements | 보강의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_Sanctification | 축성의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_the_Sovereign | 군주의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_the_Liege | 군왕의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_the_Blackblooded | 검은 피의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_Putrefaction | 부패의 징조 | USER_DEFERRED | Unimplemented |
| Omen_of_Light | 빛의 징조 | USER_DEFERRED | Unimplemented |
| Diluted_Liquid_Ire | 희석된 액체 진노 | USER_DEFERRED | Unimplemented |
| Diluted_Liquid_Guilt | 희석된 액체 죄책감 | USER_DEFERRED | Unimplemented |
| Diluted_Liquid_Greed | 희석된 액체 탐욕 | USER_DEFERRED | Unimplemented |
| Liquid_Paranoia | 액체 집착 | USER_DEFERRED | Unimplemented |
| Liquid_Envy | 액체 선망 | USER_DEFERRED | Unimplemented |
| Liquid_Disgust | 액체 혐오 | USER_DEFERRED | Unimplemented |
| Liquid_Despair | 액체 절망 | USER_DEFERRED | Unimplemented |
| Concentrated_Liquid_Fear | 농축된 액체 두려움 | USER_DEFERRED | Unimplemented |
| Concentrated_Liquid_Suffering | 농축된 액체 고통 | USER_DEFERRED | Unimplemented |
| Concentrated_Liquid_Isolation | 농축된 액체 고립 | USER_DEFERRED | Unimplemented |
| Ancient_Diluted_Liquid_Ire | 고대 희석된 액체 진노 | USER_DEFERRED | Unimplemented |
| Ancient_Diluted_Liquid_Guilt | 고대 희석된 액체 죄책감 | USER_DEFERRED | Unimplemented |
| Ancient_Diluted_Liquid_Greed | 고대 희석된 액체 탐욕 | USER_DEFERRED | Unimplemented |
| Ancient_Liquid_Paranoia | 고대 액체 집착 | USER_DEFERRED | Unimplemented |
| Ancient_Liquid_Envy | 고대 액체 선망 | USER_DEFERRED | Unimplemented |
| Ancient_Liquid_Disgust | 고대 액체 혐오 | USER_DEFERRED | Unimplemented |
| Ancient_Liquid_Despair | 고대 액체 절망 | USER_DEFERRED | Unimplemented |
| Ancient_Concentrated_Liquid_Fear | 고대 농축된 액체 두려움 | USER_DEFERRED | Unimplemented |
| Ancient_Concentrated_Liquid_Suffering | 고대 농축된 액체 고통 | USER_DEFERRED | Unimplemented |
| Ancient_Concentrated_Liquid_Isolation | 고대 농축된 액체 고립 | USER_DEFERRED | Unimplemented |
| Potent_Liquid_Melancholy | 위력적인 액체 우울 | USER_DEFERRED | Unimplemented |
| Potent_Liquid_Ferocity | 위력적인 액체 흉포함 | USER_DEFERRED | Unimplemented |
| Potent_Liquid_Contempt | 위력적인 액체 경멸 | USER_DEFERRED | Unimplemented |
| Ancient_Potent_Liquid_Melancholy | 고대 위력적인 액체 우울 | USER_DEFERRED | Unimplemented |
| Ancient_Potent_Liquid_Ferocity | 고대 위력적인 액체 흉포함 | USER_DEFERRED | Unimplemented |
| Ancient_Potent_Liquid_Contempt | 고대 위력적인 액체 경멸 | USER_DEFERRED | Unimplemented |
| Liquid_Verisium | 액체 베리시움 | USER_DEFERRED | Unimplemented |
| Mirror_of_Kalandra | 칼란드라의 거울 | USER_DEFERRED | Unimplemented |
| Architects_Orb | 건축가의 오브 | USER_DEFERRED | Unimplemented |
| Core_Destabiliser | 핵 불안정화기 | USER_DEFERRED | Unimplemented |
| Vaal_Cultivation_Orb | 바알 함양 오브 | USER_DEFERRED | Unimplemented |
| Yaomacs_Orb_of_Sacrifice | 야오맥의 희생의 오브 | USER_DEFERRED | Unimplemented |
| Kopecs_Orb_of_Sacrifice | 코펙의 희생의 오브 | USER_DEFERRED | Unimplemented |
| Kamasas_Orb_of_Sacrifice | 카마사의 희생의 오브 | USER_DEFERRED | Unimplemented |
| Yuguls_Orb_of_Sacrifice | 유굴의 희생의 오브 | USER_DEFERRED | Unimplemented |
| Vaal_Armourers_Infuser | 바알 방어구 장인의 주입기 | USER_DEFERRED | Unimplemented |
| Vaal_Blacksmiths_Infuser | 바알 대장장이의 주입기 | USER_DEFERRED | Unimplemented |
| Vaal_Arcanists_Infuser | 바알 신비학자의 주입기 | USER_DEFERRED | Unimplemented |
| Vaal_Catalysing_Infuser | 바알 촉진시키는 주입기 | USER_DEFERRED | Unimplemented |

Current68 pending principal blockers, re-filtered from the dated per-identity triage: **41 other-target requirements** (26 Essence,13 Refined Catalyst,2 weapon/caster quality currencies), **20 rule/data gaps**, **2 engine/input-state gaps** (Wisdom/Extraction), and **5 reintroduction-unverified omens**. Other-target41 does not mean41 unknown game effects: a different complete supported equipment/resource catalog is needed. Some records may also have secondary conditions needing review; the principal bucket is not an assurance that every rule is complete. The user explicitly authorised bounded base expansion; only Crude Bow is added in this batch.