const fs=require('fs');
const dir='backend/src/main/java/com/poe2craft/';
function edit(p,f){const s=fs.readFileSync(dir+p,'utf8');fs.writeFileSync(dir+p,f(s));}
edit('item/infrastructure/ItemCatalogLoader.java',s=>s.replace('  /** Complete ordinary Bow',`  public static ItemCatalog loadBasicJewel(String base) {
    if (!java.util.Set.of("ruby", "emerald", "diamond").contains(base))
      throw new IllegalArgumentException("Unsupported Basic Jewel catalog");
    String root = "/catalog/" + base + "/";
    try (var data = ItemCatalogLoader.class.getResourceAsStream(root + "catalog.json");
        var raw = ItemCatalogLoader.class.getResourceAsStream(root + "base.raw.json");
        var details = ItemCatalogLoader.class.getResourceAsStream(root + "details.raw.json")) {
      return load(data, raw, details);
    } catch (IOException e) { throw new IllegalStateException("Cannot load Basic Jewel catalog", e); }
  }

  /** Complete ordinary Bow`).replace('List.of("poe2db-sapphire-editor-20261004-63e81efc2080")','List.of("poe2db-sapphire-editor-20261004-63e81efc2080", "poe2db-sapphire-basic-liquid-20261004-2eaf12518879")'));
edit('item/ItemStateValidator.java',s=>s.replaceAll('state.baseItemId().equals(SapphireJewel.BASE_ID)','BasicJewel.supported(state.baseItemId())').replace('if (slots.usedPrefixes() > slots.maxPrefixes()\n          || slots.usedSuffixes() > slots.maxSuffixes())',`if (BasicJewel.supported(state.baseItemId()) && state.rarity() == ItemState.Rarity.RARE
          ? !BasicJewel.existingCapacity(slots.usedPrefixes(), slots.usedSuffixes(), slots.maxPrefixes(), slots.maxSuffixes())
          : slots.usedPrefixes() > slots.maxPrefixes() || slots.usedSuffixes() > slots.maxSuffixes())`).replace('    return new AffixSlots(prefixes, suffixes, maxPrefixes, maxSuffixes);',`    if (BasicJewel.supported(state.baseItemId()) && state.rarity() == ItemState.Rarity.RARE) {
      var ids = state.explicits().stream().map(ModifierInstance::modifierId).toList();
      maxPrefixes += BasicJewel.extra(ids, AffixType.PREFIX);
      maxSuffixes += BasicJewel.extra(ids, AffixType.SUFFIX);
    }
    return new AffixSlots(prefixes, suffixes, maxPrefixes, maxSuffixes);`));
edit('item/CatalystQuality.java',s=>s.replace('base.equals(SapphireJewel.BASE_ID)','BasicJewel.supported(base)'));
edit('crafting/domain/ModifierPoolResolver.java',s=>s.replace('    if (prefixCount >= p)',`    if (com.poe2craft.item.BasicJewel.supported(state.baseItemId()) && state.rarity() == ItemState.Rarity.RARE) {
      p += com.poe2craft.item.BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.PREFIX);
      s += com.poe2craft.item.BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.SUFFIX);
    }
    if (prefixCount >= p)`));
edit('crafting/domain/CraftingEngine.java',s=>s.replace('    if (p > maxP || s > maxS)',`    if (BasicJewel.supported(state.baseItemId()) && state.rarity() == ItemState.Rarity.RARE) {
      maxP += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.PREFIX);
      maxS += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.SUFFIX);
      if (!BasicJewel.existingCapacity(p, s, maxP, maxS)) throw new IllegalArgumentException("Affix capacity exceeded");
      return;
    }
    if (p > maxP || s > maxS)`));
edit('crafting/domain/WorkbenchCurrency.java',s=>s.replace('  DILUTED_LIQUID_IRE',`  POTENT_LIQUID_MELANCHOLY(null, 0),
  POTENT_LIQUID_FEROCITY(null, 0),
  POTENT_LIQUID_CONTEMPT(null, 0),
  DILUTED_LIQUID_IRE`).replace('    return switch (this) {\n      case DILUTED_LIQUID_IRE',`    return switch (this) {
      case POTENT_LIQUID_MELANCHOLY -> List.of("sapphire:crafted:CraftedJewelExposureOnHitWhileRubyEmeraldSocketed");
      case POTENT_LIQUID_FEROCITY -> List.of("sapphire:crafted:CraftedJewelSuffixEffect", "sapphire:crafted:CraftedJewelPrefixEffect");
      case POTENT_LIQUID_CONTEMPT -> List.of("sapphire:crafted:CraftedJewelAdditionalSuffixAllowed", "sapphire:crafted:CraftedJewelAdditionalPrefixAllowed");
      case DILUTED_LIQUID_IRE`));
