package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventConsumer;
import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.event.InvoiceRelaunchSavedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
class EventServiceInvokerTest extends MockedThirdParties {
  EventServiceInvoker subject;
  @MockBean
  InvoiceRelaunchSavedService invoiceRelaunchSavedService;
  @Autowired
  ApplicationContext applicationContext;
  @BeforeEach
  void setUp() {
    subject = new EventServiceInvoker(applicationContext);
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  @Test
  void invoiceRelaunchSaved_invokes_corresponding_service(){
    InvoiceRelaunchSaved emailSent = InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build();

    doNothing().when(invoiceRelaunchSavedService).accept(any(InvoiceRelaunchSaved.class));

    subject.accept(new EventConsumer.TypedEvent("app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved", emailSent));

    verify(invoiceRelaunchSavedService, times(1)).accept(emailSent);
  }

}