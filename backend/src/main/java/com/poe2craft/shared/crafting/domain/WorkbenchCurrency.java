package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.CatalystQuality;
import java.util.List;

/**
 * 공개 화폐 실행 ID와 JSON 규칙을 연결한다. enum의 선언 순서는 API 순서이며 삭제·재배열하지 않는다.
 *
 * <p>enum 값은 이름 자체가 영속 식별자다. *_LIQUID_*는 감정액 종류/농도(ANCIENT_는 고대형), CATALYST_* /REFINED_CATALYST_*는
 * 촉매 종류·정제형, LESSER_ESSENCE_* /ESSENCE_* /GREATER_ESSENCE_* / PERFECT_ESSENCE_*는 에센스 종류·등급,
 * *_ALLOY는 합금 종류다. TRANSMUTATION/AUGMENTATION/REGAL/ EXALTED/ANNULMENT/CHAOS와 GREATER_/PERFECT_형은
 * 변환/확장/제왕/고귀/소멸/혼돈 행동이다. ARTIFICER는 소켓 확장, DIVINE은 수치 재굴림, ALCHEMY는 연금, FRACTURING은 분열,
 * VAAL_*_INFUSER는 품질 주입, ARMOURERS_SCRAP/BLACKSMITHS_WHETSTONE은 장비 품질 재료다. 각 개별 ID의 실제 후보·수치·출처는
 * definitions JSON에서 조회한다. enum에 별도 인스턴스 필드는 없다.
 *
 * <p>definition은 초기화된 WorkbenchDefinitions의 이 ID를 찾는다. essenceModifierIds/fixedModifierId는 추가 후보/단일
 * 후보, essenceChoiceSource는 후보 선택 근거다. baseAction/minimumModifierLevel은 기본 알고리즘/최소 옵션 레벨이다.
 * materialPolicy는 분류·적용 정책이고 isLiquid/isAlloy는 그 분류를 읽는다. replacementModifiers는 전체 교체 후보,
 * replacementEssenceModifiers는 에센스 교체 후보만 반환한다. replacementEssenceSource/replacementSource는 근거
 * URL이며 해당 동작이 아니면 IllegalArgumentException이다. catalystType은 촉매 ID의 종류(그 외 null), refinedCatalyst는
 * 정제형 여부를 판정한다. 모든 메서드는 읽기 전용이며 active 정의 초기화가 선행되어야 한다.
 */
