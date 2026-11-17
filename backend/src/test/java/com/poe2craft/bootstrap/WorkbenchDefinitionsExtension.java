package com.poe2craft.bootstrap;

import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * 작업대·정의목록·Extension 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Test JVM bootstrap mirrors the application's explicit immutable metadata
 * initialization.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code beforeAll(context)}: 변경전·All 처리에서 {@code WorkbenchDefinitionsLoader.initialize}을(를)
 *       사용한다. 입력: {@code context}(문맥).
 * </ul>
 */
public final class WorkbenchDefinitionsExtension implements BeforeAllCallback {
  @Override
  public void beforeAll(ExtensionContext context) {
    WorkbenchDefinitionsLoader.initialize();
  }
}
