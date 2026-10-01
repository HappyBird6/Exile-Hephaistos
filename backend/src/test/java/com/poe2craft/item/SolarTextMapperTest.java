package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.item.testparser.ItemTextService;
import org.junit.jupiter.api.Test;

class SolarTextMapperTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final SolarTextMapper mapper = new SolarTextMapper(catalog);
  private final ItemTextService parser = new ItemTextService();

  private String text(String explicit) {
    return "Item Class: Amulets\nRarity: Rare\nTest Item\nSolar Amulet\n--------\nItem Level: 82\n--------\n+15 to Spirit (implicit)\n--------\n"
        + explicit;
  }

  @Test
  void exactValuesMapToAValidatedStateWithoutKeepingTheTextInTheState() {
    var parsed = parser.parseText(text("+17 to maximum Life"));
    var result = mapper.map(parsed);
    assertThat(result.mapped()).isTrue();
    assertThat(result.issues()).isEmpty();
    assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
    assertThat(result.state().explicits()).hasSize(1);
    assertThat(parsed.text().originalText()).isEqualTo(text("+17 to maximum Life"));
  }

  @Test
  void unknownAndSpecialLinesBlockMappingAndArePreserved() {
    String text = text("+17 to maximum Life\nGrants Skill: Unknown crafting effect\nCorrupted");
    var parsed = parser.parseText(text);
    var result = mapper.map(parsed);
    assertThat(result.mapped()).isFalse();
    assertThat(result.state()).isNull();
    assertThat(result.issues()).anyMatch(i -> i.message().contains("Unparsed"));
    assertThat(result.issues()).anyMatch(i -> i.message().contains("Special"));
    assertThat(parsed.text().originalText()).isEqualTo(text);
  }

  @Test
  void scaledDisplayValuesAreRefusedUntilRoundingAndQuantizationAreVerified() {
    String text = text("20 Life Regeneration per second");
    var result = mapper.map(parser.parseText(text));
    assertThat(result.mapped()).isFalse();
    assertThat(result.state()).isNull();
    assertThat(parser.parseText(text).text().originalText()).isEqualTo(text);
  }

  @Test
  void outOfRangeAndUnmodeledPropertiesAreNotSilentlyDropped() {
    assertThat(mapper.map(parser.parseText(text("+999999 to maximum Life"))).mapped()).isFalse();
    assertThat(
            mapper
                .map(
                    parser.parseText(
                        text("+17 to maximum Life")
                            .replace("Item Level: 82", "Quality: +20%\nItem Level: 82")))
                .mapped())
        .isFalse();
  }
}
