package com.poe2craft.shared.ids;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.shared.json.BundledJson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.core.io.ClassPathResource;

/**
 * 발급된 짧은 ID를 서버 시작 시 한 번 검증·색인한다. 표시는 원문을 유지하며 도메인 간 ID를 섞지 않는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code HEADER}: 헤더; 초기값 {@code "X-Crafting-Ids"} ({@code String}).
 *   <li>{@code ACTIVE}: 활성 ({@code GameIdMappings}).
 *   <li>{@code identity}: 정체성 ({@code String}).
 *   <li>{@code encoded}: 원본 키에서 짧은 ID로 가는 도메인별 불변 색인 ({@code Map<String, Map<String, String>>}).
 *   <li>{@code decoded}: 짧은 ID에서 원본 키로 가는 도메인별 불변 색인 ({@code Map<String, Map<String, String>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GameIdMappings(source)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code source}(출처). 사용하는 예외:
 *       {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code GameIdMappings}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code active()}: 서버가 시작할 때 검증한 공유 매핑을 반환한다. 반환: {@code GameIdMappings}.
 *   <li>{@code identity()}: 전송 사전 해시와 변환 필드 버전을 함께 반환한다. 반환: {@code String}.
 *   <li>{@code convert(domain, value, compact)}: 등록된 식별자를 변환하고 모르는 값은 원문 그대로 반환한다. 입력: {@code
 *       domain}(식별자종류), {@code value}(값), {@code compact}(압축여부). 반환: {@code String}.
 * </ul>
 */
public final class GameIdMappings {
  public static final String HEADER = "X-Crafting-Ids";
  private static final GameIdMappings ACTIVE = load();
  private final String identity;
  private final Map<String, Map<String, String>> encoded;
  private final Map<String, Map<String, String>> decoded;

  public GameIdMappings(JsonNode source) {
    if (source.path("version").asInt() != 1 || !source.path("domains").isObject())
      throw new IllegalArgumentException("Invalid identifier mapping version");
    var forwards = new TreeMap<String, Map<String, String>>();
    var backwards = new TreeMap<String, Map<String, String>>();
    var allIds = new HashSet<String>();
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var domainNames = new TreeSet<String>();
      source.path("domains").fieldNames().forEachRemaining(domainNames::add);
      if (!domainNames.equals(
          Set.of(
              "bases",
              "modifiers",
              "stats",
              "families",
              "tags",
              "actions",
              "augments",
              "itemClasses",
              "pseudos"))) throw new IllegalArgumentException("Invalid identifier domains");
      for (var domain : domainNames) {
        var entries = source.path("domains").path(domain);
        var ids = new TreeSet<String>();
        entries.fieldNames().forEachRemaining(ids::add);
        var forward = new HashMap<String, String>();
        var backward = new HashMap<String, String>();
        for (var id : ids) {
          var value = entries.get(id);
          if (!id.matches("[a-z][0-9a-z]{4,}")
              || !allIds.add(id)
              || !value.isTextual()
              || value.asText().isBlank()
              || value.asText().matches("(?s).*[\\x00\\r\\n].*"))
            throw new IllegalArgumentException("Invalid identifier mapping entry");
          var original = value.asText();
          if (forward.put(original, id) != null)
            throw new IllegalArgumentException("Duplicate identifier source");
          backward.put(id, original);
          digest.update(
              (domain + "\0" + id + "\0" + original + "\n").getBytes(StandardCharsets.UTF_8));
        }
        forwards.put(domain, Map.copyOf(forward));
        backwards.put(domain, Map.copyOf(backward));
      }
      String dictionary = "game-ids-v1-" + HexFormat.of().formatHex(digest.digest());
      if (!dictionary.equals(source.path("identity").asText()))
        throw new IllegalArgumentException("Identifier mapping digest mismatch");
      identity = dictionary + "-fields2";
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
    encoded = Map.copyOf(forwards);
    decoded = Map.copyOf(backwards);
  }

  private static GameIdMappings load() {
    try (var input = new ClassPathResource("contracts/game-ids-v1.json").getInputStream()) {
      return new GameIdMappings(BundledJson.mapper().readTree(input));
    } catch (java.io.IOException error) {
      throw new IllegalStateException("Cannot load identifier mappings", error);
    }
  }

  public static GameIdMappings active() {
    return ACTIVE;
  }

  public String identity() {
    return identity;
  }

  String convert(String domain, String value, boolean compact) {
    return (compact ? encoded : decoded).getOrDefault(domain, Map.of()).getOrDefault(value, value);
  }
}
