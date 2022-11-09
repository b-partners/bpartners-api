package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateTransactionCategoryValidator implements Consumer<CreateTransactionCategory> {

  @Override
  public void accept(CreateTransactionCategory transactionCategory) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (transactionCategory.getType() == null) {
      exceptionMessageBuilder.append("Type is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
