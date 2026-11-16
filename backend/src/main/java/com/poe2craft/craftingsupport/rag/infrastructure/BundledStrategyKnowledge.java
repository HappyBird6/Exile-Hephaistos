package com.poe2craft.craftingsupport.rag.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.rag.application.StrategyKnowledge;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.ClassPathResource;

/**
 * Only manifest-listed Markdown is read; reference notes and historical policies never enter
 * retrieval.
 */
public final class BundledStrategyKnowledge {
  private BundledStrategyKnowledge() {}

  public static StrategyKnowledge load(ObjectMapper json) {
    try {
      var manifest = json.readTree(read("corpus.json"));
      if (manifest.path("schemaVersion").asInt() != 1)
        throw new IOException("Unsupported corpus schema");
      var required = new HashSet<String>();
      manifest.path("requiredDocuments").forEach(n -> required.add(n.asText()));
      var documents = new ArrayList<StrategyKnowledge.Document>();
      for (var entry : manifest.path("retrievalDocuments")) {
        var path = entry.path("path").asText();
        if (!path.matches("knowledge/[a-z0-9-]+\\.md")
            || !entry.path("scope").asText().equals("automatic-main"))
          throw new IOException("Invalid retrieval allowlist");
        var dependencies = new ArrayList<String>();
        entry.path("requires").forEach(n -> dependencies.add(n.asText()));
        var keywords = new ArrayList<String>();
        entry.path("keywords").forEach(n -> keywords.add(n.asText()));
        var id = entry.path("id").asText();
        documents.add(
            new StrategyKnowledge.Document(
                id,
                read(path),
                List.copyOf(keywords),
                List.copyOf(dependencies),
                required.contains(id),
                entry.path("sha256").asText()));
      }
      if (documents.isEmpty()
          || required.isEmpty()
          || !documents.stream().map(StrategyKnowledge.Document::id).toList().containsAll(required))
        throw new IOException("Incomplete strategy corpus");
      return new StrategyKnowledge(
          manifest.path("corpusVersion").asText(),
          manifest.path("policyVersion").asText(),
          documents);
    } catch (IOException error) {
      throw new IllegalStateException("Cannot load reviewed strategy corpus", error);
    }
  }

  private static String read(String path) throws IOException {
    try (var input = new ClassPathResource("support-rag/" + path).getInputStream()) {
      var bytes = input.readNBytes(262145);
      if (bytes.length > 262144) throw new IOException("Strategy resource exceeds bound");
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }
}
