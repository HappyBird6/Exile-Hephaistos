package com.poe2craft.shared.item.text;

import com.poe2craft.shared.item.text.ItemModels.Item;
import com.poe2craft.shared.item.text.dto.ParseRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

/** 아이템 텍스트 파싱 요청을 처리한다. parse는 원문의 크기를 검사하고 service에 해석을 요청한다. */
@RestController
@RequestMapping("/api/v1/items")
public final class ItemTextController {
  private final ItemTextService service;

  public ItemTextController(ItemTextService service) {
    this.service = service;
  }

  @PostMapping("/parse")
  public Item parse(@RequestBody ParseRequest request) {
    if (request.text() != null
        && request.text().getBytes(StandardCharsets.UTF_8).length
            > ItemTextService.MAX_TEXT_BYTES) {
      throw new ItemTextTooLargeException();
    }
    return service.parseText(request.text());
  }
}
