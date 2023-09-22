package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.service.CustomerCrupdatedService;
import app.bpartners.api.service.FeedbackRequestedService;
import app.bpartners.api.service.InvoiceCrupdatedService;
import app.bpartners.api.service.InvoiceRelaunchSavedService;
import app.bpartners.api.service.ProspectEvaluationJobInitiatedService;
import app.bpartners.api.service.UserOnboardedService;
import app.bpartners.api.service.UserUpsertedService;
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

  @BeforeEach
  void setUp() {
    invoiceRelaunchSavedService = mock(InvoiceRelaunchSavedService.class);
    invoiceCrupdatedService = mock(InvoiceCrupdatedService.class);
    userUpsertedService = mock(UserUpsertedService.class);
    feedbackRequestedService = mock(FeedbackRequestedService.class);
    prospectJobInitiatedServiceMock = mock(ProspectEvaluationJobInitiatedService.class);
    subject = new EventServiceInvoker(
        invoiceRelaunchSavedService, invoiceCrupdatedService, userUpsertedService,
        feedbackRequestedService, customerCrupdatedService, userOnboardedService,
        prospectJobInitiatedServiceMock);
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