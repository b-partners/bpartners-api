package app.bpartners.api.service.payment;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.service.account.AccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentInitiationService {
  private final PaymentInitiationRepository repository;
  private final AccountService accountService;

  public List<PaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoiceId, InvoiceStatus status) {
    if (status == InvoiceStatus.CONFIRMED || status == InvoiceStatus.PAID) {
      return new ArrayList<>();
    }
    return repository.retrievePaymentEntities(paymentInitiations, invoiceId).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
  }
}
