package app.bpartners.api.service;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
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
  private final PaymentRequestMapper mapper;

  public List<PaymentRedirection> initiatePayments(List<PaymentInitiation> paymentInitiations) {
    return repository.saveAll(paymentInitiations, null);
  }

  public PaymentRedirection initiateInvoicePayment(
      Invoice invoice, Fraction totalPriceWithVat) {
    if (Objects.equals(totalPriceWithVat, new Fraction())) {
      return new PaymentRedirection();
    }
    PaymentInitiation paymentInitiation = PaymentInitiation.builder()
        .id(String.valueOf(randomUUID()))
        .reference(invoice.getRef())
        .label(invoice.getTitle())
        .amount(totalPriceWithVat)
        //TODO: use customerName and customerEmail when overriding
        .payerName(invoice.getCustomer() == null ? null
            : invoice.getCustomer().getName())
        .payerEmail(invoice.getCustomer() == null
            ? null : invoice.getCustomer().getEmail())
        .successUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .failureUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .build();
    return repository.saveAll(List.of(paymentInitiation), invoice.getId()).get(0);
  }

}
