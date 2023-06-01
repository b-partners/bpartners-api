package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Money;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.connectors.transaction.TransactionConnector;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.service.utils.MoneyUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionMapper {
  private final InvoiceMapper invoiceMapper;

  public TransactionConnector toConnector(BridgeTransaction bridgeTransaction) {
    return TransactionConnector.builder()
        .id(String.valueOf(bridgeTransaction.getId()))
        .amount(MoneyUtils.fromMinor(bridgeTransaction.getAbsAmount()))
        .currency(bridgeTransaction.getCurrency())
        .label(bridgeTransaction.getLabel())
        .transactionDate(bridgeTransaction.getTransactionDate())
        .updatedAt(bridgeTransaction.getUpdatedAt())
        .side(bridgeTransaction.getSide())
        .status(bridgeTransaction.getStatus())
        .build();
  }

  //TODO: check if necessary to set ZoneId to Paris
  public TransactionConnector toConnector(HTransaction entity) {
    LocalDate transactionDate =
        LocalDate.ofInstant(entity.getPaymentDateTime(), ZoneId.systemDefault());
    return TransactionConnector.builder()
        .id(String.valueOf(entity.getIdBridge()))
        .amount(MoneyUtils.fromMinorString(entity.getAmount()))
        .currency(entity.getCurrency())
        .label(entity.getLabel())
        .transactionDate(transactionDate)
        .updatedAt(transactionDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        .side(entity.getSide())
        .status(entity.getStatus())
        .build();
  }

  public HTransaction toEntity(String idAccount, TransactionConnector connector) {
    return HTransaction.builder()
        .idBridge(Long.valueOf(connector.getId()))
        .idAccount(idAccount)
        //TODO: must not be multiplied by 100 but persisted fraction is in major for now
        // * 100 is to delete when persisted fraction is in minor
        .amount(String.valueOf(connector.getAmount().multiply(new Money(100L))))
        .paymentDateTime(connector.getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant())
        .side(connector.getSide())
        .status(connector.getStatus())
        .currency(connector.getCurrency())
        .label(connector.getLabel())
        .build();
  }

  public Transaction toDomain(HTransaction entity, TransactionCategory category) {
    return Transaction.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .invoiceDetails(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
        .amount(MoneyUtils.fromMinorString(entity.getAmount()))
        .currency(entity.getCurrency())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .paymentDatetime(entity.getPaymentDateTime())
        .side(entity.getSide())
        .status(entity.getStatus())
        .category(category)
        .build();
  }
}
