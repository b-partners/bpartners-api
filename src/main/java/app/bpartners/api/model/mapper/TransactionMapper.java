package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionMapper {
  private final InvoiceMapper invoiceMapper;

  public static TransactionStatus getStatusFromBridge(BridgeTransaction bridgeTransaction) {
    //TODO: add DELETED status
    return !bridgeTransaction.isFuture()
        ? TransactionStatus.BOOKED :
        TransactionStatus.UPCOMING;
  }

  public Transaction toDomain(
      BridgeTransaction bridgeTransaction,
      HTransaction entity,
      TransactionCategory category) {
    return Transaction.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .idBridge(entity.getIdBridge())
        .invoiceDetails(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
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
        .invoiceDetails(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
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
}
