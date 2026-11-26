package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.CatalystQuality;
import java.util.List;

/**
 * 공개 화폐 실행 ID와 JSON 규칙을 연결한다. enum의 선언 순서는 API 순서이며 삭제·재배열하지 않는다.
 *
 * <p>공개 화폐 실행 ID와 JSON 규칙을 연결한다. enum의 선언 순서는 API 순서이며 삭제·재배열하지 않는다.
 *
 * <p>enum 값은 이름 자체가 영속 식별자다. *_LIQUID_*는 감정액 종류/농도(ANCIENT_는 고대형), CATALYST_* /REFINED_CATALYST_*는
 * 촉매 종류·정제형, LESSER_ESSENCE_* /ESSENCE_* /GREATER_ESSENCE_* / PERFECT_ESSENCE_*는 에센스 종류·등급,
 * *_ALLOY는 합금 종류다. TRANSMUTATION/AUGMENTATION/REGAL/ EXALTED/ANNULMENT/CHAOS와 GREATER_/PERFECT_형은
 * 변환/확장/제왕/고귀/소멸/혼돈 행동이다. ARTIFICER는 소켓 확장, DIVINE은 수치 재굴림, ALCHEMY는 연금, FRACTURING은 분열,
 * VAAL_*_INFUSER는 품질 주입, ARMOURERS_SCRAP/BLACKSMITHS_WHETSTONE/ARCANISTS_ETCHER는 장비 품질 재료다. 각 개별
 * ID의 실제 후보·수치·출처는 definitions JSON에서 조회한다. enum에 별도 인스턴스 필드는 없다.
 *
 * <p>definition은 초기화된 WorkbenchDefinitions의 이 ID를 찾는다. essenceModifierIds/fixedModifierId는 추가 후보/단일
 * 후보, essenceChoiceSource는 후보 선택 근거다. baseAction/minimumModifierLevel은 기본 알고리즘/최소 옵션 레벨이다.
 * materialPolicy는 분류·적용 정책이고 isLiquid/isAlloy는 그 분류를 읽는다. replacementModifiers는 전체 교체 후보,
 * replacementEssenceModifiers는 에센스 교체 후보만 반환한다. replacementEssenceSource/replacementSource는 근거
 * URL이며 해당 동작이 아니면 IllegalArgumentException이다. catalystType은 촉매 ID의 종류(그 외 null), refinedCatalyst는
 * 정제형 여부를 판정한다. 모든 메서드는 읽기 전용이며 active 정의 초기화가 선행되어야 한다.
 *
 * <p>필드·상수·열거값:
 *
 * <ul>
 *   <li>{@code ANCIENT_DILUTED_LIQUID_IRE}: IRE 구분값.
 *   <li>{@code ANCIENT_DILUTED_LIQUID_GUILT}: GUILT 구분값.
 *   <li>{@code ANCIENT_DILUTED_LIQUID_GREED}: GREED 구분값.
 *   <li>{@code ANCIENT_LIQUID_PARANOIA}: PARANOIA 구분값.
 *   <li>{@code ANCIENT_LIQUID_ENVY}: ENVY 구분값.
 *   <li>{@code ANCIENT_LIQUID_DISGUST}: DISGUST 구분값.
 *   <li>{@code ANCIENT_LIQUID_DESPAIR}: DESPAIR 구분값.
 *   <li>{@code ANCIENT_CONCENTRATED_LIQUID_FEAR}: FEAR 구분값.
 *   <li>{@code ANCIENT_CONCENTRATED_LIQUID_SUFFERING}: SUFFERING 구분값.
 *   <li>{@code ANCIENT_CONCENTRATED_LIQUID_ISOLATION}: ISOLATION 구분값.
 *   <li>{@code ANCIENT_POTENT_LIQUID_MELANCHOLY}: MELANCHOLY 구분값.
 *   <li>{@code ANCIENT_POTENT_LIQUID_FEROCITY}: FEROCITY 구분값.
 *   <li>{@code ANCIENT_POTENT_LIQUID_CONTEMPT}: CONTEMPT 구분값.
 *   <li>{@code POTENT_LIQUID_MELANCHOLY}: MELANCHOLY 구분값.
 *   <li>{@code POTENT_LIQUID_FEROCITY}: FEROCITY 구분값.
 *   <li>{@code POTENT_LIQUID_CONTEMPT}: CONTEMPT 구분값.
 *   <li>{@code DILUTED_LIQUID_IRE}: IRE 구분값.
 *   <li>{@code DILUTED_LIQUID_GUILT}: GUILT 구분값.
 *   <li>{@code DILUTED_LIQUID_GREED}: GREED 구분값.
 *   <li>{@code LIQUID_PARANOIA}: PARANOIA 구분값.
 *   <li>{@code LIQUID_ENVY}: ENVY 구분값.
 *   <li>{@code LIQUID_DISGUST}: DISGUST 구분값.
 *   <li>{@code LIQUID_DESPAIR}: DESPAIR 구분값.
 *   <li>{@code CONCENTRATED_LIQUID_FEAR}: FEAR 구분값.
 *   <li>{@code CONCENTRATED_LIQUID_SUFFERING}: SUFFERING 구분값.
 *   <li>{@code CONCENTRATED_LIQUID_ISOLATION}: ISOLATION 구분값.
 *   <li>{@code CATALYST_FLESH}: FLESH 구분값.
 *   <li>{@code REFINED_CATALYST_FLESH}: FLESH 구분값.
 *   <li>{@code CATALYST_NEURAL}: NEURAL 구분값.
 *   <li>{@code REFINED_CATALYST_NEURAL}: NEURAL 구분값.
 *   <li>{@code CATALYST_CARAPACE}: CARAPACE 구분값.
 *   <li>{@code REFINED_CATALYST_CARAPACE}: CARAPACE 구분값.
 *   <li>{@code CATALYST_UUL_NETOL}: NETOL 구분값.
 *   <li>{@code REFINED_CATALYST_UUL_NETOL}: NETOL 구분값.
 *   <li>{@code CATALYST_XOPH}: XOPH 구분값.
 *   <li>{@code REFINED_CATALYST_XOPH}: XOPH 구분값.
 *   <li>{@code CATALYST_TUL}: TUL 구분값.
 *   <li>{@code REFINED_CATALYST_TUL}: TUL 구분값.
 *   <li>{@code CATALYST_ESH}: ESH 구분값.
 *   <li>{@code REFINED_CATALYST_ESH}: ESH 구분값.
 *   <li>{@code CATALYST_CHAYULA}: CHAYULA 구분값.
 *   <li>{@code REFINED_CATALYST_CHAYULA}: CHAYULA 구분값.
 *   <li>{@code CATALYST_REAVER}: REAVER 구분값.
 *   <li>{@code REFINED_CATALYST_REAVER}: REAVER 구분값.
 *   <li>{@code CATALYST_SIBILANT}: SIBILANT 구분값.
 *   <li>{@code REFINED_CATALYST_SIBILANT}: SIBILANT 구분값.
 *   <li>{@code CATALYST_SKITTERING}: SKITTERING 구분값.
 *   <li>{@code REFINED_CATALYST_SKITTERING}: SKITTERING 구분값.
 *   <li>{@code CATALYST_ADAPTIVE}: ADAPTIVE 구분값.
 *   <li>{@code REFINED_CATALYST_ADAPTIVE}: ADAPTIVE 구분값.
 *   <li>{@code CATALYST_NECROTIC}: NECROTIC 구분값.
 *   <li>{@code REFINED_CATALYST_NECROTIC}: NECROTIC 구분값.
 *   <li>{@code PERFECT_ESSENCE_MIND}: 정신 구분값.
 *   <li>{@code PERFECT_ESSENCE_THAWING}: THAWING 구분값.
 *   <li>{@code PERFECT_ESSENCE_INSULATION}: INSULATION 구분값.
 *   <li>{@code PERFECT_ESSENCE_COMMAND}: 명령 구분값.
 *   <li>{@code LESSER_ESSENCE_COMMAND}: 명령 구분값.
 *   <li>{@code ESSENCE_COMMAND}: 명령 구분값.
 *   <li>{@code GREATER_ESSENCE_COMMAND}: 명령 구분값.
 *   <li>{@code PERFECT_ESSENCE_BODY}: 본문 구분값.
 *   <li>{@code PERFECT_ESSENCE_RUIN}: 파멸 구분값.
 *   <li>{@code PERFECT_ESSENCE_SEEKING}: SEEKING 구분값.
 *   <li>{@code LESSER_ESSENCE_ABRASION}: ABRASION 구분값.
 *   <li>{@code ESSENCE_ABRASION}: ABRASION 구분값.
 *   <li>{@code GREATER_ESSENCE_ABRASION}: ABRASION 구분값.
 *   <li>{@code LESSER_ESSENCE_FLAMES}: FLAMES 구분값.
 *   <li>{@code ESSENCE_FLAMES}: FLAMES 구분값.
 *   <li>{@code GREATER_ESSENCE_FLAMES}: FLAMES 구분값.
 *   <li>{@code LESSER_ESSENCE_ICE}: ICE 구분값.
 *   <li>{@code ESSENCE_ICE}: ICE 구분값.
 *   <li>{@code GREATER_ESSENCE_ICE}: ICE 구분값.
 *   <li>{@code LESSER_ESSENCE_ELECTRICITY}: ELECTRICITY 구분값.
 *   <li>{@code ESSENCE_ELECTRICITY}: ELECTRICITY 구분값.
 *   <li>{@code GREATER_ESSENCE_ELECTRICITY}: ELECTRICITY 구분값.
 *   <li>{@code LESSER_ESSENCE_BATTLE}: BATTLE 구분값.
 *   <li>{@code ESSENCE_BATTLE}: BATTLE 구분값.
 *   <li>{@code LESSER_ESSENCE_HASTE}: HASTE 구분값.
 *   <li>{@code ESSENCE_HASTE}: HASTE 구분값.
 *   <li>{@code GREATER_ESSENCE_HASTE}: HASTE 구분값.
 *   <li>{@code LESSER_ESSENCE_SEEKING}: SEEKING 구분값.
 *   <li>{@code ESSENCE_SEEKING}: SEEKING 구분값.
 *   <li>{@code GREATER_ESSENCE_SEEKING}: SEEKING 구분값.
 *   <li>{@code ARTIFICER}: ARTIFICER 구분값.
 *   <li>{@code TRANSMUTATION}: 진화 구분값.
 *   <li>{@code GREATER_TRANSMUTATION}: 진화 구분값.
 *   <li>{@code PERFECT_TRANSMUTATION}: 진화 구분값.
 *   <li>{@code AUGMENTATION}: 확장 구분값.
 *   <li>{@code GREATER_AUGMENTATION}: 확장 구분값.
 *   <li>{@code PERFECT_AUGMENTATION}: 확장 구분값.
 *   <li>{@code REGAL}: 제왕 구분값.
 *   <li>{@code GREATER_REGAL}: 제왕 구분값.
 *   <li>{@code PERFECT_REGAL}: 제왕 구분값.
 *   <li>{@code EXALTED}: 엑잘티드 구분값.
 *   <li>{@code GREATER_EXALTED}: 엑잘티드 구분값.
 *   <li>{@code PERFECT_EXALTED}: 엑잘티드 구분값.
 *   <li>{@code ANNULMENT}: 소멸 구분값.
 *   <li>{@code CHAOS}: 카오스 구분값.
 *   <li>{@code GREATER_CHAOS}: 카오스 구분값.
 *   <li>{@code PERFECT_CHAOS}: 카오스 구분값.
 *   <li>{@code DIVINE}: 신성 구분값.
 *   <li>{@code ALCHEMY}: 연금 구분값.
 *   <li>{@code FRACTURING}: 분열 구분값.
 *   <li>{@code ESSENCE_HYSTERIA}: HYSTERIA 구분값.
 *   <li>{@code ESSENCE_HORROR}: HORROR 구분값.
 *   <li>{@code PERFECT_ESSENCE_SORCERY}: SORCERY 구분값.
 *   <li>{@code PERFECT_ESSENCE_ALACRITY}: ALACRITY 구분값.
 *   <li>{@code PERFECT_ESSENCE_ABRASION}: ABRASION 구분값.
 *   <li>{@code PERFECT_ESSENCE_FLAMES}: FLAMES 구분값.
 *   <li>{@code PERFECT_ESSENCE_ICE}: ICE 구분값.
 *   <li>{@code PERFECT_ESSENCE_ELECTRICITY}: ELECTRICITY 구분값.
 *   <li>{@code PERFECT_ESSENCE_BATTLE}: BATTLE 구분값.
 *   <li>{@code PERFECT_ESSENCE_HASTE}: HASTE 구분값.
 *   <li>{@code PERFECT_ESSENCE_GROUNDING}: GROUNDING 구분값.
 *   <li>{@code PERFECT_ESSENCE_OPULENCE}: 풍요 구분값.
 *   <li>{@code PERFECT_ESSENCE_INFINITE}: 무한 구분값.
 *   <li>{@code PERFECT_ESSENCE_ENHANCEMENT}: 강화 구분값.
 *   <li>{@code ESSENCE_BREACH}: BREACH 구분값.
 *   <li>{@code RUNIC_ALLOY}: 합금 구분값.
 *   <li>{@code PRISMATIC_ALLOY}: 합금 구분값.
 *   <li>{@code ADAPTIVE_ALLOY}: 합금 구분값.
 *   <li>{@code SWIFT_ALLOY}: 합금 구분값.
 *   <li>{@code SOVEREIGN_ALLOY}: 합금 구분값.
 *   <li>{@code EXPANSIVE_ALLOY}: 합금 구분값.
 *   <li>{@code CYCLONIC_ALLOY}: 합금 구분값.
 *   <li>{@code MYSTIC_ALLOY}: 합금 구분값.
 *   <li>{@code ESSENCE_ABYSS}: 훼손 구분값.
 *   <li>{@code LESSER_ESSENCE_SORCERY}: SORCERY 구분값.
 *   <li>{@code ESSENCE_SORCERY}: SORCERY 구분값.
 *   <li>{@code GREATER_ESSENCE_SORCERY}: SORCERY 구분값.
 *   <li>{@code LESSER_ESSENCE_ALACRITY}: ALACRITY 구분값.
 *   <li>{@code ESSENCE_ALACRITY}: ALACRITY 구분값.
 *   <li>{@code GREATER_ESSENCE_ALACRITY}: ALACRITY 구분값.
 *   <li>{@code LESSER_ESSENCE_ENHANCEMENT}: 강화 구분값.
 *   <li>{@code ESSENCE_ENHANCEMENT}: 강화 구분값.
 *   <li>{@code GREATER_ESSENCE_ENHANCEMENT}: 강화 구분값.
 *   <li>{@code GREATER_ESSENCE_BATTLE}: BATTLE 구분값.
 *   <li>{@code LESSER_ESSENCE_BODY}: 본문 구분값.
 *   <li>{@code ESSENCE_BODY}: 본문 구분값.
 *   <li>{@code GREATER_ESSENCE_BODY}: 본문 구분값.
 *   <li>{@code LESSER_ESSENCE_MIND}: 정신 구분값.
 *   <li>{@code ESSENCE_MIND}: 정신 구분값.
 *   <li>{@code GREATER_ESSENCE_MIND}: 정신 구분값.
 *   <li>{@code LESSER_ESSENCE_RUIN}: 파멸 구분값.
 *   <li>{@code ESSENCE_RUIN}: 파멸 구분값.
 *   <li>{@code GREATER_ESSENCE_RUIN}: 파멸 구분값.
 *   <li>{@code LESSER_ESSENCE_INSULATION}: INSULATION 구분값.
 *   <li>{@code ESSENCE_INSULATION}: INSULATION 구분값.
 *   <li>{@code GREATER_ESSENCE_INSULATION}: INSULATION 구분값.
 *   <li>{@code LESSER_ESSENCE_THAWING}: THAWING 구분값.
 *   <li>{@code ESSENCE_THAWING}: THAWING 구분값.
 *   <li>{@code GREATER_ESSENCE_THAWING}: THAWING 구분값.
 *   <li>{@code LESSER_ESSENCE_GROUNDING}: GROUNDING 구분값.
 *   <li>{@code ESSENCE_GROUNDING}: GROUNDING 구분값.
 *   <li>{@code GREATER_ESSENCE_GROUNDING}: GROUNDING 구분값.
 *   <li>{@code LESSER_ESSENCE_OPULENCE}: 풍요 구분값.
 *   <li>{@code ESSENCE_OPULENCE}: 풍요 구분값.
 *   <li>{@code GREATER_ESSENCE_OPULENCE}: 풍요 구분값.
 *   <li>{@code LESSER_ESSENCE_INFINITE}: 무한 구분값.
 *   <li>{@code ESSENCE_INFINITE}: 무한 구분값.
 *   <li>{@code GREATER_ESSENCE_INFINITE}: 무한 구분값.
 *   <li>{@code VAAL_ARMOURERS_INFUSER}: INFUSER 구분값.
 *   <li>{@code VAAL_BLACKSMITHS_INFUSER}: INFUSER 구분값.
 *   <li>{@code VAAL_ARCANISTS_INFUSER}: INFUSER 구분값.
 *   <li>{@code VAAL_CATALYSING_INFUSER}: INFUSER 구분값.
 *   <li>{@code ARMOURERS_SCRAP}: SCRAP 구분값.
 *   <li>{@code BLACKSMITHS_WHETSTONE}: 무술 무기 품질용 숫돌.
 *   <li>{@code ARCANISTS_ETCHER}: 마법봉·지팡이·셉터 품질용 식각기.
 *   <li>{@code PROTECTIVE_ALLOY}: 합금 구분값.
 *   <li>{@code CELESTIAL_ALLOY}: 합금 구분값.
 *   <li>{@code TRANSCENDENT_ALLOY}: 합금 구분값.
 *   <li>{@code THE_RUNEBINDERS_ALLOY}: 합금 구분값.
 *   <li>{@code THE_RUNEFATHERS_ALLOY}: 합금 구분값.
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code definition()}: 정의 값을 조회·구성한다. 반환: {@code WorkbenchDefinitions.Currency}.
 *   <li>{@code essenceModifierIds()}: 에센스·옵션·식별자목록 값을 조회·구성한다. 반환: {@code List<String>}.
 *   <li>{@code essenceChoiceSource()}: 에센스·선택·출처 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code fixedModifierId()}: 고정·옵션·식별자 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code baseAction()}: 베이스·행동 값을 조회·구성한다. 반환: {@code CraftingAction}.
 *   <li>{@code minimumModifierLevel()}: 최소값·옵션·레벨 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code replacementEssenceModifiers()}: 교체·에센스·옵션목록 값을 조회·구성한다. 반환: {@code List<String>}.
 *   <li>{@code materialPolicy()}: 제작재료·정책 값을 조회·구성한다. 반환: {@code
 *       WorkbenchDefinitions.MaterialPolicy}.
 *   <li>{@code isLiquid()}: 감정액을(를) 여부를 판정한다. 반환: {@code boolean}.
 *   <li>{@code isAlloy()}: 합금을(를) 여부를 판정한다. 반환: {@code boolean}.
 *   <li>{@code replacementModifiers()}: 교체·옵션목록 값을 조회·구성한다. 반환: {@code List<String>}.
 *   <li>{@code replacementEssenceSource()}: 교체·에센스·출처 값을 조회·구성한다. 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code catalystType()}: 기폭제·타입 값을 조회·구성한다. 반환: {@code CatalystQuality.Type}.
 *   <li>{@code refinedCatalyst()}: refined·기폭제 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code replacementSource()}: 교체·출처 값을 조회·구성한다. 반환: {@code String}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
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
  ARCANISTS_ETCHER,
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
