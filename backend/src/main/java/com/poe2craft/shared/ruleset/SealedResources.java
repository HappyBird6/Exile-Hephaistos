package com.poe2craft.shared.ruleset;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.security.*;
import java.util.*;

/**
 * JSON을 읽기 전에 규칙 manifest에 등록된 경로와 내용 해시를 검사한다.
 *
 * <p>기존 계약: Every bundled consumer verifies membership before parsing; archive-only files may also
 * be sealed.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code FIXED}: 고정 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SealedResources()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code open(absolutePath)}: 조회할 자원 또는 작업 범위를 연다. 입력: {@code absolutePath}(absolute·경로). 반환:
 *       {@code InputStream}. 사용하는 예외: {@code IllegalArgumentException}, {@code IOException}.
 *   <li>{@code verify(key, bytes, files)}: 검증 처리에서 {@code files.containsKey}, {@code
 *       HexFormat.of}을(를) 사용한다. 입력: {@code key}(키), {@code bytes}(바이트수), {@code files}(파일목록). 사용하는
 *       예외: {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code requiredPaths()}: 필수·경로목록 값을 조회·구성한다. 반환: {@code Set<String>}. 사용하는 예외: {@code
 *       IOException}.
 *   <li>{@code requiredPaths(registry)}: 필수·경로목록 처리에서 {@code base.path}, {@code pool.matches}을(를)
 *       사용한다. 입력: {@code registry}(등록목록). 반환: {@code Set<String>}. 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code IOException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Bundle}: {@code SealedResources} 내부의 Bundle 처리를 맡는다.
 *   <li>{@code Dependencies}: {@code SealedResources} 내부의 의존목록 처리를 맡는다.
 * </ul>
 */
public final class SealedResources {
  private SealedResources() {}

  private static final Set<String> FIXED =
      Set.of(
          "catalog/attuned-wand/catalog.json",
          "catalog/attuned-wand/perfect-essences.catalog.json",
          "catalog/base-policies.json",
          "catalog/equipment-currencies.json",
          "catalog/augment-crafting.json",
          "catalog/augment-effect-templates.json",
          "catalog/crude-bow/catalog.json",
          "catalog/crude-bow/perfect-essences.catalog.json",
          "catalog/diamond/catalog.json",
          "catalog/emerald/catalog.json",
          "catalog/iron-ring/catalog.json",
          "catalog/iron-ring/perfect-essences.catalog.json",
          "catalog/rattling-sceptre/catalog.json",
          "catalog/rattling-sceptre/perfect-essences.catalog.json",
          "catalog/rawhide-belt/catalog.json",
          "catalog/rawhide-belt/perfect-essences.catalog.json",
          "catalog/ruby/catalog.json",
          "catalog/rusted-cuirass/catalog.json",
          "catalog/rusted-cuirass/perfect-essences.catalog.json",
          "catalog/rusted-greathelm/catalog.json",
          "catalog/rusted-greathelm/perfect-essences.catalog.json",
          "catalog/sapphire/catalog.json",
          "catalog/solar-amulet/abyss-essence.catalog.json",
          "catalog/solar-amulet/breach-essence.catalog.json",
          "catalog/solar-amulet/catalog.json",
          "catalog/solar-amulet/perfect-enhancement.catalog.json",
          "catalog/solar-amulet/perfect-infinite.catalog.json",
          "catalog/solar-amulet/runic-alloy.catalog.json",
          "catalog/stocky-mitts/abyss-essence.catalog.json",
          "catalog/stocky-mitts/catalog.json",
          "catalog/stocky-mitts/horror-essence.catalog.json",
          "catalog/stocky-mitts/perfect-grounding-opulence.catalog.json",
          "catalog/stocky-mitts/prismatic-alloy.catalog.json",
          "catalog/stocky-mitts/reviewed-alloys.catalog.json",
          "catalog/stocky-mitts/scalar-alloys.catalog.json",
          "catalog/time-lost-diamond/catalog.json",
          "catalog/time-lost-emerald/catalog.json",
          "catalog/time-lost-ruby/catalog.json",
          "catalog/time-lost-sapphire/catalog.json",
          "catalog/manual-reveal.json",
          "catalog/desecration-currencies.json",
          "catalog/top-base-essences.json",
          "catalog/top-bases.json",
          "crafting/goalfilter/bases-v1.json",
          "crafting/goalfilter/base-inventory-v5-readonly.json",
          "crafting/goalfilter/definitions-v1.json",
          "crafting/goalfilter/definitions-v3-readonly.json",
          "crafting/goalfilter/definitions-v4-readonly.json",
          "crafting/goalfilter/definitions-v5-readonly.json",
          "crafting/registry-v2.json",
          "crafting/supported-base-sets-v1.json",
          "crafting/workbench-definitions-v1.json",
          "crafting/automatic-currency-policy-v1.json");

