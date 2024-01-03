package app.bpartners.api.model.mapper;

import static app.bpartners.api.model.Money.fromMajor;
import static app.bpartners.api.model.Money.fromMinor;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionSupportingDocs;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.connectors.transaction.TransactionConnector;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class TransactionMapper {
  private final InvoiceMapper invoiceMapper;
  private final TransactionSupportingDocsMapper docsMapper;

  public TransactionConnector toConnector(BridgeTransaction bridgeTransaction) {
    return TransactionConnector.builder()
        .id(String.valueOf(bridgeTransaction.getId()))
        .amount(fromMinor(bridgeTransaction.getAbsAmount()))
        .currency(bridgeTransaction.getCurrency())
        .label(bridgeTransaction.getLabel())
        .transactionDate(bridgeTransaction.getTransactionDate())
        .updatedAt(bridgeTransaction.getUpdatedAt())
        .side(bridgeTransaction.getSide())
        .status(bridgeTransaction.getStatus())
        .build();
  }

  // TODO: check if necessary to set ZoneId to Paris
  public TransactionConnector toConnector(HTransaction entity) {
    LocalDate transactionDate =
        LocalDate.ofInstant(entity.getPaymentDateTime(), ZoneId.systemDefault());
    Money amount = fromMajor(entity.getAmount());
    return TransactionConnector.builder()
        .id(String.valueOf(entity.getIdBridge()))
        .amount(amount)
        .currency(entity.getCurrency())
        .label(entity.getLabel())
        .transactionDate(transactionDate)
        .updatedAt(transactionDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        .side(entity.getSide())
        .status(entity.getStatus())
        .build();
  }

  public HTransaction toEntity(Transaction domain,
                               HInvoice invoiceEntity) {

    return HTransaction.builder()
        .id(domain.getId())
        .idBridge(domain.getIdBridge())
        .idAccount(domain.getIdAccount())
        .amount(String.valueOf(domain.getAmount().getValue()))
        .paymentDateTime(domain.getPaymentDatetime())
        .side(domain.getSide())
        .status(domain.getStatus())
        .currency(domain.getCurrency())
        .label(domain.getLabel())
        .enableStatus(domain.getEnableStatus())
        .invoice(invoiceEntity)
        .build();
  }

  public HTransaction toEntity(String idAccount, TransactionConnector connector) {
    return HTransaction.builder()
        .id(String.valueOf(randomUUID()))
        .idBridge(Long.valueOf(connector.getId()))
        .idAccount(idAccount)
        .amount(String.valueOf(connector.getAmount().getValue()))
        .paymentDateTime(
            connector.getTransactionDate().atStartOfDay(ZoneId.systemDefault()).toInstant())
        .side(connector.getSide())
        .status(connector.getStatus())
        .currency(connector.getCurrency())
        .label(connector.getLabel())
        .enableStatus(EnableStatus.ENABLED)
        .build();
  }

  public Transaction toDomain(
      HTransaction entity,
      TransactionCategory category,
      List<TransactionSupportingDocs> supportingDocs) {
    return Transaction.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .invoiceDetails(invoiceMapper.toTransactionInvoice(entity.getInvoice()))
        .amount(fromMajor(entity.getAmount()))
        .currency(entity.getCurrency())
        .label(entity.getLabel())
        .reference(entity.getReference())
        .paymentDatetime(entity.getPaymentDateTime())
        .side(entity.getSide())
        .status(entity.getStatus())
        .category(category)
        .enableStatus(entity.getEnableStatus())
        .supportingDocuments(supportingDocs)
        .build();
  }
}
