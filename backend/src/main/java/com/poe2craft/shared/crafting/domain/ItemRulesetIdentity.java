package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemCatalog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 현재 베이스의 제작 의미를 해시로 묶어 저장 상태의 규칙 호환성을 판정한다. 과거 엔진을 재현하는 자료는 아니다.
 *
 * <p>private 생성자는 인스턴스 생성을 막는다. of(catalog,simulator,coupled)는 버전·슬롯 한도·베이스 정책· 옵션 정의·복합 수치 대상·에센스
 * 연결·화폐/징조 정책·기본 제작 한도를 parts에 모은다. 집합은 정렬하고 각 문자열 길이를 앞에 붙여 경계가 다른 입력이 합쳐지지 않도록 SHA-256에 넣는다. 반환값은
 * item-rules-v2 접두사의 호환성 ID이며 외관 번역은 제외한다. 입력 객체는 변경하지 않는다. SHA-256 부재는 IllegalStateException이다. 정책
 * JSON이 바뀌면 같은 ID라고 간주하지 않는다.
 */
public final class ItemRulesetIdentity {
  private ItemRulesetIdentity() {}

  public static String of(ItemCatalog catalog, WorkbenchSimulator simulator, Set<String> coupled) {
    var parts = new ArrayList<String>();
    var base = catalog.base();
    Collections.addAll(
        parts,
        "item-rules-v2",
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
    for (var currency : WorkbenchDefinitions.active().document().currencies())
      if (currency.materialPolicy().mode() != WorkbenchDefinitions.MaterialMode.NONE)
        parts.add(currency.id() + ":" + currency.materialPolicy());
    parts.add("crafted-limit:" + com.poe2craft.shared.item.AugmentCraftingRules.baseCraftedLimit());
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (var part : parts)
        digest.update((part.length() + ":" + part).getBytes(StandardCharsets.UTF_8));
      return "item-rules-v2:" + HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
