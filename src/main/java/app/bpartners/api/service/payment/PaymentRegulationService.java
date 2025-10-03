package app.bpartners.api.service.payment;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.PaymentRegulation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.PaymentRegulationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentRegulationService {
  private final PaymentRegulationRepository repository;

  public List<PaymentRequest> retrievePaymentEntities(
      List<PaymentRegulation> paymentRegulations, String invoiceId, InvoiceStatus status) {
    return repository.retrievePaymentEntities(paymentRegulations, invoiceId).stream()
        .map(PaymentRequest::new)
        .collect(Collectors.toList());
  }
}
