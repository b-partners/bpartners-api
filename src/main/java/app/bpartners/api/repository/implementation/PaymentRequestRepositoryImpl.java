package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PaymentRequestRepositoryImpl implements PaymentRequestRepository {
  private final PaymentRequestJpaRepository jpaRepository;
  private final PaymentRequestMapper mapper;

  @Override
  public List<PaymentRequest> findByAccountId(String accountId, Pageable pageable) {
    return jpaRepository.findByAccountId(accountId, pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Optional<PaymentRequest> findByInvoiceId(String invoiceId) {
    Optional<HPaymentRequest> optional = jpaRepository.findByIdInvoice(invoiceId);
    return optional.map(mapper::toDomain);
  }
}
