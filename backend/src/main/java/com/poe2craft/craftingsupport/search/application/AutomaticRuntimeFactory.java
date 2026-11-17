package com.poe2craft.craftingsupport.search.application;

/**
 * 현재 또는 과거 카탈로그에 맞는 탐색 실행 환경 생성 계약이다.
 *
 * <p>기존 계약: Rebuilds live or historical search runtimes without exposing bundled data loaders to
 * jobs.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code create(request)}: 새 작업 또는 값을 생성한다. 입력: {@code request}(요청). 반환: {@code
 *       AutomaticSearchService.Runtime}.
 *   <li>{@code historical(request)}: 과거 정의의 읽기 전용 실행 환경을 조회한다. 입력: {@code request}(요청). 반환: {@code
 *       AutomaticSearchService.Runtime}.
 * </ul>
 */
public interface AutomaticRuntimeFactory {
  AutomaticSearchService.Runtime create(MethodSearchProtocol.Create request);

  AutomaticSearchService.Runtime historical(MethodSearchProtocol.Create request);
}
