package com.poe2craft.shared.item;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 복사한 효과 문장에서 가능한 증강물 조합을 역산하고 장착 효과를 표시한다. 슬롯의 원래 순서는 추측하지 않는다.
 *
 * <p>복사한 효과 문장에서 가능한 증강물 조합을 역산하고 장착 효과를 표시한다. 슬롯의 원래 순서는 추측하지 않는다.
 *
 * <p>private 생성자는 생성을 막는다. NUMBER는 수치, COMPLEX는 단순 합산으로 역산할 수 없는 조건문을 구별하는 정규식이다. Resolution은 확정 장착
 * 목록과 미해석 문장, Effect는 문장 구조와 수치, Candidate는 증강물 ID와 그 효과다. 카탈로그는 SocketAugments.definitions의 읽기 뷰를
 * 재사용한다.
 *
 * <p>resolve의 두 오버로드는 base/sockets/source와 선택적 multiplier로 조합을 찾는다. known은 확정된 구조 효과, remaining은 아직
 * 소비하지 않은 문장, target은 구조별 합계, candidates는 가능한 증강물, solutions는 유효 조합이다. 유일하지 않거나 해석 불가능하면 원문을
 * unidentified에 남긴다. search는 후보 순서를 고정한 역추적으로 chosen/solutions를 갱신하되 재귀 복귀 때 chosen을 복원한다. fits는
 * 수치별 포함 여부, add는 같은 차원 수치 합(차원 불일치 예외), effect는 단순 문장 파싱, normalize는 표시 장식·공백·대소문자를 정규화한다.
 *
 * <p>sameEffects는 fingerprint의 구조별 합계를 비교한다. fingerprint는 해석 불가 문장도 opaque 키로 보존한다. effects의 두
 * 오버로드는 상태와 선택적 카탈로그로 증강물 배율·우상 결합·미해석 관측 효과를 렌더링한다. affectsCrafting은 문장이 제작 구조를 바꾸거나 알려지지 않은 효과인지
 * 보수적으로 판정한다. 모든 공개 메서드는 입력 상태·공유 JSON을 수정하지 않는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code NUMBER}: 숫자 ({@code Pattern}).
 *   <li>{@code COMPLEX}: COMPLEX ({@code Pattern}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SocketAugmentText()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code resolve(base, sockets, source)}: 해결의 입력에 따른 분기·결과를 처리한다. 입력: {@code base}(베이스),
 *       {@code sockets}(장착홈수), {@code source}(출처). 반환: {@code Resolution}.
 *   <li>{@code resolve(base, sockets, source, multiplier)}: 해결 처리에서 {@code source.isEmpty}, {@code
 *       BaseRegistry.socketExecutionMaximum}을(를) 사용한다. 입력: {@code base}(베이스), {@code
 *       sockets}(장착홈수), {@code source}(출처), {@code multiplier}(배수). 반환: {@code Resolution}.
 *   <li>{@code search(base, sockets, candidates, start, remaining, chosen, solutions)}: 탐색 처리에서
 *       {@code solutions.size}, {@code remaining.values}을(를) 사용한다. 입력: {@code base}(베이스), {@code
 *       sockets}(장착홈수), {@code candidates}(후보목록), {@code start}(시작값), {@code remaining}(남은), {@code
 *       chosen}(chosen), {@code solutions}(solutions).
 *   <li>{@code fits(part, total)}: fits 처리에서 {@code part.size}, {@code total.size}을(를) 사용한다. 입력:
 *       {@code part}(part), {@code total}(합계). 반환: {@code boolean}.
 *   <li>{@code add(a, b)}: 값 또는 항목을 더한다. 입력: {@code a}(a), {@code b}(b). 반환: {@code
 *       List<BigDecimal>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code effect(source)}: 효과 처리에서 {@code COMPLEX.matcher}, {@code NUMBER.matcher}을(를) 사용한다.
 *       입력: {@code source}(출처). 반환: {@code Effect}.
 *   <li>{@code normalize(text)}: 정규화 처리에서 {@code SocketAugmentEffects.visible}을(를) 사용한다. 입력: {@code
 *       text}(텍스트). 반환: {@code String}.
 *   <li>{@code sameEffects(a, b)}: 동일·효과목록의 입력에 따른 분기·결과를 처리한다. 입력: {@code a}(a), {@code b}(b). 반환:
 *       {@code boolean}.
 *   <li>{@code fingerprint(source)}: 지문 처리에서 {@code effect.key}, {@code effect.values}을(를) 사용한다.
 *       입력: {@code source}(출처). 반환: {@code Map<String, List<BigDecimal>>}.
 *   <li>{@code effects(state)}: 효과목록의 입력에 따른 분기·결과를 처리한다. 입력: {@code state}(상태). 반환: {@code
 *       List<String>}.
 *   <li>{@code effects(state, catalog)}: 효과목록 처리에서 {@code SocketAugmentEffects.multiplier}, {@code
 *       state.installedAugments}을(를) 사용한다. 입력: {@code state}(상태), {@code catalog}(카탈로그). 반환: {@code
 *       List<String>}.
 *   <li>{@code affectsCrafting(effects)}: affects·제작 처리에서 {@code Pattern.compile}, {@code
 *       SocketAugments.definitions}을(를) 사용한다. 입력: {@code effects}(효과목록). 반환: {@code boolean}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Resolution}: installed는 유일하게 식별한 장착 목록, unidentified는 추측하지 않고 보존한 원문이다.
 *   <li>{@code Effect}: key는 수치를 #으로 치환한 문장 구조, values는 등장 순서의 수치 벡터다.
 *   <li>{@code Candidate}: id는 후보 증강물 식별자, effect는 그 후보가 기여하는 단순 합산 효과다.
 * </ul>
 */
public final class SocketAugmentText {
  private SocketAugmentText() {}

  /**
   * installed는 유일하게 식별한 장착 목록, unidentified는 추측하지 않고 보존한 원문이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code installed}: 장착된 ({@code List<ItemState.InstalledAugment>}).
   *   <li>{@code unidentified}: unidentified ({@code List<String>}).
   * </ul>
   */
  public record Resolution(List<ItemState.InstalledAugment> installed, List<String> unidentified) {}

  /**
   * key는 수치를 #으로 치환한 문장 구조, values는 등장 순서의 수치 벡터다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code String}).
   *   <li>{@code values}: 출처 단위로 보존하는 수치 값 ({@code List<BigDecimal>}).
   * </ul>
   */
  private record Effect(String key, List<BigDecimal> values) {}

  /**
   * id는 후보 증강물 식별자, effect는 그 후보가 기여하는 단순 합산 효과다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code effect}: 효과 ({@code Effect}).
   * </ul>
   */
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
        for (var entry : SocketAugments.definitions())
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
    for (var entry : SocketAugments.definitions()) {
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
    for (var entry : SocketAugments.definitions()) {
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
      for (var entry : SocketAugments.definitions())
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
