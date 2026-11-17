package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * 구체 아이템 상태와 출처를 손실 없이 보존한다.
 *
 * <p>기존 계약: Lossless state plus provenance; no goal-dependent projection or legacy relabeling.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code item}: 아이템 ({@code ItemState}).
 *   <li>{@code provenance}: 출처이력 ({@code Provenance}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicCurrencyState(item, provenance)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code item}(아이템),
 *       {@code provenance}(출처이력).
 *   <li>{@code catalogDigest(catalog)}: 카탈로그·다이제스트 처리에서 {@code catalog.metadata}, {@code
 *       m.snapshotId}을(를) 사용한다. 입력: {@code catalog}(카탈로그). 반환: {@code String}.
 *   <li>{@code canonicalKey()}: 정규형·키 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code modifiers(out, modifiers)}: 옵션목록 처리에서 {@code out.writeInt}, {@code
 *       modifiers.size}을(를) 사용한다. 입력: {@code out}(출력), {@code modifiers}(옵션목록). 사용하는 예외: {@code
 *       IOException}.
 *   <li>{@code strings(out, values)}: strings 처리에서 {@code out.writeInt}, {@code values.size}을(를)
 *       사용한다. 입력: {@code out}(출력), {@code values}(값목록). 사용하는 예외: {@code IOException}.
 *   <li>{@code string(out, value)}: string 처리에서 {@code value.getBytes}, {@code out.writeInt}을(를)
 *       사용한다. 입력: {@code out}(출력), {@code value}(값). 사용하는 예외: {@code IOException}.
 *   <li>{@code digest(encoder)}: 다이제스트 처리에서 {@code encoder.write}, {@code HexFormat.of}을(를) 사용한다.
 *       입력: {@code encoder}(encoder). 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Provenance}: {@code BasicCurrencyState}에서 사용하는 출처이력의 불변 값이다.
 *   <li>{@code Encoder}: {@code BasicCurrencyState} 내부의 Encoder 처리를 맡는다.
 * </ul>
 */
public record BasicCurrencyState(ItemState item, Provenance provenance) {
  public BasicCurrencyState {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(provenance, "provenance");
  }

  /**
   * {@code BasicCurrencyState}에서 사용하는 출처이력의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code rulesetIdentity}: 현재 실행 규칙 묶음의 식별자 ({@code String}).
   *   <li>{@code catalogDigest}: 카탈로그·다이제스트 ({@code String}).
   *   <li>{@code modelVersion}: 모형·버전 ({@code String}).
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code weightPolicy}: 가중치·정책 ({@code String}).
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code retrievedAt}: 수집된·기준 ({@code String}).
   *   <li>{@code rawSha256}: 원문·SHA·256 ({@code String}).
   *   <li>{@code detailsSha256}: 세부정보·SHA·256 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Provenance(rulesetIdentity, catalogDigest, modelVersion, ruleVersion,
   *       ledgerVersion, weightPolicy, sourceUrl, retrievedAt, rawSha256, detailsSha256)}: 전달받은 값으로
   *       구성원을 초기화한다. 입력: {@code rulesetIdentity}(규칙묶음·정체성), {@code catalogDigest}(카탈로그·다이제스트),
   *       {@code modelVersion}(모형·버전), {@code ruleVersion}(규칙·버전), {@code ledgerVersion}(규칙장부·버전),
   *       {@code weightPolicy}(가중치·정책), {@code sourceUrl}(출처·출처주소), {@code retrievedAt}(수집된·기준),
   *       {@code rawSha256}(원문·SHA·256), {@code detailsSha256}(세부정보·SHA·256). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Provenance(
      String rulesetIdentity,
      String catalogDigest,
      String modelVersion,
      String ruleVersion,
      String ledgerVersion,
      String weightPolicy,
      String sourceUrl,
      String retrievedAt,
      String rawSha256,
      String detailsSha256) {
    public Provenance {
      for (String value :
          List.of(
              rulesetIdentity,
              catalogDigest,
              modelVersion,
              ruleVersion,
              ledgerVersion,
              weightPolicy,
              sourceUrl,
              retrievedAt,
              rawSha256,
              detailsSha256))
        if (value.isBlank()) throw new IllegalArgumentException("Explicit provenance is required");
      if (!catalogDigest.matches("[0-9a-f]{64}")
          || !rawSha256.matches("[0-9a-f]{64}")
          || !detailsSha256.matches("[0-9a-f]{64}"))
        throw new IllegalArgumentException("Invalid catalog provenance digest");
    }
  }

  /**
   * Digest includes actual catalog contents and ordered source arrays, not just claimed metadata.
   */
  public static String catalogDigest(ItemCatalog catalog) {
    return digest(
        out -> {
          string(out, "basic-catalog-key-v1");
          var m = catalog.metadata();
          strings(
              out,
              List.of(
                  m.snapshotId(),
                  m.retrievedAt(),
                  m.sourceUrl(),
                  m.weightPolicy(),
                  m.rawSha256(),
                  m.detailsSha256()));
          out.writeInt(m.prefixCount());
          out.writeInt(m.suffixCount());
          out.writeLong(m.prefixWeight());
          out.writeLong(m.suffixWeight());
          var b = catalog.base();
          strings(out, List.of(b.id(), b.name(), b.sourceUrl(), b.implicitModifierId()));
          out.writeInt(b.magicPrefixes());
          out.writeInt(b.magicSuffixes());
          out.writeInt(b.rarePrefixes());
          out.writeInt(b.rareSuffixes());
          strings(out, catalog.compatibleSnapshotIds());
          out.writeInt(catalog.modifiers().size());
          for (var d : catalog.modifiers().values()) {
            strings(
                out,
                List.of(
                    d.id(),
                    d.name(),
                    d.layer().name(),
                    d.affixType() == null ? "" : d.affixType().name()));
            strings(out, d.familyIds().stream().sorted().toList());
            out.writeInt(d.requiredItemLevel());
            out.writeLong(d.weight());
            out.writeInt(d.tier());
            string(out, d.text());
            out.writeInt(d.stats().size());
            for (var s : d.stats()) {
              string(out, s.id());
              out.writeLong(s.min());
              out.writeLong(s.max());
            }
            strings(out, d.tags().stream().sorted().toList());
            string(out, d.sourceUrl());
          }
        });
  }

