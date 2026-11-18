package com.poe2craft.workbench.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** 반복 제작 실패 응답을 만든다. problem은 HTTP 상태와 오류 코드를 ProblemDetail에 담는다. */
final class ChaosRepeatResponse {
  private ChaosRepeatResponse() {}

  static ProblemDetail problem(int status, String code, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    problem.setTitle(HttpStatus.valueOf(status).getReasonPhrase());
    problem.setProperty("code", code);
    return problem;
  }
}
