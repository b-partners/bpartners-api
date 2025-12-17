package app.bpartners.api.service.subscription;

import com.stripe.model.Invoice;
import com.stripe.param.InvoiceListParams;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

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
}
