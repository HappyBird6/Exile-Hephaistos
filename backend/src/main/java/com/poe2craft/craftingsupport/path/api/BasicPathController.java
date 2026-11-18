package com.poe2craft.craftingsupport.path.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.path.api.dto.BasicPathResponse;
import com.poe2craft.craftingsupport.path.application.BasicPathService;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import org.springframework.web.bind.annotation.*;

/**
 * 기본 제작 경로의 확률 API다. reader는 본문을 계산 요청으로 읽고 service는 확률을 계산한다. provenance는 규칙 출처를, firstHit는 최초
 * 성공까지의 분포를, recovery는 복구를 포함한 결과를 반환한다.
 */
@RestController
@RequestMapping("/api/v1/crafting/basic-paths")
public final class BasicPathController {
  private final BasicPathService service;
  private final BasicPathRequestReader reader;

  public BasicPathController(BasicPathService service, ObjectMapper mapper) {
    this.service = service;
    reader = new BasicPathRequestReader(mapper);
  }

  @GetMapping("/provenance")
  public BasicCurrencyState.Provenance provenance() {
    return service.provenance();
  }

  @PostMapping("/first-hit")
  public BasicPathResponse firstHit(@RequestBody JsonNode body) {
    var request = reader.read(body);
    return BasicPathResponse.from(
        request, service.calculate(request, false, () -> Thread.currentThread().isInterrupted()));
  }

  @PostMapping("/recovery")
  public BasicPathResponse recovery(@RequestBody JsonNode body) {
    var request = reader.read(body);
    return BasicPathResponse.from(
        request, service.calculate(request, true, () -> Thread.currentThread().isInterrupted()));
  }
}
