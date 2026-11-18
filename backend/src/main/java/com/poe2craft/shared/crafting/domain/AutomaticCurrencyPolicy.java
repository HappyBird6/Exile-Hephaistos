package com.poe2craft.shared.crafting.domain;

import java.util.*;

/**
 * 사용자가 승인한 탐색 허용 범위를 표현한다. 게임상 실행 가능성과는 별개다.
 *
 * <p>기존 계약: User-approved exploration policy. This never grants game eligibility or probability
 * support.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "approved-187-2026-11-18-first-chaos-v11"} ({@code
 *       String}).
 *   <li>{@code MAXIMUM_EXPLORED_EXPLICITS}: 명시옵션목록; 초기값 {@code 6} ({@code int}).
 *   <li>{@code MANUAL_CURRENCY_POLICY_IDS}: 식별자목록 ({@code Map<String, String>}).
 *   <li>{@code MANUAL_ONLY_CURRENCIES}: 화폐목록 ({@code Set<String>}).
 *   <li>{@code document}: 문서 ({@code Document}).
 *   <li>{@code entries}: 항목목록 ({@code Map<String, Entry>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticCurrencyPolicy(document)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code document}(문서).
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code document()}: {@code document} 값을 조회한다. 반환: {@code Document}.
 *   <li>{@code candidates()}: 후보목록 값을 조회·구성한다. 반환: {@code List<Entry>}.
 *   <li>{@code evaluate(id, context)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code id}(식별자), {@code
 *       context}(문맥). 반환: {@code Verdict}.
 *   <li>{@code withinExplorationBoundary(explicitCount)}: within·Exploration·경계 처리에서 {@code
 *       document.maximumExploredExplicits}을(를) 사용한다. 입력: {@code explicitCount}(명시옵션·개수). 반환: {@code
 *       boolean}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Decision}: {@code AutomaticCurrencyPolicy}의 결정 구분값을 정의한다.
 *   <li>{@code Condition}: {@code AutomaticCurrencyPolicy}의 조건 구분값을 정의한다.
 *   <li>{@code Purpose}: {@code AutomaticCurrencyPolicy}의 목적 구분값을 정의한다.
 *   <li>{@code Verdict}: {@code AutomaticCurrencyPolicy}의 Verdict 구분값을 정의한다.
 *   <li>{@code Entry}: {@code AutomaticCurrencyPolicy}에서 사용하는 항목의 불변 값이다.
 *   <li>{@code Document}: {@code AutomaticCurrencyPolicy}에서 사용하는 문서의 불변 값이다.
 *   <li>{@code Context}: {@code AutomaticCurrencyPolicy}에서 사용하는 문맥의 불변 값이다.
 * </ul>
 */
public final class AutomaticCurrencyPolicy {
  public static final String VERSION = "approved-187-2026-11-18-first-chaos-v11";
  public static final int MAXIMUM_EXPLORED_EXPLICITS = 6;
  // The frozen 187-entry policy already excludes these as supplementary resources.
  // Their new manual enum names must not expand the approved automatic search inventory.
  private static final Map<String, String> MANUAL_CURRENCY_POLICY_IDS =
      Map.of(
          WorkbenchCurrency.ARMOURERS_SCRAP.name(), "EXTRA:Armourers_Scrap",
          WorkbenchCurrency.BLACKSMITHS_WHETSTONE.name(), "EXTRA:Blacksmiths_Whetstone");

  // These additions are available only in the manual workbench; the approved roster stays frozen.
  private static final Set<String> MANUAL_ONLY_CURRENCIES =
      Set.of(
          "VAAL_ARMOURERS_INFUSER",
          "VAAL_BLACKSMITHS_INFUSER",
          "VAAL_ARCANISTS_INFUSER",
          "VAAL_CATALYSING_INFUSER",
          "PROTECTIVE_ALLOY",
          "CELESTIAL_ALLOY",
          "TRANSCENDENT_ALLOY",
          "THE_RUNEBINDERS_ALLOY",
          "THE_RUNEFATHERS_ALLOY");

  /**
   * {@code AutomaticCurrencyPolicy}의 결정 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code USE}: 사용 구분값.
   *   <li>{@code CONDITIONAL}: 조건부 구분값.
   *   <li>{@code EXCLUDED}: EXCLUDED 구분값.
   *   <li>{@code DEFERRED}: 지연계산 구분값.
   * </ul>
   */
  public enum Decision {
    USE,
    CONDITIONAL,
    EXCLUDED,
    DEFERRED
  }

