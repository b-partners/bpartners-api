package app.bpartners.api.unit.service.prospect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.prospect.relaunch.ProspectRelaunchEmail;
import app.bpartners.api.service.prospect.relaunch.ProspectRelaunchEmailFactory;
import app.bpartners.api.service.prospect.relaunch.ProspectRelaunchMailer;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProspectRelaunchMailerTest {
  private static final ProspectRelaunchEmail EMAIL =
      new ProspectRelaunchEmail("holder@mail.com", "admin@birdia.fr", "subject", "<html></html>");
  ProspectRelaunchEmailFactory emailFactoryMock = mock(ProspectRelaunchEmailFactory.class);
  SesService sesServiceMock = mock(SesService.class);
  ProspectRelaunchMailer subject = new ProspectRelaunchMailer(emailFactoryMock, sesServiceMock);

  @BeforeEach
  void setUp() {
    when(emailFactoryMock.from(any(), any())).thenReturn(EMAIL);
  }

  @Test
  void sends_the_composed_email() throws MessagingException, IOException {
    subject.send(accountHolder(), prospects());

    verify(emailFactoryMock).from(accountHolder(), prospects());
    verify(sesServiceMock)
        .sendEmail(EMAIL.recipient(), EMAIL.cc(), EMAIL.subject(), EMAIL.htmlBody(), List.of());
  }

  @Test
  void wraps_mail_sending_failure() throws MessagingException, IOException {
    doThrow(new MessagingException("mail is down"))
        .when(sesServiceMock)
        .sendEmail(any(), any(), any(), any(), any());

    var actual = assertThrows(ApiException.class, () -> subject.send(accountHolder(), prospects()));

    assertEquals(ApiException.ExceptionType.SERVER_EXCEPTION, actual.getType());
  }

  private static HAccountHolder accountHolder() {
    return HAccountHolder.builder().id("holder_id").email("holder@mail.com").build();
  }

  private static List<Prospect> prospects() {
    return List.of(Prospect.builder().id("prospect_id").build());
  }
}
