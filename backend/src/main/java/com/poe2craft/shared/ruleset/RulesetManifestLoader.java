package com.poe2craft.shared.ruleset;

import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.json.BundledJson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * 봉인된 번들 목록과 다이제스트를 검증하고 규칙 식별자를 공유한다.
 *
 * <p>기존 계약: Reviewed bundle identity is separate from JSON schema and an unverified game season.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RulesetManifestLoader()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code Manifest}.
 *   <li>{@code loadBundle()}: 번들 자원을 읽고 검증한 결과를 구성한다. 반환: {@code Manifest}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code validate(manifest)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code manifest}(번들목록). 사용하는 예외:
 *       {@code IllegalArgumentException}, {@code IOException}.
 *   <li>{@code validateCoverage(manifest, consumedPaths)}: Coverage을(를) 검증한다. 입력: {@code
 *       manifest}(번들목록), {@code consumedPaths}(소비된·경로목록). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code read(stream)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code stream}(스트림). 반환: {@code Manifest}.
 *       사용하는 예외: {@code IllegalArgumentException}, {@code IOException}.
 *   <li>{@code hash(value)}: 해시 처리에서 {@code value.getBytes}을(를) 사용한다. 입력: {@code value}(값). 반환:
 *       {@code String}.
 *   <li>{@code hash(value)}: 해시 처리에서 {@code HexFormat.of}, {@code MessageDigest.getInstance}을(를)
 *       사용한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Manifest}: {@code RulesetManifestLoader}에서 사용하는 번들목록의 불변 값이다.
 *   <li>{@code Loaded}: 최초 접근 시 검증한 번들 값을 JVM 클래스 초기화로 한 번만 공개한다.
 * </ul>
 */
public final class RulesetManifestLoader {
  private RulesetManifestLoader() {}

  /**
   * {@code RulesetManifestLoader}에서 사용하는 번들목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
   *   <li>{@code rulesetVersion}: 규칙묶음·버전 ({@code String}).
   *   <li>{@code gameSeason}: 게임·Season ({@code String}).
   *   <li>{@code gamePatch}: 게임·Patch ({@code String}).
   *   <li>{@code provenance}: 출처이력 ({@code String}).
   *   <li>{@code engineRuleVersion}: 계산엔진·규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code files}: 파일목록 ({@code Map<String, String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Manifest(schemaVersion, rulesetVersion, gameSeason, gamePatch, provenance,
   *       engineRuleVersion, ledgerVersion, files)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       schemaVersion}(스키마·버전), {@code rulesetVersion}(규칙묶음·버전), {@code gameSeason}(게임·Season),
   *       {@code gamePatch}(게임·Patch), {@code provenance}(출처이력), {@code
   *       engineRuleVersion}(계산엔진·규칙·버전), {@code ledgerVersion}(규칙장부·버전), {@code files}(파일목록). 사용하는
   *       예외: {@code IllegalArgumentException}.
   *   <li>{@code identity()}: 현재 값 또는 규칙의 정체성 식별자를 조회한다. 반환: {@code String}.
   * </ul>
   */
  public record Manifest(
      int schemaVersion,
      String rulesetVersion,
      String gameSeason,
      String gamePatch,
      String provenance,
      String engineRuleVersion,
      String ledgerVersion,
      Map<String, String> files) {
    public Manifest {
      files = Collections.unmodifiableMap(new TreeMap<>(files));
      if (schemaVersion != 1
          || rulesetVersion == null
          || rulesetVersion.isBlank()
          || gameSeason == null
          || gameSeason.isBlank()
          || gamePatch == null
          || gamePatch.isBlank()
          || provenance == null
          || provenance.isBlank()
          || files.isEmpty())
        throw new IllegalArgumentException("Invalid reviewed ruleset manifest");
      files.forEach(
          (path, hash) -> {
            if (!path.matches("(?:catalog|crafting)/[a-zA-Z0-9_./-]+\\.json")
                || path.contains("..")
                || hash == null
                || !hash.matches("[0-9a-f]{64}"))
              throw new IllegalArgumentException("Invalid ruleset resource identity");
          });
    }

    public String identity() {
      return rulesetVersion
          + "-"
          + hash(
              rulesetVersion
                  + "|"
                  + gameSeason
                  + "|"
                  + gamePatch
                  + "|"
                  + engineRuleVersion
                  + "|"
                  + ledgerVersion
                  + "|"
                  + files);
    }
  }

  /**
   * 최초 접근 시 검증한 번들 값을 JVM 클래스 초기화로 한 번만 공개한다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code VALUE}: 한 번 검증한 공유 값 ({@code Manifest}).
   * </ul>
   */
  private static final class Loaded {
    private static final Manifest VALUE = loadBundle();
  }

  public static Manifest load() {
    return Loaded.VALUE;
  }

  private static Manifest loadBundle() {
    try (var stream =
        RulesetManifestLoader.class.getResourceAsStream("/crafting/ruleset-v1.json")) {
      var manifest = read(stream);
      validate(manifest);
      return manifest;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed ruleset manifest", e);
    }
  }

  public static void validate(Manifest manifest) throws IOException {
    validateCoverage(manifest, SealedResources.requiredPaths());
    for (var entry : manifest.files().entrySet()) {
      try (var data = RulesetManifestLoader.class.getResourceAsStream("/" + entry.getKey())) {
        if (data == null || !entry.getValue().equals(hash(data.readAllBytes())))
          throw new IllegalArgumentException("Ruleset resource digest differs: " + entry.getKey());
      }
    }
  }

  static void validateCoverage(Manifest manifest, Set<String> consumedPaths) {
    var missing = new TreeSet<>(consumedPaths);
    missing.removeAll(manifest.files().keySet());
    if (!missing.isEmpty())
      throw new IllegalArgumentException("Consumed resources are absent from ruleset: " + missing);
  }

  public static Manifest read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing reviewed ruleset manifest");
    var mapper = BundledJson.mapper();
    var manifest = mapper.readValue(stream, Manifest.class);
    if (!WorkbenchSimulator.RULE_VERSION.equals(manifest.engineRuleVersion())
        || !WorkbenchSimulator.LEDGER_VERSION.equals(manifest.ledgerVersion()))
      throw new IllegalArgumentException("Ruleset engine or ledger version differs");
    return manifest;
  }

  private static String hash(String value) {
    return hash(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String hash(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
