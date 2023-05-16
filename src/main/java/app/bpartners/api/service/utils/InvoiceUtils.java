package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class InvoiceUtils {
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final PaymentRequestMapper requestMapper;

  public boolean hasAvailableReference(
      String accountId, String invoiceId, String reference, InvoiceStatus status) {
    if (reference == null) {
      return true;
    }
    List<HInvoice> actual =
        invoiceJpaRepository.findByIdAccountAndRefAndStatus(accountId, reference, status);
    return actual.isEmpty() || actual.get(0).getId().equals(invoiceId);
  }

  public static Fraction computeTotalPriceWithVatAndDiscount(
      Fraction discount, List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getPriceWithVatAndDiscount(discount)));
  }

  public static Fraction computeTotalDiscountAmount(Fraction discount,
                                                    List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getDiscountAmount(discount)));
  }

  public static Fraction computeSum(List<InvoiceProduct> products,
                                    Stream<Fraction> fractionStream) {
    if (products == null) {
      return new Fraction();
    }
    Aprational aprational = fractionStream
        .map(a -> toAprational(a.getNumerator(), a.getDenominator()))
        .reduce(new Aprational(0), Aprational::add);
    return parseFraction(aprational);
  }

  public static Fraction computeTotalVatWithDiscount(Fraction discount,
                                                     List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(product ->
            product.getVatWithDiscount(discount)));
  }

  public static Fraction computePriceNoVatWithDiscount(Fraction discount,
                                                       List<InvoiceProduct> actualProducts) {
    return computeSum(actualProducts, actualProducts.stream()
        .map(product ->
            product.getPriceNoVatWithDiscount(discount)));
  }

  public static Fraction computePriceWithoutDiscount(List<InvoiceProduct> products) {
    return computeSum(products, products.stream()
        .map(InvoiceProduct::getPriceWithoutVat));
  }

  public static Instant getCreatedDatetime(Optional<HInvoice> persisted) {
    return persisted.map(HInvoice::getCreatedDatetime).orElse(Instant.now());
  }

  public static List<HPaymentRequest> getPaymentRequests(List<HPaymentRequest> payments,
                                                         Invoice domain) {
    return payments.stream()
        .map(payment -> payment.toBuilder()
            .payerName(domain.getCustomer().getName())
            .payerEmail(domain.getCustomer() == null ? domain.getCustomerEmail()
                : domain.getCustomer().getEmail())
            .build())
        .collect(Collectors.toUnmodifiableList());
  }

  public List<PaymentInitiation> getPaymentInitiations(
      Invoice domain,
      Fraction totalPriceWithVat) {
    List<PaymentInitiation> payments = domain.getMultiplePayments().stream()
        .map(payment -> {
          String randomId = String.valueOf(randomUUID());
          payment.setEndToEndId(randomId);
          return requestMapper.convertFromInvoice(
              randomId, domain, totalPriceWithVat, payment);
        })
        .sorted(Comparator.comparing(PaymentInitiation::getPaymentDueDate))
        .collect(Collectors.toUnmodifiableList());
    for (int i = 0; i < payments.size(); i++) {
      if (i != payments.size() - 1) {
        payments.get(i).setLabel(domain.getTitle() + " - Acompte N°" + (i + 1));
      } else {
        payments.get(i).setLabel(domain.getTitle() + " - Restant dû");
      }
    }
    return payments;
  }

}
