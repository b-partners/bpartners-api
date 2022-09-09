package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.model.exception.BadRequestException;

public class TransactionValidator {
  public void accept(Transaction transaction) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    TransactionCategory category = transaction.getCategory();
    if (category == null) {
      exceptionMessageBuilder.append("category is missing. ");
    }
    if (category != null) {
      if (category.getId() == null) {
        exceptionMessageBuilder.append("category.id is missing. ");
      }
      if (category.getComment() == null) {
        exceptionMessageBuilder.append("category.comment is missing. ");
      }
      if (category.getLabel() == null) {
        exceptionMessageBuilder.append("category.label is missing. ");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
