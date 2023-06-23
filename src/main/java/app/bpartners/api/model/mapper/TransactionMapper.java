package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.connectors.transaction.TransactionConnector;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.MoneyUtils.fromMajor;
import static app.bpartners.api.service.utils.MoneyUtils.fromMinorString;
import static java.util.UUID.randomUUID;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionMapper {
  private final InvoiceMapper invoiceMapper;

  public TransactionConnector toConnector(BridgeTransaction bridgeTransaction) {
    return TransactionConnector.builder()
        .id(String.valueOf(bridgeTransaction.getId()))

        // TODO(bad-cents): it _seems_ bridge.amount is in major, while connector.amount is in minor
        .amount(fromMajor(bridgeTransaction.getAbsAmount()))

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
        .amount(fromMinorString(entity.getAmount()))
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
        .id(String.valueOf(randomUUID()))
        .idBridge(Long.valueOf(connector.getId()))
        .idAccount(idAccount)
        .amount(String.valueOf(connector.getAmount().getValue()))
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
        .amount(fromMinorString(entity.getAmount()))
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
