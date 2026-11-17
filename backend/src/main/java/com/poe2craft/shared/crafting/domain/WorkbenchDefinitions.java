package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;

/**
 * JSON에서 읽은 제작 정의를 검증하고 프로세스 전체에 하나의 불변 규칙 묶음을 제공한다.
 *
 * <p>document는 원본의 불변 투영, currencies/omens는 enum 키의 조회 인덱스다. active는 volatile로 공개하는 현재 규칙이며
 * initialize의 동기화로 다른 묶음의 재등록을 거절한다. active()는 초기화 전 IllegalStateException을 던진다. NO_TARGETS는 베이스별
 * 대상이 없을 때 공유하는 빈 값이다.
 *
 * <p>생성자는 버전·enum 전체 목록/순서·중복·대상 모드·출처·실제 구현된 화폐와 징조 메커니즘 경계를 검사한다. currencyMap/omenMap은 검사 중 인덱스,
 * tiered와 implementedTrigger는 구현된 징조 조합이다. ids는 빈/중복 옵션 ID, url은 근거 URL 형식을 검사한다.
 * validateTargets(known)는 실제 카탈로그에 존재하는 명시 옵션인지 확인한다. 잘못된 문서는 IllegalArgumentException으로 거절한다.
 * document/currency/omen은 불변 값을 반환하고 targets(baseId)는 베이스별 추가·교체 후보를 반환한다.
 *
 * <p>중첩 타입: Currency는 화폐, Omen은 징조, MaterialKind/MaterialMode는 코드가 지원하는 유한한 제작 분류·알고리즘,
 * MaterialPolicy는 패치 가능한 적용 희귀도, BaseTargets는 베이스 후보, Document는 직렬화 루트다. 게임 수치·후보는 JSON에 두고 임의
 * 표현식이나 코드를 실행하지 않는다.
 */
public final class WorkbenchDefinitions {
  /**
   * 화폐 정의다. id는 실행 ID, baseAction은 기본 알고리즘(없으면 null), minimumModifierLevel은 최소 옵션 레벨,
   * fixedModifierId는 단일 추가 후보, essenceModifierIds는 추가 후보, essenceChoiceSource는 복수 후보 근거,
   * replacementModifiers는 교체 후보, replacementEssenceSource/replacementSource는 에센스·교체 동작 근거,
   * materialPolicy는 재료 분류·희귀도 정책이다. 생성자는 정책 누락을 거절하고 후보 목록을 방어 복사한다.
   */
  public record Currency(
      WorkbenchCurrency id,
      CraftingAction baseAction,
      int minimumModifierLevel,
      String fixedModifierId,
      List<String> essenceModifierIds,
      String essenceChoiceSource,
      List<String> replacementModifiers,
      String replacementEssenceSource,
      String replacementSource,
      MaterialPolicy materialPolicy) {
    public Currency {
      Objects.requireNonNull(materialPolicy, "Material policy required");
      essenceModifierIds = List.copyOf(essenceModifierIds);
      replacementModifiers = List.copyOf(replacementModifiers);
    }
  }

  /**
   * key는 코드 enum, id는 공개 식별자, trigger는 적용 화폐, affix는 접두/접미 제한(없으면 null), supportsTieredCurrency는 상위
   * 화폐 지원 여부다.
   */
  public record Omen(
      WorkbenchOmen key,
      String id,
      WorkbenchCurrency trigger,
      AffixType affix,
      boolean supportsTieredCurrency) {}

  /** OTHER는 별도 기본 동작, ESSENCE는 에센스, ALLOY는 합금, LIQUID는 감정액이다. 표시 이름이 아닌 실행 분류다. */
  public enum MaterialKind {
    OTHER,
    ESSENCE,
    ALLOY,
    LIQUID
  }

  /** NONE은 전용 후보 없음, ADD는 보장 옵션 추가, REPLACE는 기존 옵션 제거 후 교체다. 새 메커니즘은 Java 구현을 함께 추가한다. */
  public enum MaterialMode {
    NONE,
    ADD,
    REPLACE
  }

