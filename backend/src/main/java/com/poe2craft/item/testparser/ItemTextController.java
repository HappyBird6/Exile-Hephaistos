package com.poe2craft.item.testparser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.poe2craft.item.testparser.ItemTextModels.ParsedItemText;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
public final class ItemTextController {
  private final ItemTextService service;

  public ItemTextController(ItemTextService service) {
    this.service = service;
  }

  @PostMapping("/parse")
  public ParsedItemText parse(@RequestBody ParseRequest request) {
    if (request.text() != null
        && request.text().getBytes(StandardCharsets.UTF_8).length
            > ItemTextService.MAX_TEXT_BYTES) {
      throw new ItemTextTooLargeException();
    }
    return service.parseText(request.text());
  }

  public record ParseRequest(String text) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ParseRequest fromJson(Map<String, Object> body) {
      Object text = body.get("text");
      if (text != null && !(text instanceof String)) {
        throw new IllegalArgumentException("text must be a string");
      }
      return new ParseRequest((String) text);
    }
  }

  static final class ItemTextTooLargeException extends RuntimeException {}
}
