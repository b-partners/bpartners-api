package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransaction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.Transaction.BOOKED_STATUS;
import static app.bpartners.api.model.Transaction.PENDING_STATUS;
import static app.bpartners.api.model.Transaction.REJECTED_STATUS;
import static app.bpartners.api.model.Transaction.UPCOMING_STATUS;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionMapper {
  private AuthenticatedResourceProvider provider;

  private static TransactionStatus getTransactionStatus(String status) {
    switch (status) {
      case PENDING_STATUS:
        return TransactionStatus.PENDING;
      case BOOKED_STATUS:
        return TransactionStatus.BOOKED;
      case UPCOMING_STATUS:
        return TransactionStatus.UPCOMING;
      case REJECTED_STATUS:
        return TransactionStatus.REJECTED;
      default:
        log.warn("Unknown transaction status : " + status);
        return TransactionStatus.UNKNOWN;
    }
  }

  public Transaction toDomain(
      app.bpartners.api.repository.swan.model.Transaction external,
      HTransaction entity,
      TransactionCategory category) {
    String status = external.getNode().getStatusInfo().getStatus();
    return Transaction.builder()
        .id(entity.getId())
        .amount(parseFraction(external.getNode().getAmount().getValue() * 100))
        .currency(external.getNode().getAmount().getCurrency())
        .label(external.getNode().getLabel())
        .reference(external.getNode().getReference())
        .paymentDatetime(external.getNode().getCreatedAt())
        .category(category)
        .side(external.getNode().getSide())
        .status(getTransactionStatus(status))
        .build();
  }

  public HTransaction toEntity(
      app.bpartners.api.repository.swan.model.Transaction swanTransaction) {
    return HTransaction.builder()
        .idSwan(swanTransaction.getNode().getId())
        .idAccount(provider.getAccount().getId())
        .build();
  }
}
