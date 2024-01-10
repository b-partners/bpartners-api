package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PaymentRepositoryImpl implements PaymentRequestRepository {
  private final PaymentRequestJpaRepository jpaRepository;
  private final PaymentRequestMapper mapper;

  @Override
  public List<PaymentRequest> saveAll(List<PaymentRequest> payments) {
    List<HPaymentRequest> paymentEntities = payments.stream()
        .map(domain -> {
          HPaymentRequest existing = domain.getId() == null ? null
              : jpaRepository.getById(domain.getId());
          return mapper.toEntity(domain, existing);
        })
        .collect(Collectors.toList());
    return jpaRepository.saveAll(paymentEntities).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
