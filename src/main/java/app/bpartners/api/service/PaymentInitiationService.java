package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
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
  private final AccountService accountService;

  public List<PaymentRedirection> initiatePayments(
      String accountId, List<PaymentInitiation> paymentInitiations) {
    checkAccountRequiredInfos(accountId);
    return repository.saveAll(paymentInitiations, null);
  }

  public PaymentRedirection initiateInvoicePayment(
      Invoice invoice, Fraction totalPriceWithVat) {
    if (Objects.equals(totalPriceWithVat, new Fraction())) {
      return new PaymentRedirection();
    }
    PaymentInitiation paymentInitiation = mapper.convertFromInvoice(
        String.valueOf(randomUUID()), invoice, totalPriceWithVat, null);
    return repository.saveAll(List.of(paymentInitiation), invoice.getId()).get(0);
  }

  public List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoiceId, InvoiceStatus status) {
    if (status == InvoiceStatus.CONFIRMED || status == InvoiceStatus.PAID) {
      return List.of();
    }
    return repository.retrievePaymentEntities(paymentInitiations, invoiceId);
  }

  public List<HPaymentRequest> retrievePaymentEntitiesWithUrl(
      List<PaymentInitiation> paymentInitiations, String invoiceId) {
    return repository.retrievePaymentEntitiesWithUrl(paymentInitiations, invoiceId);
  }

  private void checkAccountRequiredInfos(String accountId) {
    Account account = accountService.getAccountById(accountId);
    StringBuilder builder = new StringBuilder();
    if (account.getBic() == null) {
      builder.append("Bic is mandatory for initiating payments. ");
    }
    if (account.getIban() == null) {
      builder.append("Iban is mandatory for initiating payments. ");
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }
}
