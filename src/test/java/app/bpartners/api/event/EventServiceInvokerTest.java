package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.gen.InvoiceRelaunchSaved;
import app.bpartners.api.service.event.CustomerCrupdatedService;
import app.bpartners.api.service.event.FeedbackRequestedService;
import app.bpartners.api.service.event.InvoiceCrupdatedService;
import app.bpartners.api.service.event.InvoiceRelaunchSavedService;
import app.bpartners.api.service.event.ProspectEvaluationJobInitiatedService;
import app.bpartners.api.service.event.UserOnboardedService;
import app.bpartners.api.service.event.UserUpsertedService;
import app.bpartners.api.service.event.ProspectUpdatedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventServiceInvokerTest {
  EventServiceInvoker subject;
  InvoiceRelaunchSavedService invoiceRelaunchSavedService;
  InvoiceCrupdatedService invoiceCrupdatedService;
  UserUpsertedService userUpsertedService;
  FeedbackRequestedService feedbackRequestedService;
  CustomerCrupdatedService customerCrupdatedService;
  UserOnboardedService userOnboardedService;
  ProspectEvaluationJobInitiatedService prospectJobInitiatedServiceMock;
  ProspectUpdatedService prospectUpdatedServiceMock;

  @BeforeEach
  void setUp() {
    invoiceRelaunchSavedService = mock(InvoiceRelaunchSavedService.class);
    invoiceCrupdatedService = mock(InvoiceCrupdatedService.class);
    userUpsertedService = mock(UserUpsertedService.class);
    feedbackRequestedService = mock(FeedbackRequestedService.class);
    prospectJobInitiatedServiceMock = mock(ProspectEvaluationJobInitiatedService.class);
    prospectUpdatedServiceMock = mock(ProspectUpdatedService.class);
    subject = new EventServiceInvoker(
        invoiceRelaunchSavedService,
        invoiceCrupdatedService,
        userUpsertedService,
        feedbackRequestedService,
        customerCrupdatedService,
        userOnboardedService,
        prospectJobInitiatedServiceMock,
        prospectUpdatedServiceMock);
  }

  @Test
  void invoiceRelaunchSaved_invokes_corresponding_service() {
    TypedInvoiceRelaunchSaved emailSent =
        new TypedInvoiceRelaunchSaved(InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build());
    subject.accept(emailSent);

    verify(invoiceRelaunchSavedService, times(1)).accept(
        (InvoiceRelaunchSaved) emailSent.getPayload());
  }

}