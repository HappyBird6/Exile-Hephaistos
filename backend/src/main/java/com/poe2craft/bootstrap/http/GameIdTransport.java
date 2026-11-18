package com.poe2craft.bootstrap.http;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.ids.*;
import java.io.*;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

/**
 * 협상한 제작 API의 식별자만 압축한다. 내부 도메인·저장 JSON·서명된 반복 요청 bytes는 유지한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BODY_HEADER}: 헤더; 초기값 {@code "X-Crafting-Ids-Body"} ({@code String}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code strict}: 중복 키·후행 토큰을 거절하는 입력 JSON 변환기 ({@code ObjectMapper}).
 *   <li>{@code mappings}: 매핑목록 ({@code GameIdMappings}).
 *   <li>{@code codec}: 저장·전송 표현의 변환기 ({@code GameIdCodec}).
 *   <li>{@code maximumBodyBytes}: 압축 전후 요청 본문의 허용 바이트 수 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GameIdTransport(json, settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code json}(JSON),
 *       {@code settings}(운영설정).
 *   <li>{@code path(path)}: 경로 처리에서 {@code path.startsWith}, {@code path.contains}을(를) 사용한다. 입력:
 *       {@code path}(경로). 반환: {@code boolean}.
 *   <li>{@code supports(parameter, type, converter)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code
 *       parameter}(parameter), {@code type}(타입), {@code converter}(변환기). 반환: {@code boolean}.
 *   <li>{@code beforeBodyRead(input, parameter, type, converter)}: 컨트롤러가 읽기 전에 협상된 짧은 ID 본문을 복원한다.
 *       입력: {@code input}(입력), {@code parameter}(parameter), {@code type}(타입), {@code
 *       converter}(변환기). 반환: {@code HttpInputMessage}. 사용하는 예외: {@code
 *       HttpMessageNotReadableException}, {@code IOException}.
 *   <li>{@code supports(parameter, converter)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code
 *       parameter}(parameter), {@code converter}(변환기). 반환: {@code boolean}.
 *   <li>{@code beforeBodyWrite(body, parameter, type, converter, request, response)}: 응답 본문과 헤더에 전송
 *       계약을 적용한다. 입력: {@code body}(본문), {@code parameter}(parameter), {@code type}(타입), {@code
 *       converter}(변환기), {@code request}(요청), {@code response}(응답). 반환: {@code Object}. 사용하는 예외:
 *       {@code IllegalStateException}.
 * </ul>
 */
@RestControllerAdvice
@Order(100)
public final class GameIdTransport extends RequestBodyAdviceAdapter
    implements ResponseBodyAdvice<Object> {
  public static final String BODY_HEADER = "X-Crafting-Ids-Body";
  private final ObjectMapper json;
  private final ObjectMapper strict;
  private final GameIdMappings mappings;
  private final GameIdCodec codec;
  private final int maximumBodyBytes;

  public GameIdTransport(ObjectMapper json, BackendSettings settings) {
    this.json = json;
    strict =
        json.copy()
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    mappings = GameIdMappings.active();
    codec = new GameIdCodec(mappings);
    maximumBodyBytes = settings.requestProtection().maxBodyBytes();
  }

  private static boolean path(String path) {
    return (path.startsWith("/api/v1/crafting/") || path.startsWith("/api/v2/crafting/"))
        && !path.contains("/chaos-repeat");
  }

  @Override
  public boolean supports(
      MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
    return true;
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage input,
      MethodParameter parameter,
      Type type,
      Class<? extends HttpMessageConverter<?>> converter)
      throws IOException {
    var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    if (!path(attributes.getRequest().getRequestURI())
        || !"compact".equals(input.getHeaders().getFirst(BODY_HEADER))) return input;
    if (!mappings.identity().equals(input.getHeaders().getFirst(GameIdMappings.HEADER)))
      throw new HttpMessageNotReadableException(
          "Identifier mapping differs; refresh the catalog", input);
    var bytes = input.getBody().readNBytes(maximumBodyBytes + 1);
    if (bytes.length > maximumBodyBytes)
      throw new HttpMessageNotReadableException("Request too large", input);
    byte[] decoded;
    try {
      decoded = json.writeValueAsBytes(codec.decode(strict.readTree(bytes)));
    } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException error) {
      throw new HttpMessageNotReadableException("Invalid identifier document", error, input);
    }
    if (decoded.length > maximumBodyBytes)
      throw new HttpMessageNotReadableException("Expanded request too large", input);
    var headers = new HttpHeaders();
    headers.putAll(input.getHeaders());
    headers.setContentLength(decoded.length);
    return new HttpInputMessage() {
      public InputStream getBody() {
        return new ByteArrayInputStream(decoded);
      }

      public HttpHeaders getHeaders() {
        return headers;
      }
    };
  }

  @Override
  public boolean supports(
      MethodParameter parameter, Class<? extends HttpMessageConverter<?>> converter) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter parameter,
      MediaType type,
      Class<? extends HttpMessageConverter<?>> converter,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (!path(request.getURI().getPath())) return body;
    var vary = new java.util.ArrayList<>(response.getHeaders().getVary());
    if (!vary.contains(GameIdMappings.HEADER)) vary.add(GameIdMappings.HEADER);
    response.getHeaders().setVary(vary);
    if (body == null
        || !mappings.identity().equals(request.getHeaders().getFirst(GameIdMappings.HEADER))
        || !(MediaType.APPLICATION_JSON.isCompatibleWith(type)
            || type.getSubtype().endsWith("+json"))) return body;
    var encoded = codec.encode(json.valueToTree(body));
    String representation = mappings.identity();
    if (request.getURI().getPath().equals("/api/v1/crafting/workbench/apply")
        && encoded instanceof com.fasterxml.jackson.databind.node.ObjectNode result
        && result.path("snapshotId").isTextual()
        && result.path("snapshotId").equals(result.path("state").path("snapshotId"))) {
      result.remove("snapshotId");
      representation += ".s";
    }
    response.getHeaders().set(GameIdMappings.HEADER, representation);
    response.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
    if (StringHttpMessageConverter.class.isAssignableFrom(converter)) {
      try {
        return json.writeValueAsString(encoded);
      } catch (IOException error) {
        throw new IllegalStateException("Cannot encode identifier response", error);
      }
    }
    return encoded;
  }
}
