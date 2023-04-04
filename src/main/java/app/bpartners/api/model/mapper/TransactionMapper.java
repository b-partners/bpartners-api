package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.swan.model.SwanTransaction;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.Transaction.BOOKED_STATUS;
import static app.bpartners.api.model.Transaction.PENDING_STATUS;
import static app.bpartners.api.model.Transaction.REJECTED_STATUS;
import static app.bpartners.api.model.Transaction.RELEASED_STATUS;
import static app.bpartners.api.model.Transaction.UPCOMING_STATUS;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionMapper {

  private final InvoiceMapper invoiceMapper;

  public static TransactionStatus getTransactionStatus(String status) {
    switch (status) {
      case PENDING_STATUS:
        return TransactionStatus.PENDING;
      case BOOKED_STATUS:
        return TransactionStatus.BOOKED;
      case UPCOMING_STATUS:
        return TransactionStatus.UPCOMING;
      case REJECTED_STATUS:
        return TransactionStatus.REJECTED;
      case RELEASED_STATUS:
        return TransactionStatus.RELEASED;
      default:
        log.warn("Unknown transaction status : " + status);
        return TransactionStatus.UNKNOWN;
    }
  }

  public static TransactionStatus getStatusFromBridge(BridgeTransaction bridgeTransaction) {
    //TODO: add DELETED status
    return !bridgeTransaction.isFuture()
        ? TransactionStatus.BOOKED :
        TransactionStatus.UPCOMING;
  }

  public Transaction toDomain(
      SwanTransaction external,
      HTransaction entity,
      TransactionCategory category) {
    String status = external.getNode().getStatusInfo().getStatus();
    return Transaction.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .idSwan(entity.getIdSwan())
        .transactionInvoice(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
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

  public Transaction toDomain(
      BridgeTransaction bridgeTransaction,
      HTransaction entity,
      TransactionCategory category) {
    return Transaction.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .idSwan(entity.getIdSwan())
        .idBridge(entity.getIdBridge())
        .transactionInvoice(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
        .amount(parseFraction(bridgeTransaction.getAbsAmount() * 100))
        .currency(bridgeTransaction.getCurrency())
        .label(bridgeTransaction.getLabel())
        .reference(entity.getReference())
        .paymentDatetime(bridgeTransaction.getCreatedDatetime())
        .category(category)
        .side(bridgeTransaction.getSide())
        .status(getStatusFromBridge(bridgeTransaction))
        .build();
  }

  public Transaction toDomain(HTransaction entity, TransactionCategory category) {
    return Transaction.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .idSwan(entity.getIdSwan())
        .transactionInvoice(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
        .amount(parseFraction(entity.getAmount()))
        .currency(entity.getCurrency())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .paymentDatetime(entity.getPaymentDateTime())
        .side(entity.getSide())
        .status(entity.getStatus())
        .category(category)
        .build();
  }

  public HTransaction toEntity(
      String accountId,
      SwanTransaction swanTransaction) {
    return HTransaction.builder()
        .idSwan(swanTransaction.getNode().getId())
        .idAccount(accountId)
        .amount(String.valueOf(parseFraction(swanTransaction.getNode().getAmount().getValue())))
        .paymentDateTime(swanTransaction.getNode().getCreatedAt())
        .side(swanTransaction.getNode().getSide())
        .status(getTransactionStatus(swanTransaction.getNode().getStatusInfo().getStatus()))
        .reference(swanTransaction.getNode().getReference())
        .currency(swanTransaction.getNode().getAmount().getCurrency())
        .label(swanTransaction.getNode().getLabel())
        .build();
  }

  public HTransaction toEntity(
      String accountId,
      BridgeTransaction bridgeTransaction) {
    return HTransaction.builder()
        .idBridge(bridgeTransaction.getId())
        .idAccount(accountId)
        .amount(String.valueOf(parseFraction(bridgeTransaction.getAbsAmount())))
        .paymentDateTime(bridgeTransaction
            .getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant())
        .side(bridgeTransaction.getSide())
        .status(getStatusFromBridge(bridgeTransaction))
        .reference(null)
        .currency(bridgeTransaction.getCurrency())
        .label(bridgeTransaction.getLabel())
        .build();
  }


  public HTransaction toEntity(Transaction domain) {
    return HTransaction.builder()
        .id(domain.getId())
        .idSwan(domain.getIdSwan())
        .idAccount(domain.getIdAccount())
        .invoice(invoiceMapper.toEntity(domain.getTransactionInvoice()))
        .side(domain.getSide())
        .paymentDateTime(domain.getPaymentDatetime())
        .amount(String.valueOf(domain.getAmount()))
        .currency(domain.getCurrency())
        .reference(domain.getReference())
        .status(domain.getStatus())
        .label(domain.getLabel())
        .build();
  }
}
