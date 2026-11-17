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

/**
 * 로그인·로그아웃·현재 사용자 및 개인 보관함 요청을 인증 서비스에 전달한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code library}: 보관함 ({@code AccountLibrary}).
 *   <li>{@code passwords}: passwords ({@code AccountAuthentication}).
 *   <li>{@code enabled}: 사용여부 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AccountController(library, passwords, enabled)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       library}(보관함), {@code passwords}(passwords), {@code enabled}(사용여부).
 *   <li>{@code readBatch(body, request)}: 일괄실행을(를) 읽는다. 입력: {@code body}(본문), {@code request}(요청).
 *       반환: {@code List<AccountLibrary.Document>}. 사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code token(session)}: 인증토큰 처리에서 {@code session.getAttribute}, {@code UUID.randomUUID}을(를)
 *       사용한다. 입력: {@code session}(세션). 반환: {@code String}.
 *   <li>{@code me(request)}: 현재 인증된 사용자를 조회한다. 입력: {@code request}(요청). 반환: {@code Status}.
 *   <li>{@code login(body, request)}: 로그인을 처리하고 인증된 세션을 만든다. 입력: {@code body}(본문), {@code
 *       request}(요청). 반환: {@code Status}. 사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code logout(request)}: 현재 인증 세션을 종료한다. 입력: {@code request}(요청).
 *   <li>{@code owner(request)}: 요청 또는 작업의 소유자를 판정한다. 입력: {@code request}(요청). 반환: {@code UUID}.
 *       사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code list(request)}: 조건에 맞는 목록을 조회한다. 입력: {@code request}(요청). 반환: {@code
 *       org.springframework.http.ResponseEntity<List<AccountLibrary.Reference>>}.
 *   <li>{@code read(kind, id, request)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code kind}(종류), {@code
 *       id}(식별자), {@code request}(요청). 반환: {@code AccountLibrary.Document}.
 *   <li>{@code clear(request)}: 보유한 항목을 비운다. 입력: {@code request}(요청). 반환: {@code Map<String,
 *       Long>}.
 *   <li>{@code save(kind, id, body, request)}: 현재 값을 저장 경계에 기록한다. 입력: {@code kind}(종류), {@code
 *       id}(식별자), {@code body}(본문), {@code request}(요청). 반환: {@code AccountLibrary.Document}. 사용하는
 *       예외: {@code ResponseStatusException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Status}: {@code AccountController}에서 사용하는 상태의 불변 값이다.
 *   <li>{@code Credentials}: {@code AccountController}에서 사용하는 Credentials의 불변 값이다.
 *   <li>{@code Save}: {@code AccountController}에서 사용하는 Save의 불변 값이다.
 *   <li>{@code ReadBatch}: {@code AccountController}에서 사용하는 읽기·일괄실행의 불변 값이다.
 * </ul>
 */
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

  /**
   * {@code AccountController}에서 사용하는 상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code enabled}: 사용여부 ({@code boolean}).
   *   <li>{@code user}: 사용자 ({@code Account}).
   *   <li>{@code csrf}: csrf ({@code String}).
   * </ul>
   */
  public record Status(boolean enabled, Account user, String csrf) {}

  /**
   * {@code AccountController}에서 사용하는 Credentials의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code username}: 사용자명 ({@code String}).
   *   <li>{@code password}: 비밀번호 ({@code String}).
   * </ul>
   */
  public record Credentials(String username, String password) {}

  /**
   * {@code AccountController}에서 사용하는 Save의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code long}).
   *   <li>{@code content}: 내용 ({@code JsonNode}).
   * </ul>
   */
  public record Save(long version, JsonNode content) {}

  /**
   * {@code AccountController}에서 사용하는 읽기·일괄실행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code Long}).
   *   <li>{@code references}: 참조목록 ({@code List<AccountLibrary.ReadKey>}).
   * </ul>
   */
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
