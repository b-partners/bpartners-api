package app.bpartners.api.service.subscription;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.InvoiceUpcomingParams;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StripeInvoiceService {
  @SneakyThrows
  private List<Invoice> getStripeInvoices(
      String stripeCustomerIdentifier, InvoiceListParams.Status invoiceStatus) {
    InvoiceListParams params =
        InvoiceListParams.builder()
            .setCustomer(stripeCustomerIdentifier)
            .setStatus(invoiceStatus)
            .build();

    return Invoice.list(params).getData();
  }

  public List<Invoice> getUnpaidStripeInvoices(String stripeCustomerIdentifier) {
    var unpaidInvoices = getStripeInvoices(stripeCustomerIdentifier, InvoiceListParams.Status.OPEN);
    var uncollectibleInvoice =
        getStripeInvoices(stripeCustomerIdentifier, InvoiceListParams.Status.UNCOLLECTIBLE);
    return Stream.concat(unpaidInvoices.stream(), uncollectibleInvoice.stream()).toList();
  }

  @SneakyThrows
  public Invoice getUpcomingStripeInvoice(String stripeCustomerIdentifier) {
    try {
      InvoiceUpcomingParams params =
          InvoiceUpcomingParams.builder().setCustomer(stripeCustomerIdentifier).build();

      return Invoice.upcoming(params);
    } catch (StripeException e) {
      log.info(
          "Unable to get upcoming invoice for identifier {} for cause {}",
          stripeCustomerIdentifier,
          e.getMessage());
      return null;
    }
  }
}
