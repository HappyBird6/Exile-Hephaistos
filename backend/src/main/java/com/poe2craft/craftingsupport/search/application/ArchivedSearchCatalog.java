package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * v8~v10 저장 세션의 표시 전용 자료다. 문서에는 당시 카탈로그, 목표 정의와 제작 모델이 있으며 최초 조회 시 한 번 검증해 읽는다. supports는 스냅샷과 목표
 * 판본을 대조한다. catalog와 goalDefinitions는 과거 노드 표시 자료를 제공한다. decodeModel은 보관된 모델과 동일한 JSON에만 구버전 기본값을
 * 채우고 matchesModel은 읽기 전용 식별자를 검증한다.
 */
public final class ArchivedSearchCatalog {
  private ArchivedSearchCatalog() {}

  private static final String SNAPSHOT =
      "solar-special-0eddf6e6993c96d9c19942db5e81ff0774afe85c42d4a6646b978d442700d7b4";
  private static final ObjectMapper JSON = new ObjectMapper();

  /** document와 catalog는 최초 접근 때만 준비한다. model은 구형 모델 검증에만 사용한다. */
  private static final class Archive {
    static final JsonNode document;
    static final ItemCatalog catalog;
    static final WorkbenchSimulator.AutomaticIdentity model;

    static {
      try (var stream =
          new GZIPInputStream(
              SealedResources.openArchive(
                  "search-history/solar-v8-v10.json.gz",
                  "fce6e3c72e3d941f4d3a5f277ef29cba1882d79ceb29130094dc260a6de55ff2"))) {
        document = JSON.readTree(stream);
        var data = document.path("catalog");
        catalog =
            new ItemCatalog(
                JSON.treeToValue(data.get("metadata"), ItemCatalog.Metadata.class),
                JSON.treeToValue(data.get("base"), ItemCatalog.BaseItem.class),
                Arrays.asList(
                    JSON.treeToValue(data.get("definitions"), ModifierDefinition[].class)),
                Arrays.asList(JSON.treeToValue(data.get("compatibleSnapshotIds"), String[].class)));
        var old = document.path("model").deepCopy();
        fillPolicies(old);
        model = JSON.treeToValue(old, WorkbenchSimulator.AutomaticIdentity.class);
      } catch (IOException error) {
        throw new IllegalStateException("Invalid historical catalog", error);
      }
    }
  }

  public static boolean supports(String snapshot, String version) {
    return SNAPSHOT.equals(snapshot) && Archive.document.path("goalDefinitions").has(version);
  }

  public static ItemCatalog catalog() {
    return Archive.catalog;
  }

  public static JsonNode goalDefinitions(String version) {
    return Archive.document.path("goalDefinitions").required(version).deepCopy();
  }

  static void decodeModel(JsonNode identity) {
    var model = identity.path("model");
    if (model.isObject() && model.equals(Archive.document.path("model"))) fillPolicies(model);
  }

  // 이 필드는 당시 존재하지 않았다. 실행기에는 전달하지 않으며 구형 식별자를 비교할 때만 사용한다.
  private static void fillPolicies(JsonNode model) {
    ((ObjectNode) model.path("definitions")).putObject("baseTargets");
    for (var currency : model.path("definitions").path("currencies")) {
      var policy = ((ObjectNode) currency).putObject("materialPolicy");
      policy.put("kind", "OTHER");
      policy.put("mode", "NONE");
      policy.putArray("inputRarities");
      policy.putNull("resultRarity");
    }
  }

  static boolean matchesModel(WorkbenchSimulator.AutomaticIdentity saved, String catalogDigest) {
    return "4300505361f8d98c1b7f69ee87c4e8c447a1bb7ab28fd6b9b21556260c7da8c7".equals(catalogDigest)
        && saved != null
        && saved
            .ruleVersion()
            .equals(
                "solar-workbench-abyss-essence-v16-homogenising-legacy-v1-legacy-five-v1-omen-composition-v1-catalyst-max-v1-refined-sapphire-v1-quality-cap-preserve-v2")
        && Archive.model.equals(saved);
  }
}
