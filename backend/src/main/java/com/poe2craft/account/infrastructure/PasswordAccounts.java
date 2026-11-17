package com.poe2craft.account.infrastructure;

import com.poe2craft.account.application.AccountLibrary.Account;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 비밀번호를 검증하고 세션에 연결할 계정 신원을 만든다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code db}: DB ({@code JdbcTemplate}).
 *   <li>{@code encoder}: encoder ({@code BCryptPasswordEncoder}).
 *   <li>{@code dummy}: dummy ({@code String}).
 *   <li>{@code slots}: 슬롯목록 ({@code Semaphore}).
 *   <li>{@code minimum}: 최소값 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PasswordAccounts(db, cost, concurrency, minimum)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       db}(DB), {@code cost}(비용), {@code concurrency}(동시작업수), {@code minimum}(최소값).
 *   <li>{@code authenticate(username, password, register)}: 제출한 인증 정보를 확인한다. 입력: {@code
 *       username}(사용자명), {@code password}(비밀번호), {@code register}(등록여부). 반환: {@code Account}. 사용하는
 *       예외: {@code ResponseStatusException}.
 * </ul>
 */
@Service
public class PasswordAccounts implements com.poe2craft.account.application.AccountAuthentication {
  private final JdbcTemplate db;
  private final BCryptPasswordEncoder encoder;
  private final String dummy;
  private final Semaphore slots;
  private final int minimum;

  public PasswordAccounts(
      JdbcTemplate db,
      @Value("${account.auth.bcrypt-cost}") int cost,
      @Value("${account.auth.max-concurrent}") int concurrency,
      @Value("${account.auth.minimum-password-length}") int minimum) {
    this.db = db;
    this.encoder = new BCryptPasswordEncoder(cost);
    this.dummy = encoder.encode(UUID.randomUUID().toString());
    this.slots = new Semaphore(concurrency);
    this.minimum = minimum;
  }

  @Transactional
  public Account authenticate(String username, String password, boolean register) {
    if (username == null
        || !username.matches("[A-Za-z0-9_]{3,32}")
        || password == null
        || password.length() < minimum
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIAL_FORMAT");
    username = username.toLowerCase(Locale.ROOT);
    if (!slots.tryAcquire())
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_BUSY");
    try {
      if (register) {
        var account = new Account(UUID.randomUUID(), username);
        String hash = encoder.encode(password);
        // Lock same username registrations together, including across application instances.
        db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", username);
        if (Boolean.TRUE.equals(
            db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM app_user.password_identity WHERE username=?)",
                Boolean.class,
                username)))
          throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_UNAVAILABLE");
        db.update(
            "INSERT INTO app_user.account(id,display_name) VALUES (?,?)",
            account.id(),
            account.displayName());
        db.update(
            "INSERT INTO app_user.password_identity(username,account_id,password_hash) VALUES (?,?,?)",
            username,
            account.id(),
            hash);
        return account;
      }
      var rows =
          db.queryForList(
              "SELECT p.password_hash,a.id,a.display_name FROM app_user.password_identity p JOIN app_user.account a ON a.id=p.account_id WHERE p.username=?",
              username);
      String hash = rows.isEmpty() ? dummy : (String) rows.getFirst().get("password_hash");
      boolean matches = encoder.matches(password, hash);
      if (rows.isEmpty() || !matches)
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
      var row = rows.getFirst();
      return new Account((UUID) row.get("id"), (String) row.get("display_name"));
    } finally {
      slots.release();
    }
  }
}
