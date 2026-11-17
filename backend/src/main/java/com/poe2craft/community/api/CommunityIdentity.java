package com.poe2craft.community.api;

import com.poe2craft.community.application.SessionBoard.Actor;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 공개 게시 작업에서 사용할 사용자 신원을 요청 문맥에서 결정한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code actor(request, response, mutation)}: 작업주체 처리에서 {@code request.getCookies}, {@code
 *       c.getName}을(를) 사용한다. 입력: {@code request}(요청), {@code response}(응답), {@code mutation}(변경).
 *       반환: {@code Actor}. 사용하는 예외: {@code ResponseStatusException}.
 * </ul>
 */
@Component
public class CommunityIdentity {
  public Actor actor(HttpServletRequest request, HttpServletResponse response, boolean mutation) {
    UUID browser = null;
    if (request.getCookies() != null)
      for (var c : request.getCookies())
        if (c.getName().equals("hephaistos_community"))
          try {
            browser = UUID.fromString(c.getValue());
          } catch (IllegalArgumentException ignored) {
          }
    if (browser == null) {
      browser = UUID.randomUUID();
      Cookie c = new Cookie("hephaistos_community", browser.toString());
      c.setPath("/");
      c.setHttpOnly(true);
      c.setSecure(request.isSecure());
      c.setMaxAge(31536000);
      c.setAttribute("SameSite", "Lax");
      response.addCookie(c);
    }
    var session = request.getSession(false);
    String raw = session == null ? null : (String) session.getAttribute("account.id");
    UUID account = raw == null ? null : UUID.fromString(raw);
    if (mutation && !java.util.Objects.equals(raw, request.getHeader("X-Account-Id")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ACCOUNT_CHANGED");
    return new Actor(account, browser);
  }
}
