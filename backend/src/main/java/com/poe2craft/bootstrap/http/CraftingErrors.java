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
  @ExceptionHandler(WorkbenchController.UnsupportedItemProperties.class)
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
