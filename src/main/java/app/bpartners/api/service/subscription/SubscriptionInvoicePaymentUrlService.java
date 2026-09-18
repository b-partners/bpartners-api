package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.user.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionInvoicePaymentUrlService {
  private final UserService userService;
  private final StripeInvoiceService stripeInvoiceService;

  public Map<String, String> getPaymentUrlByInvoiceId(String userId, List<Invoice> invoices) {
    var unpaidInvoices =
        invoices.stream().filter(invoice -> invoice.paymentStatus() == UNPAID).toList();
    if (unpaidInvoices.isEmpty()) {
      return Map.of();
    }
    var stripeCustomerId = userService.getUserById(userId).getUserSubscriptionId();
    if (stripeCustomerId == null) {
      return Map.of();
    }
    var unpaidStripeInvoices = stripeInvoiceService.getUnpaidStripeInvoices(stripeCustomerId);
    if (unpaidStripeInvoices.isEmpty()) {
      return Map.of();
    }
    var paymentUrlByInvoiceId = new HashMap<String, String>();
    for (var invoice : unpaidInvoices) {
      var paymentUrl = hostedInvoiceUrlOf(invoice, unpaidStripeInvoices);
      if (paymentUrl != null) {
        paymentUrlByInvoiceId.put(invoice.getId(), paymentUrl);
      }
    }
    return paymentUrlByInvoiceId;
  }

  private String hostedInvoiceUrlOf(
      Invoice invoice, List<com.stripe.model.Invoice> unpaidStripeInvoices) {
    var totalInCents =
        invoice.getTotalPriceWithVat() == null
            ? null
            : Long.valueOf(invoice.getTotalPriceWithVat().getCentsRoundUp());
    return unpaidStripeInvoices.stream()
        .filter(stripeInvoice -> Objects.equals(stripeInvoice.getTotal(), totalInCents))
        .findFirst()
        .or(
            () ->
                unpaidStripeInvoices.size() == 1
                    ? Optional.of(unpaidStripeInvoices.getFirst())
                    : Optional.empty())
        .map(com.stripe.model.Invoice::getHostedInvoiceUrl)
        .orElse(null);
  }
}
