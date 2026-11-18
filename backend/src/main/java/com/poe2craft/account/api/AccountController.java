package com.poe2craft.account.api;

import com.poe2craft.account.api.dto.Credentials;
import com.poe2craft.account.api.dto.ReadBatch;
import com.poe2craft.account.api.dto.Save;
import com.poe2craft.account.api.dto.Status;
import com.poe2craft.account.application.AccountAuthentication;
import com.poe2craft.account.application.AccountLibrary;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 계정과 보관함의 HTTP 요청을 연결한다. enabled는 인증 기능 설정이며 passwords와 sessions는 로그인 검증과 세션 관리를 맡는다. me는 현재 계정,
 * login과 logout은 세션 시작과 종료를 처리한다. library에 list·read·readBatch·save·clear 요청을 전달해 문서 조회·저장·세션 문서
 * 정리를 처리한다.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {
  private final AccountLibrary library;
  private final AccountAuthentication passwords;
  private final boolean enabled;
  private final AccountSession sessions;

  public AccountController(
      AccountLibrary library,
      AccountAuthentication passwords,
      @Value("${account.auth.enabled}") boolean enabled) {
    this.library = library;
    this.passwords = passwords;
    this.enabled = enabled;
    this.sessions = new AccountSession(enabled);
  }

  @PostMapping("/library/read-batch")
  public List<AccountLibrary.Document> readBatch(
      @RequestBody ReadBatch body, HttpServletRequest request) {
    UUID account = sessions.owner(request);
    if (body.generation() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_BATCH");
    return library.readBatch(account, body.generation(), body.references());
  }

  @GetMapping("/me")
  public Status me(HttpServletRequest request) {
    return sessions.current(request);
  }

  @PostMapping({"/login", "/register"})
  public Status login(@RequestBody Credentials body, HttpServletRequest request) {
    if (!enabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    var account =
        passwords.authenticate(
            body.username(),
            body.password(),
            org.springframework.web.util.UrlPathHelper.defaultInstance
                .getLookupPathForRequest(request)
                .endsWith("/register"));
    return sessions.start(request, account);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request) {
    var s = request.getSession(false);
    if (s != null) s.invalidate();
  }

  @GetMapping("/library")
  public org.springframework.http.ResponseEntity<List<AccountLibrary.Reference>> list(
      HttpServletRequest request) {
    var snapshot = library.snapshot(sessions.owner(request));
    return org.springframework.http.ResponseEntity.ok()
        .header("X-Session-Generation", Long.toString(snapshot.generation()))
        .header("X-Library-Read-Batch-Size", Integer.toString(library.readBatchSize()))
        .body(snapshot.documents());
  }

  @GetMapping("/library/{kind}/{id}")
  public AccountLibrary.Document read(
      @PathVariable String kind, @PathVariable String id, HttpServletRequest request) {
    return library.read(sessions.owner(request), kind, id);
  }

  @PostMapping("/library/sessions/clear")
  public Map<String, Long> clear(HttpServletRequest request) {
    return Map.of("generation", library.clearSessions(sessions.owner(request)));
  }

  @PutMapping("/library/{kind}/{id}")
  public AccountLibrary.Document save(
      @PathVariable String kind,
      @PathVariable String id,
      @RequestBody Save body,
      HttpServletRequest request) {
    UUID account = sessions.owner(request);
    if (kind.equals("session")) {
      String raw = request.getHeader("X-Session-Generation");
      if (raw == null || !raw.matches("[0-9]{1,18}"))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSION_GENERATION_REQUIRED");
      return library.saveSession(account, id, body.version(), Long.parseLong(raw), body.content());
    }
    return library.save(account, kind, id, body.version(), body.content());
  }
}
