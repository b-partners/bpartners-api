package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class PaymentInitiationRepositoryImpl implements PaymentInitiationRepository {
  private final PaymentRequestMapper paymentRequestMapper;

  @Override
  public List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoice) {
    return paymentInitiations.stream()
        .map(payment -> paymentRequestMapper.toEntity(payment, invoice))
        .collect(Collectors.toList());
  }
}
