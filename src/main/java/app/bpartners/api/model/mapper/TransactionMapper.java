package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
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
  private final InvoiceMapper invoiceMapper;

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

  public HTransaction toEntity(Transaction domain) {
    return HTransaction.builder()
        .id(domain.getId())
        .idSwan(domain.getIdSwan())
        .idAccount(domain.getIdAccount())
        .invoice(invoiceMapper.toEntity(domain.getTransactionInvoice()))
        .status(domain.getStatus())
        .side(domain.getSide())
        .label(domain.getLabel())
        .reference(domain.getReference())
        .paymentDateTime(domain.getPaymentDatetime())
        .currency(domain.getCurrency())
        .amount(domain.getAmount().getCentsRoundUp())
        .type(domain.getType())
        .build();
  }

  public HTransaction toEntity(
      String accountId,
      app.bpartners.api.repository.swan.model.Transaction swanTransaction) {
    return HTransaction.builder()
        .idSwan(swanTransaction.getNode().getId())
        .idAccount(accountId)
        .status(getTransactionStatus(swanTransaction.getNode().getStatusInfo().getStatus()))
        .side(swanTransaction.getNode().getSide())
        .label(swanTransaction.getNode().getLabel())
        .reference(swanTransaction.getNode().getReference())
        .paymentDateTime(swanTransaction.getNode().getCreatedAt())
        .currency(swanTransaction.getNode().getAmount().getCurrency())
        .amount((int) (swanTransaction.getNode().getAmount().getValue() * 100))
        .build();
  }
}