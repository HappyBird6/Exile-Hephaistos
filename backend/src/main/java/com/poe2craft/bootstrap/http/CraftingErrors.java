package com.poe2craft.bootstrap.http;

import com.poe2craft.craftingsupport.path.api.BasicPathController;
import com.poe2craft.craftingsupport.path.api.LegacyGraphController;
import com.poe2craft.craftingsupport.recommendation.api.SupportAssessmentController;
import com.poe2craft.craftingsupport.recommendation.api.SupportRecommendationController;
import com.poe2craft.shared.crafting.api.CraftingCatalogController;
import com.poe2craft.workbench.api.CustomExaltController;
import com.poe2craft.workbench.api.WorkbenchController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 제작 입력·상태 오류를 내부 정보가 없는 Problem Details 응답으로 변환한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code unsupportedItemProperties()}: 미지원·아이템·속성 값을 조회·구성한다. 반환: {@code ProblemDetail}.
 *   <li>{@code invalid()}: 잘못된 입력을 나타내는 오류 결과를 만든다. 반환: {@code ProblemDetail}.
 *   <li>{@code malformed()}: 잘못된형식 값을 조회·구성한다. 반환: {@code ProblemDetail}.
 *   <li>{@code problem(status, code, detail)}: 오류상세 처리에서 {@code ProblemDetail.forStatusAndDetail},
 *       {@code result.setTitle}을(를) 사용한다. 입력: {@code status}(상태), {@code code}(코드), {@code
 *       detail}(상세). 반환: {@code ProblemDetail}.
 * </ul>
 */
@RestControllerAdvice(
    assignableTypes = {
      LegacyGraphController.class,
      CraftingCatalogController.class,
      BasicPathController.class,
      WorkbenchController.class,
      CustomExaltController.class,
      SupportAssessmentController.class,
      SupportRecommendationController.class
    })
public final class CraftingErrors {
  @ExceptionHandler(
      com.poe2craft.workbench.application.WorkbenchStateReader.UnsupportedItemProperties.class)
  public ProblemDetail unsupportedItemProperties() {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "UNSUPPORTED_ITEM_PROPERTIES",
        "This item contains unsupported properties. Its state has not been changed.");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail invalid() {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "INVALID_CRAFTING_REQUEST",
        "The crafting request contains an invalid item state or unsupported parameters.");
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ProblemDetail malformed() {
    return problem(
        HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The crafting request could not be read.");
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    var result = ProblemDetail.forStatusAndDetail(status, detail);
    result.setTitle(status.getReasonPhrase());
    result.setProperty("code", code);
    return result;
  }
}
