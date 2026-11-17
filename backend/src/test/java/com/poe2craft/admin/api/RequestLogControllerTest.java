package com.poe2craft.admin.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.poe2craft.admin.application.RequestLogService;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 관리자에게 요청 로그의 조회·집계·아카이브 다운로드를 제공한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code allLogRoutesRequireAuthenticationAndValidateInput()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class RequestLogControllerTest {
  @TempDir Path directory;

  @Test
  void allLogRoutesRequireAuthenticationAndValidateInput() throws Exception {
    var logs = mock(RequestLogService.class);
    when(logs.today()).thenReturn(LocalDate.parse("2026-11-11"));
    when(logs.query(any())).thenReturn(Map.of("rows", List.of()));
    var mvc =
        MockMvcBuilders.standaloneSetup(new RequestLogController(logs))
            .addFilters(new AdminAccessFilter("secret"))
            .build();
    String auth =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("admin:secret".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    for (String path :
        List.of(
            "/api/admin/requests",
            "/api/admin/archives",
            "/api/admin/archives/" + UUID.randomUUID()))
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
    verifyNoInteractions(logs);
    mvc.perform(get("/api/admin/requests").header("Authorization", auth))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(get("/api/admin/requests?date=invalid").header("Authorization", auth))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/requests?page=0").header("Authorization", auth))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/requests?kind=unknown").header("Authorization", auth))
        .andExpect(status().isBadRequest());
    UUID id = UUID.randomUUID();
    var path = Files.write(directory.resolve("requests.jsonl.gz"), new byte[] {31, -117});
    when(logs.download(id)).thenReturn(path);
    mvc.perform(get("/api/admin/archives/" + id).header("Authorization", auth))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"requests.jsonl.gz\""))
        .andExpect(content().bytes(new byte[] {31, -117}));
  }
}
