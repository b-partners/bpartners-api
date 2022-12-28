package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.Transaction.BOOKED_STATUS;
import static app.bpartners.api.model.Transaction.PENDING_STATUS;
import static app.bpartners.api.model.Transaction.UPCOMING_STATUS;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
public class TransactionMapper {


  public Transaction toDomain(app.bpartners.api.repository.swan.model.Transaction external,
                              TransactionCategory category) {
    String status = external.getNode().getStatusInfo().getStatus();
    return Transaction.builder()
        .id(external.getNode().getId())
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

  private static TransactionStatus getTransactionStatus(String status) {
    switch (status) {
      case PENDING_STATUS:
        return TransactionStatus.PENDING;
      case BOOKED_STATUS:
        return TransactionStatus.BOOKED;
      case UPCOMING_STATUS:
        return TransactionStatus.UPCOMING;
      default:
        log.warn("Unknown transaction status : " + status);
        return TransactionStatus.UNKNOWN;
    }
  }
}
