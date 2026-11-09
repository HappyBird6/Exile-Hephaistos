package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Version this projection when its meaning changes; it is not a historical engine archive. */
public final class ItemRulesetIdentity {
  private ItemRulesetIdentity() {}

  public static String of(ItemCatalog catalog, WorkbenchSimulator simulator, Set<String> coupled) {
    var parts = new ArrayList<String>();
    var base = catalog.base();
    Collections.addAll(
        parts,
        "item-rules-v1",
        WorkbenchSimulator.RULE_VERSION,
        WorkbenchSimulator.LEDGER_VERSION,
        simulator.ruleVersion(),
        simulator.ledgerVersion(),
        catalog.metadata().weightPolicy(),
        base.id(),
        base.implicitModifierId(),
        "" + base.magicPrefixes(),
        "" + base.magicSuffixes(),
        "" + base.rarePrefixes(),
        "" + base.rareSuffixes());
    if (BaseRegistry.supports(base.id())) {
      var policy = BaseRegistry.policy(base.id());
      Collections.addAll(
          parts,
          "" + policy.qualityLimit(),
          "" + policy.ordinaryCatalyst(),
          "" + policy.refinedCatalyst(),
          "" + policy.catalystQuality(),
          "" + policy.maximumQualityBreach(),
          "" + policy.initializeImplicit(),
          "" + policy.socketExecutionMaximum(),
          "" + policy.divine(),
          "" + policy.sourcePropertyUnscaled(),
          "" + policy.rareOnly());
    }
    catalog.modifiers().values().stream()
        .sorted(Comparator.comparing(d -> d.id()))
        .forEach(
            d -> {
              Collections.addAll(
                  parts,
                  d.id(),
                  d.layer().name(),
                  d.affixType().name(),
                  new TreeSet<>(d.familyIds()).toString(),
                  "" + d.requiredItemLevel(),
                  "" + d.weight(),
                  "" + d.tier(),
                  new TreeSet<>(d.tags()).toString());
              d.stats().stream()
                  .sorted(Comparator.comparing(s -> s.id()))
                  .forEach(s -> parts.add(s.toString()));
            });
    parts.add(new TreeSet<>(coupled).toString());
    parts.add(simulator.referenceEssenceTargets().toString());
    for (var c : WorkbenchDefinitions.active().document().currencies()) {
      if (c.baseAction() != null)
        parts.add(c.id() + ":" + c.baseAction() + ":" + c.minimumModifierLevel());
    }
    parts.add(WorkbenchDefinitions.active().document().omens().toString());
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (var part : parts)
        digest.update((part.length() + ":" + part).getBytes(StandardCharsets.UTF_8));
      return "item-rules-v1:" + HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
