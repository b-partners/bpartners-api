package app.bpartners.api.service.payment;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.PaymentInitiationRepository;
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

  public List<PaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoiceId, InvoiceStatus status) {
    return repository.retrievePaymentEntities(paymentInitiations, invoiceId).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
  }
}
