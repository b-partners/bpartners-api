package app.bpartners.api.unit.service.prospect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.prospect.relaunch.ProspectRelaunchEmailFactory;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProspectRelaunchEmailFactoryTest {
  private static final String ADMIN_EMAIL = "admin@birdia.fr";
  private static final String HOLDER_EMAIL = "holder@mail.com";
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();
  SesConf sesConfMock = mock(SesConf.class);
  ProspectRelaunchEmailFactory subject =
      new ProspectRelaunchEmailFactory(
          new TemplateResolverEngine(), customDateFormatter, sesConfMock);

  @Test
  void addresses_the_holder_and_copies_the_admin() {
    when(sesConfMock.getAdminEmail()).thenReturn(ADMIN_EMAIL);

    var actual = subject.from(accountHolder(), List.of(prospect()));

    assertEquals(HOLDER_EMAIL, actual.recipient());
    assertEquals(ADMIN_EMAIL, actual.cc());
  }

  @Test
  void subjects_the_mail_with_birdia_and_today() {
    var actual = subject.from(accountHolder(), List.of(prospect()));

    assertEquals(
        "[BIRDIA] Pensez à modifier le statut de vos prospects pour les conserver - "
            + customDateFormatter.formatFrenchDate(Instant.now()),
        actual.subject());
  }

  @Test
  void renders_the_holder_and_its_prospects() {
    var actual = subject.from(accountHolder(), List.of(prospect()));

    assertTrue(actual.htmlBody().contains("Joe Doe"));
    assertTrue(actual.htmlBody().contains("Boulangerie Dupont"));
    assertTrue(actual.htmlBody().contains("contact@dupont.fr"));
    assertTrue(actual.htmlBody().contains("+33600000000"));
    assertTrue(actual.htmlBody().contains("14 rue Soleillet"));
    assertTrue(actual.htmlBody().contains("Dupont Jean"));
    assertTrue(actual.htmlBody().contains("8,00"));
  }

  @Test
  void renders_the_birdia_signature() {
    var actual = subject.from(accountHolder(), List.of(prospect()));

    assertTrue(actual.htmlBody().contains("L'équipe BIRDIA"));
    assertTrue(actual.htmlBody().contains("06 68 62 48 36"));
    assertTrue(actual.htmlBody().contains("https://www.birdia.fr/"));
    assertTrue(actual.htmlBody().contains("14 rue Soleillet, 75020, Paris"));
  }

  private static HAccountHolder accountHolder() {
    return HAccountHolder.builder().id("holder_id").name("Joe Doe").email(HOLDER_EMAIL).build();
  }

  private static Prospect prospect() {
    return Prospect.builder()
        .id("prospect_id")
        .name("Boulangerie Dupont")
        .email("contact@dupont.fr")
        .phone("+33600000000")
        .address("14 rue Soleillet")
        .managerName("Dupont Jean")
        .rating(Prospect.ProspectRating.builder().value(8.0).build())
        .build();
  }
}