  /**
   * {@code AutomaticCurrencyPolicy}의 조건 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code NONE}: NONE 구분값.
   *   <li>{@code GOAL_ACQUISITION}: ACQUISITION 구분값.
   *   <li>{@code FIRST_AFFIX_ACQUISITION}: ACQUISITION 구분값.
   *   <li>{@code RARITY_UPGRADE}: UPGRADE 구분값.
   *   <li>{@code TEMPORARY_REMOVAL}: 제거 구분값.
   *   <li>{@code HYSTERIA_PARTS}: 부분목록 구분값.
   *   <li>{@code BREACH_QUALITY}: 품질 구분값.
   *   <li>{@code PRISMATIC_PARTS}: 부분목록 구분값.
   *   <li>{@code SWIFT_PARTS}: 부분목록 구분값.
   *   <li>{@code SOVEREIGN_PARTS}: 부분목록 구분값.
   *   <li>{@code EXPANSIVE_PARTS}: 부분목록 구분값.
   *   <li>{@code CYCLONIC_PARTS}: 부분목록 구분값.
   *   <li>{@code MYSTIC_PARTS}: 부분목록 구분값.
   *   <li>{@code TEMPORARY_REGAL_PREFIX}: 접두 구분값.
   *   <li>{@code TEMPORARY_REGAL_SUFFIX}: 접미 구분값.
   *   <li>{@code TEMPORARY_EXALTED_PREFIX}: 접두 구분값.
   *   <li>{@code TEMPORARY_EXALTED_SUFFIX}: 접미 구분값.
   * </ul>
   */
  public enum Condition {
    ANNULMENT,
    NONE,
    GOAL_ACQUISITION,
    FIRST_AFFIX_ACQUISITION,
    RARITY_UPGRADE,
    TEMPORARY_REMOVAL,
    HYSTERIA_PARTS,
    BREACH_QUALITY,
    PRISMATIC_PARTS,
    SWIFT_PARTS,
    SOVEREIGN_PARTS,
    EXPANSIVE_PARTS,
    CYCLONIC_PARTS,
    MYSTIC_PARTS,
    TEMPORARY_REGAL_PREFIX,
    TEMPORARY_REGAL_SUFFIX,
    TEMPORARY_EXALTED_PREFIX,
    TEMPORARY_EXALTED_SUFFIX
  }

  /**
   * {@code AutomaticCurrencyPolicy}의 목적 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code ORDINARY}: 일반 구분값.
   *   <li>{@code RARITY_UPGRADE}: UPGRADE 구분값.
   *   <li>{@code GOAL_ACQUISITION}: ACQUISITION 구분값.
   *   <li>{@code TARGET_PREPARATION}: 준비 구분값.
   *   <li>{@code TEMPORARY_AFFIX_FOR_REMOVAL}: 제거 구분값.
   *   <li>{@code QUALITY_CHECKPOINT}: 체크포인트 구분값.
   *   <li>{@code LAST_RESORT_EXALTED}: 다른 획득 경로가 없을 때 목표 옵션 추가를 시도한다.
   *   <li>{@code REFILL_AFTER_ANNULMENT}: 소멸로 확보한 자리에 목표 옵션을 추가한다.
   *   <li>{@code QUALITY_BASE_CHECKPOINT}: 체크포인트 구분값.
   * </ul>
   */
  public enum Purpose {
    ORDINARY,
    RARITY_UPGRADE,
    GOAL_ACQUISITION,
    TARGET_PREPARATION,
    TEMPORARY_AFFIX_FOR_REMOVAL,
    QUALITY_CHECKPOINT,
    QUALITY_BASE_CHECKPOINT,
    REFILL_AFTER_ANNULMENT,
    LAST_RESORT_EXALTED
  }

  /**
   * {@code AutomaticCurrencyPolicy}의 Verdict 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code PERMITTED}: PERMITTED 구분값.
   *   <li>{@code EXCLUDED}: EXCLUDED 구분값.
   *   <li>{@code DEFERRED}: 지연계산 구분값.
   *   <li>{@code CONDITION_NOT_MET}: MET 구분값.
   *   <li>{@code UNREVIEWED}: UNREVIEWED 구분값.
   * </ul>
   */
  public enum Verdict {
    PERMITTED,
    EXCLUDED,
    DEFERRED,
    CONDITION_NOT_MET,
    UNREVIEWED
  }

