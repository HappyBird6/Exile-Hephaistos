package com.poe2craft;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.bootstrap.AdminCredentials;
import com.poe2craft.season.api.CrawlModels.Target;
import com.poe2craft.season.application.service.TargetValidation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminValidationTest {
  @Test
  void missingVersionAndEnabledCannotSilentlyBecomeDefaults() {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    assertThatThrownBy(
            () ->
                mapper.readValue(
                    "{\"targets\":[]}", com.poe2craft.season.api.CrawlModels.Update.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    assertThatThrownBy(
            () ->
                mapper.readValue(
                    "{\"version\":null,\"targets\":[]}",
                    com.poe2craft.season.api.CrawlModels.Update.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    assertThatThrownBy(
            () ->
                mapper.readValue(
                    "{\"name\":\"test\",\"url\":\"https://poe2db.tw/us/Currency\"}", Target.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
  }

  @Test
  void optionalCredentialsAreValidatedWithoutExposingTheirValue() {
    assertThat(new AdminCredentials("", "").configured()).isFalse();
    assertThatThrownBy(() -> new AdminCredentials("operator", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AdminCredentials("", "SyntheticStrong!12345"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AdminCredentials("operator", "password"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageNotContaining("password must");
    assertThat(new AdminCredentials("operator", "SyntheticStrong!12345").configured()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://poe2db.tw/us/Currency",
        "https://poe2db.tw:443/us/Currency",
        "https://poe2db.tw.evil/us/Currency",
        "https://u@poe2db.tw/us/Currency",
        "https://poe2db.tw/us/Currency?q=x",
        "https://poe2db.tw/us/Currency#x",
        "https://poe2db.tw/us/%43urrency",
        "https://poe2db.tw/us/../Currency",
        "https://poe2db.tw/us/foo/bar",
        "https://poe2db.tw/jp/Currency"
      })
  void unsafeTargetsAreRejected(String url) {
    assertThatThrownBy(
            () ->
                TargetValidation.validate(
                    List.of(new Target(UUID.randomUUID(), "Currency", url, true))))
        .hasMessage("INVALID_TARGETS");
  }

  @Test
  void targetLimitsAndDuplicatesAreRejected() {
    var target = new Target(UUID.randomUUID(), "Currency", "https://poe2db.tw/kr/Currency", true);
    TargetValidation.validate(List.of(target));
    assertThatThrownBy(() -> TargetValidation.validate(List.of(target, target)))
        .hasMessage("INVALID_TARGETS");
    assertThatThrownBy(() -> TargetValidation.validate(java.util.Collections.nCopies(21, target)))
        .hasMessage("INVALID_TARGETS");
  }
}
