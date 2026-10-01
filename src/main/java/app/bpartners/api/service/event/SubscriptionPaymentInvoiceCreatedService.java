package app.bpartners.api.service.event;

import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.SUBSCRIPTION_INVOICE_MAIL_TEMPLATE;

import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.subscription.SubscriptionInvoiceMailer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentInvoiceCreatedService
    implements Consumer<SubscriptionPaymentInvoiceCreated> {
  private final InvoiceRepository invoiceRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final SubscriptionInvoiceMailer subscriptionInvoiceMailer;

  @Override
  public void accept(SubscriptionPaymentInvoiceCreated event) {
    var invoice = invoiceRepository.findById(event.getInvoiceId());
    if (invoice == null) {
      log.warn("No Invoice.id={} to send by mail, skipping", event.getInvoiceId());
      return;
    }
    var subscriptionPayment =
        subscriptionPaymentRepository.findById(event.getSubscriptionPaymentId()).orElse(null);
    subscriptionInvoiceMailer.send(
        invoice,
        subscriptionPayment,
        SUBSCRIPTION_INVOICE_MAIL_TEMPLATE,
        subscriptionInvoiceMailer.subscriberRecipientOf(invoice));
  }
}
