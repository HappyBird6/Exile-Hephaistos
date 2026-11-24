package com.poe2craft.craftingsupport.llmtest.api;

import com.poe2craft.craftingsupport.llmtest.application.ExperimentCards;
import org.springframework.web.bind.annotation.*;

/** cards 서비스에 목표 목록, 선택 카드 미리보기, 명시적 모델 전송 요청을 각각 매핑한다. */
@RestController
@RequestMapping("/api/v1/support-test")
public class ExperimentCardController {
  private final ExperimentCards cards;

  public ExperimentCardController(ExperimentCards cards) {
    this.cards = cards;
  }

  @GetMapping("/cards")
  public ExperimentCards.Index index() {
    return cards.index();
  }

  @GetMapping("/cards/{id}")
  public ExperimentCards.Preview preview(@PathVariable String id) {
    return cards.preview(id);
  }

  @PostMapping(value = "/send", consumes = "application/json")
  public ExperimentCards.Reply send(@RequestBody ExperimentCards.Selection selection) {
    return cards.send(selection);
  }
}
