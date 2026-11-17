package com.poe2craft.shared.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 검증된 불변 아이템 데이터 스냅샷이다. 외부 조회나 확률 계산을 수행하지 않는다.
 *
 * <p>기존 계약: A validated immutable data snapshot. No database, HTTP, or probability calculation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code augmentContextApplied}: 증강물·문맥·적용여부 ({@code boolean}).
 *   <li>{@code metadata}: 메타데이터 ({@code Metadata}).
 *   <li>{@code base}: 베이스 ({@code BaseItem}).
 *   <li>{@code modifiers}: 옵션목록 ({@code Map<String, ModifierDefinition>}).
 *   <li>{@code compatibleSnapshotIds}: 호환되는·스냅샷·식별자목록 ({@code List<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemCatalog(metadata, base, definitions)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       metadata}(메타데이터), {@code base}(베이스), {@code definitions}(정의목록).
 *   <li>{@code ItemCatalog(metadata, base, definitions, compatibleSnapshotIds)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code metadata}(메타데이터), {@code base}(베이스), {@code definitions}(정의목록), {@code
 *       compatibleSnapshotIds}(호환되는·스냅샷·식별자목록).
 *   <li>{@code ItemCatalog(metadata, base, definitions, compatibleSnapshotIds,
 *       augmentContextApplied)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code metadata}(메타데이터), {@code
 *       base}(베이스), {@code definitions}(정의목록), {@code compatibleSnapshotIds}(호환되는·스냅샷·식별자목록),
 *       {@code augmentContextApplied}(증강물·문맥·적용여부). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code augmentContextApplied()}: {@code augmentContextApplied} 값을 조회한다. 반환: {@code
 *       boolean}.
 *   <li>{@code metadata()}: {@code metadata} 값을 조회한다. 반환: {@code Metadata}.
 *   <li>{@code base()}: {@code base} 값을 조회한다. 반환: {@code BaseItem}.
 *   <li>{@code modifiers()}: {@code modifiers} 값을 조회한다. 반환: {@code Map<String,
 *       ModifierDefinition>}.
 *   <li>{@code compatibleSnapshotIds()}: {@code compatibleSnapshotIds} 값을 조회한다. 반환: {@code
 *       List<String>}.
 *   <li>{@code find(id)}: 조건에 맞는 항목을 찾는다. 입력: {@code id}(식별자). 반환: {@code
 *       Optional<ModifierDefinition>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Metadata}: {@code ItemCatalog}에서 사용하는 메타데이터의 불변 값이다.
 *   <li>{@code BaseItem}: {@code ItemCatalog}에서 사용하는 베이스·아이템의 불변 값이다.
 * </ul>
 */
public final class ItemCatalog {
  private final boolean augmentContextApplied;
  private final Metadata metadata;
  private final BaseItem base;
  private final Map<String, ModifierDefinition> modifiers;
  private final List<String> compatibleSnapshotIds;

  public ItemCatalog(Metadata metadata, BaseItem base, List<ModifierDefinition> definitions) {
    this(metadata, base, definitions, List.of());
  }

  public ItemCatalog(
      Metadata metadata,
      BaseItem base,
      List<ModifierDefinition> definitions,
      List<String> compatibleSnapshotIds) {
    this(metadata, base, definitions, compatibleSnapshotIds, false);
  }

  public ItemCatalog(
      Metadata metadata,
      BaseItem base,
      List<ModifierDefinition> definitions,
      List<String> compatibleSnapshotIds,
      boolean augmentContextApplied) {
    this.augmentContextApplied = augmentContextApplied;
    this.metadata = Objects.requireNonNull(metadata);
    this.base = Objects.requireNonNull(base);
    this.compatibleSnapshotIds = List.copyOf(compatibleSnapshotIds);
    if (this.compatibleSnapshotIds.stream()
        .anyMatch(id -> id.isBlank() || id.equals(metadata.snapshotId())))
      throw new IllegalArgumentException("Invalid compatible snapshot identity");
    var indexed = new LinkedHashMap<String, ModifierDefinition>();
    for (var definition : definitions) {
      if (indexed.putIfAbsent(definition.id(), definition) != null) {
        throw new IllegalArgumentException("Duplicate modifier: " + definition.id());
      }
    }
    this.modifiers = Collections.unmodifiableMap(indexed);
    var implicits =
        definitions.stream().filter(d -> d.layer() == ModifierDefinition.Layer.IMPLICIT).toList();
    if (base.hasImplicit()
        ? implicits.size() != 1 || !implicits.getFirst().id().equals(base.implicitModifierId())
        : !implicits.isEmpty()) {
      throw new IllegalArgumentException("Implicit definitions must match the base exactly");
    }
    for (var type :
        List.of(ModifierDefinition.AffixType.PREFIX, ModifierDefinition.AffixType.SUFFIX)) {
      long count = definitions.stream().filter(d -> d.affixType() == type).count();
      long sum =
          definitions.stream()
              .filter(d -> d.affixType() == type)
              .mapToLong(ModifierDefinition::weight)
              .sum();
      if (count
              != (type == ModifierDefinition.AffixType.PREFIX
                  ? metadata.prefixCount()
                  : metadata.suffixCount())
          || sum
              != (type == ModifierDefinition.AffixType.PREFIX
                  ? metadata.prefixWeight()
                  : metadata.suffixWeight())) {
        throw new IllegalArgumentException("Snapshot counts or weight totals differ: " + type);
      }
    }
  }

