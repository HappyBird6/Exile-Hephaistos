package com.poe2craft.account.api;

import com.poe2craft.account.api.dto.Status;
import com.poe2craft.account.application.AccountLibrary.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 계정 세션과 CSRF 토큰을 관리한다. enabled는 인증 기능의 활성 상태다. current는 현재 로그인 상태를 읽고 start는 인증 후 세션을 교체한다. owner는
 * 요청한 계정을 확인하고 token은 세션 토큰을 발급하거나 재사용한다.
 */
final class AccountSession {
  private final boolean enabled;

  AccountSession(boolean enabled) {
    this.enabled = enabled;
  }

  UUID owner(HttpServletRequest request) {
    if (!enabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    var s = request.getSession(false);
    String id = s == null ? null : (String) s.getAttribute("account.id");
    if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (!id.equals(request.getHeader("X-Account-Id")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ACCOUNT_CHANGED");
    return UUID.fromString(id);
  }

  private String token(HttpSession session) {
    var token = (String) session.getAttribute("account.csrf");
    if (token == null) {
      token = UUID.randomUUID().toString();
      session.setAttribute("account.csrf", token);
    }
    return token;
  }

  Status current(HttpServletRequest request) {
    if (!enabled) return new Status(false, null, "");
    var s = request.getSession();
    var id = (String) s.getAttribute("account.id");
    return new Status(
        true,
        id == null
            ? null
            : new Account(UUID.fromString(id), (String) s.getAttribute("account.name")),
        token(s));
  }

  Status start(HttpServletRequest request, Account account) {
    var previous = request.getSession(false);
    if (previous != null) previous.invalidate();
    var s = request.getSession(true);
    s.setAttribute("account.id", account.id().toString());
    s.setAttribute("account.name", account.displayName());
    return new Status(true, account, token(s));
  }
}
