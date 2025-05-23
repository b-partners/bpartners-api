package app.bpartners.api.service.event;

import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static com.stripe.param.InvoiceCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY;

import app.bpartners.api.endpoint.event.model.MonthlyCancelledClientsPayment;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceItem;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceItemCreateParams;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyCancelledClientsPaymentService
    implements Consumer<MonthlyCancelledClientsPayment> {
  private final UserSubscriptionSessionRepository userSubscriptionSessionRepository;
  private final TemporalUtils temporalUtils;
  private final UserRepository userRepository;
  private final SubscriptionProductRepository productRepository;

  @Override
  public void accept(MonthlyCancelledClientsPayment event) {
    var userCancelled =
        userSubscriptionSessionRepository.findAll().stream()
            .filter(UserSubscriptionSession::isCancelled)
            .toList();
    var userCancelledThisMonth =
        userCancelled.stream()
            .filter(
                session ->
                    session.getTrialUntil() != null
                        && session.getTrialUntil().isAfter(temporalUtils.startOfActualMonth()))
            .toList();
    var users =
        userCancelledThisMonth.stream()
            .map(
                userSubscriptionSession ->
                    userRepository.getById(userSubscriptionSession.getUserId()))
            .toList();
    for (User user : users) {
      var essentialProductPriceTTC = 5880;
      SubscriptionProduct essentialProductTTC =
          productRepository.findByPriceInCents(essentialProductPriceTTC);
      try {
        var invoiceCreateParams =
            InvoiceCreateParams.builder()
                .setCustomer(user.getUserSubscriptionId())
                .setCollectionMethod(CHARGE_AUTOMATICALLY)
                .build();
        var invoice = Invoice.create(invoiceCreateParams);
        var invoiceItemParams =
            InvoiceItemCreateParams.builder()
                .setCustomer(user.getUserSubscriptionId())
                .setPriceData(
                    InvoiceItemCreateParams.PriceData.builder()
                        .setCurrency(defaultCurrency())
                        .setProduct(essentialProductTTC.getE2Id())
                        .setUnitAmount(essentialProductTTC.getPriceInCents())
                        .build())
                .setQuantity(1L)
                .setInvoice(invoice.getId())
                .setCustomer(user.getUserSubscriptionId())
                .build();

        var invoiceItem = InvoiceItem.create(invoiceItemParams);
        log.info("invoice item created: {}", invoiceItem);
        invoice.finalizeInvoice();
        invoice.pay();
        log.info("invoice created: {}", invoice);
      } catch (StripeException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
