package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Rejected;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService.Disk;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.RareOutcomePolicy;
import java.time.*;
import java.util.*;

/**
 * 저장 문서의 호환 기본값을 읽는다. 입장 심사에서는 그래프 전체를 객체로 복원하지 않고 헤더만 읽는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code fragments}: 이미 열린 파서에서 부분 문서만 읽는 변환기 ({@code
 *       com.fasterxml.jackson.databind.ObjectReader}).
 *   <li>{@code repository}: 조회·저장을 위임하는 저장소 ({@code AutomaticJobRepository}).
 * </ul>
 *
 * <p>originalRequest(id)는 기본값을 보충하기 전의 요청만 스트리밍으로 읽어 원본 내보내기에 사용한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticJobCodec(json, repository)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON),
 *       {@code repository}(저장소).
 *   <li>{@code qualityDefaults(node)}: 과거 문서의 생략된 품질·출처 필드만 호환 기본값으로 보충한다. 입력: {@code node}(노드).
 *   <li>{@code header(id)}: 저장 문서의 소유권·만료·초기 조건을 읽는다. 입력: {@code id}(식별자). 반환: {@code
 *       StoredHeader}. 사용하는 예외: {@code java.io.IOException}, {@code Rejected}.
 *   <li>{@code read(id)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code id}(식별자). 반환: {@code Disk}. 사용하는 예외:
 *       {@code java.io.IOException}, {@code Rejected}.
 *   <li>{@code readHeader(id)}: 저장 JSON에서 입장 심사에 필요한 필드만 스트리밍으로 읽는다. 입력: {@code id}(식별자). 반환:
 *       {@code ObjectNode}. 사용하는 예외: {@code java.io.IOException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code StoredHeader}: {@code AutomaticJobCodec}에서 사용하는 저장된·헤더의 불변 값이다.
 * </ul>
 */
final class AutomaticJobCodec {
  private final ObjectMapper json;
  private final com.fasterxml.jackson.databind.ObjectReader fragments;
  private final AutomaticJobRepository repository;

  AutomaticJobCodec(ObjectMapper json, AutomaticJobRepository repository) {
    this.json = json;
    this.repository = repository;
    fragments =
        json.reader()
            .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  }

  /**
   * {@code AutomaticJobCodec}에서 사용하는 저장된·헤더의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code owner}: 서버가 확인한 작업 소유자 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code MethodSearchProtocol.Create}).
   *   <li>{@code expires}: 저장 값의 만료 시각 ({@code long}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code quality40}: 품질·40 ({@code boolean}).
   *   <li>{@code abstractQuality}: 추상화된·품질 ({@code int}).
   *   <li>{@code rareOutcomePolicy}: 희귀·결과·정책 ({@code com.fasterxml.jackson.databind.JsonNode}).
   * </ul>
   */
  record StoredHeader(
      String owner,
      MethodSearchProtocol.Create request,
      long expires,
      String status,
      boolean quality40,
      int abstractQuality,
      com.fasterxml.jackson.databind.JsonNode rareOutcomePolicy) {}

