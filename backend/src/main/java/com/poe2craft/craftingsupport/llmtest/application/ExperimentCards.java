package com.poe2craft.craftingsupport.llmtest.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 빌드에서 추출한 보강 카드와 공통 프롬프트를 한 번 읽는다. 목록에는 목표 표시 필드만 담고, preview와 send는 같은 입력 문자열을 사용한다. */
@Service
public class ExperimentCards {
  private final String prompt;
  private final JsonNode cards;
  private final List<GoalCard> goals;
  private final CardModelClient client;
  private final CardCallBudget budget;

  public ExperimentCards(ObjectMapper mapper, CardModelClient client, CardCallBudget budget)
      throws IOException {
    this.client = client;
    this.budget = budget;
    try (var stream = new ClassPathResource("llmtest/cards.json").getInputStream()) {
      var catalog = mapper.readTree(stream);
      prompt = catalog.required("prompt").asText();
      cards = catalog.required("cards");
    }
    var summaries = new ArrayList<GoalCard>();
    cards
        .properties()
        .forEach(
            entry -> {
              var input = entry.getValue().required("input");
              summaries.add(
                  new GoalCard(
                      entry.getKey(),
                      entry.getValue().required("baseId").asText(),
                      entry.getValue().required("templates"),
                      input.required("base").asText(),
                      input.required("ilvl").asInt(),
                      input.required("optionRefs"),
                      input.required("goalGroups"),
                      input.get("derived")));
            });
    goals = List.copyOf(summaries);
  }

  /** 카드 식별자, 베이스 이름과 레벨, 옵션 참조, 목표 그룹 및 합산 속성으로 화면의 목표 카드를 구성한다. */
  public record GoalCard(
      String id,
      String baseId,
      JsonNode templates,
      String base,
      int ilvl,
      JsonNode optionRefs,
      JsonNode goalGroups,
      JsonNode derived) {}

  /** 목록과 전송 가능 여부를 제공한다. */
  public record Index(List<GoalCard> cards, boolean configured) {}

  /** 모델에 보낼 시스템 메시지와 사용자 메시지 원문이다. */
  public record Preview(String prompt, String inputJson) {}

  /** 사용자가 선택한 카드의 식별자다. */
  public record Selection(String cardId) {}

  /** 이번 모델 호출에서 반환된 메시지 본문이다. */
  public record Reply(String answer) {}

  public Index index() {
    return new Index(goals, client.configured());
  }

  private JsonNode card(String id) {
    var card = id == null ? null : cards.get(id);
    if (card == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown goal card");
    return card;
  }

  private Preview messages(JsonNode card) {
    return new Preview(prompt, card.required("input").toPrettyString());
  }

  public Preview preview(String id) {
    return messages(card(id));
  }

  public Reply send(Selection selection) {
    var card = card(selection.cardId());
    if (!client.configured())
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Model key is missing");
    budget.reserve();
    return new Reply(client.send(messages(card), card.required("schema")));
  }
}