  /**
   * Length-prefixed binary fields avoid separator/toString collisions and retain unknown sockets.
   */
  public String canonicalKey() {
    return digest(
        out -> {
          string(out, "basic-full-state-key-v1");
          strings(
              out,
              List.of(
                  provenance.rulesetIdentity(),
                  provenance.catalogDigest(),
                  provenance.modelVersion(),
                  provenance.ruleVersion(),
                  provenance.ledgerVersion(),
                  provenance.weightPolicy(),
                  provenance.sourceUrl(),
                  provenance.retrievedAt(),
                  provenance.rawSha256(),
                  provenance.detailsSha256()));
          strings(out, List.of(item.snapshotId(), item.baseItemId(), item.rarity().name()));
          out.writeInt(item.itemLevel());
          modifiers(out, item.implicits());
          modifiers(out, item.explicits());
          strings(out, item.conditions().stream().map(Enum::name).sorted().toList());
          out.writeBoolean(item.augmentSockets() != null);
          if (item.augmentSockets() != null) out.writeInt(item.augmentSockets());
          out.writeBoolean(item.catalystQuality() != null);
          if (item.catalystQuality() != null) {
            string(out, item.catalystQuality().type().name());
            out.writeInt(item.catalystQuality().amount());
          }
        });
  }

  private static void modifiers(DataOutputStream out, List<ModifierInstance> modifiers)
      throws IOException {
    out.writeInt(modifiers.size());
    for (var modifier : modifiers) {
      string(out, modifier.modifierId());
      out.writeBoolean(modifier.fractured());
      if (modifier.crafted()) {
        out.writeUTF("crafted");
      }
      out.writeInt(modifier.values().size());
      for (var entry : new TreeMap<>(modifier.values()).entrySet()) {
        string(out, entry.getKey());
        out.writeLong(entry.getValue());
      }
    }
  }

  private static void strings(DataOutputStream out, List<String> values) throws IOException {
    out.writeInt(values.size());
    for (var value : values) string(out, value);
  }

  private static void string(DataOutputStream out, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String digest(Encoder encoder) {
    try {
      var bytes = new ByteArrayOutputStream();
      try (var out = new DataOutputStream(bytes)) {
        encoder.write(out);
      }
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Cannot encode transition identity", e);
    }
  }

  /**
   * {@code BasicCurrencyState} 내부의 Encoder 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code write(out)}: 쓰기의 실행 계약을 정의한다. 입력: {@code out}(출력). 사용하는 예외: {@code IOException}.
   * </ul>
   */
  private interface Encoder {
    void write(DataOutputStream out) throws IOException;
  }
}