  private static void qualityDefaults(com.fasterxml.jackson.databind.JsonNode node) {
    if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
      if (object.has("quality40") && !object.has("abstractQuality"))
        object.put("abstractQuality", object.path("quality40").asBoolean() ? 40 : 0);
      if (object.has("initialQuality40") && !object.has("initialAbstractQuality"))
        object.put("initialAbstractQuality", object.path("initialQuality40").asBoolean() ? 40 : 0);
      // Historical executions have no source-lineage flag. They are read-only;
      // keep strict primitive validation for the current cursor schema.
      if (object.has("initialQuality40")
          && !object.has("symbolicSource")
          && Set.of(
                  "automatic-method-execution-v4-prepared-renewal",
                  "automatic-method-execution-v5-tier-laws")
              .contains(object.path("version").asText())) object.put("symbolicSource", false);
    }
    if (node != null && node.isContainerNode()) node.forEach(AutomaticJobCodec::qualityDefaults);
  }

  StoredHeader header(String id) {
    try {
      var tree = readHeader(id);
      if (tree == null) throw new java.io.IOException("Empty saved document");
      com.poe2craft.shared.item.ItemStateWireDefaults.normalizeOwned(tree);
      qualityDefaults(tree);
      if (tree == null
          || !id.equals(tree.path("id").asText())
          || !tree.path("owner").isTextual()
          || !tree.path("status").isTextual()
          || !tree.path("expires").isIntegralNumber()
          || !tree.path("request").isObject())
        throw new java.io.IOException("Invalid saved header");
      var root = tree.path("graph").path("nodes").path(0);
      return new StoredHeader(
          tree.path("owner").asText(),
          json.treeToValue(tree.get("request"), MethodSearchProtocol.Create.class),
          tree.path("expires").asLong(),
          tree.path("status").asText(),
          root.path("quality40").asBoolean(),
          root.path("abstractQuality").asInt(),
          tree.path("graph").has("rareOutcomePolicy")
              ? tree.path("graph").get("rareOutcomePolicy")
              : json.valueToTree(RareOutcomePolicy.disabled()));
    } catch (java.io.IOException | IllegalArgumentException e) {
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    }
  }

  Disk read(String id) {
    try (var input = repository.open(id)) {
      var tree = json.readTree(input);
      if (tree == null) throw new java.io.IOException("Empty saved document");
      com.poe2craft.shared.item.ItemStateWireDefaults.normalizeOwned(tree);
      qualityDefaults(tree);
      // Historical identities predate the stage flag. Decode only for read-only viewing;
      // never persist this compatibility default or resume under the new graph contract.
      var graph = tree.path("graph");
      if (!AutomaticMethodGraph.VERSION.equals(graph.path("version").asText()))
        for (var row : graph.path("nodes"))
          for (var member : row.path("members"))
            if (member instanceof com.fasterxml.jackson.databind.node.ObjectNode object
                && !object.has("initialStage")) object.put("initialStage", false);
      if (ArchivedSearchCatalog.supports(
              tree.at("/request/start/item/snapshotId").asText(),
              tree.at("/request/goal/catalogVersion").asText())
          && !AutomaticMethodGraph.VERSION.equals(graph.path("version").asText())) {
        ArchivedSearchCatalog.decodeModel(graph.path("identity"));
        for (var row : graph.path("executions"))
          ArchivedSearchCatalog.decodeModel(row.path("execution").path("identity"));
      }
      var saved = json.treeToValue(tree, Disk.class);
      if (saved == null
          || saved.graph() == null
          || saved.request() == null
          || saved.owner() == null
          || saved.status() == null
          || !id.equals(saved.id())) throw new java.io.IOException("Invalid saved document");
      return saved;
    } catch (java.io.IOException | IllegalArgumentException e) {
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    }
  }

  com.fasterxml.jackson.databind.JsonNode originalRequest(String id) {
    try {
      return readHeader(id).path("request");
    } catch (java.io.IOException | IllegalArgumentException error) {
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    }
  }

  private ObjectNode readHeader(String id) throws java.io.IOException {
    try (var input = repository.open(id);
        var parser = json.createParser(input)) {
      if (parser.nextToken() != JsonToken.START_OBJECT)
        throw new java.io.IOException("Invalid saved header");
      var result = json.createObjectNode();
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String name = parser.currentName();
        if (name == null || parser.nextToken() == null)
          throw new java.io.IOException("Truncated saved header");
        if (Set.of("id", "owner", "status", "expires", "request").contains(name))
          result.set(name, fragments.readTree(parser));
        else if (name.equals("graph") && parser.currentToken() == JsonToken.START_OBJECT) {
          var graph = result.putObject("graph");
          while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            if (field == null || parser.nextToken() == null)
              throw new java.io.IOException("Truncated saved graph");
            if (field.equals("rareOutcomePolicy")) graph.set(field, fragments.readTree(parser));
            else if (field.equals("nodes") && parser.currentToken() == JsonToken.START_ARRAY) {
              var nodes = graph.putArray("nodes");
              if (parser.nextToken() != JsonToken.END_ARRAY) {
                // First node supplies only the initial quality flags; its option/edge arrays can be
                // large.
                var initial = json.createObjectNode();
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                  while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String key = parser.currentName();
                    if (key == null || parser.nextToken() == null)
                      throw new java.io.IOException("Truncated saved node");
                    if (key.equals("quality40") || key.equals("abstractQuality"))
                      initial.set(key, fragments.readTree(parser));
                    else parser.skipChildren();
                  }
                } else parser.skipChildren();
                nodes.add(initial);
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                  if (parser.currentToken() == null)
                    throw new java.io.IOException("Truncated saved nodes");
                  parser.skipChildren();
                }
              }
            } else parser.skipChildren();
          }
        } else parser.skipChildren();
      }
      if (parser.nextToken() != null) throw new java.io.IOException("Trailing saved content");
      return result;
    }
  }
}
