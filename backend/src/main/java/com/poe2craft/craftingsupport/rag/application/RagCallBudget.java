package com.poe2craft.craftingsupport.rag.application;

/** Shared admission before a paid provider request. Denials and storage failures fail closed. */
public interface RagCallBudget {
  void acquire();
}
