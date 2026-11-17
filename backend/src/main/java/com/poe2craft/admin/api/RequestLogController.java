package com.poe2craft.admin.api;

import com.poe2craft.admin.application.RequestLogRepository;
import com.poe2craft.admin.application.RequestLogService;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 관리자에게 요청 로그의 조회·집계·아카이브 다운로드를 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code logs}: 로그목록 ({@code RequestLogService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RequestLogController(logs)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code logs}(로그목록).
 *   <li>{@code requests(date, kind, ip, visitor, route, status, page, view)}: 요청목록 처리에서 {@code
 *       ip.length}, {@code visitor.length}을(를) 사용한다. 입력: {@code date}(날짜), {@code kind}(종류), {@code
 *       ip}(ip), {@code visitor}(visitor), {@code route}(경로), {@code status}(상태), {@code
 *       page}(페이지), {@code view}(조회표현). 반환: {@code ResponseEntity<?>}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code archives(page)}: archives 처리에서 {@code ResponseEntity.ok}, {@code
 *       CacheControl.noStore}을(를) 사용한다. 입력: {@code page}(페이지). 반환: {@code ResponseEntity<?>}. 사용하는
 *       예외: {@code ResponseStatusException}.
 *   <li>{@code download(id)}: 다운로드 처리에서 {@code logs.download}, {@code ResponseEntity.ok}을(를) 사용한다.
 *       입력: {@code id}(식별자). 반환: {@code ResponseEntity<?>}. 사용하는 예외: {@code
 *       ResponseStatusException}, {@code IOException}.
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class RequestLogController {
  private final RequestLogService logs;

  public RequestLogController(RequestLogService logs) {
    this.logs = logs;
  }

  @GetMapping("/requests")
  public ResponseEntity<?> requests(
      @RequestParam(required = false) String date,
      @RequestParam(defaultValue = "requests") String kind,
      @RequestParam(defaultValue = "") String ip,
      @RequestParam(defaultValue = "") String visitor,
      @RequestParam(defaultValue = "") String route,
      @RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "all") String view) {
    if (!Set.of("all", "dashboard", "logs").contains(view)
        || !Set.of("requests", "errors", "limited").contains(kind)
        || ip.length() > 64
        || visitor.length() > 36
        || route.length() > 256
        || page < 1
        || page > 25000
        || (status != null && (status < 100 || status > 599)))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    try {
      return ResponseEntity.ok()
          .cacheControl(CacheControl.noStore())
          .body(
              logs.query(
                  new RequestLogRepository.Filters(
                      date == null ? logs.today() : LocalDate.parse(date),
                      kind,
                      ip,
                      visitor,
                      route,
                      status,
                      page),
                  view));
    } catch (IllegalArgumentException | java.time.DateTimeException invalid) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/archives")
  public ResponseEntity<?> archives(@RequestParam(defaultValue = "1") int page) {
    if (page < 1 || page > 25000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(logs.archives(page));
  }

  @GetMapping("/archives/{id}")
  public ResponseEntity<?> download(@PathVariable UUID id) throws IOException {
    var path = logs.download(id);
    if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType("application/gzip"))
        .contentLength(Files.size(path))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(path.getFileName().toString())
                .build()
                .toString())
        .body(new FileSystemResource(path));
  }
}
