package com.poe2craft.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 운영 설정의 로딩과 검증을 Spring 구성에 연결한다.
 *
 * <p>직접 선언한 필드·생성자·메서드는 없다.
 */
@Configuration
@EnableConfigurationProperties(BackendSettings.class)
public class BackendConfiguration {}
