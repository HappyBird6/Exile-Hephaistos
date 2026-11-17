package com.poe2craft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 서버를 시작하고 기능별 설정과 HTTP API를 등록한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code main(args)}: 애플리케이션 또는 독립 실행 도구를 시작한다. 입력: {@code args}(명령인자).
 * </ul>
 */
@SpringBootApplication
public class Poe2CraftApplication {
  public static void main(String[] args) {
    SpringApplication.run(Poe2CraftApplication.class, args);
  }
}
