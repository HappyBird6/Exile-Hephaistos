package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.repeat.ChaosDisplayResources;
import com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;
import com.poe2craft.workbench.repeat.ChaosRepeatService;
import com.poe2craft.workbench.repeat.ChaosRepeatSubmission;
import com.poe2craft.workbench.repeat.RepeatRejected;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 카오스 반복 제작 HTTP 요청을 받는다. submission은 원문 검사와 실행을 담당한다. 생성자는 실행 설정과 요청 저장소를 연결한다. repeat는 결과 또는 부분
 * 실패를 HTTP 응답으로 반환하고 cancel은 취소 상태를 응답한다.
 */
@RestController
public final class ChaosRepeatController {
  private final ChaosRepeatSubmission submission;

  public ChaosRepeatController(CraftingOperations workbench, ObjectMapper mapper) {
    this(workbench, mapper, (ChaosRepeatService) null);
  }

  @Autowired
  public ChaosRepeatController(
      CraftingOperations workbench,
      ObjectMapper mapper,
      BackendSettings settings,
      com.poe2craft.workbench.repeat.RepeatRequestStore requests) {
    submission =
        new ChaosRepeatSubmission(
            workbench,
            mapper,
            new ChaosRepeatService(
                workbench,
                new ChaosDisplayResources(mapper),
                RulesetBoundary.identity(),
                settings.workbench()),
            requests);
  }

  ChaosRepeatController(
      CraftingOperations workbench, ObjectMapper mapper, ChaosRepeatService supplied) {
    submission = new ChaosRepeatSubmission(workbench, mapper, supplied, null);
  }

  @PostMapping(
      value = "/api/v1/crafting/workbench/chaos-repeat",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> repeat(HttpServletRequest request) {
    try {
      JsonNode outcome = submission.submit(request.getInputStream());
      if (!outcome.path("failure").isNull()) {
        var problem =
            ChaosRepeatResponse.problem(
                500,
                "REPEAT_EXECUTION_FAILED",
                "Chaos repeat stopped after an internal failure. The verified completed portion is"
                    + " included.");
        problem.setProperty("partialOutcome", outcome);
        return ResponseEntity.status(500)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
      }
      return ResponseEntity.ok(outcome);
    } catch (RepeatRejected rejected) {
      return ResponseEntity.status(rejected.status())
          .header("Retry-After", "1")
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ChaosRepeatResponse.problem(
                  rejected.status(), rejected.code(), rejected.getMessage()));
    } catch (IOException malformed) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ChaosRepeatResponse.problem(
                  400, "MALFORMED_REQUEST", "Provide one valid UTF-8 JSON request."));
    } catch (IllegalArgumentException invalid) {
      var rejected = RepeatRejected.input();
      return ResponseEntity.unprocessableEntity()
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(ChaosRepeatResponse.problem(422, rejected.code(), rejected.getMessage()));
    } catch (RuntimeException unavailable) {
      return ResponseEntity.internalServerError()
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ChaosRepeatResponse.problem(
                  500, "REPEAT_EXECUTION_FAILED", "Chaos repeat could not be completed."));
    }
  }

  @PostMapping("/api/v1/crafting/workbench/chaos-repeat/{id}/cancel")
  public ResponseEntity<Void> cancel(@PathVariable String id) {
    return submission.cancel(id)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.badRequest().build();
  }
}
