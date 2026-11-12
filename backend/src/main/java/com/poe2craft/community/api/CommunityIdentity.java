package com.poe2craft.community.api;

import com.poe2craft.community.application.SessionBoard.Actor;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
