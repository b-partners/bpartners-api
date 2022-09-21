package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateTransactionCategoryValidator implements Consumer<CreateTransactionCategory> {

  @Override
  public void accept(CreateTransactionCategory transactionCategory) {
    if (transactionCategory.getType() == null) {
      throw new BadRequestException("Type is mandatory");
    }
    if (transactionCategory.getVat() == null) {
      throw new BadRequestException("Vat is mandatory");
    }
  }
}
