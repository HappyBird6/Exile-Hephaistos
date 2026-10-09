package com.poe2craft.bootstrap;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Test JVM bootstrap mirrors the application's explicit immutable metadata initialization. */
public final class WorkbenchDefinitionsExtension implements BeforeAllCallback {
  @Override
  public void beforeAll(ExtensionContext context) {
    com.poe2craft.crafting.infrastructure.WorkbenchDefinitionsLoader.initialize();
  }
}
