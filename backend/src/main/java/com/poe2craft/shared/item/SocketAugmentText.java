package com.poe2craft.shared.item;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Resolves effect combinations, not clipboard line order or arbitrary socket positions. */
public final class SocketAugmentText {
  private SocketAugmentText() {}

  public record Resolution(List<ItemState.InstalledAugment> installed, List<String> unidentified) {}

  private record Effect(String key, List<BigDecimal> values) {}

  private record Candidate(String id, Effect effect) {}

  private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
  private static final Pattern COMPLEX =
      Pattern.compile(
          "\\b(per|while|when|if|every|socket|modifier|prefix|suffix|additional|grants|has)\\b",
          Pattern.CASE_INSENSITIVE);

  public static Resolution resolve(String base, Integer sockets, List<String> source) {
    return resolve(base, sockets, source, 1);
  }

  public static Resolution resolve(
      String base, Integer sockets, List<String> source, double multiplier) {
    if (source.isEmpty()) return new Resolution(List.of(), List.of());
    var maximum = BaseRegistry.socketExecutionMaximum(base);
    if (sockets == null || maximum == null || sockets < 1 || sockets > maximum)
      return new Resolution(List.of(), List.copyOf(source));
    if (source.stream().allMatch(line -> line.startsWith("Rune:"))) {
      var installed = new ArrayList<ItemState.InstalledAugment>();
      for (var line : source) {
        String name = line.replaceFirst("^Rune: (?:Slot [0-9]+: )?", "");
        String id = null;
        for (var entry : SocketAugments.catalog())
          if (SocketAugments.supports(base, entry.path("id").asText())
              && entry.path("names").path("en").asText().equals(name))
            id = entry.path("id").asText();
        if (id == null) return new Resolution(List.of(), List.copyOf(source));
        installed.add(new ItemState.InstalledAugment(installed.size(), id));
      }
      try {
        SocketAugments.validate(base, sockets, installed);
        return new Resolution(List.copyOf(installed), List.of());
      } catch (IllegalArgumentException invalid) {
        return new Resolution(List.of(), List.copyOf(source));
      }
    }
    // Recover unique structural effects first, even when other socket identities are ambiguous.
    var known = new ArrayList<ItemState.InstalledAugment>();
    var remaining = new ArrayList<>(source);
    for (var entry : SocketAugments.catalog()) {
      String id = entry.path("id").asText();
      if (!SocketAugments.supports(base, id)
          || (!SocketAugmentEffects.structural(id)
              && AugmentCraftingRules.effect(id).runeEffectPercent() == 0
              && !AugmentCraftingRules.effect(id).idolBonded())) continue;
      var rendered = SocketAugmentEffects.render(base, id, List.of(), multiplier, false);
      if (rendered.isEmpty()) continue;
      var trial = new ArrayList<>(remaining);
      boolean matches = true;
      for (var line : rendered) {
        int index = -1;
        for (int i = 0; i < trial.size(); i++)
          if (normalize(trial.get(i)).equals(normalize(line))) {
            index = i;
            break;
          }
        if (index < 0) {
          matches = false;
          break;
        }
        trial.remove(index);
      }
      if (matches && known.size() < sockets) {
        known.add(new ItemState.InstalledAugment(known.size(), id));
        remaining = trial;
      }
    }
    try {
      SocketAugments.validate(base, sockets, known);
    } catch (IllegalArgumentException invalid) {
      return new Resolution(List.of(), List.copyOf(source));
    }
    if (known.stream().anyMatch(a -> AugmentCraftingRules.effect(a.augmentId()).idolBonded())) {
      for (var augment : known) {
        if (!SocketAugments.kind(augment.augmentId()).equals("Idol")) continue;
        var ordinary =
            SocketAugmentEffects.render(base, augment.augmentId(), known, multiplier, false);
        var combined =
            new ArrayList<>(
                SocketAugmentEffects.render(base, augment.augmentId(), known, multiplier, true));
        combined.removeAll(ordinary);
        for (var line : combined) {
          for (int i = 0; i < remaining.size(); i++)
            if (normalize(remaining.get(i)).equals(normalize(line))) {
              remaining.remove(i);
              break;
            }
        }
      }
    }
    if (remaining.isEmpty()) return new Resolution(List.copyOf(known), List.of());
    var target = new TreeMap<String, List<BigDecimal>>();
    for (var line : remaining) {
      var effect = effect(line);
      if (effect == null) return new Resolution(List.copyOf(known), List.copyOf(remaining));
      target.merge(effect.key(), effect.values(), SocketAugmentText::add);
    }
    var candidates = new ArrayList<Candidate>();
    for (var entry : SocketAugments.catalog()) {
      String id = entry.path("id").asText();
      if (!SocketAugments.supports(base, id)
          || SocketAugmentEffects.structural(id)
          || AugmentCraftingRules.effect(id).runeEffectPercent() > 0) continue;
      var rendered = SocketAugmentEffects.render(base, id, known, multiplier, false);
      var effect = effect(String.join("\n", rendered));
      if (effect != null
          && target.containsKey(effect.key())
          && fits(effect.values(), target.get(effect.key())))
        candidates.add(new Candidate(id, effect));
    }
    candidates.sort(Comparator.comparing(Candidate::id));
    var solutions = new ArrayList<List<ItemState.InstalledAugment>>();
    search(base, sockets, candidates, 0, target, new ArrayList<>(known), solutions);
    return solutions.size() == 1
        ? new Resolution(solutions.getFirst(), List.of())
        : new Resolution(List.copyOf(known), List.copyOf(remaining));
  }