public enum WorkbenchCurrency {
  ANCIENT_DILUTED_LIQUID_IRE,
  ANCIENT_DILUTED_LIQUID_GUILT,
  ANCIENT_DILUTED_LIQUID_GREED,
  ANCIENT_LIQUID_PARANOIA,
  ANCIENT_LIQUID_ENVY,
  ANCIENT_LIQUID_DISGUST,
  ANCIENT_LIQUID_DESPAIR,
  ANCIENT_CONCENTRATED_LIQUID_FEAR,
  ANCIENT_CONCENTRATED_LIQUID_SUFFERING,
  ANCIENT_CONCENTRATED_LIQUID_ISOLATION,
  ANCIENT_POTENT_LIQUID_MELANCHOLY,
  ANCIENT_POTENT_LIQUID_FEROCITY,
  ANCIENT_POTENT_LIQUID_CONTEMPT,
  POTENT_LIQUID_MELANCHOLY,
  POTENT_LIQUID_FEROCITY,
  POTENT_LIQUID_CONTEMPT,
  DILUTED_LIQUID_IRE,
  DILUTED_LIQUID_GUILT,
  DILUTED_LIQUID_GREED,
  LIQUID_PARANOIA,
  LIQUID_ENVY,
  LIQUID_DISGUST,
  LIQUID_DESPAIR,
  CONCENTRATED_LIQUID_FEAR,
  CONCENTRATED_LIQUID_SUFFERING,
  CONCENTRATED_LIQUID_ISOLATION,
  CATALYST_FLESH,
  REFINED_CATALYST_FLESH,
  CATALYST_NEURAL,
  REFINED_CATALYST_NEURAL,
  CATALYST_CARAPACE,
  REFINED_CATALYST_CARAPACE,
  CATALYST_UUL_NETOL,
  REFINED_CATALYST_UUL_NETOL,
  CATALYST_XOPH,
  REFINED_CATALYST_XOPH,
  CATALYST_TUL,
  REFINED_CATALYST_TUL,
  CATALYST_ESH,
  REFINED_CATALYST_ESH,
  CATALYST_CHAYULA,
  REFINED_CATALYST_CHAYULA,
  CATALYST_REAVER,
  REFINED_CATALYST_REAVER,
  CATALYST_SIBILANT,
  REFINED_CATALYST_SIBILANT,
  CATALYST_SKITTERING,
  REFINED_CATALYST_SKITTERING,
  CATALYST_ADAPTIVE,
  REFINED_CATALYST_ADAPTIVE,
  CATALYST_NECROTIC,
  REFINED_CATALYST_NECROTIC,
  PERFECT_ESSENCE_MIND,
  PERFECT_ESSENCE_THAWING,
  PERFECT_ESSENCE_INSULATION,
  PERFECT_ESSENCE_COMMAND,
  LESSER_ESSENCE_COMMAND,
  ESSENCE_COMMAND,
  GREATER_ESSENCE_COMMAND,
  PERFECT_ESSENCE_BODY,
  PERFECT_ESSENCE_RUIN,
  PERFECT_ESSENCE_SEEKING,
  LESSER_ESSENCE_ABRASION,
  ESSENCE_ABRASION,
  GREATER_ESSENCE_ABRASION,
  LESSER_ESSENCE_FLAMES,
  ESSENCE_FLAMES,
  GREATER_ESSENCE_FLAMES,
  LESSER_ESSENCE_ICE,
  ESSENCE_ICE,
  GREATER_ESSENCE_ICE,
  LESSER_ESSENCE_ELECTRICITY,
  ESSENCE_ELECTRICITY,
  GREATER_ESSENCE_ELECTRICITY,
  LESSER_ESSENCE_BATTLE,
  ESSENCE_BATTLE,
  LESSER_ESSENCE_HASTE,
  ESSENCE_HASTE,
  GREATER_ESSENCE_HASTE,
  LESSER_ESSENCE_SEEKING,
  ESSENCE_SEEKING,
  GREATER_ESSENCE_SEEKING,
  ARTIFICER,
  TRANSMUTATION,
  GREATER_TRANSMUTATION,
  PERFECT_TRANSMUTATION,
  AUGMENTATION,
  GREATER_AUGMENTATION,
  PERFECT_AUGMENTATION,
  REGAL,
  GREATER_REGAL,
  PERFECT_REGAL,
  EXALTED,
  GREATER_EXALTED,
  PERFECT_EXALTED,
  ANNULMENT,
  CHAOS,
  GREATER_CHAOS,
  PERFECT_CHAOS,
  DIVINE,
  ALCHEMY,
  FRACTURING,
  ESSENCE_HYSTERIA,
  ESSENCE_HORROR,
  PERFECT_ESSENCE_SORCERY,
  PERFECT_ESSENCE_ALACRITY,
  PERFECT_ESSENCE_ABRASION,
  PERFECT_ESSENCE_FLAMES,
  PERFECT_ESSENCE_ICE,
  PERFECT_ESSENCE_ELECTRICITY,
  PERFECT_ESSENCE_BATTLE,
  PERFECT_ESSENCE_HASTE,
  PERFECT_ESSENCE_GROUNDING,
  PERFECT_ESSENCE_OPULENCE,
  PERFECT_ESSENCE_INFINITE,
  PERFECT_ESSENCE_ENHANCEMENT,
  ESSENCE_BREACH,
  RUNIC_ALLOY,
  PRISMATIC_ALLOY,
  ADAPTIVE_ALLOY,
  SWIFT_ALLOY,
  SOVEREIGN_ALLOY,
  EXPANSIVE_ALLOY,
  CYCLONIC_ALLOY,
  MYSTIC_ALLOY,
  ESSENCE_ABYSS,
  LESSER_ESSENCE_SORCERY,
  ESSENCE_SORCERY,
  GREATER_ESSENCE_SORCERY,
  LESSER_ESSENCE_ALACRITY,
  ESSENCE_ALACRITY,
  GREATER_ESSENCE_ALACRITY,
  LESSER_ESSENCE_ENHANCEMENT,
  ESSENCE_ENHANCEMENT,
  GREATER_ESSENCE_ENHANCEMENT,
  GREATER_ESSENCE_BATTLE,
  LESSER_ESSENCE_BODY,
  ESSENCE_BODY,
  GREATER_ESSENCE_BODY,
  LESSER_ESSENCE_MIND,
  ESSENCE_MIND,
  GREATER_ESSENCE_MIND,
  LESSER_ESSENCE_RUIN,
  ESSENCE_RUIN,
  GREATER_ESSENCE_RUIN,
  LESSER_ESSENCE_INSULATION,
  ESSENCE_INSULATION,
  GREATER_ESSENCE_INSULATION,
  LESSER_ESSENCE_THAWING,
  ESSENCE_THAWING,
  GREATER_ESSENCE_THAWING,
  LESSER_ESSENCE_GROUNDING,
  ESSENCE_GROUNDING,
  GREATER_ESSENCE_GROUNDING,
  LESSER_ESSENCE_OPULENCE,
  ESSENCE_OPULENCE,
  GREATER_ESSENCE_OPULENCE,
  LESSER_ESSENCE_INFINITE,
  ESSENCE_INFINITE,
  GREATER_ESSENCE_INFINITE,
  VAAL_ARMOURERS_INFUSER,
  VAAL_BLACKSMITHS_INFUSER,
  VAAL_ARCANISTS_INFUSER,
  VAAL_CATALYSING_INFUSER,
  ARMOURERS_SCRAP,
  BLACKSMITHS_WHETSTONE,
  PROTECTIVE_ALLOY,
  CELESTIAL_ALLOY,
  TRANSCENDENT_ALLOY,
  THE_RUNEBINDERS_ALLOY,
  THE_RUNEFATHERS_ALLOY;