  /**
   * kind/mode는 실행 분류·알고리즘, inputRarities는 허용 입력 희귀도, resultRarity는 추가 후 희귀도다. 교체는 기존 희귀도를 유지하므로
   * resultRarity=null이다. 생성자는 중복·빈 입력·모드 불일치를 거절한다. allows(state)는 희귀도 허용 여부, result(state)는 적용 후
   * 희귀도를 반환하며 상태를 변경하지 않는다.
   */
  public record MaterialPolicy(
      MaterialKind kind,
      MaterialMode mode,
      List<ItemState.Rarity> inputRarities,
      ItemState.Rarity resultRarity) {
    public MaterialPolicy {
      Objects.requireNonNull(kind);
      Objects.requireNonNull(mode);
      inputRarities = List.copyOf(inputRarities);
      if ((kind == MaterialKind.OTHER) != (mode == MaterialMode.NONE)
          || (kind != MaterialKind.ESSENCE && mode == MaterialMode.ADD)
          || new HashSet<>(inputRarities).size() != inputRarities.size()
          || (mode == MaterialMode.NONE
              ? !inputRarities.isEmpty() || resultRarity != null
              : inputRarities.isEmpty())
          || (mode == MaterialMode.ADD && resultRarity == null)
          || (mode == MaterialMode.REPLACE && resultRarity != null))
        throw new IllegalArgumentException("Invalid material execution policy");
    }

    public boolean allows(ItemState state) {
      return inputRarities.contains(state.rarity());
    }

    public ItemState.Rarity result(ItemState state) {
      return resultRarity == null ? state.rarity() : resultRarity;
    }
  }

  /**
   * fixed/replacements는 화폐별 추가/교체 후보다. 생성자는 copyTargets로 중첩 목록까지 불변 복사한다. copyTargets는 enum 순서를
   * 유지하며 빈 목록·중복·빈 ID를 거절한다. JSON 수정 시 후보 수 자체를 제한하지 않는다.
   */
  public record BaseTargets(
      Map<WorkbenchCurrency, List<String>> fixed,
      Map<WorkbenchCurrency, List<String>> replacements) {
    public BaseTargets {
      fixed = copyTargets(fixed);
      replacements = copyTargets(replacements);
    }

    private static Map<WorkbenchCurrency, List<String>> copyTargets(
        Map<WorkbenchCurrency, List<String>> source) {
      var copy = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
      source.forEach(
          (action, ids) -> {
            ids(ids);
            if (ids.isEmpty()) throw new IllegalArgumentException("Empty base target set");
            copy.put(action, List.copyOf(ids));
          });
      return Collections.unmodifiableMap(copy);
    }
  }

  private static final BaseTargets NO_TARGETS = new BaseTargets(Map.of(), Map.of());

  /**
   * schemaVersion/ruleVersion은 문서 계약, currencies/omens는 순서 있는 실행 정의, baseTargets는 베이스 ID별 후보다. 기본
   * 생성자는 모든 컬렉션을 불변 복사한다. 4인자 생성자는 베이스 후보가 없던 기존 Java 호출의 호환 진입점이며 빈 map을 사용한다.
   */
  public record Document(
      int schemaVersion,
      String ruleVersion,
      List<Currency> currencies,
      List<Omen> omens,
      Map<String, BaseTargets> baseTargets) {
    public Document {
      currencies = List.copyOf(currencies);
      omens = List.copyOf(omens);
      baseTargets = Map.copyOf(baseTargets);
    }

    public Document(
        int schemaVersion, String ruleVersion, List<Currency> currencies, List<Omen> omens) {
      this(schemaVersion, ruleVersion, currencies, omens, Map.of());
    }
  }

  private final Document document;
  private final Map<WorkbenchCurrency, Currency> currencies;
  private final Map<WorkbenchOmen, Omen> omens;
  private static volatile WorkbenchDefinitions active;

