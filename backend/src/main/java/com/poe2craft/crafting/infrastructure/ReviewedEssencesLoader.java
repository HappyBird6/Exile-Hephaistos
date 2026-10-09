package com.poe2craft.crafting.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.item.BaseRegistry;
import java.io.*;
import java.util.*;

/** Typed shape validation; simulator retains target applicability and modifier validation. */
public final class ReviewedEssencesLoader {
  private ReviewedEssencesLoader() {}

  public record Targets(
      Map<WorkbenchCurrency, List<String>> fixed,
      Map<WorkbenchCurrency, List<String>> replacements) {
    public Targets {
      fixed = copy(fixed);
      replacements = copy(replacements);
    }

    private static Map<WorkbenchCurrency, List<String>> copy(
        Map<WorkbenchCurrency, List<String>> source) {
      if (source == null) throw new IllegalArgumentException("Missing reviewed essence target map");
      var result = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
      source.forEach(
          (action, ids) -> {
            if (ids == null || ids.stream().anyMatch(id -> id == null || id.isBlank()))
              throw new IllegalArgumentException("Missing reviewed essence target ID");
            result.put(action, List.copyOf(ids));
          });
      return Collections.unmodifiableMap(result);
    }
  }

  public static Map<String, Targets> load() {
    try (var stream =
        ReviewedEssencesLoader.class.getResourceAsStream("/catalog/top-base-essences.json")) {
      return read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load reviewed essence manifest", e);
    }
  }

  public static Map<String, Targets> read(InputStream stream) throws IOException {
    if (stream == null) throw new IllegalArgumentException("Missing reviewed essence manifest");
    var mapper = BundledJson.mapper();
    Map<String, Targets> manifest =
        mapper.readValue(stream, new TypeReference<Map<String, Targets>>() {});
    var expected = new HashSet<String>();
    BaseRegistry.topBases()
        .forEach(
            (key, base) -> {
              if (base.policy().legacyCatalog() == null) expected.add(key);
            });
    if (manifest == null
        || !expected.equals(manifest.keySet())
        || manifest.values().stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("Reviewed essence base scope differs");
    return Collections.unmodifiableMap(new LinkedHashMap<>(manifest));
  }
}
