package com.poe2craft.craftingsupport.llmtest.api;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** 카드 조회·전송의 HTTP 오류와 잘못된 요청 본문을 Spring의 표준 Problem Details로 반환한다. */
@RestControllerAdvice(assignableTypes = ExperimentCardController.class)
public class ExperimentCardErrors extends ResponseEntityExceptionHandler {}