  private WorkbenchDefinitions.Currency definition() {
    return WorkbenchDefinitions.active().currency(this);
  }

  public List<String> essenceModifierIds() {
    return definition().essenceModifierIds();
  }

  public String essenceChoiceSource() {
    return definition().essenceChoiceSource();
  }

  public String fixedModifierId() {
    return definition().fixedModifierId();
  }

  public CraftingAction baseAction() {
    return definition().baseAction();
  }

  public int minimumModifierLevel() {
    return definition().minimumModifierLevel();
  }

  public List<String> replacementEssenceModifiers() {
    return isAlloy() || isLiquid() ? List.of() : replacementModifiers();
  }

  public WorkbenchDefinitions.MaterialPolicy materialPolicy() {
    return definition().materialPolicy();
  }

  public boolean isLiquid() {
    return materialPolicy().kind() == WorkbenchDefinitions.MaterialKind.LIQUID;
  }

  public boolean isAlloy() {
    return materialPolicy().kind() == WorkbenchDefinitions.MaterialKind.ALLOY;
  }

  public List<String> replacementModifiers() {
    return definition().replacementModifiers();
  }

  public String replacementEssenceSource() {
    var source = definition().replacementEssenceSource();
    if (source == null) throw new IllegalArgumentException("Not a supported replacement essence");
    return source;
  }

  public CatalystQuality.Type catalystType() {
    String id = name().replaceFirst("^REFINED_", "");
    return id.startsWith("CATALYST_") ? CatalystQuality.Type.valueOf(id.substring(9)) : null;
  }

  public boolean refinedCatalyst() {
    return name().startsWith("REFINED_CATALYST_");
  }

  public String replacementSource() {
    var source = definition().replacementSource();
    if (source == null) throw new IllegalArgumentException("Not a supported replacement essence");
    return source;
  }
}
