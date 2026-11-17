package com.poe2craft.shared.crafting.infrastructure;

import com.poe2craft.shared.crafting.domain.WorkbenchDefinitions;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.*;
import java.util.*;

/**
 * 작업대 정의 JSON을 엄격하게 읽고 실행 카탈로그의 실제 옵션과 연결을 검증한다.
 *
 * <p>private 생성자는 인스턴스 생성을 막는다. read(stream)는 WorkbenchDefinitions를 만들며 입력 누락은
 * IllegalArgumentException, JSON 읽기 실패는 IOException으로 알린다. load는 봉인 파일을 읽고 validateTargets를 수행하며
 * I/O 실패를 IllegalStateException으로 감싼다. readTargets의 ids는 옵션 ID별 실제 정의 목록이다. 같은 ID의 문맥 차이를 보존하며
 * ItemCatalogLoader/EquipmentCurrencyCatalog의 공유 객체를 참조하므로 카탈로그를 다시 파싱하지 않는다. Loaded.DEFINITIONS는
 * 검증을 마친 한 번의 로드 결과이고 initialize는 이를 프로세스의 active 정의로 등록한다. 다른 규칙을 같은 프로세스에 재등록하는 경우는 도메인 초기화 검사가
 * 거절한다.
 */
public final class WorkbenchDefinitionsLoader {
  private WorkbenchDefinitionsLoader() {}

  public static WorkbenchDefinitions read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing Workbench definitions");
    return new WorkbenchDefinitions(
        BundledJson.mapper().readValue(stream, WorkbenchDefinitions.Document.class));
  }

  public static WorkbenchDefinitions load() {
    try (var stream = SealedResources.open("/crafting/workbench-definitions-v1.json")) {
      var definitions = read(stream);
      definitions.validateTargets(readTargets());
      return definitions;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Workbench definitions", e);
    }
  }

  static Map<String, List<ModifierDefinition>> readTargets() throws IOException {
    var ids = new HashMap<String, List<ModifierDefinition>>();
    for (var path : SealedResources.requiredPaths()) {
      if (!path.startsWith("catalog/")
          || !(path.endsWith("catalog.json") || path.equals("catalog/equipment-currencies.json")))
        continue;
      List<ModifierDefinition> definitions;
      if (path.equals("catalog/equipment-currencies.json")) {
        definitions = com.poe2craft.shared.item.EquipmentCurrencyCatalog.definitions();
      } else {
        definitions =
            com.poe2craft.shared.item.infrastructure.ItemCatalogLoader.bundledDefinitions(path);
      }
      for (var target : definitions) {
        ids.computeIfAbsent(target.id(), ignored -> new ArrayList<>()).add(target);
      }
    }
    ids.replaceAll((id, references) -> List.copyOf(references));
    return Map.copyOf(ids);
  }

  /** DEFINITIONS를 최초 initialize 호출에 한 번 로드하는 홀더다. 실패하면 검증되지 않은 규칙을 공개하지 않는다. */
  private static final class Loaded {
    private static final WorkbenchDefinitions DEFINITIONS = load();
  }

  public static WorkbenchDefinitions initialize() {
    return WorkbenchDefinitions.initialize(Loaded.DEFINITIONS);
  }
}
