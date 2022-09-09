package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategoryType;
import app.bpartners.api.model.exception.BadRequestException;

public class TransactionCategoryTypeValidator {
  public void accept(CreateTransactionCategoryType rest) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (rest.getLabel() == null) {
      exceptionMessageBuilder.append("label is missing. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
