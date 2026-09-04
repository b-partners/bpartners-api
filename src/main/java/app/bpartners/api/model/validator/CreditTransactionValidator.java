package app.bpartners.api.model.validator;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class CreditTransactionValidator {
  public void accept(CreditTransaction creditTransaction) {
    StringBuilder messageBuilder = new StringBuilder();
    if (creditTransaction.getUserId() == null) {
      messageBuilder.append("CreditTransaction.userId is mandatory. ");
    }
    if (creditTransaction.getType() == null) {
      messageBuilder.append("CreditTransaction.type is mandatory. ");
    }
    if (creditTransaction.getMovementType() == null) {
      messageBuilder.append("CreditTransaction.movementType is mandatory. ");
    }
    if (creditTransaction.getCredits() == null || creditTransaction.getCredits() <= 0) {
      messageBuilder.append("CreditTransaction.credits must be a positive magnitude. ");
    }
    String errorMessage = messageBuilder.toString().trim();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }
}
