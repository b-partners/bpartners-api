package app.bpartners.api.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.mail.EmailSubjectPrefixer;
import org.junit.jupiter.api.Test;

class EmailSubjectPrefixerTest {
  private static final String SUBJECT = "Votre facture";

  @Test
  void prod_subject_is_left_as_is() {
    var prodPrefixer = new EmailSubjectPrefixer("prod");

    assertEquals(SUBJECT, prodPrefixer.apply(SUBJECT));
  }

  @Test
  void preprod_subject_is_prefixed() {
    var preprodPrefixer = new EmailSubjectPrefixer("preprod");

    assertEquals("[preprod] " + SUBJECT, preprodPrefixer.apply(SUBJECT));
  }

  @Test
  void other_env_subject_is_prefixed() {
    var testPrefixer = new EmailSubjectPrefixer("test");

    assertEquals("[test] " + SUBJECT, testPrefixer.apply(SUBJECT));
  }

  @Test
  void blank_env_subject_is_left_as_is() {
    var blankEnvPrefixer = new EmailSubjectPrefixer("  ");
    var nullEnvPrefixer = new EmailSubjectPrefixer(null);

    assertEquals(SUBJECT, blankEnvPrefixer.apply(SUBJECT));
    assertEquals(SUBJECT, nullEnvPrefixer.apply(SUBJECT));
  }

  @Test
  void null_subject_is_left_as_is() {
    var preprodPrefixer = new EmailSubjectPrefixer("preprod");

    assertNull(preprodPrefixer.apply(null));
  }
}
