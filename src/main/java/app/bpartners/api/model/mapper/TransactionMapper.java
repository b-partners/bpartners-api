package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.exception.ApiException;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.Transaction.BOOKED_STATUS;
import static app.bpartners.api.model.Transaction.PENDING_STATUS;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class TransactionMapper {

  public static final String UPCOMING_STATUS = "Upcoming";

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
    TransactionStatus transactionStatus;
    switch (status) {
      case PENDING_STATUS:
        transactionStatus = TransactionStatus.PENDING;
        break;
      case BOOKED_STATUS:
        transactionStatus = TransactionStatus.BOOKED;
        break;
      case UPCOMING_STATUS:
        transactionStatus = TransactionStatus.UPCOMING;
        break;
      default:
        throw new ApiException(SERVER_EXCEPTION, "Unknown transactions status " + status);
    }
    return transactionStatus;
  }
}
