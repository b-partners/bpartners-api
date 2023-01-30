package app.bpartners.api.service;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.PaymentInitiationRepository;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class PaymentInitiationService {
  private final PaymentInitiationRepository repository;

  public List<PaymentRedirection> initiatePayments(List<PaymentInitiation> paymentReqs) {
    if (paymentReqs.size() > 1) {
      throw new NotImplementedException("Only one payment request is supported.");
    }
    return repository.save(paymentReqs.get(0));
  }

  public PaymentRedirection initiateInvoicePayment(Invoice invoice, Fraction totalPriceWithVat) {
    if (Objects.equals(totalPriceWithVat, new Fraction())) {
      return new PaymentRedirection();
    }
    String customerName = invoice.getCustomer() == null ? null : invoice.getCustomer().getName();
    String customerEmail = invoice.getCustomer() == null ? null : invoice.getCustomer().getEmail();
    PaymentInitiation paymentInitiation = PaymentInitiation.builder()
        .id(String.valueOf(randomUUID()))
        .reference(invoice.getRef())
        .label(invoice.getTitle())
        .amount(totalPriceWithVat)
        //TODO: use customerName and customerEmail when overriding
        .payerName(customerName)
        .payerEmail(customerEmail)
        .successUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .failureUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .build();
    return repository.save(paymentInitiation, invoice.getId()).get(0);
  }

}