  /**
   * {@code AutomaticCurrencyPolicy}에서 사용하는 항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code number}: 숫자 ({@code int}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code nameKo}: 이름·Ko ({@code String}).
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code effectKo}: 효과·Ko ({@code String}).
   *   <li>{@code decision}: 결정 ({@code Decision}).
   *   <li>{@code restrictionKo}: restriction·Ko ({@code String}).
   *   <li>{@code condition}: 조건 ({@code Condition}).
   * </ul>
   */
  public record Entry(
      int number,
      String id,
      String nameKo,
      String sourceUrl,
      String effectKo,
      Decision decision,
      String restrictionKo,
      Condition condition) {}

  /**
   * {@code AutomaticCurrencyPolicy}에서 사용하는 문서의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
   *   <li>{@code policyVersion}: 정책·버전 ({@code String}).
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code sourceModifiedAt}: 출처·Modified·기준 ({@code String}).
   *   <li>{@code maximumExploredExplicits}: 최대값·Explored·명시옵션목록 ({@code int}).
   *   <li>{@code entries}: 항목목록 ({@code List<Entry>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Document(schemaVersion, policyVersion, sourceUrl, sourceModifiedAt,
   *       maximumExploredExplicits, entries)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       schemaVersion}(스키마·버전), {@code policyVersion}(정책·버전), {@code sourceUrl}(출처·출처주소), {@code
   *       sourceModifiedAt}(출처·Modified·기준), {@code maximumExploredExplicits}(최대값·Explored·명시옵션목록),
   *       {@code entries}(항목목록).
   * </ul>
   */
  public record Document(
      int schemaVersion,
      String policyVersion,
      String sourceUrl,
      String sourceModifiedAt,
      int maximumExploredExplicits,
      List<Entry> entries) {
    public Document {
      entries = List.copyOf(entries);
    }
  }

  /**
   * {@code AutomaticCurrencyPolicy}에서 사용하는 문맥의 불변 값이다.
   *
   * <p>기존 계약: Family uses the reviewed base registry vocabulary. Omens also require the paired
   * currency.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code family}: 옵션계열 ({@code String}).
   *   <li>{@code purpose}: 목적 ({@code Purpose}).
   *   <li>{@code currency}: 화폐 ({@code WorkbenchCurrency}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Context(family, purpose, currency)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       family}(옵션계열), {@code purpose}(목적), {@code currency}(화폐).
   * </ul>
   */
  public record Context(String family, Purpose purpose, WorkbenchCurrency currency) {
    public Context {
      Objects.requireNonNull(family);
      Objects.requireNonNull(purpose);
    }
  }

  private final Document document;
  private final Map<String, Entry> entries;

  public AutomaticCurrencyPolicy(Document document) {
    if (document.schemaVersion() != 1
        || !VERSION.equals(document.policyVersion())
        || document.maximumExploredExplicits() != MAXIMUM_EXPLORED_EXPLICITS
        || document.entries().size() != 187)
      throw new IllegalArgumentException("Unrecognized automatic currency policy contract");
    var indexed = new LinkedHashMap<String, Entry>();
    for (int i = 0; i < document.entries().size(); i++) {
      var entry = document.entries().get(i);
      if (entry.number() != i + 1
          || entry.id() == null
          || entry.id().isBlank()
          || indexed.putIfAbsent(entry.id(), entry) != null
          || entry.decision() == null
          || entry.condition() == null
          || entry.sourceUrl() == null
          || !entry.sourceUrl().startsWith("https://poe2db.tw/kr/")
          || (entry.decision() == Decision.CONDITIONAL) != (entry.condition() != Condition.NONE))
        throw new IllegalArgumentException("Invalid policy entry or fixed numbering");
      if (entry.number() <= 160) WorkbenchCurrency.valueOf(entry.id());
      else if (entry.number() <= 178) {
        if (!entry.id().startsWith("OMEN:")) throw new IllegalArgumentException("Omen ID required");
        WorkbenchOmen.valueOf(entry.id().substring(5));
      } else if (!entry.id().startsWith("EXTRA:"))
        throw new IllegalArgumentException("Supplementary ID required");
    }
    if (indexed.keySet().stream().filter(id -> !id.contains(":")).count()
                + MANUAL_CURRENCY_POLICY_IDS.size()
                + MANUAL_ONLY_CURRENCIES.size()
            != WorkbenchCurrency.values().length
        || Arrays.stream(WorkbenchCurrency.values())
            .anyMatch(
                currency ->
                    !MANUAL_ONLY_CURRENCIES.contains(currency.name())
                        && !indexed.containsKey(
                            MANUAL_CURRENCY_POLICY_IDS.getOrDefault(
                                currency.name(), currency.name())))
        || MANUAL_CURRENCY_POLICY_IDS.values().stream()
            .anyMatch(id -> indexed.get(id).decision() != Decision.EXCLUDED))
      throw new IllegalArgumentException("Policy must cover every defined currency");
    this.document = document;
    this.entries = Collections.unmodifiableMap(indexed);
  }

