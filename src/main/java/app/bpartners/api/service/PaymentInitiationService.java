package app.bpartners.api.service;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentInitiationService {
  private final PaymentInitiationRepository repository;
  private final PaymentRequestMapper mapper;
  private final AccountService accountService;

  public List<PaymentRedirection> initiatePayments(
      String accountId, List<PaymentInitiation> paymentInitiations) {
    checkAccountRequiredInfos(accountId);
    return repository.saveAll(paymentInitiations, null, null);
  }

  public PaymentRedirection initiateInvoicePayment(Invoice invoice) {
    log.info("Initiate invoice payment processing");
    if (Objects.equals(invoice.getTotalPriceWithVat(), new Fraction())) {
      return new PaymentRedirection();
    }
    CreatePaymentRegulation paymentReg = null;
    PaymentHistoryStatus paymentHistoryStatus = null;
    PaymentInitiation paymentInitiation =
        mapper.convertFromInvoice(
            String.valueOf(randomUUID()),
            invoice.getTitle(),
            invoice.getRealReference(),
            invoice,
            paymentReg,
            paymentHistoryStatus);
    log.info("Payment initiation: {}", paymentInitiation);
    return repository
        .saveAll(List.of(paymentInitiation), invoice.getId(), invoice.getUser())
        .get(0);
  }

  public List<PaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoiceId, InvoiceStatus status) {
    if (status == InvoiceStatus.CONFIRMED || status == InvoiceStatus.PAID) {
      return List.of();
    }
    return repository.retrievePaymentEntities(paymentInitiations, invoiceId).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
  }

  public List<PaymentRequest> retrievePaymentEntitiesWithUrl(
      List<PaymentInitiation> paymentInitiations, String invoiceId, User user) {
    return repository.retrievePaymentEntitiesWithUrl(paymentInitiations, invoiceId, user).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
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