  /**
   * {@code SealedResources} 내부의 Bundle 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code FILES}: 파일목록 ({@code Map<String, String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code files()}: 파일목록 값을 조회·구성한다. 반환: {@code Map<String, String>}. 사용하는 예외: {@code
   *       IllegalStateException}.
   * </ul>
   */
  private static final class Bundle {
    private static final Map<String, String> FILES = files();

    private static Map<String, String> files() {
      try (var stream = SealedResources.class.getResourceAsStream("/crafting/ruleset-v1.json")) {
        var mapper =
            new ObjectMapper()
                .enable(
                    com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION
                        .mappedFeature());
        var result = new TreeMap<String, String>();
        mapper
            .readTree(stream)
            .path("files")
            .fields()
            .forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
        return Collections.unmodifiableMap(result);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read ruleset resource seal", e);
      }
    }
  }

  /** 과거 결과 전용 자료는 당시 해시로 검증하여 현재 게임 규칙 판본과 독립적으로 읽는다. */
  public static InputStream openArchive(String name, String digest) throws IOException {
    if (!name.startsWith("search-history/") || name.contains(".."))
      throw new IllegalArgumentException("Invalid archive path");
    try (var stream = SealedResources.class.getResourceAsStream("/" + name)) {
      if (stream == null) throw new IOException("Missing historical resource");
      byte[] bytes = stream.readAllBytes();
      verify(name, bytes, Map.of(name, digest));
      return new ByteArrayInputStream(bytes);
    }
  }

  public static InputStream open(String absolutePath) throws IOException {
    String key = absolutePath.startsWith("/") ? absolutePath.substring(1) : absolutePath;
    if (!Bundle.FILES.containsKey(key))
      throw new IllegalArgumentException("Consumed resource is absent from ruleset: " + key);
    try (var stream = SealedResources.class.getResourceAsStream("/" + key)) {
      if (stream == null) throw new IllegalArgumentException("Missing sealed resource: " + key);
      byte[] bytes = stream.readAllBytes();
      verify(key, bytes, Bundle.FILES);
      return new ByteArrayInputStream(bytes);
    }
  }

  public static void verify(String key, byte[] bytes, Map<String, String> files) {
    if (!files.containsKey(key))
      throw new IllegalArgumentException("Consumed resource is absent from ruleset: " + key);
    try {
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (!hash.equals(files.get(key)))
        throw new IllegalArgumentException("Consumed resource digest differs: " + key);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Loader dependencies, including pools selected by registry data; no directory scan. */
  public static Set<String> requiredPaths() throws IOException {
    return Dependencies.PATHS;
  }

  /**
   * {@code SealedResources} 내부의 의존목록 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code PATHS}: 경로목록 ({@code Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code paths()}: 경로목록 값을 조회·구성한다. 반환: {@code Set<String>}. 사용하는 예외: {@code
   *       IllegalStateException}.
   * </ul>
   */
  private static final class Dependencies {
    private static final Set<String> PATHS = paths();

    private static Set<String> paths() {
      try (var stream = SealedResources.class.getResourceAsStream("/catalog/top-bases.json")) {
        return requiredPaths(stream);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read sealed resource dependencies", e);
      }
    }
  }

  public static Set<String> requiredPaths(InputStream registry) throws IOException {
    var paths = new TreeSet<>(FIXED);
    var bases = new ObjectMapper().readTree(registry);
    for (var base : bases) {
      String pool = base.path("pool").asText();
      if (!pool.matches("[a-z0-9-]+"))
        throw new IllegalArgumentException("Invalid sealed pool identity");
      paths.add("catalog/" + pool + "/catalog.json");
    }
    return Collections.unmodifiableSet(paths);
  }
}
