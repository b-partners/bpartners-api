package app.bpartners.api.service.subscription;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.InvoiceUpcomingParams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
      String stripeCustomerIdentifier,
      String stripeSubscriptionIdentifier,
      InvoiceListParams.Status invoiceStatus) {
    var paramsBuilder =
        InvoiceListParams.builder().setCustomer(stripeCustomerIdentifier).setStatus(invoiceStatus);
    if (stripeSubscriptionIdentifier != null) {
      paramsBuilder.setSubscription(stripeSubscriptionIdentifier);
    }

    return Invoice.list(paramsBuilder.build()).getData();
  }

  public List<Invoice> getUnpaidStripeInvoices(String stripeCustomerIdentifier) {
    return getUnpaidStripeInvoices(stripeCustomerIdentifier, null);
  }

  public List<Invoice> getUnpaidStripeInvoices(
      String stripeCustomerIdentifier, String stripeSubscriptionIdentifier) {
    var unpaidInvoices =
        getStripeInvoices(
            stripeCustomerIdentifier, stripeSubscriptionIdentifier, InvoiceListParams.Status.OPEN);
    var uncollectibleInvoice =
        getStripeInvoices(
            stripeCustomerIdentifier,
            stripeSubscriptionIdentifier,
            InvoiceListParams.Status.UNCOLLECTIBLE);
    return Stream.concat(unpaidInvoices.stream(), uncollectibleInvoice.stream()).toList();
  }

  @SneakyThrows
  public List<Invoice> getPaidStripeInvoicesSince(
      String stripeCustomerIdentifier, Instant paidOrCreatedSince) {
    var params =
        InvoiceListParams.builder()
            .setCustomer(stripeCustomerIdentifier)
            .setStatus(InvoiceListParams.Status.PAID)
            .setCreated(
                InvoiceListParams.Created.builder()
                    .setGte(paidOrCreatedSince.getEpochSecond())
                    .build())
            .setLimit(100L)
            .build();
    var paidInvoices = new ArrayList<Invoice>();
    for (Invoice invoice : Invoice.list(params).autoPagingIterable()) {
      paidInvoices.add(invoice);
    }
    paidInvoices.sort(Comparator.comparing(Invoice::getCreated));
    return paidInvoices;
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