  private static void search(
      String base,
      int sockets,
      List<Candidate> candidates,
      int start,
      Map<String, List<BigDecimal>> remaining,
      List<ItemState.InstalledAugment> chosen,
      List<List<ItemState.InstalledAugment>> solutions) {
    if (solutions.size() > 1) return;
    if (remaining.values().stream().flatMap(List::stream).allMatch(n -> n.signum() == 0)) {
      try {
        SocketAugments.validate(base, sockets, chosen);
        solutions.add(List.copyOf(chosen));
      } catch (IllegalArgumentException invalid) {
        /* Source item-wide limits also constrain inversion. */
      }
      return;
    }
    if (chosen.size() == sockets) return;
    for (int i = start; i < candidates.size(); i++) {
      var candidate = candidates.get(i);
      var amounts = remaining.get(candidate.effect().key());
      if (!fits(candidate.effect().values(), amounts)) continue;
      var next = new TreeMap<>(remaining);
      var difference = new ArrayList<BigDecimal>();
      for (int k = 0; k < amounts.size(); k++)
        difference.add(amounts.get(k).subtract(candidate.effect().values().get(k)));
      next.put(candidate.effect().key(), difference);
      chosen.add(new ItemState.InstalledAugment(chosen.size(), candidate.id()));
      search(base, sockets, candidates, i, next, chosen, solutions);
      chosen.removeLast();
      if (solutions.size() > 1) return;
    }
  }

  private static boolean fits(List<BigDecimal> part, List<BigDecimal> total) {
    if (part.size() != total.size()) return false;
    for (int i = 0; i < part.size(); i++) if (part.get(i).compareTo(total.get(i)) > 0) return false;
    return true;
  }

  private static List<BigDecimal> add(List<BigDecimal> a, List<BigDecimal> b) {
    var result = new ArrayList<BigDecimal>();
    if (a.size() != b.size())
      throw new IllegalArgumentException("Conflicting rune effect dimensions");
    for (int i = 0; i < a.size(); i++) result.add(a.get(i).add(b.get(i)));
    return result;
  }

  private static Effect effect(String source) {
    String text = normalize(source);
    if (COMPLEX.matcher(text).find()) return null;
    var numbers = NUMBER.matcher(text);
    var amounts = new ArrayList<BigDecimal>();
    while (numbers.find()) amounts.add(new BigDecimal(numbers.group()));
    if (amounts.isEmpty() || amounts.stream().allMatch(n -> n.signum() == 0)) return null;
    return new Effect(NUMBER.matcher(text).replaceAll("#"), amounts);
  }

  private static String normalize(String text) {
    return SocketAugmentEffects.visible(text)
        .replaceAll("(?m)^\\{.*?}\\s*", "")
        .replaceAll("\\s*\\(rune\\)", "")
        .trim()
        .replaceAll("\\s+", " ")
        .toLowerCase(Locale.ROOT);
  }

  public static boolean sameEffects(List<String> a, List<String> b) {
    return fingerprint(a).equals(fingerprint(b));
  }

  private static Map<String, List<BigDecimal>> fingerprint(List<String> source) {
    var result = new TreeMap<String, List<BigDecimal>>();
    for (var line : source) {
      var effect = effect(line);
      String key = effect == null ? "opaque:" + normalize(line) : effect.key();
      var values = effect == null ? List.of(BigDecimal.ONE) : effect.values();
      result.merge(key, values, SocketAugmentText::add);
    }
    result.replaceAll(
        (key, values) -> values.stream().map(BigDecimal::stripTrailingZeros).toList());
    return result;
  }

  public static List<String> effects(ItemState state) {
    return effects(state, null);
  }

  public static List<String> effects(ItemState state, ItemCatalog catalog) {
    var result = new ArrayList<String>();
    double multiplier = SocketAugmentEffects.multiplier(state, catalog);
    boolean idolBonded =
        state.installedAugments().stream()
            .anyMatch(a -> AugmentCraftingRules.effect(a.augmentId()).idolBonded());
    for (var installed : state.installedAugments())
      result.addAll(
          SocketAugmentEffects.render(
              state.baseItemId(),
              installed.augmentId(),
              state.installedAugments(),
              multiplier,
              idolBonded && SocketAugments.kind(installed.augmentId()).equals("Idol")));
    if (state.observations() != null) {
      double anchor =
          state.observations().augmentEffectScale() == null
              ? 1
              : state.observations().augmentEffectScale();
      double ratio =
          SocketAugmentEffects.runeMultiplier(state.installedAugments(), multiplier) / anchor;
      for (var line : state.observations().augmentEffects())
        result.add(SocketAugmentEffects.observed(line, ratio));
    }
    return List.copyOf(result);
  }

  public static boolean affectsCrafting(List<String> effects) {
    for (var line : effects) {
      if (Pattern.compile(
              "\\b(prefix|suffix|modifier|rarity|quality|corrupt|fractur|socket)",
              Pattern.CASE_INSENSITIVE)
          .matcher(line)
          .find()) return true;
      boolean recognized = false;
      var observed = effect(line);
      for (var entry : SocketAugments.catalog())
        for (var equipment : entry.path("effects")) {
          String source = equipment.path("en").asText();
          var known = effect(source);
          if (normalize(source).equals(normalize(line))
              || (observed != null && known != null && observed.key().equals(known.key())))
            recognized = true;
        }
      if (!recognized) return true;
    }
    return false;
  }
}
