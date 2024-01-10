package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionExportLink;
import app.bpartners.api.model.TransactionExportDetails;
import app.bpartners.api.model.mapper.FileMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionRestMapper {
  private final TransactionCategoryRestMapper categoryRestMapper;
  private final InvoiceRestMapper invoiceRestMapper;
  private final FileMapper fileMapper;

  public TransactionExportLink toRest(TransactionExportDetails domain) {
    return new TransactionExportLink()
        .downloadLink(domain.getDownloadLink())
        .createdAt(domain.getCreatedAt())
        .expiredAt(domain.getExpiredAt());
  }

  public Transaction toRest(app.bpartners.api.model.Transaction internal) {
    Transaction transaction =
        new Transaction()
            .id(internal.getId())
            .amount(internal.getAmount().getCents())
            .label(internal.getLabel())
            .paymentDatetime(internal.getPaymentDatetime())
            .reference(internal.getReference())
            .status(internal.getStatus())
            .type(internal.getType())
            .invoice(invoiceRestMapper.toRest(internal.getInvoiceDetails()))
            .supportingDocs(internal.getSupportingDocuments().stream()
                .map(docs -> fileMapper.toRest(docs.getFileInfo()))
                .toList());
    if (internal.getCategory() != null) {
      transaction.setCategory(List.of(categoryRestMapper.toRest(internal.getCategory())));
    }
    return transaction;
  }
}