  public Document document() {
    return document;
  }

  public List<Entry> candidates() {
    return document.entries().stream()
        .filter(e -> e.decision() == Decision.USE || e.decision() == Decision.CONDITIONAL)
        .toList();
  }

  public Verdict evaluate(String id, Context context) {
    Objects.requireNonNull(context);
    if (MANUAL_ONLY_CURRENCIES.contains(id) || "OMEN:LIGHT".equals(id)) return Verdict.EXCLUDED;
    var entry = entries.get(MANUAL_CURRENCY_POLICY_IDS.getOrDefault(id, id));
    if (entry == null) return Verdict.UNREVIEWED;
    if (entry.decision() == Decision.EXCLUDED) return Verdict.EXCLUDED;
    if (entry.decision() == Decision.DEFERRED) return Verdict.DEFERRED;
    boolean temporary = context.purpose() == Purpose.TEMPORARY_AFFIX_FOR_REMOVAL;
    boolean permitted =
        switch (entry.condition()) {
          case NONE, BREACH_QUALITY -> true;
          case ANNULMENT -> context.currency() == WorkbenchCurrency.ANNULMENT;
          case RARITY_UPGRADE ->
              context.purpose() == Purpose.RARITY_UPGRADE
                  && context.currency() == WorkbenchCurrency.TRANSMUTATION;
          case FIRST_AFFIX_ACQUISITION -> context.purpose() == Purpose.GOAL_ACQUISITION;
          case GOAL_ACQUISITION ->
              context.purpose() == Purpose.GOAL_ACQUISITION
                  || context.purpose() == Purpose.TARGET_PREPARATION;
          case TEMPORARY_REMOVAL ->
              temporary
                  || context.purpose() == Purpose.GOAL_ACQUISITION
                  || ((context.purpose() == Purpose.REFILL_AFTER_ANNULMENT
                          || context.purpose() == Purpose.LAST_RESORT_EXALTED)
                      && context.currency() == WorkbenchCurrency.EXALTED);
          case TEMPORARY_REGAL_PREFIX, TEMPORARY_REGAL_SUFFIX ->
              (temporary || context.purpose() == Purpose.GOAL_ACQUISITION)
                  && context.currency() == WorkbenchCurrency.REGAL;
          case TEMPORARY_EXALTED_PREFIX, TEMPORARY_EXALTED_SUFFIX ->
              (temporary || context.purpose() == Purpose.GOAL_ACQUISITION)
                  && context.currency() == WorkbenchCurrency.EXALTED;
          case HYSTERIA_PARTS -> Set.of("gloves", "boots", "quivers").contains(context.family());
          case PRISMATIC_PARTS -> context.family().equals("gloves");
          case SWIFT_PARTS ->
              Set.of("gloves", "rings", "shields", "bucklers", "foci").contains(context.family());
          case SOVEREIGN_PARTS -> Set.of("rings", "amulets", "belts").contains(context.family());
          case EXPANSIVE_PARTS -> context.family().equals("boots");
          case CYCLONIC_PARTS ->
              Set.of("boots", "gloves", "helmet", "helmets").contains(context.family());
          case MYSTIC_PARTS ->
              Set.of("helmet", "helmets", "gloves", "boots", "quivers").contains(context.family());
        };
    return permitted ? Verdict.PERMITTED : Verdict.CONDITION_NOT_MET;
  }

  /** Crossing this boundary is unexplored mass, including successful outcomes beyond the bound. */
  public boolean withinExplorationBoundary(int explicitCount) {
    if (explicitCount < 0) throw new IllegalArgumentException("Negative explicit count");
    return explicitCount <= document.maximumExploredExplicits();
  }
}
