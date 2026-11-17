package com.poe2craft.craftingsupport.goal.infrastructure;

import com.poe2craft.craftingsupport.goal.domain.GoalDefinitions;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.*;

/**
 * 현재·과거 목표 정의를 엄격히 검증해 불변 값으로 읽고 번들별로 공유한다.
 *
 * <p>기존 계약: Strict bundled configuration; domain receives only immutable typed data.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code DEFINITIONS}: 정의목록 ({@code java.util.concurrent.ConcurrentMap<String,
 *       GoalDefinitions>}).
 *   <li>{@code JSON}: 공통 JSON 변환기 ({@code com.fasterxml.jackson.databind.ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalDefinitionsLoader()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code GoalDefinitions}.
 *   <li>{@code bundled(resource)}: 허용된 번들 경로의 검증된 값을 공유한다. 입력: {@code resource}(자원). 반환: {@code
 *       GoalDefinitions}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code read(stream)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code stream}(스트림). 반환: {@code
 *       GoalDefinitions}. 사용하는 예외: {@code IllegalArgumentException}, {@code IOException}.
 *   <li>{@code loadHistoricalV5()}: 과거·5을(를) 읽어 검증한다. 반환: {@code GoalDefinitions}.
 *   <li>{@code loadHistoricalV3()}: 과거·3을(를) 읽어 검증한다. 반환: {@code GoalDefinitions}.
 *   <li>{@code loadHistoricalV4()}: 과거·4을(를) 읽어 검증한다. 반환: {@code GoalDefinitions}.
 * </ul>
 */
public final class GoalDefinitionsLoader {
  private GoalDefinitionsLoader() {}

  private static final java.util.concurrent.ConcurrentMap<String, GoalDefinitions> DEFINITIONS =
      new java.util.concurrent.ConcurrentHashMap<>();
  private static final com.fasterxml.jackson.databind.ObjectMapper JSON = BundledJson.mapper();

  public static GoalDefinitions load() {
    return bundled("definitions-v1.json");
  }

  private static GoalDefinitions bundled(String resource) {
    return DEFINITIONS.computeIfAbsent(
        resource,
        key -> {
          try (var stream = SealedResources.open("/crafting/goalfilter/" + key)) {
            return read(stream);
          } catch (IOException error) {
            throw new IllegalStateException("Cannot load goal definitions", error);
          }
        });
  }

  public static GoalDefinitions read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing goal definitions");
    var mapper = JSON;
    var data = mapper.readTree(stream);
    if (data.has("stats") && data.get("stats").isArray())
      for (var stat : data.get("stats"))
        if (stat.isObject()) {
          var object = (com.fasterxml.jackson.databind.node.ObjectNode) stat;
          if (!stat.has("sourceDivisor")) object.put("sourceDivisor", 1);
          if (!stat.has("evaluationOnly")) object.putNull("evaluationOnly");
        }
    return mapper.treeToValue(data, GoalDefinitions.class);
  }

  public static GoalDefinitions loadHistoricalV5() {
    return bundled("definitions-v5-readonly.json");
  }

  public static GoalDefinitions loadHistoricalV3() {
    return bundled("definitions-v3-readonly.json");
  }

  public static GoalDefinitions loadHistoricalV4() {
    return bundled("definitions-v4-readonly.json");
  }
}
