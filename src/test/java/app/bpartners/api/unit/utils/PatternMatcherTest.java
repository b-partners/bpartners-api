package app.bpartners.api.unit.utils;

import org.junit.jupiter.api.Test;

import static app.bpartners.api.repository.mapper.FintectureMapper.CUSTOM_COMMUNICATION_PATTERN;
import static app.bpartners.api.service.utils.PatternMatcher.filterCharacters;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternMatcherTest {

  @Test
  void filter_fintecture_communication_ok() {
    assertEquals("Test  dépannage.,;:/!?$%_=+()*°@#ÂÇÉÈÊËÎÔÙÛ",
        filterCharacters("Test & dépannage.,;:/!?$%_=+()*°@#ÂÇÉÈÊËÎÔÙÛ",
            CUSTOM_COMMUNICATION_PATTERN));
    assertEquals("BON DINTERVENTION",
        filterCharacters("BON D'INTERVENTION",
            CUSTOM_COMMUNICATION_PATTERN));
    assertEquals("PlomberieTest",
        filterCharacters("[Plomberie-Test]",
            CUSTOM_COMMUNICATION_PATTERN));
    assertEquals("Test $",
        filterCharacters("Test $",
            CUSTOM_COMMUNICATION_PATTERN));
  }
}