  public WorkbenchDefinitions(Document document) {
    this.document = Objects.requireNonNull(document);
    if (document.schemaVersion() != 1
        || !"workbench-static-definitions-v1".equals(document.ruleVersion()))
      throw new IllegalArgumentException("Unsupported Workbench definitions version");
    var currencyMap = new EnumMap<WorkbenchCurrency, Currency>(WorkbenchCurrency.class);
    for (var c : document.currencies()) {
      if (c.id() == null || currencyMap.putIfAbsent(c.id(), c) != null)
        throw new IllegalArgumentException("Missing or duplicate currency definition");
      if (c.minimumModifierLevel() < 0 || (c.baseAction() == null && c.minimumModifierLevel() != 0))
        throw new IllegalArgumentException("Invalid currency modifier level");
      String ordinary = c.id().name().replaceFirst("^(GREATER_|PERFECT_)", "");
      CraftingAction expected =
          Arrays.stream(CraftingAction.values())
              .filter(a -> a.name().equals(ordinary))
              .findFirst()
              .orElse(null);
      if (c.baseAction() != expected)
        throw new IllegalArgumentException(
            "Currency operation is outside its implemented boundary");
      if ((!c.essenceModifierIds().isEmpty() && c.materialPolicy().mode() != MaterialMode.ADD)
          || (!c.replacementModifiers().isEmpty()
              && c.materialPolicy().mode() != MaterialMode.REPLACE))
        throw new IllegalArgumentException(
            "Currency targets do not match their execution mechanism");
      ids(c.essenceModifierIds());
      ids(c.replacementModifiers());
      if (c.fixedModifierId() != null
          && !c.essenceModifierIds().equals(List.of(c.fixedModifierId())))
        throw new IllegalArgumentException("Fixed essence target differs");
      if ((!c.essenceModifierIds().isEmpty() && c.materialPolicy().kind() != MaterialKind.ESSENCE)
          || (!c.essenceModifierIds().isEmpty() && !c.replacementModifiers().isEmpty())
          || (c.essenceModifierIds().size() > 1
              && (c.fixedModifierId() != null || c.essenceChoiceSource() == null))
          || (c.essenceModifierIds().size() == 1 && c.fixedModifierId() == null)
          || (c.essenceModifierIds().size() <= 1 && c.essenceChoiceSource() != null))
        throw new IllegalArgumentException("Invalid essence target mode");
      boolean replacementKind = c.materialPolicy().mode() == MaterialMode.REPLACE;
      boolean essenceReplacement =
          c.materialPolicy().kind() == MaterialKind.ESSENCE && !c.replacementModifiers().isEmpty();
      if ((!c.replacementModifiers().isEmpty()
              && (!replacementKind || c.replacementSource() == null))
          || ((c.replacementEssenceSource() != null) != essenceReplacement)
          || (c.replacementEssenceSource() != null
              && ((c.materialPolicy().kind() == MaterialKind.LIQUID)
                  || (c.materialPolicy().kind() == MaterialKind.ALLOY)
                  || c.replacementModifiers().isEmpty())))
        throw new IllegalArgumentException(
            "Replacement definition is outside its implemented boundary");
      url(c.essenceChoiceSource());
      url(c.replacementEssenceSource());
      url(c.replacementSource());
    }
    if (!document.currencies().stream()
        .map(Currency::id)
        .toList()
        .equals(List.of(WorkbenchCurrency.values())))
      throw new IllegalArgumentException(
          "Currency definitions must cover enum identities in API order");
    var omenMap = new EnumMap<WorkbenchOmen, Omen>(WorkbenchOmen.class);
    var omenIds = new HashSet<String>();
    var tiered =
        Set.of(
            WorkbenchOmen.SINISTRAL_CORONATION,
            WorkbenchOmen.DEXTRAL_CORONATION,
            WorkbenchOmen.SINISTRAL_EXALTATION,
            WorkbenchOmen.DEXTRAL_EXALTATION,
            WorkbenchOmen.SINISTRAL_ERASURE,
            WorkbenchOmen.DEXTRAL_ERASURE,
            WorkbenchOmen.WHITTLING,
            WorkbenchOmen.HOMOGENISING_EXALTATION,
            WorkbenchOmen.HOMOGENISING_CORONATION);
    for (var o : document.omens()) {
      if (o.key() == null
          || o.id() == null
          || !o.id().matches("Omen_of_[A-Za-z_]+")
          || !omenIds.add(o.id())
          || omenMap.putIfAbsent(o.key(), o) != null
          || !currencyMap.containsKey(o.trigger()))
        throw new IllegalArgumentException("Missing, duplicate or unknown omen identity/trigger");
      AffixType expected =
          o.key().name().startsWith("SINISTRAL_")
              ? AffixType.PREFIX
              : o.key().name().startsWith("DEXTRAL_") ? AffixType.SUFFIX : null;
      if (o.affix() != expected || (o.supportsTieredCurrency() && !tiered.contains(o.key())))
        throw new IllegalArgumentException("Omen applicability is outside its reviewed boundary");
      // Each effect is implemented on a specific operation, not every globally known trigger.
      WorkbenchCurrency implementedTrigger =
          switch (o.key()) {
            case SINISTRAL_ALCHEMY, DEXTRAL_ALCHEMY -> WorkbenchCurrency.ALCHEMY;
            case SINISTRAL_CORONATION, DEXTRAL_CORONATION, HOMOGENISING_CORONATION ->
                WorkbenchCurrency.REGAL;
            case GREATER_ANNULMENT, SINISTRAL_ANNULMENT, DEXTRAL_ANNULMENT, LIGHT ->
                WorkbenchCurrency.ANNULMENT;
            case SINISTRAL_CRYSTALLISATION, DEXTRAL_CRYSTALLISATION ->
                WorkbenchCurrency.ESSENCE_HYSTERIA;
            case GREATER_EXALTATION,
                HOMOGENISING_EXALTATION,
                SINISTRAL_EXALTATION,
                DEXTRAL_EXALTATION ->
                WorkbenchCurrency.EXALTED;
            case SINISTRAL_ERASURE, DEXTRAL_ERASURE, WHITTLING -> WorkbenchCurrency.CHAOS;
            case BLESSED, SANCTIFICATION -> WorkbenchCurrency.DIVINE;
          };
      if (o.trigger() != implementedTrigger)
        throw new IllegalArgumentException(
            "Omen trigger is outside its implemented effect boundary");
    }
    if (!document.omens().stream().map(Omen::key).toList().equals(List.of(WorkbenchOmen.values())))
      throw new IllegalArgumentException(
          "Omen definitions must cover enum identities in API order");
    currencies = Collections.unmodifiableMap(currencyMap);
    omens = Collections.unmodifiableMap(omenMap);
  }

