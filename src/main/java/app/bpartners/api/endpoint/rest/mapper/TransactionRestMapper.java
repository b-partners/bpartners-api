package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionRestMapper {
  private final TransactionCategoryRestMapper categoryRestMapper;
  private final InvoiceRestMapper invoiceRestMapper;

  public Transaction toRest(app.bpartners.api.model.Transaction internal) {
    Transaction transaction = new Transaction()
        .id(internal.getId())
        .amount(internal.getAmount().getCentsRoundUp())
        .label(internal.getLabel())
        .paymentDatetime(internal.getPaymentDatetime())
        .reference(internal.getReference())
        .status(internal.getStatus())
        .type(internal.getType())
        .invoice(invoiceRestMapper.toRest(internal.getInvoiceDetails()));
    if (internal.getCategory() != null) {
      transaction.setCategory(List.of(categoryRestMapper.toRest(internal.getCategory())));
    }
    return transaction;
  }
}
