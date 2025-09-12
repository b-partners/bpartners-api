package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentRegulation;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentRegulationRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class PaymentRegulationRepositoryImpl implements PaymentRegulationRepository {
  private final PaymentRequestMapper paymentRequestMapper;

  @Override
  public List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentRegulation> paymentRegulations, String invoice) {
    return paymentRegulations.stream()
        .map(payment -> paymentRequestMapper.toEntity(payment, invoice))
        .collect(Collectors.toList());
  }
}
