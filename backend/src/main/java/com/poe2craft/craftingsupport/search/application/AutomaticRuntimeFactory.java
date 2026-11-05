package com.poe2craft.craftingsupport.search.application;

/** Rebuilds live or historical search runtimes without exposing bundled data loaders to jobs. */
public interface AutomaticRuntimeFactory {
  AutomaticSearchService.Runtime create(MethodSearchProtocol.Create request);

  AutomaticSearchService.Runtime historical(MethodSearchProtocol.Create request);
}