edit('crafting/domain/WorkbenchSimulator.java',s=>s.replace('    return replacementTargetOverrides.getOrDefault(action, action.replacementModifiers());',`    if (action.isLiquid() && BasicJewel.supported(catalog.base().id()))
      return catalog.modifiers().values().stream().filter(d -> d.tags().contains("crafted")
          && d.sourceUrl().equals(action.replacementSource())).map(ModifierDefinition::id).toList();
    return replacementTargetOverrides.getOrDefault(action, action.replacementModifiers());`).replaceAll('catalog.base().id().equals(SapphireJewel.BASE_ID)','BasicJewel.supported(catalog.base().id())').replaceAll('state.baseItemId().equals(SapphireJewel.BASE_ID)','BasicJewel.supported(state.baseItemId())').replace('return "sapphire-basic-liquid-v2"','return "basic-jewel-potent-v3"').replace('    if (action.isLiquid()\n        && state.explicits()',`    if (action.isLiquid() && replacementTargets(action).isEmpty())
      return blocked(action, "No sourced Liquid outcome for this Basic Jewel base.");
    if (action.isLiquid()
        && state.explicits()`).replace('        if (targets.stream().anyMatch(target -> !canAddFixed(afterRemoval, target)))','        if (!action.isLiquid() && targets.stream().anyMatch(target -> !canAddFixed(afterRemoval, target)))').replace('    return state.itemLevel() >= target.requiredItemLevel()',`    if (BasicJewel.supported(state.baseItemId()))
      capacity += BasicJewel.extra(state.explicits().stream().map(ModifierInstance::modifierId).toList(), target.affixType());
    return state.itemLevel() >= target.requiredItemLevel()`).replace('    var target = catalog.find(replacementTargets(action).getFirst()).orElseThrow();','    var targets = replacementTargets(action).stream().map(id -> catalog.find(id).orElseThrow()).toList();').replace('return canAddFixed(copy(state, state.rarity(), state.implicits(), rest), target);','return targets.stream().anyMatch(target -> canAddFixed(copy(state, state.rarity(), state.implicits(), rest), target));').replace('      var targets = replacementTargets(action);\n      var definition =',`      var afterRemoval = copy(state, state.rarity(), state.implicits(), explicits);
      var targets = replacementTargets(action).stream().filter(id -> !action.isLiquid()
          || canAddFixed(afterRemoval, catalog.find(id).orElseThrow())).toList();
      var definition =`).replace('"uniform-essence-choice-v1",\n                "fixed essence modifier outcome",','action.isLiquid() ? "uniform-liquid-outcomes-v1" : "uniform-essence-choice-v1",\n                "valid sourced modifier outcome",').replace('"https://poe2db.tw/us/Sapphire",\n              "USER-APPROVED', 'catalog.metadata().sourceUrl(),\n              "USER-APPROVED'));
edit('crafting/application/WorkbenchService.java',s=>s.replace('  private final ItemCatalog sapphire;',`  private final Map<String, ItemCatalog> basicJewels = new HashMap<>();
  private final Map<String, WorkbenchSimulator> basicJewelSimulators = new HashMap<>();
  private final ItemCatalog sapphire;`).replace('    this.sapphire = sapphire;',`    if (sapphire != null) for (String base : List.of("ruby", "emerald", "diamond")) {
      var c = com.poe2craft.item.infrastructure.ItemCatalogLoader.loadBasicJewel(base);
      basicJewels.put(base, c);
      basicJewelSimulators.put(c.base().id(), new WorkbenchSimulator(c, new CraftingEngine(c)));
    }
    this.sapphire = sapphire;`).replace('  private ItemCatalog catalog(String base) {','  private ItemCatalog catalog(String base) {\n    if (basicJewels.containsKey(base)) return basicJewels.get(base);').replace('  private WorkbenchSimulator simulator(ItemState state) {','  private WorkbenchSimulator simulator(ItemState state) {\n    if (state != null && basicJewelSimulators.containsKey(state.baseItemId())) return basicJewelSimulators.get(state.baseItemId());').replace('    var selected =\n        java.util.stream.Stream.of(', '    var selected =\n        java.util.stream.Stream.concat(basicJewels.values().stream(), java.util.stream.Stream.of(').replace('solar, stocky, bow, wand, body, sceptre, belt, helmet, ring, sapphire)\n            .filter','solar, stocky, bow, wand, body, sceptre, belt, helmet, ring, sapphire))\n            .filter'));
// All supported Basic Jewels share refined Catalyst semantics and the reachable 20 quality cap.
edit('crafting/domain/QualityLimitRules.java',s=>s.replaceAll('state.baseItemId().equals(SapphireJewel.BASE_ID)','BasicJewel.supported(state.baseItemId())'));
