package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PathSearchDataTest {
  final ObjectMapper json = new ObjectMapper();
  final ItemCatalog original = ItemCatalogLoader.loadDefault();
  final String stat = "fixture_reviewed_flat";

  ItemCatalog catalog() {
    var template = original.modifiers().get("amulet:prefix:adept-s");
    var added =
        new ModifierDefinition(
            template.id(),
            template.name(),
            template.layer(),
            template.affixType(),
            template.familyIds(),
            1,
            1,
            1,
            template.text(),
            List.of(new ModifierDefinition.StatRange(stat, 1, 2)),
            template.tags(),
            template.sourceUrl());
    var old = original.modifiers().get("amulet:suffix:of-the-seal");
    var miss =
        new ModifierDefinition(
            old.id(),
            old.name(),
            old.layer(),
            old.affixType(),
            old.familyIds(),
            1,
            1,
            1,
            old.text(),
            List.of(new ModifierDefinition.StatRange(old.stats().getFirst().id(), 6, 6)),
            old.tags(),
            old.sourceUrl());
    var m = original.metadata();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            m.snapshotId(),
            m.retrievedAt(),
            m.sourceUrl(),
            m.weightPolicy(),
            m.rawSha256(),
            m.detailsSha256(),
            1,
            1,
            1,
            1),
        original.base(),
        List.of(added, miss, original.modifiers().get(original.base().implicitModifierId())));
  }

  GoalDefinitions definitions() throws Exception {
    var data =
        (ObjectNode)
            json.readTree(
                getClass().getResourceAsStream("/crafting/goalfilter/definitions-v1.json"));
    ((ArrayNode) data.get("stats"))
        .add(
            json.createObjectNode()
                .put("sourceStatId", stat)
                .put("label", "Fixture flat stat")
                .put("unit", "flat"));
    return GoalDefinitionsLoader.read(new ByteArrayInputStream(json.writeValueAsBytes(data)));
  }

  Create request(PathSearchService service, GoalCatalogIndex index, String key) {
    var initial = SolarAmulet.initial(catalog());
    var item =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(
                new ModifierInstance(
                    "amulet:suffix:of-the-seal", Map.of("base_cold_damage_resistance_%", 6L))),
            Set.of(),
            null,
            null);
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new GoalFilter.General(
                item.baseItemId(),
                new GoalFilter.Range(BigDecimal.ONE, BigDecimal.valueOf(100)),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new GoalFilter.Group(
                    "g",
                    GoalFilter.Type.COUNT,
                    false,
                    new GoalFilter.Range(BigDecimal.ONE, null),
                    List.of(
                        new GoalFilter.Entry(
                            "e",
                            GoalCatalogIndex.id("explicit", stat),
                            "flat",
                            new GoalFilter.Range(BigDecimal.valueOf(2), BigDecimal.valueOf(2)),
                            null,
                            false)))));
    return new Create(
        1,
        key,
        new BasicCurrencyState(item, service.provenance()),
        goal,
        List.of(),
        List.of("0", "1", "2", "100"));
  }

  @Test
  void dataOnlyReviewedStatUsesCommonPredicateSearchAndVersionInvalidation() throws Exception {
    var catalog = catalog();
    var definitions = definitions();
    var index = new GoalCatalogIndex(List.of(catalog), definitions, "synthetic-v1");
    var tasks = new ArrayDeque<Runnable>();
    try (var service =
        new PathSearchService(
            catalog,
            new GoalFilterService(index),
            "synthetic-v1",
            json,
            Clock.systemUTC(),
            tasks::add,
            50000)) {
      var request = request(service, index, "added");
      var queued = service.create("owner", request);
      tasks.remove().run();
      var done = service.get("owner", queued.jobId());
      assertThat(done.status()).isEqualTo("COMPLETED");
      assertThat(done.recommendations().getFirst().points().get(1).lower().fraction())
          .isEqualTo(Fraction.of(1, 4));
      var failure =
          done.graph().executions().stream()
              .filter(e -> e.policyId().equals("solar-policy-1"))
              .filter(
                  e ->
                      done.graph().nodes().stream()
                          .anyMatch(
                              n ->
                                  n.id().equals(e.stateId())
                                      && n.goalStatus().equals("NO_MATCH")
                                      && !n.id().equals(request.start().canonicalKey())))
              .findFirst()
              .orElseThrow();
      var recovery =
          service.recover(
              "owner",
              done.jobId(),
              new Recover(
                  1,
                  "recover",
                  done.revision(),
                  failure.id(),
                  request.start().canonicalKey(),
                  List.of("0", "1", "2")));
      tasks.remove().run();
      var recovered = service.get("owner", recovery.jobId());
      assertThat(recovered.status()).isEqualTo("COMPLETED");
      assertThat(recovered.recommendations().getFirst().points().get(1).lower().fraction())
          .isEqualTo(Fraction.of(1, 2));
      assertThat(service.get("owner", done.jobId())).isEqualTo(done);
      var hit =
          done.graph().nodes().stream()
              .filter(n -> n.goalStatus().equals("MATCH"))
              .findFirst()
              .orElseThrow();
      assertThatThrownBy(
              () ->
                  service.recover(
                      "owner",
                      done.jobId(),
                      new Recover(
                          1,
                          "non-ancestor",
                          done.revision(),
                          failure.id(),
                          hit.id(),
                          List.of("1"))))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("INVALID_RECOVERY_CHECKPOINT"));
      var changed =
          new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load(), "synthetic-v1");
      assertThat(changed.version()).isNotEqualTo(index.version());
      var changedGoals = new GoalFilterService(changed);
      assertThatThrownBy(() -> changedGoals.evaluate(request.start().item(), request.goal()))
          .isInstanceOfSatisfying(
              GoalFilterService.InvalidGoal.class, e -> assertThat(e.staleCatalog()).isTrue());
      var deleted =
          new GoalFilter(1, changed.version(), request.goal().general(), request.goal().groups());
      // An unreviewed source is retained as an unknown-unit catalog entry, never reinterpreted.
      assertThatThrownBy(() -> changedGoals.evaluate(request.start().item(), deleted))
          .isInstanceOf(GoalFilterService.InvalidGoal.class);
      var all = new ArrayList<>(definitions.stats());
      all.removeIf(s -> s.sourceStatId().equals("base_cold_damage_resistance_%"));
      assertThatThrownBy(
              () -> new GoalDefinitions(1, definitions.ruleVersion(), all, definitions.pseudos()))
          .isInstanceOf(IllegalArgumentException.class);
      var missingItem =
          new ItemState(
              request.start().item().snapshotId(),
              request.start().item().baseItemId(),
              82,
              ItemState.Rarity.RARE,
              request.start().item().implicits(),
              List.of(new ModifierInstance("deleted-modifier", Map.of(stat, 1L))),
              Set.of(),
              null,
              null);
      assertThatThrownBy(
              () ->
                  service.create(
                      "owner",
                      new Create(
                          1,
                          "deleted",
                          new BasicCurrencyState(missingItem, service.provenance()),
                          request.goal(),
                          List.of(),
                          List.of("1"))))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("INVALID_ITEM"));
    }
  }

  @Test
  void rootHitAbsorbsAndCompletedCancelDoesNotChangeRevision() throws Exception {
    var catalog = catalog();
    var index = new GoalCatalogIndex(List.of(catalog), definitions(), "synthetic-v1");
    var tasks = new ArrayDeque<Runnable>();
    try (var service =
        new PathSearchService(
            catalog,
            new GoalFilterService(index),
            "synthetic-v1",
            json,
            Clock.systemUTC(),
            tasks::add,
            100)) {
      var r = request(service, index, "hit");
      var item = r.start().item();
      item =
          new ItemState(
              item.snapshotId(),
              item.baseItemId(),
              item.itemLevel(),
              item.rarity(),
              item.implicits(),
              List.of(new ModifierInstance("amulet:prefix:adept-s", Map.of(stat, 2L))),
              item.conditions(),
              item.augmentSockets(),
              item.catalystQuality());
      var accepted =
          service.create(
              "owner",
              new Create(
                  1,
                  "hit",
                  new BasicCurrencyState(item, service.provenance()),
                  r.goal(),
                  List.of(),
                  r.observations()));
      tasks.remove().run();
      var done = service.get("owner", accepted.jobId());
      assertThat(done.recommendations())
          .allSatisfy(
              c ->
                  assertThat(c.points())
                      .allSatisfy(p -> assertThat(p.lower().fraction()).isEqualTo(Fraction.ONE)));
      assertThat(
              service.mutate(
                  "owner", done.jobId(), new Mutation(1, "CANCEL", "cancel", done.revision())))
          .isEqualTo(done);
      assertThat(done.graph().edges()).isEmpty();
    }
  }

  @Test
  void enormousObservationPreservesKnownLowerButCannotFakeResumePastOutputLimit() throws Exception {
    var catalog = catalog();
    var index = new GoalCatalogIndex(List.of(catalog), definitions(), "synthetic-v1");
    var tasks = new ArrayDeque<Runnable>();
    try (var service =
        new PathSearchService(
            catalog,
            new GoalFilterService(index),
            "synthetic-v1",
            json,
            Clock.systemUTC(),
            tasks::add,
            100)) {
      var r = request(service, index, "large-observation");
      var queued =
          service.create(
              "owner",
              new Create(
                  1,
                  r.clientRequestId(),
                  r.start(),
                  r.goal(),
                  List.of(),
                  List.of("1", Long.toString(Long.MAX_VALUE))));
      tasks.remove().run();
      var result = service.get("owner", queued.jobId());
      assertThat(result.status()).isEqualTo("PAUSED");
      assertThat(result.reasonCode()).isEqualTo("FRACTION_OUTPUT_LIMIT");
      assertThat(result.resumable()).isFalse();
      var points = result.recommendations().getFirst().points();
      assertThat(points.getFirst().lower().fraction()).isEqualTo(Fraction.of(1, 4));
      assertThat(points.getLast().status()).isEqualTo("PARTIAL");
      assertThat(points.getLast().upper().fraction()).isEqualTo(Fraction.ONE);
      assertThat(
              points
                  .getLast()
                  .lower()
                  .fraction()
                  .numerator()
                  .multiply(java.math.BigInteger.valueOf(4)))
          .isGreaterThan(points.getLast().lower().fraction().denominator());
      assertThatThrownBy(
              () ->
                  service.mutate(
                      "owner",
                      result.jobId(),
                      new Mutation(1, "RESUME", "cannot-resume", result.revision())))
          .isInstanceOfSatisfying(
              Rejected.class, e -> assertThat(e.code()).isEqualTo("JOB_NOT_RESUMABLE"));
    }
  }
}
