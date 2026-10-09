package com.poe2craft.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.security.*;
import java.util.*;

/**
 * Every bundled consumer verifies membership before parsing; archive-only files may also be sealed.
 */
public final class SealedResources {
  private SealedResources() {}

  private static final Set<String> FIXED =
      Set.of(
          "catalog/attuned-wand/base.raw.json",
          "catalog/attuned-wand/catalog.json",
          "catalog/attuned-wand/details.raw.json",
          "catalog/attuned-wand/perfect-essences.catalog.json",
          "catalog/attuned-wand/perfect-essences.raw.json",
          "catalog/base-policies.json",
          "catalog/crude-bow/base.raw.json",
          "catalog/crude-bow/catalog.json",
          "catalog/crude-bow/details.raw.json",
          "catalog/crude-bow/perfect-essences.catalog.json",
          "catalog/crude-bow/perfect-essences.raw.json",
          "catalog/diamond/base.raw.json",
          "catalog/diamond/catalog.json",
          "catalog/diamond/details.raw.json",
          "catalog/emerald/base.raw.json",
          "catalog/emerald/catalog.json",
          "catalog/emerald/details.raw.json",
          "catalog/iron-ring/base.raw.json",
          "catalog/iron-ring/catalog.json",
          "catalog/iron-ring/details.raw.json",
          "catalog/iron-ring/perfect-essences.catalog.json",
          "catalog/iron-ring/perfect-essences.raw.json",
          "catalog/rattling-sceptre/base.raw.json",
          "catalog/rattling-sceptre/catalog.json",
          "catalog/rattling-sceptre/details.raw.json",
          "catalog/rattling-sceptre/perfect-essences.catalog.json",
          "catalog/rattling-sceptre/perfect-essences.raw.json",
          "catalog/rawhide-belt/base.raw.json",
          "catalog/rawhide-belt/catalog.json",
          "catalog/rawhide-belt/details.raw.json",
          "catalog/rawhide-belt/perfect-essences.catalog.json",
          "catalog/rawhide-belt/perfect-essences.raw.json",
          "catalog/ruby/base.raw.json",
          "catalog/ruby/catalog.json",
          "catalog/ruby/details.raw.json",
          "catalog/rusted-cuirass/base.raw.json",
          "catalog/rusted-cuirass/catalog.json",
          "catalog/rusted-cuirass/details.raw.json",
          "catalog/rusted-cuirass/perfect-essences.catalog.json",
          "catalog/rusted-cuirass/perfect-essences.raw.json",
          "catalog/rusted-greathelm/base.raw.json",
          "catalog/rusted-greathelm/catalog.json",
          "catalog/rusted-greathelm/details.raw.json",
          "catalog/rusted-greathelm/perfect-essences.catalog.json",
          "catalog/rusted-greathelm/perfect-essences.raw.json",
          "catalog/sapphire/base.raw.json",
          "catalog/sapphire/catalog.json",
          "catalog/sapphire/details.raw.json",
          "catalog/solar-amulet/abyss-essence.catalog.json",
          "catalog/solar-amulet/abyss-essence.raw.json",
          "catalog/solar-amulet/base.raw.json",
          "catalog/solar-amulet/breach-essence.catalog.json",
          "catalog/solar-amulet/breach-essence.raw.json",
          "catalog/solar-amulet/catalog.json",
          "catalog/solar-amulet/details.raw.json",
          "catalog/solar-amulet/perfect-enhancement.catalog.json",
          "catalog/solar-amulet/perfect-enhancement.raw.json",
          "catalog/solar-amulet/perfect-infinite.catalog.json",
          "catalog/solar-amulet/perfect-infinite.raw.json",
          "catalog/solar-amulet/runic-alloy.catalog.json",
          "catalog/solar-amulet/runic-alloy.raw.json",
          "catalog/stocky-mitts/abyss-essence.catalog.json",
          "catalog/stocky-mitts/abyss-essence.raw.json",
          "catalog/stocky-mitts/base.raw.json",
          "catalog/stocky-mitts/catalog.json",
          "catalog/stocky-mitts/details.raw.json",
          "catalog/stocky-mitts/horror-essence.catalog.json",
          "catalog/stocky-mitts/horror-essence.raw.json",
          "catalog/stocky-mitts/perfect-grounding-opulence.catalog.json",
          "catalog/stocky-mitts/perfect-grounding-opulence.raw.json",
          "catalog/stocky-mitts/prismatic-alloy.catalog.json",
          "catalog/stocky-mitts/prismatic-alloy.raw.json",
          "catalog/stocky-mitts/reviewed-alloys.catalog.json",
          "catalog/stocky-mitts/reviewed-alloys.raw.json",
          "catalog/stocky-mitts/scalar-alloys.catalog.json",
          "catalog/stocky-mitts/scalar-alloys.raw.json",
          "catalog/time-lost-diamond/base.raw.json",
          "catalog/time-lost-diamond/catalog.json",
          "catalog/time-lost-diamond/details.raw.json",
          "catalog/time-lost-emerald/base.raw.json",
          "catalog/time-lost-emerald/catalog.json",
          "catalog/time-lost-emerald/details.raw.json",
          "catalog/time-lost-ruby/base.raw.json",
          "catalog/time-lost-ruby/catalog.json",
          "catalog/time-lost-ruby/details.raw.json",
          "catalog/time-lost-sapphire/base.raw.json",
          "catalog/time-lost-sapphire/catalog.json",
          "catalog/time-lost-sapphire/details.raw.json",
          "catalog/top-base-essences.json",
          "catalog/top-bases.json",
          "crafting/goalfilter/bases-v1.json",
          "crafting/goalfilter/definitions-v1.json",
          "crafting/registry-v2.json",
          "crafting/supported-base-sets-v1.json");

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
      for (var file : List.of("catalog.json", "base.raw.json", "details.raw.json"))
        paths.add("catalog/" + pool + "/" + file);
    }
    return Collections.unmodifiableSet(paths);
  }
}
