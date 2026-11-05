package com.poe2craft.bootstrap;

import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Test JVM bootstrap mirrors the application's explicit immutable metadata initialization. */
public final class WorkbenchDefinitionsExtension implements BeforeAllCallback {
  @Override
  public void beforeAll(ExtensionContext context) {
    WorkbenchDefinitionsLoader.initialize();
  }
}