  private static void ids(List<String> ids) {
    if (new HashSet<>(ids).size() != ids.size() || ids.stream().anyMatch(i -> i.isBlank()))
      throw new IllegalArgumentException("Blank or duplicate modifier target");
  }

  private static void url(String value) {
    if (value != null && !value.matches("https://poe2db\\.tw/us/[A-Za-z0-9_]+"))
      throw new IllegalArgumentException("Invalid reviewed currency source URL");
  }

  public BaseTargets targets(String baseId) {
    return document.baseTargets().getOrDefault(baseId, NO_TARGETS);
  }

  public Document document() {
    return document;
  }

  public Currency currency(WorkbenchCurrency id) {
    return currencies.get(Objects.requireNonNull(id));
  }

  public Omen omen(WorkbenchOmen id) {
    return omens.get(Objects.requireNonNull(id));
  }

  public void validateTargets(Map<String, List<ModifierDefinition>> known) {
    document
        .baseTargets()
        .forEach(
            (base, targets) -> {
              if (base.isBlank())
                throw new IllegalArgumentException("Missing base target identity");
              for (var row : targets.fixed().entrySet()) {
                if (currency(row.getKey()).materialPolicy().mode() != MaterialMode.ADD)
                  throw new IllegalArgumentException(
                      "Fixed target action has no addition mechanism");
                for (var id : row.getValue())
                  if (!known.containsKey(id)
                      || known.get(id).stream()
                          .noneMatch(
                              d ->
                                  d.layer() == ModifierDefinition.Layer.EXPLICIT && d.weight() > 0))
                    throw new IllegalArgumentException("Invalid fixed base target: " + id);
              }
              for (var row : targets.replacements().entrySet()) {
                if (currency(row.getKey()).materialPolicy().mode() != MaterialMode.REPLACE)
                  throw new IllegalArgumentException(
                      "Replacement target action has no replacement mechanism");
                for (var id : row.getValue())
                  if (!known.containsKey(id)
                      || known.get(id).stream()
                          .noneMatch(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT))
                    throw new IllegalArgumentException("Invalid replacement base target: " + id);
              }
            });
    for (var c : document.currencies()) {
      if (!known.keySet().containsAll(c.essenceModifierIds())
          || !known.keySet().containsAll(c.replacementModifiers()))
        throw new IllegalArgumentException("Unknown currency modifier target: " + c.id());
      for (var id : c.essenceModifierIds()) {
        if (known.get(id).stream()
            .noneMatch(
                target ->
                    target.layer() == ModifierDefinition.Layer.EXPLICIT && target.weight() > 0))
          throw new IllegalArgumentException("Fixed essence requires an ordinary explicit target");
      }
      for (var id : c.replacementModifiers()) {
        if (known.get(id).stream()
            .noneMatch(target -> target.layer() == ModifierDefinition.Layer.EXPLICIT))
          throw new IllegalArgumentException("Replacement requires an explicit target");
      }
    }
  }

  public static synchronized WorkbenchDefinitions initialize(WorkbenchDefinitions definitions) {
    Objects.requireNonNull(definitions);
    if (active != null && !active.document.equals(definitions.document))
      throw new IllegalStateException("A different Workbench ruleset is already initialized");
    if (active == null) active = definitions;
    return active;
  }

  public static WorkbenchDefinitions active() {
    var definitions = active;
    if (definitions == null)
      throw new IllegalStateException("Workbench definitions have not been initialized");
    return definitions;
  }
}
