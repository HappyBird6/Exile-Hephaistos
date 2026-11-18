package com.poe2craft.craftingsupport.search.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 검색 종류별 익명 브라우저 쿠키를 관리한다. cookieName, path, maxAgeSeconds는 쿠키 이름·범위·유효시간이며 owner는 기존 식별자를 읽거나 새
 * 쿠키를 발급한다.
 */
enum SearchClientCookie {
  PATH("crafting_path_client", "/api/v1/crafting/path-searches", 3600),
  METHOD("crafting_method_client", "/api/v2/crafting/method-searches", 3600),
  AUTOMATIC("crafting_method_client", "/api/v2/crafting/method-searches", 86400);
  private final String cookieName;
  private final String path;
  private final int maxAgeSeconds;

  SearchClientCookie(String cookieName, String path, int maxAgeSeconds) {
    this.cookieName = cookieName;
    this.path = path;
    this.maxAgeSeconds = maxAgeSeconds;
  }

  String owner(HttpServletRequest request, HttpServletResponse response) {
    if (request.getCookies() != null)
      for (var cookie : request.getCookies())
        if (cookie.getName().equals(cookieName) && cookie.getValue().matches("[0-9a-f-]{36}"))
          return cookie.getValue();
    var id = UUID.randomUUID().toString();
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(cookieName, id)
            .httpOnly(true)
            .sameSite("Strict")
            .secure(request.isSecure())
            .path(path)
            .maxAge(maxAgeSeconds)
            .build()
            .toString());
    return id;
  }
}
