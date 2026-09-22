package com.poe2craft.season.presentation;

import com.poe2craft.season.api.CrawlModels.*;
import com.poe2craft.season.application.service.CrawlService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class CrawlController {
  private final CrawlService service;

  public CrawlController(CrawlService service) {
    this.service = service;
  }

  @GetMapping("/crawl-settings")
  public Settings settings() {
    return service.settings();
  }

  @PutMapping("/crawl-settings")
  public Settings update(@RequestBody Update input) {
    return service.update(input);
  }

  @GetMapping("/crawl-runs")
  public Map<String, List<Run>> runs() {
    return Map.of("runs", service.runs());
  }

  @PostMapping("/crawl-runs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Run launch() {
    return service.launch();
  }

  @GetMapping("/crawl-runs/{id}")
  public Run run(@PathVariable UUID id) {
    return service.get(id);
  }
}