  public boolean augmentContextApplied() {
    return augmentContextApplied;
  }

  public Metadata metadata() {
    return metadata;
  }

  public BaseItem base() {
    return base;
  }

  public Map<String, ModifierDefinition> modifiers() {
    return modifiers;
  }

  /** Only snapshots whose existing base and every existing definition were preserved exactly. */
  public List<String> compatibleSnapshotIds() {
    return compatibleSnapshotIds;
  }

  public Optional<ModifierDefinition> find(String id) {
    return Optional.ofNullable(modifiers.get(id));
  }

  /**
   * {@code ItemCatalog}에서 사용하는 메타데이터의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
   *   <li>{@code retrievedAt}: 수집된·기준 ({@code String}).
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code weightPolicy}: 가중치·정책 ({@code String}).
   *   <li>{@code rawSha256}: 원문·SHA·256 ({@code String}).
   *   <li>{@code detailsSha256}: 세부정보·SHA·256 ({@code String}).
   *   <li>{@code prefixCount}: 접두·개수 ({@code int}).
   *   <li>{@code suffixCount}: 접미·개수 ({@code int}).
   *   <li>{@code prefixWeight}: 접두·가중치 ({@code long}).
   *   <li>{@code suffixWeight}: 접미·가중치 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Metadata(snapshotId, retrievedAt, sourceUrl, weightPolicy, rawSha256,
   *       detailsSha256, prefixCount, suffixCount, prefixWeight, suffixWeight)}: 전달받은 값으로 구성원을
   *       초기화한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code retrievedAt}(수집된·기준), {@code
   *       sourceUrl}(출처·출처주소), {@code weightPolicy}(가중치·정책), {@code rawSha256}(원문·SHA·256), {@code
   *       detailsSha256}(세부정보·SHA·256), {@code prefixCount}(접두·개수), {@code suffixCount}(접미·개수),
   *       {@code prefixWeight}(접두·가중치), {@code suffixWeight}(접미·가중치). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Metadata(
      String snapshotId,
      String retrievedAt,
      String sourceUrl,
      String weightPolicy,
      String rawSha256,
      String detailsSha256,
      int prefixCount,
      int suffixCount,
      long prefixWeight,
      long suffixWeight) {
    public Metadata {
      for (String value :
          List.of(snapshotId, retrievedAt, sourceUrl, weightPolicy, rawSha256, detailsSha256)) {
        if (value.isBlank())
          throw new IllegalArgumentException("Snapshot metadata must not be blank");
      }
      if (!rawSha256.matches("[0-9a-f]{64}")
          || !detailsSha256.matches("[0-9a-f]{64}")
          || prefixCount < 0
          || suffixCount < 0
          || prefixWeight < 0
          || suffixWeight < 0
          || (!weightPolicy.equals("STARTING_ITEM_ONLY_NO_GENERATION")
              && (prefixCount < 1 || suffixCount < 1 || prefixWeight < 1 || suffixWeight < 1))
          || (weightPolicy.equals("STARTING_ITEM_ONLY_NO_GENERATION")
              && (prefixWeight != 0 || suffixWeight != 0))) {
        throw new IllegalArgumentException("Invalid snapshot metadata");
      }
    }
  }

  /**
   * {@code ItemCatalog}에서 사용하는 베이스·아이템의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code implicitModifierId}: 암시옵션·옵션·식별자 ({@code String}).
   *   <li>{@code magicPrefixes}: 마법·접두목록 ({@code int}).
   *   <li>{@code magicSuffixes}: 마법·접미목록 ({@code int}).
   *   <li>{@code rarePrefixes}: 희귀·접두목록 ({@code int}).
   *   <li>{@code rareSuffixes}: 희귀·접미목록 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code BaseItem(id, name, sourceUrl, implicitModifierId, magicPrefixes, magicSuffixes,
   *       rarePrefixes, rareSuffixes)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code name}(이름),
   *       {@code sourceUrl}(출처·출처주소), {@code implicitModifierId}(암시옵션·옵션·식별자), {@code
   *       magicPrefixes}(마법·접두목록), {@code magicSuffixes}(마법·접미목록), {@code rarePrefixes}(희귀·접두목록),
   *       {@code rareSuffixes}(희귀·접미목록). 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code hasImplicit()}: 암시옵션을(를) 보유 여부를 판정한다. 반환: {@code boolean}.
   * </ul>
   */
  public record BaseItem(
      String id,
      String name,
      String sourceUrl,
      String implicitModifierId,
      int magicPrefixes,
      int magicSuffixes,
      int rarePrefixes,
      int rareSuffixes) {
    public BaseItem {
      for (String value : List.of(id, name, sourceUrl)) {
        if (value.isBlank()) throw new IllegalArgumentException("Base identity is required");
      }
      Objects.requireNonNull(implicitModifierId, "implicitModifierId");
      if (!implicitModifierId.isEmpty() && implicitModifierId.isBlank()) {
        throw new IllegalArgumentException("Implicit identity must be empty or nonblank");
      }
      if (magicPrefixes < 0 || magicSuffixes < 0 || rarePrefixes < 0 || rareSuffixes < 0) {
        throw new IllegalArgumentException("Invalid base capacity");
      }
    }

    /** Empty identity denotes a sourced base with no implicit; base properties are separate. */
    public boolean hasImplicit() {
      return !implicitModifierId.isEmpty();
    }
  }
}
