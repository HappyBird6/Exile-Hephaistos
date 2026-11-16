package com.poe2craft.craftingsupport.rag.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/** Small reviewed corpus: deterministic lexical retrieval plus mandatory/dependency closure. */
public final class StrategyKnowledge {
  public enum Mode {
    ALL,
    RETRIEVED
  }

  public record Document(
      String id,
      String text,
      List<String> keywords,
      List<String> requires,
      boolean required,
      String sha256) {}

  public record Selection(Mode mode, List<Document> documents, List<String> hits) {}

  private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+(?:_[\\p{L}\\p{N}]+)*");
  private final String version, policyVersion, digest;
  private final List<Document> documents;
  private final Map<String, Document> byId;

  public StrategyKnowledge(String version, String policyVersion, List<Document> documents) {
    this.version = Objects.requireNonNull(version);
    this.policyVersion = Objects.requireNonNull(policyVersion);
    this.documents = List.copyOf(documents);
    var index = new LinkedHashMap<String, Document>();
    for (var document : documents) {
      if (index.put(document.id(), document) != null
          || !hash(document.text()).equals(document.sha256()))
        throw new IllegalArgumentException("Invalid strategy document identity or digest");
    }
    byId = Collections.unmodifiableMap(index);
    for (var d : documents) closure(d.id(), new LinkedHashSet<>(), new HashSet<>());
    digest = hash(version + policyVersion + documents.toString());
  }

  public String version() {
    return version;
  }

  public String policyVersion() {
    return policyVersion;
  }

  public String digest() {
    return digest;
  }

  public Selection select(Mode mode, String query, int count) {
    if (mode == Mode.ALL)
      return new Selection(mode, documents, documents.stream().map(Document::id).toList());
    var terms = tokens(query);
    var frequencies = new HashMap<String, Integer>();
    var words = new HashMap<String, Set<String>>();
    for (var d : documents) {
      var termsInDocument = tokens(d.text() + " " + String.join(" ", d.keywords()));
      words.put(d.id(), termsInDocument);
      termsInDocument.forEach(t -> frequencies.merge(t, 1, Integer::sum));
    }
    var scores = new HashMap<String, Double>();
    for (var d : documents) {
      double score = 0;
      var keywords = tokens(String.join(" ", d.keywords()));
      for (var t : terms)
        if (words.get(d.id()).contains(t))
          score +=
              Math.log(1 + (double) documents.size() / frequencies.get(t))
                  * (keywords.contains(t) ? 3 : 1);
      scores.put(d.id(), score);
    }
    // List sorting is stable: ties preserve the reviewed manifest order.
    var hits =
        documents.stream()
            .filter(d -> !d.required() && scores.get(d.id()) > 0)
            .sorted(Comparator.comparingDouble((Document d) -> scores.get(d.id())).reversed())
            .limit(count)
            .map(Document::id)
            .toList();
    var selected = new LinkedHashSet<String>();
    documents.stream()
        .filter(Document::required)
        .forEach(d -> closure(d.id(), selected, new HashSet<>()));
    hits.forEach(id -> closure(id, selected, new HashSet<>()));
    return new Selection(
        mode, documents.stream().filter(d -> selected.contains(d.id())).toList(), hits);
  }

  private void closure(String id, Set<String> selected, Set<String> visiting) {
    if (!visiting.add(id)) throw new IllegalArgumentException("Strategy dependency cycle");
    var d = byId.get(id);
    if (d == null) throw new IllegalArgumentException("Missing strategy dependency");
    for (var dependency : d.requires()) closure(dependency, selected, visiting);
    selected.add(id);
    visiting.remove(id);
  }

  private static Set<String> tokens(String text) {
    var result = new HashSet<String>();
    var matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
    while (matcher.find()) result.add(matcher.group());
    return result;
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
