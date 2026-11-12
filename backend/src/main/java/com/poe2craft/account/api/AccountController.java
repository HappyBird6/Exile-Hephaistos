package com.poe2craft.account.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.poe2craft.account.application.AccountAuthentication;
import com.poe2craft.account.application.AccountLibrary;
import com.poe2craft.account.application.AccountLibrary.Account;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
public class AccountController {
  private final AccountLibrary library;
  private final AccountAuthentication passwords;
  private final boolean enabled;

  public AccountController(
      AccountLibrary library,
      AccountAuthentication passwords,
      @Value("${account.auth.enabled}") boolean enabled) {
    this.library = library;
    this.passwords = passwords;
    this.enabled = enabled;
  }

  public record Status(boolean enabled, Account user, String csrf) {}

  public record Credentials(String username, String password) {}

  public record Save(long version, JsonNode content) {}

  public record ReadBatch(Long generation, List<AccountLibrary.ReadKey> references) {}

  @PostMapping("/library/read-batch")
  public List<AccountLibrary.Document> readBatch(
      @RequestBody ReadBatch body, HttpServletRequest request) {
    UUID account = owner(request);
    if (body.generation() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_BATCH");
    return library.readBatch(account, body.generation(), body.references());
  }

  private String token(HttpSession session) {
    var token = (String) session.getAttribute("account.csrf");
    if (token == null) {
      token = UUID.randomUUID().toString();
      session.setAttribute("account.csrf", token);
    }
    return token;
  }

  @GetMapping("/me")
  public Status me(HttpServletRequest request) {
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
    var previous = request.getSession(false);
    if (previous != null) previous.invalidate();
    var s = request.getSession(true);
    s.setAttribute("account.id", account.id().toString());
    s.setAttribute("account.name", account.displayName());
    return new Status(true, account, token(s));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request) {
    var s = request.getSession(false);
    if (s != null) s.invalidate();
  }

  private UUID owner(HttpServletRequest request) {
    if (!enabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    var s = request.getSession(false);
    String id = s == null ? null : (String) s.getAttribute("account.id");
    if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (!id.equals(request.getHeader("X-Account-Id")))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "ACCOUNT_CHANGED");
    return UUID.fromString(id);
  }

  @GetMapping("/library")
  public org.springframework.http.ResponseEntity<List<AccountLibrary.Reference>> list(
      HttpServletRequest request) {
    var snapshot = library.snapshot(owner(request));
    return org.springframework.http.ResponseEntity.ok()
        .header("X-Session-Generation", Long.toString(snapshot.generation()))
        .header("X-Library-Read-Batch-Size", Integer.toString(library.readBatchSize()))
        .body(snapshot.documents());
  }

  @GetMapping("/library/{kind}/{id}")
  public AccountLibrary.Document read(
      @PathVariable String kind, @PathVariable String id, HttpServletRequest request) {
    return library.read(owner(request), kind, id);
  }

  @PostMapping("/library/sessions/clear")
  public Map<String, Long> clear(HttpServletRequest request) {
    return Map.of("generation", library.clearSessions(owner(request)));
  }

  @PutMapping("/library/{kind}/{id}")
  public AccountLibrary.Document save(
      @PathVariable String kind,
      @PathVariable String id,
      @RequestBody Save body,
      HttpServletRequest request) {
    UUID account = owner(request);
    if (kind.equals("session")) {
      String raw = request.getHeader("X-Session-Generation");
      if (raw == null || !raw.matches("[0-9]{1,18}"))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSION_GENERATION_REQUIRED");
      return library.saveSession(account, id, body.version(), Long.parseLong(raw), body.content());
    }
    return library.save(account, kind, id, body.version(), body.content());
  }
}
